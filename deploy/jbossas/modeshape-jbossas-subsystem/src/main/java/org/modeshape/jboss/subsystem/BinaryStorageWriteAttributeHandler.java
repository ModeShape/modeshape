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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;

/**
 * {@link OperationStepHandler} implementations that handles changes to the model values for the binary storage submodel's
 * {@link AttributeDefinition attribute definitions}, including the different kinds of binary storage. Those attributes that can
 * be changed {@link org.jboss.as.controller.registry.AttributeAccess.Flag#RESTART_NONE RESTART_NONE without restarting} will be
 * immediately reflected in the repository's configuration; other attributes will be changed in the submodel and used upon the
 * next restart.
 */
public class BinaryStorageWriteAttributeHandler extends AbstractRepositoryConfigWriteAttributeHandler {

    static final BinaryStorageWriteAttributeHandler TRANSIENT_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.TRANSIENT_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler FILE_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.FILE_BINARY_STORAGE_ATTRIBUTES);
    
    static final BinaryStorageWriteAttributeHandler DATABASE_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.DATABASE_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler CASSANDRA_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.CASSANDRA_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler MONGO_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.MONGO_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler S3_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.S3_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler COMPOSITE_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.COMPOSITE_BINARY_STORAGE_ATTRIBUTES);

    static final BinaryStorageWriteAttributeHandler CUSTOM_BINARY_STORAGE_INSTANCE = new BinaryStorageWriteAttributeHandler(
            ModelAttributes.CUSTOM_BINARY_STORAGE_ATTRIBUTES);

    private BinaryStorageWriteAttributeHandler( AttributeDefinition... attributes ) {
        super(attributes);
    }
}
