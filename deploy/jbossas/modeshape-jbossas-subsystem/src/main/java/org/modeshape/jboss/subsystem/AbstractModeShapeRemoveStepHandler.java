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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
            if (serviceController.getState().equals(ServiceController.State.UP)) {
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
            removedServices = new HashMap<ServiceName,Service<?>>();
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
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        for (PathElement pathElement : pathAddress) {
            if (pathElement.getKey().equalsIgnoreCase(ModelKeys.REPOSITORY)) {
                return pathElement.getValue();
            }
        }
        throw new OperationFailedException("Cannot determine repository name for: " + operation.asString());
    }

    abstract List<ServiceName> servicesToRemove( OperationContext context,
                                                 ModelNode operation,
                                                 ModelNode model ) throws OperationFailedException;
}
