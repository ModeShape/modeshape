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
public class AddCassandraBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddCassandraBinaryStorage INSTANCE = new AddCassandraBinaryStorage();

    private AddCassandraBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_CASSANDRA);
        String address = ModelAttributes.CASSANDRA_HOST.resolveModelAttribute(context, model).asString();
        binaries.setString(FieldName.ADDRESS, address);
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.CASSANDRA_BINARY_STORAGE_ATTRIBUTES);
    }

}
