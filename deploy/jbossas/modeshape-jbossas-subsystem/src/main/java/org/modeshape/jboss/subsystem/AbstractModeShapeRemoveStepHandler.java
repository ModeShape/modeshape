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
import java.util.List;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Base {@link AbstractRemoveStepHandler} which should be extended by all ModeShape subsystem services, as removal & recovery is
 * similar for all.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
abstract class AbstractModeShapeRemoveStepHandler extends AbstractRemoveStepHandler {

    protected final Logger log = Logger.getLogger(getClass().getName());

    @Override
    protected void performRuntime( OperationContext context,
                                   ModelNode operation,
                                   ModelNode model ) throws OperationFailedException {
        String repositoryName = null;

        for (ServiceName serviceName : servicesToRemove(context, operation, model)) {
            context.removeService(serviceName);
            if (log.isDebugEnabled()) {
                if (repositoryName == null) {
                    repositoryName = repositoryName(operation);
                }
                log.debugf("service '%s' removed for repository '%s'", serviceName, repositoryName);
            }
        }
    }

    @Override
    protected void recoverServices( OperationContext context,
                                    ModelNode operation,
                                    ModelNode model ) throws OperationFailedException {
        String repositoryName = null;

        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        for (ServiceName serviceName : servicesToRemove(context, operation, model)) {
            context.getServiceTarget().addService(serviceName, serviceRegistry.getService(serviceName).getService());
            if (log.isDebugEnabled()) {
                if (repositoryName == null) {
                    repositoryName = repositoryName(operation);
                }
                log.debugf("service '%s' recovered for repository '%s'", serviceName, repositoryName);
            }
        }
    }

    abstract List<ServiceName> servicesToRemove( OperationContext context,
                                                 ModelNode operation,
                                                 ModelNode model ) throws OperationFailedException;

    String repositoryName( ModelNode operation ) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        for (PathElement pathElement : pathAddress) {
            if (pathElement.getKey().equalsIgnoreCase(ModelKeys.REPOSITORY)) {
                return pathElement.getValue();
            }
        }
        throw new OperationFailedException("Cannot determine repository name for: " + operation.asString());
    }
}
