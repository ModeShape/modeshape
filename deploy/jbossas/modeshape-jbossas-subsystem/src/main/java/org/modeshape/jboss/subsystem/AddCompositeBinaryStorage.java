package org.modeshape.jboss.subsystem;

import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
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
import org.modeshape.jcr.RepositoryConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

public class AddCompositeBinaryStorage extends AbstractAddBinaryStorage {

    public static final AddCompositeBinaryStorage INSTANCE = new AddCompositeBinaryStorage();

    private Map<String, EditableDocument> namedStores;
    private EditableDocument binaries;

    private AddCompositeBinaryStorage() {
        binaries = null;
        namedStores = new HashMap<String, EditableDocument>();
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) throws OperationFailedException {

        this.binaries = binaries;
        binaries.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_COMPOSITE);

        EditableDocument namedBinaryStoreConfig = Schematic.newDocument();

        for (Map.Entry<String, EditableDocument> entry : namedStores.entrySet()) {
            namedBinaryStoreConfig.setDocument(entry.getKey(), entry.getValue());
        }

        binaries.setDocument(RepositoryConfiguration.FieldName.COMPOSITE_STORE_NAMED_BINARY_STORES, namedBinaryStoreConfig);

    }

    protected void writeNamedBinaryStore(String storeName, EditableDocument namedStoreDocument) throws OperationFailedException {
        namedStores.put(storeName, namedStoreDocument);

        if (binaries != null) {
            final EditableDocument document = binaries.getDocument(RepositoryConfiguration.FieldName.COMPOSITE_STORE_NAMED_BINARY_STORES);
            document.setDocument(storeName, namedStoreDocument);
        }

    }

    protected void updateConfigurationOnBinaryStoreChange(OperationContext context, ModelNode operation, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getElement(1).getValue();

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

    @Override
    protected void addControllersAndDependencies( String repositoryName,
                                                  BinaryStorageService service,
                                                  ServiceBuilder<BinaryStorage> builder,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceTarget target ) throws OperationFailedException {


    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelKeys.COMPOSITE_BINARY_STORAGE, ModelAttributes.COMPOSITE_BINARY_STORAGE_ATTRIBUTES);
    }

    public void removeNamedBinaryStore(ModelNode model) {
        final String storeName = model.get(ModelKeys.STORE_NAME).asString();
        namedStores.remove(storeName);


        if (binaries != null) {
            final EditableDocument document = binaries.getDocument(RepositoryConfiguration.FieldName.COMPOSITE_STORE_NAMED_BINARY_STORES);
            document.remove(storeName);
        }
    }
}
