/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.parsing.ParseUtils.missingOneOf;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

public class ModeShapeSubsystemXMLReader_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

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

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (reader.isStartElement()) {
                // elements
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case MODESHAPE_1_0:
                        Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case REPOSITORY:
                                parseRepository(reader, subsystem, repositories);
                                break;
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    case UNKNOWN:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        list.addAll(repositories);

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
                        repositoryAddress.add("repository", attrValue); //$NON-NLS-1$
                        repositoryAddress.protect();
                        repository.get(OP).set(ADD);
                        repository.get(OP_ADDR).set(repositoryAddress);
                        repositories.add(repository);
                        break;
                    case CACHE_NAME:
                        ModelAttributes.CACHE_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case CACHE_CONTAINER:
                        ModelAttributes.CACHE_CONTAINER.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case JNDI_NAME:
                        ModelAttributes.JNDI_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ENABLE_MONITORING:
                        ModelAttributes.ENABLE_MONITORING.parseAndSetParameter(attrValue, repository, reader);
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
                    case CLUSTER_STACK:
                        ModelAttributes.CLUSTER_STACK.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case CLUSTER_NAME:
                        ModelAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ModelNode indexStorage = null;
        ModelNode binaryStorage = null;
        List<ModelNode> sequencers = new ArrayList<ModelNode>();
        List<ModelNode> textExtractors = new ArrayList<ModelNode>();
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case WORKSPACES:
                    parseWorkspaces(reader, address, repository);
                    break;
                case INDEXING:
                    parseIndexing(reader, address, repository);
                    break;

                // Index storage ...
                case RAM_INDEX_STORAGE:
                    indexStorage = parseRamIndexStorage(reader, repositoryName);
                    break;
                case LOCAL_FILE_INDEX_STORAGE:
                    indexStorage = parseFileIndexStorage(reader,
                                                         repositoryName,
                                                         ModelKeys.ADD_LOCAL_FILE_INDEX_STORAGE,
                                                         ModelKeys.LOCAL_FILE_INDEX_STORAGE);
                    break;
                case MASTER_FILE_INDEX_STORAGE:
                    indexStorage = parseFileIndexStorage(reader,
                                                         repositoryName,
                                                         ModelKeys.ADD_MASTER_FILE_INDEX_STORAGE,
                                                         ModelKeys.MASTER_FILE_INDEX_STORAGE);
                    break;
                case SLAVE_FILE_INDEX_STORAGE:
                    indexStorage = parseFileIndexStorage(reader,
                                                         repositoryName,
                                                         ModelKeys.ADD_SLAVE_FILE_INDEX_STORAGE,
                                                         ModelKeys.SLAVE_FILE_INDEX_STORAGE);
                    break;
                case CACHE_INDEX_STORAGE:
                    indexStorage = parseCacheIndexStorage(reader, repositoryName);
                    break;
                case CUSTOM_INDEX_STORAGE:
                    indexStorage = parseCustomIndexStorage(reader, repositoryName);
                    break;

                // Binary storage ...
                case FILE_BINARY_STORAGE:
                    binaryStorage = parseFileBinaryStorage(reader, repositoryName);
                    break;
                case DB_BINARY_STORAGE:
                    binaryStorage = parseDatabaseBinaryStorage(reader, repositoryName);
                    break;
                case CACHE_BINARY_STORAGE:
                    binaryStorage = parseCacheBinaryStorage(reader, repositoryName);
                    break;
                case CUSTOM_BINARY_STORAGE:
                    binaryStorage = parseCustomBinaryStorage(reader, repositoryName);
                    break;

                // Authentication ...
                case AUTHENTICATORS:
                    parseAuthenticators(reader, address, repository);
                    break;
                // Sequencing ...
                case SEQUENCERS:
                    sequencers = parseSequencers(reader, address, repositoryName);
                    break;
                // Text extracting ...
                case TEXT_EXTRACTORS:
                    textExtractors = parseTextExtracting(reader, repositoryName);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        if (indexStorage != null) repositories.add(indexStorage);
        if (binaryStorage != null) repositories.add(binaryStorage);
        repositories.addAll(sequencers);
        repositories.addAll(textExtractors);
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
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseWorkspace( final XMLExtendedStreamReader reader,
                                 final ModelNode repository ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case NAME:
                        repository.get(ModelKeys.PREDEFINED_WORKSPACE_NAMES).add(attrValue);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoElements(reader);
    }

    private void parseIndexing( final XMLExtendedStreamReader reader,
                                final ModelNode parentAddress,
                                final ModelNode repository ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute element = Attribute.forName(attrName);
                switch (element) {
                    case REBUILD_UPON_STARTUP:
                        ModelAttributes.REBUILD_INDEXES_UPON_STARTUP.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case THREAD_POOL:
                        ModelAttributes.THREAD_POOL.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case BATCH_SIZE:
                        ModelAttributes.BATCH_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case READER_STRATEGY:
                        ModelAttributes.READER_STRATEGY.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case MODE:
                        ModelAttributes.MODE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case SYSTEM_CONTENT_MODE:
                        ModelAttributes.SYSTEM_CONTENT_MODE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ASYNC_THREAD_POOL_SIZE:
                        ModelAttributes.ASYNC_THREAD_POOL_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ASYNC_MAX_QUEUE_SIZE:
                        ModelAttributes.ASYNC_MAX_QUEUE_SIZE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ANALYZER_CLASSNAME:
                        ModelAttributes.ANALYZER_CLASSNAME.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    case ANALYZER_MODULE:
                        ModelAttributes.ANALYZER_MODULE.parseAndSetParameter(attrValue, repository, reader);
                        break;
                    default:
                        if (attrName.startsWith("hibernate")) {
                            repository.get(attrName).set(attrValue);
                        } else {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                }
            }
        }

        requireNoElements(reader);

    }

    private ModelNode parseRamIndexStorage( final XMLExtendedStreamReader reader,
                                            final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_RAM_INDEX_STORAGE);
        storage.get(ModelKeys.INDEX_STORAGE_TYPE).set(ModelKeys.RAM_INDEX_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute element = Attribute.forName(attrName);
                switch (element) {
                    case THREAD_POOL:
                        ModelAttributes.THREAD_POOL.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case BATCH_SIZE:
                        ModelAttributes.BATCH_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case READER_STRATEGY:
                        ModelAttributes.READER_STRATEGY.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MODE:
                        ModelAttributes.MODE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_THREAD_POOL_SIZE:
                        ModelAttributes.ASYNC_THREAD_POOL_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_MAX_QUEUE_SIZE:
                        ModelAttributes.ASYNC_MAX_QUEUE_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_CLASSNAME:
                        ModelAttributes.ANALYZER_CLASSNAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_MODULE:
                        ModelAttributes.ANALYZER_MODULE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        storage.get(attrName).set(attrValue);
                        break;
                }
            }
        }

        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.INDEX_STORAGE, ModelKeys.INDEX_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseFileIndexStorage( final XMLExtendedStreamReader reader,
                                             final String repositoryName,
                                             String operationName,
                                             String name ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(operationName);
        storage.get(ModelKeys.INDEX_STORAGE_TYPE).set(name);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // Set these as properties on the storage ModelNode ...
                    case FORMAT:
                        ModelAttributes.INDEX_FORMAT.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case REBUILD_UPON_STARTUP:
                        ModelAttributes.REBUILD_INDEXES_UPON_STARTUP.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case THREAD_POOL:
                        ModelAttributes.THREAD_POOL.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case BATCH_SIZE:
                        ModelAttributes.BATCH_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case READER_STRATEGY:
                        ModelAttributes.READER_STRATEGY.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MODE:
                        ModelAttributes.MODE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_THREAD_POOL_SIZE:
                        ModelAttributes.ASYNC_THREAD_POOL_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_MAX_QUEUE_SIZE:
                        ModelAttributes.ASYNC_MAX_QUEUE_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_CLASSNAME:
                        ModelAttributes.ANALYZER_CLASSNAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_MODULE:
                        ModelAttributes.ANALYZER_MODULE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    // These are file-related
                    case RELATIVE_TO:
                        ModelAttributes.RELATIVE_TO.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case PATH:
                        ModelAttributes.PATH.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ACCESS_TYPE:
                        ModelAttributes.ACCESS_TYPE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case LOCKING_STRATEGY:
                        ModelAttributes.LOCKING_STRATEGY.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case REFRESH_PERIOD:
                        ModelAttributes.REFRESH_PERIOD.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case COPY_BUFFER_SIZE:
                        ModelAttributes.COPY_BUFFER_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case SOURCE_PATH:
                        ModelAttributes.SOURCE_PATH.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case SOURCE_RELATIVE_TO:
                        ModelAttributes.SOURCE_RELATIVE_TO.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    // These are JMS-related
                    case CONNECTION_FACTORY_JNDI_NAME:
                        ModelAttributes.CONNECTION_FACTORY_JNDI_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case QUEUE_JNDI_NAME:
                        ModelAttributes.QUEUE_JNDI_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        storage.get(attrName).set(attrValue);
                        break;
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.INDEX_STORAGE, ModelKeys.INDEX_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseCacheIndexStorage( final XMLExtendedStreamReader reader,
                                              final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_CACHE_INDEX_STORAGE);
        storage.get(ModelKeys.INDEX_STORAGE_TYPE).set(ModelKeys.CACHE_INDEX_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case FORMAT:
                        ModelAttributes.INDEX_FORMAT.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case LOCK_CACHE_NAME:
                        ModelAttributes.LOCK_CACHE_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case DATA_CACHE_NAME:
                        ModelAttributes.DATA_CACHE_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case META_CACHE_NAME:
                        ModelAttributes.METADATA_CACHE_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case CACHE_CONTAINER_JNDI_NAME:
                        ModelAttributes.CACHE_CONTAINER_JNDI_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        storage.get(attrName).set(attrValue);
                        break;
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.INDEX_STORAGE, ModelKeys.INDEX_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseCustomIndexStorage( final XMLExtendedStreamReader reader,
                                               final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_CUSTOM_INDEX_STORAGE);
        storage.get(ModelKeys.INDEX_STORAGE_TYPE).set(ModelKeys.CUSTOM_INDEX_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // Set these as properties on the repository ModelNode ...
                    case FORMAT:
                        ModelAttributes.INDEX_FORMAT.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case REBUILD_UPON_STARTUP:
                        ModelAttributes.REBUILD_INDEXES_UPON_STARTUP.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case THREAD_POOL:
                        ModelAttributes.THREAD_POOL.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case BATCH_SIZE:
                        ModelAttributes.BATCH_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case READER_STRATEGY:
                        ModelAttributes.READER_STRATEGY.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MODE:
                        ModelAttributes.MODE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_THREAD_POOL_SIZE:
                        ModelAttributes.ASYNC_THREAD_POOL_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ASYNC_MAX_QUEUE_SIZE:
                        ModelAttributes.ASYNC_MAX_QUEUE_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_CLASSNAME:
                        ModelAttributes.ANALYZER_CLASSNAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case ANALYZER_MODULE:
                        ModelAttributes.ANALYZER_MODULE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    // The rest go on the ModelNode for the type ...
                    case CLASSNAME:
                        ModelAttributes.CLASSNAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        storage.get(attrName).set(attrValue);
                        break;
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.INDEX_STORAGE, ModelKeys.INDEX_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseFileBinaryStorage( final XMLExtendedStreamReader reader,
                                              final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_FILE_BINARY_STORAGE);
        storage.get(ModelKeys.BINARY_STORAGE_TYPE).set(ModelKeys.FILE_BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // The rest go on the ModelNode for the type ...
                    case RELATIVE_TO:
                        ModelAttributes.RELATIVE_TO.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case PATH:
                        ModelAttributes.RELATIVE_TO.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.BINARY_STORAGE, ModelKeys.BINARY_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseCacheBinaryStorage( final XMLExtendedStreamReader reader,
                                               final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_CACHE_BINARY_STORAGE);
        storage.get(ModelKeys.BINARY_STORAGE_TYPE).set(ModelKeys.CACHE_BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // The rest go on the ModelNode for the type ...
                    case DATA_CACHE_NAME:
                        ModelAttributes.DATA_CACHE_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case META_CACHE_NAME:
                        ModelAttributes.METADATA_CACHE_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case CACHE_CONTAINER:
                        ModelAttributes.CACHE_CONTAINER.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.BINARY_STORAGE, ModelKeys.BINARY_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseDatabaseBinaryStorage( final XMLExtendedStreamReader reader,
                                                  final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_DB_BINARY_STORAGE);
        storage.get(ModelKeys.BINARY_STORAGE_TYPE).set(ModelKeys.DB_BINARY_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // The rest go on the ModelNode for the type ...
                    case DATA_SOURCE_JNDI_NAME:
                        ModelAttributes.DATA_SOURCE_JNDI_NAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.BINARY_STORAGE, ModelKeys.BINARY_STORAGE_NAME);

        return storage;
    }

    private ModelNode parseCustomBinaryStorage( final XMLExtendedStreamReader reader,
                                                final String repositoryName ) throws XMLStreamException {
        final ModelNode storage = new ModelNode();
        storage.get(OP).set(ModelKeys.ADD_CUSTOM_BINARY_STORAGE);
        storage.get(ModelKeys.BINARY_STORAGE_TYPE).set(ModelKeys.CUSTOM_INDEX_STORAGE);

        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    case CLASSNAME:
                        ModelAttributes.CLASSNAME.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    case MIN_VALUE_SIZE:
                        ModelAttributes.MINIMUM_BINARY_SIZE.parseAndSetParameter(attrValue, storage, reader);
                        break;
                    default:
                        storage.get(attrName).set(attrValue);
                        break;
                }
            }
        }
        requireNoElements(reader);

        storage.get(OP_ADDR)
               .add(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME)
               .add(ModelKeys.REPOSITORY, repositoryName)
               .add(ModelKeys.BINARY_STORAGE, ModelKeys.BINARY_STORAGE_NAME);

        return storage;
    }

    private void parseAuthenticators( final XMLExtendedStreamReader reader,
                                      final ModelNode parentAddress,
                                      final ModelNode repository ) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case AUTHENTICATOR: {
                    parseAuthenticator(reader, repository);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseAuthenticator( final XMLExtendedStreamReader reader,
                                     final ModelNode repository ) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            ModelNode authenticator = new ModelNode();
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(attrName);
                switch (attribute) {
                    // Set these as properties on the repository ModelNode ...
                    case NAME:
                        ModelAttributes.NAME.parseAndSetParameter(attrValue, authenticator, reader);
                        break;
                    case CLASSNAME:
                        ModelAttributes.CLASSNAME.parseAndSetParameter(attrValue, authenticator, reader);
                        if (!authenticator.has(ModelKeys.NAME)) {
                            ModelAttributes.NAME.parseAndSetParameter(attrValue, authenticator, reader);
                        }
                        break;
                    case MODULE:
                        ModelAttributes.MODULE.parseAndSetParameter(attrValue, authenticator, reader);
                        break;
                    default:
                        authenticator.get(attrName).set(attrValue);
                        break;
                }
            }
            if (authenticator.has(ModelKeys.NAME)) repository.get(ModelKeys.AUTHENTICATORS).add(authenticator);
        }
        requireNoElements(reader);
    }

    private List<ModelNode> parseSequencers( final XMLExtendedStreamReader reader,
                                             final ModelNode parentAddress,
                                             final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

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
                        sequencer.get(attrName).set(attrValue);
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

    private List<ModelNode> parseTextExtracting( final XMLExtendedStreamReader reader,
                                                 final String repositoryName ) throws XMLStreamException {
        requireNoAttributes(reader);

        List<ModelNode> extractors = new ArrayList<ModelNode>();
        while (reader.hasNext()  && reader.nextTag() != END_ELEMENT) {
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
                        extractor.get(attrName).set(attrValue);
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
