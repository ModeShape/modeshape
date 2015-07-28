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
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.Binary;
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

    public static final class MimeTypeConstants {
        public static final String MICROSOFT_APPLICATION_MS_WORD = "application/msword";
        public static final String MICROSOFT_WORD = "application/vnd.ms-word";
        public static final String MICROSOFT_EXCEL = "application/vnd.ms-excel";
        public static final String MICROSOFT_POWERPOINT = "application/vnd.ms-powerpoint";
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("msoffice.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.MICROSOFT_EXCEL,
                                 MimeTypeConstants.MICROSOFT_POWERPOINT,
                                 MimeTypeConstants.MICROSOFT_WORD,
                                 MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = (Binary)inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        String inputFileName = getInputFileName(inputProperty);
        String mimeType = binaryValue.getMimeType(inputFileName);

        Node sequencedNode = outputNode;
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(METADATA_NODE);
        } else {
            sequencedNode = outputNode.addNode(METADATA_NODE, METADATA_NODE);
        }

        if (mimeType != null) {
            setProperty(sequencedNode, JCR_MIME_TYPE, mimeType);
        }
        
        if (isPowerpoint(mimeType)) {
            try (InputStream stream = binaryValue.getStream()) {
                sequencePowerpoint(sequencedNode, context.valueFactory(), stream);
                return true;
            }
        }

        if (isWord(mimeType)) {
            try (InputStream stream = binaryValue.getStream()) {
                sequenceWord(sequencedNode, context.valueFactory(), stream);
                return true;
            }
        }

        if (isExcel(mimeType)) {
            try (InputStream stream = binaryValue.getStream()) {
                sequenceExcel(sequencedNode, context.valueFactory(), stream);
                return true;
            }
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
        setProperty(sequencedNode, FULL_CONTENT, excelMetadata.getText());

        for (ExcelSheetMetadata sheetMetadata : excelMetadata.getSheets()) {
            Node sheet = sequencedNode.addNode(EXCEL_SHEET, EXCEL_SHEET_NODE);
            setProperty(sheet, SHEET_NAME, sheetMetadata.getName());
            setProperty(sheet, TEXT, sheetMetadata.getText());
        }
    }

    private boolean isWord( String mimeType ) {
        // See http://blogs.msdn.com/b/vsofficedeveloper/archive/2008/05/08/office-2007-open-xml-mime-types.aspx
        return MimeTypeConstants.MICROSOFT_WORD.equalsIgnoreCase(mimeType)
               || MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD.equalsIgnoreCase(mimeType);
    }

    private void sequenceWord( Node rootNode,
                               org.modeshape.jcr.api.ValueFactory valueFactory,
                               InputStream stream ) throws RepositoryException, IOException {
        // Sometime in the future this will sequence WORD Table of contents.
        WordMetadata wordMetadata = WordMetadataReader.instance(stream);
        recordMetadata(rootNode, valueFactory, wordMetadata.getMetadata());

        for (WordMetadata.WordHeading headingMetadata : wordMetadata.getHeadings()) {
            Node heading = rootNode.addNode(HEADING_NODE, HEADING_NODE);
            setProperty(heading, HEADING_NAME, headingMetadata.getText());
            setProperty(heading, HEADING_LEVEL, headingMetadata.getHeaderLevel());
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
            setProperty(slide, TITLE, slideMetadata.getTitle());
            setProperty(slide, TEXT, slideMetadata.getText());
            setProperty(slide, NOTES, slideMetadata.getNotes());
            setProperty(slide, THUMBNAIL, valueFactory.createBinary(slideMetadata.getThumbnail()));
        }
    }

    private void recordMetadata( Node rootNode,
                                 org.modeshape.jcr.api.ValueFactory valueFactory,
                                 MSOfficeMetadata metadata ) throws RepositoryException {
        setProperty(rootNode, TITLE, metadata.getTitle());
        setProperty(rootNode, SUBJECT, metadata.getSubject());
        setProperty(rootNode, AUTHOR, metadata.getAuthor());
        setProperty(rootNode, KEYWORDS, metadata.getKeywords());
        setProperty(rootNode, COMMENT, metadata.getComment());
        setProperty(rootNode, TEMPLATE, metadata.getTemplate());
        setProperty(rootNode, SAVED, valueFactory.createValue(metadata.getLastSaved()));
        setProperty(rootNode, REVISION, metadata.getRevision());
        setProperty(rootNode, TOTAL_EDITING_TIME, metadata.getTotalEditingTime());
        setProperty(rootNode, LAST_PRINTED, valueFactory.createValue(metadata.getLastPrinted()));
        setProperty(rootNode, CREATED, valueFactory.createValue(metadata.getCreated()));
        setProperty(rootNode, PAGES, metadata.getPages());
        setProperty(rootNode, WORDS, metadata.getWords());
        setProperty(rootNode, CHARACTERS, metadata.getCharacters());
        setProperty(rootNode, CREATING_APPLICATION, metadata.getCreatingApplication());
        setProperty(rootNode, THUMBNAIL, valueFactory.createBinary(metadata.getThumbnail()));
    }

    private void setProperty( Node node,
                              String propertyName,
                              String value ) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value);
        }
    }

    private void setProperty( Node node,
                              String propertyName,
                              Value value ) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value);
        }
    }

    private void setProperty( Node node,
                              String propertyName,
                              Binary value ) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value);
        }
    }

    // Intentionally use the Long object form, in case this is called by methods that return a null Long reference
    // for optional values
    private void setProperty( Node node,
                              String propertyName,
                              Long value ) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value.longValue());
        }
    }

    // Intentionally use the Integer object form, in case this is called by methods that return a null Integer reference
    // for optional values
    private void setProperty( Node node,
                              String propertyName,
                              Integer value ) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value.longValue());
        }
    }

}
