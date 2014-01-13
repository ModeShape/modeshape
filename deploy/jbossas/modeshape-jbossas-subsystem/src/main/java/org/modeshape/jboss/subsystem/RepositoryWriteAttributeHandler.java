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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;

/**
 * An {@link OperationStepHandler} implementation that handles changes to the model values for a repository submodel's
 * {@link AttributeDefinition attribute definitions}. Those attributes that can be changed
 * {@link org.jboss.as.controller.registry.AttributeAccess.Flag#RESTART_NONE RESTART_NONE without restarting} will be immediately
 * reflected in the repository's configuration; other attributes will be changed in the submodel and used upon the next restart.
 */
public class RepositoryWriteAttributeHandler extends AbstractRepositoryConfigWriteAttributeHandler {

    static final RepositoryWriteAttributeHandler INSTANCE = new RepositoryWriteAttributeHandler();

    private RepositoryWriteAttributeHandler() {
        super(ModelAttributes.REPOSITORY_ATTRIBUTES);
    }
}
