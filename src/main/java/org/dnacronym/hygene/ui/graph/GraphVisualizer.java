package org.dnacronym.hygene.ui.graph;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.core.HygeneEventBus;
import org.dnacronym.hygene.event.SnapshotButtonWasPressed;
import org.dnacronym.hygene.graph.Graph;
import org.dnacronym.hygene.graph.annotation.Annotation;
import org.dnacronym.hygene.graph.edge.Edge;
import org.dnacronym.hygene.graph.node.AggregateSegment;
import org.dnacronym.hygene.graph.node.GfaNode;
import org.dnacronym.hygene.graph.node.Node;
import org.dnacronym.hygene.graph.node.Segment;
import org.dnacronym.hygene.ui.bookmark.BookmarkStore;
import org.dnacronym.hygene.ui.drawing.EdgeDrawingToolkit;
import org.dnacronym.hygene.ui.drawing.HighlightType;
import org.dnacronym.hygene.ui.drawing.NodeDrawingToolkit;
import org.dnacronym.hygene.ui.drawing.SegmentDrawingToolkit;
import org.dnacronym.hygene.ui.drawing.SnpDrawingToolkit;
import org.dnacronym.hygene.ui.path.GenomePath;
import org.dnacronym.hygene.ui.query.Query;
import org.dnacronym.hygene.ui.settings.BasicSettingsViewController;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A simple canvas that allows drawing of primitive shapes.
 * <p>
 * It observes the {@link Graph} in {@link GraphDimensionsCalculator}. When the list of nodes queried nodes changes
 * in {@link GraphDimensionsCalculator}, then it will clear the {@link Canvas} and draw the new {@link Segment}s on the
 * {@link Canvas} using a {@link GraphicsContext}.
 *
 * @see Canvas
 * @see GraphicsContext
 * @see GraphDimensionsCalculator
 */
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.GodClass", "PMD.TooManyFields", "PMD.TooManyMethods",
        "PMD.CyclomaticComplexity"})
// This will be fixed at a later date.
public final class GraphVisualizer {
    private static final Logger LOGGER = LogManager.getLogger(GraphVisualizer.class);

    private static final double DEFAULT_NODE_HEIGHT = 20;
    private static final double DEFAULT_DASH_LENGTH = 10;

    private static final double DEFAULT_EDGE_THICKNESS = 3;
    private static final Color DEFAULT_EDGE_COLOR = Color.GREY;
    private static final int EDGE_OPACITY_OFFSET = 20;
    private static final double EDGE_OPACITY_ALPHA = 1.5;
    private static final double EDGE_OPACITY_BETA = 4.25;

    private static final int MAX_PATH_THICKNESS_DRAWING_RADIUS = 150;
    private static final int MAX_SEQUENCE_DRAWING_RADIUS = 150;

    private final GraphAnnotation graphAnnotation;
    private final GraphDimensionsCalculator graphDimensionsCalculator;
    private final Query query;

    private final ColorRoulette colorRoulette;

    private final ObjectProperty<GfaNode> selectedSegmentProperty;
    private final ObjectProperty<GfaNode> hoveredSegmentProperty;

    private final ObservableList<GenomePath> genomePaths;
    private final ObservableMap<String, Color> selectedGenomePaths;

    private final ObjectProperty<Color> edgeColorProperty;

    private final DoubleProperty nodeHeightProperty;

    private final BooleanProperty displayLaneBordersProperty;

    private Graph graph;

