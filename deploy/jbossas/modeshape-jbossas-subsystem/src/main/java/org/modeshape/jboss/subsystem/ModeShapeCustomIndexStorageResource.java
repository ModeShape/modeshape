package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeCustomIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCustomIndexStorageResource INSTANCE = new ModeShapeCustomIndexStorageResource();

    private ModeShapeCustomIndexStorageResource() {
        super(ModeShapeExtension.CUSTOM_INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.CUSTOM_INDEX_STORAGE), AddCustomIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.CUSTOM_INDEX_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
