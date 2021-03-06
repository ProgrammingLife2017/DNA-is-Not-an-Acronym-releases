package org.dnacronym.hygene.ui.bookmark;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.dnacronym.hygene.ui.genomeindex.GenomeMappingView;
import org.dnacronym.hygene.ui.runnable.Hygene;
import org.dnacronym.hygene.ui.runnable.UIInitialisationException;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;


/**
 * Simple view for creating new bookmarks.
 */
public class BookmarkCreateView {
    private static final String TITLE = "Create a Bookmark";
    private static final String GENOME_MAPPING_VIEW = "/ui/bookmark/bookmark_create_view.fxml";

    private final Stage stage;


    /**
     * Creates an instance of a {@link GenomeMappingView}.
     *
     * @throws IOException if unable to load the controller
     */
    @Inject
    public BookmarkCreateView(final FXMLLoader fxmlLoader) throws UIInitialisationException, IOException {
        stage = new Stage();
        stage.setTitle(TITLE);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        final URL resource = getClass().getResource(GENOME_MAPPING_VIEW);
        fxmlLoader.setLocation(resource);
        final Scene rootScene = new Scene(fxmlLoader.load());

        stage.setScene(rootScene);
    }


    /**
     * Show the stage, which blocks the underlying view.
     *
     * @throws UIInitialisationException if the UI has not been initialized
     */
    public void showAndWait() throws UIInitialisationException {
        if (stage.getOwner() == null) {
            final Stage primaryStage =Hygene.getInstance().getPrimaryStage();

            stage.initOwner(primaryStage);
            stage.getIcons().add(primaryStage.getIcons().get(0));
        }

        stage.showAndWait();
    }
}
