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
        String defaultDataCache = repositoryName + "-binary-data";
        String defaultMetaCache = repositoryName + "-binary-metadata";
        ModelNode dataNode = ModelAttributes.DATA_CACHE_NAME.resolveModelAttribute(context, model);
        ModelNode metaNode = ModelAttributes.METADATA_CACHE_NAME.resolveModelAttribute(context, model);
        String dataCache = dataNode.isDefined() ? dataNode.asString() : defaultDataCache;
        String metaCache = metaNode.isDefined() ? metaNode.asString() : defaultMetaCache;
        binaries.set(FieldName.DATA_CACHE_NAME, dataCache);
        binaries.set(FieldName.METADATA_CACHE_NAME, metaCache);
        ModelNode chunkSize = ModelAttributes.CHUNK_SIZE.resolveModelAttribute(context, model);
        if (chunkSize.isDefined()) {
            binaries.set(FieldName.CHUNK_SIZE, chunkSize.asInt());
        }
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
                                                  ServiceTarget target,
                                                  String binariesStoreName ) {
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
        populate(operation, model, ModelAttributes.CACHE_BINARY_STORAGE_ATTRIBUTES);
    }
}
