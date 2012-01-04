/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.internal.schema;

import org.infinispan.schematic.document.JsonSchema.Type;
import org.infinispan.schematic.document.NotThreadSafe;
import org.infinispan.schematic.document.Path;

/**
 * An interface for recording problems.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@NotThreadSafe
public interface Problems {

    /**
     * Record a successful validation.
     */
    void recordSuccess();

    /**
     * Record an error at the given path in a document.
     * 
     * @param path the path; may not be null
     * @param message the message describing the error; may not be null
     */
    void recordError( Path path,
                      String message );

    /**
     * Record an error at the given path in a document.
     * 
     * @param path the path; may not be null
     * @param message the message describing the error; may not be null
     * @param exception the exception that occurred and that is considered the cause
     */
    void recordError( Path path,
                      String message,
                      Throwable exception );

    /**
     * Record a warning at the given path in a document.
     * 
     * @param path the path; may not be null
     * @param message the message describing the warning; may not be null
     */
    void recordWarning( Path path,
                        String message );

    /**
     * Record a field value was encountered whose type was not the expected type but could be converted to the expected type.
     * 
     * @param path the path; may not be null
     * @param message the message describing the warning; may not be null
     * @param actualType the actual type of the field value; may not be null
     * @param actualValue the actual field value
     * @param requiredType the expected type; may not be null
     * @param convertedValue the converted value
     */
    void recordTypeMismatch( Path path,
                             String message,
                             Type actualType,
                             Object actualValue,
                             Type requiredType,
                             Object convertedValue );

}
