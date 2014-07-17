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

import java.util.Arrays;
import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

class RemoveAuthenticator extends AbstractModeShapeRemoveStepHandler {

    static final RemoveAuthenticator INSTANCE = new RemoveAuthenticator();

    private RemoveAuthenticator() {
    }

    @Override
    List<ServiceName> servicesToRemove( OperationContext context,
                                        ModelNode operation,
                                        ModelNode model ) {
        AddressContext addressContext = AddressContext.forOperation(operation);

        // Get the repository name ...
        final String authenticatorName = addressContext.lastPathElementValue();
        final String repositoryName = addressContext.repositoryName();

        return Arrays.asList(ModeShapeServiceNames.authenticatorServiceName(repositoryName, authenticatorName));
    }
}
