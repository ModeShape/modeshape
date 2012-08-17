package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeCacheIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCacheIndexStorageResource INSTANCE = new ModeShapeCacheIndexStorageResource();

    private ModeShapeCacheIndexStorageResource() {
        super(ModeShapeExtension.CACHE_INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.CACHE_INDEX_STORAGE), AddCacheIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.CACHE_INDEX_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
