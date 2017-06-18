package org.dnacronym.hygene.ui.settings;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;


/**
 * Settings controller for more advanced features.
 */
public final class AdvancedSettingsViewController extends SettingsController {
    private static final Logger LOGGER = LogManager.getLogger(AdvancedSettingsViewController.class);

    @FXML
    private CheckBox displayLaneBorders;


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        displayLaneBorders.setSelected(getGraphVisualizer().getDisplayBordersProperty().get());
    }

    /**
     * When user interacts with the show lane borders {@link CheckBox}.
     *
     * @param actionEvent the {@link ActionEvent}
     */
    @FXML
    void showLaneBordersClicked(final ActionEvent actionEvent) {
        getSettings().addRunnable(() -> {
            final boolean newValue = ((CheckBox) actionEvent.getSource()).isSelected();
            getGraphVisualizer().getDisplayBordersProperty().setValue(newValue);
            LOGGER.info("Displaying lane borders has now been " + (newValue ? "enabled." : "disabled."));
        });
    }
}
