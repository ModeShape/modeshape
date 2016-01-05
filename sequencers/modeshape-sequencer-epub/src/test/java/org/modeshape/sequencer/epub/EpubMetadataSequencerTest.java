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
package org.modeshape.sequencer.epub;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import javax.jcr.Node;

import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link EpubMetadataSequencer}.
 */
public class EpubMetadataSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceEpub() throws Exception {
        // GIVEN
        String filename = "sample.epub";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/epub/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/epub+zip"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.CREATOR).getString(), is("Creator"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.CONTRIBUTOR).getString(), is("Contributor"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.LANGUAGE).getString(), is("en"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.IDENTIFIER).getString(), is("Identifier"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.SUBJECT).getString(), is("Subject"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.DESCRIPTION).getString(), is("Description"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.PUBLISHER).getString(), is("Publisher"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.DATE).getString(), is("2015"));
    }

    @Test
    public void shouldSequenceEpub3() throws Exception {
        // GIVEN
        String filename = "sample_v3.epub";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/epub/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("application/epub+zip"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.CREATOR).getString(), is("Creator"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.CONTRIBUTOR).getString(), is("Contributor"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.LANGUAGE).getString(), is("en"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.IDENTIFIER).getString(), is("Identifier"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.SUBJECT).getString(), is("Subject"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.DESCRIPTION).getString(), is("Description"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.PUBLISHER).getString(), is("Publisher"));
        assertThat(sequencedNode.getProperty(EpubMetadataLexicon.DATE).getString(), is("2015"));
    }
}