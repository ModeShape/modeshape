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
package org.modeshape.jcr.value;

import org.modeshape.common.annotation.Immutable;

/**
 * A runtime exception denoting that a value could not be converted to a specific type because of the value's format.
 */
@Immutable
public class ValueFormatException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final Object value;
    private final PropertyType targetType;

    /**
     * @param value the value that was not able to be converted
     * @param targetType the {@link PropertyType} to which the value was being converted
     * @param message the message
     */
    public ValueFormatException( Object value,
                                 PropertyType targetType,
                                 String message ) {
        super(message);
        this.value = value;
        this.targetType = targetType;
    }

    /**
     * @param targetType the {@link PropertyType} to which the value was being converted
     * @param message the message
     * @param cause the cause of the exception
     */
    public ValueFormatException( PropertyType targetType,
                                 String message,
                                 Throwable cause ) {
        super(message, cause);
        this.targetType = targetType;
        this.value = null;
    }

    /**
     * @param value the value that was not able to be converted
     * @param targetType the {@link PropertyType} to which the value was being converted
     * @param message the message
     * @param cause the cause of the exception
     */
    public ValueFormatException( Object value,
                                 PropertyType targetType,
                                 String message,
                                 Throwable cause ) {
        super(message, cause);
        this.value = value;
        this.targetType = targetType;
    }

    /**
     * Get the {@link PropertyType} to which the {@link #getValue() value} was being converted.
     *
     * @return the target type
     */
    public PropertyType getTargetType() {
        return targetType;
    }

    /**
     * Get the original value that was being converted.
     *
     * @return the value, which can be {@code null} in certain cases (e.g. when streams cause this exception)
     */
    public Object getValue() {
        return value;
    }
}
