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
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.msoffice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadata;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadataReader;
import org.modeshape.sequencer.msoffice.powerpoint.PowerPointMetadataReader;
import org.modeshape.sequencer.msoffice.powerpoint.SlideMetadata;
import org.modeshape.sequencer.msoffice.word.WordMetadata;
import org.modeshape.sequencer.msoffice.word.WordMetadataReader;

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
 */
public class MSOfficeMetadataSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {

        MSOfficeMetadata metadata = MSOfficeMetadataReader.instance(stream);

        String mimeType = context.getMimeType();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path metadataNode = pathFactory.create(MSOfficeMetadataLexicon.METADATA_NODE);

        if (metadata != null) {
            output.setProperty(metadataNode, JcrLexicon.PRIMARY_TYPE, MSOfficeMetadataLexicon.METADATA_NODE);
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TITLE, metadata.getTitle());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.SUBJECT, metadata.getSubject());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.AUTHOR, metadata.getAuthor());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.KEYWORDS, metadata.getKeywords());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.COMMENT, metadata.getComment());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TEMPLATE, metadata.getTemplate());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.LAST_SAVED_BY, metadata.getLastSavedBy());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.REVISION, metadata.getRevision());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TOTAL_EDITING_TIME, metadata.getTotalEditingTime());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.LAST_PRINTED, metadata.getLastPrinted());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CREATED, metadata.getCreated());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.SAVED, metadata.getSaved());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.PAGES, metadata.getPages());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.WORDS, metadata.getWords());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CHARACTERS, metadata.getCharacters());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CREATING_APPLICATION, metadata.getCreatingApplication());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.THUMBNAIL, metadata.getThumbnail());

        } else {
            return;
        }

        // process PowerPoint specific metadata
        if (mimeType.equals("application/vnd.ms-powerpoint")) { // replace true with check if it's ppt file being sequenced
            try {
                List<SlideMetadata> ppt_metadata = PowerPointMetadataReader.instance(stream);
                if (ppt_metadata != null) {
                    Path pptPath = pathFactory.create(metadataNode, MSOfficeMetadataLexicon.SLIDE);
                    for (SlideMetadata sm : ppt_metadata) {
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.TITLE, sm.getTitle());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.TEXT, sm.getText());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.NOTES, sm.getNotes());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.THUMBNAIL, sm.getThumbnail());
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
                WordMetadata wordMetadata = WordMetadataReader.instance(stream);
                Path wordPath = pathFactory.create(metadataNode, MSOfficeMetadataLexicon.HEADING_NODE);

                for (Iterator<WordMetadata.WordHeading> iter = wordMetadata.getHeadings().iterator(); iter.hasNext();) {
                    WordMetadata.WordHeading heading = iter.next();

                    output.setProperty(wordPath, MSOfficeMetadataLexicon.HEADING_NAME, heading.getText());
                    output.setProperty(wordPath, MSOfficeMetadataLexicon.HEADING_LEVEL, heading.getHeaderLevel());

                }

            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getLogger(this.getClass()).debug(e, "Error while extracting the Word document metadata");
            }

        }

        if (mimeType.equals("application/vnd.ms-excel")) {
            try {
                ExcelMetadata excel_metadata = ExcelMetadataReader.instance(stream);
                if (excel_metadata != null) {
                    output.setProperty(metadataNode, MSOfficeMetadataLexicon.FULL_CONTENT, excel_metadata.getText());
                    for (String sheet : excel_metadata.getSheets()) {
                        output.setProperty(metadataNode, MSOfficeMetadataLexicon.SHEET_NAME, sheet);
                    }
                }
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getLogger(this.getClass()).debug(e, "Error while extracting the Excel metadata");
            }
        }
    }
}
