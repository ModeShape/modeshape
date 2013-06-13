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
