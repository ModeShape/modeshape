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
