package org.dnacronym.hygene.ui.drawing;

import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;


/**
 * Toolkit used to draw segments.
 */
public final class SegmentDrawingToolkit extends NodeDrawingToolkit {
    /**
     * Fills a round rectangle based on the node position and width, with the set {@link Color} fill.
     *
     * @param segmentX     the top left x position of the node
     * @param segmentY     the top left y position of the node
     * @param segmentWidth the width of the node
     * @param color     the {@link Color} to fill the node with
     */
    @Override
    public void draw(final double segmentX, final double segmentY, final double segmentWidth, final Color color) {
        drawGenomes(segmentX, segmentY, segmentWidth, Collections.singletonList(color));
    }

    /**
     * Fills a round rectangle based on the node position and width.
     * <p>
     * The genome colors are spread evenly across the height of the node. They are drawn as lanes along the node.
     *
     * @param segmentX        the top left x position of the node
     * @param segmentY        the top left y position of the node
     * @param segmentWidth    the width of the node
     * @param genomeColors the colors of the paths going through the node
     */
    @Override
    public void drawGenomes(final double segmentX, final double segmentY, final double segmentWidth,
                            final List<Color> genomeColors) {
        double laneHeightOffset = segmentY;
        final double laneHeight = getNodeHeight() / genomeColors.size();

        for (final Color color : genomeColors) {
            getGraphicsContext().setFill(color);
            getGraphicsContext().fillRoundRect(segmentX, laneHeightOffset, segmentWidth, laneHeight, ARC_SIZE,
                    ARC_SIZE);

            laneHeightOffset += laneHeight;
        }
    }

    /**
     * Draws annotations below a node.
     * <p>
     * Annotations have the given colors, and are dashed.
     *
     * @param segmentX            the top left x position of the node
     * @param segmentY            the top left y position of the node
     * @param segmentWidth        the width of the node
     * @param annotationColors the colors of the annotations going through the node
     */
    @Override
    public void drawAnnotations(final double segmentX, final double segmentY, final double segmentWidth,
                                final List<Color> annotationColors) {
        getGraphicsContext().setLineDashes(ANNOTATION_DASH_LENGTH);
        getGraphicsContext().setLineWidth(getAnnotationHeight());

        double annotationYOffset = segmentY + getNodeHeight() + getAnnotationHeight() + getAnnotationHeight() / 2;
        for (final Color color : annotationColors) {
            getGraphicsContext().setStroke(color);
            getGraphicsContext().strokeLine(segmentX, annotationYOffset, segmentX + segmentWidth, annotationYOffset);

            annotationYOffset += getAnnotationHeight();
        }

        getGraphicsContext().setLineDashes(ANNOTATION_DASH_DEFAULT);
    }

    /**
     * Draw a highlight band around a given node of width {@value NODE_OUTLINE_WIDTH}.
     *
     * @param segmentX         the top left x position of the node
     * @param segmentY         the top left y position of the node
     * @param segmentWidth     the width of the node
     * @param highlightType the type of highlight
     */
    @Override
    public void drawHighlight(final double segmentX, final double segmentY, final double segmentWidth,
                              final HighlightType highlightType) {
        getGraphicsContext().setStroke(highlightType.getColor());
        getGraphicsContext().setLineWidth(NODE_OUTLINE_WIDTH);
        getGraphicsContext().strokeRoundRect(
                segmentX - NODE_OUTLINE_WIDTH / 2.0,
                segmentY - NODE_OUTLINE_WIDTH / 2.0,
                segmentWidth + NODE_OUTLINE_WIDTH,
                getNodeHeight() + NODE_OUTLINE_WIDTH,
                ARC_SIZE, ARC_SIZE);

        if (highlightType == HighlightType.BOOKMARKED) {
            drawBookmarkIndicator(segmentX, segmentWidth);
        }
    }

    /**
     * Draw a sequence as black text inside a node.
     *
     * @param segmentX     the top left x position of the node
     * @param segmentY     the top left y position of the node
     * @param segmentWidth the width of the node
     * @param sequence  the sequence of the node
     */
    @Override
    public void drawSequence(final double segmentX, final double segmentY, final double segmentWidth,
                             final String sequence) {
        getGraphicsContext().setFill(Color.BLACK);
        getGraphicsContext().setFont(getNodeFont());
        final int charCount = (int) Math.max((segmentWidth - ARC_SIZE) / getCharWidth(), 0);

        final double fontX = segmentX + (segmentWidth + (ARC_SIZE / 4.0) - charCount * getCharWidth()) / 2;
        final double fontY = segmentY + getNodeHeight() / 2 + getCharHeight() / 2;

        getGraphicsContext().fillText(sequence.substring(0, Math.min(sequence.length(), charCount)), fontX, fontY);
    }
}