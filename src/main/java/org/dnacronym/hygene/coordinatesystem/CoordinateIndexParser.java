package org.dnacronym.hygene.coordinatesystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dnacronym.hygene.models.GraphIterator;
import org.dnacronym.hygene.models.SequenceDirection;
import org.dnacronym.hygene.parser.GfaFile;
import org.dnacronym.hygene.parser.MetadataParser;
import org.dnacronym.hygene.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Class responsible for indexing the genome coordinate system of a file.
 */
public final class CoordinateIndexParser {
    static final int BASE_CACHE_INTERVAL = 1000;

    private static final Logger LOGGER = LogManager.getLogger(GfaFile.class);

    private GfaFile gfaFile;
    private Map<String, Integer> genomeBaseCounts;


    public CoordinateIndexParser(GfaFile gfaFile) {
        this.gfaFile = gfaFile;
        this.genomeBaseCounts = new HashMap<>();
    }

    void parse() throws ParseException {
        loadGenomeList();

        final GraphIterator graphIterator = new GraphIterator(gfaFile.getGraph());
        graphIterator.visitAll(SequenceDirection.RIGHT, nodeId -> {
            try {
                final List<String> nodeGenomes = gfaFile.getGraph().getNode(nodeId).retrieveMetadata().getGenomes();

                for (final String genome : nodeGenomes) {
                    final Integer previousBaseCount = genomeBaseCounts.get(genome);

                    if (previousBaseCount == null) {
                        throw new ParseException("Unrecognized genome found at node " + nodeId + ".");
                    }

                    final int nodeBaseCount = gfaFile.getGraph().getSequenceLength(nodeId);
                    if (previousBaseCount + nodeBaseCount >= BASE_CACHE_INTERVAL) {
                        // TODO save position
                        genomeBaseCounts.put(genome, 0);
                    } else {
                        genomeBaseCounts.put(genome, previousBaseCount + nodeBaseCount);
                    }
                }
            } catch (final ParseException e) {
                LOGGER.warn("Failed to read metadata of node " + nodeId + ".");
            }
        });
    }

    /**
     * Retrieves the list of genomes in the GFA file from the header.
     *
     * @throws ParseException in case of errors during parsing of the GFA file
     */
    private void loadGenomeList() throws ParseException {
        final BufferedReader bufferedReader = gfaFile.readFile();

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null && line.startsWith("H")) {
                parseHeaderLine(line);
            }
        } catch (final IOException e) {
            throw new ParseException("An error while reading the GFA file.", e);
        }
    }

    /**
     * Parses one header line and checks for genome metadata.
     *
     * @param line the line to parse
     * @throws ParseException in case of errors during parsing of the GFA file
     */
    private void parseHeaderLine(final String line) throws ParseException {
        final StringTokenizer stringTokenizer = new StringTokenizer(line, "\t");

        stringTokenizer.nextToken(); // Ignore "H" token
        final String headerBody = stringTokenizer.nextToken();
        if (!headerBody.startsWith(MetadataParser.GENOME_LIST_HEADER_PREFIX)) {
            return;
        }

        final String genomeListString = headerBody.substring(MetadataParser.GENOME_LIST_HEADER_PREFIX.length());
        final StringTokenizer bodyTokenizer = new StringTokenizer(genomeListString, ";");

        while (bodyTokenizer.hasMoreTokens()) {
            final String nextGenome = bodyTokenizer.nextToken();

            if (!nextGenome.equals("")) {
                genomeBaseCounts.put(nextGenome, 0);
            }
        }

        if (genomeBaseCounts.isEmpty()) {
            throw new ParseException("Expected at least one genome to be present in GFA file.");
        }
    }
}
