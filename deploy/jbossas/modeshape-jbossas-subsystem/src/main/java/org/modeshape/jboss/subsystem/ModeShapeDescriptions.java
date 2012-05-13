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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 */
public class ModeShapeDescriptions {

    public static final String RESOURCE_NAME = ModeShapeDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private ModeShapeDescriptions() {
        // Hide
    }

    static ModelNode getSequencerDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "repository.sequencer";
        final ModelNode sequencer = createDescription(resources, keyPrefix);
        // attributes
        for (AttributeDefinition attr : ModelAttributes.SEQUENCER_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, keyPrefix, sequencer);
        }
        return sequencer;
    }

    static ModelNode getSequencerAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "repository.sequencer.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.SEQUENCER_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.sequencer", op);
        }
        return op;
    }

    static ModelNode getSequencerRemoveDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "repository.sequencer.remove");
        return op;
    }

    static ModelNode getIndexStorageDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "repository.index-storage";
        final ModelNode indexes = createDescription(resources, keyPrefix);
        return indexes;
    }

    static ModelNode getRamIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_RAM_INDEX_STORAGE,
                                                        resources,
                                                        "repository.ram-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.RAM_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.ram-index-storage", op);
        }
        return op;
    }

    static ModelNode getCacheIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_CACHE_INDEX_STORAGE,
                                                        resources,
                                                        "repository.cache-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.CACHE_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.cache-index-storage", op);
        }
        return op;
    }

    static ModelNode getCustomIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_CUSTOM_INDEX_STORAGE,
                                                        resources,
                                                        "repository.custom-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.CUSTOM_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.custom-index-storage", op);
        }
        return op;
    }

    static ModelNode getLocalFileIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_LOCAL_FILE_INDEX_STORAGE,
                                                        resources,
                                                        "repository.local-file-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.local-file-index-storage", op);
        }
        return op;
    }

    static ModelNode getMasterFileIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_MASTER_FILE_INDEX_STORAGE,
                                                        resources,
                                                        "repository.master-file-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.master-file-index-storage", op);
        }
        return op;
    }

    static ModelNode getSlaveFileIndexStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.ADD_SLAVE_FILE_INDEX_STORAGE,
                                                        resources,
                                                        "repository.slave-file-index-storage.add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "repository.slave-file-index-storage", op);
        }
        return op;
    }

    static ModelNode getIndexStorageRemoveDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ModelKeys.REMOVE_INDEX_STORAGE,
                                                        resources,
                                                        "repository.index-storage.remove");
        return op;
    }

    static ModelNode getBinaryStorageDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "repository.binary-storage";
        final ModelNode binaries = createDescription(resources, keyPrefix);
        return binaries;
    }

    static ModelNode getFileBinaryStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final String key = "repository.file-binary-storage";
        final ModelNode op = createOperationDescription(ModelKeys.ADD_FILE_BINARY_STORAGE, resources, key + ".add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.FILE_BINARY_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, key, op);
        }
        return op;
    }

    static ModelNode getBinaryStorageRemoveDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final String key = "repository.binary-storage";
        final ModelNode op = createOperationDescription(ModelKeys.REMOVE_BINARY_STORAGE, resources, key + ".remove");
        return op;
    }

    static ModelNode getCacheBinaryStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final String key = "repository.cache-binary-storage";
        final ModelNode op = createOperationDescription(ModelKeys.ADD_CACHE_BINARY_STORAGE, resources, key + ".add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.CACHE_BINARY_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, key, op);
        }
        return op;
    }

    static ModelNode getDatabaseBinaryStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final String key = "repository.db-binary-storage";
        final ModelNode op = createOperationDescription(ModelKeys.ADD_DB_BINARY_STORAGE, resources, key + ".add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.DATABASE_BINARY_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, key, op);
        }
        return op;
    }

    static ModelNode getCustomBinaryStorageAddDescription( Locale locale ) {
        ResourceBundle resources = getResources(locale);
        final String key = "repository.custom-binary-storage";
        final ModelNode op = createOperationDescription(ModelKeys.ADD_CUSTOM_BINARY_STORAGE, resources, key + ".add");
        // request parameters
        for (AttributeDefinition attr : ModelAttributes.CUSTOM_BINARY_STORAGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, key, op);
        }
        return op;
    }

    static ResourceBundle getResources( Locale locale ) {
        return ResourceBundle.getBundle(RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
    }

    private static ModelNode createDescription( ResourceBundle resources,
                                                String key ) {
        return createOperationDescription(null, resources, key);
    }

    private static ModelNode createOperationDescription( String operation,
                                                         ResourceBundle resources,
                                                         String key ) {
        ModelNode description = new ModelNode();
        if (operation != null) {
            description.get(OPERATION_NAME).set(operation);
        }
        description.get(DESCRIPTION).set(resources.getString(key));
        return description;
    }
}
