package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModeShapeIndexStorageResource extends SimpleResourceDefinition {
    protected final static ModeShapeIndexStorageResource INSTANCE = new ModeShapeIndexStorageResource();

    private ModeShapeIndexStorageResource() {
        super(ModeShapeExtension.INDEX_STORAGE_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY, ModelKeys.INDEX_STORAGE), AddIndexStorage.INSTANCE, RemoveIndexStorage.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration indexStorageSubmodel) {
        super.registerOperations(indexStorageSubmodel);
    }
}
