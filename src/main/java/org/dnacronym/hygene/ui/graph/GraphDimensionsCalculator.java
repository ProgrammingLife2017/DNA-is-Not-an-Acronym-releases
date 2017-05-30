package org.dnacronym.hygene.ui.graph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.dnacronym.hygene.models.Graph;
import org.dnacronym.hygene.models.GraphQuery;

import java.util.LinkedList;
import java.util.List;


/**
 * Performs calculations for determining node/graph positions and dimensions.
 * <p>
 * These positions are used by the {@link GraphVisualizer} to draw nodes onscreen. The positions are determined by the
 * set canvas width and height, set center node id and set radius.
 * <p>
 * Every time the center node id is updated, or the range, a new query is performed to update the neighbours list. This
 * list represents the nodes that should be drawn onscreen. Node coordinates are determined as to make sure the nodes
 * fill the screen lengthwise.
 *
 * @see GraphDimensionsCalculator
 * @see GraphQuery
 */
public final class GraphDimensionsCalculator {
    /**
     * The default horizontal displacement between two adjacent nodes.
     */
    private static final int DEFAULT_EDGE_WIDTH = 1000;
    private static final int DEFAULT_RADIUS = 10;

    private final IntegerProperty minXNodeIdProperty;
    private final IntegerProperty maxXNodeIdProperty;

    private final IntegerProperty centerNodeIdProperty;
    private final IntegerProperty radiusProperty;
    private final IntegerProperty nodeCountProperty;

    private final DoubleProperty nodeHeightProperty;
    private final DoubleProperty laneHeightProperty;
    private final IntegerProperty laneCountProperty;

    /**
     * The {@link Graph} used to get the unscaled coordinates of nodes.
     */
    private Graph graph;
    private int minX;
    private int maxX;
    private int minY;

    private double canvasWidth = 0;
    private double canvasHeight = 0;

    /**
     * List of nodes that should be drawn onscreen.
     */
    private final ObservableList<Integer> observableNeighbours;
    private GraphQuery graphQuery;


    /**
     * Create a new instance of {@link GraphDimensionsCalculator}.
     */
    public GraphDimensionsCalculator() {
        minXNodeIdProperty = new SimpleIntegerProperty(1);
        maxXNodeIdProperty = new SimpleIntegerProperty(1);

        centerNodeIdProperty = new SimpleIntegerProperty(1);
        radiusProperty = new SimpleIntegerProperty(1);
        centerNodeIdProperty.addListener((observable, oldValue, newValue) -> {
            centerNodeIdProperty.set(Math.max(
                    0,
                    Math.min(newValue.intValue(), getNodeCountProperty().subtract(1).get())));
            query();
        });
        radiusProperty.addListener((observable, oldValue, newValue) -> {
            radiusProperty.set(Math.max(
                    1,
                    Math.min(newValue.intValue(), getNodeCountProperty().divide(2).get())));
            query();
        });

        nodeCountProperty = new SimpleIntegerProperty(1);

        nodeHeightProperty = new SimpleDoubleProperty();
        laneHeightProperty = new SimpleDoubleProperty(1);
        laneCountProperty = new SimpleIntegerProperty(1);

        observableNeighbours = FXCollections.observableArrayList();
    }


