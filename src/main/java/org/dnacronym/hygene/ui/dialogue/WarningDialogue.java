package org.dnacronym.hygene.ui.dialogue;

import javafx.scene.control.Alert;


/**
 * Warning dialogue.
 * <p>
 * Displayed when the user did something they shouldn't do.
 */
public final class WarningDialogue extends Dialogue {
    /**
     * Construct a new {@link WarningDialogue}.
     *
     * @param message message to display in the {@link WarningDialogue}
     */
    public WarningDialogue(final String message) {
        super("Warning", message, Alert.AlertType.WARNING);
    }
}
