package org.dnacronym.hygene.graph;

import org.dnacronym.hygene.graph.edge.Edge;
import org.dnacronym.hygene.graph.node.GfaNode;
import org.dnacronym.hygene.graph.node.Node;
import org.dnacronym.hygene.graph.node.Segment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Class representing a subgraph.
 */
public final class Subgraph {
    /**
     * A mapping from {@link UUID}s to their respective {@link Node}s.
     */
    private final Map<UUID, Node> nodes;
    /**
     * A mapping from ids to their respective {@link Segment}s.
     */
    private final Map<Integer, GfaNode> segments;


    /**
     * Constructs a new, empty {@link Subgraph} instance.
     */
    public Subgraph() {
        this.segments = Collections.synchronizedMap(new LinkedHashMap<>());
        this.nodes = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    /**
     * Constructs a copy of an existing {@link Subgraph}.
     *
     * @param subgraph an existing {@link Subgraph}
     */
    public Subgraph(final Subgraph subgraph) {
        this.nodes = Collections.synchronizedMap(new LinkedHashMap<>(subgraph.nodes));
        this.segments = Collections.synchronizedMap(new LinkedHashMap<>(subgraph.segments));
    }


    /**
     * Returns the {@link Node} with the given {@link UUID}, or {code null} if no such node exists.
     *
     * @param nodeUuid a {@link UUID}
     * @return the {@link Node} with the given {@link UUID}, or {code null} if no such node exists
     */
    public Optional<Node> getNode(final UUID nodeUuid) {
        return Optional.ofNullable(nodes.get(nodeUuid));
    }

    /**
     * Returns the nodes.
     *
     * @return the nodes
     */
    public Collection<Node> getNodes() {
        return nodes.values();
    }

    /**
     * Returns the {@link Segment} with the given id, or {code null} if no such segment exists.
     *
     * @param segmentId a segment id
     * @return the {@link Segment} with the given id, or {code null} if no such segment exists
     */
    public Optional<Segment> getSegment(final int segmentId) {
        final GfaNode gfaNode = segments.get(segmentId);
        if (gfaNode == null) {
            return Optional.empty();
        }

        return gfaNode.getSegment(segmentId);
    }

    /**
     * Returns all nodes that are {@link Segment}s.
     *
     * @return all nodes that are {@link Segment}s
     */
    public Collection<GfaNode> getGfaNodes() {
        return segments.values();
    }

    /**
     * Returns a {@link Collection} of all the {@link Node}s in this {@link Subgraph} in breadth-first order.
     *
     * @param direction the direction to traverse in
     * @return a {@link Collection} of all the {@link Node}s in this {@link Subgraph} in breadth-first order
     */
    public Collection<Node> getNodesBFS(final SequenceDirection direction) {
        final Queue<Node> queue = new LinkedList<>();
        nodes.values().forEach(node -> {
            if (direction.ternary(isSinkNeighbour(node), isSourceNeighbour(node))) {
                queue.add(node);
            }
        });

        final Set<Node> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            final Node head = queue.remove();
            if (visited.contains(head)) {
                continue;
            }

            visited.add(head);

            getNeighbours(head, direction).forEach(neighbour -> {
                if (!visited.contains(neighbour) && nodes.containsValue(neighbour)) {
                    queue.add(neighbour);
                }
            });
        }

        return visited;
    }

    /**
     * Returns a {@link Set} of the given node's neighbours.
     *
     * @param node      a {@link Node}
     * @param direction the direction of the neighbours
     * @return a {@link Set} of the given node's neighbours.
     */
    public Collection<Node> getNeighbours(final Node node, final SequenceDirection direction) {
        final Predicate<Edge> filter = direction.ternary(
                edge -> nodes.containsValue(edge.getFrom()),
                edge -> nodes.containsValue(edge.getTo()));
        final Function<Edge, Node> mapper = direction.ternary(Edge::getFrom, Edge::getTo);
        return direction.ternary(node.getIncomingEdges(), node.getOutgoingEdges()).stream()
                .filter(filter)
                .map(mapper)
                .collect(Collectors.toSet());
    }

    /**
     * Adds the given node.
     *
     * @param node the node to be added
     */
    public void add(final Node node) {
        idempotentAdd(node);
    }

    /**
     * Adds the given nodes.
     *
     * @param nodes the nodes to be added
     */
    public void addAll(final Collection<? extends Node> nodes) {
        nodes.forEach(this::idempotentAdd);
    }

    /**
     * Adds the given node.
     *
     * @param node a {@link Node}
     */
    public void remove(final Node node) {
        idempotentRemove(node);
    }

    /**
     * Removes the given nodes.
     *
     * @param nodes the nodes to be removed
     */
    public void removeAll(final Collection<? extends Node> nodes) {
        nodes.forEach(this::idempotentRemove);
    }

    /**
     * Returns {@code true} iff. this subgraph contains a {@link Node} with the given {@link UUID}.
     *
     * @param uuid a {@link UUID}
     * @return {@code true} iff. this subgraph contains a {@link Node} with the given {@link UUID}
     */
    public boolean contains(final UUID uuid) {
        return nodes.containsKey(uuid);
    }

    /**
     * Returns {@code true} iff. this subgraph contains the given {@link Node}.
     *
     * @param node a {@link Node}
     * @return {@code true} iff. this subgraph contains the given {@link Node}
     */
    public boolean containsNode(final Node node) {
        return nodes.containsKey(node.getUuid());
    }

    /**
     * Returns {@code true} iff. a {@link Segment} with the given id is present in this subgraph.
     *
     * @param segmentId the id of a {@link Segment}
     * @return {@code true} iff. a {@link Segment} with the given id is present in this subgraph
     */
    public boolean containsSegment(final int segmentId) {
        return segments.containsKey(segmentId);
    }

    /**
     * Clears all nodes from this subgraph.
     */
    public void clear() {
        nodes.clear();
        segments.clear();
    }


    /**
     * Checks whether the given node is a neighbour of the subgraph source.
     *
     * @param node the node to be checked
     * @return {@code true} iff. the node is a neighbour of the subgraph source
     */
    private boolean isSourceNeighbour(final Node node) {
        return getNeighbours(node, SequenceDirection.LEFT).isEmpty();
    }

    /**
     * Checks whether the given node is a neighbour of the subgraph sink.
     *
     * @param node the node to be checked
     * @return {@code true} iff. the node is a neighbour of the subgraph sink
     */
    private boolean isSinkNeighbour(final Node node) {
        return getNeighbours(node, SequenceDirection.RIGHT).isEmpty();
    }

    /**
     * Adds a {@link Node} without any other side effects.
     * <p>
     * Calling this method twice with the same {@link Node} will result in no side effects whatsoever, thus
     * promising idempotence.
     *
     * @param node a {@link Node}
     */
    private void idempotentAdd(final Node node) {
        nodes.put(node.getUuid(), node);

        if (node instanceof GfaNode) {
            ((GfaNode) node).getSegments().forEach(segment -> segments.put(segment.getId(), (GfaNode) node));
        }
    }

    /**
     * Removes a {@link Node} without any other side effects.
     * <p>
     * Calling this method twice with the same {@link Node} will result in no side effects whatsoever, thus promising
     * idempotence.
     *
     * @param node a {@link Node}
     */
    private void idempotentRemove(final Node node) {
        nodes.remove(node.getUuid());

        if (node instanceof GfaNode) {
            ((GfaNode) node).getSegments().forEach(segment -> segments.remove(segment.getId()));
        }
    }
}
