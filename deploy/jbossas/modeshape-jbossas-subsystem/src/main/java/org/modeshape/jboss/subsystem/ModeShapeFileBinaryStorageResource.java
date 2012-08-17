package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 */
public class ModeShapeFileBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeFileBinaryStorageResource INSTANCE = new ModeShapeFileBinaryStorageResource();

    private ModeShapeFileBinaryStorageResource() {
        super(ModeShapeExtension.FILE_BINARY_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.FILE_BINARY_STORAGE), AddFileBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.FILE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }

}