    private Canvas canvas;
    private GraphicsContext graphicsContext;
    private final SegmentDrawingToolkit segmentDrawingToolkit;
    private final SnpDrawingToolkit snpDrawingToolkit;
    private final EdgeDrawingToolkit edgeDrawingToolkit;
    private final GraphAnnotationVisualizer graphAnnotationVisualizer;
    private final BookmarkStore bookmarkStore;

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
     * @param query                     the {@link Query} used to get the currently queried nodes
     * @param bookmarkStore             the {@link BookmarkStore} used to draw bookmark indications
     * @param graphAnnotation           the {@link GraphAnnotation} used to draw annotations
     */
    @Inject
    public GraphVisualizer(final GraphDimensionsCalculator graphDimensionsCalculator, final Query query,
                           final BookmarkStore bookmarkStore, final GraphAnnotation graphAnnotation) {
        HygeneEventBus.getInstance().register(this);
        this.graphDimensionsCalculator = graphDimensionsCalculator;
        this.query = query;
        this.bookmarkStore = bookmarkStore;
        this.colorRoulette = new ColorRoulette();
        this.graphAnnotation = graphAnnotation;

        selectedSegmentProperty = new SimpleObjectProperty<>();

        hoveredSegmentProperty = new SimpleObjectProperty<>();
        hoveredSegmentProperty.addListener((observable, oldValue, newValue) -> draw());

        genomePaths = FXCollections.observableArrayList(new HashSet<>());
        selectedGenomePaths = FXCollections.observableHashMap();
        selectedGenomePaths.addListener((MapChangeListener<String, Color>) change -> draw());

        edgeColorProperty = new SimpleObjectProperty<>(DEFAULT_EDGE_COLOR);
        nodeHeightProperty = new SimpleDoubleProperty(DEFAULT_NODE_HEIGHT);
        graphDimensionsCalculator.getNodeHeightProperty().bind(nodeHeightProperty);

        edgeColorProperty.addListener((observable, oldValue, newValue) -> draw());
        nodeHeightProperty.addListener((observable, oldValue, newValue) -> draw());

        Node.setColorScheme(BasicSettingsViewController.NODE_COLOR_SCHEMES.get(0).getValue());

        displayLaneBordersProperty = new SimpleBooleanProperty();
        displayLaneBordersProperty.addListener((observable, oldValue, newValue) -> draw());

        graphDimensionsCalculator.getGraphProperty()
                .addListener((observable, oldValue, newValue) -> setGraph(newValue));

        graphDimensionsCalculator.getObservableQueryNodes()
                .addListener((ListChangeListener<Node>) change -> draw());

        query.getQueriedNodes().addListener((ListChangeListener<Integer>) observable -> draw());

        segmentDrawingToolkit = new SegmentDrawingToolkit();
        snpDrawingToolkit = new SnpDrawingToolkit();
        edgeDrawingToolkit = new EdgeDrawingToolkit();
        graphAnnotationVisualizer = new GraphAnnotationVisualizer(graphDimensionsCalculator);
    }


    /**
     * Draw a node on the canvas.
     * <p>
     * Node outlines are also drawn when one of the following conditions are met:
     * If selected, it is {@link HighlightType#SELECTED}.<br>
     * If it is not selected, and highlighted, it is {@link HighlightType#HIGHLIGHTED}.<br>
     * If it is not highlighted, and queried, it is {@link HighlightType#QUERIED}.<br>
     * If it is not queried, and bookmarked, is it {@link HighlightType#BOOKMARKED}.
     * <p>
     * The node is afterwards added to the {@link RTree}.
     *
     * @param node        the node to draw
     * @param bookmarked  the boolean indicating whether this node is bookmarked
     * @param queried     the boolean indicating whether this node has been queried
     * @param annotations the list of annotations in view
     */
    @SuppressWarnings("PMD.NPathComplexity") // See comment at top of class
    private void drawNode(final Node node, final boolean bookmarked, final boolean queried,
                          final List<Annotation> annotations) {
        if (!(node instanceof GfaNode)) {
            return;
        }

        final double nodeX = graphDimensionsCalculator.computeXPosition(node);
        final double nodeWidth = graphDimensionsCalculator.computeWidth(node);

        if (nodeX + nodeWidth < 0 || nodeX > canvas.getWidth()) {
            return;
        }

        final NodeDrawingToolkit nodeDrawingToolkit = createNodeDrawingToolkit(node);
        if (nodeDrawingToolkit == null) {
            return;
        }

        final GfaNode gfaNode = (GfaNode) node;
        final double nodeY = graphDimensionsCalculator.computeYPosition(node);
        nodeDrawingToolkit.draw(nodeX, nodeY, nodeWidth, node.getColor());

        if (selectedSegmentProperty.isNotNull().get() && gfaNode.getSegmentIds().stream()
                .anyMatch(segmentId -> selectedSegmentProperty.get().containsSegment(segmentId))) {
            nodeDrawingToolkit.drawHighlight(nodeX, nodeY, nodeWidth, HighlightType.SELECTED);
        }
        if (hoveredSegmentProperty.isNotNull().get() && gfaNode.getSegmentIds().stream()
                .anyMatch(segmentId -> hoveredSegmentProperty.get().containsSegment(segmentId))) {
            nodeDrawingToolkit.drawHighlight(nodeX, nodeY, nodeWidth, HighlightType.HIGHLIGHTED);
        }
        if (queried) {
            nodeDrawingToolkit.drawHighlight(nodeX, nodeY, nodeWidth, HighlightType.QUERIED);
        }
        if (bookmarked) {
            nodeDrawingToolkit.drawHighlight(nodeX, nodeY, nodeWidth, HighlightType.BOOKMARKED);
        }

        if (gfaNode.hasMetadata()
                && graphDimensionsCalculator.getRadiusProperty().get() < MAX_SEQUENCE_DRAWING_RADIUS) {
            final String sequence = gfaNode.getMetadata().getSequence();
            nodeDrawingToolkit.drawSequence(nodeX, nodeY, nodeWidth, sequence);
        }

        gfaNode.getSegments().forEach(segment -> {
            nodeDrawingToolkit.drawAnnotations(nodeX, nodeY, nodeWidth, segmentAnnotationColors(segment, annotations));
            rTree.addNode(segment.getId(), nodeX, nodeY, nodeWidth, nodeHeightProperty.get());
        });
    }

