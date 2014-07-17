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
import java.util.List;
import java.util.Properties;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(AddIndexProvider.class.getPackage().getName());

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
                                   final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler,
                                   final List<ServiceController<?>> newControllers ) {

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
            if (key.equals(ModelKeys.CLASSNAME) && ModelAttributes.CLASSNAME.isMarshallable(operation)) {
                props.put(FieldName.CLASSNAME, node.asString());
            } else if (key.equals(ModelKeys.MODULE) && ModelAttributes.MODULE.isMarshallable(operation)) {
                props.put(FieldName.CLASSLOADER, node.asString());
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    props.put(property.getName(), property.getValue().asString());
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
        providerBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrRepository> controller = providerBuilder.install();
        newControllers.add(controller);
    }

    private void ensureClassLoadingPropertyIsSet( Properties providerProperties ) {
        // could be already set if the "module" element is present in the xml (AddIndexProvider)
        if (providerProperties.containsKey(FieldName.CLASSLOADER)) {
            return;
        }
        String providerClassName = providerProperties.getProperty(FieldName.CLASSNAME);
        if (StringUtil.isBlank(providerClassName)) {
            LOG.warnv("Required property: {0} not found among the index provider properties: {1}",
                      FieldName.CLASSNAME,
                      providerProperties);
            return;
        }
        // try to see if an alias is configured
        String fqProviderClass = RepositoryConfiguration.getBuiltInIndexProviderClassName(providerClassName);
        if (fqProviderClass == null) {
            fqProviderClass = providerClassName;
        }
        // set the classloader to the package name of the sequencer class
        int index = fqProviderClass.lastIndexOf(".");
        String providerModuleName = index != -1 ? fqProviderClass.substring(0, index) : fqProviderClass;
        providerProperties.setProperty(FieldName.CLASSLOADER, providerModuleName);
    }

}
