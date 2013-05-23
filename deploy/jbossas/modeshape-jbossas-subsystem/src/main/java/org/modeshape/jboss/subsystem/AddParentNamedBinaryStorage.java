package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

public class AddParentNamedBinaryStorage extends AbstractAddStepHandler {
    public static AddParentNamedBinaryStorage INSTANCE = new AddParentNamedBinaryStorage();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }
}
