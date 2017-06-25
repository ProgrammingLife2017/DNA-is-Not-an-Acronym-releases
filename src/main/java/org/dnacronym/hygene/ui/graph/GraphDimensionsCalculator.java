package org.dnacronym.hygene.ui.graph;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import org.dnacronym.hygene.core.HygeneEventBus;
import org.dnacronym.hygene.event.LayoutDoneEvent;
import org.dnacronym.hygene.event.NodeMetadataCacheUpdateEvent;
import org.dnacronym.hygene.graph.CenterPointQuery;
import org.dnacronym.hygene.graph.Graph;
import org.dnacronym.hygene.graph.Subgraph;
import org.dnacronym.hygene.graph.edge.Edge;
import org.dnacronym.hygene.graph.layout.FafospLayerer;
import org.dnacronym.hygene.graph.node.GfaNode;
import org.dnacronym.hygene.graph.node.Node;
import org.dnacronym.hygene.graph.node.Segment;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Performs calculations for determining node positions and dimensions onscreen.
 * <p>
 * These positions are used by the {@link GraphVisualizer} to draw nodes onscreen. The positions are determined by the
 * set canvas dimensions, center node id and radius.
 * <p>
 * Every time the center node id is updated, or the range, a new query is performed to update the neighbours list. This
 * list represents the nodes that should be drawn onscreen. Node coordinates are determined as to make sure the nodes
 * fill the screen lengthwise.
 *
 * @see GraphDimensionsCalculator
 * @see CenterPointQuery
 */
@SuppressWarnings("PMD.TooManyFields") // This class is tightly coupled, and does not need to be divided further.
public final class GraphDimensionsCalculator {
    /**
     * The default horizontal displacement between two adjacent nodes.
     */
    private static final int DEFAULT_RADIUS = 10;
    private static final int DEFAULT_LANE_COUNT = 10;

    private final IntegerProperty minXNodeIdProperty;
    private final IntegerProperty maxXNodeIdProperty;

    private final IntegerProperty centerNodeIdProperty;
    private final IntegerProperty radiusProperty;
    private final IntegerProperty viewPointProperty;
    private final IntegerProperty viewRadiusProperty;

    private final DoubleProperty nodeHeightProperty;
    private final IntegerProperty nodeCountProperty;
    private final DoubleProperty laneHeightProperty;
    private final IntegerProperty laneCountProperty;

    /**
     * The {@link Graph} used to get the unscaled coordinates of nodes.
     */
    private final ObjectProperty<Graph> graphProperty;
    private CenterPointQuery centerPointQuery;
    private Subgraph subgraph;

    private long minX;
    private long maxX;
    private int minY;
    private Dimension2D canvasDimension;

    private final ObservableList<Node> observableQueryNodes;
    private final ReadOnlyListWrapper<Node> readOnlyObservableNodes;


    /**
     * Create a new instance of {@link GraphDimensionsCalculator}.
     *
     * @param graphStore the {@link GraphStore} who's {@link org.dnacronym.hygene.parser.GfaFile} is observed
     */
    @Inject
    public GraphDimensionsCalculator(final GraphStore graphStore) {
        observableQueryNodes = FXCollections.observableArrayList();
        readOnlyObservableNodes = new ReadOnlyListWrapper<>(observableQueryNodes);

        minXNodeIdProperty = new SimpleIntegerProperty(1);
        maxXNodeIdProperty = new SimpleIntegerProperty(1);

        centerNodeIdProperty = new SimpleIntegerProperty(1);
        radiusProperty = new SimpleIntegerProperty(DEFAULT_RADIUS);

        nodeCountProperty = new SimpleIntegerProperty(1);

        centerNodeIdProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() < 0) {
                centerNodeIdProperty.set(0);
                return;
            }
            if (newValue.intValue() >= getNodeCountProperty().intValue()) {
                centerNodeIdProperty.set(nodeCountProperty.intValue() - 1);
                return;
            }

