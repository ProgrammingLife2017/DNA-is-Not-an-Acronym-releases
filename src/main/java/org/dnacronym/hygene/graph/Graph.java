package org.dnacronym.hygene.graph;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.dnacronym.hygene.core.UnsignedInteger;
import org.dnacronym.hygene.graph.layout.FafospLayerer;
import org.dnacronym.hygene.parser.GfaFile;

import java.util.Map;
import java.util.TreeMap;


/**
 * Class wraps around the graph data represented as a nested array and provides utility methods.
 * <p>
 * Node array format:
 * [[nodeByteOffset, sequenceLength, outgoingEdges, xPosition, edge1, edge1ByteOffset...]]
 */
public final class Graph {
    public static final int NODE_BYTE_OFFSET_INDEX = 0;
    public static final int NODE_SEQUENCE_LENGTH_INDEX = 1;
    public static final int UNSCALED_X_POSITION_INDEX = 2;
    public static final int NODE_OUTGOING_EDGES_INDEX = 3;
    public static final int NODE_EDGE_DATA_OFFSET = 4;
    public static final int EDGE_BYTE_OFFSET_OFFSET = 1;
    public static final int EDGE_DATA_SIZE = 2;
    static final int MINIMUM_SEQUENCE_LENGTH = 500;

    private final int[][] nodeArrays;
    private final GfaFile gfaFile;
    @SuppressWarnings("PMD.LooseCoupling")
    private @MonotonicNonNull TreeMap<Long, Integer> nodePositions;


    /**
     * Constructs a graph from array based data structure.
     *
     * @param nodeArrays nested array containing the graph's data
     * @param gfaFile    a reference to the GFA file from which the graph is created
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "For performance reasons, we don't want to create a copy here"
    )
    @SuppressWarnings("PMD.ArrayIsStoredDirectly") // Performance
    public Graph(final int[][] nodeArrays, final GfaFile gfaFile) {
        this.nodeArrays = nodeArrays;
        this.gfaFile = gfaFile;
    }


    /**
     * Creates an empty node array without edge details used to initialize a new node.
     *
     * @return an empty node array
     */
    public static int[] createEmptyNodeArray() {
        return new int[] {0, 0, -1, 0};
    }


    /**
     * Getter for the array representing a {@link Node}'s metadata.
     *
     * @param id the {@link Node}'s id
     * @return the array representing a {@link Node}'s metadata
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray") // Performance
    public int[] getNodeArray(final int id) {
        return nodeArrays[id];
    }

    /**
     * Gets the array representation of all node arrays.
     *
     * @return the array representation of all node arrays
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "For performance reasons, we don't want to create a copy here"
    )
    @SuppressWarnings("PMD.MethodReturnsInternalArray") // Performance
    public int[][] getNodeArrays() {
        return nodeArrays;
    }

    /**
     * Getter for the byte offset where the {@link Node}'s metadata resides.
     *
     * @param id the {@link Node}'s id
     * @return the {@link Node}'s byte offset
     */
    public long getByteOffset(final int id) {
        return UnsignedInteger.toLong(nodeArrays[id][NODE_BYTE_OFFSET_INDEX]);
    }

    /**
     * Getter for the length of a {@link Node} (capped at a lower bound).
     *
     * @param id the {@link Node}'s id
     * @return the {@link Node}'s (capped) sequence length
     */
    public int getLength(final int id) {
        if (nodeArrays[id][NODE_SEQUENCE_LENGTH_INDEX] < MINIMUM_SEQUENCE_LENGTH) {
            return MINIMUM_SEQUENCE_LENGTH;
        }
        return nodeArrays[id][NODE_SEQUENCE_LENGTH_INDEX];
    }

    /**
     * Getter for the raw sequence length (number of base pairs) of a {@link Node}.
     *
     * @param id the {@link Node}'s id
     * @return the {@link Node}'s sequence length
     */
    public int getSequenceLength(final int id) {
        return nodeArrays[id][NODE_SEQUENCE_LENGTH_INDEX];
    }

    /**
     * Getter for the unscaled x position.
     *
     * @param id the {@link Node}'s id
     * @return the unscaled x position
     */
    public int getUnscaledXPosition(final int id) {
        return nodeArrays[id][UNSCALED_X_POSITION_INDEX];
    }

    public long getRealStartXPosition(final int id) {
        return (long) FafospLayerer.LAYER_WIDTH * getUnscaledXPosition(id);
    }

    public long getRealEndXPosition(final int id) {
        return getRealStartXPosition(id) + getLength(id);
    }

    /**
     * Sets the unscaled x position.
     *
     * @param id                the {@link Node}'s id
     * @param unscaledXPosition the unscaled x position
     */
    public void setUnscaledXPosition(final int id, final int unscaledXPosition) {
        nodeArrays[id][UNSCALED_X_POSITION_INDEX] = unscaledXPosition;
    }

    /**
     * Returns the number of neighbours of a node in the given direction.
     *
     * @param id        the node's identifier
     * @param direction the direction of neighbours to count
     * @return the number of neighbours of a node in the given direction
     */
    public int getNeighbourCount(final int id, final SequenceDirection direction) {
        return direction.ternary(
                (nodeArrays[id].length
                        - nodeArrays[id][NODE_OUTGOING_EDGES_INDEX] * EDGE_DATA_SIZE
                        - (NODE_OUTGOING_EDGES_INDEX + 1)
                ) / EDGE_DATA_SIZE,
                nodeArrays[id][NODE_OUTGOING_EDGES_INDEX]
        );
    }

    @SuppressWarnings({"PMD.LooseCoupling", "squid:S1319"}) // I need a TreeMap
    public void setNodePositions(final TreeMap<Long, Integer> nodePositions) {
        this.nodePositions = nodePositions;
    }

    public int getNodeAtPosition(final long position) {
        if (nodePositions == null) {
            throw new IllegalStateException("Cannot give node position while TreeMap was not set.");
        }

        Map.Entry<Long, Integer> myEntry = nodePositions.floorEntry(position);
        if (myEntry == null) {
            myEntry = nodePositions.ceilingEntry(position);
            if (myEntry == null) {
                throw new IllegalStateException("Could not find that kind of node.");
            }
        }

        return myEntry.getValue();
    }

    /**
     * Getter for the {@link GfaFile} instance where the graph belongs to.
     *
     * @return the {@link GfaFile} instance where the graph belongs to
     */
    public GfaFile getGfaFile() {
        return gfaFile;
    }
}
