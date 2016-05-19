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

import static org.modeshape.sequencer.video.VideoMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within video sequencer.
 * 
 * @since 5.1
 */
@Immutable
public class VideoMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/video/1.0/";
        public static final String PREFIX = "video";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";

    public static final String DURATION = PREFIX + ":duration";
    public static final String BITRATE = PREFIX + ":bitrate";
    public static final String TITLE = PREFIX + ":title";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String ENCODER = PREFIX + ":encoder";

    public static final String STREAM_NODE = PREFIX + ":stream";

    public static final String STREAM_TYPE = PREFIX + ":streamType";
    public static final String CODEC = PREFIX + ":codec";
    public static final String FRAMERATE = PREFIX + ":framerate";
    public static final String SAMPLERATE = PREFIX + ":samplerate";
    public static final String CHANNELS = PREFIX + ":channels";
    public static final String WIDTH = PREFIX + ":width";
    public static final String HEIGHT = PREFIX + ":height";
}
