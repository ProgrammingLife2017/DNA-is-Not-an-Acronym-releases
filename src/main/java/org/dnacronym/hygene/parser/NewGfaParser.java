package org.dnacronym.hygene.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.nbio.core.sequence.io.BufferedReaderBytesRead;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.dnacronym.hygene.core.UnsignedInteger;
import org.dnacronym.hygene.models.Graph;
import org.dnacronym.hygene.models.Node;
import org.dnacronym.hygene.models.NodeColor;
import org.dnacronym.hygene.models.SequenceDirection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


/**
 * Parses GFA to a {@link Graph}.
 *
 * @see <a href="https://github.com/GFA-spec/GFA-spec/">GFA v1 specification</a>
 */
@SuppressWarnings("PMD.TooManyMethods") // No reasonable refactor possible
public final class NewGfaParser {
    private static final Logger LOGGER = LogManager.getLogger(NewGfaParser.class);
    private static final int PROGRESS_UPDATE_INTERVAL = 1000;
    private static final int PROGRESS_ALLOCATE_HEURISTIC = 1750000;
    private static final long PROGRESS_ALLOCATE_TOTAL = 20;
    private static final long PROGRESS_PARSE_LINE_TOTAL = 80;
    private static final String SOURCE_NAME = "<source>";
    private static final String SINK_NAME = "<sink>";

    private final Map<String, Integer> nodeIds; // node id string => nodeArrays index (internal node id)
    private final AtomicInteger nodeVectorPosition = new AtomicInteger(0);
    private int[][] nodeArrays;
    private int lineCount;


    /**
     * Constructs and initializes a new instance of {@link NewGfaParser}.
     */
    public NewGfaParser() {
        this.nodeIds = new ConcurrentHashMap<>();
        this.nodeArrays = new int[0][];
    }


    /**
     * Parses a GFA file to a {@link Graph}.
     *
     * @param gfaFile         an instance of {@link GfaFile}
     * @param progressUpdater a {@link ProgressUpdater} to notify interested parties on progress updates
     * @return a {@link Graph}
     * @throws ParseException if the given {@link String} is not GFA-compliant
     */
    @EnsuresNonNull("nodeArrays")
    public Graph parse(final GfaFile gfaFile, final ProgressUpdater progressUpdater) throws ParseException {
        try {
            LOGGER.info("Start allocating nodes");
            final BufferedReader nodeAllocationReader = gfaFile.readFile();
            allocateNodes(nodeAllocationReader, progressUpdater);
            LOGGER.info("Finished allocating nodes");

            nodeArrays = new int[nodeIds.size()][];
            Arrays.setAll(nodeArrays, i -> Node.createEmptyNodeArray());

            final BufferedReaderBytesRead lineParsingReader = new BufferedReaderBytesRead(new InputStreamReader(gfaFile.getInputStream()));

            int iteration = 0;
            long byteOffset = 0;
            String line;
            LOGGER.info("Start parsing lines");
            while ((line = lineParsingReader.readLine()) != null) {
                if (iteration % PROGRESS_UPDATE_INTERVAL == 0) {
                    progressUpdater.updateProgress(
                            (int) (PROGRESS_ALLOCATE_TOTAL + PROGRESS_PARSE_LINE_TOTAL * iteration / lineCount),
                            "Parsing nodes and edges..."
                    );
                }
                parseLine(line, byteOffset);

                byteOffset = lineParsingReader.getBytesRead();
                iteration++;
            }
            LOGGER.info("Finished parsing lines");
        } catch (final IOException e) {
            throw new ParseException("An error while reading the GFA file.", e);
        }

        final Graph graph = new Graph(nodeArrays, gfaFile);

        addEdgesToSentinelNodes(graph);

        return graph;
    }

