package org.dnacronym.hygene.ui.controller.leftpane;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.ui.runnable.Hygene;
import org.dnacronym.hygene.ui.runnable.UIInitialisationException;
import org.dnacronym.hygene.ui.store.GraphStore;

import java.net.URL;
import java.util.ResourceBundle;


/**
 * Controller for the left pane.
 */
public final class LeftPaneController implements Initializable {
    private static final Logger LOGGER = LogManager.getLogger(LeftPaneController.class);

    @FXML
    private SplitPane leftPane;


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final GraphStore graphStore;
        try {
            graphStore = Hygene.getInstance().getGraphStore();
        } catch (final UIInitialisationException e) {
            LOGGER.error("Unable to initialize LeftPaneController.", e);
            return;
        }

        leftPane.managedProperty().bind(Bindings.isNotNull(graphStore.getGfaFileProperty()));
        leftPane.visibleProperty().bind(Bindings.isNotNull(graphStore.getGfaFileProperty()));
    }
}