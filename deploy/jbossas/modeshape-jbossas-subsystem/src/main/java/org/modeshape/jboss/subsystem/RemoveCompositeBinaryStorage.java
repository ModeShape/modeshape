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
