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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import java.util.List;
import java.util.Properties;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.service.TextExtractorService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class AddTextExtractor extends AbstractAddStepHandler {

    public static final AddTextExtractor INSTANCE = new AddTextExtractor();

    private static final Logger LOG = Logger.getLogger(AddTextExtractor.class.getPackage().getName());

    private AddTextExtractor() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model);
    }

    static void populate( ModelNode operation,
                          ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.TEXT_EXTRACTOR_ATTRIBUTES) {
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

        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getElement(1).getValue();
        final String extractorName = pathAddress.getLastElement().getValue();

        // Record the properties ...
        props.put(FieldName.NAME, extractorName);
        for (String key : operation.keys()) {
            if (key.equals(ADDRESS) || key.equals(OP) || key.equals(OPERATION_HEADERS)) {
                // Ignore these ...
                continue;
            }
            ModelNode node = operation.get(key);
            if (!node.isDefined()) continue;
            if (key.equals(ModelKeys.TEXT_EXTRACTOR_CLASSNAME)
                && ModelAttributes.TEXT_EXTRACTOR_CLASSNAME.isMarshallable(operation)) {
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

        TextExtractorService extractorService = new TextExtractorService(repositoryName, props);

        ServiceName serviceName = ModeShapeServiceNames.textExtractorServiceName(repositoryName, extractorName);
        ServiceBuilder<JcrRepository> extractorBuilder = target.addService(serviceName, extractorService);
        extractorBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                       ModeShapeEngine.class,
                                       extractorService.getModeShapeEngineInjector());
        extractorBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                       JcrRepository.class,
                                       extractorService.getJcrRepositoryInjector());
        extractorBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrRepository> controller = extractorBuilder.install();
        newControllers.add(controller);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return true;
    }

    private void ensureClassLoadingPropertyIsSet( Properties properties ) {
        // could be already set if the "module" element is present in the xml
        if (properties.containsKey(FieldName.CLASSLOADER)) {
            return;
        }
        String textExtractorClassName = properties.getProperty(FieldName.CLASSNAME);
        if (StringUtil.isBlank(textExtractorClassName)) {
            LOG.warnv("Required property: {0} not found among the text extractor properties: {1}",
                      FieldName.CLASSNAME,
                      properties);
            return;
        }
        // try to see if an alias is configured
        String fqExtractorClass = RepositoryConfiguration.getBuiltInTextExtractorClassName(textExtractorClassName);
        if (fqExtractorClass == null) {
            fqExtractorClass = textExtractorClassName;
        }
        // set the classloader to the package name of the sequencer class
        int index = fqExtractorClass.lastIndexOf(".");
        String extractorModuleName = index != -1 ? fqExtractorClass.substring(0, index) : fqExtractorClass;
        properties.setProperty(FieldName.CLASSLOADER, extractorModuleName);
    }

}
