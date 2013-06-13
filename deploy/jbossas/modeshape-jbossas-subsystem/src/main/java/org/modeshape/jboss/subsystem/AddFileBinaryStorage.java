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
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

/**
 * 
 */
public class AddFileBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddFileBinaryStorage INSTANCE = new AddFileBinaryStorage();

    private String binaryStoragePathInDataDirectory;

    private AddFileBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {
        binaries.set(FieldName.TYPE, FieldValue.BINARY_STORAGE_TYPE_FILE);

        ModelNode pathNode = ModelAttributes.PATH.resolveModelAttribute(context, model);
        String path = pathNode.isDefined() ? pathNode.asString() : "modeshape/" + repositoryName + "/binaries";

        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            //the relative-to path should be the default jboss-data-dir.
            binaryStoragePathInDataDirectory = ".";
            binaries.set(FieldName.DIRECTORY, path);
        } else {
            if (!relativeTo.endsWith("/")) {
                relativeTo = relativeTo + "/";
            }
            binaries.set(FieldName.DIRECTORY, relativeTo + path);
        }
    }

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target,
                                                  String binariesStoreName ) {
        if (binaryStoragePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName storageDirectoryServiceName = binariesStoreName != null ?
                                                      ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName,
                                                                                                              binariesStoreName)
                                                                                  :
                                                      ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName);
            newControllers.add(RelativePathService.addService(storageDirectoryServiceName,
                                                              binaryStoragePathInDataDirectory,
                                                              ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE,
                                                              target));
            // and add dependency on this path ...
            builder.addDependency(storageDirectoryServiceName, String.class, service.getBinaryStorageBasePathInjector());
        }
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.FILE_BINARY_STORAGE_ATTRIBUTES);
    }

}
