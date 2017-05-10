package org.dnacronym.hygene.parser;

import org.dnacronym.hygene.parser.factories.SequenceAlignmentGraphParserFactory;
import org.dnacronym.hygene.parser.factories.GfaParserFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


class GfaFileTest {

    @Test
    void testGfaFileObjectCanBeConstructed() {
        final GfaFile gfaFile = new GfaFile("name_of_the_file.gfa");

        assertThat(gfaFile.getFileName()).isEqualTo("name_of_the_file.gfa");
    }

    @Test
    void testCannotReadNonExistingFile() {
        final Throwable e = catchThrowable(() -> new GfaFile("random-file-name").parse());

        assertThat(e).isInstanceOf(ParseException.class);
    }

    @Test
    void testGetGraphWithoutParsing() {
        final Throwable e = catchThrowable(() -> new GfaFile("random-file-name").getGraph());

        assertThat(e).isInstanceOf(ParseException.class);
    }

    @Test
    void testParseFile() throws IOException, ParseException {
        GfaParser gfaParser = spy(GfaParser.class);
        GfaParserFactory.setInstance(gfaParser);
        SequenceAlignmentGraphParser sequenceAlignmentGraphParser = spy(SequenceAlignmentGraphParser.class);
        SequenceAlignmentGraphParserFactory.setInstance(sequenceAlignmentGraphParser);

        GfaFile gfaFile = new GfaFile("src/test/resources/gfa/simple.gfa");
        gfaFile.parse();

        verify(gfaParser).parse(String.format(
                "H\tVN:Z:1.0%n"
                        + "S\t11\tACCTT%n"
                        + "S\t12\tTCAAGG%n"
                        + "L\t11\t+\t12\t-\t4M%n"
        ));
        verify(sequenceAlignmentGraphParser).parse(any(SequenceAlignmentGraph.class));
        assertThat(gfaFile.getGraph()).isNotNull();
    }
}