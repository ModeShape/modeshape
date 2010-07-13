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
package org.modeshape.jdbc.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Static utilities for working with classes.
 */
public final class ClassUtil {

    private static void addObjectString( Object object,
                                         int includeInheritedFieldDepth,
                                         Class<?> clazz,
                                         StringBuffer text ) {

        // Add class's name
        text.append(nonPackageQualifiedName(clazz));

        text.append('(');

        // Add class's field names and object's corresponding values
        Field[] flds = clazz.getDeclaredFields();
        boolean separatorNeeded = false;
        for (int ndx = 0, len = flds.length; ndx < len; ++ndx) {
            Field fld = flds[ndx];
            try {

                // Attempt to ensure fields is accessible. Getting the value will throw an exception if the attempt failed.
                makeAccessible(fld);
                Object val = fld.get(object);

                // Skip static fields
                if ((fld.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }

                // Skip synthetic fields
                String name = fld.getName();
                if (name.indexOf('$') >= 0) {
                    continue;
                }

                // Add separator in text between fields
                separatorNeeded = addSeparator(separatorNeeded, text);

                // Add field's name and value to text
                text.append(fld.getName());
                text.append('=');
                text.append(val);

            } catch (Exception err) {
            }
        }

        // Add inheritied fields if requested
        if (includeInheritedFieldDepth > 0) {
            separatorNeeded = addSeparator(separatorNeeded, text);
            addObjectString(object, includeInheritedFieldDepth - 1, clazz.getSuperclass(), text);
        }

        text.append(')');
    }

    private static boolean addSeparator( boolean separatorNeeded,
                                         StringBuffer text ) {
        if (separatorNeeded) {
            text.append(", ");
        }
        return true;
    }

    /**
     * @param object
     */
    public static void makeAccessible( final AccessibleObject object ) {
        if (!object.isAccessible()) {
            if (System.getSecurityManager() == null) {
                object.setAccessible(true);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {

                    public Object run() {
                        object.setAccessible(true);
                        return null;
                    }
                });
            }
        }
    }

    /**
     * @param clazz A class.
     * @return The non-package-qualified name of the specified class. Note, inner class names will still be qualified by their
     *         enclosing class names and a "$" delimiter.
     */
    public static String nonPackageQualifiedName( final Class<?> clazz ) {
        // if (clazz == null) {
        // throw new IllegalArgumentException(I18n.format(CommonI18n.mustNotBeNull, "Class"));
        // }
        String name = clazz.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * @param object An object.
     * @return The non-package-qualified name of the class of the specified object. Note, inner class names will still be
     *         qualified by their enclosing class names and a "$" delimiter.
     */
    public static String nonPackageQualifiedName( final Object object ) {
        // if (object == null) {
        // throw new IllegalArgumentException(I18n.format(CommonI18n.mustNotBeNull, "Object"));
        // }
        return nonPackageQualifiedName(object.getClass());
    }

    /**
     * @param object
     * @param includeInheritedFieldDepth
     * @return A string representation of the specified object, consisting of its class name, properties, and property values.
     */
    public static String toString( Object object,
                                   int includeInheritedFieldDepth ) {
        StringBuffer text = new StringBuffer();
        addObjectString(object, includeInheritedFieldDepth, object.getClass(), text);
        return text.toString();
    }

    /**
     * Determine whether the supplied string represents a well-formed fully-qualified Java classname. This utility method enforces
     * no conventions (e.g., packages are all lowercase) nor checks whether the class is available on the classpath.
     * 
     * @param classname
     * @return true if the string is a fully-qualified class name
     */
    public static boolean isFullyQualifiedClassname( String classname ) {
        if (classname == null) return false;
        String[] parts = classname.split("[\\.]");
        if (parts.length == 0) return false;
        for (String part : parts) {
            CharacterIterator iter = new StringCharacterIterator(part);
            // Check first character (there should at least be one character for each part) ...
            char c = iter.first();
            if (c == CharacterIterator.DONE) return false;
            if (!Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c)) return false;
            c = iter.next();
            // Check the remaining characters, if there are any ...
            while (c != CharacterIterator.DONE) {
                if (!Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c)) return false;
                c = iter.next();
            }
        }
        return true;
    }

    private ClassUtil() {
    }
}
