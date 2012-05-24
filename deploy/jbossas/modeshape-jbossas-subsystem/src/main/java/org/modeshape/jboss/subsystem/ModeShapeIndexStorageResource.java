package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
//todo every resource should have its own add/remove operation
public class ModeShapeIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeIndexStorageResource INSTANCE = new ModeShapeIndexStorageResource();

    private ModeShapeIndexStorageResource() {
        super(ModeShapeExtension.INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.INDEX_STORAGE)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
    }

    private void registerOperation(final ManagementResourceRegistration resourceRegistration,
                                   final String name,
                                   final OperationStepHandler op,
                                   final AttributeDefinition... attributes) {

        resourceRegistration.registerOperationHandler(name,
                op,
                new DefaultOperationDescriptionProvider(name, getResourceDescriptionResolver(), attributes),
                false);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration indexStorageSubmodel) {
        super.registerOperations(indexStorageSubmodel);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_RAM_INDEX_STORAGE, AddRamIndexStorage.INSTANCE, ModelAttributes.RAM_INDEX_STORAGE_ATTRIBUTES);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_LOCAL_FILE_INDEX_STORAGE, AddLocalFileSystemIndexStorage.INSTANCE, ModelAttributes.LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_MASTER_FILE_INDEX_STORAGE, AddMasterFileSystemIndexStorage.INSTANCE, ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_SLAVE_FILE_INDEX_STORAGE, AddSlaveFileSystemIndexStorage.INSTANCE, ModelAttributes.SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_CACHE_INDEX_STORAGE, AddCacheIndexStorage.INSTANCE, ModelAttributes.CACHE_INDEX_STORAGE_ATTRIBUTES);
        registerOperation(indexStorageSubmodel, ModelKeys.ADD_CUSTOM_INDEX_STORAGE, AddCustomIndexStorage.INSTANCE, ModelAttributes.CUSTOM_INDEX_STORAGE_ATTRIBUTES);

        registerOperation(indexStorageSubmodel, ModelKeys.REMOVE_INDEX_STORAGE, RemoveIndexStorage.INSTANCE);
        //todo is it ok to have just one remove method?


    }
}
