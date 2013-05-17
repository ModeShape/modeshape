package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

public class ModeShapeCompositeBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCompositeBinaryStorageResource INSTANCE = new ModeShapeCompositeBinaryStorageResource();

    private ModeShapeCompositeBinaryStorageResource() {
        super(ModeShapeExtension.COMPOSITE_BINARY_STORAGE_PATH,
                     ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.COMPOSITE_BINARY_STORAGE),
                     AddCompositeBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE);
    }

    @Override
    public void registerAttributes( ManagementResourceRegistration resourceRegistration ) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.COMPOSITE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
