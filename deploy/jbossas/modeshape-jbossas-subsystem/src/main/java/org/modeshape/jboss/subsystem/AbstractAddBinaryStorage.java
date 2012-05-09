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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
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
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;

public abstract class AbstractAddBinaryStorage extends AbstractAddStepHandler {

    protected AbstractAddBinaryStorage() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        String opName = operation.get(OP).asString();
        if (ModelKeys.ADD_FILE_BINARY_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.FILE_BINARY_STORAGE, ModelAttributes.FILE_BINARY_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_CACHE_BINARY_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.CACHE_BINARY_STORAGE, ModelAttributes.CACHE_BINARY_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_DB_BINARY_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.DB_BINARY_STORAGE, ModelAttributes.DATABASE_BINARY_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_CUSTOM_BINARY_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.CUSTOM_BINARY_STORAGE, ModelAttributes.CUSTOM_BINARY_STORAGE_ATTRIBUTES);
        }
    }

    static void populate( ModelNode operation,
                          ModelNode model,
                          String modelName,
                          AttributeDefinition[] attributes ) throws OperationFailedException {
        for (AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
        // Set the binary storage type last (overwriting any value that they've manually added) ...
        model.get(ModelKeys.BINARY_STORAGE_TYPE).set(modelName);
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
        writeBinaryStorageConfiguration(repositoryName, context, model, binaries);

        // Now create the service ...
        BinaryStorageService service = new BinaryStorageService(repositoryName, binaries);

        ServiceBuilder<BinaryStorage> builder = target.addService(ModeShapeServiceNames.binaryStorageServiceName(repositoryName),
                                                                  service);
        // Add dependencies to the various data directories ...
        addControllersAndDependencies(repositoryName, service, builder, newControllers, target);
        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(builder.install());

    }

    protected abstract void writeBinaryStorageConfiguration( String repositoryName,
                                                             OperationContext context,
                                                             ModelNode storage,
                                                             EditableDocument binaryStorage ) throws OperationFailedException;

    @SuppressWarnings( "unused" )
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target ) throws OperationFailedException {
    }
}
