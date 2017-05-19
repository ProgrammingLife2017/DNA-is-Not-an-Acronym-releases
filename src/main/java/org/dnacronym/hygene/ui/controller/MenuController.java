package org.dnacronym.hygene.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dnacronym.hygene.ui.console.ConsoleWrapper;
import org.dnacronym.hygene.ui.runnable.Hygene;
import org.dnacronym.hygene.ui.runnable.UIInitialisationException;
import org.dnacronym.hygene.ui.store.GraphStore;
import org.dnacronym.hygene.ui.store.RecentFiles;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


/**
 * Controller for the menu bar of the application. Handles user interaction with the menu.
 */
public final class MenuController implements Initializable {
    private static final Logger LOGGER = LogManager.getLogger(MenuController.class);

    private @MonotonicNonNull FileChooser fileChooser;
    private @MonotonicNonNull GraphStore graphStore;

    private File parentDirectory;

    @FXML
    private @MonotonicNonNull Menu recentFilesMenu;

    private @MonotonicNonNull ConsoleWrapper consoleWrapper;


    /**
     * Create an instance of a {@link MenuController} and set the directory for use by {@link FileChooser}.
     */
    public MenuController() {
        super();
        parentDirectory = new File(System.getProperty("user.home"));
    }


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            setGraphStore(Hygene.getInstance().getGraphStore());

            populateRecentFilesMenu();
            initFileChooser();
        } catch (final UIInitialisationException e) {
            LOGGER.error("Failed to initialize MenuController.", e);
        }
    }

    /**
     * Opens a {@link FileChooser} and sets the parent {@link javafx.stage.Window} as
     * {@link Hygene#getPrimaryStage()#getOwner()}.
     *
     * @param event {@link ActionEvent} associated with the event
     * @throws IOException               if unable to open or parse the file
     * @throws UIInitialisationException if this method was called before {@link Hygene} was instantiated
     * @see GraphStore#load(File)
     */
    @FXML
    void openFileAction(final ActionEvent event) throws IOException, UIInitialisationException {
        if (fileChooser == null || graphStore == null) {
            return;
        }

        final Stage primaryStage = Hygene.getInstance().getPrimaryStage();

        if (parentDirectory != null) {
            fileChooser.setInitialDirectory(parentDirectory);
        }
        final File gfaFile = fileChooser.showOpenDialog(primaryStage.getOwner());

        if (gfaFile != null) {
            loadFile(gfaFile);
            parentDirectory = Optional.ofNullable(gfaFile.getParentFile()).orElse(parentDirectory);
        }
    }

    /**
     * Opens the an independent stage showing the current.
     *
     * @param event {@link ActionEvent} associated with the event
     * @throws IOException if unable to located the FXML resource
     */
    @FXML
    void openConsoleAction(final ActionEvent event) throws IOException {
        try {
            if (consoleWrapper == null) {
                consoleWrapper = new ConsoleWrapper();
                LOGGER.info("Launched GUI console window");
            }

            consoleWrapper.bringToFront();
        } catch (final UIInitialisationException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Initializes the file chooser dialog.
     */
    void initFileChooser() {
        final String chooserTitle = "Open GFA File";
        final FileChooser.ExtensionFilter gfaFilter =
                new FileChooser.ExtensionFilter(
                        "GFA (*." + GraphStore.GFA_EXTENSION + ")",
                        "*." + GraphStore.GFA_EXTENSION);

        final FileChooser chooser = new FileChooser();
        chooser.setTitle(chooserTitle);
        chooser.getExtensionFilters().add(gfaFilter);

        setFileChooser(chooser);
    }

    /**
     * Populates the sub-menu of recent files with items representing those files.
     *
     * @throws UIInitialisationException if initialisation of the UI fails
     */
    void populateRecentFilesMenu() throws UIInitialisationException {
        if (recentFilesMenu == null) {
            throw new UIInitialisationException("Recent files menu instance not initialised by framework.");
        }

        final List<File> recentFiles;
        try {
            recentFiles = RecentFiles.getAll();
        } catch (final IOException e) {
            throw new UIInitialisationException("Error while reading recent files from data file.", e);
        }

        if (!recentFiles.isEmpty() && recentFilesMenu.getItems() != null) {
            // Remove default information item (telling the user that there are no recent files)
            recentFilesMenu.getItems().clear();

            MenuItem menuItem;
            for (final File file : recentFiles) {
                menuItem = new MenuItem(file.getPath());
                recentFilesMenu.getItems().add(menuItem);

                menuItem.addEventHandler(ActionEvent.ACTION, event -> {
                    try {
                        loadFile(file);
                    } catch (final IOException | UIInitialisationException e) {
                        LOGGER.error("Failed to load the selected recent file.", e);
                    }
                });
            }
        }
    }

    /**
     * Sets the {@link GraphStore} in the controller.
     *
     * @param graphStore the {@link GraphStore}
     */
    void setGraphStore(final GraphStore graphStore) {
        this.graphStore = graphStore;
    }

    /**
     * Returns the {@link FileChooser} used by the menu.
     *
     * @return the {@link FileChooser}
     */
    @Nullable FileChooser getFileChooser() {
        return fileChooser;
    }

    /**
     * Sets the {@link FileChooser} used by the menu.
     *
     * @param fileChooser the {@link FileChooser}
     */
    void setFileChooser(final FileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    /**
     * Returns the {@link ConsoleWrapper} attached to this menu.
     *
     * @return the {@link ConsoleWrapper}
     */
    public @Nullable ConsoleWrapper getConsoleWrapper() {
        return consoleWrapper;
    }

    /**
     * Sets the recentFilesMenu.
     *
     * @param recentFilesMenu the menu to be set
     */
    void setRecentFilesMenu(final Menu recentFilesMenu) {
        this.recentFilesMenu = recentFilesMenu;
    }

    /**
     * Loads the given file and updates recent files history accordingly.
     *
     * @param file the file to be opened
     * @throws IOException               if an IO error occurrs during loading or saving
     * @throws UIInitialisationException if initialisation of the UI fails
     */
    private void loadFile(final File file) throws IOException, UIInitialisationException {
        if (graphStore == null) {
            throw new UIInitialisationException("Unable to load file.");
        }

        graphStore.load(file);
        RecentFiles.add(file);

        // Update menu only in initialized state (not in test-cases)
        if (recentFilesMenu != null) {
            populateRecentFilesMenu();
        }
    }
}
