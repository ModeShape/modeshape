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
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

class RemoveRepository extends AbstractRemoveStepHandler {

    private static final Logger log = Logger.getLogger(RemoveRepository.class.getPackage().getName());

    public static final RemoveRepository INSTANCE = new RemoveRepository();

    private RemoveRepository() {
    }

    @Override
    protected void performRuntime( OperationContext context,
                                   ModelNode operation,
                                   ModelNode model ) {
        // Get the service addresses ...
        final PathAddress serviceAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        // Get the repository name ...
        final String repositoryName = serviceAddress.getLastElement().getValue();

        // Remove the JNDI binder service
        final String jndiName = ModeShapeJndiNames.jndiNameFrom(model, repositoryName);
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        context.removeService(bindInfo.getBinderServiceName());

        // Remove the repository service ...
        final ServiceName serviceName = ModeShapeServiceNames.repositoryServiceName(repositoryName);
        context.removeService(serviceName);

        // Remove the data path service added by the AddRepository ...
        final ServiceName dataDirServiceName = ModeShapeServiceNames.dataDirectoryServiceName(repositoryName);
        context.removeService(dataDirServiceName);

        log.debugf("repository '%s' removed", repositoryName);
    }

    @Override
    protected void recoverServices( OperationContext context,
                                    ModelNode operation,
                                    ModelNode model ) {
        // TODO: RE-ADD SERVICES
    }
}
