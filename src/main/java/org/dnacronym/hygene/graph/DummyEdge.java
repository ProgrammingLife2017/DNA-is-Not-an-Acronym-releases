package org.dnacronym.hygene.graph;


/**
 * Class representing a dummy edge, to be used for graph layout.
 */
public final class DummyEdge extends Edge {
    private final Edge originalEdge;


    /**
     * Constructs a new {@link DummyEdge} instance.
     *
     * @param from         the source of the edge
     * @param to           the destination of the edge
     * @param originalEdge the original edge this dummy edge is replacing (cannot be a {@link DummyEdge})
     */
    public DummyEdge(final Node from, final Node to, final Edge originalEdge) {
        super(from, to);

        assert !(originalEdge instanceof DummyEdge);
        this.originalEdge = originalEdge;
    }


    /**
     * Returns the original edge.
     *
     * @return the original edge
     */
    public Edge getOriginalEdge() {
        return originalEdge;
    }
}
