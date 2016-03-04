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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import org.modeshape.schematic.document.EditableDocument;

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
        
        ModelNode trashNode = ModelAttributes.TRASH.resolveModelAttribute(context, model);
        String trash = trashNode.isDefined() ? trashNode.asString() : null; 
        
        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            //the relative-to path should be the default jboss-data-dir.
            binaryStoragePathInDataDirectory = ".";
            binaries.set(FieldName.DIRECTORY, path);
            if (trash != null) {
                binaries.set(FieldName.TRASH_DIRECTORY, trash);
            }
        } else {
            if (!relativeTo.endsWith("/")) {
                relativeTo = relativeTo + "/";
            }
            binaries.set(FieldName.DIRECTORY, relativeTo + path);
            if (trash != null) {
                binaries.set(FieldName.TRASH_DIRECTORY, relativeTo + trash);
            }
        }
    }

    @Override
    protected void addControllersAndDependencies(String repositoryName,
                                                 BinaryStorageService service,
                                                 ServiceBuilder<BinaryStorage> builder,
                                                 ServiceTarget target,
                                                 String binariesStoreName) {
        if (binaryStoragePathInDataDirectory != null) {
            // Add a controller that creates the required directory ...
            ServiceName storageDirectoryServiceName = binariesStoreName != null ?
                                                      ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName,
                                                                                                              binariesStoreName)
                                                                                  :
                                                      ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName);
            RelativePathService.addService(storageDirectoryServiceName, binaryStoragePathInDataDirectory,
                                           ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE,
                                           target);
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
