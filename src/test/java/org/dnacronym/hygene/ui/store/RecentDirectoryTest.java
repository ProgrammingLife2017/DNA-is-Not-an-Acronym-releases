package org.dnacronym.hygene.ui.store;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test suite for the {@link RecentDirectory} class.
 */
class RecentDirectoryTest {
    @BeforeEach
    void setUp() throws IOException {
        deleteDataFile();
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteDataFile();
    }


    @Test
    void testStoreGet() throws IOException {
        final File testDirectory = new File("TestDirectory");

        RecentDirectory.store(testDirectory);
        assertThat(RecentDirectory.get().getAbsolutePath()).isEqualTo(testDirectory.getAbsolutePath());
    }

    @Test
    void testGetWithMissingDataFile() throws IOException {
        assertThat(RecentDirectory.get().getAbsolutePath()).isEqualTo(System.getProperty("user.home"));
    }


    /**
     * Deletes the data file for the recently opened directory.
     *
     * @throws IOException if an exception occurs during file IO
     */
    private void deleteDataFile() throws IOException {
        Files.deleteIfExists(org.dnacronym.hygene.core.Files.getInstance()
                .getAppDataFile(RecentDirectory.DATA_FILE_NAME).toPath());
    }
}
