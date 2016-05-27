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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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
                                   final ModelNode model) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();

        Properties props = new Properties();

        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String sequencerName = addressContext.lastPathElementValue();

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
                String className = node.asString();
                if (className.toLowerCase().contains("mp3")) {
                    LOG.warn("The Mp3 sequencer is deprecated and will be removed in the next major version of ModeShape. Use the Audio sequencer instead.");
                }
                props.put(FieldName.CLASSNAME, className);
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
        sequencerBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private void ensureClassLoadingPropertyIsSet( Properties sequencerProperties ) throws OperationFailedException {
        // could be already set if the "module" element is present in the xml (AddSequencer)
        if (sequencerProperties.containsKey(FieldName.CLASSLOADER)) {
            return;
        }
        String sequencerClassName = sequencerProperties.getProperty(FieldName.CLASSNAME);
        if (StringUtil.isBlank(sequencerClassName)) {
            throw new OperationFailedException(
                    String.format("Required property: %s not found among the sequencer properties: %s",
                                  FieldName.CLASSNAME,
                                  sequencerProperties));
        }
        // try to see if an alias is configured
        String fqSequencerClass = RepositoryConfiguration.getBuiltInSequencerClassName(sequencerClassName);
        if (fqSequencerClass == null) {
            fqSequencerClass = sequencerClassName;
        }
        // set the classloader to the package name of the sequencer class
        String sequencerModuleName = ModuleNamesProvider.moduleNameFor(fqSequencerClass);
        sequencerProperties.setProperty(FieldName.CLASSLOADER, sequencerModuleName);
    }

}
