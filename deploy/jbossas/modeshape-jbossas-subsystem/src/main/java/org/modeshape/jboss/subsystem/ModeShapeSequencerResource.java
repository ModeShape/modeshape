package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModeShapeSequencerResource extends SimpleResourceDefinition {
    protected final static ModeShapeSequencerResource INSTANCE = new ModeShapeSequencerResource();

    private ModeShapeSequencerResource() {
        super(ModeShapeExtension.SEQUENCER_PATH,
                ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY,ModelKeys.SEQUENCER),
                AddSequencer.INSTANCE,
                RemoveSequencer.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        SequencerWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
    }
}
