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
package org.infinispan.schematic.internal.document;

import java.util.Map;
import org.infinispan.schematic.document.Document;

/**
 * A mutable {@link Document} used when building a MutableBsonObject.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface MutableDocument extends Document {

    /**
     * Sets a name/value pair in this object.
     * 
     * @param name The name; may not be null
     * @param value The value; may be null
     * @return the previous value
     */
    public Object put( String name,
                       Object value );

    /**
     * Sets on this object all name/value pairs from the supplied object. If the supplied object is null, this method does
     * nothing.
     * 
     * @param object the object containing the name/value pairs to be set on this object
     */
    public void putAll( Document object );

    /**
     * Sets on this object all key/value pairs from the supplied map. If the supplied map is null, this method does nothing.
     * 
     * @param map the map containing the name/value pairs to be set on this object
     */
    public void putAll( Map<? extends String, ? extends Object> map );

    /**
     * Removes from this object the name/value pair with the given name.
     * 
     * @param name The name of the pair to remove
     * @return The value removed from this object, or null this object does not contain a pair with the supplied name
     */
    public Object remove( String name );

    /**
     * Remove all fields from this document.
     */
    public void removeAll();

}
