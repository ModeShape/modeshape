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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * A ParameterValidator that validates a value as a correctly-formatted projection path expression.
 */
public class ProjectionValidator extends ModelTypeValidator {

    /**
     * Creates a ParameterValidator that allows values that are correctly-formatted projection path expressions
     *
     * @param nullable whether {@link org.jboss.dmr.ModelType#UNDEFINED} is allowed
     */
    public ProjectionValidator( boolean nullable ) {
        super(ModelType.STRING, nullable, true, true);
    }

    @Override
    public void validateParameter( String parameterName,
                                   ModelNode value ) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        String valueString = value.asString();
        if (!RepositoryConfiguration.PROJECTION_PATH_EXPRESSION_PATTERN.matcher(valueString).matches()) {
            throw new OperationFailedException(valueString + " is not a valid projection expression");
        }
    }
}
