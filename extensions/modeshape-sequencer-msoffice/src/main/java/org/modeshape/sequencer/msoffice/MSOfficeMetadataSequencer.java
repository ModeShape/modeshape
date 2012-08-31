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
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadata;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadataReader;
import org.modeshape.sequencer.msoffice.excel.ExcelSheetMetadata;
import org.modeshape.sequencer.msoffice.powerpoint.PowerPointMetadataReader;
import org.modeshape.sequencer.msoffice.powerpoint.SlideDeckMetadata;
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
    @Override
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {

        String mimeType = context.getMimeType();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        Path inputPath = context.getInputPath();
        Name docName = inputPath.getLastSegment().getName();
        if (!inputPath.isRoot()) {
            // Remove the 'jcr:content' node (of type 'nt:resource'), if it is there ...
            if (docName.equals(JcrLexicon.CONTENT)) {
                inputPath = inputPath.getParent();
                if (!inputPath.isRoot()) {
                    docName = inputPath.getLastSegment().getName();
                }
            }
        }

        Path docNode = pathFactory.createRelativePath(docName);
        //the next call registers the doc node with the output, so the derived properties will be set
        output.setProperty(docNode, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
        Path metadataNode = pathFactory.create(docNode, MSOfficeMetadataLexicon.METADATA_NODE);
        output.setProperty(metadataNode, JcrLexicon.MIMETYPE, mimeType);
        // process PowerPoint specific metadata
        if (mimeType.equals("application/vnd.ms-powerpoint") || mimeType.equals("application/mspowerpoint")) {
            try {
                SlideDeckMetadata deck = PowerPointMetadataReader.instance(stream);
                List<SlideMetadata> ppt_metadata = deck.getHeadings();
                recordMetadata(output, context, metadataNode, deck.getMetadata());
                if (ppt_metadata != null) {
                    BinaryFactory binary = context.getValueFactories().getBinaryFactory();
                    int index = 0;
                    for (SlideMetadata sm : ppt_metadata) {
                        Path pptPath = pathFactory.create(docNode, MSOfficeMetadataLexicon.SLIDE, ++index);
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.TITLE, sm.getTitle());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.TEXT, sm.getText());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.NOTES, sm.getNotes());
                        output.setProperty(pptPath, MSOfficeMetadataLexicon.THUMBNAIL, binary.create(sm.getThumbnail()));
                    }
                }
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getProblems().addError(e, MSOfficeMetadataI18n.errorExtractingPowerpointMetadata, e.getMessage());
            }
        }

        if (mimeType.equals("application/vnd.ms-word") || mimeType.equals("application/msword")) {
            // Sometime in the future this will sequence WORD Table of contents.
            try {
                WordMetadata wordMetadata = WordMetadataReader.instance(stream);
                recordMetadata(output, context, metadataNode, wordMetadata.getMetadata());

                int index = 0;
                for (Iterator<WordMetadata.WordHeading> iter = wordMetadata.getHeadings().iterator(); iter.hasNext();) {
                    WordMetadata.WordHeading heading = iter.next();
                    Path wordPath = pathFactory.create(docNode, MSOfficeMetadataLexicon.HEADING_NODE, ++index);
                    output.setProperty(wordPath, MSOfficeMetadataLexicon.HEADING_NAME, heading.getText());
                    output.setProperty(wordPath, MSOfficeMetadataLexicon.HEADING_LEVEL, heading.getHeaderLevel());
                }

            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getProblems().addError(e, MSOfficeMetadataI18n.errorExtractingWordMetadata, e.getMessage());
            }

        }

        if (mimeType.equals("application/vnd.ms-excel") || mimeType.equals("application/msexcel")) {
            try {
                ExcelMetadata excel_metadata = ExcelMetadataReader.instance(stream);
                if (excel_metadata != null) {
                    recordMetadata(output, context, metadataNode, excel_metadata.getMetadata());
                    output.setProperty(metadataNode, MSOfficeMetadataLexicon.FULL_CONTENT, excel_metadata.getText());
                    for (ExcelSheetMetadata sheet : excel_metadata.getSheets()) {
                        Path sheetPath = pathFactory.create(docNode, MSOfficeMetadataLexicon.SHEET);
                        output.setProperty(sheetPath, MSOfficeMetadataLexicon.SHEET_NAME, sheet.getName());
                        output.setProperty(sheetPath, MSOfficeMetadataLexicon.TEXT, sheet.getText());
                    }
                }
            } catch (IOException e) {
                // There was an error reading, so log and continue ...
                context.getProblems().addError(e, MSOfficeMetadataI18n.errorExtractingExcelMetadata, e.getMessage());
            }
        }
    }

    protected void recordMetadata( SequencerOutput output,
                                   StreamSequencerContext context,
                                   Path metadataNode,
                                   MSOfficeMetadata metadata ) {
        if (metadata != null) {
            DateTimeFactory dates = context.getValueFactories().getDateFactory();
            BinaryFactory binary = context.getValueFactories().getBinaryFactory();
            output.setProperty(metadataNode, JcrLexicon.PRIMARY_TYPE, MSOfficeMetadataLexicon.METADATA_NODE);
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TITLE, metadata.getTitle());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.SUBJECT, metadata.getSubject());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.AUTHOR, metadata.getAuthor());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.KEYWORDS, metadata.getKeywords());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.COMMENT, metadata.getComment());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TEMPLATE, metadata.getTemplate());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.SAVED, dates.create(metadata.getLastSaved()));
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.REVISION, metadata.getRevision());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.TOTAL_EDITING_TIME, metadata.getTotalEditingTime());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.LAST_PRINTED, dates.create(metadata.getLastPrinted()));
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CREATED, dates.create(metadata.getCreated()));
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.PAGES, metadata.getPages());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.WORDS, metadata.getWords());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CHARACTERS, metadata.getCharacters());
            output.setProperty(metadataNode, MSOfficeMetadataLexicon.CREATING_APPLICATION, metadata.getCreatingApplication());
            byte[] thumbnail = metadata.getThumbnail();
            if (thumbnail != null && thumbnail.length > 0) {
                output.setProperty(metadataNode, MSOfficeMetadataLexicon.THUMBNAIL, binary.create(thumbnail));
            }
        }
    }
}
