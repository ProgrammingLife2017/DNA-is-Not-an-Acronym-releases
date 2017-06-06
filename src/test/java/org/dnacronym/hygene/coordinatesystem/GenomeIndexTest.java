package org.dnacronym.hygene.coordinatesystem;

import org.dnacronym.hygene.parser.GfaFile;
import org.dnacronym.hygene.parser.ParseException;
import org.dnacronym.hygene.parser.ProgressUpdater;
import org.dnacronym.hygene.persistence.FileDatabase;
import org.dnacronym.hygene.persistence.FileDatabaseDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


/**
 * Test suite for the {@link GenomeIndex} class.
 */
class GenomeIndexTest {
    private static final String TEST_GFA_FILE_NAME = "src/test/resources/gfa/index.gfa";
    private static final int TEST_BASE_CACHE_INTERVAL = 1;

    private GenomeIndex genomeIndex;
    private FileDatabase fileDatabase;


    @BeforeEach
    void setUp() throws IOException, SQLException, ParseException {
        deleteDatabase();
        fileDatabase = new FileDatabase(TEST_GFA_FILE_NAME);

        final GfaFile gfaFile = new GfaFile(TEST_GFA_FILE_NAME);
        gfaFile.parse(mock(ProgressUpdater.class));
        genomeIndex = new GenomeIndex(gfaFile, fileDatabase);
        genomeIndex.setBaseCacheInterval(TEST_BASE_CACHE_INTERVAL);
    }

    @AfterEach
    void tearDown() throws IOException, SQLException {
        fileDatabase.close();
        deleteDatabase();
    }


    @Test
    void testGetClosestNodeId() throws ParseException, SQLException {
        genomeIndex.populateIndex();
        assertThat(genomeIndex.getClosestNodeId("g2.fasta", 6)).isEqualTo(3);
    }

    @Test
    void testGetBaseCacheInterval() {
        assertThat(genomeIndex.getBaseCacheInterval()).isEqualTo(TEST_BASE_CACHE_INTERVAL);
    }

    private void deleteDatabase() throws IOException {
        Files.deleteIfExists(new File(TEST_GFA_FILE_NAME + FileDatabaseDriver.DB_FILE_EXTENSION).toPath());
    }
}