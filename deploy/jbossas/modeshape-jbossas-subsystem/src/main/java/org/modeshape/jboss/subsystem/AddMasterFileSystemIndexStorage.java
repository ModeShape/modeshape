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
        populate(operation, model, ModelKeys.MASTER_FILE_INDEX_STORAGE, ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES);
    }
}
