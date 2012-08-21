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
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

class RemoveBinaryStorage extends AbstractRemoveStepHandler {

    protected static final Logger log = Logger.getLogger(RemoveBinaryStorage.class.getPackage().getName());

    public static final RemoveBinaryStorage INSTANCE = new RemoveBinaryStorage();

    private RemoveBinaryStorage() {
    }

    @Override
    protected void performRuntime( OperationContext context,
                                   ModelNode operation,
                                   ModelNode model ) {
        // Get the service addresses ...
        final PathAddress serviceAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        // Get the repository name ...
        final String repositoryName = serviceAddress.getElement(1).getValue();
        final ServiceName serviceName = ModeShapeServiceNames.binaryStorageServiceName(repositoryName);

        // Simply remove all services started by any/all of the Add*BinaryStorage operations ...
        context.removeService(serviceName);

        // Now see if we need to remove the path service ...
        if (model.has(ModelKeys.RELATIVE_TO)
            && model.get(ModelKeys.RELATIVE_TO).asString().contains(AddFileBinaryStorage.DATA_DIR_VARIABLE)) {
            // The binaries were stored in the data directory, so we need to remove the path service ...
            ServiceName dirServiceName = ModeShapeServiceNames.binaryStorageDirectoryServiceName(repositoryName);
            context.removeService(dirServiceName);
        }

        String service = serviceAddress.getLastElement().getValue();
        log.debugf("binary storage '%s' removed for repository '%s'", service, repositoryName);
    }

    @Override
    protected void recoverServices( OperationContext context,
                                    ModelNode operation,
                                    ModelNode model ) {
        // TODO: RE-ADD SERVICES
    }
}
