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
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexStorage;
import org.modeshape.jboss.service.IndexStorageService;

/**
 * 
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
                                                  ServiceTarget target ) {
        ServiceBuilder<IndexStorage> builder = null;
        if (indexStoragePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName serviceName = ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName);
            newControllers.add(RelativePathService.addService(serviceName,
                                                              indexStoragePathInDataDirectory,
                                                              ModeShapeExtension.DATA_DIR_VARIABLE,
                                                              target));
            // and add dependency on this path ...
            builder = target.addService(serviceName, service);
            builder.addDependency(serviceName, String.class, service.getIndexStorageBasePathInjector());
        }
        if (indexSourcePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName serviceName = ModeShapeServiceNames.indexSourceStorageDirectoryServiceName(repositoryName);
            newControllers.add(RelativePathService.addService(serviceName,
                                                              indexSourcePathInDataDirectory,
                                                              ModeShapeExtension.DATA_DIR_VARIABLE,
                                                              target));
            // and add dependency on this path ...
            if (builder == null) {
                builder = target.addService(serviceName, service);
            }
            builder.addDependency(serviceName, String.class, service.getIndexStorageSourceBasePathInjector());
        }
        if (builder != null) {
            newControllers.add(builder.install());
        }
    }
}
