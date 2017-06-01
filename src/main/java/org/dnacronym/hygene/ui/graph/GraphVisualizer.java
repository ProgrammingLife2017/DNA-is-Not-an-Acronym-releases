package org.dnacronym.hygene.ui.graph;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.core.HygeneEventBus;
import org.dnacronym.hygene.events.NodeMetadataCacheUpdateEvent;
import org.dnacronym.hygene.models.Edge;
import org.dnacronym.hygene.models.Graph;
import org.dnacronym.hygene.models.Node;
import org.dnacronym.hygene.models.NodeColor;
import org.dnacronym.hygene.models.NodeMetadataCache;
import org.dnacronym.hygene.models.SequenceDirection;
import org.dnacronym.hygene.parser.ParseException;


/**
 * A simple canvas that allows drawing of primitive shapes.
 * <p>
 * It observes the {@link Graph} in {@link GraphDimensionsCalculator}. When the list of nodes qeuried nodes changes
 * in {@link GraphDimensionsCalculator}, then it will clear the {@link Canvas} and draw the new {@link Node}s on the
 * {@link Canvas} using a {@link GraphicsContext}.
 *
 * @see Canvas
 * @see GraphicsContext
 * @see GraphDimensionsCalculator
 */
public final class GraphVisualizer {
    private static final Logger LOGGER = LogManager.getLogger(GraphVisualizer.class);

    private static final double DEFAULT_NODE_HEIGHT = 20;
    private static final double DEFAULT_DASH_LENGTH = 10;
    /**
     * Font used inside the nodes, this should always be a monospace font.
     */
    private static final String DEFAULT_NODE_FONT = "Consolas";
    /**
     * Scalar for the size of the node text font as fraction of the node's height.
     */
    private static final double DEFAULT_NODE_FONT_HEIGHT_SCALAR = 0.7;
    /**
     * Range used when new graph is set, unless graph contains too few nodes.
     */
    private static final int MAX_GRAPH_RADIUS_NODE_TEXT = 100;
    private static final double DEFAULT_RANGE = 10;

    private static final double DEFAULT_EDGE_WIDTH = 1;
    private static final Color DEFAULT_EDGE_COLOR = Color.GREY;
    private static final int EDGE_OPACITY_OFFSET = 80;
    private static final double EDGE_OPACITY_ALPHA = 1.08;
    private static final double EDGE_OPACITY_BETA = 4.25;

    private final GraphDimensionsCalculator graphDimensionsCalculator;

    private final ObjectProperty<Node> selectedNodeProperty;
    private final ObjectProperty<Edge> selectedEdgeProperty;

    private final ObjectProperty<Color> edgeColorProperty;
    private final DoubleProperty nodeHeightProperty;

    private final BooleanProperty displayLaneBordersProperty;

    private Graph graph;
    private NodeMetadataCache nodeMetadataCache;

    private Canvas canvas;
    private GraphicsContext graphicsContext;

    private RTree rTree;


    /**
     * Create a new {@link GraphVisualizer} instance.
     * <p>
     * The passed {@link GraphStore} is observed by this class. If the {@link GraphStore}
     * {@link org.dnacronym.hygene.parser.GfaFile} is updated, it will prompt a redraw. Changing the properties of this
     * class will also prompt a redraw if the {@link org.dnacronym.hygene.parser.GfaFile} in {@link GraphStore} is not
     * {@code null}.
     *
     * @param graphDimensionsCalculator {@link GraphDimensionsCalculator} used to calculate node positions
     */
    public GraphVisualizer(final GraphDimensionsCalculator graphDimensionsCalculator) {
        HygeneEventBus.getInstance().register(this);

        this.graphDimensionsCalculator = graphDimensionsCalculator;

        selectedNodeProperty = new SimpleObjectProperty<>();
        selectedEdgeProperty = new SimpleObjectProperty<>();

        getSelectedNodeProperty().addListener((observable, oldValue, newValue) -> draw());

        edgeColorProperty = new SimpleObjectProperty<>(DEFAULT_EDGE_COLOR);
        nodeHeightProperty = new SimpleDoubleProperty(DEFAULT_NODE_HEIGHT);
        graphDimensionsCalculator.getNodeHeightProperty().bind(nodeHeightProperty);

        edgeColorProperty.addListener((observable, oldValue, newValue) -> draw());
        nodeHeightProperty.addListener((observable, oldValue, newValue) -> draw());

        displayLaneBordersProperty = new SimpleBooleanProperty();
        displayLaneBordersProperty.addListener((observable, oldValue, newValue) -> draw());

        graphDimensionsCalculator.getGraphProperty().addListener((observable, oldValue, newValue) ->
                setGraph(newValue));
        graphDimensionsCalculator.getObservableQueryNodes().addListener((ListChangeListener<Integer>) change -> draw());
    }


