package org.dnacronym.hygene.parser;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dnacronym.hygene.models.FeatureAnnotation;
import org.dnacronym.hygene.models.SubFeatureAnnotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


/**
 * Parses GFF files.
 * <p>
 * These files become {@link FeatureAnnotation}s.
 *
 * @see <a href="https://github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md">GFF v3 specification</a>
 * @see FeatureAnnotation
 */
public final class GffParser {
    private static final String GFF_VERSION_HEADER = "##gff-version 3.2.1";
    private static final int PROGRESS_UPDATE_INTERVAL = 1000;
    /**
     * Progress is always the same, as we don't know how many lines the file has in advance.
     */
    private static final int PROGRESS = 50;
    private static final int PROGRESS_TOTAL = 100;
    private static final int GFF_COLUMNS = 9;

    private static final int SEQ_ID_COLUMN = 0;
    private static final int SOURCE_COLUMN = 1;
    private static final int TYPE_COLUMN = 2;
    private static final int START_COLUMN = 3;
    private static final int END_COLUMN = 4;
    private static final int SCORE_COLUMN = 5;
    private static final int STRAND_COLUMN = 6;
    private static final int PHASE_COLUMN = 7;
    private static final int ATTRIBUTES_COLUMN = 8;


    /**
     * Parses a GFF file.
     * <p>
     * Firstly checks that the files starts with the '{@value GFF_VERSION_HEADER}'.<br>
     * Afterwards, starts parsing the file. Blank lines and lines starting with '#' are ignored. Lines starting with
     * '##' are added as file meta-data to the {@link FeatureAnnotation}.
     * <p>
     * All other lines are parsed and converted to {@link SubFeatureAnnotation}s to be stored in the
     * {@link FeatureAnnotation}. Only the first encountered seqid is used to construct a {@link FeatureAnnotation}, all
     * subsequent seqid's are ignored and assumed to be correct.
     *
     * @param gffFile         the path of the GFF file to parse
     * @param progressUpdater the {@link ProgressUpdater} to update whilst parsing
     * @return a {@link FeatureAnnotation} representing the GFF file
     * @throws ParseException if unable to parse the {@link java.io.File}, which can either be caused by an
     *                        {@link IOException} when opening the file or a semantic error in the GFF file itself
     */
    @SuppressWarnings("squid:S135") // An object is only created once in the loop.
    public FeatureAnnotation parse(final String gffFile, final ProgressUpdater progressUpdater) throws ParseException {
        @MonotonicNonNull FeatureAnnotation featureAnnotation = null;
        final List<String> fileMetadata = new LinkedList<>();

        int lineNumber = 1;
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(gffFile), StandardCharsets.UTF_8)) {
            String line = bufferedReader.readLine();
            checkValidHeader(line);

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                if (lineNumber % PROGRESS_UPDATE_INTERVAL == 0) {
                    progressUpdater.updateProgress(PROGRESS, "Reading line " + lineNumber + " of " + gffFile);
                }
                if (!lineIsFeature(line, fileMetadata)) {
                    continue;
                }

                final String[] columns = parseLine(line);
                if (featureAnnotation == null) {
                    featureAnnotation = createFeatureAnnotation(columns[SEQ_ID_COLUMN]);
                }

                featureAnnotation.addSubFeatureAnnotation(parseSubFeatureAnnotation(columns));
            }

            if (featureAnnotation == null) {
                throw new ParseException("An error occurred while reading the GFF file: There was no seqid.");
            }
        } catch (final IOException e) {
            throw new ParseException("An IO error occurred while reading the GFF file.", e);
        } catch (final IllegalArgumentException e) {
            throw new ParseException("There was an error at line " + lineNumber + ".", e);
        }

        featureAnnotation.addMetadata(fileMetadata);
        progressUpdater.updateProgress(PROGRESS_TOTAL, "Finished reading the file.");

        progressUpdater.updateProgress(PROGRESS_TOTAL, "Finished read the file.");

        return featureAnnotation;
    }

    /**
     * Check if the given line is equal to {@value GFF_VERSION_HEADER}.
     *
     * @param line the line to check
     */
    private void checkValidHeader(final @Nullable String line) {
        if (line == null || !line.equals(GFF_VERSION_HEADER)) {
            throw new IllegalArgumentException("The GFF file does not have the appropriate header: '"
                    + GFF_VERSION_HEADER + "', it was: '" + line + "'.");
        }
    }

    /**
     * Checks whether the given line is a feature.
     * <p>
     * If it is an empty line or a comment, the line is not a feature.<br>
     * If it start with "##", it is not a feature, but it is metadata and is therefore added to fileMetadata.
     *
     * @param line         the line to check
     * @param fileMetadata the file meta data to append to if line is meta data
     * @return true if line is an actual feature, false otherwise
     */
    private static boolean lineIsFeature(final String line, final List<String> fileMetadata) {
        if (line.isEmpty() || line.charAt(0) == '#' && line.charAt(1) != '#') {
            return false;
        } else if (line.startsWith("##")) {
            fileMetadata.add(line.substring(2, line.length() - 1));
            return false;
        }

        return true;
    }

    /**
     * Creates a {@link FeatureAnnotation} representing the current file.
     *
     * @param seqId the seqId of the {@link FeatureAnnotation}
     * @return a {@link FeatureAnnotation} representing the current file
     */
    private FeatureAnnotation createFeatureAnnotation(final String seqId) {
        return new FeatureAnnotation(seqId);
    }

    /**
     * Creates a {@link SubFeatureAnnotation} annotation representing the current line.
     *
     * @param columns the columns of the line to convert to a feature annotation
     * @return a {@link SubFeatureAnnotation} representing the current row in the file
     */
    private SubFeatureAnnotation parseSubFeatureAnnotation(final String[] columns) {
        final SubFeatureAnnotation subFeatureAnnotation;

        try {
            subFeatureAnnotation = new SubFeatureAnnotation(
                    columns[SOURCE_COLUMN],
                    columns[TYPE_COLUMN],
                    Integer.parseInt(columns[START_COLUMN]), // start must be valid
                    Integer.parseInt(columns[END_COLUMN]), // end must be valid
                    ".".equals(columns[SCORE_COLUMN]) ? -1 : Double.parseDouble(columns[SCORE_COLUMN]),
                    columns[STRAND_COLUMN],
                    ".".equals(columns[PHASE_COLUMN]) ? -1 : Integer.parseInt(columns[PHASE_COLUMN]));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("A number could not be parsed.", e);
        }

        final String[] attributes = columns[ATTRIBUTES_COLUMN].split(";");
        for (final String attribute : attributes) {
            final String[] keyValuePair = attribute.split("=");
            if (keyValuePair.length != 2) {
                throw new IllegalArgumentException("An attribute contained a key without a value: '" + attribute
                        + "'.");
            }

            subFeatureAnnotation.setAttribute(keyValuePair[0], keyValuePair[1]);
        }

        return subFeatureAnnotation;
    }

    /**
     * Parses a single line of a GFF file.
     *
     * @param line the string containing the line contents of the file
     * @return a {@link String} array representing the columns of a line
     */
    private String[] parseLine(final String line) {
        final String[] columns = line.split("\\s+");
        if (columns.length != GFF_COLUMNS) {
            throw new IllegalArgumentException("Line did not contain " + GFF_COLUMNS + " columns, it contained "
                    + columns.length + " columns.");
        }

        return columns;
    }
}
