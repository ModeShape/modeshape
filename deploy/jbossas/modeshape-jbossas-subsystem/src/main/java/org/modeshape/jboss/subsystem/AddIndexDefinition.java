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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import java.util.Properties;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexDefinitionService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class AddIndexDefinition extends AbstractAddStepHandler {

    public static final AddIndexDefinition INSTANCE = new AddIndexDefinition();

    private AddIndexDefinition() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model);
    }

    static void populate( ModelNode operation,
                          ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.INDEX_DEFINITION_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model) {

        ServiceTarget target = context.getServiceTarget();

        Properties props = new Properties();

        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String providerName = addressContext.lastPathElementValue();

        // Record the properties ...
        props.put(FieldName.NAME, providerName);
        for (String key : operation.keys()) {
            if (key.equals(ADDRESS) || key.equals(OP) || key.equals(OPERATION_HEADERS)) {
                // Ignore these ...
                continue;
            }
            ModelNode node = operation.get(key);
            if (!node.isDefined()) continue;
            if (key.equals(ModelKeys.PROVIDER_NAME) && ModelAttributes.PROVIDER_NAME.isMarshallable(operation)) {
                props.put(FieldName.PROVIDER_NAME, node.asString());
            } else if (key.equals(ModelKeys.INDEX_KIND) && ModelAttributes.INDEX_KIND.isMarshallable(operation)) {
                props.put(FieldName.KIND, node.asString());
            } else if (key.equals(ModelKeys.SYNCHRONOUS) && ModelAttributes.SYNCHRONOUS.isMarshallable(operation)) {
                props.put(FieldName.SYNCHRONOUS, node.asBoolean());
            } else if (key.equals(ModelKeys.NODE_TYPE_NAME) && ModelAttributes.NODE_TYPE_NAME.isMarshallable(operation)) {
                props.put(FieldName.NODE_TYPE, node.asString());
            } else if (key.equals(ModelKeys.INDEX_COLUMNS) && ModelAttributes.INDEX_COLUMNS.isMarshallable(operation)) {
                props.put(FieldName.COLUMNS, node.asString());
            } else if (key.equals(ModelKeys.WORKSPACES) && ModelAttributes.WORKSPACES.isMarshallable(operation)) {
                props.put(FieldName.WORKSPACES, node.asString());
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    props.put(property.getName(), property.getValue().asString());
                }
            } else {
                props.put(key, node.asString());
            }
        }

        IndexDefinitionService indexDefnService = new IndexDefinitionService(repositoryName, props);

        ServiceBuilder<JcrRepository> indexDefnBuilder = target.addService(ModeShapeServiceNames.indexDefinitionServiceName(repositoryName,
                                                                                                                            providerName),
                                                                           indexDefnService);
        indexDefnBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                       ModeShapeEngine.class,
                                       indexDefnService.getModeShapeEngineInjector());
        indexDefnBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                       JcrRepository.class,
                                       indexDefnService.getJcrRepositoryInjector());
        indexDefnBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}
