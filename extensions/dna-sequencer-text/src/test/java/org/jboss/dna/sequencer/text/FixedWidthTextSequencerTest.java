package org.jboss.dna.sequencer.text;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.sequencer.MockSequencerContext;
import org.jboss.dna.graph.sequencer.MockSequencerOutput;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FixedWidthTextSequencerTest {
    private FixedWidthTextSequencer sequencer;
    private InputStream input;
    private MockSequencerOutput output;
    private StreamSequencerContext context;

    @Before
    public void beforeEach() throws Exception {
        sequencer = new FixedWidthTextSequencer();
        context = new MockSequencerContext();
        context.getNamespaceRegistry().register("text", "http://www.jboss.org/dna/sequencer/text/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        output = new MockSequencerOutput(context);

        sequencer.setColumnStartPositions("3,6");
    }

    @After
    public void afterEach() throws Exception {
        if (input != null) {
            try {
                input.close();
            } finally {
                input = null;
            }
        }
    }

    protected InputStream getTestDocument( String resourcePath ) {
        return this.getClass().getResourceAsStream("/fixed/" + resourcePath);
    }
    @Test
    public void shouldSequenceFixedWidthFileWithOneLine() {
        input = getTestDocument("oneLineFixedWidthFile.txt");

        sequencer.sequence(input, output, context);

        assertThat(output.getProperty("text:row", "jcr:primaryType"), is(notNullValue()));
        assertThat((Name)output.getProperty("text:row", "jcr:primaryType").getFirstValue(), is(JcrNtLexicon.UNSTRUCTURED));

        String[] columns = new String[] {"foo", "bar", "baz"};
        for (int col = 1; col <= columns.length; col++) {
            assertThat((Name)output.getProperty("text:row/text:column[" + col + "]", "jcr:primaryType").getFirstValue(),
                       is(JcrNtLexicon.UNSTRUCTURED));
            assertThat((Name)output.getProperty("text:row/text:column[" + col + "]", "jcr:mixinTypes").getFirstValue(),
                       is(TextSequencerLexicon.COLUMN));
            assertThat((String)output.getProperty("text:row/text:column[" + col + "]", "text:data").getFirstValue(),
                       is(columns[col - 1]));
        }
    }

    @Test
    public void shouldSequenceFixedWidthFileWithOneLineAndNoTrailingNewLine() {
        input = getTestDocument("oneLineFixedWidthFileNoTrailingNewLine.txt");

        sequencer.sequence(input, output, context);

        assertThat(output.getProperty("text:row", "jcr:primaryType"), is(notNullValue()));
        assertThat((Name)output.getProperty("text:row", "jcr:primaryType").getFirstValue(), is(JcrNtLexicon.UNSTRUCTURED));

        String[] columns = new String[] {"foo", "bar", "baz"};
        for (int col = 1; col <= columns.length; col++) {
            assertThat((Name)output.getProperty("text:row/text:column[" + col + "]", "jcr:primaryType").getFirstValue(),
                       is(JcrNtLexicon.UNSTRUCTURED));
            assertThat((Name)output.getProperty("text:row/text:column[" + col + "]", "jcr:mixinTypes").getFirstValue(),
                       is(TextSequencerLexicon.COLUMN));
            assertThat((String)output.getProperty("text:row/text:column[" + col + "]", "text:data").getFirstValue(),
                       is(columns[col - 1]));
        }
    }

    @Test
    public void shouldSequenceFixedWidthFileWithMultipleLines() {
        input = getTestDocument("multiLineFixedWidthFile.txt");

        sequencer.sequence(input, output, context);

        assertThat(output.getProperty("text:row", "jcr:primaryType"), is(notNullValue()));
        assertThat((Name)output.getProperty("text:row", "jcr:primaryType").getFirstValue(), is(JcrNtLexicon.UNSTRUCTURED));

        final int ROW_COUNT = 6;
        for (int row = 1; row <= ROW_COUNT; row++) {
            String[] columns = new String[] {"foo", "bar", "baz"};
            for (int col = 1; col <= columns.length; col++) {
                assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:primaryType").getFirstValue(),
                           is(JcrNtLexicon.UNSTRUCTURED));
                assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:mixinTypes").getFirstValue(),
                           is(TextSequencerLexicon.COLUMN));
                assertThat((String)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "text:data").getFirstValue(),
                           is(columns[col - 1]));
            }
        }
    }

    @Test
    public void shouldSequenceFixedWidthFileWithMultipleLinesAndMissingRecords() {
        input = getTestDocument("multiLineFixedWidthFileMissingRecords.txt");

        sequencer.sequence(input, output, context);

        assertThat(output.getProperty("text:row", "jcr:primaryType"), is(notNullValue()));
        assertThat((Name)output.getProperty("text:row", "jcr:primaryType").getFirstValue(), is(JcrNtLexicon.UNSTRUCTURED));

        final int ROW_COUNT = 6;
        for (int row = 1; row <= ROW_COUNT; row++) {
            String[] columns = new String[] {"foo", "bar", "baz"};
            for (int col = 1; col <= columns.length; col++) {
                // This file only has one record in line 3
                if (row == 3 && col > 1) {
                    assertThat(output.hasProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:primaryType"), is(false));

                } else {
                    assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:primaryType").getFirstValue(),
                               is(JcrNtLexicon.UNSTRUCTURED));
                    assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:mixinTypes").getFirstValue(),
                               is(TextSequencerLexicon.COLUMN));
                    assertThat((String)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "text:data").getFirstValue(),
                               is(columns[col - 1]));
                }
            }
        }
    }

    @Test
    public void shouldSequenceFixedWidthFileWithCustomRowFactory() throws Exception {
        input = getTestDocument("multiLineFixedWidthFile.txt");

        sequencer.setRowFactoryClassName(CustomRowFactory.class.getName());
        sequencer.sequence(input, output, context);

        final int ROW_COUNT = 6;
        for (int row = 1; row <= ROW_COUNT; row++) {
            String[] columns = new String[] {"foo", "bar", "baz"};
            for (int col = 0; col < columns.length; col++) {
                assertThat((String)output.getProperty("text:row[" + row + "]", "text:data" + col).getFirstValue(),
                           is(columns[col]));
            }
        }
    }

    @Test
    public void shouldSequenceFixedWidthFileWithComments() throws Exception {
        input = getTestDocument("multiLineFixedWidthFileWithComments.txt");

        sequencer.setCommentMarker("#");
        sequencer.sequence(input, output, context);

        final int ROW_COUNT = 4;
        String[] columns = new String[] {"foo", "bar", "baz"};
        for (int row = 1; row <= ROW_COUNT; row++) {
            for (int col = 1; col <= columns.length; col++) {
                assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:primaryType").getFirstValue(),
                           is(JcrNtLexicon.UNSTRUCTURED));
                assertThat((Name)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:mixinTypes").getFirstValue(),
                           is(TextSequencerLexicon.COLUMN));
                assertThat((String)output.getProperty("text:row[" + row + "]/text:column[" + col + "]", "text:data").getFirstValue(),
                           is(columns[col - 1]));
            }
        }

        // There should be no row 5, as the input file contains 6 lines, but two are commented out
        int row = 5;
        for (int col = 1; col <= columns.length; col++) {
            assertThat(output.hasProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:primaryType"), is(false));
            assertThat(output.hasProperty("text:row[" + row + "]/text:column[" + col + "]", "jcr:mixinTypes"), is(false));
            assertThat(output.hasProperty("text:row[" + row + "]/text:column[" + col + "]", "text:data"), is(false));
        }
    }

    public static class CustomRowFactory implements RowFactory {

        int rowNum = 1;

        public void recordRow( StreamSequencerContext context,
                            SequencerOutput output,
                            String[] columns ) {
            for (int i = 0; i < columns.length; i++) {
                output.setProperty("text:row[" + rowNum + "]", "text:data" + i, columns[i]);
            }

            rowNum++;
        }

    }
}
