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
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration;

import java.util.List;

/**
 * 
 */
public class AddNamedFileBinaryStorage extends AbstractAddNamedBinaryStorage {

    public static final AddNamedFileBinaryStorage INSTANCE = new AddNamedFileBinaryStorage();

    private String binaryStoragePathInDataDirectory;

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        AddFileBinaryStorage.INSTANCE.writeBinaryStorageConfiguration(repositoryName, context, model, binaries);

        String defaultPath = "modeshape/" + repositoryName + "/binaries";
        ModelNode pathNode = ModelAttributes.PATH.resolveModelAttribute(context, model);
        String path = pathNode.isDefined() ? pathNode.asString() : defaultPath;
        if (model.has(ModelKeys.RELATIVE_TO) && model.get(ModelKeys.RELATIVE_TO).asString().contains(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            binaryStoragePathInDataDirectory = path;
        }

    }

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target ) {


        // This is essentially the same as the AddFileBinaryStorage#addControllersAndDependencies, but
        // includes the binary store name in the directory service
        if (binaryStoragePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            final BinaryStorage binaryConfiguration = service.getValue();

            ServiceName serviceName = ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName, binaryConfiguration.getStoreName());
            newControllers.add(RelativePathService.addService(serviceName,
                                                                     binaryStoragePathInDataDirectory,
                                                                     ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE,
                                                                     target));
            // and add dependency on this path ...
            builder.addDependency(serviceName, String.class, service.getBinaryStorageBasePathInjector());
        }
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.FILE_BINARY_STORAGE, ModelAttributes.NAMED_FILE_BINARY_STORAGE_ATTRIBUTES);
    }

}
