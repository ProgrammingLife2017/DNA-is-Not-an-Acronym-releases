package org.dnacronym.hygene.graph;

import java.util.Set;


/**
 * Class representing a dummy node, to be used for graph layout.
 * <p>
 * This is a 'dummy node', used to create space in complex graph scenarios. As a consequence, it is not intended to be
 * drawn and serves as a connector of {@link DummyEdge}s. One or more of these {@link DummyNode}s forms a 'diversion',
 * which is a path starting from one non-dummy node, followed by one or more {@link DummyNode}s (connected by
 * {@link DummyEdge}s). The diversion reaches its end when it connects to the first non-dummy node again.
 * <p>
 * An example of such a diversion is illustrated below:
 * <p>
 * <pre>
 * A ----- B
 * *       *
 * X * Y * Z
 * </pre>
 * <i>
 * A and B are {@link Segment}s and the connection between them is an {@link Link}. The diversion in this scenario
 * consists of the 'stars' (representing {@link DummyEdge}s) and the {@link DummyNode}s they connect (X, Y, and Z).
 * </i>
 */
@SuppressWarnings("squid:S2160") // Superclass equals/hashCode use UUID, which is unique enough
public final class DummyNode extends NewNode {
    private final NewNode diversionSource;
    private final NewNode diversionDestination;


    /**
     * Constructs a new {@link DummyNode} instance.
     *
     * @param incomingEdges        the incoming edges
     * @param outgoingEdges        the outgoing edges
     * @param diversionSource      the original source node this dummy node is (indirectly) connected to
     * @param diversionDestination the original destination node this dummy node is (indirectly) connected to
     */
    public DummyNode(final Set<Edge> incomingEdges, final Set<Edge> outgoingEdges,
                     final NewNode diversionSource, final NewNode diversionDestination) {
        super(incomingEdges, outgoingEdges);
        this.diversionSource = diversionSource;
        this.diversionDestination = diversionDestination;
    }


    /**
     * Returns the diversion source node.
     *
     * @return the diversion source node
     */
    public NewNode getDiversionSource() {
        return diversionSource;
    }

    /**
     * Returns the diversion destination node.
     *
     * @return the diversion destination node
     */
    public NewNode getDiversionDestination() {
        return diversionDestination;
    }

    @Override
    public int getLength() {
        return 0;
    }
}