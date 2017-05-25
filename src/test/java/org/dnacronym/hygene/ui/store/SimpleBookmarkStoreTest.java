package org.dnacronym.hygene.ui.store;

import javafx.beans.property.IntegerProperty;
import org.dnacronym.hygene.models.Bookmark;
import org.dnacronym.hygene.parser.ParseException;
import org.dnacronym.hygene.ui.visualizer.GraphVisualizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit tests for {@link SimpleBookmarkStore}s.
 */
final class SimpleBookmarkStoreTest {
    private SimpleBookmarkStore simpleBookmarkStore;
    private IntegerProperty centerNodeIdProperty;

    private Bookmark bookmark;
    private GraphVisualizer graphVisualizer;


    @BeforeEach
    void beforeEach() throws ParseException {
        bookmark = new Bookmark(0, 1, 32, "1234");

        final GraphStore graphStore = new GraphStore();

        graphVisualizer = mock(GraphVisualizer.class);
        centerNodeIdProperty = mock(IntegerProperty.class);
        when(graphVisualizer.getCenterNodeIdProperty()).thenReturn(centerNodeIdProperty);

        simpleBookmarkStore = new SimpleBookmarkStore(graphStore, graphVisualizer);
    }


    @Test
    void testGetBookmarks() {
        simpleBookmarkStore.addBookmark(bookmark);

        final SimpleBookmark simpleBookmark = simpleBookmarkStore.getBookmarks().get(0);

        assertThat(simpleBookmark.getBookmark()).isEqualTo(bookmark);
    }

    @Test
    void testSetBookmarks() throws ParseException {
        final Bookmark bookmark2 = new Bookmark(1, 20, 20, "asdf");
        simpleBookmarkStore.addBookmarks(bookmark, bookmark2);

        assertThat(simpleBookmarkStore.getBookmarks()).hasSize(2);
        assertThat(simpleBookmarkStore.getBookmarks().get(0)).isNotEqualTo(simpleBookmarkStore.getBookmarks().get(1));
    }

    @Test
    void testSetOnClick() {
        simpleBookmarkStore.addBookmark(bookmark);

        final SimpleBookmark simpleBookmark = simpleBookmarkStore.getBookmarks().get(0);
        simpleBookmark.getOnClick().run();

        verify(graphVisualizer, times(1)).setSelectedNode(0);
        verify(centerNodeIdProperty, times(1)).set(0);
    }
}
