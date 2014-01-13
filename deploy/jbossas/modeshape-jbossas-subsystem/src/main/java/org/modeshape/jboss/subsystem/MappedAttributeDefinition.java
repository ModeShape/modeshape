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

import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * An {@link AttributeDefinition} that is mapped directly to a field within a {@link RepositoryConfiguration}.
 */
public interface MappedAttributeDefinition {

    /**
     * Get the path to the field within the {@link RepositoryConfiguration}.
     * 
     * @return the path; never null and never empty
     */
    List<String> getPathToField();

    /**
     * Get the path to the field that contains the mapped field within the {@link RepositoryConfiguration}.
     * 
     * @return the parent path; never null but possibly empty if the mapped field is at the top-level of the configuration
     *         document
     */
    List<String> getPathToContainerOfField();

    /**
     * Get the name of the mapped field in the {@link RepositoryConfiguration}.
     * 
     * @return the field name; never null
     */
    String getFieldName();

    /**
     * Obtain from the supplied model node value the value that can be used in the RepositoryConfiguration field.
     * 
     * @param node the model node value
     * @return the field value
     * @throws OperationFailedException if there was an error obtaining the value from the model node
     */
    Object getTypedValue( ModelNode node ) throws OperationFailedException;
}
