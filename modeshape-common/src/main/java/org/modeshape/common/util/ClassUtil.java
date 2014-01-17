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
package org.modeshape.common.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import org.modeshape.common.annotation.Immutable;

/**
 * Static utilities for working with classes.
 */
@Immutable
public final class ClassUtil {

    private static void addObjectString( Object object,
                                         int includeInheritedFieldDepth,
                                         Class<?> clazz,
                                         StringBuilder text ) {

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
                                         StringBuilder text ) {
        if (separatorNeeded) {
            text.append(", ");
        }
        return true;
    }

    public static void makeAccessible( final AccessibleObject object ) {
        if (!object.isAccessible()) {
            if (System.getSecurityManager() == null) {
                object.setAccessible(true);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {

                    @Override
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
        StringBuilder text = new StringBuilder();
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
