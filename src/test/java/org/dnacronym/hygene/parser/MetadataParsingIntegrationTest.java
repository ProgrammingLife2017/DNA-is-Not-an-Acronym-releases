package org.dnacronym.hygene.parser;

import org.dnacronym.hygene.models.Edge;
import org.dnacronym.hygene.models.EdgeMetadata;
import org.dnacronym.hygene.models.Graph;
import org.dnacronym.hygene.models.Node;
import org.dnacronym.hygene.models.NodeMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the integration between {@link MetadataParser}, {@link GfaFile}, {@link Graph}, {@link Node} and {@link Edge}.
 */
final class MetadataParsingIntegrationTest {
    @Test
    void testGetNodeMetadata() throws ParseException {
        NewGfaFile gfaFile = new NewGfaFile("src/test/resources/gfa/simple.gfa");
        gfaFile.parse();
        System.out.println(Arrays.toString(gfaFile.getGraph().getNodeArray(0)));
        System.out.println(Arrays.toString(gfaFile.getGraph().getNodeArray(1)));
        Node node = gfaFile.getGraph().getNode(0);

        System.out.println(node.getLineNumber());
        NodeMetadata nodeMetadata = node.retrieveMetadata();

        assertThat(nodeMetadata.getSequence()).isEqualTo("ACCTT");
        assertThat(nodeMetadata.getOriginalNodeId()).isEqualTo("11");
    }

    @Test
    void testGetEdgeMetadata() throws ParseException {
        NewGfaFile gfaFile = new NewGfaFile("src/test/resources/gfa/simple.gfa");
        gfaFile.parse();
        Edge edge = gfaFile.getGraph().getNode(0).getOutgoingEdges().iterator().next();

        EdgeMetadata edgeMetadata = edge.retrieveMetadata();

        assertThat(edgeMetadata.getFromOrient()).isEqualTo("+");
        assertThat(edgeMetadata.getToOrient()).isEqualTo("-");
        assertThat(edgeMetadata.getOverlap()).isEqualTo("4M");
    }
}
