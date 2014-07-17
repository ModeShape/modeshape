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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Base {@link AbstractRemoveStepHandler} which should be extended by all ModeShape subsystem services, as removal & recovery is
 * similar for all.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
abstract class AbstractModeShapeRemoveStepHandler extends AbstractRemoveStepHandler {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private Map<ServiceName, Service<?>> removedServices;

    @Override
    protected void performRuntime( OperationContext context,
                                   ModelNode operation,
                                   ModelNode model ) throws OperationFailedException {
        String repositoryName = null;

        for (ServiceName serviceName : servicesToRemove(context, operation, model)) {
            ServiceController<?> serviceController = context.getServiceRegistry(false).getService(serviceName);
            //only remove a service if it's up an running
            if (serviceController != null && serviceController.getState().in(ServiceController.State.UP)) {
                context.removeService(serviceController);
                removedServices().put(serviceName, serviceController.getService());
                if (log.isDebugEnabled()) {
                    if (repositoryName == null) {
                        repositoryName = repositoryName(operation);
                    }
                    log.debugf("service '%s' removed for repository '%s'", serviceName, repositoryName);
                }
            }
        }
    }

    private Map<ServiceName, Service<?>> removedServices() {
        if (removedServices == null) {
            removedServices = new HashMap<>();
        }
        return removedServices;
    }

    @Override
    protected boolean requiresRuntime( OperationContext context ) {
        //always require the performRuntime method to be called
        return true;
    }

    @Override
    protected void recoverServices( OperationContext context,
                                    ModelNode operation,
                                    ModelNode model ) throws OperationFailedException {
        String repositoryName = null;

        Map<ServiceName, Service<?>> removedServices = removedServices();
        for (Map.Entry<ServiceName, Service<?>> serviceEntry : removedServices.entrySet()) {
            context.getServiceTarget().addService(serviceEntry.getKey(), serviceEntry.getValue());
            if (log.isDebugEnabled()) {
                if (repositoryName == null) {
                    repositoryName = repositoryName(operation);
                }
                log.debugf("service '%s' recovered for repository '%s'", serviceEntry.getKey(), repositoryName);
            }
        }
    }

    String repositoryName( ModelNode operation ) throws OperationFailedException {
        AddressContext addressContext = AddressContext.forOperation(operation);
        String repositoryName = addressContext.repositoryName();
        if (repositoryName == null) {
            throw new OperationFailedException("Cannot determine repository name for: " + operation.asString());
        }
        return repositoryName;
    }

    abstract List<ServiceName> servicesToRemove( OperationContext context,
                                                 ModelNode operation,
                                                 ModelNode model ) throws OperationFailedException;
}
