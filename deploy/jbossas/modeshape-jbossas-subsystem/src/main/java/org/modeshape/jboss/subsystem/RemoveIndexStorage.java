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

class RemoveIndexStorage extends AbstractModeShapeRemoveStepHandler {

    static final RemoveIndexStorage INSTANCE = new RemoveIndexStorage();

    private RemoveIndexStorage() {
    }

    @Override
    List<ServiceName> servicesToRemove( OperationContext context,
                                        ModelNode operation,
                                        ModelNode model ) throws OperationFailedException {
        String repositoryName = repositoryName(operation);
        List<ServiceName> servicesToRemove = new ArrayList<ServiceName>();
        servicesToRemove.add(ModeShapeServiceNames.indexStorageServiceName(repositoryName));

        // Now see if we need to remove the path service ...
        String relativeTo = ModelAttributes.RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            servicesToRemove.add(ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName));
        }

        String sourceRelativeTo = ModelAttributes.SOURCE_RELATIVE_TO.resolveModelAttribute(context, model).asString();
        if (sourceRelativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
            servicesToRemove.add(ModeShapeServiceNames.indexSourceStorageDirectoryServiceName(repositoryName));
        }

        return servicesToRemove;
    }
}
