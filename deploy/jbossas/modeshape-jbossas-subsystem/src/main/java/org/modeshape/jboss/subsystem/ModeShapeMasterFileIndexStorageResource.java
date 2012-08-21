package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeMasterFileIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeMasterFileIndexStorageResource INSTANCE = new ModeShapeMasterFileIndexStorageResource();

    private ModeShapeMasterFileIndexStorageResource() {
        super(ModeShapeExtension.MASTER_FILE_INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.MASTER_FILE_INDEX_STORAGE), AddMasterFileSystemIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.MASTER_FILE_INDEX_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
