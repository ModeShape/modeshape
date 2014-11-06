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

import java.util.HashSet;
import java.util.Set;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.operations.validation.ModelTypeValidator} which accepts only a set of case-insensitive strings.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class StringSetValidator extends ModelTypeValidator {
    private final Set<String> allowedValues;

    protected StringSetValidator( boolean nullable, boolean strictType, String... allowedValues ) {
        super(ModelType.STRING, nullable, false, strictType);
        this.allowedValues = new HashSet<>();
        for (String allowedValue : allowedValues) {
            this.allowedValues.add(allowedValue.toLowerCase());
        }
    }

    @Override
    public void validateParameter( String parameterName, ModelNode value ) throws OperationFailedException {
        // check nullable constraint
        super.validateParameter(parameterName, value);
        if (!value.isDefined()) {
            assert nullable;
            return;
        }
        String string = value.asString();
        if (!allowedValues.contains(string.toLowerCase())) {
            throw new OperationFailedException("Invalid value '" + string + "' for attribute '" + parameterName + "'. Expected values: " + allowedValues);
        }
    }
}
