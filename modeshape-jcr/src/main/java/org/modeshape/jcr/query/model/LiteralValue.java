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
package org.modeshape.jcr.query.model;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Implementation of the literal value static operand for the JCR Query Object Model, used only in bound variables.
 * 
 * @see javax.jcr.query.Query#bindValue(String, Value)
 */
public class LiteralValue extends Literal implements javax.jcr.query.qom.Literal {

    private static final long serialVersionUID = 1L;

    private final Value jcrValue;

    public static Object rawValue( Value value ) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return value.getBinary();
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DECIMAL:
                return value.getDecimal();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
            default:
                return value.getString();
        }
    }

    /**
     * @param value the JCR value
     * @throws RepositoryException if there is a problem obtaining the raw value from the supplied JCR value.
     */
    public LiteralValue( Value value ) throws RepositoryException {
        super(rawValue(value));
        this.jcrValue = value;
    }

    public LiteralValue( Value value,
                         Object rawValue ) {
        super(rawValue);
        this.jcrValue = value;
    }

    @Override
    public Value getLiteralValue() {
        return jcrValue;
    }
}
