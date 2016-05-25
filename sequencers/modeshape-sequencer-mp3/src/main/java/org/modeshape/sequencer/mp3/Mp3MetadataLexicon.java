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
package org.modeshape.sequencer.mp3;

import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.Namespace.PREFIX;
import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the mp3 sequencer.
 *
 * @deprecated starting with 5.1 this should be replaced with the Audio sequencer; this will be removed in the next major version
 */
@Immutable
@Deprecated
public class Mp3MetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/mp3/1.0";
        public static final String PREFIX = "mp3";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";
    public static final String TITLE = PREFIX + ":title";
    public static final String AUTHOR = PREFIX + ":author";
    public static final String ALBUM = PREFIX + ":album";
    public static final String YEAR = PREFIX + ":year";
    public static final String COMMENT = PREFIX + ":comment";

}
