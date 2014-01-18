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
package org.modeshape.sequencer.msoffice;

import org.modeshape.common.annotation.Immutable;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.Namespace.PREFIX;


/**
 * A lexicon of names used within the MS Office sequencer.
 */
@Immutable
public class MSOfficeMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/msoffice/1.0";
        public static final String PREFIX = "msoffice";
    }

    public static final String METADATA_NODE = PREFIX + ":metadata";
    public static final String TITLE = PREFIX + ":title";
    public static final String SUBJECT = PREFIX + ":subject";
    public static final String AUTHOR = PREFIX + ":author";
    public static final String KEYWORDS = PREFIX + ":keywords";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String TEMPLATE = PREFIX + ":template";
    public static final String LAST_SAVED = PREFIX + ":last_saved";
    public static final String REVISION = PREFIX + ":revision";
    public static final String TOTAL_EDITING_TIME = PREFIX + ":total_editing_time";
    public static final String LAST_PRINTED = PREFIX + ":last_printed";
    public static final String CREATED = PREFIX + ":created";
    public static final String SAVED = PREFIX + ":saved";
    public static final String PAGES = PREFIX + ":pages";
    public static final String WORDS = PREFIX + ":words";
    public static final String CHARACTERS = PREFIX + ":characters";
    public static final String CREATING_APPLICATION = PREFIX + ":creating_application";
    public static final String THUMBNAIL = PREFIX + ":thumbnail";
    public static final String SLIDE = PREFIX + ":slide";
    public static final String TEXT = PREFIX + ":text";
    public static final String NOTES = PREFIX + ":notes";
    public static final String FULL_CONTENT = PREFIX + ":full_content";
    public static final String SHEET_NAME = PREFIX + ":sheet_name";
    public static final String HEADING_NODE = PREFIX + ":heading";
    public static final String HEADING_NAME = PREFIX + ":heading_name";
    public static final String HEADING_LEVEL = PREFIX + ":heading_level";
    public static final String SLIDE_NODE = PREFIX + ":pptslide";
    public static final String EXCEL_SHEET_NODE = PREFIX + ":xlssheet";
    public static final String EXCEL_SHEET= PREFIX + ":sheet";

}
