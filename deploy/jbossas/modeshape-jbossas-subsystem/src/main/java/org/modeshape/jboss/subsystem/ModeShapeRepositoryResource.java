package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModeShapeRepositoryResource extends SimpleResourceDefinition {
    protected final static ModeShapeRepositoryResource INSTANCE = new ModeShapeRepositoryResource();

    private ModeShapeRepositoryResource() {
        super(ModeShapeExtension.REPOSITORY_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY),
                AddRepository.INSTANCE,
                RemoveRepository.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        RepositoryWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
    }
}
