package org.modeshape.jboss.subsystem;

import java.util.List;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.service.BinaryStorage;
import org.modeshape.jboss.service.CompositeBinaryStorageService;
import org.modeshape.jcr.RepositoryConfiguration;

public class AddCompositeBinaryStorage extends AbstractAddBinaryStorage {

    static final AddCompositeBinaryStorage INSTANCE = new AddCompositeBinaryStorage();

    private AddCompositeBinaryStorage() {
    }

    @Override
    protected void writeBinaryStorageConfiguration( String repositoryName,
                                                    OperationContext context,
                                                    ModelNode model,
                                                    EditableDocument binaries ) /*throws OperationFailedException*/ {
        binaries.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_COMPOSITE);
    }

    @Override
    protected void createBinaryStorageService( OperationContext context,
                                               ModelNode model,
                                               List<ServiceController<?>> newControllers,
                                               ServiceTarget target,
                                               String repositoryName,
                                               EditableDocument binaries,
                                               ServiceName serviceName ) throws OperationFailedException {
        CompositeBinaryStorageService service = new CompositeBinaryStorageService(repositoryName, binaries);
        ServiceBuilder<BinaryStorage> builder = target.addService(serviceName, service);

        List<ModelNode> nestedStores = ModelAttributes.NESTED_STORES.resolveModelAttribute(context, model).asList();

        //parse the nested store names and add a dependency on each of those services
        for (ModelNode nestedStore : nestedStores) {
            String nestedStoreName = nestedStore.asString();
            ServiceName nestedServiceName = ModeShapeServiceNames.binaryStorageNestedServiceName(repositoryName,
                                                                                                 nestedStoreName);
            if (!StringUtil.isBlank(nestedStoreName)) {
                builder.addDependency(nestedServiceName,
                                      BinaryStorage.class,
                                      service.nestedStoreConfiguration(nestedStoreName));
            }
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        newControllers.add(builder.install());
    }


    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        populate(operation, model, ModelAttributes.COMPOSITE_BINARY_STORAGE_ATTRIBUTES);
    }
}
