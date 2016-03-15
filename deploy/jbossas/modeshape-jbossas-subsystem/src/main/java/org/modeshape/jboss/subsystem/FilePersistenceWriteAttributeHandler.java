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

import javax.jcr.RepositoryException;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.modeshape.jboss.service.RepositoryService;

/**
 * An {@link OperationStepHandler} implementation that handles changes to the model values for a repository's database 
 * persistence {@link AttributeDefinition attribute definitions}.
 */
public class FilePersistenceWriteAttributeHandler extends AbstractRepositoryConfigWriteAttributeHandler {

    static final FilePersistenceWriteAttributeHandler INSTANCE = new FilePersistenceWriteAttributeHandler();

    private FilePersistenceWriteAttributeHandler() {
        super(ModelAttributes.PERSISTENCE_FS_ATTRIBUTES);
    }

    @Override
    protected boolean changeField( OperationContext context,
                                   ModelNode operation,
                                   RepositoryService repositoryService,
                                   MappedAttributeDefinition defn,
                                   ModelNode newValue ) throws RepositoryException, OperationFailedException {
        repositoryService.changeField(defn, newValue);
        return true;
    }
}
