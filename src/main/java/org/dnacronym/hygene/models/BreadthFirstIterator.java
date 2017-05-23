package org.dnacronym.hygene.models;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Function;


/**
 * An iterator that iterates in breadth-first order over {@link SequenceNode}s.
 */
final class BreadthFirstIterator implements Iterator<SequenceNode> {
    private final Queue<SequenceNode> queue;
    private final SequenceDirection direction;
    private final Function<SequenceNode, Boolean> duplicateDetector;


    /**
     * Creates a new {@link BreadthFirstIterator} over the nodes connected to the given node.
     *
     * @param startNode the root {@link SequenceNode}
     * @param direction {@code true} if the iterator should go to the right, or {@code false} if the iterator should go
     *                  to the left
     */
    BreadthFirstIterator(final SequenceNode startNode, final SequenceDirection direction) {
        this(startNode, direction, node -> false);
    }

    /**
     * Creates a new {@link BreadthFirstIterator} with custom duplicate detection over the nodes connected to the
     * given node.
     * <p>
     * The custom {@code duplicateDetector} can be used to indicate that a node has already been visited. This
     * can be used when you change the nodes while iterating, and this change results in a detectable property.
     *
     * @param startNode         the root {@link SequenceNode}
     * @param direction         {@code true} if the iterator should go to the right, or {@code false} if the iterator
     *                          should go to the left
     * @param duplicateDetector a {@link Function} that returns {@code true} iff. the {@link SequenceNode} has been
     *                          visited
     */
    BreadthFirstIterator(final SequenceNode startNode, final SequenceDirection direction,
                         final Function<SequenceNode, Boolean> duplicateDetector) {
        this.direction = direction;
        this.duplicateDetector = duplicateDetector;

        queue = new LinkedList<>();
        queue.add(startNode);
    }


    @Override
    public boolean hasNext() {
        SequenceNode head = queue.peek();
        if (head == null) {
            return false;
        }

        while (duplicateDetector.apply(head)) {
            queue.remove();
            head = queue.peek();
            if (head == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public SequenceNode next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements in the iterator.");
        }

        final SequenceNode head = queue.remove();
        queue.addAll(getNeighbours(head));
        return head;
    }

    /**
     * Returns the neighbours of the node relevant to this iterator.
     *
     * @param node a {@link SequenceNode}
     * @return the neighbours of the node relevant to this iterator
     */
    private List<SequenceNode> getNeighbours(final SequenceNode node) {
        return direction.ternary(node.getLeftNeighbours(), node.getRightNeighbours());
    }
}