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

import static org.modeshape.sequencer.audio.AudioMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within audio sequencer.
 * 
 * @since 5.1
 */
@Immutable
public class AudioMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/audio/1.0";
        public static final String PREFIX = "audio";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";

    public static final String FORMAT_NAME = PREFIX + ":formatName";
    public static final String BITRATE = PREFIX + ":bitrate";
    public static final String SAMPLE_RATE = PREFIX + ":sampleRate";
    public static final String CHANNELS = PREFIX + ":channels";
    public static final String DURATION = PREFIX + ":duration";

    public static final String TAG_NODE = PREFIX + ":tag";

    public static final String TITLE = PREFIX + ":title";
    public static final String ARTIST = PREFIX + ":artist";
    public static final String ALBUM = PREFIX + ":album";
    public static final String YEAR = PREFIX + ":year";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String TRACK = PREFIX + ":track";
    public static final String GENRE = PREFIX + ":genre";

    public static final String ARTWORK_NODE = PREFIX + ":artwork";
    public static final String ARTWORK_TYPE = PREFIX + ":artworkType";

}
