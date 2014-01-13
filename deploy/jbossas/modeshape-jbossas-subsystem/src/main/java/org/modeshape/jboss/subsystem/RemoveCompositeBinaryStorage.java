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

class RemoveCompositeBinaryStorage extends AbstractModeShapeRemoveStepHandler {

    static final RemoveCompositeBinaryStorage INSTANCE = new RemoveCompositeBinaryStorage();

    private RemoveCompositeBinaryStorage() {
    }

    @Override
    List<ServiceName> servicesToRemove( OperationContext context,
                                        ModelNode operation,
                                        ModelNode model ) throws OperationFailedException {
        String repositoryName = repositoryName(operation);
        List<ServiceName> servicesToRemove = new ArrayList<ServiceName>();
        //add the services for each of the nested stores
        List<ModelNode> nestedStores = model.get(ModelKeys.NESTED_STORES).asList();
        for (ModelNode nestedStore : nestedStores) {
            String nestedStoreName = nestedStore.asString();
            ServiceName nestedServiceName = ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName,
                                                                                                 nestedStoreName);
            servicesToRemove.add(nestedServiceName);
        }

        //iterate through each of nested store nodes to check is the binary path storage service need to be removed as well
        if (model.has(ModelKeys.NESTED_STORAGE_TYPE_FILE)) {
            List<ModelNode> storageNodes = model.get(ModelKeys.NESTED_STORAGE_TYPE_FILE).asList();
            for (ModelNode storageNode : storageNodes) {
                String storeName = (String)storageNode.keys().toArray()[0];
                ModelNode storageContent = storageNode.get(storeName);
                String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, storageContent).asString();
                if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
                    ServiceName dirServiceName = ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName, storeName);
                    servicesToRemove.add(dirServiceName);
                }
            }
        }
        //add the main repository service
        servicesToRemove.add(ModeShapeServiceNames.binaryStorageDefaultServiceName(repositoryName));
        return servicesToRemove;
    }
}
