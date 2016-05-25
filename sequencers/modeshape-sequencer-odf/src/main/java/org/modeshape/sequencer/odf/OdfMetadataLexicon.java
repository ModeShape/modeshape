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

import static org.modeshape.sequencer.odf.OdfMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within OpenDocument sequencer.
 * 
 * @since 5.1
 */
@Immutable
public class OdfMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/odf/1.0/";
        public static final String PREFIX = "odf";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";

    public static final String PAGES = PREFIX + ":pages";
    public static final String SHEETS = PREFIX + ":sheets";

    public static final String CREATION_DATE = PREFIX + ":creationDate";
    public static final String CREATOR = PREFIX + ":creator";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String EDITING_CYCLES = PREFIX + ":editingCycles";
    public static final String EDITING_TIME = PREFIX + ":editingTime";
    public static final String GENERATOR = PREFIX + ":generator";
    public static final String INITIAL_CREATOR = PREFIX + ":initialCreator";
    public static final String KEYWORDS = PREFIX + ":keywords";
    public static final String LANGUAGE = PREFIX + ":language";
    public static final String MODIFICATION_DATE = PREFIX + ":modificationDate";
    public static final String PRINTED_BY = PREFIX + ":printedBy";
    public static final String PRINT_DATE = PREFIX + ":printDate";
    public static final String SUBJECT = PREFIX + ":subject";
    public static final String TITLE = PREFIX + ":title";
}
