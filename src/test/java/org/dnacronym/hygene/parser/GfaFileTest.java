package org.dnacronym.hygene.parser;

import org.dnacronym.hygene.graph.metadata.EdgeMetadata;
import org.dnacronym.hygene.graph.metadata.NodeMetadata;
import org.dnacronym.hygene.parser.factories.MetadataParserFactory;
import org.dnacronym.hygene.parser.factories.GfaParserFactory;
import org.dnacronym.hygene.persistence.FileDatabaseDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


/**
 * Tests the behavior of the {@link GfaFile} class.
 */
final class GfaFileTest {
    private static final String GFA_TEST_FILE = "src/test/resources/gfa/simple.gfa";
    private static final String SIMPLE_GFA_CONTENTS = String.format("H\tVN:Z:1.0%n"
            + "H\tORI:Z:g1.fasta;g2.fasta;%n"
            + "S\t11\tACCTT\t*\tORI:Z:g1.fasta%n"
            + "S\t12\tTCAAGG\t*\tORI:Z:g2.fasta%n"
            + "L\t11\t+\t12\t-\t4M%n");

    private String currentFileName;


    @AfterEach
    void afterEach() throws IOException {
        GfaParserFactory.setInstance(null);
        MetadataParserFactory.setInstance(null);
        if (currentFileName != null) {
            Files.deleteIfExists(Paths.get(currentFileName + FileDatabaseDriver.DB_FILE_EXTENSION));
            Files.deleteIfExists(Paths.get(currentFileName + ".hygenecache"));
        }
    }

    @AfterAll
    static void resetFactories() {
        GfaParserFactory.setInstance(null);
        MetadataParserFactory.setInstance(null);
    }


    @Test
    void testGfaFileObjectCanBeConstructed() {
        currentFileName = "name_of_the_file.gfa";
        final GfaFile gfaFile = new GfaFile(currentFileName);

        assertThat(gfaFile.getFileName()).isEqualTo(currentFileName);
    }

    @Test
    void testCannotReadNonExistingFile() {
        currentFileName = "random-file-name";
        final Throwable e = catchThrowable(() -> new GfaFile(currentFileName).parse(ProgressUpdater.DUMMY));

        assertThat(e).isInstanceOf(GfaParseException.class);
    }

    @Test
    void testGetGraphWithoutParsing() {
        currentFileName = "random-file-name";
        final Throwable e = catchThrowable(() -> new GfaFile(currentFileName).getGraph());

        assertThat(e).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testReadFile() throws GfaParseException {
        currentFileName = GFA_TEST_FILE;
        final GfaFile gfaFile = new GfaFile(GFA_TEST_FILE);

        assertThat(bufferedReaderToString(gfaFile.readFile())).isEqualTo(SIMPLE_GFA_CONTENTS);
    }

    @Test
    void testParseFile() throws IOException, GfaParseException {
        final GfaParser gfaParser = spy(GfaParser.class);
        GfaParserFactory.setInstance(gfaParser);

        currentFileName = GFA_TEST_FILE;
        final GfaFile gfaFile = new GfaFile(GFA_TEST_FILE);
        gfaFile.parse(ProgressUpdater.DUMMY);

        verify(gfaParser).parse(eq(gfaFile), any(ProgressUpdater.class));
        assertThat(gfaFile.getGraph()).isNotNull();
    }

    @Test
    void testParseNodeMetadata() throws IOException, GfaParseException, MetadataParseException {
        final MetadataParser metadataParser = spy(MetadataParser.class);
        MetadataParserFactory.setInstance(metadataParser);

        currentFileName = GFA_TEST_FILE;
        final GfaFile gfaFile = new GfaFile(GFA_TEST_FILE);
        final NodeMetadata nodeMetadata = gfaFile.parseNodeMetadata(38);

        verify(metadataParser).parseNodeMetadata(gfaFile, 38);
        assertThat(nodeMetadata.getSequence()).isEqualTo("ACCTT");
    }

    @Test
    void testGetGenomeMapping() {
        final GfaFile gfaFile = new GfaFile(GFA_TEST_FILE);

        final Map<String, String> mapping = new HashMap<>();
        mapping.put("basta.fasta", "123");
        gfaFile.setGenomeMapping(mapping);

        assertThat(gfaFile.getGenomeMapping()).containsExactly(entry("basta.fasta", "123"));
    }

    @Test
    void testParseEdgeMetadata() throws IOException, GfaParseException, MetadataParseException {
        final MetadataParser metadataParser = spy(MetadataParser.class);
        MetadataParserFactory.setInstance(metadataParser);

        currentFileName = GFA_TEST_FILE;
        final GfaFile gfaFile = new GfaFile(GFA_TEST_FILE);
        final EdgeMetadata edgeMetadata = gfaFile.parseEdgeMetadata(95);

        verify(metadataParser).parseEdgeMetadata(gfaFile, 95);
        assertThat(edgeMetadata.getToOrient()).isEqualTo("-");
    }

    private String bufferedReaderToString(final BufferedReader reader) {
        final String newline = String.format("%n");
        return reader.lines().collect(Collectors.joining(newline)) + newline;
    }
}
