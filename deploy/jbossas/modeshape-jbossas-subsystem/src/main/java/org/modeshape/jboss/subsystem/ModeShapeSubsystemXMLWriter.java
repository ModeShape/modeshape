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
package org.modeshape.jboss.subsystem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * XML writer for current ModeShape subsystem schema version.
 */
public class ModeShapeSubsystemXMLWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent( XMLExtendedStreamWriter writer,
                              SubsystemMarshallingContext context ) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            for (Property entry : model.get(ModelKeys.REPOSITORY).asPropertyList()) {
                String repositoryName = entry.getName();
                ModelNode repository = entry.getValue();
                writeRepositoryConfiguration(writer, repository, repositoryName);
            }
        }
        writer.writeEndElement(); // End of subsystem element
    }

    private void writeRepositoryConfiguration( XMLExtendedStreamWriter writer,
                                               ModelNode repository,
                                               String repositoryName ) throws XMLStreamException {

        writer.writeStartElement(Element.REPOSITORY.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), repositoryName);

        // Repository attributes ...

        ModelAttributes.CACHE_NAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CACHE_CONTAINER.marshallAsAttribute(repository, false, writer);
        ModelAttributes.JNDI_NAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.ENABLE_MONITORING.marshallAsAttribute(repository, false, writer);
        ModelAttributes.SECURITY_DOMAIN.marshallAsAttribute(repository, false, writer);
        writeAttributeAsList(writer, repository, ModelAttributes.ANONYMOUS_ROLES);
        ModelAttributes.ANONYMOUS_USERNAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.USE_ANONYMOUS_IF_AUTH_FAILED.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_NAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_STACK.marshallAsAttribute(repository, false, writer);

        // Nested elements ...
        writeNodeTypes(writer, repository);
        writeWorkspaces(writer, repository);
        writeIndexing(writer, repository);
        writeIndexStorage(writer, repository);
        writeBinaryStorage(writer, repository);
        writeAuthenticators(writer, repository);
        writeSequencing(writer, repository);
        writeTextExtraction(writer, repository);
        writer.writeEndElement();
    }

    private void writeNodeTypes( XMLExtendedStreamWriter writer,
                                 ModelNode repository ) throws XMLStreamException {
        boolean started = false;

        if (has(repository, ModelKeys.NODE_TYPES)) {
            started = startIfNeeded(writer, Element.NODE_TYPES, started);
            List<ModelNode> nodeTypes = repository.get(ModelKeys.NODE_TYPES).asList();
            for (ModelNode nodeType : nodeTypes) {
                writer.writeStartElement(Element.NODE_TYPE.getLocalName());
                writer.writeCharacters(nodeType.asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeWorkspaces( XMLExtendedStreamWriter writer,
                                  ModelNode repository ) throws XMLStreamException {
        boolean started = false;
        // Write these model attributes of 'repository' onto the 'workspaces' XML element ...
        if (ModelAttributes.DEFAULT_WORKSPACE.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.WORKSPACES, started);
            ModelAttributes.DEFAULT_WORKSPACE.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.ALLOW_WORKSPACE_CREATION.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.WORKSPACES, started);
            ModelAttributes.ALLOW_WORKSPACE_CREATION.marshallAsAttribute(repository, writer);
        }
        if (has(repository, ModelKeys.PREDEFINED_WORKSPACE_NAMES)) {
            started = startIfNeeded(writer, Element.WORKSPACES, started);
            ModelNode names = repository.get(ModelKeys.PREDEFINED_WORKSPACE_NAMES);
            if (names.isDefined()) {
                Map<String, String> workspacesInitialContent = new HashMap<String, String>();
                if (has(repository, ModelKeys.WORKSPACES_INITIAL_CONTENT)) {
                    List<ModelNode> initialContentNodes = repository.get(ModelKeys.WORKSPACES_INITIAL_CONTENT).asList();
                    for (ModelNode modelNode : initialContentNodes) {
                        Property property = modelNode.asProperty();
                        workspacesInitialContent.put(property.getName(), property.getValue().asString());
                    }
                }

                for (ModelNode workspace : repository.get(ModelKeys.PREDEFINED_WORKSPACE_NAMES).asList()) {
                    writer.writeStartElement(Element.WORKSPACE.getLocalName());
                    String name = workspace.asString();
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);

                    if (workspacesInitialContent.containsKey(name)) {
                        writer.writeStartElement(Element.INITIAL_CONTENT.getLocalName());
                        writer.writeCharacters(workspacesInitialContent.get(name));
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }
            }
        }
        if (has(repository, ModelKeys.DEFAULT_INITIAL_CONTENT)) {
            writer.writeStartElement(Element.INITIAL_CONTENT.getLocalName());
            writer.writeCharacters(repository.get(ModelKeys.DEFAULT_INITIAL_CONTENT).asString());
            writer.writeEndElement();
        }
        if (started) {
            writer.writeEndElement();
        }
    }

    private void writeIndexing( XMLExtendedStreamWriter writer,
                                ModelNode repository ) throws XMLStreamException {
        // Repository's indexing attributes
        boolean started = false;
        for (String key : repository.keys()) {
            if (ModelKeys.REBUILD_INDEXES_UPON_STARTUP.equals(key)) {
                started = startAndWriteAttribute(writer,
                                                 repository,
                                                 ModelAttributes.REBUILD_INDEXES_UPON_STARTUP,
                                                 Element.INDEXING,
                                                 started);
            } else if (ModelKeys.ANALYZER_CLASSNAME.equals(key)) {
                started = startAndWriteAttribute(writer,
                                                 repository,
                                                 ModelAttributes.ANALYZER_CLASSNAME,
                                                 Element.INDEXING,
                                                 started);
            } else if (ModelKeys.ANALYZER_MODULE.equals(key)) {
                started = startAndWriteAttribute(writer, repository, ModelAttributes.ANALYZER_MODULE, Element.INDEXING, started);
            } else if (ModelKeys.ASYNC_THREAD_POOL_SIZE.equals(key)) {
                started = startAndWriteAttribute(writer,
                                                 repository,
                                                 ModelAttributes.ASYNC_THREAD_POOL_SIZE,
                                                 Element.INDEXING,
                                                 started);
            } else if (ModelKeys.ASYNC_MAX_QUEUE_SIZE.equals(key)) {
                started = startAndWriteAttribute(writer,
                                                 repository,
                                                 ModelAttributes.ASYNC_MAX_QUEUE_SIZE,
                                                 Element.INDEXING,
                                                 started);
            } else if (ModelKeys.BATCH_SIZE.equals(key)) {
                started = startAndWriteAttribute(writer, repository, ModelAttributes.BATCH_SIZE, Element.INDEXING, started);
            } else if (ModelKeys.MODE.equals(key)) {
                started = startAndWriteAttribute(writer, repository, ModelAttributes.MODE, Element.INDEXING, started);
            } else if (ModelKeys.SYSTEM_CONTENT_MODE.equals(key)) {
                started = startAndWriteAttribute(writer,
                                                 repository,
                                                 ModelAttributes.SYSTEM_CONTENT_MODE,
                                                 Element.INDEXING,
                                                 started);
            } else if (ModelKeys.READER_STRATEGY.equals(key)) {
                started = startAndWriteAttribute(writer, repository, ModelAttributes.READER_STRATEGY, Element.INDEXING, started);
            } else if (ModelKeys.THREAD_POOL.equals(key)) {
                started = startAndWriteAttribute(writer, repository, ModelAttributes.THREAD_POOL, Element.INDEXING, started);
            } else if (key.startsWith("hibernate")) {
                writer.writeAttribute(key, repository.get(key).asString());
            } // otherwise ignore ...
        }

        if (started) {
            writer.writeEndElement();
        }
    }

    private boolean startIfNeeded( XMLExtendedStreamWriter writer,
                                   Element name,
                                   boolean alreadyStarted ) throws XMLStreamException {
        if (!alreadyStarted) {
            writer.writeStartElement(name.getLocalName());
        }
        return true;
    }

    private void writeIndexStorageAttributes( XMLExtendedStreamWriter writer,
                                              ModelNode node,
                                              Element element,
                                              boolean started ) throws XMLStreamException {
        ModelNode storage = node.get((String)node.keys().toArray()[0]);
        for (String key : storage.keys()) {
            if (ModelKeys.INDEX_STORAGE_TYPE.equals(key)) {
                // skip this ...
            }
            // General indexing parameters ...
            else if (ModelKeys.INDEX_FORMAT.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.INDEX_FORMAT, element, started);
            }
            // File-related ...
            else if (ModelKeys.PATH.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.PATH, element, started);
            } else if (ModelKeys.RELATIVE_TO.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.RELATIVE_TO, element, started);
            } else if (ModelKeys.SOURCE_PATH.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.SOURCE_PATH, element, started);
            } else if (ModelKeys.SOURCE_RELATIVE_TO.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.SOURCE_RELATIVE_TO, element, started);
            } else if (ModelKeys.ACCESS_TYPE.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.ACCESS_TYPE, element, started);
            } else if (ModelKeys.LOCKING_STRATEGY.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.LOCKING_STRATEGY, element, started);
            } else if (ModelKeys.REFRESH_PERIOD.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.REFRESH_PERIOD, element, started);
            } else if (ModelKeys.COPY_BUFFER_SIZE.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.COPY_BUFFER_SIZE, element, started);
            }
            // JMS-backend (for master & slave file storage only) ...
            else if (ModelKeys.CONNECTION_FACTORY_JNDI_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.CONNECTION_FACTORY_JNDI_NAME, element, started);
            } else if (ModelKeys.QUEUE_JNDI_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.QUEUE_JNDI_NAME, element, started);
            }
            // Cache-related ...
            else if (ModelKeys.LOCK_CACHE_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.LOCK_CACHE_NAME, element, started);
            } else if (ModelKeys.DATA_CACHE_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.DATA_CACHE_NAME, element, started);
            } else if (ModelKeys.METADATA_CACHE_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.METADATA_CACHE_NAME, element, started);
            } else if (ModelKeys.CACHE_CONTAINER_JNDI_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.CACHE_CONTAINER_JNDI_NAME, element, started);
            } else if (ModelKeys.DATA_CACHE_NAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.DATA_CACHE_NAME, element, started);
            } else if (ModelKeys.CHUNK_SIZE.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.CHUNK_SIZE, element, started);
            }

            // Custom ...
            else if (ModelKeys.CLASSNAME.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.CLASSNAME, element, started);
            } else if (ModelKeys.MODULE.equals(key)) {
                started = startAndWriteAttribute(writer, storage, ModelAttributes.CLASSNAME, element, started);
            }
            // Extra parameters ...
            else {
                writer.writeAttribute(key, storage.get(key).asString());
            }
        }
        if (started) {
            writer.writeEndElement();
        }
    }

    private void writeIndexStorage( XMLExtendedStreamWriter writer,
                                    ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.CONFIGURATION, ModelKeys.INDEX_STORAGE)) {
            ModelNode indexStorage = repository.get(ModelKeys.CONFIGURATION).get(ModelKeys.INDEX_STORAGE);
            ModelNode indexStorageType = indexStorage.get(ModelKeys.STORAGE_TYPE);
            String storageType = indexStorageType.isDefined() && indexStorageType.keys().size() == 1 ? (String)indexStorageType.keys()
                                                                                                                               .toArray()[0] : null;
            if (ModelKeys.RAM_INDEX_STORAGE.equals(storageType)) {
                // Have to write out this element, because there are no attributes (other than the ignored NAME)
                // and it's not the default storage. So we have to start the element and then write any attributes ...
                writer.writeStartElement(Element.RAM_INDEX_STORAGE.getLocalName());
                writeIndexStorageAttributes(writer, indexStorageType, Element.RAM_INDEX_STORAGE, true);
            } else if (ModelKeys.LOCAL_FILE_INDEX_STORAGE.equals(storageType)) {
                writeIndexStorageAttributes(writer, indexStorageType, Element.LOCAL_FILE_INDEX_STORAGE, false);
            } else if (ModelKeys.MASTER_FILE_INDEX_STORAGE.equals(storageType)) {
                writeIndexStorageAttributes(writer, indexStorageType, Element.MASTER_FILE_INDEX_STORAGE, false);
            } else if (ModelKeys.SLAVE_FILE_INDEX_STORAGE.equals(storageType)) {
                writeIndexStorageAttributes(writer, indexStorageType, Element.SLAVE_FILE_INDEX_STORAGE, false);
            } else if (ModelKeys.CACHE_INDEX_STORAGE.equals(storageType)) {
                writeIndexStorageAttributes(writer, indexStorageType, Element.CACHE_INDEX_STORAGE, false);
            } else if (ModelKeys.CUSTOM_INDEX_STORAGE.equals(storageType)) {
                writeIndexStorageAttributes(writer, indexStorageType, Element.CUSTOM_INDEX_STORAGE, false);
            }
        }
    }

    private void writeBinaryStorage( XMLExtendedStreamWriter writer,
                                     ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE)) {
            ModelNode configuration = repository.get(ModelKeys.CONFIGURATION);
            ModelNode binaryStorage = configuration.get(ModelKeys.BINARY_STORAGE);
            ModelNode binaryStorageType = binaryStorage.get(ModelKeys.STORAGE_TYPE);
            String storageType = binaryStorageType.isDefined() && binaryStorageType.keys().size() == 1 ? (String)binaryStorageType.keys()
                                                                                                                                  .toArray()[0] : null;
            ModelNode storage = storageType != null ? binaryStorageType.get((String)binaryStorageType.keys().toArray()[0]) : new ModelNode();
            if (ModelKeys.FILE_BINARY_STORAGE.equals(storageType)) {
                // This is the default, but there is no default value for the ModelAttributes.PATH (which is required),
                // which means we always have to write this out. If it is the default binary storage, then there
                // won't even be a 'binary-storage=BINARIES' model node.
                writer.writeStartElement(Element.FILE_BINARY_STORAGE.getLocalName());
                ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
                ModelAttributes.PATH.marshallAsAttribute(storage, false, writer);
                ModelAttributes.RELATIVE_TO.marshallAsAttribute(storage, false, writer);
                writer.writeEndElement();
            } else if (ModelKeys.CACHE_BINARY_STORAGE.equals(storageType)) {
                writer.writeStartElement(Element.CACHE_BINARY_STORAGE.getLocalName());
                ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
                ModelAttributes.DATA_CACHE_NAME.marshallAsAttribute(storage, false, writer);
                ModelAttributes.METADATA_CACHE_NAME.marshallAsAttribute(storage, false, writer);
                ModelAttributes.CACHE_CONTAINER.marshallAsAttribute(storage, false, writer);
                writer.writeEndElement();
            } else if (ModelKeys.DB_BINARY_STORAGE.equals(storageType)) {
                writer.writeStartElement(Element.DB_BINARY_STORAGE.getLocalName());
                ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
                ModelAttributes.DATA_SOURCE_JNDI_NAME.marshallAsAttribute(storage, false, writer);
                writer.writeEndElement();
            } else if (ModelKeys.CUSTOM_BINARY_STORAGE.equals(storageType)) {
                ModelNode custom_storage = binaryStorage.get(ModelKeys.CUSTOM_BINARY_STORAGE);
                writer.writeStartElement(Element.CUSTOM_BINARY_STORAGE.getLocalName());
                ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(repository, false, writer);
                for (String key : storage.keys()) {
                    if (key.equals(ModelKeys.CLASSNAME)) {
                        ModelAttributes.CLASSNAME.marshallAsAttribute(storage, false, writer);
                    } else if (key.equals(ModelKeys.MODULE)) {
                        ModelAttributes.MODULE.marshallAsAttribute(storage, false, writer);
                    } else {
                        writer.writeAttribute(key, custom_storage.get(key).asString());
                    }
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeAuthenticators( XMLExtendedStreamWriter writer,
                                      ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.AUTHENTICATORS)) {
            ModelNode indexing = repository.get(ModelKeys.AUTHENTICATORS);
            writer.writeStartElement(Element.AUTHENTICATORS.getLocalName());
            for (String key : indexing.keys()) {
                if (ModelKeys.NAME.equals(key)) {
                    ModelAttributes.NAME.marshallAsAttribute(indexing, writer);
                } else if (ModelKeys.CLASSNAME.equals(key)) {
                    ModelAttributes.CLASSNAME.marshallAsAttribute(indexing, writer);
                } else if (ModelKeys.MODULE.equals(key)) {
                    ModelAttributes.MODULE.marshallAsAttribute(indexing, writer);
                } else {
                    ModelNode param = indexing.get(key);
                    writer.writeAttribute(key, param.asString());
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeSequencing( XMLExtendedStreamWriter writer,
                                  ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.SEQUENCER)) {
            writer.writeStartElement(Element.SEQUENCERS.getLocalName());
            ModelNode sequencerNode = repository.get(ModelKeys.SEQUENCER);
            for (Property sequencer : sequencerNode.asPropertyList()) {
                writer.writeStartElement(Element.SEQUENCER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), sequencer.getName());
                ModelNode prop = sequencer.getValue();
                ModelAttributes.SEQUENCER_CLASSNAME.marshallAsAttribute(prop, writer);
                ModelAttributes.MODULE.marshallAsAttribute(prop, writer);

                // Write out the extra properties ...
                if (has(prop, ModelKeys.PROPERTIES)) {
                    ModelNode properties = prop.get(ModelKeys.PROPERTIES);
                    for (Property property : properties.asPropertyList()) {
                        writer.writeAttribute(property.getName(), property.getValue().asString());
                    }
                }
                List<ModelNode> pathExpressions = prop.get(ModelKeys.PATH_EXPRESSIONS).asList();
                switch (pathExpressions.size()) {
                    case 0:
                        break;
                    case 1:
                        ModelNode pathExpression = pathExpressions.iterator().next();
                        writer.writeAttribute(Attribute.PATH_EXPRESSION.getLocalName(), pathExpression.asString());
                        break;
                    default:
                        for (ModelNode pathExpr : pathExpressions) {
                            writer.writeStartElement(Element.PATH_EXPRESSION.getLocalName());
                            writer.writeCharacters(pathExpr.asString());
                            writer.writeEndElement();
                        }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

    }

    private void writeTextExtraction( XMLExtendedStreamWriter writer,
                                      ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.TEXT_EXTRACTOR)) {
            writer.writeStartElement(Element.TEXT_EXTRACTORS.getLocalName());
            for (Property extractor : repository.get(ModelKeys.TEXT_EXTRACTOR).asPropertyList()) {
                writer.writeStartElement(Element.TEXT_EXTRACTOR.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), extractor.getName());
                ModelNode prop = extractor.getValue();
                ModelAttributes.TEXT_EXTRACTOR_CLASSNAME.marshallAsAttribute(prop, writer);
                ModelAttributes.MODULE.marshallAsAttribute(prop, writer);

                // Write out the extra properties ...
                if (has(prop, ModelKeys.PROPERTIES)) {
                    ModelNode properties = prop.get(ModelKeys.PROPERTIES);
                    for (Property property : properties.asPropertyList()) {
                        writer.writeAttribute(property.getName(), property.getValue().asString());
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private boolean has( ModelNode node,
                         String name ) {
        return node.isDefined() && node.has(name) && node.get(name).isDefined();
    }

    private boolean has( ModelNode node,
                         String... names ) {
        for (String name : names) {
            if (!node.isDefined() || !node.has(name)) return false;
            node = node.get(name);
        }
        return true;
    }

    private boolean startAndWriteAttribute( final XMLExtendedStreamWriter writer,
                                            final ModelNode node,
                                            final SimpleAttributeDefinition modelAttribute,
                                            Element name,
                                            boolean started ) throws XMLStreamException {
        assert modelAttribute.getXmlName() != null;
        boolean result = started;
        if (modelAttribute.isMarshallable(node, false)) {
            result = startIfNeeded(writer, name, started);
            modelAttribute.marshallAsAttribute(node, false, writer);
        }
        return result;
    }

    private void writeAttributeAsList( XMLExtendedStreamWriter writer,
                                       final ModelNode node,
                                       final ListAttributeDefinition modelAttribute ) throws XMLStreamException {
        if (modelAttribute.isMarshallable(node, false)) {
            StringBuilder sb = new StringBuilder();
            Iterator<ModelNode> iter = node.get(modelAttribute.getName()).asList().iterator();
            if (iter.hasNext()) {
                sb.append(iter.next().asString());
                while (iter.hasNext()) {
                    sb.append(" ").append(iter.next().asString());
                }
                writer.writeAttribute(modelAttribute.getXmlName(), sb.toString());
            }
        }
    }

}
