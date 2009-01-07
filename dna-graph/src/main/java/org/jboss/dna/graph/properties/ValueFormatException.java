/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.properties;

/**
 * @author Randall Hauch
 */
public class ValueFormatException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final Object value;
    private final PropertyType targetType;

    /**
     * @param value the value that was not able to be converted
     * @param targetType the {@link PropertyType} to which the value was being converted
     */
    public ValueFormatException( Object value,
                                 PropertyType targetType ) {
        this.value = value;
        this.targetType = targetType;
    }

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
     * @param value the value that was not able to be converted
     * @param targetType the {@link PropertyType} to which the value was being converted
     * @param cause the cause of the exception
     */
    public ValueFormatException( Object value,
                                 PropertyType targetType,
                                 Throwable cause ) {
        super(cause);
        this.value = value;
        this.targetType = targetType;
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
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
     * @return the value
     */
    public Object getValue() {
        return value;
    }
}
