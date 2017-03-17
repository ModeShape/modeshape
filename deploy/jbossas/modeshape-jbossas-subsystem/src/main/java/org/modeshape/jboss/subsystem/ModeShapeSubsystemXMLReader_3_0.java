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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class ModeShapeSubsystemXMLReader_3_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement( final XMLExtendedStreamReader reader,
                             final List<ModelNode> list ) throws XMLStreamException {

        final ModelNode subsystem = new ModelNode();
        subsystem.add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME);
        subsystem.protect();

        final ModelNode bootServices = new ModelNode();
        bootServices.get(OP).set(ADD);
        bootServices.get(OP_ADDR).set(subsystem);
        list.add(bootServices);

        // no attributes
        requireNoAttributes(reader);

        final List<ModelNode> repositories = new ArrayList<ModelNode>();
        final List<ModelNode> webapps = new ArrayList<ModelNode>();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (reader.isStartElement()) {
                // elements
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case MODESHAPE_3_0:
                        Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case REPOSITORY:
                                parseRepository(reader, subsystem, repositories);
                                break;
                            case WEBAPP: {
                                parseWebApp(reader, subsystem, webapps);
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    case UNKNOWN:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        list.addAll(webapps);
        list.addAll(repositories);
    }

    private void parseWebApp( final XMLExtendedStreamReader reader,
                              final ModelNode address,
                              final List<ModelNode> webapps ) throws XMLStreamException {
        final ModelNode webappAddress = address.clone();
        final ModelNode webapp = Util.getEmptyOperation(ModelDescriptionConstants.ADD, webappAddress);

        String webappName = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attrName = reader.getAttributeLocalName(i);
            String attrValue = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(attrName);
            switch (attribute) {
                case NAME: {
                    webappName = attrValue;
                    webappAddress.add(ModelKeys.WEBAPP, webappName);
                    webappAddress.protect();
                    webapp.get(OP).set(ADD);
                    webapp.get(OP_ADDR).set(webappAddress);
                    webapps.add(webapp);
                    break;
                }
                case EXPLODED: {
                    ModelAttributes.EXPLODED.parseAndSetParameter(attrValue, webapp, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        requireNoElements(reader);
    }

    private void parseRepository( final XMLExtendedStreamReader reader,
                                  final ModelNode address,
                                  final List<ModelNode> repositories ) throws XMLStreamException {

        final ModelNode repositoryAddress = address.clone();
        final ModelNode repository = Util.getEmptyOperation(ModelDescriptionConstants.ADD, repositoryAddress);

        String repositoryName = null;
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        repositoryName = attrValue;
                        repositoryAddress.add(ModelKeys.REPOSITORY, attrValue);
                        repositoryAddress.protect();
                        repository.get(OP).set(ADD);
                        repository.get(OP_ADDR).set(repositoryAddress);
                        repositories.add(repository);
                        break;
                    case JNDI_NAME:
                        ModelAttributes.JNDI_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ENABLE_MONITORING:
                        ModelAttributes.ENABLE_MONITORING.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case CLUSTER_STACK:
                        ModelAttributes.CLUSTER_STACK.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case CLUSTER_NAME:
                        ModelAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;   
                    case CLUSTER_CONFIG:
                        ModelAttributes.CLUSTER_CONFIG.parseAndSetParameter(attrValue, repository, reader);
                        break; 
                    case CLUSTER_LOCKING:
                        ModelAttributes.CLUSTER_LOCKING.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case SECURITY_DOMAIN:
                        ModelAttributes.SECURITY_DOMAIN.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ANONYMOUS_ROLES:
                        for (String role : reader.getListAttributeValue(i)) {
                            repository.get(ModelKeys.ANONYMOUS_ROLES).add(role);
                        }
                        break;
                    case ANONYMOUS_USERNAME:
                        ModelAttributes.ANONYMOUS_USERNAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case USE_ANONYMOUS_IF_AUTH_FAILED:
                        ModelAttributes.USE_ANONYMOUS_IF_AUTH_FAILED.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case GARBAGE_COLLECTION_THREAD_POOL:
                        ModelAttributes.GARBAGE_COLLECTION_THREAD_POOL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case GARBAGE_COLLECTION_INITIAL_TIME:
                        ModelAttributes.GARBAGE_COLLECTION_INITIAL_TIME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case GARBAGE_COLLECTION_INTERVAL:
                        ModelAttributes.GARBAGE_COLLECTION_INTERVAL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case DOCUMENT_OPTIMIZATION_THREAD_POOL:
                        ModelAttributes.DOCUMENT_OPTIMIZATION_THREAD_POOL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case DOCUMENT_OPTIMIZATION_INITIAL_TIME:
                        ModelAttributes.DOCUMENT_OPTIMIZATION_INITIAL_TIME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case DOCUMENT_OPTIMIZATION_INTERVAL:
                        ModelAttributes.DOCUMENT_OPTIMIZATION_INTERVAL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET:
                        ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TARGET.parseAndSetParameter(attrValue,
                                                                                                      repository,
                                                                                                      reader);
                        break;
                    case DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE:
                        ModelAttributes.DOCUMENT_OPTIMIZATION_CHILD_COUNT_TOLERANCE.parseAndSetParameter(attrValue,
                                                                                                         repository,
                                                                                                         reader);
                        break;
                    case EVENT_BUS_SIZE: 
                        ModelAttributes.EVENT_BUS_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case LOCK_TIMEOUT_MILLIS: 
                        ModelAttributes.LOCK_TIMEOUT_MILLIS.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case REPOSITORY_MODULE_DEPENDENCIES:
                        ModelAttributes.REPOSITORY_MODULE_DEPENDENCIES.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ModelNode persistence = null;
        ModelNode binaryStorage = null;
        List<ModelNode> sequencers = new ArrayList<ModelNode>();
        List<ModelNode> indexProviders = new ArrayList<ModelNode>();
        List<ModelNode> indexes = new ArrayList<ModelNode>();
        List<ModelNode> externalSources = new ArrayList<ModelNode>();
        List<ModelNode> textExtractors = new ArrayList<ModelNode>();
        List<ModelNode> authenticators = new ArrayList<ModelNode>();
        List<ModelNode> multipleStorageNodes = new ArrayList<ModelNode>();
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DB_PERSISTENCE: {
                    persistence = parseDBPersistence(reader, repositoryName);
                    break;
                }
                case FILE_PERSISTENCE: {
                    persistence = parseFilePersistence(reader, repositoryName);
                    break;
                }
                case WORKSPACES:
                    parseWorkspaces(reader, address, repository);
                    break;
                case JOURNALING: {
                    parseJournaling(reader, repository);
                    break;
                }
                case NODE_TYPES:
                    parseNodeTypes(reader, repository);
                    break;

                // Binary storage ...
                case TRANSIENT_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseTransientBinaryStorage(reader, repositoryName);
                    break;
                case FILE_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseFileBinaryStorage(reader, repositoryName, false);
                    break;
                case DB_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseDatabaseBinaryStorage(reader, repositoryName, false);
                    break;
                case CASSANDRA_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseCassandraBinaryStorage(reader, repositoryName, false);
                    break;
                case MONGO_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseMongoBinaryStorage(reader, repositoryName, false);
                    break;
                case S3_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseS3BinaryStorage(reader, repositoryName, false);
                    break;
                case COMPOSITE_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    multipleStorageNodes = parseCompositeBinaryStorage(reader, repositoryName);
                    break;
                case CUSTOM_BINARY_STORAGE:
                    addBinaryStorageConfiguration(repositories, repositoryName);
                    binaryStorage = parseCustomBinaryStorage(reader, repositoryName, false);
                    break;

                // Authenticators ...
                case AUTHENTICATORS:
                    authenticators = parseAuthenticators(reader, repositoryName);
                    break;

                // Sequencing ...
                case SEQUENCERS:
                    sequencers = parseSequencers(reader, repository, address, repositoryName);
                    break;

                // Index providers ...
                case INDEX_PROVIDERS:
                    indexProviders = parseIndexProviders(reader, address, repositoryName);
                    break;

                // Indexes ...
                case INDEXES:
                    indexes = parseIndexes(reader, address, repositoryName);
                    break;
                
                // Reindexing...
                case REINDEXIG: {
                    parseReindexing(reader, repository);
                    break;
                }

                // External sources ...
                case EXTERNAL_SOURCES:
                    externalSources = parseExternalSources(reader, address, repositoryName);
                    break;

                // Text extracting ...
                case TEXT_EXTRACTORS:
                    textExtractors = parseTextExtracting(reader, repository, repositoryName);
                    break;

                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        if (binaryStorage != null) repositories.add(binaryStorage);
        if (persistence != null) repositories.add(persistence);
        repositories.addAll(multipleStorageNodes);
        repositories.addAll(sequencers);
        repositories.addAll(indexProviders);
        repositories.addAll(indexes);
        repositories.addAll(externalSources);
        repositories.addAll(textExtractors);
        repositories.addAll(authenticators);
    }

    private ModelNode parseDBPersistence(XMLExtendedStreamReader reader, String repositoryName) throws XMLStreamException {
        final ModelNode persistence = new ModelNode();
        persistence.get(OP).set(ADD);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case TABLE_NAME:
                        ModelAttributes.TABLE_NAME.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case CREATE_ON_START:
                        ModelAttributes.CREATE_ON_START.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case DROP_ON_EXIT:
                        ModelAttributes.DROP_ON_EXIT.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case URL:
                        ModelAttributes.CONNECTION_URL.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case USERNAME:
                        ModelAttributes.USERNAME.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case PASSWORD:
                        ModelAttributes.PASSWORD.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case DRIVER:
                        ModelAttributes.DRIVER.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case FETCH_SIZE:
                        ModelAttributes.FETCH_SIZE.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case COMPRESS:
                        ModelAttributes.DB_COMPRESS.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case DATA_SOURCE_JNDI_NAME:
                        ModelAttributes.PERSISTENCE_DS_JNDI_NAME.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case POOL_SIZE:
                        ModelAttributes.POOL_SIZE.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    default:
                        // extra attributes are allowed
                        persistence.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        break;
                }
            }
        }

        String dbPersistenceKey = Attribute.DB_PERSISTENCE.getLocalName();
        persistence.get(OP_ADDR)
                     .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                     .add(ModelKeys.REPOSITORY, repositoryName)
                     .add(dbPersistenceKey, dbPersistenceKey);

        requireNoElements(reader);
        
        return persistence;
    }

    private ModelNode parseFilePersistence(XMLExtendedStreamReader reader, String repositoryName) throws XMLStreamException {
        final ModelNode persistence = new ModelNode();
        persistence.get(OP).set(ADD);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case PATH:
                        ModelAttributes.FS_PATH.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    case COMPRESS:
                        ModelAttributes.FS_COMPRESS.parseAndSetParameter(attrValue, persistence, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        String fsPersistenceKey = Attribute.FS_PERSISTENCE.getLocalName();
        persistence.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(fsPersistenceKey, fsPersistenceKey);

        requireNoElements(reader);

        return persistence;
    }

    private void parseNodeTypes( XMLExtendedStreamReader reader,
                                 ModelNode repository ) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NODE_TYPE: {
                    repository.get(ModelKeys.NODE_TYPES).add(reader.getElementText());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void addBinaryStorageConfiguration( final List<ModelNode> repositories,
                                                String repositoryName ) {
        ModelNode configuration = new ModelNode();
        configuration.get(OP).set(ADD);
        configuration.get(OP_ADDR)
                     .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                     .add(ModelKeys.REPOSITORY, repositoryName)
                     .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);
        repositories.add(configuration);
    }

    private void parseWorkspaces( final XMLExtendedStreamReader reader,
                                  final ModelNode parentAddress,
                                  final ModelNode repository ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                // Set these as properties on the repository ModelNode ...
                    case ALLOW_WORKSPACE_CREATION:
                        ModelAttributes.ALLOW_WORKSPACE_CREATION.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case DEFAULT_WORKSPACE:
                        ModelAttributes.DEFAULT_WORKSPACE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case CACHE_SIZE: {
                        ModelAttributes.WORKSPACES_CACHE_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case WORKSPACE: {
                    parseWorkspace(reader, repository);
                    break;
                }
                case INITIAL_CONTENT: {
                    repository.get(ModelKeys.DEFAULT_INITIAL_CONTENT).set(reader.getElementText());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseReindexing( final XMLExtendedStreamReader reader,
                                  final ModelNode repository ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // Set these as properties on the repository ModelNode ...
                    case REINDEXING_ASNC:
                        ModelAttributes.REINDEXING_ASYNC.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case REINDEXING_MODE:
                        ModelAttributes.REINDEXING_MODE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
    }

    private void parseJournaling( final XMLExtendedStreamReader reader,
                                  final ModelNode repository ) throws XMLStreamException {
        repository.get(ModelAttributes.JOURNALING.getName()).set(true);
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // Set these as properties on the repository ModelNode ...
                    case JOURNAL_ENABLED: 
                        ModelAttributes.JOURNAL_ENABLED.parseAndSetParameter(attrValue, repository, reader);                        
                        break;                
                    case JOURNAL_PATH:
                        ModelAttributes.JOURNAL_PATH.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case JOURNAL_RELATIVE_TO:
                        ModelAttributes.JOURNAL_RELATIVE_TO.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case MAX_DAYS_TO_KEEP_RECORDS:
                        ModelAttributes.MAX_DAYS_TO_KEEP_RECORDS.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ASYNC_WRITES:
                        ModelAttributes.ASYNC_WRITES.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case JOURNAL_GC_THREAD_POOL: {
                        ModelAttributes.JOURNAL_GC_THREAD_POOL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    }
                    case JOURNAL_GC_INITIAL_TIME: {
                        ModelAttributes.JOURNAL_GC_INITIAL_TIME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
    }

    private void parseWorkspace( final XMLExtendedStreamReader reader,
                                 final ModelNode repository ) throws XMLStreamException {
        String workspaceName = null;
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        workspaceName = attrValue;
                        repository.get(ModelKeys.PREDEFINED_WORKSPACE_NAMES).add(attrValue);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INITIAL_CONTENT: {
                    if (workspaceName != null) {
                        repository.get(ModelKeys.WORKSPACES_INITIAL_CONTENT).add(workspaceName, reader.getElementText());
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseFileBinaryStorage( final XMLExtendedStreamReader reader,
                                              final String repositoryName,
                                              boolean nested ) throws XMLStreamException {

        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);

        String storeName = null;
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                // The rest go on the ModelNode for the type ...
                    case RELATIVE_TO:
                        ModelAttributes.RELATIVE_TO.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case PATH:
                        ModelAttributes.PATH.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case TRASH:
                        ModelAttributes.TRASH.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case STORE_NAME:
                        if (nested) {
                            // part of a composite binary store
                            storeName = attrValue.trim();
                            ModelAttributes.STORE_NAME.parseAndSetParameter(attrValue, storageType, reader);
                            break;
                        }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);

        if (nested) {
            storageType.get(OP_ADDR)
                       .add(ModelKeys.STORAGE_TYPE, ModelKeys.COMPOSITE_BINARY_STORAGE)
                       .add(ModelKeys.NESTED_STORAGE_TYPE_FILE, storeName);
        } else {
            storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.FILE_BINARY_STORAGE);
        }

        return storageType;
    }

    private ModelNode parseTransientBinaryStorage( final XMLExtendedStreamReader reader,
                                                   final String repositoryName) throws XMLStreamException {

        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                // The rest go on the ModelNode for the type ...
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
        storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.TRANSIENT_BINARY_STORAGE);
        return storageType;
    }

    private ModelNode parseDatabaseBinaryStorage( final XMLExtendedStreamReader reader,
                                                  final String repositoryName,
                                                  boolean nested ) throws XMLStreamException {
        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);

        String storeName = null;
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                // The rest go on the ModelNode for the type ...
                    case DATA_SOURCE_JNDI_NAME:
                        ModelAttributes.DATA_SOURCE_JNDI_NAME.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case STORE_NAME:
                        if (nested) {
                            // part of a composite binary store
                            storeName = attrValue.trim();
                            ModelAttributes.STORE_NAME.parseAndSetParameter(attrValue, storageType, reader);
                            break;
                        }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);

        if (nested) {
            storageType.get(OP_ADDR)
                       .add(ModelKeys.STORAGE_TYPE, ModelKeys.COMPOSITE_BINARY_STORAGE)
                       .add(ModelKeys.NESTED_STORAGE_TYPE_DB, storeName);
        } else {
            storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.DB_BINARY_STORAGE);
        }

        return storageType;
    }

    private ModelNode parseCassandraBinaryStorage(final XMLExtendedStreamReader reader,
                                                  final String repositoryName,
                                                  boolean nested) throws XMLStreamException {
        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);
        
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                   // The rest go on the ModelNode for the type ...
                    case HOST:
                        ModelAttributes.CASSANDRA_HOST.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
        storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.CASSANDRA_BINARY_STORAGE);
        return storageType;
    }

    private ModelNode parseMongoBinaryStorage(final XMLExtendedStreamReader reader,
                                              final String repositoryName,
                                              boolean nested) throws XMLStreamException {
        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);
        
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                   // The rest go on the ModelNode for the type ...
                    case HOST:
                        ModelAttributes.MONGO_HOST.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case PORT:
                        ModelAttributes.MONGO_PORT.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case DATABASE:
                        ModelAttributes.MONGO_DATABASE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case USERNAME:
                        ModelAttributes.MONGO_USERNAME.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case PASSWORD:
                        ModelAttributes.MONGO_PASSWORD.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case HOST_ADDRESSES:
                        ModelAttributes.MONGO_HOST_ADDRESSES.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
        storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.MONGO_BINARY_STORAGE);
        return storageType;
    }

    private ModelNode parseS3BinaryStorage(final XMLExtendedStreamReader reader,
                                           final String repositoryName,
                                           boolean nested) throws XMLStreamException {
        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case BUCKET_NAME:
                        ModelAttributes.S3_BUCKET_NAME.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case USERNAME:
                        ModelAttributes.S3_USERNAME.parseAndSetParameter(attrValue, storageType, reader);
                    break;
                    case PASSWORD:
                        ModelAttributes.S3_PASSWORD.parseAndSetParameter(attrValue, storageType, reader);
                    break;
                    case ENDPOINT_URL:
                        ModelAttributes.S3_ENDPOINT_URL.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);
        storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.S3_BINARY_STORAGE);
        return storageType;
    }

    private ModelNode parseCustomBinaryStorage( final XMLExtendedStreamReader reader,
                                                final String repositoryName,
                                                boolean nested ) throws XMLStreamException {
        final ModelNode storageType = new ModelNode();
        storageType.get(OP).set(ADD);
        storageType.get(OP_ADDR)
                   .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                   .add(ModelKeys.REPOSITORY, repositoryName)
                   .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);

        String storeName = null;
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case CLASSNAME:
                        ModelAttributes.CLASSNAME.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, storageType, reader);
                        break;
                    case STORE_NAME:
                        if (nested) {
                            // part of a composite binary store
                            storeName = attrValue.trim();
                            ModelAttributes.STORE_NAME.parseAndSetParameter(attrValue, storageType, reader);
                            break;
                        }
                    default:
                        storageType.get(attrName).set(attrValue);
                        break;
                }
            }
        }
        requireNoElements(reader);

        if (nested) {
            storageType.get(OP_ADDR)
                       .add(ModelKeys.STORAGE_TYPE, ModelKeys.COMPOSITE_BINARY_STORAGE)
                       .add(ModelKeys.NESTED_STORAGE_TYPE_CUSTOM, storeName);
        } else {
            storageType.get(OP_ADDR).add(ModelKeys.STORAGE_TYPE, ModelKeys.CUSTOM_BINARY_STORAGE);
        }
        return storageType;
    }

    private List<ModelNode> parseCompositeBinaryStorage( final XMLExtendedStreamReader reader,
                                                         final String repositoryName ) throws XMLStreamException {

        final List<ModelNode> stores = new ArrayList<ModelNode>();
        final ModelNode compositeBinaryStorage = new ModelNode();

        compositeBinaryStorage.get(OP).set(ADD);
        compositeBinaryStorage.get(OP_ADDR)
                              .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                              .add(ModelKeys.REPOSITORY, repositoryName)
                              .add(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE)
                              .add(ModelKeys.STORAGE_TYPE, ModelKeys.COMPOSITE_BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, compositeBinaryStorage, reader);
                        break;
                    case MIN_STRING_SIZE:
                        ModelAttributes.MINIMUM_STRING_SIZE.parseAndSetParameter(attrValue, compositeBinaryStorage, reader);
                        break;
                    case MIME_TYPE_DETECTION:
                        ModelAttributes.MIME_TYPE_DETECTION.parseAndSetParameter(attrValue, compositeBinaryStorage, reader);
                        break;
                    case STORE_NAME:
                        ModelAttributes.STORE_NAME.parseAndSetParameter(attrValue, compositeBinaryStorage, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        stores.add(compositeBinaryStorage);

        List<String> storeNames = new ArrayList<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final ModelNode nestedBinaryStore;
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case FILE_BINARY_STORAGE:
                    nestedBinaryStore = parseFileBinaryStorage(reader, repositoryName, true);
                    break;
                case DB_BINARY_STORAGE:
                    nestedBinaryStore = parseDatabaseBinaryStorage(reader, repositoryName, true);
                    break;
                case CUSTOM_BINARY_STORAGE:
                    nestedBinaryStore = parseCustomBinaryStorage(reader, repositoryName, true);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
            // validate store-name uniqueness within a composite store
            String storeName = nestedBinaryStore.get(ModelKeys.STORE_NAME).asString();
            if (storeNames.contains(storeName)) {
                throw ParseUtils.duplicateAttribute(reader, ModelKeys.STORE_NAME + "=" + storeName);
            }
            storeNames.add(storeName);
            stores.add(nestedBinaryStore);
            ModelAttributes.NESTED_STORES.parseAndAddParameterElement(storeName, compositeBinaryStorage, reader);
        }

        return stores;
    }

    private List<ModelNode> parseAuthenticators( final XMLExtendedStreamReader reader,
                                                 final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> authenticators = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case AUTHENTICATOR:
                    parseAuthenticator(reader, repositoryName, authenticators);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return authenticators;
    }

    private void parseAuthenticator( final XMLExtendedStreamReader reader,
                                     String repositoryName,
                                     final List<ModelNode> authenticators ) throws XMLStreamException {
        final ModelNode authenticator = new ModelNode();
        authenticator.get(OP).set(ADD);
        String name = null;

        authenticators.add(authenticator);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case CLASSNAME:
                        ModelAttributes.AUTHENTICATOR_CLASSNAME.parseAndSetParameter(attrValue, authenticator, reader);
                        if (name == null) name = attrValue;
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, authenticator, reader);
                        break;
                    default:
                        // extra attributes are allowed to set extractor-specific properties ...
                        authenticator.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        break;
                }
            }
        }

        authenticator.get(OP_ADDR)
                     .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                     .add(ModelKeys.REPOSITORY, repositoryName)
                     .add(ModelKeys.AUTHENTICATOR, name);

        requireNoElements(reader);
    }

    private List<ModelNode> parseSequencers( final XMLExtendedStreamReader reader,
                                             final ModelNode repository, 
                                             final ModelNode parentAddress,
                                             final String repositoryName ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case THREAD_POOL_NAME:
                        ModelAttributes.SEQUENCER_THREAD_POOL_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case MAX_POOL_SIZE:
                        ModelAttributes.SEQUENCER_MAX_POOL_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        List<ModelNode> sequencers = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SEQUENCER:
                    parseSequencer(reader, repositoryName, sequencers);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return sequencers;
    }

    private void parseSequencer( XMLExtendedStreamReader reader,
                                 String repositoryName,
                                 final List<ModelNode> sequencers ) throws XMLStreamException {

        final ModelNode sequencer = new ModelNode();
        sequencer.get(OP).set(ADD);
        String name = null;

        sequencers.add(sequencer);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case PATH_EXPRESSION:
                        ModelAttributes.PATH_EXPRESSIONS.parseAndAddParameterElement(attrValue, sequencer, reader);
                        break;
                    case CLASSNAME:
                        ModelAttributes.SEQUENCER_CLASSNAME.parseAndSetParameter(attrValue, sequencer, reader);
                        if (name == null) name = attrValue;
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, sequencer, reader);
                        break;
                    default:
                        // extra attributes are allowed to set sequencer-specific properties ...
                        sequencer.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        break;
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PATH_EXPRESSION:
                    String value = reader.getElementText();
                    ModelAttributes.PATH_EXPRESSIONS.parseAndAddParameterElement(value, sequencer, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        sequencer.get(OP_ADDR)
                 .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                 .add(ModelKeys.REPOSITORY, repositoryName)
                 .add(ModelKeys.SEQUENCER, name);

    }

    private List<ModelNode> parseIndexProviders( final XMLExtendedStreamReader reader,
                                                 final ModelNode parentAddress,
                                                 final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> providers = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INDEX_PROVIDER:
                    parseIndexProvider(reader, repositoryName, providers);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return providers;
    }

    private void parseIndexProvider( XMLExtendedStreamReader reader,
                                     String repositoryName,
                                     final List<ModelNode> providers ) throws XMLStreamException {

        final ModelNode provider = new ModelNode();
        provider.get(OP).set(ADD);
        String name = null;

        providers.add(provider);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case CLASSNAME:
                        ModelAttributes.CLASSNAME.parseAndSetParameter(attrValue, provider, reader);
                        if (name == null) name = attrValue;
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, provider, reader);
                        break;
                    case RELATIVE_TO:
                        ModelAttributes.RELATIVE_TO.parseAndSetParameter(attrValue, provider, reader);
                        break;
                    case PATH:
                        ModelAttributes.PATH.parseAndSetParameter(attrValue, provider, reader);
                        break;
                    default:
                        provider.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        break;
                }
            }
        }

        provider.get(OP_ADDR)
                .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                .add(ModelKeys.REPOSITORY, repositoryName)
                .add(ModelKeys.INDEX_PROVIDER, name);

        requireNoElements(reader);
    }

    private List<ModelNode> parseIndexes( final XMLExtendedStreamReader reader,
                                          final ModelNode parentAddress,
                                          final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> indexes = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INDEX:
                    parseIndex(reader, repositoryName, indexes);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return indexes;
    }

    private void parseIndex( XMLExtendedStreamReader reader,
                             String repositoryName,
                             final List<ModelNode> indexes ) throws XMLStreamException {

        final ModelNode index = new ModelNode();
        index.get(OP).set(ADD);
        String name = null;

        indexes.add(index);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case PROVIDER_NAME:
                        ModelAttributes.PROVIDER_NAME.parseAndSetParameter(attrValue, index, reader);
                        break;
                    case INDEX_KIND:
                        ModelAttributes.INDEX_KIND.parseAndSetParameter(attrValue, index, reader);
                        break;
                    case SYNCHRONOUS:
                        ModelAttributes.SYNCHRONOUS.parseAndSetParameter(attrValue, index, reader);
                        break;
                    case NODE_TYPE:
                        ModelAttributes.NODE_TYPE_NAME.parseAndSetParameter(attrValue, index, reader);
                        break;
                    case COLUMNS:
                        ModelAttributes.INDEX_COLUMNS.parseAndSetParameter(attrValue, index, reader);
                        break;
                    case WORKSPACES: 
                        ModelAttributes.WORKSPACES.parseAndSetParameter(attrValue, index, reader);
                        break;
                    default:
                       throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        index.get(OP_ADDR)
             .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
             .add(ModelKeys.REPOSITORY, repositoryName)
             .add(ModelKeys.INDEX, name);

        requireNoElements(reader);
    }

    private List<ModelNode> parseExternalSources( final XMLExtendedStreamReader reader,
                                                  final ModelNode parentAddress,
                                                  final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> externalSources = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SOURCE:
                    parseExternalSource(reader, repositoryName, externalSources);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return externalSources;
    }

    private void parseExternalSource( XMLExtendedStreamReader reader,
                                      String repositoryName,
                                      final List<ModelNode> externalSources ) throws XMLStreamException {

        final ModelNode externalSource = new ModelNode();
        externalSource.get(OP).set(ADD);
        String name = null;

        externalSources.add(externalSource);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case CLASSNAME:
                        ModelAttributes.CONNECTOR_CLASSNAME.parseAndSetParameter(attrValue, externalSource, reader);
                        if (name == null) {
                            name = attrValue;
                        }
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, externalSource, reader);
                        break;
                    case CACHEABLE: {
                        ModelAttributes.CACHEABLE.parseAndSetParameter(attrValue, externalSource, reader);
                        break;
                    }
                    case QUERYABLE: {
                        ModelAttributes.QUERYABLE.parseAndSetParameter(attrValue, externalSource, reader);
                        break;
                    }
                    case READONLY: {
                        ModelAttributes.READONLY.parseAndSetParameter(attrValue, externalSource, reader);
                        break;
                    }
                    case EXPOSE_AS_WORKSPACE: {
                        ModelAttributes.EXPOSE_AS_WORKSPACE.parseAndSetParameter(attrValue, externalSource, reader);
                        break;
                    }
                    default:
                        // extra attributes are allowed to set externalSource-specific properties ...
                        if (!attrValue.startsWith("$")) {
                            externalSource.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        } else {
                            externalSource.get(ModelKeys.PROPERTIES).add(attrName, new ValueExpression(attrValue));
                        }
                        break;
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROJECTION:
                    String value = reader.getElementText();
                    ModelAttributes.PROJECTIONS.parseAndAddParameterElement(value, externalSource, reader);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        externalSource.get(OP_ADDR)
                      .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                      .add(ModelKeys.REPOSITORY, repositoryName)
                      .add(ModelKeys.SOURCE, name);
    }

    private List<ModelNode> parseTextExtracting( final XMLExtendedStreamReader reader,
                                                 final ModelNode repository, 
                                                 final String repositoryName ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case THREAD_POOL_NAME:
                        ModelAttributes.TEXT_EXTRACTOR_THREAD_POOL_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case MAX_POOL_SIZE:
                        ModelAttributes.TEXT_EXTRACTOR_MAX_POOL_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        List<ModelNode> extractors = new ArrayList<ModelNode>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TEXT_EXTRACTOR:
                    parseTextExtractor(reader, repositoryName, extractors);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        return extractors;
    }

    private void parseTextExtractor( XMLExtendedStreamReader reader,
                                     String repositoryName,
                                     final List<ModelNode> extractors ) throws XMLStreamException {

        final ModelNode extractor = new ModelNode();
        extractor.get(OP).set(ADD);
        String name = null;

        extractors.add(extractor);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        name = attrValue;
                        break;
                    case CLASSNAME:
                        ModelAttributes.TEXT_EXTRACTOR_CLASSNAME.parseAndSetParameter(attrValue, extractor, reader);
                        if (name == null) name = attrValue;
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, extractor, reader);
                        break;
                    default:
                        // extra attributes are allowed to set extractor-specific properties ...
                        extractor.get(ModelKeys.PROPERTIES).add(attrName, attrValue);
                        break;
                }
            }
        }

        extractor.get(OP_ADDR)
                 .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
                 .add(ModelKeys.REPOSITORY, repositoryName)
                 .add(ModelKeys.TEXT_EXTRACTOR, name);

        requireNoElements(reader);
    }

    /**
     * Checks that the current element has no attributes, throwing an {@link javax.xml.stream.XMLStreamException} if one is found.
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    protected void requireNoElements( final XMLExtendedStreamReader reader ) throws XMLStreamException {
        if (reader.nextTag() != END_ELEMENT) {
            throw ParseUtils.unexpectedElement(reader);
        }
    }
}