            centerPointQuery.query(centerNodeIdProperty.get(), radiusProperty.get());
        });
        radiusProperty.addListener((observable, oldValue, newValue) -> {
            if (centerPointQuery == null) {
                return;
            }
            centerPointQuery.query(centerNodeIdProperty.get(), radiusProperty.get());
        });

        viewPointProperty = new SimpleIntegerProperty(0);
        viewPointProperty.addListener(((observable, oldValue, newValue) -> {
            int difference = newValue.intValue() - oldValue.intValue();
            GfaNode centerNode = subgraph.getSegment(centerNodeIdProperty.intValue()).get();

            if (difference > 0) {
                // Move to right
                while (difference > 0) {
                    final Set<Edge> neighbours = centerNode.getOutgoingEdges();
                    if (neighbours.isEmpty()) {
                        return;
                    }

                    centerNode = neighbours.iterator().next().getToSegment();
                    centerNodeIdProperty.set(centerNode.getSegmentIds().get(0));
                    difference--;
                }
            } else {
                // Move to left
                while (difference < 0) {
                    final Set<Edge> neighbours = centerNode.getIncomingEdges();
                    if (neighbours.isEmpty()) {
                        return;
                    }

                    centerNode = neighbours.iterator().next().getFromSegment();
                    centerNodeIdProperty.set(centerNode.getSegmentIds().get(0));
                    difference++;
                }
            }
        }));
        viewRadiusProperty = new SimpleIntegerProperty(1);
        viewRadiusProperty.addListener((observable, oldValue, newValue) -> {
            calculate(subgraph);
            radiusProperty.set(((newValue.intValue() + FafospLayerer.LAYER_WIDTH - 1)
                    / FafospLayerer.LAYER_WIDTH) / 2);
        });

        nodeHeightProperty = new SimpleDoubleProperty(1);
        laneHeightProperty = new SimpleDoubleProperty(1);
        laneCountProperty = new SimpleIntegerProperty(1);

        graphProperty = new SimpleObjectProperty<>();
        graphStore.getGfaFileProperty().addListener((observable, oldValue, newValue) -> setGraph(newValue.getGraph()));

        HygeneEventBus.getInstance().register(this);
    }


    /**
     * Recalculates (and thereby redraws) the graph when the layout is done calculating.
     *
     * @param event a {@link LayoutDoneEvent}
     */
    @Subscribe
    public void onLayoutDoneEvent(final LayoutDoneEvent event) {
        subgraph = event.getSubgraph();

        Platform.runLater(() -> calculate(event.getSubgraph()));
    }

    /**
     * Recalculates (and thereby redraws) the graph when the metadata has been gathered.
     *
     * @param event a {@link NodeMetadataCacheUpdateEvent}
     */
    @Subscribe
    public void onNodeMetadataCacheUpdate(final NodeMetadataCacheUpdateEvent event) {
        Platform.runLater(() -> calculate(event.getSubgraph()));
    }

    /**
     * Calculates the following values.
     * <p><ul>
     * <li>Neighbours list
     * <li>Minimum unscaled x
     * <li>Maximum unscaled y
     * <li>Minimum unscaled y
     * <li>Lane count property
     * <li>Lane height property
     * <li>On screen node count property
     * </ul>
     * <p>If the graph or canvas has not been set, this method does nothing.
     *
     * @param subgraph the {@link Subgraph} to recalculate dimensions for
     */
    void calculate(final Subgraph subgraph) {
        final Graph graph = graphProperty.get();
        if (graph == null || subgraph == null || canvasDimension == null) {
            return;
        }

        final Segment centerNode = subgraph.getSegment(centerNodeIdProperty.get())
                .orElseThrow(() -> new IllegalStateException("Cannot calculate properties without a center node."));
        final long unscaledCenterX = centerNode.getXPosition();

        final int[] tempMinY = {centerNode.getYPosition()};
        final int[] tempMaxY = {centerNode.getYPosition()};

        final List<Node> neighbours = new LinkedList<>();
        subgraph.getNodes().forEach(node -> {
            neighbours.add(node);

            tempMinY[0] = Math.min(tempMinY[0], node.getYPosition());
            tempMaxY[0] = Math.max(tempMaxY[0], node.getYPosition());
        });

        this.minX = unscaledCenterX - viewRadiusProperty.get() / 2;
        this.maxX = unscaledCenterX + viewRadiusProperty.get() / 2;
        this.minY = tempMinY[0];
        final int maxY = tempMaxY[0];

        laneCountProperty.set(Math.max(Math.abs(maxY - minY) + 1, DEFAULT_LANE_COUNT));
        laneHeightProperty.set(canvasDimension.getHeight() / laneCountProperty.get());

        observableQueryNodes.setAll(neighbours);
    }

    /**
     * Sets the {@link Graph} used for calculations.
     * <p>
     * Sets the center node id to halfway the graph, and the range to 10.
     *
     * @param graph the {@link Graph} for use by calculations
     */
    void setGraph(final Graph graph) {
        graphProperty.set(graph);
        centerPointQuery = new CenterPointQuery(graph);

        nodeCountProperty.set(graph.getNodeArrays().length);
        centerNodeIdProperty.set(nodeCountProperty.divide(2).intValue());
        viewRadiusProperty.set(DEFAULT_RADIUS * FafospLayerer.LAYER_WIDTH);
    }

    /**
     * Gets the {@link ReadOnlyObjectProperty} which describes the current {@link Graph} of the
     * {@link GraphDimensionsCalculator}.
     *
     * @return the {@link ReadOnlyObjectProperty} which describes the current {@link Graph} of {@link
     * GraphDimensionsCalculator}
     */
    public ReadOnlyObjectProperty<Graph> getGraphProperty() {
        return graphProperty;
    }

    /**
     * Sets the size of the canvas on which the {@link Graph} {@link org.dnacronym.hygene.model.Node}s will be drawn.
     * <p>
     * This will perform another calculation.
     *
     * @param canvasWidth  the width of the {@link javafx.scene.canvas.Canvas}
     * @param canvasHeight the height of the {@link javafx.scene.canvas.Canvas}
     */
    void setCanvasSize(final double canvasWidth, final double canvasHeight) {
        canvasDimension = new Dimension2D(canvasWidth, canvasHeight);
    }

    /**
     * Computes the absolute x position of a node within the current canvas.
     *
     * @param node a {@link Node}
     * @return the absolute x position of a node within the current canvas
     */
    double computeXPosition(final Node node) {
        final long xPosition = node.getXPosition();
        return (double) (xPosition - minX) / (maxX - minX) * canvasDimension.getWidth();
    }

    /**
     * Computes the absolute right x position of a node in pixels within the set canvas dimensions.
     *
     * @param node a {@link Node}
     * @return the absolute right x position of a node within the current canvas
     */
    double computeRightXPosition(final Node node) {
        return computeXPosition(node) + computeWidth(node);
    }

    /**
     * Computes the absolute y position of a node in pixels within the set canvas dimensions.
     *
     * @param node a {@link Node}
     * @return the absolute y position of a node within the current canvas
     */
    double computeYPosition(final Node node) {
        final int yPosition = node.getYPosition();
        return (yPosition - minY) * laneHeightProperty.get() + laneHeightProperty.get() / 2
                - nodeHeightProperty.get() / 2;
    }

    /**
     * Computes the absolute middle y position of a node in pixels within the set canvas dimensions.
     *
     * @param node a {@link Node}
     * @return the absolute middle y position of a node within the current canvas
     */
    double computeMiddleYPosition(final Node node) {
        return computeYPosition(node) + nodeHeightProperty.get() / 2;
    }

    /**
     * Computes the width of a node in pixels within the set canvas dimensions.
     *
     * @param node a {@link Node}
     * @return the width of a node
     */
    double computeWidth(final Node node) {
        return ((double) node.getLength()) / (maxX - minX) * canvasDimension.getWidth();
    }

    /**
     * Gets the {@link ReadOnlyIntegerProperty} which describes the {@link Node} in the current query with the
     * smallest (leftmost) x position.
     *
     * @return the {@link ReadOnlyIntegerProperty} describing the {@link Node} with the smallest x position
     */
    public ReadOnlyIntegerProperty getMinXNodeIdProperty() {
        return minXNodeIdProperty;
    }

    /**
     * Gets the {@link ReadOnlyIntegerProperty} which describes the {@link Node} in the current query with the
     * largest (rightmost) x position.
     *
     * @return the {@link ReadOnlyIntegerProperty} describing the {@link Node} with the largest x position
     */
    public ReadOnlyIntegerProperty getMaxXNodeIdProperty() {
        return maxXNodeIdProperty;
    }

    /**
     * Gets the {@link ReadOnlyIntegerProperty} which describes the amount of lanes.
     *
     * @return the {@link ReadOnlyIntegerProperty} which describes the amount of lanes
     */
    public ReadOnlyIntegerProperty getLaneCountProperty() {
        return laneCountProperty;
    }

    /**
     * Gets the {@link ReadOnlyDoubleProperty} which describes the height of lanes.
     *
     * @return the {@link ReadOnlyDoubleProperty} which describes the height of lanes
     */
    public ReadOnlyDoubleProperty getLaneHeightProperty() {
        return laneHeightProperty;
    }

    /**
     * Gets the {@link ReadOnlyIntegerProperty} which describes the amount of nodes in the set graph.
     *
     * @return the {@link ReadOnlyIntegerProperty} which describes the amount of nodes in the set graph
     */
    public ReadOnlyIntegerProperty getNodeCountProperty() {
        return nodeCountProperty;
    }

    /**
     * Gets the {@link ReadOnlyListProperty} of the queried nodes.
     * <p>
     * Every time this list changes, all nodes in the previous list should be discarded and all nodes in the new list
     * should be drawn. This list is updated every time a new calculation is performed, which happens every time the
     * center node id or radius is changed to a new value, new canvas dimensions are set, or if the graph is updated.
     *
     * @return a {@link ReadOnlyListProperty} of the {@link Node} in the cache
     */
    public ReadOnlyListProperty<Node> getObservableQueryNodes() {
        return readOnlyObservableNodes;
    }

    /**
     * Gets the {@link DoubleProperty} which describes the height of nodes.
     *
     * @return the {@link DoubleProperty} which describes the height of nodes
     */
    public DoubleProperty getNodeHeightProperty() {
        return nodeHeightProperty;
    }

    /**
     * Gets the {@link IntegerProperty} which determines the current center
     * {@link org.dnacronym.hygene.model.Node} id.
     * <p>
     * Every time this value is changed, a calculation is done. When updated, it does a range check to make sure the
     * value remains in the range {@code [0, node count - 1]}.
     *
     * @return property which decides the current center {@link org.dnacronym.hygene.model.Node} id
     */
    public IntegerProperty getCenterNodeIdProperty() {
        return centerNodeIdProperty;
    }

    /**
     * Gets the {@link IntegerProperty} which determines the range to draw around the center
     * {@link org.dnacronym.hygene.model.Node}.
     * <p>
     * Every time this value is changed, a calculation is done. When updated, it does a check to make sure that the
     * range remains in the range {@code [1, node count / 2]}.
     *
     * @return property which determines the amount of hops to draw in each direction around the center node
     */
    public IntegerProperty getRadiusProperty() {
        return radiusProperty;
    }

    /**
     * Returns the view point property.
     *
     * @return the view point property
     */
    public IntegerProperty getViewPointProperty() {
        return viewPointProperty;
    }

    /**
     * Returns the view radius property.
     *
     * @return the view radius property
     */
    public IntegerProperty getViewRadiusProperty() {
        return viewRadiusProperty;
    }

    /**
     * Returns the center-point query.
     *
     * @return the center-point query
     */
    public CenterPointQuery getCenterPointQuery() {
        return centerPointQuery;
    }
}
