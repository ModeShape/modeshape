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
package org.modeshape.sequencer.video;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.CHANNELS;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.CODEC;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.COMMENT;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.DURATION;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.ENCODER;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.FRAMERATE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.HEIGHT;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.SAMPLERATE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.STREAM_NODE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.STREAM_TYPE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.TITLE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.WIDTH;

import javax.jcr.Node;

import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link VideoMetadataSequencer}.
 */
public class VideoMetadataSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceAvi() throws Exception {
        // GIVEN
        String filename = "sample.avi";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/video/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("video/x-msvideo"));
        assertThat(sequencedNode.getProperty(DURATION).getDouble(), is(3.0));
        assertThat(sequencedNode.getProperty(TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(COMMENT).getString(), is("Comment"));
        assertThat(sequencedNode.getProperty(ENCODER).getString(), is("Lavf56.25.101"));

        Node videoStream = sequencedNode.getNode(STREAM_NODE + "0");
        assertThat(videoStream.getProperty(STREAM_TYPE).getString(), is("video"));
        assertThat(videoStream.getProperty(CODEC).getString(), is("mpeg4"));
        assertThat(videoStream.getProperty(WIDTH).getLong(), is(320L));
        assertThat(videoStream.getProperty(HEIGHT).getLong(), is(240L));
        assertThat(videoStream.getProperty(FRAMERATE).getDouble(), is(15.0));

        Node audioStream = sequencedNode.getNode(STREAM_NODE + "1");
        assertThat(audioStream.getProperty(STREAM_TYPE).getString(), is("audio"));
        assertThat(audioStream.getProperty(CODEC).getString(), is("mp3"));
        assertThat(audioStream.getProperty(SAMPLERATE).getLong(), is(48000L));
        assertThat(audioStream.getProperty(CHANNELS).getLong(), is(2L));
    }

    @Test
    public void shouldSequenceMp4() throws Exception {
        // GIVEN
        String filename = "sample.mp4";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/video/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("video/mp4"));
        assertThat(sequencedNode.getProperty(TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(COMMENT).getString(), is("Comment"));
        assertThat(sequencedNode.getProperty(ENCODER).getString(), is("Lavf56.25.101"));

        Node videoStream = sequencedNode.getNode(STREAM_NODE + "0");
        assertThat(videoStream.getProperty(STREAM_TYPE).getString(), is("video"));
        assertThat(videoStream.getProperty(CODEC).getString(), is("h264"));
        assertThat(videoStream.getProperty(HEIGHT).getLong(), is(240L));
        assertThat(videoStream.getProperty(FRAMERATE).getDouble(), is(30.0));

        Node audioStream = sequencedNode.getNode(STREAM_NODE + "1");
        assertThat(audioStream.getProperty(STREAM_TYPE).getString(), is("audio"));
        assertThat(audioStream.getProperty(CODEC).getString(), is("aac"));
        assertThat(audioStream.getProperty(SAMPLERATE).getLong(), is(48000L));
        assertThat(audioStream.getProperty(CHANNELS).getLong(), is(6L));
    }

    @Test
    public void shouldSequenceMkv() throws Exception {
        // GIVEN
        String filename = "sample.mkv";

        // WHEN
        createNodeWithContentFromFile(filename, filename);

        // THEN
        Node sequencedNode = getOutputNode(rootNode, "sequenced/video/" + filename);
        assertThat(sequencedNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString(), is("video/x-matroska"));
        assertThat(sequencedNode.getProperty(TITLE).getString(), is("Title"));
        assertThat(sequencedNode.getProperty(ENCODER).getString(), is("Lavf56.25.101"));

        Node videoStream = sequencedNode.getNode(STREAM_NODE + "0");
        assertThat(videoStream.getProperty(STREAM_TYPE).getString(), is("video"));
        assertThat(videoStream.getProperty(CODEC).getString(), is("h264"));
        assertThat(videoStream.getProperty(HEIGHT).getLong(), is(240L));
        assertThat(videoStream.getProperty(FRAMERATE).getDouble(), is(50.0));

        Node audioStream = sequencedNode.getNode(STREAM_NODE + "1");
        assertThat(audioStream.getProperty(STREAM_TYPE).getString(), is("audio"));
        assertThat(audioStream.getProperty(CODEC).getString(), is("aac"));
        assertThat(audioStream.getProperty(SAMPLERATE).getLong(), is(48000L));
        assertThat(audioStream.getProperty(CHANNELS).getLong(), is(6L));
    }
}