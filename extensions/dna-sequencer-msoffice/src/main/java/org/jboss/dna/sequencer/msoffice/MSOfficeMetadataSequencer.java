/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.msoffice;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jboss.dna.graph.sequencer.SequencerContext;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.sequencer.msoffice.excel.ExcelMetadata;
import org.jboss.dna.sequencer.msoffice.excel.ExcelMetadataReader;
import org.jboss.dna.sequencer.msoffice.powerpoint.PowerPointMetadataReader;
import org.jboss.dna.sequencer.msoffice.powerpoint.SlideMetadata;
import org.jboss.dna.sequencer.msoffice.word.WordMetadataReader;

/**
 * A sequencer that processes the content of an MS Office document, extracts the metadata for the file, and then writes that
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>msoffice:metadata</strong> node of type <code>msoffice:metadata</code>
 * <ul>
 * <li><strong>msoffice:title</strong> optional string property for the title of the documnt</li>
 * <li><strong>msoffice:subject</strong> optional string property for the subject of the document</li>
 * <li><strong>msoffice:author</strong> optional string property for the author of the document</li>
 * <li><strong>msoffice:keywords</strong> optional string property for the document keywords</li>
 * <li><strong>msoffice:comment</strong> optional string property for the document comment</li>
 * <li><strong>msoffice:template</strong> optional string property for the template from which this document originates</li>
 * <li><strong>msoffice:last_saved_by</strong> optional string property for the person that last saved this document</li>
 * <li><strong>msoffice:revision</strong> optional string property for this document revision</li>
 * <li><strong>msoffice:total_editing_time</strong> optional long property for the length this document has been edited</li>
 * <li><strong>msoffice:last_printed</strong> optional date property for the date of last printing this document</li>
 * <li><strong>msoffice:created</strong> date property for the date of creation of the document</li>
 * <li><strong>msoffice:saved</strong> date property for the date of last save of this document</li>
 * <li><strong>msoffice:pages</strong> long property for the number of pages of this document</li>
 * <li><strong>msoffice:words</strong> long property for the number of words in this document</li>
 * <li><strong>msoffice:characters</strong> long property for the number of characters in this document</li>
 * <li><strong>msoffice:creating_application</strong> string property for the application used to create this document</li>
 * <li><strong>msoffice:thumbnail</strong> optional binary property for the thumbanail of this document</li>
 * <li><strong>msoffice:full_contents</strong> optional String property holding the text contents of an excel file</li>
 * <li><strong>msoffice:sheet_name</strong> optional String property for the name of a sheet in excel (multiple)</li>
 * </ul>
 * </li>
 * <li><strong>msoffice:slide</strong> node of type <code>msoffice:pptslide</code>
 * <ul>
 * <li><strong>msoffice:title</strong> optional String property for the title of a slide</li>
 * <li><strong>msoffice:notes</strong> optional String property for the notes of a slide</li>
 * <li><strong>msoffice:text</strong> optional String property for the text of a slide</li>
 * <li><strong>msoffice:thumbnail</strong> optional binary property for the thumbnail of a slide (PNG image)</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @author Michael Trezzi
 * @author John Verhaeg
 */
public class MSOfficeMetadataSequencer implements StreamSequencer {

    public static final String METADATA_NODE = "msoffice:metadata";
    public static final String MSOFFICE_PRIMARY_TYPE = "jcr:primaryType";
    public static final String MSOFFICE_TITLE = "msoffice:title";
    public static final String MSOFFICE_SUBJECT = "msoffice:subject";
    public static final String MSOFFICE_AUTHOR = "msoffice:author";
    public static final String MSOFFICE_KEYWORDS = "msoffice:keywords";
    public static final String MSOFFICE_COMMENT = "msoffice:comment";
    public static final String MSOFFICE_TEMPLATE = "msoffice:template";
    public static final String MSOFFICE_LAST_SAVED_BY = "msoffice:last_saved_by";
    public static final String MSOFFICE_REVISION = "msoffice:revision";
    public static final String MSOFFICE_TOTAL_EDITING_TIME = "msoffice:total_editing_time";
    public static final String MSOFFICE_LAST_PRINTED = "msoffice:last_printed";
    public static final String MSOFFICE_CREATED = "msoffice:created";
    public static final String MSOFFICE_SAVED = "msoffice:saved";
    public static final String MSOFFICE_PAGES = "msoffice:pages";
    public static final String MSOFFICE_WORDS = "msoffice:words";
    public static final String MSOFFICE_CHARACTERS = "msoffice:characters";
    public static final String MSOFFICE_CREATING_APPLICATION = "msoffice:creating_application";
    public static final String MSOFFICE_THUMBNAIL = "msoffice:thumbnail";

