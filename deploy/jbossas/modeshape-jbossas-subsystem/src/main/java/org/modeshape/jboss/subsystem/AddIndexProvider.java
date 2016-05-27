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
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.service.IndexProviderService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class AddIndexProvider extends AbstractAddStepHandler {

    public static final AddIndexProvider INSTANCE = new AddIndexProvider();

    private AddIndexProvider() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model);
    }

    static void populate( ModelNode operation,
                          ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.INDEX_PROVIDER_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();

        Properties props = new Properties();

        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String providerName = addressContext.lastPathElementValue();

        // Record the properties ...
        props.put(FieldName.NAME, providerName);
        String path = null;
        String relativeTo = null;
        for (String key : operation.keys()) {
            if (key.equals(ADDRESS) || key.equals(OP) || key.equals(OPERATION_HEADERS)) {
                // Ignore these ...
                continue;
            }
            ModelNode node = operation.get(key);
            if (!node.isDefined()) continue;
            if (key.equals(ModelKeys.CLASSNAME) && ModelAttributes.CLASSNAME.isMarshallable(operation)) {
                props.put(FieldName.CLASSNAME, node.asString());
            } else if (key.equals(ModelKeys.MODULE) && ModelAttributes.MODULE.isMarshallable(operation)) {
                props.put(FieldName.CLASSLOADER, node.asString());
            } else if (key.equals(ModelKeys.RELATIVE_TO) && ModelAttributes.RELATIVE_TO.isMarshallable(operation)) {
                // Optional field, but it is a JBoss convention that this might be a variable. Try to resolve this ...
                relativeTo = context.resolveExpressions(node).asString();
                if (relativeTo.equalsIgnoreCase(ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE)) {
                    // the relative-to path should be the default jboss-data-dir.
                    relativeTo = context.resolveExpressions(new ModelNode("${" + relativeTo + "}")).asString();
                }
                props.put(ModelKeys.RELATIVE_TO, relativeTo);
            } else if (key.equals(ModelKeys.PATH) && ModelAttributes.PATH.isMarshallable(operation)) {
                // Optional field, but it is a JBoss convention that this might be a variable. Try to resolve this ...
                path = context.resolveExpressions(node).asString();
                props.put(ModelKeys.PATH, path);
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    // Try resolving it in case it's an expression ...
                    String value = context.resolveExpressions(property.getValue()).asString();
                    props.put(property.getName(), value);
                }
            } else {
                props.put(key, node.asString());
            }
        }
        ensureClassLoadingPropertyIsSet(props);

        IndexProviderService providerService = new IndexProviderService(repositoryName, props);

        ServiceBuilder<JcrRepository> providerBuilder = target.addService(ModeShapeServiceNames.indexProviderServiceName(repositoryName,
                                                                                                                         providerName),
                                                                          providerService);
        providerBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                      ModeShapeEngine.class,
                                      providerService.getModeShapeEngineInjector());
        providerBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                      JcrRepository.class,
                                      providerService.getJcrRepositoryInjector());
        providerBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private void ensureClassLoadingPropertyIsSet( Properties providerProperties ) throws OperationFailedException {
        // could be already set if the "module" element is present in the xml (AddIndexProvider)
        if (providerProperties.containsKey(FieldName.CLASSLOADER)) {
            return;
        }
        String providerClassName = providerProperties.getProperty(FieldName.CLASSNAME);
        if (StringUtil.isBlank(providerClassName)) {
            throw new OperationFailedException(
                    String.format("Required property: %s not found among the index provider properties: %s", FieldName.CLASSNAME,
                                  providerProperties));
        }
        // try to see if an alias is configured
        String fqProviderClass = RepositoryConfiguration.getBuiltInIndexProviderClassName(providerClassName);
        if (fqProviderClass == null) {
            fqProviderClass = providerClassName;
        }
        String providerModuleName = ModuleNamesProvider.moduleNameFor(fqProviderClass);
        providerProperties.setProperty(FieldName.CLASSLOADER, providerModuleName);
    }

}
