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

import java.util.Iterator;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A factory for creating {@link Property} objects.
 */
@ThreadSafe
public interface PropertyFactory {
    /**
     * Create an empty multi-valued property with the supplied name.
     * 
     * @param name the property name; may not be null
     * @return the resulting property
     */
    Property create( Name name );

    /**
     * Create a single-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param value the value
     * @return the resulting property
     */
    Property create( Name name,
                     Object value );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Object[] values );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterable<?> values );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterator<?> values );

    /**
     * Create a single-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param firstValue the first value; may not be null
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Object firstValue );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values; may not be null but may be empty
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Object[] values );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterable<?> values );

    /**
     * Create a multi-valued property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterator<?> values );

    /**
     * Create a single-valued property with the supplied name and {@link Path} value. This method is provided because Path
     * implements Iterable&lt;Segment>.
     * 
     * @param name the property name; may not be null
     * @param value the path value
     * @return the resulting property
     */
    Property create( Name name,
                     Path value );
}
