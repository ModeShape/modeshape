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
package org.modeshape.jboss.subsystem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.ListAttributeDefinition;
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
            if (model.hasDefined(ModelKeys.REPOSITORY)) {
                for (Property entry : model.get(ModelKeys.REPOSITORY).asPropertyList()) {
                    String repositoryName = entry.getName();
                    ModelNode repository = entry.getValue();
                    writeRepositoryConfiguration(writer, repository, repositoryName);
                }
            }

            if (model.hasDefined(ModelKeys.WEBAPP)) {
                for (Property entry : model.get(ModelKeys.WEBAPP).asPropertyList()) {
                    String webappName = entry.getName();
                    ModelNode webapp = entry.getValue();
                    writeWebAppConfiguration(writer, webapp, webappName);
                }
            }
        }
        writer.writeEndElement(); // End of subsystem element
    }

    private void writeWebAppConfiguration( XMLExtendedStreamWriter writer,
                                           ModelNode webapp,
                                           String repositoryName ) throws XMLStreamException {
        writer.writeStartElement(Element.WEBAPP.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), repositoryName);
        ModelAttributes.EXPLODED.marshallAsAttribute(webapp, false, writer);
        writer.writeEndElement();
    }

    private void writeRepositoryConfiguration( XMLExtendedStreamWriter writer,
                                               ModelNode repository,
                                               String repositoryName ) throws XMLStreamException {

        writer.writeStartElement(Element.REPOSITORY.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), repositoryName);

        // Repository attributes ...

        ModelAttributes.JNDI_NAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.ENABLE_MONITORING.marshallAsAttribute(repository, false, writer);
        ModelAttributes.SECURITY_DOMAIN.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_NAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_STACK.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_CONFIG.marshallAsAttribute(repository, false, writer);
        ModelAttributes.CLUSTER_LOCKING.marshallAsAttribute(repository, false, writer);
        writeAttributeAsList(writer, repository, ModelAttributes.ANONYMOUS_ROLES);
        ModelAttributes.ANONYMOUS_USERNAME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.USE_ANONYMOUS_IF_AUTH_FAILED.marshallAsAttribute(repository, false, writer);
        ModelAttributes.GARBAGE_COLLECTION_THREAD_POOL.marshallAsAttribute(repository, false, writer);
        ModelAttributes.GARBAGE_COLLECTION_INITIAL_TIME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.GARBAGE_COLLECTION_INTERVAL.marshallAsAttribute(repository, false, writer);
        ModelAttributes.DOCUMENT_OPTIMIZATION_THREAD_POOL.marshallAsAttribute(repository, false, writer);
        ModelAttributes.DOCUMENT_OPTIMIZATION_INITIAL_TIME.marshallAsAttribute(repository, false, writer);
        ModelAttributes.DOCUMENT_OPTIMIZATION_INTERVAL.marshallAsAttribute(repository, false, writer);
        ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET.marshallAsAttribute(repository, false, writer);
        ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE.marshallAsAttribute(repository, false, writer);
        ModelAttributes.EVENT_BUS_SIZE.marshallAsAttribute(repository, false, writer);
        ModelAttributes.LOCK_TIMEOUT_MILLIS.marshallAsAttribute(repository, false, writer);
        ModelAttributes.REPOSITORY_MODULE_DEPENDENCIES.marshallAsAttribute(repository, false, writer);

        // Nested elements ...
        writePersistence(writer, repository);
        writeNodeTypes(writer, repository);
        writeWorkspaces(writer, repository);
        writeJournaling(writer, repository);
        writeAuthenticators(writer, repository);
        writeIndexProviders(writer, repository);
        writeIndexes(writer, repository);
        writeReindexing(writer, repository);
        writeBinaryStorage(writer, repository);
        writeSequencing(writer, repository);
        writeExternalSources(writer, repository);
        writeTextExtraction(writer, repository);
        writer.writeEndElement();
    }

    private void writePersistence(XMLExtendedStreamWriter writer, ModelNode repository) throws XMLStreamException {

        String dbPersistence = Attribute.DB_PERSISTENCE.getLocalName();
        if (has(repository, dbPersistence)) {
            startIfNeeded(writer, Element.DB_PERSISTENCE, false);
            ModelNode persistence = repository.get(dbPersistence).get(dbPersistence);
            ModelAttributes.TABLE_NAME.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.CREATE_ON_START.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.DROP_ON_EXIT.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.CONNECTION_URL.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.DRIVER.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.USERNAME.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.PASSWORD.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.DB_COMPRESS.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.FETCH_SIZE.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.POOL_SIZE.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.PERSISTENCE_DS_JNDI_NAME.marshallAsAttribute(persistence, false, writer);
            // Write out any extra properties ...
            if (has(persistence, ModelKeys.PROPERTIES)) {
                ModelNode properties = persistence.get(ModelKeys.PROPERTIES);
                for (Property property : properties.asPropertyList()) {
                    writer.writeAttribute(property.getName(), property.getValue().asString());
                }
            }
            writer.writeEndElement();
        }
        
        String filePersistence = Attribute.FS_PERSISTENCE.getLocalName();
        if (has(repository, filePersistence)) {
            startIfNeeded(writer, Element.FILE_PERSISTENCE, false);
            ModelNode persistence = repository.get(filePersistence).get(filePersistence);
            ModelAttributes.FS_PATH.marshallAsAttribute(persistence, false, writer);
            ModelAttributes.FS_COMPRESS.marshallAsAttribute(persistence, false, writer);
            writer.writeEndElement();
        }
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
        if (ModelAttributes.WORKSPACES_CACHE_SIZE.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.WORKSPACES, started);
            ModelAttributes.WORKSPACES_CACHE_SIZE.marshallAsAttribute(repository, writer);
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
            started = startIfNeeded(writer, Element.WORKSPACES, started);
            writer.writeStartElement(Element.INITIAL_CONTENT.getLocalName());
            writer.writeCharacters(repository.get(ModelKeys.DEFAULT_INITIAL_CONTENT).asString());
            writer.writeEndElement();
        }
        if (started) {
            writer.writeEndElement();
        }
    } 
    
    private void writeReindexing( XMLExtendedStreamWriter writer,
                                  ModelNode repository ) throws XMLStreamException {
        boolean started = false;
        // Write these model attributes of 'repository' onto the 'reindexing' XML element ...
        if (ModelAttributes.REINDEXING_ASYNC.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.REINDEXIG, started);
            ModelAttributes.REINDEXING_ASYNC.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.REINDEXING_MODE.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.REINDEXIG, started);
            ModelAttributes.REINDEXING_MODE.marshallAsAttribute(repository, writer);
        }
        if (started) {
            writer.writeEndElement();
        }
    }

    private void writeJournaling( XMLExtendedStreamWriter writer,
                                  ModelNode repository ) throws XMLStreamException {
        boolean started = false;
        // Write these model attributes of 'repository' onto the 'journaling' XML element ...
        if (ModelAttributes.JOURNAL_ENABLED.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.JOURNAL_ENABLED.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.JOURNAL_PATH.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.JOURNAL_PATH.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.JOURNAL_RELATIVE_TO.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.JOURNAL_RELATIVE_TO.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.MAX_DAYS_TO_KEEP_RECORDS.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.MAX_DAYS_TO_KEEP_RECORDS.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.ASYNC_WRITES.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.ASYNC_WRITES.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.JOURNAL_GC_INITIAL_TIME.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.JOURNAL_GC_INITIAL_TIME.marshallAsAttribute(repository, writer);
        }
        if (ModelAttributes.JOURNAL_GC_THREAD_POOL.isMarshallable(repository, false)) {
            started = startIfNeeded(writer, Element.JOURNALING, started);
            ModelAttributes.JOURNAL_GC_THREAD_POOL.marshallAsAttribute(repository, writer);
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

    private void writeBinaryStorage( XMLExtendedStreamWriter writer,
                                     ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE)) {
            ModelNode configuration = repository.get(ModelKeys.CONFIGURATION);
            ModelNode binaryStorage = configuration.get(ModelKeys.BINARY_STORAGE);

            ModelNode binaryStorageType = binaryStorage.get(ModelKeys.STORAGE_TYPE);
            String storageType = binaryStorageType.isDefined() && binaryStorageType.keys().size() == 1 ? (String)binaryStorageType.keys()
                                                                                                                                  .toArray()[0] : null;
            ModelNode storage = storageType != null ? binaryStorageType.get((String)binaryStorageType.keys().toArray()[0]) : new ModelNode();
            writeBinaryStorageModel(writer, storageType, storage);
        }
    }

    private void writeBinaryStorageModel(XMLExtendedStreamWriter writer,
                                         String storageType,
                                         ModelNode storage) throws XMLStreamException {
        if (ModelKeys.TRANSIENT_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.TRANSIENT_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.FILE_BINARY_STORAGE.equals(storageType)) {
            // This is the default, but there is no default value for the ModelAttributes.PATH (which is required),
            // which means we always have to write this out. If it is the default binary storage, then there
            // won't even be a 'binary-storage=BINARIES' model node.
            writer.writeStartElement(Element.FILE_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.PATH.marshallAsAttribute(storage, false, writer);
            ModelAttributes.TRASH.marshallAsAttribute(storage, false, writer);
            ModelAttributes.RELATIVE_TO.marshallAsAttribute(storage, false, writer);
            ModelAttributes.STORE_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.DB_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.DB_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.DATA_SOURCE_JNDI_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.STORE_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.CASSANDRA_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.CASSANDRA_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            ModelAttributes.CASSANDRA_HOST.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.MONGO_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.MONGO_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_HOST.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_PORT.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_DATABASE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_USERNAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_PASSWORD.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MONGO_HOST_ADDRESSES.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.S3_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.S3_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            ModelAttributes.S3_BUCKET_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.S3_USERNAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.S3_PASSWORD.marshallAsAttribute(storage, false, writer);
            ModelAttributes.S3_ENDPOINT_URL.marshallAsAttribute(storage, false, writer);
            writer.writeEndElement();
        } else if (ModelKeys.COMPOSITE_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.COMPOSITE_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.STORE_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);

            writeNestedStoresOfType(storage, ModelKeys.NESTED_STORAGE_TYPE_FILE, ModelKeys.FILE_BINARY_STORAGE, writer);
            writeNestedStoresOfType(storage, ModelKeys.NESTED_STORAGE_TYPE_DB, ModelKeys.DB_BINARY_STORAGE, writer);
            writeNestedStoresOfType(storage, ModelKeys.NESTED_STORAGE_TYPE_CUSTOM, ModelKeys.CUSTOM_BINARY_STORAGE, writer);

            writer.writeEndElement();
        } else if (ModelKeys.CUSTOM_BINARY_STORAGE.equals(storageType)) {
            writer.writeStartElement(Element.CUSTOM_BINARY_STORAGE.getLocalName());
            ModelAttributes.MINIMUM_BINARY_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MINIMUM_STRING_SIZE.marshallAsAttribute(storage, false, writer);
            ModelAttributes.STORE_NAME.marshallAsAttribute(storage, false, writer);
            ModelAttributes.MIME_TYPE_DETECTION.marshallAsAttribute(storage, false, writer);
            List<String> toSkip = Arrays.asList(ModelKeys.MINIMUM_BINARY_SIZE, ModelKeys.MINIMUM_STRING_SIZE,
                                                ModelKeys.STORE_NAME, ModelKeys.MIME_TYPE_DETECTION); 
            storage.keys().stream().filter(key -> !toSkip.contains(key)).forEach(key -> {
                try {
                    switch (key) {
                        case ModelKeys.CLASSNAME:
                            ModelAttributes.CLASSNAME.marshallAsAttribute(storage, false, writer);
                            break;
                        case ModelKeys.MODULE:
                            ModelAttributes.MODULE.marshallAsAttribute(storage, false, writer);
                            break;
                        default:
                            writer.writeAttribute(key, storage.get(key).asString());
                            break;
                    }
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.writeEndElement();
        }
    }

    private void writeNestedStoresOfType( ModelNode storage,
                                          String nestedStorageType,
                                          String storeType,
                                          XMLExtendedStreamWriter writer ) throws XMLStreamException {
        if (has(storage, nestedStorageType)) {
            List<ModelNode> nestedCacheStores = storage.get(nestedStorageType).asList();

            for (ModelNode nestedStore : nestedCacheStores) {
                String storeName = (String)nestedStore.keys().toArray()[0];
                writeBinaryStorageModel(writer, storeType, nestedStore.get(storeName));
            }
        }
    }

    private void writeAuthenticators( XMLExtendedStreamWriter writer,
                                      ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.AUTHENTICATOR)) {
            writer.writeStartElement(Element.AUTHENTICATORS.getLocalName());
            for (Property authenticator : repository.get(ModelKeys.AUTHENTICATOR).asPropertyList()) {
                writer.writeStartElement(Element.AUTHENTICATOR.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), authenticator.getName());
                ModelNode prop = authenticator.getValue();
                ModelAttributes.AUTHENTICATOR_CLASSNAME.marshallAsAttribute(prop, writer);
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

    private void writeSequencing( XMLExtendedStreamWriter writer,
                                  ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.SEQUENCER)) {
            writer.writeStartElement(Element.SEQUENCERS.getLocalName());
            if (repository.hasDefined(ModelKeys.SEQUENCERS_THREAD_POOL_NAME)) {
                writer.writeAttribute(Attribute.THREAD_POOL_NAME.getLocalName(), repository.get(
                        ModelKeys.SEQUENCERS_THREAD_POOL_NAME).asString());
            }
            if (repository.hasDefined(ModelKeys.SEQUENCERS_MAX_POOL_SIZE)) {
                writer.writeAttribute(Attribute.MAX_POOL_SIZE.getLocalName(), repository.get(ModelKeys.SEQUENCERS_MAX_POOL_SIZE)
                                                                                        .asString());
            }

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
                if (has(prop, ModelKeys.PATH_EXPRESSIONS)) {
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
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

    }

    private void writeIndexProviders( XMLExtendedStreamWriter writer,
                                      ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.INDEX_PROVIDER)) {
            writer.writeStartElement(Element.INDEX_PROVIDERS.getLocalName());
            ModelNode providerNode = repository.get(ModelKeys.INDEX_PROVIDER);
            for (Property provider : providerNode.asPropertyList()) {
                writer.writeStartElement(Element.INDEX_PROVIDER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), provider.getName());
                ModelNode prop = provider.getValue();
                ModelAttributes.CLASSNAME.marshallAsAttribute(prop, writer);
                ModelAttributes.MODULE.marshallAsAttribute(prop, writer);
                ModelAttributes.RELATIVE_TO.marshallAsAttribute(prop, writer);
                ModelAttributes.PATH.marshallAsAttribute(prop, writer);

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

    private void writeIndexes( XMLExtendedStreamWriter writer,
                               ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.INDEX)) {
            writer.writeStartElement(Element.INDEXES.getLocalName());
            ModelNode providerNode = repository.get(ModelKeys.INDEX);
            for (Property index : providerNode.asPropertyList()) {
                writer.writeStartElement(Element.INDEX.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), index.getName());
                ModelNode prop = index.getValue();
                ModelAttributes.PROVIDER_NAME.marshallAsAttribute(prop, writer);
                ModelAttributes.INDEX_KIND.marshallAsAttribute(prop, writer);
                ModelAttributes.SYNCHRONOUS.marshallAsAttribute(prop, writer);
                ModelAttributes.NODE_TYPE_NAME.marshallAsAttribute(prop, writer);
                ModelAttributes.INDEX_COLUMNS.marshallAsAttribute(prop, writer);
                ModelAttributes.WORKSPACES.marshallAsAttribute(prop, writer);
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

    private void writeExternalSources( XMLExtendedStreamWriter writer,
                                       ModelNode repository ) throws XMLStreamException {
        if (has(repository, ModelKeys.SOURCE)) {
            writer.writeStartElement(Element.EXTERNAL_SOURCES.getLocalName());
            ModelNode externalSourceNode = repository.get(ModelKeys.SOURCE);
            for (Property externalSource : externalSourceNode.asPropertyList()) {
                writer.writeStartElement(Element.SOURCE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), externalSource.getName());

                ModelNode prop = externalSource.getValue();
                ModelAttributes.CONNECTOR_CLASSNAME.marshallAsAttribute(prop, writer);
                ModelAttributes.MODULE.marshallAsAttribute(prop, writer);
                ModelAttributes.CACHEABLE.marshallAsAttribute(prop, writer);
                ModelAttributes.QUERYABLE.marshallAsAttribute(prop, writer);
                ModelAttributes.READONLY.marshallAsAttribute(prop, writer);
                ModelAttributes.EXPOSE_AS_WORKSPACE.marshallAsAttribute(prop, writer);

                // Write out the extra properties ...
                if (has(prop, ModelKeys.PROPERTIES)) {
                    ModelNode properties = prop.get(ModelKeys.PROPERTIES);
                    for (Property property : properties.asPropertyList()) {
                        writer.writeAttribute(property.getName(), property.getValue().asString());
                    }
                }

                if (has(prop, ModelKeys.PROJECTIONS)) {
                    List<ModelNode> projections = prop.get(ModelKeys.PROJECTIONS).asList();
                    for (ModelNode projection : projections) {
                        writer.writeStartElement(Element.PROJECTION.getLocalName());
                        writer.writeCharacters(projection.asString());
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
            if (repository.hasDefined(ModelKeys.TEXT_EXTRACTORS_THREAD_POOL_NAME)) {
                writer.writeAttribute(Attribute.THREAD_POOL_NAME.getLocalName(), repository.get(
                        ModelKeys.TEXT_EXTRACTORS_THREAD_POOL_NAME).asString());
            }
            if (repository.hasDefined(ModelKeys.TEXT_EXTRACTORS_MAX_POOL_SIZE)) {
                writer.writeAttribute(Attribute.MAX_POOL_SIZE.getLocalName(), repository.get(ModelKeys.TEXT_EXTRACTORS_MAX_POOL_SIZE)
                                                                                        .asString());
            }
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
