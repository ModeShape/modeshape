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
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexStorage;
import org.modeshape.jboss.service.IndexStorageService;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 *  Base class for parsing index storage configurations which use FS configurations.
 */
public abstract class AbstractAddFileSystemIndexStorage extends AbstractAddIndexStorage {

    protected String indexStoragePathInDataDirectory;
    protected String indexSourcePathInDataDirectory;

    protected AbstractAddFileSystemIndexStorage() {
    }

    protected void setIndexStoragePathInDataDirectory( String relativePath ) {
        indexStoragePathInDataDirectory = relativePath;
    }

    protected void setIndexSourcePathInDataDirectory( String relativePath ) {
        indexSourcePathInDataDirectory = relativePath;
    }

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  IndexStorageService service,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceBuilder<IndexStorage> builder,
                                                  ServiceTarget target ) {
        if (indexStoragePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName serviceName = ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName);
            newControllers.add(RelativePathService.addService(serviceName,
                                                              indexStoragePathInDataDirectory,
                                                              ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE,
                                                              target));
            // and add dependency on this path ...
            builder.addDependency(serviceName, String.class, service.getIndexStorageBasePathInjector());
        }
        if (indexSourcePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName serviceName = ModeShapeServiceNames.indexSourceStorageDirectoryServiceName(repositoryName);
            newControllers.add(RelativePathService.addService(serviceName,
                                                              indexSourcePathInDataDirectory,
                                                              ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE,
                                                              target));
            builder.addDependency(serviceName, String.class, service.getIndexStorageSourceBasePathInjector());
        }
    }

    protected void processLocalIndexStorageLocation( OperationContext context,
                                                     ModelNode modelNode,
                                                     String repositoryName,
                                                     EditableDocument indexStorage ) throws OperationFailedException {
        ModelNode pathNode = ModelAttributes.PATH.resolveModelAttribute(context, modelNode);
        String path = pathNode.isDefined() ? pathNode.asString() : "modeshape/" + repositoryName + "/indexes";

        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, modelNode).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            //the relative-to path should be the default jboss-data-dir. Setting to an empty string will trigger path injection
            setIndexStoragePathInDataDirectory(".");
            //this only contains the path because the IndexStorageService will create the "end-path"
            indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_LOCATION, path);
        } else {
            if (!relativeTo.endsWith("/")) {
                relativeTo = relativeTo + "/";
            }
            indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_LOCATION, relativeTo + path);
        }
    }

    protected void processSourceIndexStorageLocation( OperationContext context,
                                                      ModelNode modelNode,
                                                      String repositoryName,
                                                      EditableDocument indexStorage ) throws OperationFailedException {
        ModelNode sourcePathNode = ModelAttributes.SOURCE_PATH.resolveModelAttribute(context, modelNode);
        String sourcePath = sourcePathNode.isDefined() ? sourcePathNode.asString() : "modeshape/" + repositoryName + "/indexes_master";

        String sourceRelativeTo = ModelAttributes.SOURCE_RELATIVE_TO.resolveModelAttribute(context, modelNode).asString();
        if (sourceRelativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            //the relative-to path should be the default jboss-data-dir. Setting to an empty string will trigger path injection
            setIndexSourcePathInDataDirectory(".");
            //this only contains the path because the IndexStorageService will create the "end-path"
            indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_SOURCE_LOCATION, sourcePath);
        } else {
            if (!sourceRelativeTo.endsWith("/")) {
                sourceRelativeTo = sourceRelativeTo + "/";
            }
            indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_SOURCE_LOCATION, sourceRelativeTo + sourcePath);
        }
    }

}
