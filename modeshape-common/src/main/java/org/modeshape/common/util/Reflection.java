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
package org.modeshape.common.util;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.annotation.ReadOnly;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Inflector;

/**
 * Utility class for working reflectively with objects.
 */
@Immutable
public class Reflection {

    /**
     * Build the list of classes that correspond to the list of argument objects.
     * 
     * @param arguments the list of argument objects.
     * @return the list of Class instances that correspond to the list of argument objects; the resulting list will contain a null
     *         element for each null argument.
     */
    public static Class<?>[] buildArgumentClasses( Object... arguments ) {
        if (arguments == null || arguments.length == 0) return EMPTY_CLASS_ARRAY;
        Class<?>[] result = new Class<?>[arguments.length];
        int i = 0;
        for (Object argument : arguments) {
            if (argument != null) {
                result[i] = argument.getClass();
            } else {
                result[i] = null;
            }
        }
        return result;
    }

    /**
     * Build the list of classes that correspond to the list of argument objects.
     * 
     * @param arguments the list of argument objects.
     * @return the list of Class instances that correspond to the list of argument objects; the resulting list will contain a null
     *         element for each null argument.
     */
    public static List<Class<?>> buildArgumentClassList( Object... arguments ) {
        if (arguments == null || arguments.length == 0) return Collections.emptyList();
        List<Class<?>> result = new ArrayList<Class<?>>(arguments.length);
        for (Object argument : arguments) {
            if (argument != null) {
                result.add(argument.getClass());
            } else {
                result.add(null);
            }
        }
        return result;
    }

    /**
     * Convert any argument classes to primitives.
     * 
     * @param arguments the list of argument classes.
     * @return the list of Class instances in which any classes that could be represented by primitives (e.g., Boolean) were
     *         replaced with the primitive classes (e.g., Boolean.TYPE).
     */
    public static List<Class<?>> convertArgumentClassesToPrimitives( Class<?>... arguments ) {
        if (arguments == null || arguments.length == 0) return Collections.emptyList();
        List<Class<?>> result = new ArrayList<Class<?>>(arguments.length);
        for (Class<?> clazz : arguments) {
            if (clazz == Boolean.class) clazz = Boolean.TYPE;
            else if (clazz == Character.class) clazz = Character.TYPE;
            else if (clazz == Byte.class) clazz = Byte.TYPE;
            else if (clazz == Short.class) clazz = Short.TYPE;
            else if (clazz == Integer.class) clazz = Integer.TYPE;
            else if (clazz == Long.class) clazz = Long.TYPE;
            else if (clazz == Float.class) clazz = Float.TYPE;
            else if (clazz == Double.class) clazz = Double.TYPE;
            else if (clazz == Void.class) clazz = Void.TYPE;
            result.add(clazz);
        }

        return result;
    }

    /**
     * Returns the name of the class. The result will be the fully-qualified class name, or the readable form for arrays and
     * primitive types.
     * 
     * @param clazz the class for which the class name is to be returned.
     * @return the readable name of the class.
     */
    public static String getClassName( final Class<?> clazz ) {
        final String fullName = clazz.getName();
        final int fullNameLength = fullName.length();

        // Check for array ('[') or the class/interface marker ('L') ...
        int numArrayDimensions = 0;
        while (numArrayDimensions < fullNameLength) {
            final char c = fullName.charAt(numArrayDimensions);
            if (c != '[') {
                String name = null;
                // Not an array, so it must be one of the other markers ...
                switch (c) {
                    case 'L': {
                        name = fullName.subSequence(numArrayDimensions + 1, fullNameLength).toString();
                        break;
                    }
                    case 'B': {
                        name = "byte";
                        break;
                    }
                    case 'C': {
                        name = "char";
                        break;
                    }
                    case 'D': {
                        name = "double";
                        break;
                    }
                    case 'F': {
                        name = "float";
                        break;
                    }
                    case 'I': {
                        name = "int";
                        break;
                    }
                    case 'J': {
                        name = "long";
                        break;
                    }
                    case 'S': {
                        name = "short";
                        break;
                    }
                    case 'Z': {
                        name = "boolean";
                        break;
                    }
                    case 'V': {
                        name = "void";
                        break;
                    }
                    default: {
                        name = fullName.subSequence(numArrayDimensions, fullNameLength).toString();
                    }
                }
                if (numArrayDimensions == 0) {
                    // No array markers, so just return the name ...
                    return name;
                }
                // Otherwise, add the array markers and the name ...
                if (numArrayDimensions < BRACKETS_PAIR.length) {
                    name = name + BRACKETS_PAIR[numArrayDimensions];
                } else {
                    for (int i = 0; i < numArrayDimensions; i++) {
                        name = name + BRACKETS_PAIR[1];
                    }
                }
                return name;
            }
            ++numArrayDimensions;
        }

        return fullName;
    }

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[] {};
    private static final String[] BRACKETS_PAIR = new String[] {"", "[]", "[][]", "[][][]", "[][][][]", "[][][][][]"};