    /**
     * Draw a node on the canvas.
     * <p>
     * The node is afterwards added to the {@link RTree}.
     *
     * @param calculator reference to the {@link GraphDimensionsCalculator} for the current drawing
     * @param graph      graph which contains all the nodes and their information
     * @param nodeId     id of node to draw
     * @param charWidth  the width of character
     * @param charHeight the height of a character
     * @param nodeId id of node to draw
     */
    private void drawNode(final int nodeId, final double charWidth, final double charHeight) {
        final double rectX = graphDimensionsCalculator.computeXPosition(nodeId);
        final double rectY = graphDimensionsCalculator.computeYPosition(nodeId);
        final double rectWidth = graphDimensionsCalculator.computeWidth(nodeId);
        final double rectHeight = nodeHeightProperty.get();

        graphicsContext.setFill(getNodeColor(nodeId, graph));
        graphicsContext.fillRect(rectX, rectY, rectWidth, rectHeight);

        if (nodeMetadataCache.has(nodeId) && hopsProperty.get() < MAX_GRAPH_RADIUS_NODE_TEXT) {
            graphicsContext.setFill(Color.BLACK);

            final int charCount = (int) (rectWidth / charWidth);

        final double fontX = rectX + 0.5 * (rectWidth - charCount * charWidth);
        final double fontY = rectY + 0.5 * graphDimensionsCalculator.getNodeHeight() + 0.25 * charHeight;

            try {
                final String sequence = nodeMetadataCache.getOrRetrieve(nodeId).retrieveMetadata().getSequence();
                graphicsContext.fillText(sequence.substring(0, Math.min(sequence.length(), charCount)), fontX, fontY);
            } catch (ParseException e) {
                LOGGER.error(e);
            }
        }

        rTree.addNode(nodeId, rectX, rectY, rectWidth, rectHeight);
    }

    /**
     * Generates a new {@link Font} that will be scaled such that it fits inside a node.
     *
     * @param nodeHeight the node height
     * @return the generated {@link Font}
     */
    private Font generateNodeFont(final double nodeHeight) {
        final double font1PHeight = getCharHeight(new Font(DEFAULT_NODE_FONT, 1));

        final double fontSize = DEFAULT_NODE_FONT_HEIGHT_SCALAR * nodeHeight / font1PHeight;

        return new Font(DEFAULT_NODE_FONT, fontSize);
    }

