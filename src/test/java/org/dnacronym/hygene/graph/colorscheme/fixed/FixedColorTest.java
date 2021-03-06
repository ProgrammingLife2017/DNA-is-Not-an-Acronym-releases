package org.dnacronym.hygene.graph.colorscheme.fixed;

import javafx.scene.paint.Color;
import org.dnacronym.hygene.graph.node.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


/**
 * Unit tests for the {@link FixedColorScheme}.
 */
final class FixedColorTest {
    private FixedColorScheme fixedColor;


    @BeforeEach
    void beforeEach() {
        fixedColor = new FixedColorScheme(Color.ALICEBLUE);
    }


    @Test
    void testSetFixedColor() {
        assertThat(fixedColor.calculateColor(mock(Node.class))).isEqualTo(Color.ALICEBLUE);
    }
}