    private final Class<?> targetClass;
    private Map<String, LinkedList<Method>> methodMap = null; // used for the brute-force method finder

    /**
     * Construct a Reflection instance that cache's some information about the target class. The target class is the Class object
     * upon which the methods will be found.
     * 
     * @param targetClass the target class
     * @throws IllegalArgumentException if the target class is null
     */
    public Reflection( Class<?> targetClass ) {
        CheckArg.isNotNull(targetClass, "targetClass");
        this.targetClass = targetClass;
    }

    /**
     * Return the class that is the target for the reflection methods.
     * 
     * @return the target class
     */
    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    /**
     * Find the method on the target class that matches the supplied method name.
     * 
     * @param methodName the name of the method that is to be found.
     * @param caseSensitive true if the method name supplied should match case-sensitively, or false if case does not matter
     * @return the Method objects that have a matching name, or an empty array if there are no methods that have a matching name.
     */
    public Method[] findMethods( String methodName,
                                 boolean caseSensitive ) {
        Pattern pattern = caseSensitive ? Pattern.compile(methodName) : Pattern.compile(methodName, Pattern.CASE_INSENSITIVE);
        return findMethods(pattern);
    }

    /**
     * Find the methods on the target class that matches the supplied method name.
     * 
     * @param methodNamePattern the regular expression pattern for the name of the method that is to be found.
     * @return the Method objects that have a matching name, or an empty array if there are no methods that have a matching name.
     */
    public Method[] findMethods( Pattern methodNamePattern ) {
        final Method[] allMethods = this.targetClass.getMethods();
        final List<Method> result = new ArrayList<Method>();
        for (int i = 0; i < allMethods.length; i++) {
            final Method m = allMethods[i];
            if (methodNamePattern.matcher(m.getName()).matches()) {
                result.add(m);
            }
        }
        return result.toArray(new Method[result.size()]);
    }

    /**
     * Find the getter methods on the target class that begin with "get" or "is", that have no parameters, and that return
     * something other than void. This method skips the {@link Object#getClass()} method.
     * 
     * @return the Method objects for the getters; never null but possibly empty
     */
    public Method[] findGetterMethods() {
        final Method[] allMethods = this.targetClass.getMethods();
        final List<Method> result = new ArrayList<Method>();
        for (int i = 0; i < allMethods.length; i++) {
            final Method m = allMethods[i];
            int numParams = m.getParameterTypes().length;
            if (numParams != 0) continue;
            String name = m.getName();
            if (name.equals("getClass")) continue;
            if (m.getReturnType() == Void.TYPE) continue;
            if (name.startsWith("get") || name.startsWith("is") || name.startsWith("are")) {
                result.add(m);
            }
        }
        return result.toArray(new Method[result.size()]);
    }

