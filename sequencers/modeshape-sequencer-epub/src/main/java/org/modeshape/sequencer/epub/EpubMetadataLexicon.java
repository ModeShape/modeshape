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

import static org.modeshape.sequencer.epub.EpubMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within EPUB sequencer.
 * 
 * @since 5.1
 */
@Immutable
public class EpubMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/epub/1.0/";
        public static final String PREFIX = "epub";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";
    public static final String PROPERTY_NODE = PREFIX + ":property";
    public static final String ALTERNATE_SCRIPT_NODE = PREFIX + ":alternateScript";

    public static final String IDENTIFIER = PREFIX + ":identifier";
    public static final String TITLE = PREFIX + ":title";
    public static final String LANGUAGE = PREFIX + ":language";
    public static final String CONTRIBUTOR = PREFIX + ":contributor";
    public static final String CREATOR = PREFIX + ":creator";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String PUBLISHER = PREFIX + ":publisher";
    public static final String RIGHTS = PREFIX + ":rights";
    public static final String DATE = PREFIX + ":date";

    public static final String NAME = PREFIX + ":name";
    public static final String VALUE = PREFIX + ":value";
    public static final String TITLE_TYPE = PREFIX + ":titleType";
    public static final String METADATA_AUTHORITY = PREFIX + ":metadataAuthority";
    public static final String ROLE = PREFIX + ":role";
    public static final String DISPLAY_SEQ = PREFIX + ":displaySeq";
    public static final String FILE_AS = PREFIX + ":fileAs";
    public static final String GROUP_POSITION = PREFIX + ":groupPosition";
    public static final String SCHEME = PREFIX + ":scheme";
    public static final String IDENTIFIER_TYPE = PREFIX + ":identifierType";

    public static final String LANGUAGE_CODE = PREFIX + ":languageCode";

}
