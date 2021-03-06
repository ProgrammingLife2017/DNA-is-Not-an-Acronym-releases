package org.dnacronym.hygene.graph;

import org.dnacronym.hygene.graph.edge.Link;
import org.dnacronym.hygene.graph.node.FillNode;
import org.dnacronym.hygene.graph.node.Node;
import org.dnacronym.hygene.graph.node.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link Subgraph}.
 */
final class SubgraphTest {
    private static final Random RANDOM = new Random();

    private Subgraph subgraph;


    @BeforeEach
    void setUp() {
        subgraph = new Subgraph();
    }


    @Test
    void testGetNodeEmpty() {
        assertThat(subgraph.getNode(UUID.randomUUID())).isEmpty();
    }

    @Test
    void testGetNodeNull() {
        final Node node = new FillNode();

        subgraph.add(node);

        assertThat(subgraph.getNode(UUID.randomUUID())).isEmpty();
    }

    @Test
    void testGetNodeEquals() {
        final Node node = new FillNode();

        subgraph.add(node);

        assertThat(subgraph.getNode(node.getUuid())).contains(node);
    }

    @Test
    void testGetNodesEmpty() {
        assertThat(subgraph.getNodes()).isEmpty();
    }

    @Test
    void testGetSegmentEmpty() {
        assertThat(subgraph.getSegment(36)).isEmpty();
    }

    @Test
    void testGetSegmentNull() {
        final Segment segment = new Segment(95, 42, 80);

        subgraph.add(segment);

        assertThat(subgraph.getSegment(86)).isEmpty();
    }

    @Test
    void testGetSegmentEquals() {
        final Segment segment = new Segment(37, 60, 44);

        subgraph.add(segment);

        assertThat(subgraph.getSegment(37)).contains(segment);
    }

    @Test
    void testGetSegmentsEmpty() {
        assertThat(subgraph.getGfaNodes()).isEmpty();
    }

    @Test
    void testAddNodeAndContains() {
        final Node node = new FillNode();

        subgraph.add(node);

        assertThat(subgraph.contains(node.getUuid())).isTrue();
    }

    @Test
    void testAddNodeAndContainsNode() {
        final Node node = new FillNode();

        subgraph.add(node);

        assertThat(subgraph.containsNode(node)).isTrue();
    }

    @Test
    void testAddSegmentAndContains() {
        final Segment segment = new Segment(98, 36, 35);

        subgraph.add(segment);

        assertThat(subgraph.contains(segment.getUuid())).isTrue();
    }

    @Test
    void testAddSegmentAndContainsNode() {
        final Segment segment = new Segment(98, 36, 35);

        subgraph.add(segment);

        assertThat(subgraph.containsNode(segment)).isTrue();
    }

    @Test
    void testAddSegmentAndContainsSegment() {
        final Segment segment = new Segment(58, 48, 49);

        subgraph.add(segment);

        assertThat(subgraph.containsSegment(58)).isTrue();
    }

    @Test
    void testAddNode() {
        final Node node = new FillNode();
        subgraph.add(node);
        assertThat(subgraph.getNodes()).containsExactly(node);
    }

    @Test
    void testRemoveNode() {
        final Node node = new FillNode();
        subgraph.add(node);

        subgraph.remove(node);

        assertThat(subgraph.getNodes()).isEmpty();
    }

    @Test
    void testRemoveAllNodes() {
        final Node nodeA = new FillNode();
        final Node nodeB = new FillNode();
        final Node nodeC = new FillNode();
        final Node nodeD = new FillNode();
        subgraph.add(nodeA);
        subgraph.add(nodeB);
        subgraph.add(nodeC);
        subgraph.add(nodeD);

        subgraph.removeAll(Arrays.asList(nodeA, nodeB, nodeD));

        assertThat(subgraph.getNodes()).containsExactly(nodeC);
    }

    @Test
    void testRemoveSegment() {
        final Segment segmentA = new Segment(95, 51, 93);
        final Segment segmentB = new Segment(98, 1, 94);
        subgraph.add(segmentA);
        subgraph.add(segmentB);

        subgraph.remove(segmentA);

        assertThat(subgraph.getNodes()).containsExactly(segmentB);
    }

    @Test
    void testRemoveAllSegments() {
        final Segment segmentA = new Segment(89, 30, 81);
        final Segment segmentB = new Segment(2, 61, 16);
        final Segment segmentC = new Segment(64, 86, 44);
        final Segment segmentD = new Segment(93, 87, 10);
        subgraph.add(segmentA);
        subgraph.add(segmentB);
        subgraph.add(segmentC);

        subgraph.removeAll(Arrays.asList(segmentB, segmentD));

        assertThat(subgraph.getNodes()).containsExactly(segmentA, segmentC);
    }

    @Test
    void testGetNeighbours() {
        final Segment segment1 = new Segment(100, 83, 88);
        final Segment segment2 = new Segment(42, 67, 22);
        final Segment segment3 = new Segment(87, 72, 47);

        connectSegments(segment1, segment2);
        connectSegments(segment1, segment3);

        subgraph.addAll(Arrays.asList(segment1, segment2, segment3));

        assertThat(subgraph.getNeighbours(segment1, SequenceDirection.RIGHT))
                .containsExactlyInAnyOrder(segment2, segment3);
    }

    @Test
    void testGetNodesBFS() {
        final Segment segment1 = new Segment(1, 63, 19);
        final Segment segment2 = new Segment(2, 90, 32);
        final Segment segment3 = new Segment(3, 98, 14);
        final Segment segment4 = new Segment(4, 55, 10);

        connectSegments(segment1, segment2);
        connectSegments(segment1, segment3);
        connectSegments(segment2, segment4);
        connectSegments(segment3, segment4);

        subgraph.addAll(Arrays.asList(segment1, segment2, segment3, segment4));

        final List<Node> nodes = new ArrayList<>(subgraph.getNodesBFS(SequenceDirection.RIGHT));

        assertThat(nodes).doesNotHaveDuplicates();
        assertThat(nodes.indexOf(segment1)).isLessThan(nodes.indexOf(segment2));
        assertThat(nodes.indexOf(segment1)).isLessThan(nodes.indexOf(segment3));
        assertThat(nodes.indexOf(segment2)).isLessThan(nodes.indexOf(segment4));
        assertThat(nodes.indexOf(segment3)).isLessThan(nodes.indexOf(segment4));
    }


    /**
     * Connects the two segments with a {@link Link}.
     *
     * @param leftSegment  the left segment
     * @param rightSegment the right segment
     */
    private static void connectSegments(final Segment leftSegment, final Segment rightSegment) {
        final Link link = new Link(leftSegment, rightSegment, RANDOM.nextInt());
        leftSegment.getOutgoingEdges().add(link);
        rightSegment.getIncomingEdges().add(link);
    }
}
