package org.dnacronym.hygene.models.colorscheme.minmax;

import javafx.scene.paint.Color;
import org.dnacronym.hygene.graph.NewNode;


/**
 * The color is determined by the number of incoming and outgoing edges of a node combined.
 */
public final class ColorSchemeTotalEdges extends AbstractColorSchemeMinMax {
    /**
     * Creates an instance of {@link ColorSchemeTotalEdges}.
     *
     * @param maxValue the max value before max {@link Color} is given
     * @param minColor the minimum color of the color scheme
     * @param maxColor the maximum color of the color scheme
     */
    public ColorSchemeTotalEdges(final int maxValue, final Color minColor, final Color maxColor) {
        super(maxValue, minColor, maxColor);
    }


    @Override
    public Color calculateColor(final NewNode node) {
        return calculateColor((double) node.getIncomingEdges().size() + node.getOutgoingEdges().size());
    }
}