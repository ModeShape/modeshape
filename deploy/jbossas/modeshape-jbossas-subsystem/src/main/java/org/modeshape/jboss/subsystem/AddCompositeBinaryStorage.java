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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.CompositeBinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.document.EditableDocument;

public class AddCompositeBinaryStorage extends AbstractAddBinaryStorage {

    static final AddCompositeBinaryStorage INSTANCE = new AddCompositeBinaryStorage();

    private AddCompositeBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) {
        binaries.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_COMPOSITE);
    }

    @Override
    protected void createBinaryStorageService(OperationContext context,
                                              ModelNode model,
                                              ServiceTarget target,
                                              String repositoryName,
                                              EditableDocument binaries,
                                              ServiceName serviceName) throws OperationFailedException {
        CompositeBinaryStorageService service = new CompositeBinaryStorageService(repositoryName, binaries);
        ServiceBuilder<BinaryStorage> builder = target.addService(serviceName, service);

        List<ModelNode> nestedStores = ModelAttributes.NESTED_STORES.resolveModelAttribute(context, model).asList();

        // parse the nested store names and add a dependency on each of those services
        for (ModelNode nestedStore : nestedStores) {
            String nestedStoreName = nestedStore.asString();
            ServiceName nestedServiceName = ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName, nestedStoreName);
            if (!StringUtil.isBlank(nestedStoreName)) {
                builder.addDependency(nestedServiceName, BinaryStorage.class, service.nestedStoreConfiguration(nestedStoreName));
            }
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.COMPOSITE_BINARY_STORAGE_ATTRIBUTES);
    }
}
