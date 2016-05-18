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

import static org.modeshape.sequencer.pdf.XmpMetadataLexicon.Namespace.PREFIX;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names in XMP standard namespace.
 */
@Immutable
public class XmpMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/xmp/1.0/";
        public static final String PREFIX = "xmp";
    }

    public static final String BASE_URL = PREFIX + ":baseURL";
    public static final String CREATE_DATE = PREFIX + ":createDate";
    public static final String CREATOR_TOOL = PREFIX + ":creatorTool";
    public static final String IDENTIFIER = PREFIX + ":identifier";
    public static final String METADATA_DATE = PREFIX + ":metadataDate";
    public static final String MODIFY_DATE = PREFIX + ":modifyDate";
    public static final String NICKNAME = PREFIX + ":nickname";
    public static final String RATING = PREFIX + ":rating";
    public static final String LABEL = PREFIX + ":label";
}
