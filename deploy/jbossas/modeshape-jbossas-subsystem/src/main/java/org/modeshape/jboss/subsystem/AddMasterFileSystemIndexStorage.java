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

import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

/**
 *  Handler for master-file-index-storage
 */
public class AddMasterFileSystemIndexStorage extends AbstractAddFileSystemIndexStorage {

    public static final AddMasterFileSystemIndexStorage INSTANCE = new AddMasterFileSystemIndexStorage();

    private AddMasterFileSystemIndexStorage() {
    }

    @Override
    protected void writeIndexingBackendConfiguration( OperationContext context,
                                                      ModelNode storage,
                                                      EditableDocument backend ) throws OperationFailedException {
        // Set the type of indexing backend ...
        backend.set(FieldName.TYPE, FieldValue.INDEXING_BACKEND_TYPE_JMS_MASTER);
        String connJndi = ModelAttributes.CONNECTION_FACTORY_JNDI_NAME.resolveModelAttribute(context, storage).asString();
        String queueJndi = ModelAttributes.QUEUE_JNDI_NAME.resolveModelAttribute(context, storage).asString();
        backend.set(FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME, connJndi);
        backend.set(FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME, queueJndi);
    }

    @Override
    protected void writeIndexStorageConfiguration( OperationContext context,
                                                   ModelNode storage,
                                                   EditableDocument indexStorage,
                                                   String repositoryName ) throws OperationFailedException {
        // Set the type of storage ...
        indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_FILESYSTEM_MASTER);

        processLocalIndexStorageLocation(context, storage, repositoryName, indexStorage);
        processSourceIndexStorageLocation(context, storage, repositoryName, indexStorage);

        String accessType = ModelAttributes.ACCESS_TYPE.resolveModelAttribute(context, storage).asString();
        indexStorage.set(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE, accessType.toLowerCase());

        String locking = ModelAttributes.LOCKING_STRATEGY.resolveModelAttribute(context, storage).asString();
        indexStorage.set(FieldName.INDEX_STORAGE_LOCKING_STRATEGY, locking.toLowerCase());

        String refresh = ModelAttributes.REFRESH_PERIOD.resolveModelAttribute(context, storage).asString();
        indexStorage.set(FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS, refresh);
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES);
    }
}
