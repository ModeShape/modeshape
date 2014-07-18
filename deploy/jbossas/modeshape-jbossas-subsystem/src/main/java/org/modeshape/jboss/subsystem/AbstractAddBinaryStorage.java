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

import java.util.List;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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

        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();

        // Build the 'binaryStorage' document ...
        EditableDocument binaries = Schematic.newDocument();

        writeCommonBinaryStorageConfiguration(context, model, binaries);
        writeBinaryStorageConfiguration(repositoryName, context, model, binaries);

        //the store-name attribute is specific only to nested stores, as per schema
        final boolean isNestedStore = model.hasDefined(ModelKeys.STORE_NAME);
        ServiceName serviceName = ModeShapeServiceNames.binaryStorageDefaultServiceName(repositoryName);
        if (isNestedStore) {
            //if it's part of a composite binary container, we don't want to overwrite the default binary service because
            //the composite binary storage container will do that
            String binaryStorageName = binaries.getString(FieldName.BINARY_STORE_NAME);
            assert binaryStorageName != null;
            serviceName = ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName, binaryStorageName);
        } else {
            // Remove the default service, added by "AddRepository"
            context.removeService(serviceName);
        }

        createBinaryStorageService(context, model, newControllers, target, repositoryName, binaries, serviceName);
    }

    protected void createBinaryStorageService( OperationContext context,
                                               ModelNode model,
                                               List<ServiceController<?>> newControllers,
                                               ServiceTarget target,
                                               String repositoryName,
                                               EditableDocument binaries,
                                               ServiceName serviceName ) throws OperationFailedException {
        // Now create the new service ...
        BinaryStorageService service = new BinaryStorageService(repositoryName, binaries);
        ServiceBuilder<BinaryStorage> builder = target.addService(serviceName, service);

        // Add dependencies to the various data directories ...
        String binariesStoreName = binaries.containsField(FieldName.BINARY_STORE_NAME) ? binaries.getString(FieldName.BINARY_STORE_NAME)
                                     : null;
        addControllersAndDependencies(repositoryName, service, builder, newControllers, target, binariesStoreName);
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
        ModelNode minBinarySize = ModelAttributes.MINIMUM_BINARY_SIZE.resolveModelAttribute(context, model);
        if (minBinarySize.isDefined()) {
            binaries.set(FieldName.MINIMUM_BINARY_SIZE_IN_BYTES, minBinarySize.asInt());
        }
        ModelNode stringSize = ModelAttributes.MINIMUM_STRING_SIZE.resolveModelAttribute(context, model);
        if (stringSize.isDefined()) {
            binaries.set(FieldName.MINIMUM_STRING_SIZE, stringSize.asInt());
        }
        ModelNode storeName = ModelAttributes.STORE_NAME.resolveModelAttribute(context, model);
        if (storeName.isDefined()) {
            binaries.set(FieldName.BINARY_STORE_NAME, storeName.asString());
        }
    }

    @SuppressWarnings( "unused" )
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target,
                                                  String binariesStoreName ) throws OperationFailedException {
    }
}
