package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeRamIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeRamIndexStorageResource INSTANCE = new ModeShapeRamIndexStorageResource();

    private ModeShapeRamIndexStorageResource() {
        super(ModeShapeExtension.RAM_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.RAM_INDEX_STORAGE), AddRamIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        IndexStorageWriteAttributeHandler.RAM_INDEX_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
