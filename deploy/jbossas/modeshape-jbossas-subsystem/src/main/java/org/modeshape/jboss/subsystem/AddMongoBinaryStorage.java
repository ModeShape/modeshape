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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.schematic.document.EditableDocument;

/**
 * 
 */
public class AddMongoBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddMongoBinaryStorage INSTANCE = new AddMongoBinaryStorage();

    private AddMongoBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_MONGO);
    
        boolean singleHost = false;
        ModelNode hostNode = ModelAttributes.MONGO_HOST.resolveModelAttribute(context, model);
        if (hostNode.isDefined()) {
            binaries.setString(FieldName.HOST, hostNode.asString());
            singleHost = true;
        }
    
        ModelNode portNode = ModelAttributes.MONGO_PORT.resolveModelAttribute(context, model);
        if (portNode.isDefined()) {
            binaries.setNumber(FieldName.PORT, portNode.asInt());
        } else if (singleHost) {
            throw new OperationFailedException("The MongoDB port is required if the 'host' attribute is present"); 
        }
        
        ModelNode databaseNode = ModelAttributes.MONGO_DATABASE.resolveModelAttribute(context, model);
        if (databaseNode.isDefined()) {
            binaries.setString(FieldName.DATABASE, databaseNode.asString());
        }
        ModelNode userNode = ModelAttributes.MONGO_USERNAME.resolveModelAttribute(context, model);
        if (userNode.isDefined()) {
            binaries.setString(FieldName.USER_NAME, userNode.asString());
        }
        ModelNode passwordNode = ModelAttributes.MONGO_PASSWORD.resolveModelAttribute(context, model);
        if (passwordNode.isDefined()) {
            binaries.setString(FieldName.USER_PASSWORD, passwordNode.asString());
        }
        ModelNode hostAddressesNode = ModelAttributes.MONGO_HOST_ADDRESSES.resolveModelAttribute(context, model);
        if (hostAddressesNode.isDefined()) {
            binaries.setArray(FieldName.HOST_ADDRESSES, (Object[]) hostAddressesNode.asString().split(","));
        } else if (!singleHost) {
            throw new OperationFailedException("Either 'host' and 'port' OR 'host-addresses' have to be provided for the MongoDB binary store");
        }
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.MONGO_BINARY_STORAGE_ATTRIBUTES);
    }

}
