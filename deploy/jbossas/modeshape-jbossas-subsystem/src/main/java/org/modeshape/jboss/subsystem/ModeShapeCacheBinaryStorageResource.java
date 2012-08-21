package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeCacheBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCacheBinaryStorageResource INSTANCE = new ModeShapeCacheBinaryStorageResource();

    private ModeShapeCacheBinaryStorageResource() {
        super(ModeShapeExtension.CACHE_BINARY_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.CACHE_BINARY_STORAGE), AddCacheBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.CACHE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
