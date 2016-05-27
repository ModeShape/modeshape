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
import java.util.Collections;
import java.util.List;
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
import org.modeshape.jboss.service.SourceService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * {@link AbstractAddStepHandler} implementation for an external source.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class AddSource extends AbstractAddStepHandler {

    static final AddSource INSTANCE = new AddSource();

    /**
     * The list of known custom connector properties which come in the form of comma-separated strings and should be transformed
     * into Lists before being set on the connector classes.
     */
    private static final List<String> LIST_PROPERTIES = Collections.singletonList("queryableBranches");

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
                                   final ModelNode model) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();


        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String sourceName = addressContext.lastPathElementValue();

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
                List<String> projections = new ArrayList<>();
                for (ModelNode projection : operation.get(ModelKeys.PROJECTIONS).asList()) {
                    projections.add(projection.asString());
                }
                props.put(RepositoryConfiguration.FieldName.PROJECTIONS, projections);
            } else if (key.equals(ModelKeys.PROPERTIES)) {
                for (Property property : node.asPropertyList()) {
                    props.put(property.getName(), propertyValue(property));
                }
            } else if (key.equalsIgnoreCase(ModelKeys.CACHEABLE)) {
                props.put(key, node.asBoolean());
            } else if (key.equalsIgnoreCase(ModelKeys.QUERYABLE) || key.equalsIgnoreCase(ModelKeys.READONLY)) {
                props.put(key, node.asBoolean());
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
        sourceServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private Object propertyValue(Property property) {
        String propertyName = property.getName();
        String valueAsString = property.getValue().asString();

        if (!LIST_PROPERTIES.contains(propertyName)) {
            return valueAsString;
        }

        String[] values = valueAsString.split(",");
        List<String> result = new ArrayList<>(values.length);
        for (String value : values) {
            result.add(value.trim());
        }
        return result;
    }

    private void ensureClassLoadingPropertyIsSet( Properties sourceProperties ) throws OperationFailedException {
        // could be already set if the "module" element is present in the xml
        if (sourceProperties.containsKey(RepositoryConfiguration.FieldName.CLASSLOADER)) {
            return;
        }
        String connectorClassName = sourceProperties.getProperty(RepositoryConfiguration.FieldName.CLASSNAME);
        if (StringUtil.isBlank(connectorClassName)) {
            throw new OperationFailedException(
                    String.format("Required property: %s not found among the connector properties: %s",
                                  RepositoryConfiguration.FieldName.CLASSNAME,
                                  sourceProperties));
        }
        // try to see if an alias is configured
        String fqConnectorClass = RepositoryConfiguration.getBuiltInConnectorClassName(connectorClassName);
        if (fqConnectorClass == null) {
            fqConnectorClass = connectorClassName;
        }
        String connectorModuleName = ModuleNamesProvider.moduleNameFor(fqConnectorClass);
        sourceProperties.setProperty(RepositoryConfiguration.FieldName.CLASSLOADER, connectorModuleName);
    }
}