    /**
     * Computes the width of a single character for a specific font.
     *
     * @param font the {@link Font}
     * @return the width in pixels
     */
    private double getCharWidth(final Font font) {
        final Text t = new Text("X");
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    /**
     * Computes the height of a single character for a specific font.
     *
     * @param font the {@link Font}
     * @return the width in pixels
     */
    private double getCharHeight(final Font font) {
        final Text t = new Text("X");
        t.setFont(font);
        return t.getLayoutBounds().getHeight();
    }

    /**
     * Retrieve the {@link Color} of a specific node.
     *
     * @param nodeId the node id
     * @param graph  the {@link Graph}
     * @return the {@link Color}
     */
    private Color getNodeColor(final int nodeId, final Graph graph) {
        final Node selectedNode = getSelectedNodeProperty().get();
        if (selectedNode != null && selectedNode.getId() == nodeId) {
            return NodeColor.BRIGHT_GREEN.getFXColor();
        }
        return graph.getColor(nodeId).getFXColor();
    }

    /**
     * Draws an edge on the canvas.
     * <p>
     * The edge is afterwards added to the {@link RTree}.
     *
     * @param fromNodeId edge origin node ID
     * @param toNodeId   edge destination node ID
     */
    private void drawEdge(final int fromNodeId, final int toNodeId) {
        final double fromX = graphDimensionsCalculator.computeRightXPosition(fromNodeId);
        final double fromY = graphDimensionsCalculator.computeMiddleYPosition(fromNodeId);
        final double toX = graphDimensionsCalculator.computeXPosition(toNodeId);
        final double toY = graphDimensionsCalculator.computeMiddleYPosition(toNodeId);

        graphicsContext.setStroke(getEdgeColor());
        graphicsContext.setLineWidth(DEFAULT_EDGE_WIDTH);
        graphicsContext.strokeLine(fromX, fromY, toX, toY);

        rTree.addEdge(fromNodeId, toNodeId, fromX, fromY, toX, toY);
    }


    /**
     * Computes the opacity based on the graph radius, which is an approximation of the current zoom level.
     * <p>
     * The formula used to compute the edge opacity is as follows:
     * <p>
     * opacity(radius) = 1 - 1 / ( 1 + e^( -(alpha * ln( max(1, hops - offset) - beta) ) )
     * <p><ul>
     * <li>{@code offset} is roughly the amount of hops after which the opacity scaling will start.
     * <li>{@code alpha} affects the slope of the curve.
     * <li>{@code beta} increasing beta will essentially cause the curve to lift up a bit smoothing it out.
     * </ul>
     *
     * @return the edge opacity
     */
    private double computeEdgeOpacity() {
        return 1.0 - 1.0 / (1.0 + Math.exp(-(EDGE_OPACITY_ALPHA
                * Math.log(Math.max(1, graphDimensionsCalculator.getRadiusProperty().get() - EDGE_OPACITY_OFFSET))
                - EDGE_OPACITY_BETA)));
    }

    /**
     * Retrieves the {@link Color} of an edge.
     *
     * @return the {@link Color}
     */
    private Color getEdgeColor() {
        return edgeColorProperty.getValue().deriveColor(1, 1, 1, computeEdgeOpacity());
    }

    /**
     * Clear the canvas, and resets the {@link RTree}.
     */
    private void clear() {
        rTree = new RTree();
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Populate the graphs primitives with the given graph.
     * <p>
     * First clears the graph before drawing. If {@link Graph} is null, only clears the canvas.
     *
     * @throws IllegalStateException if the {@link Canvas} has not been set
     */
    public void draw() {
        if (canvas == null || graphicsContext == null) {
            throw new IllegalStateException("Attempting to draw whilst canvas not set.");
        }

        clear();
        for (final Integer nodeId : graphDimensionsCalculator.getObservableQueryNodes()) {
            drawNode(nodeId);
        rTree = new RTree();

        final int centerNodeId = centerNodeIdProperty.get();

        graphDimensionsCalculator.calculate(graph, canvas, centerNodeId, hopsProperty.get(), nodeHeightProperty.get());
        final List<Integer> neighbours = graphDimensionsCalculator.getNeighbours();
        onScreenNodeCountProperty.set(neighbours.size());

        final Font nodeFont = generateNodeFont(nodeHeightProperty.getValue());
        graphicsContext.setFont(nodeFont);

        final double charWidth = getCharWidth(nodeFont);
        final double charHeight = getCharHeight(nodeFont);
        for (final Integer nodeId : neighbours) {
            drawNode(graphDimensionsCalculator, graph, nodeId, charWidth, charHeight);

            graph.iterator().visitDirectNeighbours(
                    nodeId, SequenceDirection.RIGHT,
                    neighbourId -> drawEdge(nodeId, neighbourId));
        }

        if (displayLaneBordersProperty.get()) {
            drawLaneBorders(
                    graphDimensionsCalculator.getLaneCountProperty().get(),
                    graphDimensionsCalculator.getLaneHeightProperty().get());
        }
    }

    /**
     * Will listen for {@link NodeMetadataCacheUpdateEvent}, if so we redraw the graph to reflect the changes.
     *
     * @param event the {@link NodeMetadataCacheUpdateEvent}
     */
    @Subscribe
    public void onNodeMetadataCacheUpdate(final NodeMetadataCacheUpdateEvent event) {
        Platform.runLater(this::draw);
    }

    /**
     * Draw the border between bands as {@link Color#BLACK}.
     *
     * @param laneCount  amount of bands onscreen
     * @param laneHeight height of each lane
     */
    private void drawLaneBorders(final int laneCount, final double laneHeight) {
        final Paint originalStroke = graphicsContext.getStroke();
        final double originalLineWidth = graphicsContext.getLineWidth();

        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(1);
        graphicsContext.setLineDashes(DEFAULT_DASH_LENGTH);

        for (int band = 1; band < laneCount; band++) {
            graphicsContext.strokeLine(
                    0,
                    band * laneHeight,
                    canvas.getWidth(),
                    band * laneHeight
            );
        }

        graphicsContext.setStroke(originalStroke);
        graphicsContext.setLineWidth(originalLineWidth);
        graphicsContext.setLineDashes(1);
    }

    /**
     * Set the {@link Graph} for use in the {@link GraphVisualizer}.
     * <p>
     * The {@link Graph} is used to get node colors.
     *
     * @param graph the {@link Graph} for use in the {@link GraphVisualizer}
     */
    void setGraph(final Graph graph) {
        this.graph = graph;

        if (nodeMetadataCache != null) {
            HygeneEventBus.getInstance().unregister(nodeMetadataCache);
        }
        nodeMetadataCache = new NodeMetadataCache(graph);
        HygeneEventBus.getInstance().register(nodeMetadataCache);

        draw();
    }

    /**
     * Set {@link Canvas} which the {@link GraphVisualizer} use to draw.
     *
     * @param canvas canvas to be used to {@link GraphVisualizer}
     */
    public void setCanvas(final Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();

        canvas.setOnMouseClicked(event -> {
            if (rTree == null) {
                return;
            }

            selectedNodeProperty.setValue(null);
            selectedEdgeProperty.setValue(null);

            rTree.find(event.getX(), event.getY(),
                    this::setSelectedNode,
                    (fromNodeId, toNodeId) -> graph.getNode(fromNodeId).getOutgoingEdges().stream()
                            .filter(edge -> edge.getTo() == toNodeId)
                            .findFirst()
                            .ifPresent(selectedEdgeProperty::setValue)
            );
        });

        graphDimensionsCalculator.setCanvasSize(canvas.getWidth(), canvas.getHeight());
        canvas.widthProperty().addListener((observable, oldValue, newValue) ->
                graphDimensionsCalculator.setCanvasSize(newValue.doubleValue(), canvas.getHeight()));
        canvas.heightProperty().addListener((observable, oldValue, newValue) ->
                graphDimensionsCalculator.setCanvasSize(canvas.getWidth(), newValue.doubleValue()));
    }

    /**
     * Update the selected {@link Node} to the node with the given id.
     *
     * @param nodeId node id of the new selected {@link Node}
     */
    public void setSelectedNode(final int nodeId) {
        try {
            selectedNodeProperty.set(nodeMetadataCache.getOrRetrieve(nodeId));
        } catch (final ParseException e) {
            LOGGER.info("Metadata of selected node " + nodeId + " could not be parsed.");
        }
    }

    /**
     * The property of the selected node.
     * <p>
     * This node is updated every time the user clicks on a node in the canvas.
     *
     * @return Selected {@link Node} by the user, which can be {@code null}
     */
    public ObjectProperty<Node> getSelectedNodeProperty() {
        return selectedNodeProperty;
    }

    /**
     * The property of the selected edge.
     * <p>
     * This edge is updated every time the user clicks on an edge in the canvas.
     *
     * @return Selected {@link Edge} by the user, which can be {@code null}
     */
    public ObjectProperty<Edge> getSelectedEdgeProperty() {
        return selectedEdgeProperty;
    }

    /**
     * The property of onscreen edge {@link Color}s.
     *
     * @return property which decides the {@link Color} of edges
     */
    public ObjectProperty<Color> getEdgeColorProperty() {
        return edgeColorProperty;
    }

    /**
     * The property of onscreen node heights.
     *
     * @return property which decides the height of nodes
     */
    public DoubleProperty getNodeHeightProperty() {
        return nodeHeightProperty;
    }

    /**
     * The property which determines whether to display the border between lanes as black lines.
     *
     * @return property which decides whether to display the border between lanes
     */
    public BooleanProperty getDisplayBordersProperty() {
        return displayLaneBordersProperty;
    }
}
