/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;

/**
 * {@link AbstractAddStepHandler} which is triggered each time an <webapp/> element is found in the ModeShape subsystem.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
class AddWebApp extends AbstractAddStepHandler {

    static final AddWebApp INSTANCE = new AddWebApp();

    @Override
    protected void populateModel( OperationContext context,
                                  ModelNode operation,
                                  org.jboss.as.controller.registry.Resource resource ) throws OperationFailedException {
        resource.getModel().setEmptyObject();

        if (requiresRuntime(context)) {
            boolean autoDeploy = attribute(context, resource.getModel(), ModelAttributes.AUTO_DEPLOY).asBoolean();
            if (!autoDeploy) {
                return;
            }

            ModelNode address = operation.require(ModelDescriptionConstants.OP_ADDR);
            PathAddress pathAddress = PathAddress.pathAddress(address);
            String webappName = pathAddress.getLastElement().getValue();
            boolean exploded = attribute(context, resource.getModel(), ModelAttributes.EXPLODED).asBoolean();

            PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT,
                                                                                            webappName));
            ModelNode op = Util.createOperation(ModelDescriptionConstants.ADD, deploymentAddress);
            op.get(ModelDescriptionConstants.ENABLED).set(true);
            op.get(ModelDescriptionConstants.PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

            Module module = Module.forClass(getClass());
            URL url = module.getExportedResource(webappName);
            ModelNode contentItem = new ModelNode();

            if (exploded) {
                String urlString = null;
                try {
                    urlString = new File(url.toURI()).getAbsolutePath();
                } catch (URISyntaxException e) {
                    throw new OperationFailedException(e.getMessage(), e);
                }
                contentItem.get(ModelDescriptionConstants.PATH).set(urlString);
                contentItem.get(ModelDescriptionConstants.ARCHIVE).set(false);
            } else {
                contentItem.set(ModelDescriptionConstants.URL).set(url.toExternalForm());
            }

            op.get(ModelDescriptionConstants.CONTENT).add(contentItem);

            ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
            OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress,
                                                                                        ModelDescriptionConstants.ADD);
            context.addStep(op, handler, OperationContext.Stage.MODEL);
        }
    }

    private ModelNode attribute( OperationContext context,
                                 ModelNode model,
                                 AttributeDefinition defn ) throws OperationFailedException {
        assert defn.getDefaultValue() != null && defn.getDefaultValue().isDefined();
        return defn.resolveModelAttribute(context, model);
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) {
        // We overrode the code that calls this method
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

}
