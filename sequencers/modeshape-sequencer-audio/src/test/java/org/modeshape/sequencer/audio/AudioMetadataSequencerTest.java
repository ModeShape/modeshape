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
package org.modeshape.sequencer.audio;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.ALBUM;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.ARTIST;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.ARTWORK_NODE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.BITRATE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.CHANNELS;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.COMMENT;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.FORMAT_NAME;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.GENRE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.DURATION;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.SAMPLE_RATE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.TAG_NODE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.TITLE;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.TRACK;
import static org.modeshape.sequencer.audio.AudioMetadataLexicon.YEAR;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.hamcrest.number.IsCloseTo.closeTo;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link AudioMetadataSequencer}
 *
 */
public class AudioMetadataSequencerTest extends AbstractSequencerTest {

    // range of error when comparing Double values (3 digits)
    private static final double ERROR = 1e-3;

    @Test
    public void shouldSequenceMp3() throws Exception {
        // GIVEN
        String filename = "sample.mp3";

        // WHEN
        Node audioNode = createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/audio/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "mp3", "audio/mpeg", 64L, 44100L, "2", 2.664,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");

        Node sequencedNodeSameLocation = getOutputNode(audioNode, METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "mp3", "audio/mpeg", 64L, 44100L, "2", 2.664,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");
    }

    @Test
    public void shouldSequenceMp4() throws Exception {
        // GIVEN
        String filename = "sample.mp4";

        // WHEN
        Node audioNode = createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/audio/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "mp4", "video/mp4", 129L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");

        Node sequencedNodeSameLocation = getOutputNode(audioNode, METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "mp4", "video/mp4", 129L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");
    }

    @Test
    public void shouldSequenceOggVorbis() throws Exception {
        // GIVEN
        String filename = "sample.ogg";

        // WHEN
        Node audioNode = createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/audio/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "ogg", "audio/vorbis", 112L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");

        Node sequencedNodeSameLocation = getOutputNode(audioNode, METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "ogg", "audio/vorbis", 112L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");
    }

    @Test
    public void shouldSequenceFlac() throws Exception {
        // GIVEN
        String filename = "sample.flac";

        // WHEN
        Node audioNode = createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/audio/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "flac", "audio/x-flac", 426L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");

        Node sequencedNodeSameLocation = getOutputNode(audioNode, METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "flac", "audio/x-flac", 426L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");
    }

    @Test
    public void shouldSequenceWma() throws Exception {
        // GIVEN
        String filename = "sample.wma";

        // WHEN
        Node audioNode = createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/audio/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "wma", "audio/x-ms-wma", 128L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");

        Node sequencedNodeSameLocation = getOutputNode(audioNode, METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "wma", "audio/x-ms-wma", 128L, 44100L, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg");
    }

    private void assertMetaDataProperties( Node sequencedNode, String formatName, String mimeType, Long bitrate, Long sampleRate,
                                           String channels, Double duration, String album, String artist, String comment, String title,
                                           String year, String track, String genre, String artworkMimeType) throws RepositoryException {
        assertThat(sequencedNode.getPrimaryNodeType().getName(), is(METADATA_NODE));
        assertThat(sequencedNode.getProperty(FORMAT_NAME).getString(), is(formatName));
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is(mimeType));
        assertThat(sequencedNode.getProperty(BITRATE).getLong(), is(bitrate));
        assertThat(sequencedNode.getProperty(SAMPLE_RATE).getLong(), is(sampleRate));
        assertThat(sequencedNode.getProperty(CHANNELS).getString(), is(channels));
        assertThat(sequencedNode.getProperty(DURATION).getDouble(), closeTo(duration, ERROR));

        Node tagNode = sequencedNode.getNode(TAG_NODE);
        assertThat(tagNode.getProperty(ALBUM).getString(), is(album));
        assertThat(tagNode.getProperty(ARTIST).getString(), is(artist));
        assertThat(tagNode.getProperty(COMMENT).getString(), is(comment));
        assertThat(tagNode.getProperty(TITLE).getString(), is(title));
        assertThat(tagNode.getProperty(YEAR).getString(), is(year));
        assertThat(tagNode.getProperty(TRACK).getString(), is(track));
        assertThat(tagNode.getProperty(GENRE).getString(), is(genre));

        Node artworkNode = tagNode.getNode(ARTWORK_NODE);
        assertThat(artworkNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is(artworkMimeType));
        assertThat(artworkNode.getProperty(JcrConstants.JCR_DATA).getBinary().getSize(), greaterThan(0L));
    }
}
