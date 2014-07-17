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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.AbstractRemoveStepHandler} which is triggered each time an <webapp/> element is removed from the ModeShape subsystem.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
class RemoveWebApp extends AbstractRemoveStepHandler {

    static final RemoveWebApp INSTANCE = new RemoveWebApp();

    @Override
    protected void performRemove( OperationContext context,
                                  ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        if (!model.isDefined()) {
            //the model hasn't been defined, which means the Add Step did not succeed
            return;
        }
        AddressContext addressContext = AddressContext.forOperation(operation);
        String webappName = addressContext.lastPathElementValue();

        PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, webappName));
        ModelNode op = Util.createOperation(ModelDescriptionConstants.DEPLOYMENT, deploymentAddress);

        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, ModelDescriptionConstants.REMOVE);
        context.addStep(op, handler, OperationContext.Stage.MODEL);

        super.performRemove(context, operation, model);
    }
}
