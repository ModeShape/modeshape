package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeSlaveFileIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeSlaveFileIndexStorageResource INSTANCE = new ModeShapeSlaveFileIndexStorageResource();

    private ModeShapeSlaveFileIndexStorageResource() {
        super(ModeShapeExtension.SLAVE_FILE_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.SLAVE_FILE_INDEX_STORAGE), AddSlaveFileSystemIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.SLAVE_FILE_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
