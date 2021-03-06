package org.dnacronym.hygene.parser;

import com.google.common.collect.ImmutableMap;
import org.dnacronym.hygene.graph.metadata.EdgeMetadata;
import org.dnacronym.hygene.graph.metadata.NodeMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


final class MetadataParserTest {
    private MetadataParser parser;
    private RandomAccessFile randomAccessFile;


    @BeforeEach
    void beforeEach() {
        parser = new MetadataParser();
    }


    @Test
    void testParseNodeMetadata() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("%n%nS 12 TCAAGG * ORI:Z:test.fasta;");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("S 12 TCAAGG * ORI:Z:test.fasta;"));

        final NodeMetadata nodeMetadata = parser.parseNodeMetadata(gfaFile, 0);

        assertThat(nodeMetadata.getName()).isEqualTo("12");
        assertThat(nodeMetadata.getSequence()).isEqualTo("TCAAGG");
        assertThat(nodeMetadata.getGenomes()).contains("test.fasta");
    }

    @Test
    void testParseNodeMetadataOfMultipleNodes() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("%n%nS 12 TCAAGG * ORI:Z:test.fasta;"
                + "%n%n%nS 12 TAG * ORI:Z:test.fasta;"
                + "%nS 12 CAT * ORI:Z:test.fasta;"
                + "%nS 12 SANITYCHECK * ORI:Z:test.fasta;");
        when(randomAccessFile.readLine()).thenReturn(
                replaceSpacesWithTabs("S 12 TCAAGG * ORI:Z:test.fasta;"),
                replaceSpacesWithTabs("S 12 TAG * ORI:Z:test.fasta;"),
                replaceSpacesWithTabs("S 12 CAT * ORI:Z:test.fasta;")
        );

        final Map<Integer, NodeMetadata> nodesMetadata = parser.parseNodeMetadata(
                gfaFile,
                ImmutableMap.of(1, 3L, 2, 6L, 3, 7L)
        );

        assertThat(nodesMetadata.get(1).getSequence()).isEqualTo("TCAAGG");
        assertThat(nodesMetadata.get(2).getSequence()).isEqualTo("TAG");
        assertThat(nodesMetadata.get(3).getSequence()).isEqualTo("CAT");
    }

    @Test
    void testParseEdgeMetadata() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("L 12 + 24 - 4M");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("L 12 + 24 - 4M"));
        final EdgeMetadata edgeMetadata = parser.parseEdgeMetadata(gfaFile, 2);

        assertThat(edgeMetadata.getFromOrient()).isEqualTo("+");
        assertThat(edgeMetadata.getToOrient()).isEqualTo("-");
        assertThat(edgeMetadata.getOverlap()).isEqualTo("4M");
    }

    @Test
    void testParseNodeMetadataWithInvalidLineBecauseTheSequenceIsMissing() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("S 12");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("S 12"));

        final Throwable e = catchThrowable(() -> parser.parseNodeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
        assertThat(e).hasMessageContaining("Not enough parameters for segment at position 1");
    }

    @Test
    void testParseNodeMetadataWithInvalidLineBecauseTheGenomeIsMissing() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("S 12 AC *");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("S 12 AC *"));

        Throwable e = catchThrowable(() -> parser.parseNodeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
    }

    @Test
    void testParseNodeMetadataWithInvalidLineBecauseTheGenomePrefixIsIncorrect()
            throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("S 12 AC * ORY:Z:test.fasta;");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("S 12 AC * ORY:Z:test.fasta;"));

        Throwable e = catchThrowable(() -> parser.parseNodeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
    }

    @Test
    void testParseEdgeMetadataWithInvalidLineBecauseTheOrientIsMissing() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("L 12 + 24");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("L 12 + 24"));

        final Throwable e = catchThrowable(() -> parser.parseEdgeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
        assertThat(e).hasMessageContaining("Not enough parameters for link at position 1");
    }

    @Test
    void testParseNodeMetadataWithAnEdgeLine() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("L 12 + 24 - 4M");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("L 12 + 24 - 4M"));

        final Throwable e = catchThrowable(() -> parser.parseNodeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
        assertThat(e).hasMessageContaining("Expected line at position 1 to start with S");
    }

    @Test
    void testParseEdgeMetadataWithANodeLine() throws MetadataParseException, IOException {
        final GfaFile gfaFile = createGfaFile("S 12 ACTG");
        when(randomAccessFile.readLine()).thenReturn(replaceSpacesWithTabs("S 12 ACTG"));

        final Throwable e = catchThrowable(() -> parser.parseEdgeMetadata(gfaFile, 1));

        assertThat(e).isInstanceOf(MetadataParseException.class);
        assertThat(e).hasMessageContaining("Expected line at position 1 to start with L");
    }


    private GfaFile createGfaFile(final String gfa) throws MetadataParseException {
        final byte[] gfaBytes = replaceSpacesWithTabs(gfa).getBytes(StandardCharsets.UTF_8);
        final GfaFile gfaFile = mock(GfaFile.class);
        try {
            when(gfaFile.readFile()).thenAnswer(invocationOnMock ->
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(gfaBytes)))
            );
        } catch (final GfaParseException e) {
            e.printStackTrace();
        }
        randomAccessFile = mock(RandomAccessFile.class);
        when(gfaFile.getRandomAccessFile()).thenReturn(randomAccessFile);
        return gfaFile;
    }

    private String replaceSpacesWithTabs(final String string) {
        return string.replaceAll(" ", "\t");
    }
}
