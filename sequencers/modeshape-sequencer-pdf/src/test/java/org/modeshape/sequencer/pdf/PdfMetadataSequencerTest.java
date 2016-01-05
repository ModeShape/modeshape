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
package org.modeshape.sequencer.pdf;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.AUTHOR;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.CREATOR;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.ENCRYPTED;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.KEYWORDS;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.ORIENTATION;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.PAGE_COUNT;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.PRODUCER;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.SUBJECT;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.TITLE;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.VERSION;
import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.XMP_NODE;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.BASE_URL;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.CREATE_DATE;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.CREATOR_TOOL;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.IDENTIFIER;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.METADATA_DATE;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.MODIFY_DATE;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.NICKNAME;
import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.RATING;

import java.util.Calendar;

import javax.jcr.Node;

import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link PdfMetadataSequencer}.
 */
public class PdfMetadataSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceBasicMetadata() throws Exception {
        // GIVEN
        String filename = "sample.pdf";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/pdf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/pdf"));
        assertThat(sequencedNode.getProperty(PAGE_COUNT).getLong(), is(2L));
        assertThat(sequencedNode.getProperty(ORIENTATION).getString(), is("portrait"));
        assertThat(sequencedNode.getProperty(ENCRYPTED).getBoolean(), is(false));
        assertThat(sequencedNode.getProperty(VERSION).getString(), is("1.4"));

        assertThat(sequencedNode.getProperty(AUTHOR).getString(), is("Author"));
        assertThat(sequencedNode.getProperty(CREATOR).getString(), is("Creator"));
        assertThat(sequencedNode.getProperty(KEYWORDS).getString(), is("Keywords"));
        assertThat(sequencedNode.getProperty(PRODUCER).getString(), is("Producer"));
        assertThat(sequencedNode.getProperty(SUBJECT).getString(), is("Subject"));
        assertThat(sequencedNode.getProperty(TITLE).getString(), is("Title"));
    }

    @Test
    public void shouldSequenceXMPMetadata() throws Exception {
        // GIVEN
        String filename = "sample.pdf";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/pdf/" + filename);
        assertThat(sequencedNode.hasNode(XMP_NODE), is(true));
        Node xmpNode = sequencedNode.getNode(XMP_NODE);

        assertThat(xmpNode.getProperty(BASE_URL).getString(), is("BaseURL"));
        assertThat(xmpNode.getProperty(CREATOR_TOOL).getString(), is("Creator Tool"));
        assertThat(xmpNode.getProperty(RATING).getLong(), is(0L));
        assertThat(xmpNode.getProperty(IDENTIFIER).getValues()[0].getString(), is("Identifier"));
        assertThat(xmpNode.getProperty(IDENTIFIER).getValues()[1].getString(), is("Second identifier"));
        assertThat(xmpNode.getProperty(CREATE_DATE).getDate().get(Calendar.YEAR), is(2000));
        assertThat(xmpNode.getProperty(METADATA_DATE).getDate().get(Calendar.YEAR), is(2005));
        assertThat(xmpNode.getProperty(MODIFY_DATE).getDate().get(Calendar.YEAR), is(2010));
        assertThat(xmpNode.getProperty(NICKNAME).getString(), is("Nickname"));

        // TODO: getLabel isn't working in jempbox 1.8.10
        // assertThat(xmpNode.getProperty(LABEL).getLong(), is("Label"));
    }

    @Test
    public void shouldSequenceEncryptedPdf() throws Exception {
        // GIVEN
        String filename = "sample-encrypted.pdf";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/pdf/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/pdf"));
        assertThat(sequencedNode.getProperty(PAGE_COUNT).getLong(), is(2L));
        assertThat(sequencedNode.getProperty(ORIENTATION).getString(), is("portrait"));
        assertThat(sequencedNode.getProperty(ENCRYPTED).getBoolean(), is(true));
        assertThat(sequencedNode.getProperty(VERSION).getString(), is("1.4"));

        assertThat(sequencedNode.hasProperty("Author"), is(false));

        assertThat(sequencedNode.hasNode(XMP_NODE), is(false));
    }
}
