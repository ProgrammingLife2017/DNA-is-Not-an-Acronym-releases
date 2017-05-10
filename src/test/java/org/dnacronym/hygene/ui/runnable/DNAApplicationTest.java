package org.dnacronym.hygene.ui.runnable;

import org.dnacronym.hygene.ui.UITest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for the {@link DNAApplication} instance.
 */
public class DNAApplicationTest extends UITest {
    @Test
    public final void testGetApplication() throws UIInitialisationException {
        assertThat(DNAApplication.getInstance()).isEqualTo(getApplication());
    }

    @Test
    public final void testGetPrimaryStage() throws UIInitialisationException {
        assertThat(DNAApplication.getInstance().getPrimaryStage()).isEqualTo(getPrimaryStage());
    }

    @Test
    public final void testGetGraphStore() throws UIInitialisationException {
        assertThat(DNAApplication.getInstance().getGraphStore()).isNotNull();
    }
}