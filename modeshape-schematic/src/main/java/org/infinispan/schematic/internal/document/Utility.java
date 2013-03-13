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
package org.infinispan.schematic.internal.document;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Document;

class Utility {

    public static Array unwrap( Array array ) {
        if (array instanceof ArrayEditor) {
            return unwrap(((ArrayEditor)array).unwrap());
        }
        return array;
    }

    public static Document unwrap( Document document ) {
        if (document instanceof DocumentEditor) {
            return unwrap(((DocumentEditor)document).unwrap());
        }
        return document;
    }

    public static Object unwrap( Object value ) {
        if (value instanceof DocumentEditor) {
            return unwrap(((DocumentEditor)value).unwrap());
        }
        if (value instanceof ArrayEditor) {
            return unwrap(((ArrayEditor)value).unwrap());
        }
        return value;
    }

    @SuppressWarnings( "unchecked" )
    public static Map<? extends String, ? extends Object> unwrapValues( Map<? extends String, ? extends Object> map ) {
        if (map == null || map.isEmpty()) return map;
        Map<String, Object> newMap = (Map<String, Object>)map; // just cast
        for (Map.Entry<? extends String, ? extends Object> entry : map.entrySet()) {
            Object orig = entry.getValue();
            Object unwrapped = unwrap(orig);
            if (orig != unwrapped) {
                String key = entry.getKey();
                newMap.put(key, unwrapped);
            }
        }
        return newMap;
    }

    @SuppressWarnings( "unchecked" )
    public static Collection<? extends Object> unwrapValues( Collection<?> c ) {
        if (c == null || c.isEmpty()) return c;
        if (c instanceof Set<?>) {
            Set<Object> replaced = null;
            Set<Object> result = (Set<Object>)c;
            Iterator<?> iter = c.iterator();
            while (iter.hasNext()) {
                Object orig = iter.next();
                Object unwrapped = unwrap(orig);
                if (orig != unwrapped) {
                    iter.remove();
                    if (replaced == null) replaced = new HashSet<Object>();
                    replaced.add(unwrapped);
                }
            }
            if (replaced != null) {
                result.addAll(replaced);
            }
            return result;
        }
        if (c instanceof List<?>) {
            List<Object> result = (List<Object>)c;
            ListIterator<Object> iter = result.listIterator();
            while (iter.hasNext()) {
                Object orig = iter.next();
                Object unwrapped = unwrap(orig);
                if (orig != unwrapped) {
                    iter.set(unwrapped);
                }
            }
            return result;
        }
        List<Object> result = new LinkedList<Object>();
        Iterator<?> iter = result.iterator();
        while (iter.hasNext()) {
            Object orig = iter.next();
            Object unwrapped = unwrap(orig);
            result.add(unwrapped);
        }
        return result;
    }

}