    /**
     * Find the property names with getter methods on the target class. This method returns the property names for the methods
     * returned by {@link #findGetterMethods()}.
     * 
     * @return the Java Bean property names for the getters; never null but possibly empty
     */
    public String[] findGetterPropertyNames() {
        final Method[] getters = findGetterMethods();
        final List<String> result = new ArrayList<String>();
        for (int i = 0; i < getters.length; i++) {
            final Method m = getters[i];
            String name = m.getName();
            String propertyName = null;
            if (name.startsWith("get") && name.length() > 3) {
                propertyName = name.substring(3);
            } else if (name.startsWith("is") && name.length() > 2) {
                propertyName = name.substring(2);
            } else if (name.startsWith("are") && name.length() > 3) {
                propertyName = name.substring(3);
            }
            if (propertyName != null) {
                propertyName = INFLECTOR.camelCase(INFLECTOR.underscore(propertyName), false);
                result.add(propertyName);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Find the method on the target class that matches the supplied method name.
     * 
     * @param methodName the name of the method that is to be found.
     * @param caseSensitive true if the method name supplied should match case-sensitively, or false if case does not matter
     * @return the first Method object found that has a matching name, or null if there are no methods that have a matching name.
     */
    public Method findFirstMethod( String methodName,
                                   boolean caseSensitive ) {
        Pattern pattern = caseSensitive ? Pattern.compile(methodName) : Pattern.compile(methodName, Pattern.CASE_INSENSITIVE);
        return findFirstMethod(pattern);
    }

    /**
     * Find the method on the target class that matches the supplied method name.
     * 
     * @param methodNamePattern the regular expression pattern for the name of the method that is to be found.
     * @return the first Method object found that has a matching name, or null if there are no methods that have a matching name.
     */
    public Method findFirstMethod( Pattern methodNamePattern ) {
        final Method[] allMethods = this.targetClass.getMethods();
        for (int i = 0; i < allMethods.length; i++) {
            final Method m = allMethods[i];
            if (methodNamePattern.matcher(m.getName()).matches()) {
                return m;
            }
        }
        return null;
    }

    /**
     * Find and execute the best method on the target class that matches the signature specified with one of the specified names
     * and the list of arguments. If no such method is found, a NoSuchMethodException is thrown.
     * <P>
     * This method is unable to find methods with signatures that include both primitive arguments <i>and</i> arguments that are
     * instances of <code>Number</code> or its subclasses.
     * </p>
     * 
     * @param methodNames the names of the methods that are to be invoked, in the order they are to be tried
     * @param target the object on which the method is to be invoked
     * @param arguments the array of Object instances that correspond to the arguments passed to the method.
     * @return the Method object that references the method that satisfies the requirements, or null if no satisfactory method
     *         could be found.
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object invokeBestMethodOnTarget( String[] methodNames,
                                            Object target,
                                            Object... arguments )
        throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        Class<?>[] argumentClasses = buildArgumentClasses(arguments);
        int remaining = methodNames.length;
        Object result = null;
        for (String methodName : methodNames) {
            --remaining;
            try {
                Method method = findBestMethodWithSignature(methodName, argumentClasses);
                result = method.invoke(target, arguments);
                break;
            } catch (NoSuchMethodException e) {
                if (remaining == 0) throw e;
            }
        }
        return result;
    }

    /**
     * Find and execute the best setter method on the target class for the supplied property name and the supplied list of
     * arguments. If no such method is found, a NoSuchMethodException is thrown.
     * <P>
     * This method is unable to find methods with signatures that include both primitive arguments <i>and</i> arguments that are
     * instances of <code>Number</code> or its subclasses.
     * </p>
     * 
     * @param javaPropertyName the name of the property whose setter is to be invoked, in the order they are to be tried
     * @param target the object on which the method is to be invoked
     * @param argument the new value for the property
     * @return the result of the setter method, which is typically null (void)
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object invokeSetterMethodOnTarget( String javaPropertyName,
                                              Object target,
                                              Object argument )
        throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        String[] methodNamesArray = findMethodNames("set" + javaPropertyName);
        try {
            return invokeBestMethodOnTarget(methodNamesArray, target, argument);
        } catch (NoSuchMethodException e) {
            // If the argument is an Object[], see if it works with an array of whatever type the actual value is ...
            if (argument instanceof Object[]) {
                Object[] arrayArg = (Object[])argument;
                for (Object arrayValue : arrayArg) {
                    if (arrayValue == null) continue;
                    Class<?> arrayValueType = arrayValue.getClass();
                    // Create an array of this type ...
                    Object typedArray = Array.newInstance(arrayValueType, arrayArg.length);
                    Object[] newArray = (Object[])typedArray;
                    for (int i = 0; i != arrayArg.length; ++i) {
                        newArray[i] = arrayArg[i];
                    }
                    // Try to execute again ...
                    try {
                        return invokeBestMethodOnTarget(methodNamesArray, target, typedArray);
                    } catch (NoSuchMethodException e2) {
                        // Throw the original exception ...
                        throw e;
                    }
                }
            }
            throw e;
        }
    }

    /**
     * Find and execute the getter method on the target class for the supplied property name. If no such method is found, a
     * NoSuchMethodException is thrown.
     * 
     * @param javaPropertyName the name of the property whose getter is to be invoked, in the order they are to be tried
     * @param target the object on which the method is to be invoked
     * @return the property value (the result of the getter method call)
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object invokeGetterMethodOnTarget( String javaPropertyName,
                                              Object target )
        throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        String[] methodNamesArray = findMethodNames("get" + javaPropertyName);
        if (methodNamesArray.length <= 0) {
            // Try 'is' getter ...
            methodNamesArray = findMethodNames("is" + javaPropertyName);
        }
        if (methodNamesArray.length <= 0) {
            // Try 'are' getter ...
            methodNamesArray = findMethodNames("are" + javaPropertyName);
        }
        return invokeBestMethodOnTarget(methodNamesArray, target);
    }

    protected String[] findMethodNames( String methodName ) {
        Method[] methods = findMethods(methodName, false);
        Set<String> methodNames = new HashSet<String>();
        for (Method method : methods) {
            String actualMethodName = method.getName();
            methodNames.add(actualMethodName);
        }
        return methodNames.toArray(new String[methodNames.size()]);
    }

    /**
     * Find the best method on the target class that matches the signature specified with the specified name and the list of
     * arguments. This method first attempts to find the method with the specified arguments; if no such method is found, a
     * NoSuchMethodException is thrown.
     * <P>
     * This method is unable to find methods with signatures that include both primitive arguments <i>and</i> arguments that are
     * instances of <code>Number</code> or its subclasses.
     * 
     * @param methodName the name of the method that is to be invoked.
     * @param arguments the array of Object instances that correspond to the arguments passed to the method.
     * @return the Method object that references the method that satisfies the requirements, or null if no satisfactory method
     *         could be found.
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     */
    public Method findBestMethodOnTarget( String methodName,
                                          Object... arguments ) throws NoSuchMethodException, SecurityException {
        Class<?>[] argumentClasses = buildArgumentClasses(arguments);
        return findBestMethodWithSignature(methodName, argumentClasses);
    }

    /**
     * Find the best method on the target class that matches the signature specified with the specified name and the list of
     * argument classes. This method first attempts to find the method with the specified argument classes; if no such method is
     * found, a NoSuchMethodException is thrown.
     * 
     * @param methodName the name of the method that is to be invoked.
     * @param argumentsClasses the list of Class instances that correspond to the classes for each argument passed to the method.
     * @return the Method object that references the method that satisfies the requirements, or null if no satisfactory method
     *         could be found.
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     */
    public Method findBestMethodWithSignature( String methodName,
                                               Class<?>... argumentsClasses ) throws NoSuchMethodException, SecurityException {

        // Attempt to find the method
        Method result;

        // -------------------------------------------------------------------------------
        // First try to find the method with EXACTLY the argument classes as specified ...
        // -------------------------------------------------------------------------------
        Class<?>[] classArgs = null;
        try {
            classArgs = argumentsClasses != null ? argumentsClasses : new Class[] {};
            result = this.targetClass.getMethod(methodName, classArgs); // this may throw an exception if not found
            return result;
        } catch (NoSuchMethodException e) {
            // No method found, so continue ...
        }

        // ---------------------------------------------------------------------------------------------
        // Then try to find a method with the argument classes converted to a primitive, if possible ...
        // ---------------------------------------------------------------------------------------------
        List<Class<?>> argumentsClassList = convertArgumentClassesToPrimitives(argumentsClasses);
        try {
            classArgs = argumentsClassList.toArray(new Class[argumentsClassList.size()]);
            result = this.targetClass.getMethod(methodName, classArgs); // this may throw an exception if not found
            return result;
        } catch (NoSuchMethodException e) {
            // No method found, so continue ...
        }

        // ---------------------------------------------------------------------------------------------
        // Still haven't found anything. So far, the "getMethod" logic only finds methods that EXACTLY
        // match the argument classes (i.e., not methods declared with superclasses or interfaces of
        // the arguments). There is no canned algorithm in Java to do this, so we have to brute-force it.
        // The following algorithm will find the first method that matches by doing "instanceof", so it
        // may not be the best method. Since there is some overhead to this algorithm, the first time
        // caches some information in class members.
        // ---------------------------------------------------------------------------------------------
        Method method;
        LinkedList<Method> methodsWithSameName;
        if (this.methodMap == null) {
            // This is idempotent, so no need to lock or synchronize ...
            this.methodMap = new HashMap<String, LinkedList<Method>>();
            Method[] methods = this.targetClass.getMethods();
            for (int i = 0; i != methods.length; ++i) {
                method = methods[i];
                methodsWithSameName = this.methodMap.get(method.getName());
                if (methodsWithSameName == null) {
                    methodsWithSameName = new LinkedList<Method>();
                    this.methodMap.put(method.getName(), methodsWithSameName);
                }
                methodsWithSameName.addFirst(method); // add lower methods first
            }
        }

        // ------------------------------------------------------------------------
        // Find the set of methods with the same name (do this twice, once with the
        // original methods and once with the primitives) ...
        // ------------------------------------------------------------------------
        // List argClass = argumentsClasses;
        for (int j = 0; j != 2; ++j) {
            methodsWithSameName = this.methodMap.get(methodName);
            if (methodsWithSameName == null) {
                throw new NoSuchMethodException(methodName);
            }
            Iterator<Method> iter = methodsWithSameName.iterator();
            Class<?>[] args;
            Class<?> clazz;
            boolean allMatch;
            while (iter.hasNext()) {
                method = iter.next();
                args = method.getParameterTypes();
                if (args.length == argumentsClassList.size()) {
                    allMatch = true; // assume they all match
                    for (int i = 0; i < args.length; ++i) {
                        clazz = argumentsClassList.get(i);
                        if (clazz != null) {
                            Class<?> argClass = args[i];
                            if (argClass.isAssignableFrom(clazz)) {
                                // It's a match
                            } else if (argClass.isArray() && clazz.isArray()
                                       && argClass.getComponentType().isAssignableFrom(clazz.getComponentType())) {
                                // They're both arrays, and they're castable, so we're good ...
                            } else {
                                allMatch = false; // found one that doesn't match
                                i = args.length; // force completion
                            }
                        } else {
                            // a null is assignable for everything except a primitive
                            if (args[i].isPrimitive()) {
                                allMatch = false; // found one that doesn't match
                                i = args.length; // force completion
                            }
                        }
                    }
                    if (allMatch) {
                        return method;
                    }
                }
            }
        }

        throw new NoSuchMethodException(methodName);
    }
    
    /**
     * Get the representation of the named property (with the supplied labe, category, description, and allowed values) on the
     * target object.
     * <p>
     * If the label is not provided, this method looks for the {@link Label} annotation on the property's field and sets the label
     * to the annotation's literal value, or if the {@link Label#i18n()} class is referenced, the localized value of the
     * referenced {@link I18n}.
     * </p>
     * If the description is not provided, this method looks for the {@link Description} annotation on the property's field and
     * sets the label to the annotation's literal value, or if the {@link Description#i18n()} class is referenced, the localized
     * value of the referenced {@link I18n}. </p>
     * <p>
     * And if the category is not provided, this method looks for the {@link Category} annotation on the property's field and sets
     * the label to the annotation's literal value, or if the {@link Category#i18n()} class is referenced, the localized value of
     * the referenced {@link I18n}.
     * </p>
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @param label the new label for the property; may be null
     * @param category the category for this property; may be null
     * @param description the description for the property; may be null if this is not known
     * @param allowedValues the of allowed values, or null or empty if the values are not constrained
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public Property getProperty( Object target,
                                 String propertyName,
                                 String label,
                                 String category,
                                 String description,
                                 Object... allowedValues )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        Method[] setters = findMethods("set" + propertyName, false);
        boolean readOnly = setters.length < 1;
        Class<?> type = Object.class;
        Method[] getters = findMethods("get" + propertyName, false);
        if (getters.length == 0) {
            getters = findMethods("is" + propertyName, false);
        }
        if (getters.length == 0) {
            getters = findMethods("are" + propertyName, false);
        }
        if (getters.length > 0) {
            type = getters[0].getReturnType();
        }
        boolean inferred = true;
        Field field = null;
        try {
            // Find the corresponding field ...
            field = getField(targetClass, propertyName);
        } catch (NoSuchFieldException e) {
            // Nothing to do here
        	
        }
        if (description == null) {
            Description desc = getAnnotation(Description.class, field, getters, setters);
            if (desc != null) {
                description = localizedString(desc.i18n(), desc.value());
                inferred = false;
            }
        }
        if (label == null) {
            Label labelAnnotation = getAnnotation(Label.class, field, getters, setters);
            if (labelAnnotation != null) {
                label = localizedString(labelAnnotation.i18n(), labelAnnotation.value());
                inferred = false;
            }
        }
        if (category == null) {
            Category cat = getAnnotation(Category.class, field, getters, setters);
            if (cat != null) {
                category = localizedString(cat.i18n(), cat.value());
                inferred = false;
            }
        }
        if (!readOnly) {
            ReadOnly readOnlyAnnotation = getAnnotation(ReadOnly.class, field, getters, setters);
            if (readOnlyAnnotation != null) {
                readOnly = true;
                inferred = false;
            }
        }

        Property property = new Property(propertyName, label, description, category, readOnly, type, allowedValues);
        property.setInferred(inferred);
        return property;
    }
    
    /**
     * Get a Field intance for a given class and property. Iterate over super classes of a class
     * when a  <@link NoSuchFieldException> occurs until no more super classes are found then re-throw
     * the <@link NoSuchFieldException>.
     * 
     * @param targetClass
     * @param propertyName
     * @return Field
     * @throws NoSuchFieldException
     */
    protected Field getField(Class<?> targetClass, String propertyName) throws NoSuchFieldException{
    	Field field = null;
    	
    	try{
    		field = targetClass.getDeclaredField(Inflector.getInstance().lowerCamelCase(propertyName));
    	}catch(NoSuchFieldException e){
    		Class<?> clazz = targetClass.getSuperclass();
    		if (clazz!=null){
    				field = getField(clazz,propertyName);
    		}else{
    			throw e;
    		}
    	}
    		
    	return field;
    }
    
 	protected static <AnnotationType extends Annotation> AnnotationType getAnnotation( Class<AnnotationType> annotationType,
                                                                                       Field field,
                                                                                       Method[] getters,
                                                                                       Method[] setters ) {
        AnnotationType annotation = null;
        if (field != null) {
            annotation = field.getAnnotation(annotationType);
        }
        if (annotation == null && getters != null) {
            for (Method getter : getters) {
                annotation = getter.getAnnotation(annotationType);
                if (annotation != null) break;
            }
        }
        if (annotation == null && setters != null) {
            for (Method setter : setters) {
                annotation = setter.getAnnotation(annotationType);
                if (annotation != null) break;
            }
        }
        return annotation;
    }

    protected static String localizedString( Class<?> i18nClass,
                                             String id ) {
        if (i18nClass != null && !Object.class.equals(i18nClass) && id != null) {
            try {
                // Look up the I18n field ...
                Field i18nMsg = i18nClass.getDeclaredField(id);
                I18n msg = (I18n)i18nMsg.get(null);
                if (msg != null) {
                    return msg.text();
                }
            } catch (SecurityException err) {
                // ignore
            } catch (NoSuchFieldException err) {
                // ignore
            } catch (IllegalArgumentException err) {
                // ignore
            } catch (IllegalAccessException err) {
                // ignore
            }
        }
        return id;
    }

    /**
     * Get the representation of the named property (with the supplied description) on the target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @param description the description for the property; may be null if this is not known
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public Property getProperty( Object target,
                                 String propertyName,
                                 String description )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        return getProperty(target, propertyName, null, null, description);
    }

    /**
     * Get the representation of the named property on the target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param propertyName the name of the Java object property; may not be null
     * @return the representation of the Java property; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public Property getProperty( Object target,
                                 String propertyName )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        return getProperty(target, propertyName, null, null, null);
    }

    /**
     * Get representations for all of the Java properties on the supplied object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @return the list of all properties; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public List<Property> getAllPropertiesOn( Object target )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        String[] propertyNames = findGetterPropertyNames();
        List<Property> results = new ArrayList<Property>(propertyNames.length);
        for (String propertyName : propertyNames) {
            Property prop = getProperty(target, propertyName);
            results.add(prop);
        }
        Collections.sort(results);
        return results;
    }

    /**
     * Get representations for all of the Java properties on the supplied object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @return the map of all properties keyed by their name; never null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, or if 'propertyName' is null or empty
     */
    public Map<String, Property> getAllPropertiesByNameOn( Object target )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        String[] propertyNames = findGetterPropertyNames();
        Map<String, Property> results = new HashMap<String, Property>();
        for (String propertyName : propertyNames) {
            Property prop = getProperty(target, propertyName);
            results.put(prop.getName(), prop);
        }
        return results;
    }

    /**
     * Set the property on the supplied target object to the specified value.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param property the property that is to be set on the target
     * @param value the new value for the property
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, 'property' is null, or 'property.getName()' is null
     */
    public void setProperty( Object target,
                             Property property,
                             Object value )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotNull(property, "property");
        CheckArg.isNotNull(property.getName(), "property.getName()");
        invokeSetterMethodOnTarget(property.getName(), target, value);
    }

    /**
     * Get current value for the property on the supplied target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param property the property that is to be set on the target
     * @return the current value for the property; may be null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, 'property' is null, or 'property.getName()' is null
     */
    public Object getProperty( Object target,
                               Property property )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        CheckArg.isNotNull(target, "target");
        CheckArg.isNotNull(property, "property");
        CheckArg.isNotNull(property.getName(), "property.getName()");
        return invokeGetterMethodOnTarget(property.getName(), target);
    }

