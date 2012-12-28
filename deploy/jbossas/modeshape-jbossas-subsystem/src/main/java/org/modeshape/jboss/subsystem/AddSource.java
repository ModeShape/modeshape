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
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.service.SourceService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * {@link AbstractAddStepHandler} implementation for an external source.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class AddSource extends AbstractAddStepHandler {

    static final AddSource INSTANCE = new AddSource();

    private static final Logger LOGGER = Logger.getLogger(AddSource.class.getPackage().getName());

    private AddSource() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model);
    }

    static void populate( ModelNode operation,
                          ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.SOURCE_ATTRIBUTES) {
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


        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getElement(1).getValue();
        final String sourceName = pathAddress.getLastElement().getValue();

        Properties props = new Properties();

        // Record the properties ...
        props.put(RepositoryConfiguration.FieldName.NAME, sourceName);
        for (String key : operation.keys()) {
            if (key.equals(ADDRESS) || key.equals(OP) || key.equals(OPERATION_HEADERS)) {
                // Ignore these ...
                continue;
            }
            ModelNode node = operation.get(key);
            if (!node.isDefined()) {
                continue;
            }
            if (key.equals(ModelKeys.CONNECTOR_CLASSNAME) && ModelAttributes.CONNECTOR_CLASSNAME.isMarshallable(operation)) {
                props.put(RepositoryConfiguration.FieldName.CLASSNAME, node.asString());
            } else if (key.equals(ModelKeys.MODULE) && ModelAttributes.MODULE.isMarshallable(operation)) {
                props.put(RepositoryConfiguration.FieldName.CLASSLOADER, node.asString());
            } else if (key.equals(ModelKeys.PROJECTIONS)) {
                List<String> projections = new ArrayList<String>();
                for (ModelNode projection : operation.get(ModelKeys.PROJECTIONS).asList()) {
                    projections.add(projection.asString());
                }
                props.put(RepositoryConfiguration.FieldName.PROJECTIONS, projections);
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    props.put(property.getName(), property.getValue().asString());
                }
            } else {
                props.put(key, node.asString());
            }
        }
        ensureClassLoadingPropertyIsSet(props);

        SourceService sourceService = new SourceService(repositoryName, props);

        ServiceBuilder<JcrRepository> sourceServiceBuilder = target.addService(ModeShapeServiceNames.sourceServiceName(
                repositoryName,
                sourceName), sourceService);
        sourceServiceBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                       ModeShapeEngine.class,
                                       sourceService.getModeShapeEngineInjector());
        sourceServiceBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                       JcrRepository.class,
                                       sourceService.getJcrRepositoryInjector());
        sourceServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrRepository> controller = sourceServiceBuilder.install();
        newControllers.add(controller);
    }

    private void ensureClassLoadingPropertyIsSet( Properties sourceProperties ) {
        // could be already set if the "module" element is present in the xml
        if (sourceProperties.containsKey(RepositoryConfiguration.FieldName.CLASSLOADER)) {
            return;
        }
        String connectorClassName = sourceProperties.getProperty(RepositoryConfiguration.FieldName.CLASSNAME);
        if (StringUtil.isBlank(connectorClassName)) {
            LOGGER.warnv("Required property: {0} not found among the source properties: {1}",
                      RepositoryConfiguration.FieldName.CLASSNAME,
                      sourceProperties);
            return;
        }

        // set the classloader to the package name of the connector class
        int index = connectorClassName.lastIndexOf(".");
        String connectorModuleName = index != -1 ? connectorClassName.substring(0, index) : connectorClassName;
        sourceProperties.setProperty(RepositoryConfiguration.FieldName.CLASSLOADER, connectorModuleName);
    }
}
