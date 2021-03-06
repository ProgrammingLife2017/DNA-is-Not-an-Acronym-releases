package org.dnacronym.hygene.graph.edge;

import org.dnacronym.hygene.graph.node.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


/**
 * Test suite for the {@link Edge} class.
 */
abstract class EdgeTest {
    private Edge edge;
    private Node from;
    private Node to;
    private Set<String> genomes;


    @BeforeEach
    void setUp() {
        from = mock(Node.class);
        to = mock(Node.class);
        genomes = new HashSet<>(Arrays.asList("a", "b", "c"));
    }


    @Test
    final void testGetFrom() {
        assertThat(edge.getFrom()).isEqualTo(from);
    }

    @Test
    final void testGetTo() {
        assertThat(edge.getTo()).isEqualTo(to);
    }

    @Test
    void testGetSetGenomes() {
        edge.setGenomes(genomes);
        assertThat(edge.getGenomes()).isEqualTo(genomes);
    }

    @Test
    void testGetImportance() {
        edge.setGenomes(genomes);
        assertThat(edge.getImportance()).isEqualTo(genomes.size());
    }

    @Test
    void testGetImportantNullCase() {
        edge.setGenomes(null);
        assertThat(edge.getImportance()).isEqualTo(1);
    }

    @Test
    void testGetImportantEmptyCase() {
        edge.setGenomes(new HashSet<>());
        assertThat(edge.getImportance()).isEqualTo(1);
    }

    @Test
    void testGetInGenome() {
        edge.setGenomes(genomes);
        assertThat(edge.inGenome("a")).isTrue();
    }

    @Test
    void testGetInGenomeNullCase() {
        edge.setGenomes(null);
        assertThat(edge.inGenome("a")).isFalse();
    }

    @Test
    void testToString() {
        final String string = edge.toString();

        assertThat(string).contains(edge.getFrom().toString());
        assertThat(string).contains(edge.getTo().toString());
    }


    /**
     * Returns the source node.
     *
     * @return the source node
     */
    final Node getFrom() {
        return from;
    }

    /**
     * Sets the source node.
     *
     * @param from the source node
     */
    final void setFrom(final Node from) {
        this.from = from;
    }

    /**
     * Returns the destination node.
     *
     * @return the destination node
     */
    final Node getTo() {
        return to;
    }

    /**
     * Sets the destination node.
     *
     * @param to the destination node
     */
    final void setTo(final Node to) {
        this.to = to;
    }

    /**
     * Sets the {@link Edge} instance to be tested.
     *
     * @param edge the {@link Edge} instance
     */
    final void setEdge(final Edge edge) {
        this.edge = edge;
    }
}
