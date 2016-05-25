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
package org.modeshape.sequencer.odf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A sequencer that processes the binary content of an OpenDocument file, extracts the metadata for the document, and then writes that
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>odf:metadata</strong> node of type <code>odf:metadata</code>
 * <ul>
 * <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the document</li>
 * <li><strong>odf:creationDate</strong> - optional date property specifying the creation date</li>
 * <li><strong>odf:creator</strong> - optional string property for the document's creator</li>
 * <li><strong>odf:description</strong> - optional string property for the document's description</li>
 * <li><strong>odf:editingCycles</strong> - optional long property for the number of editing cycles</li>
 * <li><strong>odf:editingTime</strong> - optional long property for the total editing time</li>
 * <li><strong>odf:generator</strong> - optional string for the generator of the document</li>
 * <li><strong>odf:initialCreator</strong> - optional string for the initial creator of the document</li>
 * <li><strong>odf:keywords</strong> - optional multi-valued string property for the keywords of the document</li>
 * <li><strong>odf:language</strong> - optional string property for the language of the document</li>
 * <li><strong>odf:modificationDate</strong> - optional date property for specifying the last modification date</li>
 * <li><strong>odf:printedBy</strong> - optional string property for the printedBy field</li>
 * <li><strong>odf:printDate</strong> - optional date property specifying the last print date</li>
 * <li><strong>odf:subject</strong> - optional string property for the document's subject</li>
 * <li><strong>odf:title</strong> - optional string property for the document's title</li>
 * <li><strong>odf:pages</strong> - optional long property specifying number of pages (documents and presentations)</li>
 * <li><strong>odf:sheets</strong> - optional long property specifying number of sheets (spreadsheets)</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @since 5.1
 */
public class OdfMetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("odf.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(OdfMetadata.MIME_TYPE_STRINGS);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = (Binary) inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        String mimeType = binaryValue.getMimeType();

        Node sequencedNode = getMetadataNode(outputNode);
        setPropertyIfMetadataPresent(sequencedNode, JcrConstants.JCR_MIME_TYPE, mimeType);
        return processBasicMetadata(sequencedNode, binaryValue);
    }

    private boolean processBasicMetadata( Node sequencedNode,
                                          Binary binaryValue ) {
        OdfMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new OdfMetadata(stream);
            if (metadata.check()) {
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.PAGES, metadata.getPages());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.SHEETS, metadata.getSheets());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.CREATION_DATE, metadata.getCreationDate());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.CREATOR, metadata.getCreator());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.DESCRIPTION, metadata.getDescription());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.EDITING_CYCLES, metadata.getEditingCycles());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.EDITING_TIME, metadata.getEditingTime());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.GENERATOR, metadata.getGenerator());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.INITIAL_CREATOR, metadata.getInitialCreator());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.KEYWORDS, metadata.getKeywords());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.LANGUAGE, metadata.getLanguage());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.MODIFICATION_DATE, metadata.getModificationDate());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.PRINTED_BY, metadata.getPrintedBy());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.PRINT_DATE, metadata.getPrintDate());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.SUBJECT, metadata.getSubject());
                setPropertyIfMetadataPresent(sequencedNode, OdfMetadataLexicon.TITLE, metadata.getTitle());

                return true;
            }
        } catch (Exception e) {
            getLogger().error(e, "Couldn't process stream.");
        }
        return false;
    }

    private Node getMetadataNode( Node outputNode ) throws RepositoryException {
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(OdfMetadataLexicon.METADATA_NODE);
            return outputNode;
        }
        return outputNode.addNode(OdfMetadataLexicon.METADATA_NODE, OdfMetadataLexicon.METADATA_NODE);
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
                node.setProperty(propertyName, (Calendar) value); 
            } else if (value instanceof byte[]) {
                InputStream is = new ByteArrayInputStream((byte []) value);
                javax.jcr.Binary binaryProperty = node.getSession().getValueFactory().createBinary(is);
                node.setProperty(propertyName, binaryProperty);
            } else if (value instanceof List) {
                ValueFactory vf = node.getSession().getValueFactory();
                List<Value> values = ((List<?>) value).stream().filter(val -> val instanceof String)
                                                      .map(val -> vf.createValue((String) val)).collect(Collectors.toList());
                if (!values.isEmpty()) {
                    node.setProperty(propertyName, values.toArray(new Value[values.size()]));
                }
            } else {
                getLogger().warn("The value of the property {0} has unknown type and couldn't be saved.", propertyName);
            }
        }
    }
}