    /**
     * Creates the correct toolkit for the given node.
     *
     * @param node the node
     * @return the correct toolkit for the given node
     */
    private NodeDrawingToolkit createNodeDrawingToolkit(final Node node) {
        if (node instanceof Segment) {
            return segmentDrawingToolkit;
        } else if (node instanceof AggregateSegment) {
            return snpDrawingToolkit;
        } else {
            LOGGER.warn("Cannot draw node of class " + node.getClass().getName() + ".");
            return null;
        }
    }

    /**
     * Returns all the colors of all the annotations going through this branch.
     *
     * @param segment     the {@link Segment} to get the annotation colors for
     * @param annotations the list of annotations in the current view
     * @return the list of colors of the annotations going through the given {@link Segment}
     */
    private List<Color> segmentAnnotationColors(final Segment segment, final List<Annotation> annotations) {
        final List<Color> annotationColors = new ArrayList<>();
        for (final Annotation annotation : annotations) {
            if (segment.hasMetadata()
                    && segment.getMetadata().getGenomes().contains(graphAnnotation.getMappedGenome())
                    && segment.getId() >= annotation.getStartNodeId()
                    && segment.getId() < annotation.getEndNodeId()) {
                annotationColors.add(annotation.getColor());
            }
        }

        return annotationColors;
    }

    /**
     * Draws an edge on the canvas.
     * <p>
     * The edge is afterwards added to the {@link RTree}.
     *
     * @param edge        the edge to be drawn
     * @param annotations the list of annotations in view
     */
    private void drawEdge(final Edge edge, final List<Annotation> annotations) {
        final Node fromNode = edge.getFrom();
        final Node toNode = edge.getTo();

        final double fromX = graphDimensionsCalculator.computeRightXPosition(fromNode);
        final double fromY = graphDimensionsCalculator.computeMiddleYPosition(fromNode);
        final double toX = graphDimensionsCalculator.computeXPosition(toNode);
        final double toY = graphDimensionsCalculator.computeMiddleYPosition(toNode);

        final double edgeThickness = computeEdgeThickness(edge);
        edgeDrawingToolkit.drawEdge(fromX, fromY, toX, toY, edgeThickness, computeEdgeColors(edge));
        edgeDrawingToolkit.drawEdgeAnnotations(
                fromX, fromY, toX, toY, edgeThickness, edgeAnnotationColors(edge, annotations));
    }

    /**
     * Computes the {@link Color} of the {@link Edge}.
     *
     * @param edge the {@link Edge}
     * @return list of {@link Edge} colors
     */
    private List<Color> computeEdgeColors(final Edge edge) {
        final List<Color> edgeColors;

        if (edge.getFromSegment().equals(hoveredSegmentProperty.get())
                || edge.getToSegment().equals(hoveredSegmentProperty.get())) {
            edgeColors = Collections.singletonList(HighlightType.HIGHLIGHTED.getColor());
        } else if (edge.getGenomes() != null
                && graphDimensionsCalculator.getRadiusProperty().get() < MAX_PATH_THICKNESS_DRAWING_RADIUS) {
            final Set<String> selectedGenomesInEdge
                    = Sets.intersection(edge.getGenomes(), selectedGenomePaths.keySet());

            if (selectedGenomesInEdge.isEmpty()) {
                edgeColors = Collections.singletonList(getEdgeColor());
            } else {
                edgeColors = selectedGenomesInEdge.stream()
                        .map(path -> correctColorForEdgeOpacity(selectedGenomePaths.get(path)))
                        .collect(Collectors.toList());
            }
        } else {
            edgeColors = Collections.singletonList(getEdgeColor());
        }

        return edgeColors;
    }

    /**
     * Returns the list of colors going through a given {@link Edge}.
     *
     * @param edge        the {@link Edge} to get the annotation colors for
     * @param annotations the list of onscreen annotations
     * @return the list of colors of annotations going through the given {@link Edge}
     */
    private List<Color> edgeAnnotationColors(final Edge edge, final List<Annotation> annotations) {
        final List<Color> annotationColors = new ArrayList<>();

        for (final Annotation annotation : annotations) {
            if (edgePartOfAnnotation(edge, annotation)) {
                annotationColors.add(annotation.getColor());
            }
        }
        return annotationColors;
    }


