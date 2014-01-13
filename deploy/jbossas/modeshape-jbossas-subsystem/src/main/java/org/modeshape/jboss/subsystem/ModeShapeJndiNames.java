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

import org.jboss.dmr.ModelNode;

/**
 * 
 */
public class ModeShapeJndiNames {

    public static final String JNDI_BASE_NAME = "jcr/";

    public static String jndiNameFrom( final ModelNode model,
                                       String repositoryName ) {
        String jndiName = null;
        if (model.has(ModelKeys.JNDI_NAME) && model.get(ModelKeys.JNDI_NAME).isDefined()) {
            // A JNDI name is set on the model node ...
            jndiName = model.get(ModelKeys.JNDI_NAME).asString();
        }
        if (jndiName == null || jndiName.trim().length() == 0) {
            // Otherwise it's not set in the model node, so we use the default ...
            jndiName = JNDI_BASE_NAME + repositoryName;
        }
        return jndiName;
    }

}
