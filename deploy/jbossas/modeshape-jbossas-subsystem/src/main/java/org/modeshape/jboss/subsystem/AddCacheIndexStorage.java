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

import java.util.List;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexStorage;
import org.modeshape.jboss.service.IndexStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

/**
 *
 */
public class AddCacheIndexStorage extends AbstractAddIndexStorage {

    public static final AddCacheIndexStorage INSTANCE = new AddCacheIndexStorage();

    private String cacheContainerName = null;

    private AddCacheIndexStorage() {
    }

    @Override
    protected void writeIndexStorageConfiguration( final OperationContext context,
                                                   final ModelNode storage,
                                                   EditableDocument indexStorage ) throws OperationFailedException {
        indexStorage.set(FieldName.TYPE, FieldValue.INDEX_STORAGE_INFINISPAN);

        this.cacheContainerName = ModelAttributes.INDEX_STORAGE_CACHE_CONTAINER.resolveModelAttribute(context, storage)
                                                                               .asString();
        indexStorage.set(FieldName.CACHE_CONFIGURATION, cacheContainerJNDIBinding(cacheContainerName));

        int chunkSize = ModelAttributes.CHUNK_SIZE.resolveModelAttribute(context, storage).asInt();
        indexStorage.set(FieldName.INDEX_STORAGE_INFINISPAN_CHUNK_SIZE_IN_BYTES, chunkSize);

        if (storage.hasDefined(ModelKeys.LOCK_CACHE_NAME)) {
            String lockCache = ModelAttributes.LOCK_CACHE_NAME.resolveModelAttribute(context, storage).asString();
            indexStorage.set(FieldName.INDEX_STORAGE_INFINISPAN_LOCK_CACHE, lockCache);
        }

        if (storage.hasDefined(ModelKeys.DATA_CACHE_NAME)) {
            String dataCache = ModelAttributes.DATA_CACHE_NAME.resolveModelAttribute(context, storage).asString();
            indexStorage.set(FieldName.INDEX_STORAGE_INFINISPAN_DATA_CACHE, dataCache);
        }

        if (storage.hasDefined(ModelKeys.METADATA_CACHE_NAME)) {
            String metaCache = ModelAttributes.METADATA_CACHE_NAME.resolveModelAttribute(context, storage).asString();
            indexStorage.set(FieldName.INDEX_STORAGE_INFINISPAN_META_CACHE, metaCache);
        }
    }

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  IndexStorageService service,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceBuilder<IndexStorage> builder,
                                                  ServiceTarget target ) /*throws OperationFailedException*/{
        // there is a cache container different from the cache container of the repository, so we need to inject it
        // in order for it to be started preemptively
        builder.addDependency(ServiceName.JBOSS.append("infinispan", this.cacheContainerName),
                              CacheContainer.class,
                              service.getCacheContainerInjectedValue());
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.CACHE_INDEX_STORAGE, ModelAttributes.CACHE_INDEX_STORAGE_ATTRIBUTES);
    }

    private String cacheContainerJNDIBinding( String cacheContainerName ) {
        return "java:jboss/infinispan/container/" + cacheContainerName;
    }

}