    /**
     * Allocates the required internal node IDs.
     * <p>
     * This step is necessary because we need to know the internal node IDs
     * upfront to be able to add edges to the correct node vectors.
     *
     * @param gfa             a buffered reader of a GFA file
     * @param progressUpdater a {@link ProgressUpdater} to notify interested parties on progress updates
     * @throws IOException    if the given GFA file is not valid
     * @throws ParseException if a line does not have enough tokens
     */
    private void allocateNodes(final BufferedReader gfa, final ProgressUpdater progressUpdater)
            throws IOException, ParseException {
        addNodeId(SOURCE_NAME);
        String line;
        while ((line = gfa.readLine()) != null) {
            if (lineCount % PROGRESS_UPDATE_INTERVAL == 0) {
                final int update = lineCount / PROGRESS_ALLOCATE_HEURISTIC;
                if (update <= PROGRESS_ALLOCATE_TOTAL) {
                    progressUpdater.updateProgress(update, "Allocating nodes...");
                }
            }

            if (line.startsWith("S\t")) {
                addNodeId(parseNodeName(line));
            }
            lineCount++;
        }
        addNodeId(SINK_NAME);
    }

    /**
     * Add a node ID to the list of node IDs and increment the node vector position.
     *
     * @param nodeName The name of the node as specified in the GFA file
     */
    private void addNodeId(final String nodeName) {
        nodeIds.put(nodeName, nodeVectorPosition.get());
        nodeVectorPosition.incrementAndGet();
    }

    /**
     * Parses a line of a GFA-compliant {@link String} and adds it to the node vectors.
     *
     * @param line       a line of a GFA-compliant {@link String}
     * @param byteOffset the byte offset of the current line
     * @throws ParseException if the given {@link String}s are not GFA-compliant
     */
    private void parseLine(final String line, final long byteOffset) throws ParseException {
        if (line.indexOf('\t') < 0) {
            return;
        }

        switch (line.charAt(0)) {
            case 'H':
            case 'C':
            case 'P':
                break;

            case 'S':
                parseSegment(line, 2, byteOffset);
                break;

            case 'L':
                parseLink(line, 2, byteOffset);
                break;

            default:
                throw new ParseException("Unknown record type `" + line.charAt(0) + "` at position " + byteOffset);
        }
    }

    /**
     * Parses a line to a node.
     *
     * @param line       a line
     * @param lineOffset the offset in the line to start parsing the segment at
     * @param byteOffset the byte offset of the current line
     * @throws ParseException if the line does not have enough tokens
     */
    private void parseSegment(final String line, final int lineOffset, final long byteOffset) throws ParseException {
        try {
            final int nameEnd = line.indexOf('\t', lineOffset);
            final String name = line.substring(lineOffset, nameEnd);
            final int sequenceEnd = line.indexOf('\t', nameEnd + 1);
            final String sequence = sequenceEnd < 0
                    ? line.substring(nameEnd + 1)
                    : line.substring(nameEnd + 1, sequenceEnd);

            final int nodeId = getNodeId(name);

            nodeArrays[nodeId][Node.NODE_BYTE_OFFSET_INDEX] = UnsignedInteger.fromLong(byteOffset);
            nodeArrays[nodeId][Node.NODE_SEQUENCE_LENGTH_INDEX] = sequence.length();
            nodeArrays[nodeId][Node.NODE_COLOR_INDEX] = NodeColor.sequenceToColor(sequence).ordinal();

        } catch (final StringIndexOutOfBoundsException e) {
            throw new ParseException("Not enough parameters for segment at position " + byteOffset, e);
        }
    }

    /**
     * Parses a line to an edge.
     *
     * @param line       a line
     * @param lineOffset the offset in the line to start parsing the link at
     * @param byteOffset the byte offset of the current line
     * @throws ParseException if the line does not have enough tokens
     */
    private void parseLink(final String line, final int lineOffset, final long byteOffset)
            throws ParseException {
        try {
            final int fromEnd = line.indexOf('\t', lineOffset);
            final String from = line.substring(lineOffset, fromEnd);
            final int toStart = line.indexOf('\t', fromEnd + 1) + 1;
            final int toEnd = line.indexOf('\t', toStart);
            final String to = toEnd < 0
                    ? line.substring(toStart)
                    : line.substring(toStart, toEnd);

            final int fromId = getNodeId(from);
            final int toId = getNodeId(to);

            addIncomingEdge(fromId, toId, byteOffset);
            addOutgoingEdge(fromId, toId, byteOffset);
        } catch (final StringIndexOutOfBoundsException e) {
            throw new ParseException("Not enough parameters for link at position " + byteOffset, e);
        }
    }

