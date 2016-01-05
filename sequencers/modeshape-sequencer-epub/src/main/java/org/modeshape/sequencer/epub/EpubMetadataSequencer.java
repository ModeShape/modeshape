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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
 * A sequencer that processes the binary content of an EPUB file, extracts the metadata, and then writes that
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>epub:metadata</strong> node of type <code>epub:metadata</code>
 * <ul>
 * <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the EPUB</li>
 * <li><strong>epub:title</strong> - optional string property for the title of the book</li>
 * <li><strong>epub:creator</strong> - optional string property for the creator of the book</li>
 * <li><strong>epub:contributor</strong> - optional string property for the contributor</li>
 * <li><strong>epub:language</strong> - optional string property for the book's language</li>
 * <li><strong>epub:identifier</strong> - optional string property for the book's identifier</li>
 * <li><strong>epub:subject</strong> - optional string property for the subject of the book</li>
 * <li><strong>epub:description</strong> - optional string property for the book's description</li>
 * <li><strong>epub:publisher</strong> - optional string property for the publisher</li>
 * <li><strong>epub:date</strong> - optional string property for the creation date</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
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
                                          Binary binaryValue ) throws RepositoryException {
        EpubMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new EpubMetadata();
            metadata.setInput(stream);
            if (metadata.check()) {
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.TITLE, metadata.getTitle());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.CREATOR, metadata.getCreator());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.CONTRIBUTOR, metadata.getContributor());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.LANGUAGE, metadata.getLanguage());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.IDENTIFIER, metadata.getIdentifier());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.SUBJECT, metadata.getSubject());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.DESCRIPTION, metadata.getDescription());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.PUBLISHER, metadata.getPublisher());
                setPropertyIfMetadataPresent(sequencedNode, EpubMetadataLexicon.DATE, metadata.getDate());
                return true;
            }
        } catch (IOException e) {
            getLogger().debug(e, "Couldn't process stream.");
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

    private void setPropertyIfMetadataPresent( Node node,
                                               String propertyName,
                                               Object value ) throws RepositoryException {
        if (value != null) {
            if (value instanceof String && !StringUtil.isBlank((String) value)) {
                node.setProperty(propertyName, (String) value);
            }
            if (value instanceof Boolean) {
                node.setProperty(propertyName, (Boolean) value);
            }
            if (value instanceof Long) {
                node.setProperty(propertyName, (Long) value);
            }
            if (value instanceof Integer) {
                node.setProperty(propertyName, new Long((Integer) value));
            }
            if (value instanceof Double) {
                node.setProperty(propertyName, (Double) value);
            }
            if (value instanceof Calendar) {
                node.setProperty(propertyName, (Calendar) value); 
            }
            if (value instanceof byte[]) {
                InputStream is = new ByteArrayInputStream((byte []) value);
                javax.jcr.Binary binaryProperty = node.getSession().getValueFactory().createBinary(is);
                node.setProperty(propertyName, binaryProperty);
            }
            if (value instanceof List) {
                ValueFactory vf = node.getSession().getValueFactory();
                List<Value> values = new ArrayList<Value>();
                for (Object val : (List) value) {
                    if (val instanceof String) {
                        values.add(vf.createValue((String) val));
                    }
                }
                if (values.size() > 0) {
                    Value[] valuesArray = new Value[values.size()];
                    values.toArray(valuesArray);
                    node.setProperty(propertyName, valuesArray);
                }
            }
        }
    }
}
