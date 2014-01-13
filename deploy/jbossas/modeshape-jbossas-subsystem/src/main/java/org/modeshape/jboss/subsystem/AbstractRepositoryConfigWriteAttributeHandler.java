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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.modeshape.jboss.service.RepositoryService;

/**
 * An {@link OperationStepHandler} implementation that automatically handles all write-attribute operations against
 * {@link org.jboss.as.controller.registry.AttributeAccess.Flag#RESTART_NONE RESTART_NONE} {@link AttributeDefinition}s so that
 * the changes are immediately reflected in the repository configuration.
 */
public abstract class AbstractRepositoryConfigWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition[] attributes;

    protected AbstractRepositoryConfigWriteAttributeHandler( AttributeDefinition[] attributes ) {
        super(attributes);
        this.attributes = attributes;
    }

    @Override
    protected boolean applyUpdateToRuntime( OperationContext context,
                                            ModelNode operation,
                                            String attributeName,
                                            ModelNode resolvedValue,
                                            ModelNode currentValue,
                                            HandbackHolder<Void> handbackHolder ) throws OperationFailedException {
        try {
            boolean changedImmediately = changeConfiguration(context, operation, attributeName, resolvedValue, currentValue);
            return !changedImmediately;
        } catch (RepositoryException t) {
            final String repositoryName = repositoryName(operation);
            throw new OperationFailedException("Unable to set attribute '" + attributeName + "' on the '" + repositoryName
                                               + "' repository from " + currentValue + " to " + resolvedValue + ": "
                                               + t.getMessage(), t);
        }
    }

    @Override
    protected void revertUpdateToRuntime( OperationContext context,
                                          ModelNode operation,
                                          String attributeName,
                                          ModelNode valueToRestore,
                                          ModelNode valueToRevert,
                                          Void handback ) throws OperationFailedException {
        try {
            changeConfiguration(context, operation, attributeName, valueToRestore, valueToRevert);
        } catch (RepositoryException t) {
            final String repositoryName = repositoryName(operation);
            throw new OperationFailedException("Unable to restore attribute '" + attributeName + "' on the '" + repositoryName
                                               + "' repository from " + valueToRevert + " to " + valueToRestore + ": "
                                               + t.getMessage(), t);
        }
    }

    protected final String repositoryName( ModelNode operation ) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathElement element = address.getElement(1);
        return element.getValue();
    }

    protected boolean changeConfiguration( OperationContext context,
                                           ModelNode operation,
                                           String attributeName,
                                           ModelNode newValue,
                                           ModelNode existingValue ) throws RepositoryException, OperationFailedException {
        AttributeDefinition attribute = getAttributeDefinition(attributeName);
        if (attribute instanceof MappedAttributeDefinition) {
            MappedAttributeDefinition mappedDefn = (MappedAttributeDefinition)attribute;

            // Find the repository service ...
            final String repositoryName = repositoryName(operation);
            ServiceName repositoryServiceName = ModeShapeServiceNames.repositoryServiceName(repositoryName);
            ServiceController<?> sc = context.getServiceRegistry(true).getRequiredService(repositoryServiceName);
            RepositoryService repositoryService = RepositoryService.class.cast(sc.getService());
            // And set the value (with immediate effect) ...
            return changeField(context, operation, repositoryService, mappedDefn, newValue);
        }
        // The field cannot be updated immediately and requires restart ...
        return false;
    }

    public void registerAttributes( final ManagementResourceRegistration registry ) {
        for (AttributeDefinition attr : attributes) {
            registry.registerReadWriteAttribute(attr, null, this);
        }
    }

    protected boolean changeField( OperationContext context,
                                   ModelNode operation,
                                   RepositoryService repositoryService,
                                   MappedAttributeDefinition defn,
                                   ModelNode newValue ) throws RepositoryException, OperationFailedException {
        repositoryService.changeField(defn, newValue);
        return true;
    }

    /**
     * Take the supplied arrays of AttributeDefinition arrays and merge them into a single array of uniquely-named
     * AttributeDefinition instances.
     * 
     * @param modelAttributeArrays the arrays of definition arrays
     * @return the uniquely-named definitions
     */
    protected static AttributeDefinition[] unique( AttributeDefinition[]... modelAttributeArrays ) {
        Set<String> names = new HashSet<String>();
        List<AttributeDefinition> defns = new ArrayList<AttributeDefinition>();
        for (AttributeDefinition[] array : modelAttributeArrays) {
            for (AttributeDefinition defn : array) {
                if (names.add(defn.getName())) {
                    defns.add(defn);
                }
            }
        }
        return defns.toArray(new AttributeDefinition[defns.size()]);
    }

    protected static AttributeDefinition[] allBut( AttributeDefinition[] definitions,
                                                   String... excluded ) {
        Set<String> excludedNames = new HashSet<String>();
        for (String excludedName : excluded) {
            excludedNames.add(excludedName);
        }
        boolean foundExcluded = false;
        List<AttributeDefinition> defns = new ArrayList<AttributeDefinition>();
        for (AttributeDefinition defn : definitions) {
            if (excludedNames.contains(defn.getName())) {
                foundExcluded = true;
                continue;
            }
            defns.add(defn);
        }
        return foundExcluded ? defns.toArray(new AttributeDefinition[defns.size()]) : definitions;
    }
}