    /**
     * Checks if the given {@link Edge} is part of the given {@link Annotation}.
     *
     * @param edge       the {@link Edge} to check
     * @param annotation the {@link Annotation} to check
     * @return true iff the two nodes of the edge are within bounds of the annotation and have the same genome as the
     * annotation
     */
    @SuppressWarnings("squid:S1067") // fixing this will require a re-write of the Edge class
    private boolean edgePartOfAnnotation(final Edge edge, final Annotation annotation) {
        return edge.getFromSegment().hasMetadata() && edge.getToSegment().hasMetadata()
                && edge.getFromSegment().getMetadata().getGenomes().contains(graphAnnotation.getMappedGenome())
                && edge.getToSegment().getMetadata().getGenomes().contains(graphAnnotation.getMappedGenome())
                && edge.getFromSegment() instanceof Segment
                && ((Segment) edge.getFromSegment()).getId() >= annotation.getStartNodeId()
                && edge.getToSegment() instanceof Segment
                && ((Segment) edge.getToSegment()).getId() < annotation.getEndNodeId();
    }

    /**
     * Computes the thickness of an edge based on the {@link Edge} importance.
     * <p>
     * The thickness is computed as the number unique genomes that run through this path divided by the total
     * number of unique paths.
     *
     * @param edge the edge
     * @return the edge thickness
     */
    @SuppressWarnings("MagicNumber")
    public double computeEdgeThickness(final Edge edge) {
        final int numberOfGenomes = graph.getGfaFile().getGenomeMapping().size();

        return Math.max(DEFAULT_EDGE_THICKNESS,
                0.5 * ((double) edge.getImportance()) / numberOfGenomes * nodeHeightProperty.get());
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
     * Correct a {@link Color} for the current edge opacity.
     *
     * @param color the {@link Color}
     * @return the {@link Color} corrected for edge opacity
     */
    private Color correctColorForEdgeOpacity(final Color color) {
        return color.deriveColor(1, 1, 1, computeEdgeOpacity());
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
        segmentDrawingToolkit.setNodeHeight(nodeHeightProperty.get());
        segmentDrawingToolkit.setCanvasHeight(canvas.getHeight());
        snpDrawingToolkit.setNodeHeight(nodeHeightProperty.get());
        snpDrawingToolkit.setCanvasHeight(canvas.getHeight());
        graphAnnotationVisualizer.setCanvasWidth(canvas.getWidth());

        final int[] minNodeId = {Integer.MAX_VALUE};
        final int[] maxNodeId = {0};

        for (final Node node : graphDimensionsCalculator.getObservableQueryNodes()) {
            if (!(node instanceof GfaNode)) {
                continue;
            }
            ((GfaNode) node).getSegmentIds().forEach(nodeId -> {
                minNodeId[0] = Math.min(minNodeId[0], nodeId);
                maxNodeId[0] = Math.max(maxNodeId[0], nodeId);
            });
        }
        maxNodeId[0] = Math.max(maxNodeId[0], minNodeId[0]);

        final List<Annotation> observableAnnotations = graphAnnotation.getAnnotationsInRange(minNodeId[0],
                maxNodeId[0]);

        // Edges should be drawn before nodes, don't combine this with node drawing loop
        for (final Node node : graphDimensionsCalculator.getObservableQueryNodes()) {
            node.getOutgoingEdges().forEach(edge -> drawEdge(edge, observableAnnotations));
        }

        for (final Node node : graphDimensionsCalculator.getObservableQueryNodes()) {
            final boolean bookmarked = bookmarkStore != null
                    && (bookmarkStore.containsBookmark(node)
                    || node instanceof GfaNode && bookmarkStore.getSimpleBookmarks().stream().anyMatch(
                    simpleBookmark -> ((GfaNode) node).containsSegment(simpleBookmark.getNodeIdProperty().get()))
            );
            drawNode(node,
                    bookmarked,
                    node instanceof Segment && query.getQueriedNodes().contains(((Segment) node).getId()),
                    observableAnnotations);
        }

        if (displayLaneBordersProperty.get()) {
            drawLaneBorders(
                    graphDimensionsCalculator.getLaneCountProperty().get(),
                    graphDimensionsCalculator.getLaneHeightProperty().get());
        }
    }

    /**
     * Listens for {@link SnapshotButtonWasPressed} events.
     *
     * @param event the {@link SnapshotButtonWasPressed} event
     */
    @Subscribe
    public void onScreenshotButtonWasPressed(final SnapshotButtonWasPressed event) {
        Snapshot.forGfaFile(graph.getGfaFile()).take(canvas);
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

        genomePaths.clear();
        final List<GenomePath> newGenomePaths = graph.getGfaFile().getGenomeMapping().entrySet().stream()
                .map(entry -> new GenomePath(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        newGenomePaths.forEach(path -> path.selectedProperty()
                .addListener((o, oldIsSelected, newIsSelected) -> {
                    if (newIsSelected) {
                        final Color genomeColor = colorRoulette.getNext();
                        selectedGenomePaths.put(path.getIndex(), genomeColor);
                        selectedGenomePaths.put(path.getName(), genomeColor);
                        path.setColor(genomeColor);
                    } else {
                        selectedGenomePaths.remove(path.getIndex());
                        selectedGenomePaths.remove(path.getName());
                        path.setColor(null);
                    }
                    LOGGER.debug(selectedGenomePaths);
                }));

        genomePaths.addAll(newGenomePaths);

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
        this.segmentDrawingToolkit.setGraphicsContext(graphicsContext);
        this.snpDrawingToolkit.setGraphicsContext(graphicsContext);
        this.edgeDrawingToolkit.setGraphicsContext(graphicsContext);
        this.graphAnnotationVisualizer.setGraphicsContext(graphicsContext);

        canvas.setOnMouseClicked(event -> {
            if (rTree == null) {
                return;
            }

            selectedSegmentProperty.setValue(null);

            rTree.find(event.getX(), event.getY(), this::setSelectedSegment, (ignored1, ignored2) -> {
            });
        });
        canvas.setOnMouseMoved(event -> {
            if (rTree == null) {
                return;
            }
            hoveredSegmentProperty.set(null);
            rTree.find(event.getX(), event.getY(), this::setHoveredSegmentProperty);
        });
        canvas.setOnMouseExited(event -> hoveredSegmentProperty.set(null));

        graphDimensionsCalculator.setCanvasSize(canvas.getWidth(), canvas.getHeight());
        canvas.widthProperty().addListener((observable, oldValue, newValue) ->
                graphDimensionsCalculator.setCanvasSize(newValue.doubleValue(), canvas.getHeight()));
        canvas.heightProperty().addListener((observable, oldValue, newValue) ->
                graphDimensionsCalculator.setCanvasSize(canvas.getWidth(), newValue.doubleValue()));
    }

    /**
     * Updates the selected {@link Segment} to the node with the given id.
     *
     * @param nodeId node the id of the newly selected {@link Segment}
     */
    public void setSelectedSegment(final int nodeId) {
        final FilteredList<Node> segment = graphDimensionsCalculator.getObservableQueryNodes()
                .filtered(node -> node instanceof GfaNode && ((GfaNode) node).containsSegment(nodeId));

        if (segment.isEmpty()) {
            LOGGER.error("Cannot select node that is not in subgraph.");
            return;
        }

        selectedSegmentProperty.set((GfaNode) segment.get(0));
    }

    /**
     * Updates the hovered {@link Segment} to the node with the given id.
     *
     * @param nodeId node the id of the newly hovered {@link Segment}
     */
    public void setHoveredSegmentProperty(final int nodeId) {
        final FilteredList<Node> segment = graphDimensionsCalculator.getObservableQueryNodes()
                .filtered(node -> node instanceof GfaNode && ((GfaNode) node).containsSegment(nodeId));

        if (segment.isEmpty()) {
            LOGGER.error("Cannot select node that is not in subgraph.");
            return;
        }

        hoveredSegmentProperty.set((GfaNode) segment.get(0));

        new NodeTooltip(
                graphicsContext,
                (GfaNode) segment.get(0),
                graphDimensionsCalculator.computeXPosition(segment.get(0))
                        + (graphDimensionsCalculator.computeWidth(segment.get(0)) / 2),
                graphDimensionsCalculator.computeBelowYPosition(segment.get(0))
        ).show();
    }

    /**
     * The property of the selected node.
     * <p>
     * This node is updated every time the user clicks on a node in the canvas.
     *
     * @return the selected {@link Segment} by the user, which can be {@code null}
     */
    public ObjectProperty<GfaNode> getSelectedSegmentProperty() {
        return selectedSegmentProperty;
    }

    /**
     * The property representing the selected paths.
     *
     * @return property representing the selected paths
     */
    public ObservableList<GenomePath> getGenomePathsProperty() {
        return genomePaths;
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
     * Gets the {@link Graph}.
     *
     * @return the graph
     */
    public Graph getGraph() {
        return graph;
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
