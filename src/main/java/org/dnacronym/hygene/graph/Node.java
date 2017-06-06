package org.dnacronym.hygene.graph;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dnacronym.hygene.models.NodeColor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


/**
 * Class representing a generic node.
 */
public abstract class Node {
    private final UUID uuid;
    private final Set<Edge> incomingEdges;
    private final Set<Edge> outgoingEdges;
    private int xPosition;
    private int yPosition;


    /**
     * Constructs a new {@link Node} instance without any edges.
     * <p>
     * This class should not be instantiated for regular use, please use {@link Segment} instead.
     */
    protected Node() {
        this(new HashSet<>(), new HashSet<>());
    }

    /**
     * Constructs a new {@link Node} instance.
     * <p>
     * This class should not be instantiated for regular use, please use {@link Segment} instead.
     *
     * @param incomingEdges the incoming edges
     * @param outgoingEdges the outgoing edges
     */
    protected Node(final Set<Edge> incomingEdges, final Set<Edge> outgoingEdges) {
        this.uuid = UUID.randomUUID();
        this.incomingEdges = incomingEdges;
        this.outgoingEdges = outgoingEdges;
    }


    /**
     * Returns this {@link Node}'s {@link UUID}.
     *
     * @return this {@link Node}'s {@link UUID}
     */
    public final UUID getUuid() {
        return uuid;
    }

    /**
     * Returns the X position.
     *
     * @return the X position
     */
    public final int getXPosition() {
        return xPosition;
    }

    /**
     * Sets the X position.
     *
     * @param xPosition the X position
     */
    public final void setXPosition(final int xPosition) {
        this.xPosition = xPosition;
    }

    /**
     * Returns the Y position.
     *
     * @return the Y position
     */
    public final int getYPosition() {
        return yPosition;
    }

    /**
     * Sets the Y position.
     *
     * @param yPosition the Y position
     */
    public final void setYPosition(final int yPosition) {
        this.yPosition = yPosition;
    }

    /**
     * Returns the incoming edges.
     *
     * @return the incoming edges
     */
    public final Set<Edge> getIncomingEdges() {
        return incomingEdges;
    }

    /**
     * Returns the outgoing edges.
     *
     * @return the outgoing edges
     */
    public final Set<Edge> getOutgoingEdges() {
        return outgoingEdges;
    }

    /**
     * Returns the length of the node when visualized.
     *
     * @return the length of the node when visualized
     */
    public abstract int getLength();

    //
    public final NodeColor getColor() {
        return NodeColor.BLACK;
    }

    @Override
    public final boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Node node = (Node) o;
        return Objects.equals(uuid, node.uuid);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(uuid);
    }
}