    /**
     * Calculate the following values.
     * <p>
     * <ul>
     * <li> Neighbours list
     * <li> Minimum unscaled x
     * <li> Maximum unscaled y
     * <li> Minimum unscaled y
     * <li> Lane count property
     * <li> Lane height property
     * <li> On screen node count property
     * </ul>
     * <p>
     * If the graph has not been set, this method does nothing.
     */
    private void calculate() {
        if (graph == null) {
            return;
        }

        final int centerNodeId = centerNodeIdProperty.get();

        final int unscaledCenterX = graph.getUnscaledXPosition(centerNodeId)
                + graph.getUnscaledXEdgeCount(centerNodeId) * DEFAULT_EDGE_WIDTH;
        final int[] tempMinX = {unscaledCenterX};
        final int[] tempMaxX = {unscaledCenterX};
        final int[] tempMinY = {graph.getUnscaledYPosition(centerNodeId)};
        final int[] tempMaxY = {graph.getUnscaledYPosition(centerNodeId)};

        final List<Integer> neighbours = new LinkedList<>();
        neighbours.add(centerNodeId);

        graphQuery.visit(nodeId -> {
            neighbours.add(nodeId);

            final int nodeLeftX = graph.getUnscaledXPosition(nodeId)
                    + graph.getUnscaledXEdgeCount(nodeId) * DEFAULT_EDGE_WIDTH;
            if (tempMinX[0] > nodeLeftX) {
                tempMinX[0] = nodeLeftX;
                minXNodeIdProperty.set(nodeId);
            }

            final int nodeRightX = graph.getUnscaledXPosition(nodeId) + graph.getLength(nodeId)
                    + graph.getUnscaledXEdgeCount(nodeId) * DEFAULT_EDGE_WIDTH;
            if (tempMaxX[0] < nodeRightX) {
                tempMaxX[0] = nodeRightX;
                maxXNodeIdProperty.set(nodeId);
            }

            tempMinY[0] = Math.min(tempMinY[0], graph.getUnscaledYPosition(nodeId));
            tempMaxY[0] = Math.max(tempMaxY[0], graph.getUnscaledYPosition(nodeId));
        });

        observableNeighbours.setAll(neighbours);

        this.minX = tempMinX[0];
        this.maxX = tempMaxX[0];
        this.minY = tempMinY[0];
        final int maxY = tempMaxY[0];

        laneHeightProperty.set(canvasHeight / laneCountProperty.get());
        laneCountProperty.set(Math.abs(maxY - minY) + 1);
    }

    /**
     * Sets the {@link Graph} used for calculations.
     * <p>
     * Sets the center node id to halfway the graph, and the range to 10.
     *
     * @param graph the {@link Graph} for use by calculations
     */
    void setGraph(final Graph graph) {
        this.graph = graph;
        graphQuery = new GraphQuery(graph);

        nodeCountProperty.set(graph.getNodeArrays().length);
        centerNodeIdProperty.set(nodeCountProperty.divide(2).intValue());
        radiusProperty.set(DEFAULT_RADIUS);

        calculate(); // force a recalculation
    }

    /**
     * Sets the size of the canvas on which the {@link Graph} {@link org.dnacronym.hygene.models.Node}s will be drawn.
     * <p>
     * This will perform another calculation.
     *
     * @param canvasWidth  the width of the {@link javafx.scene.canvas.Canvas}
     * @param canvasHeight the height of the {@link javafx.scene.canvas.Canvas}
     */
    void setCanvasSize(final double canvasWidth, final double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        calculate();
    }

    /**
     * Perform a new query with the set center node id and set range.
     *
     * @see GraphQuery#query(int, int)
     */
    public void query() {
        graphQuery.query(centerNodeIdProperty.get(), radiusProperty.get());
        calculate();
    }

    /**
     * Computes the absolute x position of a node within the current canvas.
     *
     * @param nodeId the id of the node
     * @return the absolute x position of a node within the current canvas
     */
    double computeXPosition(final int nodeId) {
        final int xPosition = graph.getUnscaledXPosition(nodeId) - graph.getLength(nodeId)
                + graph.getUnscaledXEdgeCount(nodeId) * DEFAULT_EDGE_WIDTH;
        return (double) (xPosition - minX) / (maxX - minX) * canvasWidth;
    }

    /**
     * Computes the absolute right x position of a node in pixels within the set canvas dimensions.
     *
     * @param nodeId the id of the node
     * @return the absolute right x position of a node within the current canvas
     */
    double computeRightXPosition(final int nodeId) {
        return computeXPosition(nodeId) + computeWidth(nodeId);
    }

    /**
     * Computes the absolute y position of a node in pixels within the set canvas dimensions.
     *
     * @param nodeId the id of the node
     * @return the absolute y position of a node within the current canvas
     */
    double computeYPosition(final int nodeId) {
        final int yPosition = graph.getUnscaledYPosition(nodeId);
        return (yPosition - minY) * laneHeightProperty.get() + laneHeightProperty.get() / 2
                - nodeHeightProperty.get() / 2;
    }

