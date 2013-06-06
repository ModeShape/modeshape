package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * {@link SimpleResourceDefinition} which handles <composite-binary-storage/> elements.
 */
public class ModeShapeCompositeBinaryStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeCompositeBinaryStorageResource INSTANCE = new ModeShapeCompositeBinaryStorageResource();

    private ModeShapeCompositeBinaryStorageResource() {
        super(PathElement.pathElement(ModelKeys.STORAGE_TYPE,ModelKeys.COMPOSITE_BINARY_STORAGE),
              ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.COMPOSITE_BINARY_STORAGE),
              AddCompositeBinaryStorage.INSTANCE, RemoveCompositeBinaryStorage.INSTANCE);
    }

    @Override
    public void registerAttributes( ManagementResourceRegistration resourceRegistration ) {
        super.registerAttributes(resourceRegistration);
        BinaryStorageWriteAttributeHandler.COMPOSITE_BINARY_STORAGE_INSTANCE.registerAttributes(resourceRegistration);
    }
}
