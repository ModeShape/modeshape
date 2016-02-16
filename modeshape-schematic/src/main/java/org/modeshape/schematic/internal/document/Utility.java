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
package org.modeshape.schematic.internal.document;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Document;

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
    public static Map<? extends String, ?> unwrapValues( Map<? extends String, ?> map ) {
        if (map == null || map.isEmpty()) return map;
        Map<String, Object> newMap = (Map<String, Object>)map; // just cast
        for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
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
    public static Collection<?> unwrapValues( Collection<?> c ) {
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
                    if (replaced == null) replaced = new HashSet<>();
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
        return c.stream().map(Utility::unwrap).collect(Collectors.toList());
    }

}
