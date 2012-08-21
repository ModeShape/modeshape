package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 */
public class ModeShapeCustomBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCustomBinaryStorageResource INSTANCE = new ModeShapeCustomBinaryStorageResource();

    private ModeShapeCustomBinaryStorageResource() {
        super(ModeShapeExtension.CUSTOM_BINARY_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.CUSTOM_BINARY_STORAGE), AddCacheBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.CUSTOM_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
   
}
