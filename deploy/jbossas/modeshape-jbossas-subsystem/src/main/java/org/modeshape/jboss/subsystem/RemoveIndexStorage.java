/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

class RemoveIndexStorage extends AbstractRemoveStepHandler {

    protected static final Logger log = Logger.getLogger(RemoveIndexStorage.class.getPackage().getName());

    public static final RemoveIndexStorage INSTANCE = new RemoveIndexStorage();

    private RemoveIndexStorage() {
    }

    @Override
    protected void performRuntime( OperationContext context,
                                   ModelNode operation,
                                   ModelNode model ) {
        // Get the service addresses ...
        final PathAddress serviceAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        // Get the repository name ...
        final String repositoryName = serviceAddress.getElement(1).getValue();
        final ServiceName serviceName = ModeShapeServiceNames.indexStorageServiceName(repositoryName);

        // Simply remove all services started by any/all of the Add*IndexStorage operations ...
        context.removeService(serviceName);

        // Now see if we need to remove the path service ...
        if (model.has(ModelKeys.RELATIVE_TO)
            && model.get(ModelKeys.RELATIVE_TO).asString().contains(AddLocalFileSystemIndexStorage.DATA_DIR_VARIABLE)) {
            // The binaries were stored in the data directory, so we need to remove the path service ...
            ServiceName dirServiceName = ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName);
            context.removeService(dirServiceName);
        }
        if (model.has(ModelKeys.SOURCE_RELATIVE_TO)
            && model.get(ModelKeys.SOURCE_RELATIVE_TO).asString().contains(AddLocalFileSystemIndexStorage.DATA_DIR_VARIABLE)) {
            // The binaries were stored in the data directory, so we need to remove the path service ...
            ServiceName dirServiceName = ModeShapeServiceNames.indexStorageDirectoryServiceName(repositoryName);
            context.removeService(dirServiceName);
        }

        String service = serviceAddress.getLastElement().getValue();
        log.debugf("index storage '%s' removed for repository '%s'", service, repositoryName);
    }

    @Override
    protected void recoverServices( OperationContext context,
                                    ModelNode operation,
                                    ModelNode model ) {
        // TODO: RE-ADD SERVICES
    }
}
