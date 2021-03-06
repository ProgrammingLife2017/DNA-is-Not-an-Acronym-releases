package org.dnacronym.hygene.ui.drawing;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.dnacronym.hygene.ui.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Unit tests for {@link NodeDrawingToolkit}.
 */
final class SegmentDrawingToolkitTest extends UITestBase {
    private NodeDrawingToolkit segmentDrawingToolkit;
    private GraphicsContext graphicsContext;


    @Override
    public void beforeEach() {
        segmentDrawingToolkit = new SegmentDrawingToolkit();

        graphicsContext = mock(GraphicsContext.class);
        segmentDrawingToolkit.setGraphicsContext(graphicsContext);
    }


    @Test
    void testNodeHeight() {
        segmentDrawingToolkit.setNodeHeight(10);
        segmentDrawingToolkit.draw(0, 0, 10, Color.BLACK, "");

        verify(graphicsContext).fillRoundRect(
                anyDouble(), anyDouble(), anyDouble(), eq(10.0), anyDouble(), anyDouble());
    }

    @Test
    void testNodeColorDraw() {
        segmentDrawingToolkit.draw(10, 20, 30, Color.ALICEBLUE, "");

        verify(graphicsContext).setFill(eq(Color.ALICEBLUE));
        verify(graphicsContext).fillRoundRect(
                eq(10d), eq(20d), eq(30d), eq(0d), eq(10d), eq(10d));
    }

    @Test
    void testNodePaths() {
        segmentDrawingToolkit.setNodeHeight(10);
        segmentDrawingToolkit.drawGenomes(20, 30, 40, Arrays.asList(Color.BLUE, Color.RED));

        verify(graphicsContext, atLeast(1)).setFill(eq(Color.BLUE));
        verify(graphicsContext, atLeast(1)).setFill(eq(Color.RED));

        verify(graphicsContext).fillRoundRect(
                eq(20d), eq(30d), eq(40d), eq(5d), eq(10d), eq(10d));
        verify(graphicsContext).fillRoundRect(
                eq(20d), eq(35d), eq(40d), eq(5d), eq(10d), eq(10d));
    }

    @Test
    void testHighlightSelectedNodeDraw() {
        segmentDrawingToolkit.drawHighlight(10, 20, 30, HighlightType.SELECTED);

        verify(graphicsContext, atLeast(1)).setStroke(Color.rgb(0, 255, 46));

        verify(graphicsContext).strokeRoundRect(10 - 3 / 2.0, 20 - 3 / 2.0, 30 + 3, 3, 10d, 10d);
    }

    @Test
    void testHighlightBookmarkedNodeDraw() {
        segmentDrawingToolkit.drawHighlight(10, 20, 30, HighlightType.BOOKMARKED);

        verify(graphicsContext, atLeast(1)).setStroke(Color.RED);
        verify(graphicsContext).strokeRoundRect(10 - 3 / 2.0, 20 - 3 / 2.0, 30 + 3, 3, 10d, 10d);
    }

    @Test
    void testDrawText() {
        final String text = "test text";

        segmentDrawingToolkit.drawSequence(0, 0, 100, text);

        verify(graphicsContext).fillText(eq(text), anyDouble(), anyDouble());
    }

    @Test
    void testDrawTextTrimmed() {
        final String text = "test text";

        segmentDrawingToolkit.drawSequence(0, 0, 0, text);

        verify(graphicsContext, times(0)).fillText(anyString(), anyDouble(), anyDouble());
    }
}