    /**
     * Parses the name of the node (the node id) from a GFA file line.
     *
     * @param line a GFA file line
     * @return the name of the node (the node id)
     * @throws ParseException if a line does not have enough tokens
     */
    private String parseNodeName(final String line) throws ParseException {
        try {
            final int startIndex = line.indexOf('\t');
            final int endIndex = line.indexOf('\t', startIndex + 1);

            return line.substring(startIndex + 1, endIndex);
        } catch (final StringIndexOutOfBoundsException e) {
            throw new ParseException("Invalid segment line.", e);
        }
    }

    /**
     * Adds an incoming edge to the node vector.
     *
     * @param fromId     node ID of edge start node
     * @param toId       node ID of edge end node
     * @param byteOffset the byte offset of the current line
     */
    private void addIncomingEdge(final int fromId, final int toId, final long byteOffset) {
        nodeArrays[toId] = Arrays.copyOf(nodeArrays[toId], nodeArrays[toId].length + Node.EDGE_DATA_SIZE);

        nodeArrays[toId][nodeArrays[toId].length - 2] = fromId;
        nodeArrays[toId][nodeArrays[toId].length - 1] = UnsignedInteger.fromLong(byteOffset);
    }

    /**
     * Adds an outgoing edge to the node vector.
     *
     * @param fromId     node ID of edge start node
     * @param toId       node ID of edge end node
     * @param byteOffset the byte offset of the current line
     */
    private void addOutgoingEdge(final int fromId, final int toId, final long byteOffset) {
        final int lastOutgoingEdgePosition = Node.NODE_EDGE_DATA_OFFSET
                + nodeArrays[fromId][Node.NODE_OUTGOING_EDGES_INDEX] * Node.EDGE_DATA_SIZE;

        nodeArrays[fromId] = insertAtPosition(
                nodeArrays[fromId],
                lastOutgoingEdgePosition,
                toId,
                UnsignedInteger.fromLong(byteOffset)
        );
        nodeArrays[fromId][Node.NODE_OUTGOING_EDGES_INDEX]++;
    }

    /**
     * Inserts one or more elements at a certain position in a given array.
     *
     * @param array    array in which new elements need to be added
     * @param position insertion position
     * @param inserts  the values that need to be inserted
     * @return array with the elements inserted
     */
    private int[] insertAtPosition(final int[] array, final int position, final int... inserts) {
        final int[] result = new int[array.length + inserts.length];

        System.arraycopy(array, 0, result, 0, position);

        for (int i = 0; i < inserts.length; i++) {
            result[position + i] = inserts[i];
        }

        System.arraycopy(array, position, result, position + inserts.length, array.length - position);

        return result;
    }

    /**
     * Add edges for nodes without incoming or outgoing edges to the source or sink.
     *
     * @param graph the graph data structure including the source and sink
     * @throws ParseException if the graph has an invalid number of nodes
     */
    private void addEdgesToSentinelNodes(final Graph graph) throws ParseException {
        if (nodeArrays.length == 2) {
            throw new ParseException("The GFA file should contain at least one segment.");
        }

        final int source = getNodeId(SOURCE_NAME);
        final int sink = getNodeId(SINK_NAME);

        IntStream.range(1, nodeArrays.length - 1).forEach(nodeId -> {
            if (graph.getNeighbourCount(nodeId, SequenceDirection.LEFT) == 0) {
                addIncomingEdge(source, nodeId, 0); // WAS -1, BUT CANNOT IN UINT
                addOutgoingEdge(source, nodeId, 0); // WAS -1, BUT CANNOT IN UINT
            }

            if (graph.getNeighbourCount(nodeId, SequenceDirection.RIGHT) == 0) {
                addIncomingEdge(nodeId, sink, 0); // WAS -1, BUT CANNOT IN UINT
                addOutgoingEdge(nodeId, sink, 0); // WAS -1, BUT CANNOT IN UINT
            }
        });
    }

    /**
     * Gets node id belonging to a node name.
     *
     * @param nodeName name of the node as specified in the GFA file
     * @return node id belonging to a node name
     * @throws ParseException if the node name does not exist
     */
    private int getNodeId(final String nodeName) throws ParseException {
        return Optional.ofNullable(nodeIds.get(nodeName)).orElseThrow(
                () -> new ParseException("Link has reference to non existing node " + nodeName)
        );
    }
}
