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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A sequencer that processes the binary content of an PDF file, extracts the metadata, and then writes that
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>pdf:metadata</strong> node of type <code>pdf:metadata</code>
 *  <ul>
 *   <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the image</li>
 *   <li><strong>pdf:pageCount</strong> - mandatory long property specifying number of pages</li>
 *   <li><strong>pdf:encrypted</strong> - mandatory boolean property specifying whether the document is encrypted</li>
 *   <li><strong>pdf:version</strong> - mandatory string property for the version of the PDF format</li>
 *   <li><strong>pdf:orientation</strong> - mandatory string property specifying the orientation of the paper (landscape, portrait, reverse landscape)</li>
 *   <li><strong>pdf:author</strong> - optional string property for the author of the document</li>
 *   <li><strong>pdf:creationDate</strong> - optional date property for the creation date of the document</li>
 *   <li><strong>pdf:creator</strong> - optional string property for the creator of the document</li>
 *   <li><strong>pdf:keywords</strong> - optional string property for the keywords of the document (comma delimited)</li>
 *   <li><strong>pdf:modificationDate</strong> - optional date property for the modification date of document</li>
 *   <li><strong>pdf:producer</strong> - optional string property for the producer of the document</li>
 *   <li><strong>pdf:subject</strong> - optional string property for the subject of the document</li>
 *   <li><strong>pdf:title</strong> - optional string property for the title of the document</li>
 *   <li><strong>pdf:xmp</strong> - optional child node for the metadata fields from XMP block
 *    <ul>
 *     <li><strong>xmp:baseURL</strong> - optional string property for the baseURL</li>
 *     <li><strong>xmp:createDate</strong> - optional date property for modification date of this object</li>
 *     <li><strong>xmp:creatorTool</strong> - optional string property specifying the creator tool used to make this document</li></li>
 *     <li><strong>xmp:identifier</strong> - optional multi-valued string property for the identifiers of the object</li>
 *     <li><strong>xmp:label</strong> - optional string property for the label of the object</li>
 *     <li><strong>xmp:metadataDate</strong> - optional date property for creation date of this metadata</li>
 *     <li><strong>xmp:modifyDate</strong> - optional date property for modification date of this object</li>
 *     <li><strong>xmp:nickname</strong> - optional string property for the nickname</li>
 *     <li><strong>xmp:rating</strong> - optional string property for the nickname</li>
 *     <li><strong>xmp:label</strong> - optional string property for the label</li>
 *     </ul>
 *   </li>
 *   <li><strong>pdf:page</strong> - optional child node for the metadata fields related to individual pages
 *    <ul>
 *     <li><strong>pdf:pageNumber</strong> - mandatory long property for the number of this page</li>
 *     <li><strong>pdf:attachement</strong> - optional child node for the metadata fields related to attachment
 *      <ul>
 *       <li><strong>pdf:creationDate</strong> - optional date property for creation date of this attachment</li>
 *       <li><strong>pdf:modificationDate</strong> - optional date property for modification date of this attachment</li>
 *       <li><strong>pdf:subject</strong> - optional string property for the subject of this attachment</li>
 *       <li><strong>pdf:name</strong> - optional string property for the name of this attachment</li>
 *       <li><strong>jcr:mimeType</strong> - optional string property for the mime type of this attachment</li>
 *       <li><strong>jcr:data</strong> - optional binary property for the content of this attachment</li>
 *      </ul>
 *     </li>
 *    </ul>
 *   </li>
 *  </ul>
 * </p>
 * 
 * @since 5.1
 */
