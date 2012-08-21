package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * 
 */
public class ModeShapeDatabaseBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeDatabaseBinaryStorageResource INSTANCE = new ModeShapeDatabaseBinaryStorageResource();

    private ModeShapeDatabaseBinaryStorageResource() {
        super(ModeShapeExtension.DB_BINARY_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.DB_BINARY_STORAGE), AddDatabaseBinaryStorage.INSTANCE, RemoveBinaryStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.DATABASE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
