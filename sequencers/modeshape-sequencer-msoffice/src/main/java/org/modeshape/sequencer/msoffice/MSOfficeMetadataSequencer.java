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

import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.AUTHOR;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.CHARACTERS;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.COMMENT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.CREATED;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.CREATING_APPLICATION;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.EXCEL_SHEET;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.EXCEL_SHEET_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.FULL_CONTENT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_LEVEL;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_NAME;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.KEYWORDS;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.LAST_PRINTED;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.NOTES;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.PAGES;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.REVISION;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SAVED;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SHEET_NAME;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SLIDE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SLIDE_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SUBJECT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TEMPLATE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TEXT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.THUMBNAIL;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TITLE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TOTAL_EDITING_TIME;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.WORDS;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadata;
import org.modeshape.sequencer.msoffice.excel.ExcelMetadataReader;
import org.modeshape.sequencer.msoffice.excel.ExcelSheetMetadata;
import org.modeshape.sequencer.msoffice.powerpoint.PowerPointMetadataReader;
import org.modeshape.sequencer.msoffice.powerpoint.PowerpointMetadata;
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
public class MSOfficeMetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("msoffice.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        InputStream inputStream = binaryValue.getStream();
        String mimeType = context.mimeTypeDetector().mimeTypeOf(getInputFileName(inputProperty), inputStream);

        Node sequencedNode = outputNode;
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(METADATA_NODE);
        } else {
            sequencedNode = outputNode.addNode(METADATA_NODE, METADATA_NODE);
        }

        sequencedNode.setProperty(JCR_MIME_TYPE, mimeType);
        if (isPowerpoint(mimeType)) {
            sequencePowerpoint(sequencedNode, context.valueFactory(), inputStream);
            return true;
        }

        if (isWord(mimeType)) {
            sequenceWord(sequencedNode, context.valueFactory(), inputStream);
            return true;
        }

        if (isExcel(mimeType)) {
            sequenceExcel(sequencedNode, context.valueFactory(), inputStream);
            return true;
        }

        getLogger().warn("Unknown mimetype: {0} for microsoft office", mimeType);
        return false;
    }

    private String getInputFileName( Property inputProperty ) throws RepositoryException {
        return inputProperty.getParent().getParent().getName();
    }

    private boolean isExcel( String mimeType ) {
        return MimeTypeConstants.MICROSOFT_EXCEL.equalsIgnoreCase(mimeType);
    }

    private void sequenceExcel( Node sequencedNode,
                                org.modeshape.jcr.api.ValueFactory valueFactory,
                                InputStream stream ) throws IOException, RepositoryException {
        ExcelMetadata excelMetadata = ExcelMetadataReader.instance(stream);
        recordMetadata(sequencedNode, valueFactory, excelMetadata.getMetadata());
        sequencedNode.setProperty(FULL_CONTENT, excelMetadata.getText());

        for (ExcelSheetMetadata sheetMetadata : excelMetadata.getSheets()) {
            Node sheet = sequencedNode.addNode(EXCEL_SHEET, EXCEL_SHEET_NODE);
            sheet.setProperty(SHEET_NAME, sheetMetadata.getName());
            sheet.setProperty(TEXT, sheetMetadata.getText());
        }
    }

    private boolean isWord( String mimeType ) {
        return MimeTypeConstants.MICROSOFT_WORD.equalsIgnoreCase(mimeType);
    }

    private void sequenceWord( Node rootNode,
                               org.modeshape.jcr.api.ValueFactory valueFactory,
                               InputStream stream ) throws RepositoryException, IOException {
        // Sometime in the future this will sequence WORD Table of contents.
        WordMetadata wordMetadata = WordMetadataReader.instance(stream);
        recordMetadata(rootNode, valueFactory, wordMetadata.getMetadata());

        for (WordMetadata.WordHeading headingMetadata : wordMetadata.getHeadings()) {
            Node heading = rootNode.addNode(HEADING_NODE, HEADING_NODE);
            heading.setProperty(HEADING_NAME, headingMetadata.getText());
            heading.setProperty(HEADING_LEVEL, headingMetadata.getHeaderLevel());
        }
    }

    private boolean isPowerpoint( String mimeType ) {
        return MimeTypeConstants.MICROSOFT_POWERPOINT.equalsIgnoreCase(mimeType);
    }

    private void sequencePowerpoint( Node rootNode,
                                     org.modeshape.jcr.api.ValueFactory valueFactory,
                                     InputStream stream ) throws IOException, RepositoryException {
        PowerpointMetadata deck = PowerPointMetadataReader.instance(stream);
        recordMetadata(rootNode, valueFactory, deck.getMetadata());

        for (SlideMetadata slideMetadata : deck.getSlides()) {
            Node slide = rootNode.addNode(SLIDE, SLIDE_NODE);
            slide.setProperty(TITLE, slideMetadata.getTitle());
            slide.setProperty(TEXT, slideMetadata.getText());
            slide.setProperty(NOTES, slideMetadata.getNotes());
            slide.setProperty(THUMBNAIL, valueFactory.createBinary(slideMetadata.getThumbnail()));
        }
    }

    private void recordMetadata( Node rootNode,
                                 org.modeshape.jcr.api.ValueFactory valueFactory,
                                 MSOfficeMetadata metadata ) throws RepositoryException {
        rootNode.setProperty(TITLE, metadata.getTitle());
        rootNode.setProperty(SUBJECT, metadata.getSubject());
        rootNode.setProperty(AUTHOR, metadata.getAuthor());
        rootNode.setProperty(KEYWORDS, metadata.getKeywords());
        rootNode.setProperty(COMMENT, metadata.getComment());
        rootNode.setProperty(TEMPLATE, metadata.getTemplate());
        rootNode.setProperty(SAVED, valueFactory.createValue(metadata.getLastSaved()));
        rootNode.setProperty(REVISION, metadata.getRevision());
        rootNode.setProperty(TOTAL_EDITING_TIME, metadata.getTotalEditingTime());
        rootNode.setProperty(LAST_PRINTED, valueFactory.createValue(metadata.getLastPrinted()));
        rootNode.setProperty(CREATED, valueFactory.createValue(metadata.getCreated()));
        rootNode.setProperty(PAGES, metadata.getPages());
        rootNode.setProperty(WORDS, metadata.getWords());
        rootNode.setProperty(CHARACTERS, metadata.getCharacters());
        rootNode.setProperty(CREATING_APPLICATION, metadata.getCreatingApplication());
        byte[] thumbnail = metadata.getThumbnail();
        if (thumbnail != null) {
            rootNode.setProperty(THUMBNAIL, valueFactory.createBinary(thumbnail));
        }
    }

}
