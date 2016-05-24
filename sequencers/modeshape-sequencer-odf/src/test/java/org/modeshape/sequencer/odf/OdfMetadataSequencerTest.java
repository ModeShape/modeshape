/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.odf;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.CREATION_DATE;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.CREATOR;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.DESCRIPTION;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.EDITING_CYCLES;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.EDITING_TIME;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.GENERATOR;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.INITIAL_CREATOR;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.KEYWORDS;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.LANGUAGE;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.MODIFICATION_DATE;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.PAGES;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.PRINTED_BY;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.PRINT_DATE;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.SHEETS;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.SUBJECT;
import static org.modeshape.sequencer.odf.OdfMetadataLexicon.TITLE;

import java.util.Calendar;

import javax.jcr.Node;

import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link OdfMetadataSequencer}.
 */
public class OdfMetadataSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceTextDocument() throws Exception {
        // GIVEN
        String filename = "text.odt";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.text"));
        assertThat(sequencedNode.getProperty(PAGES).getLong(), is(2L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceTextTemplate() throws Exception {
        // GIVEN
        String filename = "text.ott";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.text-template"));
        assertThat(sequencedNode.getProperty(PAGES).getLong(), is(2L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequencePresentation() throws Exception {
        // GIVEN
        String filename = "presentation.odp";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.presentation"));
        assertThat(sequencedNode.getProperty(PAGES).getLong(), is(2L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequencePresentationTemplate() throws Exception {
        // GIVEN
        String filename = "presentation.otp";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.presentation-template"));
        assertThat(sequencedNode.getProperty(PAGES).getLong(), is(2L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceSpreadsheet() throws Exception {
        // GIVEN
        String filename = "spreadsheet.ods";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.spreadsheet"));
        assertThat(sequencedNode.getProperty(SHEETS).getLong(), is(1L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceSpreadsheetTemplate() throws Exception {
        // GIVEN
        String filename = "spreadsheet.ots";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.spreadsheet-template"));
        assertThat(sequencedNode.getProperty(SHEETS).getLong(), is(1L));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceDrawing() throws Exception {
        // GIVEN
        String filename = "drawing.odg";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.graphics"));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceDrawingTemplate() throws Exception {
        // GIVEN
        String filename = "drawing.otg";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.graphics-template"));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceChart() throws Exception {
        // GIVEN
        String filename = "chart.odc";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.chart"));

        assertCommonMetadata(sequencedNode);
    }

    @Test
    public void shouldSequenceChartTemplate() throws Exception {
        // GIVEN
        String filename = "chart.otc";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/odf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/vnd.oasis.opendocument.chart-template"));

        assertCommonMetadata(sequencedNode);
    }

    private void assertCommonMetadata( Node sequencedNode ) throws Exception {
        assertThat(sequencedNode.getProperty(TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(SUBJECT).getString(), is("Subject"));
        assertThat(sequencedNode.getProperty(DESCRIPTION).getString(), is("Description"));
        assertThat(sequencedNode.getProperty(KEYWORDS).getValues()[0].getString(), is("Keyword"));
        assertThat(sequencedNode.getProperty(KEYWORDS).getValues()[1].getString(), is("Second keyword"));
        assertThat(sequencedNode.getProperty(GENERATOR).getString(), is("Generator"));
        assertThat(sequencedNode.getProperty(CREATOR).getString(), is("Creator"));
        assertThat(sequencedNode.getProperty(LANGUAGE).getString(), is("en"));
        assertThat(sequencedNode.getProperty(PRINTED_BY).getString(), is("Printed by"));
        assertThat(sequencedNode.getProperty(INITIAL_CREATOR).getString(), is("Initial creator"));
        assertThat(sequencedNode.getProperty(EDITING_CYCLES).getLong(), is(2L));
        assertThat(sequencedNode.getProperty(EDITING_TIME).getLong(), is(666L));
        assertThat(sequencedNode.getProperty(CREATION_DATE).getDate().get(Calendar.YEAR), is(2000));
        assertThat(sequencedNode.getProperty(MODIFICATION_DATE).getDate().get(Calendar.YEAR), is(2005));
        assertThat(sequencedNode.getProperty(PRINT_DATE).getDate().get(Calendar.YEAR), is(2010));

    }
}