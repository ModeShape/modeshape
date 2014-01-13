/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        resource.getModel().setEmptyObject();

        if (requiresRuntime(context)) {
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
            if (url == null) {
                LOGGER.error("Cannot deploy ModeShape webapp: " + webappName + " because it cannot be located by the main modeshape module");
                return;
            }
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
