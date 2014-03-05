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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

class RemoveBinaryStorage extends AbstractModeShapeRemoveStepHandler {

    static final RemoveBinaryStorage INSTANCE = new RemoveBinaryStorage();

    private RemoveBinaryStorage() {
    }

    @Override
    List<ServiceName> servicesToRemove( OperationContext context,
                                        ModelNode operation,
                                        ModelNode model ) throws OperationFailedException {
        String repositoryName = repositoryName(operation);
        List<ServiceName> servicesToRemove = new ArrayList<>();
        String storeName = null;
        if (model.hasDefined(ModelKeys.STORE_NAME)) {
            storeName = model.get(ModelKeys.STORE_NAME).asString();
        }
        ServiceName binaryStorageServiceName = storeName != null ?
                                               ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName, storeName)
                                               : ModeShapeServiceNames.binaryStorageDefaultServiceName(repositoryName);
        servicesToRemove.add(binaryStorageServiceName);

        //see if we need to remove the path service ...
        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            // The binaries were stored in the data directory, so we need to remove the path service ...
            ServiceName dirServiceName = storeName != null ?
                                         ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName, storeName) :
                                         ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName);
            servicesToRemove.add(dirServiceName);
        }

        return servicesToRemove;
    }
}