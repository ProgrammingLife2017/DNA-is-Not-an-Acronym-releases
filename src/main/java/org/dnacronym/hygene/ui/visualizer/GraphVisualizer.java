package org.dnacronym.hygene.ui.visualizer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dnacronym.hygene.models.Graph;
import org.dnacronym.hygene.models.Node;


/**
 * A simple canvas that allows drawing of primitive shapes.
 * <p>
 * When passing a {@link Graph}, it will draw it using JavaFX primitives. If the {@link Canvas} has not been set
 * all methods related to drawing will thrown an {@link IllegalStateException}.
 *
 * @see Canvas
 * @see GraphicsContext
 */
public final class GraphVisualizer {
    private static final double DEFAULT_NODE_HEIGHT = 20;
    private static final double DEFAULT_NODE_WIDTH = 0.001;
    private static final double DEFAULT_EDGE_WIDTH = 2;
    private static final double DEFAULT_DASH_LENGTH = 10;

    private static final Color DEFAULT_EDGE_COLOR = Color.GREY;
    private static final Color DEFAULT_NODE_COLOR = Color.BLUE;

    private final ObjectProperty<Node> selectedNodeProperty;

    private final ObjectProperty<Color> edgeColorProperty;
    private final DoubleProperty nodeHeightProperty;
    private final DoubleProperty nodeWidthProperty;
    private final DoubleProperty laneHeightProperty;

    private final BooleanProperty displayLaneBordersProperty;
    private final DoubleProperty borderDashLengthProperty;

    private @Nullable Graph graph;

    private @MonotonicNonNull Canvas canvas;
    private @MonotonicNonNull GraphicsContext graphicsContext;

    /**
     * Create a new {@link GraphVisualizer} instance.
     */
    public GraphVisualizer() {
        super();

        selectedNodeProperty = new SimpleObjectProperty<>();

        edgeColorProperty = new SimpleObjectProperty<>(DEFAULT_EDGE_COLOR);
        nodeHeightProperty = new SimpleDoubleProperty(DEFAULT_NODE_HEIGHT);
        nodeWidthProperty = new SimpleDoubleProperty(DEFAULT_NODE_WIDTH);
        laneHeightProperty = new SimpleDoubleProperty(DEFAULT_NODE_HEIGHT);

        displayLaneBordersProperty = new SimpleBooleanProperty();
        borderDashLengthProperty = new SimpleDoubleProperty(DEFAULT_DASH_LENGTH);
    }


    /**
     * Converts onscreen coordinates to coordinates which can be used to find the correct node.
     * <p>
     * The x coordinate depends on the widthproperty. The y property denotes in which lane the click is.
     *
     * @param xPos x position onscreen
     * @param yPos y position onscreen
     * @return x and y position in a double array of size 2 which correspond with x and y position of {@link
     * Node}.
     */
    private int[] toNodeCoordinates(final double xPos, final double yPos) {
        return new int[] {
                (int) Math.round(xPos / nodeWidthProperty.get()),
                (int) Math.floor(yPos / laneHeightProperty.get())
        };
    }

    /**
     * Draw edge on the {@link Canvas}.
     *
     * @param startHorizontal x position of the start of the line
     * @param startVertical   y position of the start of the line
     * @param endHorizontal   x position of the end of the line
     * @param endVertical     y position of the end of the line
     */
    @SuppressWarnings("nullness") // For performance, to prevent null checks during every draw.
    private void drawEdge(final double startHorizontal, final double startVertical,
                          final double endHorizontal, final double endVertical) {
        graphicsContext.setLineWidth(DEFAULT_EDGE_WIDTH);
        graphicsContext.strokeLine(
                startHorizontal * nodeWidthProperty.get(),
                (startVertical + 1.0 / 2.0) * laneHeightProperty.get(),
                endHorizontal * nodeWidthProperty.get(),
                (endVertical + 1.0 / 2.0) * laneHeightProperty.get()
        );
    }

    /**
     * Draws all onscreen edges between the current {@link Node} and it's right neighbours.
     * <p>
     * Afterwards, calls itself on each of the right neighbours of the current node.
     *
     * @param node the node who's edges should be drawn on the {@link Canvas}
     */
    private void drawEdges(final Node node) {
//        node.getRightNeighbours().forEach(neighbour -> drawEdge(
//                node.getHorizontalRightEnd(),
//                node.getVerticalPosition(),
//                (double) (neighbour.getHorizontalRightEnd() - neighbour.getLength()),
//                neighbour.getVerticalPosition()
//        ));
    }

    /**
     * Sets the fill of the {@link GraphicsContext} before proceeding to draw all onscreen edges.
     *
     * @param node the node representing the source of the graph
     * @param color        the color with which all edges should be drawn
     */
    @SuppressWarnings("nullness") // For performance, to prevent null checks during every draw.
    private void drawEdges(final Node node, final Color color) {
        graphicsContext.setStroke(color);
        drawEdges(node);
    }

    /**
     * Draw a node on the {@link Canvas}.
     *
     * @param startHorizontal  x position of the node
     * @param verticalPosition y position of the node
     * @param width            width of the node
     * @param color            color of the node
     */
    @SuppressWarnings("nullness") // For performance, to prevent null checks during every draw.
    private void drawNode(final double startHorizontal, final double verticalPosition,
                          final double width, final Color color) {
        graphicsContext.setFill(color);
        graphicsContext.fillRect(
                startHorizontal * nodeWidthProperty.get(),
                (verticalPosition + 1.0 / 2.0) * laneHeightProperty.get() - 1.0 / 2.0 * nodeHeightProperty.get(),
                width * nodeWidthProperty.get(),
                nodeHeightProperty.get()
        );
    }

