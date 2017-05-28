package org.dnacronym.hygene.ui.node;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.models.Node;
import org.dnacronym.hygene.parser.ParseException;
import org.dnacronym.hygene.ui.dialogue.ErrorDialogue;
import org.dnacronym.hygene.ui.graph.GraphDimensionsCalculator;
import org.dnacronym.hygene.ui.graph.GraphVisualizer;
import org.dnacronym.hygene.ui.runnable.Hygene;
import org.dnacronym.hygene.ui.runnable.UIInitialisationException;

import java.net.URL;
import java.util.ResourceBundle;


/**
 * Controller for the node properties window. Shows the properties of the selected node.
 */
public final class NodePropertiesController implements Initializable {
    private static final Logger LOGGER = LogManager.getLogger(NodePropertiesController.class);

    private SequenceVisualizer sequenceVisualizer;
    private GraphDimensionsCalculator graphDimensionsCalculator;
    private GraphVisualizer graphVisualizer;

    @FXML
    private AnchorPane nodePropertiesPane;
    @FXML
    private TextField nodeId;
    @FXML
    private TextField sequence;
    @FXML
    private Canvas neighbourCanvas;
    @FXML
    private TextField leftNeighbours;
    @FXML
    private TextField rightNeighbours;
    @FXML
    private TextField position;
    @FXML
    private Button viewSequence;


    /**
     * Create instance of {@link NodePropertiesController}.
     */
    public NodePropertiesController() {
        try {
            setSequenceVisualizer(Hygene.getInstance().getSequenceVisualizer());
            setGraphVisualiser(Hygene.getInstance().getGraphVisualizer());
            setGraphDimensionsCalculator(Hygene.getInstance().getGraphDimensionsCalculator());
        } catch (final UIInitialisationException e) {
            LOGGER.error("Failed to initialize NodePropertiesController.", e);
            new ErrorDialogue(e).show();
        }
    }


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final ObjectProperty<Node> selectedNodeProperty = graphVisualizer.getSelectedNodeProperty();

        final NeighbourVisualizer neighbourVisualizer
                = new NeighbourVisualizer(graphVisualizer.getEdgeColorProperty(), selectedNodeProperty);
        neighbourVisualizer.setCanvas(neighbourCanvas);

        selectedNodeProperty.addListener((observable, oldNode, newNode) -> {
            if (newNode == null) {
                return;
            }

            nodeId.setText(String.valueOf(newNode.getId()));

            try {
                sequence.setText(newNode.retrieveMetadata().getSequence());
            } catch (final ParseException e) {
                LOGGER.error("Error when parsing a node.", e);
            }

            leftNeighbours.setText(String.valueOf(newNode.getNumberOfIncomingEdges()));
            rightNeighbours.setText(String.valueOf(newNode.getNumberOfOutgoingEdges()));

            position.setText(String.valueOf(newNode.getId()));

        });

        viewSequence.visibleProperty().bind(graphVisualizer.getSelectedNodeProperty().isNotNull());
        viewSequence.managedProperty().bind(graphVisualizer.getSelectedNodeProperty().isNotNull());
        nodePropertiesPane.visibleProperty().bind(graphDimensionsCalculator.getGraphProperty().isNotNull());
        nodePropertiesPane.managedProperty().bind(graphDimensionsCalculator.getGraphProperty().isNotNull());
    }

    /**
     * Sets the {@link SequenceVisualizer} for use by the controller.
     *
     * @param sequenceVisualizer {@link SequenceVisualizer} for use by the controller
     */
    void setSequenceVisualizer(final SequenceVisualizer sequenceVisualizer) {
        this.sequenceVisualizer = sequenceVisualizer;
    }

    /**
     * Set the {@link GraphVisualizer}, whose selected node can be bound to the UI elements in the controller.
     *
     * @param graphVisualizer {@link GraphVisualizer} who's selected node we are interested in
     * @see GraphVisualizer#selectedNodeProperty
     */
    void setGraphVisualiser(final GraphVisualizer graphVisualizer) {
        this.graphVisualizer = graphVisualizer;
    }

    /**
     * Sets the {@link GraphDimensionsCalculator} for use by the controller.
     *
     * @param graphDimensionsCalculator the {@link GraphDimensionsCalculator} for use by the controller
     */
    void setGraphDimensionsCalculator(final GraphDimensionsCalculator graphDimensionsCalculator) {
        this.graphDimensionsCalculator = graphDimensionsCalculator;
    }

    /**
     * When the user clicks on the focus {@link javafx.scene.control.Button}.
     *
     * @param actionEvent the {@link ActionEvent}
     */
    @FXML
    void onFocusAction(final ActionEvent actionEvent) {
        final Node selectedNode = graphVisualizer.getSelectedNodeProperty().get();
        if (selectedNode != null) {
            graphDimensionsCalculator.getCenterNodeIdProperty().set(selectedNode.getId());
        }

        actionEvent.consume();
    }

    /**
     * When the user clicks on the view sequence button.
     *
     * @param actionEvent the {@link ActionEvent}
     */
    @FXML
    void viewSequenceAction(final ActionEvent actionEvent) {
        sequenceVisualizer.getVisibleProperty().set(true);
        actionEvent.consume();
    }
}
