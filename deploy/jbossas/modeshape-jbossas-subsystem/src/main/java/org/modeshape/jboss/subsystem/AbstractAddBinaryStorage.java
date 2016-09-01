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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.BinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.EditableDocument;

public abstract class AbstractAddBinaryStorage extends AbstractAddStepHandler {
    
    protected Logger logger;

    protected AbstractAddBinaryStorage() {
        logger = Logger.getLogger(getClass());
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
                                   final ModelNode model) throws OperationFailedException {

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

        createBinaryStorageService(context, model, target, repositoryName, binaries, serviceName);
    }

    protected void createBinaryStorageService(OperationContext context,
                                              ModelNode model,
                                              ServiceTarget target,
                                              String repositoryName,
                                              EditableDocument binaries,
                                              ServiceName serviceName) throws OperationFailedException {
        // Now create the new service ...
        BinaryStorageService service = BinaryStorageService.createWithConfiguration(binaries);
        ServiceBuilder<BinaryStorage> builder = target.addService(serviceName, service);

        // Add dependencies to the various data directories ...
        String binariesStoreName = binaries.containsField(FieldName.BINARY_STORE_NAME) ? binaries.getString(FieldName.BINARY_STORE_NAME)
                                     : null;
        addControllersAndDependencies(repositoryName, service, builder, target, binariesStoreName);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
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
        ModelNode mimeTypeDetection = ModelAttributes.MIME_TYPE_DETECTION.resolveModelAttribute(context, model);
        if (mimeTypeDetection.isDefined()) {
            binaries.set(FieldName.MIMETYPE_DETECTION, mimeTypeDetection.asString());
        }
    }

    @SuppressWarnings( "unused" )
    protected void addControllersAndDependencies(String repositoryName,
                                                 BinaryStorageService service,
                                                 ServiceBuilder<BinaryStorage> builder,
                                                 ServiceTarget target,
                                                 String binariesStoreName) throws OperationFailedException {
    }
}
