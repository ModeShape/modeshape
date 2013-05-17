package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

public class RemoveParentNamedBinaryStorage extends AbstractAddStepHandler {
    public static RemoveParentNamedBinaryStorage INSTANCE = new RemoveParentNamedBinaryStorage();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }
}
