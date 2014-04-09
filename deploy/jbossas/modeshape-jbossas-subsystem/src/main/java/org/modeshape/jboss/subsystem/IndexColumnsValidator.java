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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * A ParameterValidator that validates a value as a correctly-formatted index columns definition.
 */
public class IndexColumnsValidator extends ModelTypeValidator {

    /**
     * Creates a ParameterValidator that allows values that are correctly-formatted index column definitions
     * 
     * @param nullable whether {@link org.jboss.dmr.ModelType#UNDEFINED} is allowed
     */
    public IndexColumnsValidator( boolean nullable ) {
        super(ModelType.STRING, nullable, true, true);
    }

    @Override
    public void validateParameter( String parameterName,
                                   ModelNode value ) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        String valueString = value.asString();
        if (!RepositoryConfiguration.INDEX_COLUMN_DEFINITIONS_PATTERN.matcher(valueString).matches()) {
            throw new OperationFailedException(valueString + " is not a valid definition of index columns");
        }
    }
}