    /**
     * Get current value represented as a string for the property on the supplied target object.
     * 
     * @param target the target on which the setter is to be called; may not be null
     * @param property the property that is to be set on the target
     * @return the current value for the property; may be null
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException if access to the information is denied.
     * @throws IllegalAccessException if the setter method could not be accessed
     * @throws InvocationTargetException if there was an error invoking the setter method on the target
     * @throws IllegalArgumentException if 'target' is null, 'property' is null, or 'property.getName()' is null
     */
    public String getPropertyAsString( Object target,
                                       Property property )
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        Object value = getProperty(target, property);
        StringBuilder sb = new StringBuilder();
        writeObjectAsString(value, sb, false);
        return sb.toString();
    }

    protected void writeObjectAsString( Object obj,
                                        StringBuilder sb,
                                        boolean wrapWithBrackets ) {
        if (obj == null) {
            sb.append("null");
            return;
        }
        if (obj.getClass().isArray()) {
            Object[] array = (Object[])obj;
            boolean first = true;
            if (wrapWithBrackets) sb.append("[");
            for (Object value : array) {
                if (first) first = false;
                else sb.append(", ");
                writeObjectAsString(value, sb, true);
            }
            if (wrapWithBrackets) sb.append("]");
            return;
        }
        sb.append(obj);
    }

    protected static final Inflector INFLECTOR = Inflector.getInstance();

    /**
     * A representation of a property on a Java object.
     */
    public static class Property implements Comparable<Property>, Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String label;
        private String description;
        private Object value;
        private Collection<?> allowedValues;
        private Class<?> type;
        private boolean readOnly;
        private String category;
        private boolean inferred;

        /**
         * Create a new object property that has no fields initialized.
         */
        public Property() {
        }

        /**
         * Create a new object property with the supplied parameters set.
         * 
         * @param name the property name; may be null
         * @param label the human-readable property label; may be null
         * @param description the description for this property; may be null
         * @param readOnly true if the property is read-only, or false otherwise
         */
        public Property( String name,
                         String label,
                         String description,
                         boolean readOnly ) {
            this(name, label, description, null, readOnly, null);
        }

        /**
         * Create a new object property with the supplied parameters set.
         * 
         * @param name the property name; may be null
         * @param label the human-readable property label; may be null
         * @param description the description for this property; may be null
         * @param category the category for this property; may be null
         * @param readOnly true if the property is read-only, or false otherwise
         * @param type the value class; may be null
         * @param allowedValues the array of allowed values, or null or empty if the values are not constrained
         */
        public Property( String name,
                         String label,
                         String description,
                         String category,
                         boolean readOnly,
                         Class<?> type,
                         Object... allowedValues ) {
            setName(name);
            if (label != null) setLabel(label);
            if (description != null) setDescription(description);
            setCategory(category);
            setReadOnly(readOnly);
            setType(type);
            setAllowedValues(allowedValues);
        }

        /**
         * Get the property name in camel case. The getter method is simply "get" followed by the name of the property (with the
         * first character of the property converted to uppercase). The setter method is "set" (or "is" for boolean properties)
         * followed by the name of the property (with the first character of the property converted to uppercase).
         * 
         * @return the property name; never null, but possibly empty
         */
        public String getName() {
            return name != null ? name : "";
        }

        /**
         * Set the property name in camel case. The getter method is simply "get" followed by the name of the property (with the
         * first character of the property converted to uppercase). The setter method is "set" (or "is" for boolean properties)
         * followed by the name of the property (with the first character of the property converted to uppercase).
         * 
         * @param name the nwe property name; may be null
         */
        public void setName( String name ) {
            this.name = name;
            if (this.label == null) setLabel(null);
        }

        /**
         * Get the human-readable label for the property. This is often just a {@link Inflector#humanize(String, String...)
         * humanized} form of the {@link #getName() property name}.
         * 
         * @return label the human-readable property label; never null, but possibly empty
         */
        public String getLabel() {
            return label != null ? label : "";
        }

        /**
         * Set the human-readable label for the property. If null, this will be set to the
         * {@link Inflector#humanize(String, String...) humanized} form of the {@link #getName() property name}.
         * 
         * @param label the new label for the property; may be null
         */
        public void setLabel( String label ) {
            if (label == null && name != null) {
                label = INFLECTOR.titleCase(INFLECTOR.humanize(INFLECTOR.underscore(name)));
            }
            this.label = label;
        }

        /**
         * Get the description for this property.
         * 
         * @return the description; never null, but possibly empty
         */
        public String getDescription() {
            return description != null ? description : "";
        }

        /**
         * Set the description for this property.
         * 
         * @param description the new description for this property; may be null
         */
        public void setDescription( String description ) {
            this.description = description;
        }

        /**
         * Return whether this property is read-only.
         * 
         * @return true if the property is read-only, or false otherwise
         */
        public boolean isReadOnly() {
            return readOnly;
        }

        /**
         * Set whether this property is read-only.
         * 
         * @param readOnly true if the property is read-only, or false otherwise
         */
        public void setReadOnly( boolean readOnly ) {
            this.readOnly = readOnly;
        }

        /**
         * Get the name of the category in which this property belongs.
         * 
         * @return the category name; never null, but possibly empty
         */
        public String getCategory() {
            return category != null ? category : "";
        }

        /**
         * Set the name of the category in which this property belongs.
         * 
         * @param category the category name; may be null
         */
        public void setCategory( String category ) {
            this.category = category;
        }

        /**
         * Get the class to which the value must belong (excluding null values).
         * 
         * @return the value class; never null, but may be {@link Object Object.class}
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Set the class to which the value must belong (excluding null values).
         * 
         * @param type the value class; may be null
         */
        public void setType( Class<?> type ) {
            this.type = type != null ? type : Object.class;
        }

        /**
         * Determine if this is a boolean property (the {@link #getType() type} is a {@link Boolean} or boolean).
         * 
         * @return true if this is a boolean property, or false otherwise
         */
        public boolean isBooleanType() {
            return Boolean.class.equals(type) || Boolean.TYPE.equals(type);
        }

        /**
         * Determine if this is property's (the {@link #getType() type} is a primitive.
         * 
         * @return true if this property's type is a primitive, or false otherwise
         */
        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        /**
         * Determine if this is property's (the {@link #getType() type} is an array.
         * 
         * @return true if this property's type is an array, or false otherwise
         */
        public boolean isArrayType() {
            return type.isArray();
        }

        /**
         * Get the allowed values for this property. If this is non-null and non-empty, the value must be one of these values.
         * 
         * @return collection of allowed values, or the empty set if the values are not constrained
         */
        public Collection<?> getAllowedValues() {
            return allowedValues != null ? allowedValues : Collections.emptySet();
        }

        /**
         * Set the allowed values for this property. If this is non-null and non-empty, the value is expected to be one of these
         * values.
         * 
         * @param allowedValues the collection of allowed values, or null or empty if the values are not constrained
         */
        public void setAllowedValues( Collection<?> allowedValues ) {
            this.allowedValues = allowedValues;
        }

        /**
         * Set the allowed values for this property. If this is non-null and non-empty, the value is expected to be one of these
         * values.
         * 
         * @param allowedValues the array of allowed values, or null or empty if the values are not constrained
         */
        public void setAllowedValues( Object... allowedValues ) {
            if (allowedValues != null && allowedValues.length != 0) {
                this.allowedValues = new ArrayList<Object>(Arrays.asList(allowedValues));
            } else {
                this.allowedValues = null;
            }
        }

        /**
         * Return whether this property was inferred purely by reflection, or whether annotations were used for its definition.
         * 
         * @return true if it was inferred only by reflection, or false if at least one annotation was found and used
         */
        public boolean isInferred() {
            return inferred;
        }

        /**
         * Set whether this property was inferred purely by reflection.
         * 
         * @param inferred true if it was inferred only by reflection, or false if at least one annotation was found and used
         */
        public void setInferred( boolean inferred ) {
            this.inferred = inferred;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( Property that ) {
            if (this == that) return 0;
            if (that == null) return 1;
            int diff = ObjectUtil.compareWithNulls(this.category, that.category);
            if (diff != 0) return diff;
            diff = ObjectUtil.compareWithNulls(this.label, that.label);
            if (diff != 0) return diff;
            diff = ObjectUtil.compareWithNulls(this.name, that.name);
            if (diff != 0) return diff;
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return HashCode.compute(this.category, this.name, this.label);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Property) {
                Property that = (Property)obj;
                if (!ObjectUtil.isEqualWithNulls(this.category, that.category)) return false;
                if (!ObjectUtil.isEqualWithNulls(this.label, that.label)) return false;
                if (!ObjectUtil.isEqualWithNulls(this.name, that.name)) return false;
                if (!ObjectUtil.isEqualWithNulls(this.value, that.value)) return false;
                if (!ObjectUtil.isEqualWithNulls(this.readOnly, that.readOnly)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (name != null) sb.append(name).append(" = ");
            sb.append(value);
            sb.append(" ( ");
            sb.append(readOnly ? "readonly " : "writable ");
            if (category != null) sb.append("category=\"").append(category).append("\" ");
            if (label != null) sb.append("label=\"").append(label).append("\" ");
            if (description != null) sb.append("description=\"").append(description).append("\" ");
            sb.append(")");
            return sb.toString();
        }
    }
}
