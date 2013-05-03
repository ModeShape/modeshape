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
import java.util.List;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public abstract class AbstractAddBinaryStorage extends AbstractAddStepHandler {

    protected AbstractAddBinaryStorage() {
    }

    static void populate( ModelNode operation,
                          ModelNode model,
                          String modelName,
                          AttributeDefinition[] attributes ) throws OperationFailedException {
        for (AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler,
                                   final List<ServiceController<?>> newControllers ) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getElement(1).getValue();
        // final String storageName = pathAddress.getLastElement().getValue(); // always the same: 'indexStorage'

        // Build the 'binaryStorage' document ...
        EditableDocument binaries = Schematic.newDocument();
        writeCommonBinaryStorageConfiguration(context, model, binaries);
        writeBinaryStorageConfiguration(repositoryName, context, model, binaries);

        // Remove the default service, added by "AddRepository"
        ServiceName serviceName = ModeShapeServiceNames.binaryStorageServiceName(repositoryName);
        context.removeService(serviceName);

        // Now create the new service ...
        BinaryStorageService service = new BinaryStorageService(repositoryName, binaries);

        ServiceBuilder<BinaryStorage> builder = target.addService(serviceName, service);

        // Add dependencies to the various data directories ...
        addControllersAndDependencies(repositoryName, service, builder, newControllers, target);
        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(builder.install());
    }

    protected abstract void writeBinaryStorageConfiguration( String repositoryName,
                                                             OperationContext context,
                                                             ModelNode storage,
                                                             EditableDocument binaryStorage ) throws OperationFailedException;

    protected void writeCommonBinaryStorageConfiguration( OperationContext context,
                                                          ModelNode model,
                                                          EditableDocument binaries ) throws OperationFailedException {
        int minBinSize = ModelAttributes.MINIMUM_BINARY_SIZE.resolveModelAttribute(context, model).asInt();
        binaries.set(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, minBinSize);
        ModelNode stringSize = ModelAttributes.MINIMUM_STRING_SIZE.resolveModelAttribute(context, model);
        if (stringSize.isDefined()) {
            binaries.set(FieldName.MINIMUM_STRING_SIZE, stringSize.asInt());
        }
    }

    @SuppressWarnings( "unused" )
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target ) throws OperationFailedException {
    }
}