public class PdfMetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("pdf.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(PdfBasicMetadata.MIME_TYPE_STRING);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        Node sequencedNode = getPdfMetadataNode(outputNode);
        try {
            if (processBasicMetadata(sequencedNode, binaryValue)) {
                processXMPMetadata(sequencedNode, binaryValue);
                return true;
            } else {
                getLogger().warn("Ignoring pdf from node {0} because basic metadata cannot be extracted",
                                 inputProperty.getParent().getPath());
                return false;
            }
        } catch (java.lang.NoClassDefFoundError ncdfe) {
            if (ncdfe.getMessage().toLowerCase().contains("bouncycastle")) {
                getLogger().warn("Ignoring pdf from node {0} because it's encrypted and encrypted PDFs are not supported", 
                                 inputProperty.getParent().getPath());
                return false;
            }
            throw ncdfe;
        }
    }

    private boolean processBasicMetadata( Node sequencedNode,
                                          Binary binaryValue) {
        PdfBasicMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new PdfBasicMetadata(stream);
            if (metadata.check()) {
                setPropertyIfMetadataPresent(sequencedNode, JcrConstants.JCR_MIME_TYPE, PdfBasicMetadata.MIME_TYPE_STRING);

                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.PAGE_COUNT, metadata.getPageCount());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.ORIENTATION, metadata.getOrientation());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.ENCRYPTED, metadata.isEncrypted());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.VERSION, metadata.getVersion());

                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.AUTHOR, metadata.getAuthor());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.CREATION_DATE, metadata.getCreationDate());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.CREATOR, metadata.getCreator());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.KEYWORDS, metadata.getKeywords());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.MODIFICATION_DATE, metadata.getModificationDate());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.PRODUCER, metadata.getProducer());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.SUBJECT, metadata.getSubject());
                setPropertyIfMetadataPresent(sequencedNode, PdfMetadataLexicon.TITLE, metadata.getTitle());

                for (PdfPageMetadata pageMetadata : metadata.getPages()) {
                    Node pageNode = sequencedNode.addNode(PdfMetadataLexicon.PAGE_NODE, PdfMetadataLexicon.PAGE_NODE);

                    setPropertyIfMetadataPresent(pageNode, PdfMetadataLexicon.PAGE_NUMBER, pageMetadata.getPageNumber());
                    for (PdfAttachmentMetadata attachmentMetadata : pageMetadata.getAttachments()) {
                        Node attachmentNode = pageNode.addNode(PdfMetadataLexicon.ATTACHMENT_NODE, PdfMetadataLexicon.ATTACHMENT_NODE);

                        setPropertyIfMetadataPresent(attachmentNode, JcrConstants.JCR_MIME_TYPE, attachmentMetadata.getMimeType());
                        setPropertyIfMetadataPresent(attachmentNode, PdfMetadataLexicon.CREATION_DATE, attachmentMetadata.getCreationDate());
                        setPropertyIfMetadataPresent(attachmentNode, PdfMetadataLexicon.MODIFICATION_DATE, attachmentMetadata.getModificationDate());
                        setPropertyIfMetadataPresent(attachmentNode, PdfMetadataLexicon.SUBJECT, attachmentMetadata.getSubject());
                        setPropertyIfMetadataPresent(attachmentNode, PdfMetadataLexicon.NAME, attachmentMetadata.getName());
                        setPropertyIfMetadataPresent(attachmentNode, JcrConstants.JCR_DATA, attachmentMetadata.getData());
                    }
                }
                return true;
            }
        } catch (Exception e) {
            getLogger().error(e, "Couldn't process stream.");
        }
        return false;
    }

    private boolean processXMPMetadata( Node sequencedNode,
                                        Binary binaryValue) {
        PdfXmpMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new PdfXmpMetadata(stream);
            if (metadata.check()) {
                Node xmpNode = sequencedNode.addNode(PdfMetadataLexicon.XMP_NODE, PdfMetadataLexicon.XMP_NODE);

                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.BASE_URL, metadata.getBaseURL());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.CREATE_DATE, metadata.getCreateDate());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.CREATOR_TOOL, metadata.getCreatorTool());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.IDENTIFIER, metadata.getIdentifier());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.METADATA_DATE, metadata.getMetadataDate());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.MODIFY_DATE, metadata.getModifyDate());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.NICKNAME, metadata.getNickname());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.RATING, metadata.getRating());
                setPropertyIfMetadataPresent(xmpNode, XmpMetadataLexicon.LABEL, metadata.getLabel());
                return true;
            }
        } catch (Exception e) {
            getLogger().error(e, "Couldn't process stream.");
        }

        return false;
    }

    private Node getPdfMetadataNode( Node outputNode ) throws RepositoryException {
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(PdfMetadataLexicon.METADATA_NODE);
            return outputNode;
        }
        return outputNode.addNode(PdfMetadataLexicon.METADATA_NODE, PdfMetadataLexicon.METADATA_NODE);
    }

    private void setPropertyIfMetadataPresent( Node node,
                                               String propertyName,
                                               Object value ) throws RepositoryException {
        if (value != null) {
            if (value instanceof String && !StringUtil.isBlank((String) value)) {
                node.setProperty(propertyName, (String) value);
            } else if (value instanceof Boolean) {
                node.setProperty(propertyName, (Boolean) value);
            } else if (value instanceof Long) {
                node.setProperty(propertyName, (Long) value);
            } else if (value instanceof Integer) {
                node.setProperty(propertyName, new Long((Integer) value));
            } else if (value instanceof Calendar) {
                // pdfbox 1.8.x doesn't parse the timezones correctly...
                // see PDFBOX-3352
                Calendar calendarValue = (Calendar) value;
                if (calendarValue.getTimeZone().getID().toLowerCase().equals("unknown")) {
                    calendarValue.setTimeZone(TimeZone.getDefault());
                }
                node.setProperty(propertyName, calendarValue);
            } else if (value instanceof byte[]) {
                InputStream is = new ByteArrayInputStream((byte []) value);
                Binary binaryProperty = node.getSession().getValueFactory().createBinary(is);
                node.setProperty(propertyName, binaryProperty);
            } else if (value instanceof List<?>) {
                ValueFactory vf = node.getSession().getValueFactory();
                List<Value> values = ((List<?>) value).stream()
                                                      .filter(val -> val instanceof String)
                                                      .map(val -> vf.createValue((String) val))
                                                      .collect(Collectors.toList());
                if (!values.isEmpty()) {
                    node.setProperty(propertyName, values.toArray(new Value[values.size()]));
                }
            } else {
                throw new IllegalArgumentException(String.format("The value of the property %s has unknown type and couldn't be saved.", propertyName));
            }
        }
    }
}