    /**
     * Draws the given node to the screen.
     *
     * @param node  the node to draw
     * @param color the color to draw with
     */
    private void drawNode(final Node node, final Color color) {
//        drawNode(
//                (double) (node.getHorizontalRightEnd() - node.getLength()),
//                node.getVerticalPosition(),
//                node.getLength(),
//                color
//        );
    }

    /**
     * Draw the border between bands as {@link Color#BLACK}.
     *
     * @param laneCount  amount of bands onscreen
     * @param laneHeight height of each band
     */
    @SuppressWarnings("nullness") // For performance, to prevent null checks during every draw.
    private void drawBandEdges(final int laneCount, final double laneHeight) {
        final Paint orginalStroke = graphicsContext.getStroke();
        final double originalLineWidth = graphicsContext.getLineWidth();

        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineWidth(1);
        graphicsContext.setLineDashes(borderDashLengthProperty.get());

        for (int band = 1; band < laneCount; band++) {
            graphicsContext.strokeLine(
                    0,
                    band * laneHeight,
                    canvas.getWidth(),
                    band * laneHeight
            );
        }

        graphicsContext.setStroke(orginalStroke);
        graphicsContext.setLineWidth(originalLineWidth);
        graphicsContext.setLineDashes(0);
    }

    /**
     * Clear the canvas.
     */
    @SuppressWarnings("nullness") // For performance, to prevent null checks during every draw.
    public void clear() {
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * The property of the selected node. This node is updated every time the user clicks on the canvas.
     *
     * @return Selected {@link Node} by the user. Can be null.
     */
    public ObjectProperty<Node> getSelectedNodeProperty() {
        return selectedNodeProperty;
    }

    /**
     * The property of onscreen edge colors.
     *
     * @return property which decides the color of edges.
     */
    public ObjectProperty<Color> getEdgeColorProperty() {
        return edgeColorProperty;
    }

    /**
     * The property of onscreen node heights.
     *
     * @return property which decides the height of nodes.
     */
    public DoubleProperty getNodeHeightProperty() {
        return nodeHeightProperty;
    }

    /**
     * The property of node widths.
     * <p>
     * Node width determines how wide a single width unit in the FAFOSP algorithm.
     *
     * @return property which decides the width of nodes.
     */
    public DoubleProperty getNodeWidthProperty() {
        return nodeWidthProperty;
    }

    /**
     * The property which determines whether to display the border between bands as black bands.
     *
     * @return property which decides whether to display the border between bands.
     */
    public BooleanProperty getDisplayBordersProperty() {
        return displayLaneBordersProperty;
    }

    /**
     * The property which determines how long the onscreen dashes should be.
     *
     * @return property which determines the dash length.
     */
    public DoubleProperty getBorderDashLengthProperty() {
        return borderDashLengthProperty;
    }

    /**
     * Set {@link Canvas} which the {@link GraphVisualizer} can draw on.
     *
     * @param canvas canvas to be used to {@link GraphVisualizer}
     */
    public void setCanvas(final Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();

        canvas.setOnMouseClicked(event -> {
            final int[] positions = toNodeCoordinates(event.getX(), event.getY());
            final int nodeXPos = positions[0];
            final int nodeLane = positions[1];

//            if (graph != null) {
//                final Node node = graph.getNode(nodeXPos, nodeLane);
//                if (selectedNodeProperty != null && node != null) {
//                    selectedNodeProperty.set(node);
//                }
//            }
        });
    }

    /**
     * Bind the height of the canvas to a given {@link ReadOnlyDoubleProperty}.
     * <p>
     * This property should be the {@link ReadOnlyDoubleProperty} height property of the pane in which this canvas
     * resides.
     *
     * @param heightProperty {@link ReadOnlyDoubleProperty} to which the height property of the {@link Canvas} should be
     *                       bound
     * @throws IllegalStateException if the canvas has not yet been set.
     */
    public void bindCanvasHeight(final ReadOnlyDoubleProperty heightProperty) {
        if (canvas == null) {
            throw new IllegalStateException("Can't bind height of canvas if canvas not set.");
        }
        canvas.heightProperty().bind(heightProperty);
    }

    /**
     * Redraw the most recently set {@link Graph}. If this is null, canvas is only cleared.
     */
    public void redraw() {
        draw(this.graph);
    }

    /**
     * Populate the graphs primitives with the given graph.
     * <p>
     * First clears the graph before drawing. If {@link Graph} is null, only clears the canvas.
     *
     * @param graph {@link Graph} to populate canvas with.
     * @throws IllegalStateException if the {@link Canvas} has not been set.
     */
    public void draw(final @Nullable Graph graph) {
        if (canvas == null || graphicsContext == null) {
            throw new IllegalStateException("Attempting to draw whilst canvas not set.");
        }

        clear();
        this.graph = graph;
        if (graph != null && canvas != null) {
//            final double canvasWidth = graph.getSinkNode().getHorizontalRightEnd() * nodeWidthProperty.get();
//            canvas.setWidth(canvasWidth);
//
//            // TODO get actual laneCount from FAFOSP (as soon as fixed)
//            final int laneCount = 12;
//            laneHeightProperty.set(canvas.getHeight() / laneCount);
//
//            graph.iterator(n -> !n.isVisited()).forEachRemaining(n -> n.setVisited(false));
//
//            graph.iterator(SequenceNode::isVisited).forEachRemaining(node -> {
//                drawNode(node, DEFAULT_NODE_COLOR);
//                drawEdges(node, edgeColorProperty.get());
//
//                node.setVisited(true);
//            });
//
//            if (displayLaneBordersProperty.get()) {
//                drawBandEdges(laneCount, laneHeightProperty.get());
//            }
        }
    }
}