    /**
     * Computes the absolute middle y position of a node in pixels within the set canvas dimensions.
     *
     * @param nodeId the id of the node
     * @return the absolute middle y position of a node within the current canvas
     */
    double computeMiddleYPosition(final int nodeId) {
        return computeYPosition(nodeId) + nodeHeightProperty.get() / 2;
    }

    /**
     * Computes the width of a node in pixels within the set canvas dimensions.
     *
     * @param nodeId the id of the node
     * @return the width of a node
     */
    double computeWidth(final int nodeId) {
        return (double) graph.getLength(nodeId) / (maxX - minX) * canvasWidth;
    }

    /**
     * Returns the {@link ReadOnlyIntegerProperty} which describes the node in the current neighbours list with the
     * smallest (leftmost) x position.
     *
     * @return the {@link ReadOnlyIntegerProperty} describing the id of the node with the smallest x position in
     * neighbours
     */
    public ReadOnlyIntegerProperty getMinXNodeIdProperty() {
        return minXNodeIdProperty;
    }

    /**
     * Returns the {@link ReadOnlyIntegerProperty} which describes the node in the current neighbours list with the
     * largest (rightmost) x position.
     *
     * @return the {@link ReadOnlyIntegerProperty} describing the id of the node with the largest x position in
     * neighbours
     */
    public ReadOnlyIntegerProperty getMaxXNodeIdProperty() {
        return maxXNodeIdProperty;
    }

    /**
     * Returns the {@link ReadOnlyIntegerProperty} which describes the amount of lanes.
     *
     * @return the {@link ReadOnlyIntegerProperty} which describes the amount of lanes
     */
    public ReadOnlyIntegerProperty getLaneCountProperty() {
        return laneCountProperty;
    }

    /**
     * Returns the {@link ReadOnlyDoubleProperty} which describes the height of lanes.
     *
     * @return the {@link ReadOnlyDoubleProperty} which describes the height of lanes
     */
    public ReadOnlyDoubleProperty getLaneHeightProperty() {
        return laneHeightProperty;
    }

    /**
     * Return the {@link ReadOnlyIntegerProperty} which describes the amount of nodes in the set graph.
     *
     * @return the {@link ReadOnlyIntegerProperty} which describes the amount of nodes in the set graph
     */
    public ReadOnlyIntegerProperty getNodeCountProperty() {
        return nodeCountProperty;
    }

    /**
     * Get the {@link ReadOnlyListProperty} of the neighbours of the set center id with the set range.
     * <p>
     * Every time this list changes, all nodes in the previous list should be discarded and all nodes in the new list
     * should be drawn. This list is updated every time a new calculation is performed, which happens every time the
     * center node id or range is changed to a new value, a new canvas height and width are set or if the graph is
     * updated.
     *
     * @return the {@link ObservableList} of the neighbours of the set neighbours set range away
     */
    public ReadOnlyListProperty<Integer> getNeighbours() {
        return new ReadOnlyListWrapper<>(observableNeighbours);
    }

    /**
     * Returns the {@link DoubleProperty} which describes the height of nodes.
     *
     * @return the {@link DoubleProperty} which describes the height of nodes
     */
    public DoubleProperty getNodeHeightProperty() {
        return nodeHeightProperty;
    }

    /**
     * Returns the {@link IntegerProperty} which determines the current center
     * {@link org.dnacronym.hygene.models.Node} id.
     * <p>
     * Every time this value is updated, {@link #query} is called. When updated, it does a range check to make sure the
     * value remains in the range {@code [0, node count]}.
     *
     * @return property which decides the current center {@link org.dnacronym.hygene.models.Node} id
     */
    public IntegerProperty getCenterNodeIdProperty() {
        return centerNodeIdProperty;
    }

    /**
     * Returns the {@link IntegerProperty} which determines the range to draw around the center
     * {@link org.dnacronym.hygene.models.Node}.
     * <p>
     * Every time this value is updated, {@link #query} is called. When updated, it does a check to make sure that the
     * range remains in the range {@code [1, node count / 2]}.
     *
     * @return property which determines the amount of hops to draw in each direction around the center node
     */
    public IntegerProperty getRadiusProperty() {
        return radiusProperty;
    }
}
