/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
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
