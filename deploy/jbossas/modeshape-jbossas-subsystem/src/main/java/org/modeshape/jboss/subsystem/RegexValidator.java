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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A ParameterValidator that validates a value matches a predefined regular expression.
 */
public class RegexValidator extends ModelTypeValidator {

    private final Pattern pattern;

    /**
     * Creates a ParameterValidator that allows values that satisfy a single regular expression string.
     * 
     * @param regularExpression the regular expression
     * @param nullable whether {@link ModelType#UNDEFINED} is allowed
     */
    public RegexValidator( String regularExpression,
                           boolean nullable ) {
        super(ModelType.STRING, nullable, true, true);
        pattern = Pattern.compile(regularExpression);
    }

    @Override
    public void validateParameter( String parameterName,
                                   ModelNode value ) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        String str = value.asString();
        Matcher matcher = pattern.matcher(str);
        if (!matcher.matches()) {
            throw new OperationFailedException("The value '" + str + "' must satisfy the regular expression: " + pattern);
        }
    }

}
