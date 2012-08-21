package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 */
public class ModeShapeBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeBinaryStorageResource INSTANCE = new ModeShapeBinaryStorageResource();

    private ModeShapeBinaryStorageResource() {
        super(ModeShapeExtension.BINARY_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.BINARY_STORAGE),AddBinaryStorage.INSTANCE,RemoveBinaryStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }    
    
}
