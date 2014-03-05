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
 * Handler for slave-file-index-storage
 */
public class AddSlaveFileSystemIndexStorage extends AbstractAddFileSystemIndexStorage {

    public static final AddSlaveFileSystemIndexStorage INSTANCE = new AddSlaveFileSystemIndexStorage();

    private AddSlaveFileSystemIndexStorage() {
    }

    @Override
    protected void writeIndexingBackendConfiguration( OperationContext context,
                                                      ModelNode storage,
                                                      EditableDocument backend ) throws OperationFailedException {
        // Set the type of indexing backend ...
        backend.set(FieldName.TYPE, FieldValue.INDEXING_BACKEND_TYPE_JMS_SLAVE);
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
        indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_FILESYSTEM_SLAVE);

        processLocalIndexStorageLocation(context, storage, repositoryName, indexStorage);
        processSourceIndexStorageLocation(context, storage, repositoryName, indexStorage);

        String accessType = ModelAttributes.ACCESS_TYPE.resolveModelAttribute(context, storage).asString();
        indexStorage.set(FieldName.INDEX_STORAGE_FILE_SYSTEM_ACCESS_TYPE, accessType.toLowerCase());

        String locking = ModelAttributes.LOCKING_STRATEGY.resolveModelAttribute(context, storage).asString();
        indexStorage.set(FieldName.INDEX_STORAGE_LOCKING_STRATEGY, locking.toLowerCase());

        int refresh = ModelAttributes.REFRESH_PERIOD.resolveModelAttribute(context, storage).asInt();
        indexStorage.set(FieldName.INDEX_STORAGE_REFRESH_IN_SECONDS, refresh);

        int copyBufferSize = ModelAttributes.COPY_BUFFER_SIZE.resolveModelAttribute(context, storage).asInt();
        indexStorage.set(FieldName.INDEX_STORAGE_COPY_BUFFER_SIZE_IN_MEGABYTES, copyBufferSize);

        int retryLookup = ModelAttributes.RETRY_MARKER_LOOKUP.resolveModelAttribute(context, storage).asInt();
        indexStorage.set(FieldName.INDEX_STORAGE_RETRY_MARKER_LOOKUP, retryLookup);

        int retryPeriod = ModelAttributes.RETRY_INITIALIZE_PERIOD.resolveModelAttribute(context, storage).asInt();
        indexStorage.set(FieldName.INDEX_STORAGE_RETRY_INITIALIZE_PERIOD_IN_SECONDS, retryPeriod);
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES);
    }
}
