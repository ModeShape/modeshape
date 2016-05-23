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
package org.modeshape.sequencer.epub;

import static org.modeshape.sequencer.epub.EpubMetadataLexicon.METADATA_NODE;
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
 * A sequencer that processes the binary content of an EPUB 3.0 file, extracts
 * the metadata, and then writes that metadata to the repository.
 * <p>
 *  This sequencer produces data that corresponds to the following structure:
 *  <ul>
 *   <li><strong>epub:metadata</strong> node of type <code>epub:metadata</code>
 *    <ul>
 *     <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the EPUB</li>
 *     <li><strong>epub:identifier</strong> - optional <code>epub:identifier</code> node for the identifier</li>
 *     <li><strong>epub:title</strong> - optional <code>epub:title</code> node for the title</li>
 *     <li><strong>epub:language</strong> - optional <code>epub:language</code> node for the language</li>
 *     <li><strong>epub:contributor</strong> - optional <code>epub:contributor</code> node for the contributor</li>
 *     <li><strong>epub:creator</strong> - optional <code>epub:creator</code> node for the creator</li>
 *     <li><strong>epub:description</strong> - optional <code>epub:description</code> node for the description</li>
 *     <li><strong>epub:publisher</strong> - optional <code>epub:publisher</code> node for the publisher</li>
 *     <li><strong>epub:rights</strong> - optional <code>epub:rights</code> node for the rights</li>
 *     <li><strong>epub:date</strong> - optional <code>epub:date</code> node for the date date</li>
 *    </ul>
 *   </li>
 *   <li><strong>epub:*</strong> nodes are descendants of <code>epub:property</code> which has following structure:
 *    <ul>
 *     <li><strong>epub:value</strong> - optional string property for the value of the property</li>
 *     <li><strong>epub:titleType</strong> - optional string property for the title-type of the property</li>
 *     <li><strong>epub:identifierType</strong> - optional string property for the identifier-type of the property</li>
 *     <li><strong>epub:metadataAuthority</strong> - optional string property for the metadata-authority field of the property</li>
 *     <li><strong>epub:role</strong> - optional string property for the role of the property</li>
 *     <li><strong>epub:displaySeq</strong> - optional long property for the display-sequence field of the property</li>
 *     <li><strong>epub:fileAs</strong> - optional string property for the file-as field of the property</li>
 *     <li><strong>epub:groupPosition</strong> - optional long property for the group-position field of the property</li>
 *     <li><strong>epub:scheme</strong> - optional string property for the scheme of the property</li>
 *     <li><strong>epub:alternateScript</strong> - optional <code>epub:alternateScript</code> node for the alternate transcriptions of the property
 *      <ul>
 *       <li><strong>epub:value</strong> - optional string property for the value of the alternate script</li>
 *       <li><strong>epub:languageCode</strong> - optional string property for the value of the alternate script</li>
 *      </ul>
 *     </li>
 *    </ul>
 *   </li>
 *  </ul>
 * </p>
 * 
 * @since 5.1
 */
public class EpubMetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("epub.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(EpubMetadata.MIME_TYPE_STRINGS);
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
        EpubMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new EpubMetadata(stream);
            if (metadata.check()) {
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.TITLE, metadata.getTitle());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.CREATOR, metadata.getCreator());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.CONTRIBUTOR, metadata.getContributor());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.LANGUAGE, metadata.getLanguage());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.IDENTIFIER, metadata.getIdentifier());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.DESCRIPTION, metadata.getDescription());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.PUBLISHER, metadata.getPublisher());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.DATE, metadata.getDate());
                addEpubMetadataProperties(sequencedNode, EpubMetadataLexicon.RIGHTS, metadata.getRights());
                return true;
            }
        } catch (Exception e) {
            getLogger().error(e, "Couldn't process stream.");
        }
        return false;
    }

    private Node getMetadataNode( Node outputNode ) throws RepositoryException {
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(METADATA_NODE);
            return outputNode;
        }
        return outputNode.addNode(METADATA_NODE, METADATA_NODE);
    }

    private void addEpubMetadataProperties( Node node,
                                            String propertyName,
                                            List<EpubMetadataProperty> values ) throws RepositoryException {
        for (EpubMetadataProperty value : values) {
            Node propertyNode = node.addNode(propertyName, propertyName);

            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.VALUE, value.getValue());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.TITLE_TYPE, value.getTitleType());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.METADATA_AUTHORITY, value.getMetadataAuthority());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.ROLE, value.getRole());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.DISPLAY_SEQ, value.getDisplaySeq());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.FILE_AS, value.getFileAs());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.GROUP_POSITION, value.getGroupPosition());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.IDENTIFIER_TYPE, value.getIdentifierType());
            setPropertyIfMetadataPresent(propertyNode, EpubMetadataLexicon.SCHEME, value.getScheme());

            if (value.getAlternateScript() != null) {
                Node alternateScriptNode = propertyNode.addNode(EpubMetadataLexicon.ALTERNATE_SCRIPT_NODE, EpubMetadataLexicon.ALTERNATE_SCRIPT_NODE);
                setPropertyIfMetadataPresent(alternateScriptNode, EpubMetadataLexicon.VALUE, value.getAlternateScript().getValue());
                setPropertyIfMetadataPresent(alternateScriptNode, EpubMetadataLexicon.LANGUAGE_CODE, value.getAlternateScript().getLanguage());
            }
        }
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
            } else if (value instanceof Double) {
                node.setProperty(propertyName, (Double) value);
            } else if (value instanceof Calendar) {
                node.setProperty(propertyName, (Calendar) value); 
            } else if (value instanceof byte[]) {
                InputStream is = new ByteArrayInputStream((byte []) value);
                javax.jcr.Binary binaryProperty = node.getSession().getValueFactory().createBinary(is);
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
                getLogger().warn("The value of the property {0} has unknown type and couldn't be saved", propertyName);
            }
        }
    }
}
