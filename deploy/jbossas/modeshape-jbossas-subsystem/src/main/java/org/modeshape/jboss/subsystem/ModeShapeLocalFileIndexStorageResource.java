package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeLocalFileIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeLocalFileIndexStorageResource INSTANCE = new ModeShapeLocalFileIndexStorageResource();

    private ModeShapeLocalFileIndexStorageResource() {
        super(ModeShapeExtension.LOCAL_FILE_INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.LOCAL_FILE_INDEX_STORAGE), AddLocalFileSystemIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.LOCAL_FILE_INDEX_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
