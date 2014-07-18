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
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

/**
 * {@link AbstractAddStepHandler} which is triggered each time an <webapp/> element is found in the ModeShape subsystem.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
class AddWebApp extends AbstractAddStepHandler {

    private static final Logger LOGGER = Logger.getLogger(AddWebApp.class.getPackage().getName());

    static final AddWebApp INSTANCE = new AddWebApp();

    @Override
    protected void populateModel( OperationContext context,
                                  ModelNode operation,
                                  org.jboss.as.controller.registry.Resource resource ) throws OperationFailedException {
        if (!requiresRuntime(context)) {
            //we need to skip the execution of this handler if it does not require a "runtime mode". Runtime mode is something
            //that seems to be required only for "normal" servers, as opposed to domain controllers, admin-mode servers and
            //the likes. A standalone or a host in a group of servers will be considered "normal".
            return;
        }

        AddressContext addressContext = AddressContext.forOperation(operation);
        String webappName = addressContext.lastPathElementValue();

        Module module = Module.forClass(AddWebApp.class);
        if (module == null) {
            LOGGER.debugv(
                    "Skipping the deployment of {0} because the module which contains the {1} class cannot be loaded", webappName,
                    AddWebApp.class.getName());
            return;
        }

        URL url = module.getExportedResource(webappName);
        if (url == null) {
            LOGGER.warnv(
                    "Cannot deploy ModeShape webapp {0} because it cannot be located by the main modeshape module", webappName);
            return;
        }
        boolean exploded = attribute(context, resource.getModel(), ModelAttributes.EXPLODED).asBoolean();
        //we'll set an empty object to make sure it's defined
        resource.getModel().setEmptyObject();

        PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT,
                                                                                        webappName));
        ModelNode deploymentOp = Util.createOperation(ModelDescriptionConstants.ADD, deploymentAddress);
        deploymentOp.get(ModelDescriptionConstants.ENABLED).set(true);
        deploymentOp.get(ModelDescriptionConstants.PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml


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
            contentItem.get(ModelDescriptionConstants.URL).set(url.toExternalForm());
        }

        deploymentOp.get(ModelDescriptionConstants.CONTENT).add(contentItem);

        ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
        OperationStepHandler addDeploymentHandler = rootResourceRegistration.getOperationHandler(deploymentAddress,
                                                                                    ModelDescriptionConstants.ADD);
        context.addStep(deploymentOp, addDeploymentHandler, OperationContext.Stage.MODEL);
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
        // We've overridden the code that calls this method, so we don't want to do anything here
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

}
