/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

public class DescribeModeShapeSubsystem implements OperationStepHandler {

    public static final DescribeModeShapeSubsystem INSTANCE = new DescribeModeShapeSubsystem();

    @Override
    public void execute( OperationContext context,
                         ModelNode operation ) throws OperationFailedException {
        ModelNode result = context.getResult();

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR))
                                                                     .getLastElement());
        ModelNode subModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

        AddModeShapeSubsystem.populate(subModel, subsystemAdd);
        result.add(subsystemAdd);

        if (subModel.hasDefined(ModelKeys.REPOSITORY)) {
            for (Property container : subModel.get(ModelKeys.REPOSITORY).asPropertyList()) {
                ModelNode address = rootAddress.toModelNode();
                address.add(ModelKeys.REPOSITORY, container.getName());

                final ModelNode addOperation = new ModelNode();
                addOperation.get(OP).set(ADD);
                addOperation.get(OP_ADDR).set(address);

                AddRepository.populate(container.getValue(), addOperation);

                result.add(addOperation);
            }
        }

        // if (subModel.hasDefined(Element.TRANSLATOR_ELEMENT.getLocalName())) {
        // for (Property container : subModel.get(Element.TRANSLATOR_ELEMENT.getLocalName()).asPropertyList()) {
        // ModelNode address = rootAddress.toModelNode();
        // address.add(Element.TRANSLATOR_ELEMENT.getLocalName(), container.getName());
        //
        // final ModelNode addOperation = new ModelNode();
        // addOperation.get(OP).set(ADD);
        // addOperation.get(OP_ADDR).set(address);
        //
        // TranslatorAdd.populate(container.getValue(), addOperation);
        //
        // result.add(addOperation);
        // }
        // }

        context.completeStep();
    }
}
