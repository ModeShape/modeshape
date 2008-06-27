/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.common.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for working reflectively with objects.
 * 
 * @author Randall Hauch
 */
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

    private Class<?> targetClass;
    private Map<String, LinkedList<Method>> methodMap = null; // used for the brute-force method finder

    /**
     * Construct a Reflection instance that cache's some information about the target class. The target class is the Class object
     * upon which the methods will be found.
     * 
     * @param targetClass the target class
     * @throws IllegalArgumentException if the target class is null
     */
    public Reflection( Class<?> targetClass ) {
        ArgCheck.isNotNull(targetClass, "targetClass");
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
     * Find the method on the target class that matches the supplied method name.
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
        return invokeBestMethodOnTarget(methodNamesArray, target, argument);
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
                            if (!args[i].isAssignableFrom(clazz)) {
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

}
