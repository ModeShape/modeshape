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
package org.infinispan.schematic.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The constants pertaining to the JSON Schema (draft) specification.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class JsonSchema {

    public static class Version {
        /**
         * The version-related constants for the 3rd official draft of the specification.
         * 
         * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
         * @since 5.1
         */
        public static class Draft3 {
            /**
             * The URL of the Core Meta-Schema.
             */
            public static final String CORE_METASCHEMA_URL = "http://json-schema.org/draft-03/schema#";
        }

        /**
         * The version-related constants for the most recent version.
         * 
         * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
         * @since 5.1
         */
        public static class Latest {
            /**
             * The URL of the Core Meta-Schema.
             */
            public static final String CORE_METASCHEMA_URL = Draft3.CORE_METASCHEMA_URL;
        }
    }

    /**
     * The enumeration representing the standard types used in the JSON Schema (draft) specification. Type.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    public static enum Type {
        STRING("string"),
        NUMBER("number"),
        INTEGER("integer"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array"),
        NULL("null"),
        ANY("any"),
        UNION("union"),
        UNKNOWN("unknown");

        private static final Map<String, Type> TYPE_BY_LOWERCASE;
        private static final Map<Type, EnumSet<Type>> EQUIVALENT_TYPES;
        static {
            Map<String, Type> typeByLowercase = new HashMap<String, JsonSchema.Type>();
            typeByLowercase.put(STRING.toString().toLowerCase(), STRING);
            typeByLowercase.put(NUMBER.toString().toLowerCase(), NUMBER);
            typeByLowercase.put(INTEGER.toString().toLowerCase(), INTEGER);
            typeByLowercase.put(BOOLEAN.toString().toLowerCase(), BOOLEAN);
            typeByLowercase.put(OBJECT.toString().toLowerCase(), OBJECT);
            typeByLowercase.put(ARRAY.toString().toLowerCase(), ARRAY);
            typeByLowercase.put(NULL.toString().toLowerCase(), NULL);
            typeByLowercase.put(ANY.toString().toLowerCase(), ANY);
            typeByLowercase.put(UNION.toString().toLowerCase(), UNION);
            typeByLowercase.put(UNKNOWN.toString().toLowerCase(), UNKNOWN);
            TYPE_BY_LOWERCASE = Collections.unmodifiableMap(typeByLowercase);

            Map<Type, EnumSet<Type>> equiv = new HashMap<Type, EnumSet<Type>>();
            equiv.put(Type.STRING, EnumSet.of(Type.STRING));
            equiv.put(Type.NUMBER, EnumSet.of(Type.NUMBER, Type.INTEGER));
            equiv.put(Type.INTEGER, EnumSet.of(Type.INTEGER));
            equiv.put(Type.BOOLEAN, EnumSet.of(Type.BOOLEAN));
            equiv.put(Type.OBJECT, EnumSet.of(Type.OBJECT));
            equiv.put(Type.ARRAY, EnumSet.of(Type.ARRAY));
            equiv.put(Type.NULL, EnumSet.of(Type.NULL));
            equiv.put(Type.ANY, EnumSet.of(Type.STRING,
                                           Type.NUMBER,
                                           Type.INTEGER,
                                           Type.BOOLEAN,
                                           Type.OBJECT,
                                           Type.ARRAY,
                                           Type.NULL,
                                           Type.ANY,
                                           Type.UNION,
                                           Type.UNKNOWN));
            equiv.put(Type.UNION, EnumSet.of(Type.UNKNOWN));
            equiv.put(Type.UNKNOWN, EnumSet.of(Type.UNKNOWN));
            EQUIVALENT_TYPES = Collections.unmodifiableMap(equiv);
        }

        private final String type;

        private Type( String type ) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }

        /**
         * Find the type enumeration literal given the (case-insensitive) name.
         * 
         * @param name the case-independent name of the type
         * @return the type, or null if there is no such type
         */
        public static Type byName( String name ) {
            return TYPE_BY_LOWERCASE.get(name.toLowerCase());
        }

        /**
         * Determine whether this type is equivalent to the supplied type.
         * 
         * @param other the type to be compared with this type
         * @return true if the types are equivalent, or false otherwise
         */
        public boolean isEquivalent( Type other ) {
            return other != null ? EQUIVALENT_TYPES.get(this).contains(other) : false;
        }

        /**
         * Determine the type for the given value.
         * 
         * @param value the field value
         * @return the corresponding type for the value; never null
         */
        public static Type typeFor( Object value ) {
            if (value == null) return NULL;
            if (value instanceof String) // most will be strings
            return STRING;
            if (value instanceof Integer) return INTEGER;
            if (value instanceof Long) return INTEGER;
            if (value instanceof Float) return NUMBER;
            if (value instanceof Double) return NUMBER;
            if (value instanceof Boolean) return BOOLEAN;
            if (value instanceof List) // needs to be compared **before** Document
            return ARRAY;
            if (value instanceof Document) return OBJECT;
            if (value instanceof Null) return NULL;
            if (value instanceof Symbol) return STRING;
            return UNKNOWN;
        }

        /**
         * Obtain the set of types given the supplied name or iterable container of names.
         * 
         * @param typeName the String or Symbol representation of a single type name, or an Iterable&amp;?> list of names
         * @return the set of types that correspond to the supplied names; never null but possibly an empty set
         */
        public static EnumSet<Type> typesWithNames( Object typeName ) {
            if (typeName instanceof List) {
                List<Type> result = new ArrayList<Type>();
                Iterable<?> typeNames = (Iterable<?>)typeName;
                for (Object tname : typeNames) {
                    if (tname == null) continue;
                    String name = tname.toString();
                    Type type = byName(name);
                    if (type != null) result.add(type);
                }
                if (!result.isEmpty()) {
                    return EnumSet.copyOf(result);
                }
            } else if (typeName instanceof String || typeName instanceof Symbol) {
                String name = typeName.toString();
                Type type = byName(name);
                return EnumSet.of(type);
            }
            return EnumSet.noneOf(Type.class);
        }
    }

}