    // PowerPoint specific
    public static final String POWERPOINT_SLIDE_NODE = "msoffice:slide";
    public static final String SLIDE_TITLE = "msoffice:title";
    public static final String SLIDE_TEXT = "msoffice:text";
    public static final String SLIDE_NOTES = "msoffice:notes";
    public static final String SLIDE_THUMBNAIL = "msoffice:thumbnail";

    // Excel specific
    public static final String EXCEL_FULL_CONTENT = "msoffice:full_contents";
    public static final String EXCEL_SHEET_NAME = "msoffice:sheet_name";

    /**
     * {@inheritDoc}
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context ) {

        MSOfficeMetadata metadata = MSOfficeMetadataReader.instance(stream);

        String mimeType = context.getMimeType();

        if (metadata != null) {
            output.setProperty(METADATA_NODE, MSOFFICE_PRIMARY_TYPE, "msoffice:metadata");
            output.setProperty(METADATA_NODE, MSOFFICE_TITLE, metadata.getTitle());
            output.setProperty(METADATA_NODE, MSOFFICE_SUBJECT, metadata.getSubject());
            output.setProperty(METADATA_NODE, MSOFFICE_AUTHOR, metadata.getAuthor());
            output.setProperty(METADATA_NODE, MSOFFICE_KEYWORDS, metadata.getKeywords());
            output.setProperty(METADATA_NODE, MSOFFICE_COMMENT, metadata.getComment());
            output.setProperty(METADATA_NODE, MSOFFICE_TEMPLATE, metadata.getTemplate());
            output.setProperty(METADATA_NODE, MSOFFICE_LAST_SAVED_BY, metadata.getLastSavedBy());
            output.setProperty(METADATA_NODE, MSOFFICE_REVISION, metadata.getRevision());
            output.setProperty(METADATA_NODE, MSOFFICE_TOTAL_EDITING_TIME, metadata.getTotalEditingTime());
            output.setProperty(METADATA_NODE, MSOFFICE_LAST_PRINTED, metadata.getLastPrinted());
            output.setProperty(METADATA_NODE, MSOFFICE_CREATED, metadata.getCreated());
            output.setProperty(METADATA_NODE, MSOFFICE_SAVED, metadata.getSaved());
            output.setProperty(METADATA_NODE, MSOFFICE_PAGES, metadata.getPages());
            output.setProperty(METADATA_NODE, MSOFFICE_WORDS, metadata.getWords());
            output.setProperty(METADATA_NODE, MSOFFICE_CHARACTERS, metadata.getCharacters());
            output.setProperty(METADATA_NODE, MSOFFICE_CREATING_APPLICATION, metadata.getCreatingApplication());
            output.setProperty(METADATA_NODE, MSOFFICE_THUMBNAIL, metadata.getThumbnail());

        } else {
            return;
        }

        // process PowerPoint specific metadata
        if (mimeType.equals("application/vnd.ms-powerpoint")) { // replace true with check if it's ppt file being sequenced
            try {
                List<SlideMetadata> ppt_metadata = PowerPointMetadataReader.instance(stream);
                if (ppt_metadata != null) {
                    for (SlideMetadata sm : ppt_metadata) {
                        output.setProperty(METADATA_NODE + "/" + POWERPOINT_SLIDE_NODE, SLIDE_TITLE, sm.getTitle());
                        output.setProperty(METADATA_NODE + "/" + POWERPOINT_SLIDE_NODE, SLIDE_TEXT, sm.getText());
                        output.setProperty(METADATA_NODE + "/" + POWERPOINT_SLIDE_NODE, SLIDE_NOTES, sm.getNotes());
                        output.setProperty(METADATA_NODE + "/" + POWERPOINT_SLIDE_NODE, SLIDE_THUMBNAIL, sm.getThumbnail());
                    }
                }
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getLogger(this.getClass()).debug(e, "Error while extracting the PowerPoint metadata");
            }
        }

        if (mimeType.equals("application/vnd.ms-word")) {
            // Sometime in the future this will sequence WORD Table of contents.
            try {
                /*WordMetadata wordMetadata =*/WordMetadataReader.invoke(stream);
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getLogger(this.getClass()).debug(e, "Error while extracting the Word document metadata");
            }

        }

        if (mimeType.equals("application/vnd.ms-excel")) {
            try {
                ExcelMetadata excel_metadata = ExcelMetadataReader.instance(stream);
                if (excel_metadata != null) {
                    output.setProperty(METADATA_NODE, EXCEL_FULL_CONTENT, excel_metadata.getText());
                    for (String sheet : excel_metadata.getSheets()) {
                        output.setProperty(METADATA_NODE, EXCEL_SHEET_NAME, sheet);
                    }
                }
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getLogger(this.getClass()).debug(e, "Error while extracting the Excel metadata");
            }
        }
    }
}
