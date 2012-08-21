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
        String extractorModuleName = fqExtractorClass.substring(0, fqExtractorClass.lastIndexOf("."));
        properties.setProperty(FieldName.CLASSLOADER, extractorModuleName);
    }

}
