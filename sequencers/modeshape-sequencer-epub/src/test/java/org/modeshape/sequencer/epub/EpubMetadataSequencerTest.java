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

        Node title = sequencedNode.getNode(EpubMetadataLexicon.TITLE);
        assertThat(title.getProperty(EpubMetadataLexicon.VALUE).getString(), is("Title"));
        assertThat(title.getProperty(EpubMetadataLexicon.TITLE_TYPE).getString(), is("main"));
        Node titleAlternative = title.getNode(EpubMetadataLexicon.ALTERNATE_SCRIPT_NODE);
        assertThat(titleAlternative.getProperty(EpubMetadataLexicon.VALUE).getString(), is("Nadpis"));
        assertThat(titleAlternative.getProperty(EpubMetadataLexicon.LANGUAGE_CODE).getString(), is("cs"));

        Node language = sequencedNode.getNode(EpubMetadataLexicon.LANGUAGE);
        assertThat(language.getProperty(EpubMetadataLexicon.VALUE).getString(), is("en"));

        Node identifier = sequencedNode.getNode(EpubMetadataLexicon.IDENTIFIER);
        assertThat(identifier.getProperty(EpubMetadataLexicon.VALUE).getString(), is("Identifier"));
        assertThat(identifier.getProperty(EpubMetadataLexicon.IDENTIFIER_TYPE).getString(), is("01"));
        assertThat(identifier.getProperty(EpubMetadataLexicon.SCHEME).getString(), is("onix:codelist5"));

        Node contributor = sequencedNode.getNode(EpubMetadataLexicon.CONTRIBUTOR);
        assertThat(contributor.getProperty(EpubMetadataLexicon.VALUE).getString(), is("Contributor"));
        assertThat(contributor.getProperty(EpubMetadataLexicon.FILE_AS).getString(), is("Contributor, Mr."));

        Node date = sequencedNode.getNode(EpubMetadataLexicon.DATE);
        assertThat(date.getProperty(EpubMetadataLexicon.VALUE).getString(), is("2015-01-01"));

        assertThat(sequencedNode.hasNode(EpubMetadataLexicon.PUBLISHER), is(true));
        assertThat(sequencedNode.hasNode(EpubMetadataLexicon.DESCRIPTION), is(true));
        assertThat(sequencedNode.hasNode(EpubMetadataLexicon.RIGHTS), is(true));
        assertThat(sequencedNode.hasNode(EpubMetadataLexicon.CREATOR), is(true));
        assertThat(sequencedNode.hasNode(EpubMetadataLexicon.CREATOR + "[2]"), is(true));
    }

}