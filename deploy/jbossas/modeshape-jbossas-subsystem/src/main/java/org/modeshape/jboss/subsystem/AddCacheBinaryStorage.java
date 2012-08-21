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
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jboss.service.RepositoryService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

/**
 * 
 */
public class AddCacheBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddCacheBinaryStorage INSTANCE = new AddCacheBinaryStorage();

    private String containerName;

    private AddCacheBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_CACHE);
        int minBinSize = ModelAttributes.MINIMUM_BINARY_SIZE.resolveModelAttribute(context, model).asInt();
        binaries.set(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, minBinSize);
        String defaultDataCache = repositoryName + "-binary-data";
        String defaultMetaCache = repositoryName + "-binary-metadata";
        ModelNode dataNode = ModelAttributes.DATA_CACHE_NAME.resolveModelAttribute(context, model);
        ModelNode metaNode = ModelAttributes.METADATA_CACHE_NAME.resolveModelAttribute(context, model);
        String dataCache = dataNode.isDefined() ? dataNode.asString() : defaultDataCache;
        String metaCache = metaNode.isDefined() ? metaNode.asString() : defaultMetaCache;
        binaries.set(FieldName.DATA_CACHE_NAME, dataCache);
        binaries.set(FieldName.METADATA_CACHE_NAME, metaCache);
        if (ModelAttributes.CACHE_CONTAINER.isMarshallable(model, false)) {
            // There's a non-default value ...
            containerName = ModelAttributes.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
            // The proper container will be injected into the RepositoryService, so use the fixed container name ...
            binaries.set(FieldName.CACHE_CONFIGURATION, RepositoryService.BINARY_STORAGE_CONTAINER_NAME);
        }
    }

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target ) {
        if (containerName != null) {
            builder.addDependency(ServiceName.JBOSS.append("infinispan", containerName),
                                  CacheContainer.class,
                                  service.getBinaryCacheManagerInjector());
        }
        // otherwise, we'll use the content cache container that the RepositoryService is already dependent upon ...
    }
    
    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.CACHE_BINARY_STORAGE, ModelAttributes.CACHE_BINARY_STORAGE_ATTRIBUTES);
    }
}
