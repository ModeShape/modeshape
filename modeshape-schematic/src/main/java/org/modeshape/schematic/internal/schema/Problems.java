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
package org.modeshape.schematic.internal.schema;

import org.modeshape.schematic.annotation.NotThreadSafe;
import org.modeshape.schematic.document.JsonSchema;
import org.modeshape.schematic.document.Path;

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
                             JsonSchema.Type actualType,
                             Object actualValue,
                             JsonSchema.Type requiredType,
                             Object convertedValue );

}
