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
import org.modeshape.jboss.service.SequencerService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class AddSequencer extends AbstractAddStepHandler {

    public static final AddSequencer INSTANCE = new AddSequencer();

    private static final Logger LOG = Logger.getLogger(AddSequencer.class.getPackage().getName());

    private AddSequencer() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model);
    }

    static void populate( ModelNode operation,
                          ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.SEQUENCER_ATTRIBUTES) {
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
        final String sequencerName = pathAddress.getLastElement().getValue();

        // Record the properties ...
        props.put(FieldName.NAME, sequencerName);
        for (String key : operation.keys()) {
            if (key.equals(ADDRESS) || key.equals(OP) || key.equals(OPERATION_HEADERS)) {
                // Ignore these ...
                continue;
            }
            ModelNode node = operation.get(key);
            if (!node.isDefined()) continue;
            if (key.equals(ModelKeys.SEQUENCER_CLASSNAME) && ModelAttributes.SEQUENCER_CLASSNAME.isMarshallable(operation)) {
                props.put(FieldName.CLASSNAME, node.asString());
            } else if (key.equals(ModelKeys.MODULE) && ModelAttributes.MODULE.isMarshallable(operation)) {
                props.put(FieldName.CLASSLOADER, node.asString());
            } else if (key.equals(ModelKeys.PATH_EXPRESSIONS)) {
                List<String> pathExpressions = new ArrayList<String>();
                for (ModelNode pathExpression : operation.get(ModelKeys.PATH_EXPRESSIONS).asList()) {
                    pathExpressions.add(pathExpression.asString());
                }
                props.put(FieldName.PATH_EXPRESSIONS, pathExpressions);
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    props.put(property.getName(), property.getValue().asString());
                }
            } else {
                props.put(key, node.asString());
            }
        }
        ensureClassLoadingPropertyIsSet(props);

        SequencerService sequencerService = new SequencerService(repositoryName, props);

        ServiceBuilder<JcrRepository> sequencerBuilder = target.addService(ModeShapeServiceNames.sequencerServiceName(repositoryName,
                                                                                                                      sequencerName),
                                                                           sequencerService);
        sequencerBuilder.addDependency(ModeShapeServiceNames.ENGINE,
                                       ModeShapeEngine.class,
                                       sequencerService.getModeShapeEngineInjector());
        sequencerBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                       JcrRepository.class,
                                       sequencerService.getJcrRepositoryInjector());
        sequencerBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        ServiceController<JcrRepository> controller = sequencerBuilder.install();
        newControllers.add(controller);
    }

    @Override
    protected boolean requiresRuntime( OperationContext context ) {
        return true;
    }

    private void ensureClassLoadingPropertyIsSet( Properties sequencerProperties ) {
        // could be already set if the "module" element is present in the xml (AddSequencer)
        if (sequencerProperties.containsKey(FieldName.CLASSLOADER)) {
            return;
        }
        String sequencerClassName = sequencerProperties.getProperty(FieldName.CLASSNAME);
        if (StringUtil.isBlank(sequencerClassName)) {
            LOG.warnv("Required property: {0} not found among the sequencer properties: {1}",
                      FieldName.CLASSNAME,
                      sequencerProperties);
            return;
        }
        // try to see if an alias is configured
        String fqSequencerClass = RepositoryConfiguration.getBuiltInSequencerClassName(sequencerClassName);
        if (fqSequencerClass == null) {
            fqSequencerClass = sequencerClassName;
        }
        // set the classloader to the package name of the sequencer class
        int index = fqSequencerClass.lastIndexOf(".");
        String sequencerModuleName = index != -1 ? fqSequencerClass.substring(0, index) : fqSequencerClass;
        sequencerProperties.setProperty(FieldName.CLASSLOADER, sequencerModuleName);
    }

}
