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

import static org.modeshape.sequencer.pdf.PdfMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within PDF sequencer.
 *
 * @since 5.1
 */
@Immutable
public class PdfMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/pdf/1.0/";
        public static final String PREFIX = "pdf";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";
    public static final String XMP_NODE = PREFIX + ":xmp";
    public static final String ATTACHMENT_NODE = PREFIX + ":attachment";
    public static final String PAGE_NODE = PREFIX + ":page";

    public static final String PAGE_COUNT = PREFIX + ":pageCount";
    public static final String ORIENTATION = PREFIX + ":orientation";
    public static final String ENCRYPTED = PREFIX + ":encrypted";
    public static final String VERSION = PREFIX + ":version";

    public static final String AUTHOR = PREFIX + ":author";
    public static final String CREATION_DATE= PREFIX + ":creationDate";
    public static final String CREATOR = PREFIX + ":creator";
    public static final String KEYWORDS = PREFIX + ":keywords";
    public static final String MODIFICATION_DATE = PREFIX + ":modificationDate";
    public static final String PRODUCER = PREFIX + ":producer";
    public static final String SUBJECT = PREFIX + ":subject";
    public static final String TITLE = PREFIX + ":title";

    public static final String PAGE_NUMBER = PREFIX + ":pageNumber";
    public static final String NAME = PREFIX + ":name";
}
