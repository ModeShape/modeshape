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
package org.modeshape.graph.connector.federation;

import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.NamespaceRegistry;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A parser library for {@link Projection projections} and {@link Projection.Rule projection rules}.
 */
@ThreadSafe
public class ProjectionParser {
    private static final ProjectionParser INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(Projection.class);

    static {
        INSTANCE = new ProjectionParser();
        try {
            INSTANCE.addRuleParser(Projection.class, "parsePathRule");
            assert INSTANCE.parserMethods.size() == 1;
        } catch (Throwable err) {
            LOGGER.error(err, GraphI18n.errorAddingProjectionRuleParseMethod);
        }
    }

    /**
     * Get the shared projection parser, which is by default populated with the standard parser rules.
     * 
     * @return the parser; never null
     */
    public static ProjectionParser getInstance() {
        return INSTANCE;
    }

    private final List<Method> parserMethods = new CopyOnWriteArrayList<Method>();

    public ProjectionParser() {
    }

    /**
     * Add a static method that can be used to parse {@link Rule#getString(NamespaceRegistry, TextEncoder) rule definition
     * strings}. These methods must be static, must accept a {@link String} definition as the first parameter and an
     * {@link ExecutionContext} environment reference as the second parameter, and should return the resulting {@link Rule} (or
     * null if the definition format could not be understood by the method. Any exceptions during
     * {@link Method#invoke(Object, Object...) invocation} will be logged at the
     * {@link Logger#trace(Throwable, String, Object...) trace} level.
     * 
     * @param method the method to be added
     * @see #addRuleParser(ClassLoader, String, String)
     */
    public void addRuleParser( Method method ) {
        if (method != null) parserMethods.add(method);
    }

    /**
     * Add a static method that can be used to parse {@link Rule#getString(NamespaceRegistry, TextEncoder) rule definition
     * strings}. These methods must be static, must accept a {@link String} definition as the first parameter and an
     * {@link ExecutionContext} environment reference as the second parameter, and should return the resulting {@link Rule} (or
     * null if the definition format could not be understood by the method. Any exceptions during
     * {@link Method#invoke(Object, Object...) invocation} will be logged at the
     * {@link Logger#trace(Throwable, String, Object...) trace} level.
     * 
     * @param clazz the class on which the static method is defined; may not be null
     * @param methodName the name of the method
     * @throws SecurityException if there is a security exception while loading the class or getting the method
     * @throws NoSuchMethodException if the method does not exist on the class
     * @throws IllegalArgumentException if the class loader reference is null, or if the class name or method name are null or
     *         empty
     * @see #addRuleParser(Method)
     */
    public void addRuleParser( Class<?> clazz,
                               String methodName ) throws SecurityException, NoSuchMethodException {
        CheckArg.isNotNull(clazz, "clazz");
        CheckArg.isNotEmpty(methodName, "methodName");
        parserMethods.add(clazz.getMethod(methodName, String.class, ExecutionContext.class));
    }

    /**
     * Add a static method that can be used to parse {@link Rule#getString(NamespaceRegistry, TextEncoder) rule definition
     * strings}. These methods must be static, must accept a {@link String} definition as the first parameter and an
     * {@link ExecutionContext} environment reference as the second parameter, and should return the resulting {@link Rule} (or
     * null if the definition format could not be understood by the method. Any exceptions during
     * {@link Method#invoke(Object, Object...) invocation} will be logged at the
     * {@link Logger#trace(Throwable, String, Object...) trace} level.
     * 
     * @param classLoader the class loader that should be used to load the class on which the method is defined; may not be null
     * @param className the name of the class on which the static method is defined; may not be null
     * @param methodName the name of the method
     * @throws SecurityException if there is a security exception while loading the class or getting the method
     * @throws NoSuchMethodException if the method does not exist on the class
     * @throws ClassNotFoundException if the class could not be found given the supplied class loader
     * @throws IllegalArgumentException if the class loader reference is null, or if the class name or method name are null or
     *         empty
     * @see #addRuleParser(Method)
     */
    public void addRuleParser( ClassLoader classLoader,
                               String className,
                               String methodName ) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
        CheckArg.isNotNull(classLoader, "classLoader");
        CheckArg.isNotEmpty(className, "className");
        CheckArg.isNotEmpty(methodName, "methodName");
        Class<?> clazz = Class.forName(className, true, classLoader);
        parserMethods.add(clazz.getMethod(methodName, String.class, ExecutionContext.class));
    }

    /**
     * Remove the rule parser method.
     * 
     * @param method the method to remove
     * @return true if the method was removed, or false if the method was not a registered rule parser method
     */
    public boolean removeRuleParser( Method method ) {
        return parserMethods.remove(method);
    }

    /**
     * Remove the rule parser method.
     * 
     * @param declaringClassName the name of the class on which the static method is defined; may not be null
     * @param methodName the name of the method
     * @return true if the method was removed, or false if the method was not a registered rule parser method
     * @throws IllegalArgumentException if the class loader reference is null, or if the class name or method name are null or
     *         empty
     */
    public boolean removeRuleParser( String declaringClassName,
                                     String methodName ) {
        CheckArg.isNotEmpty(declaringClassName, "declaringClassName");
        CheckArg.isNotEmpty(methodName, "methodName");
        for (Method method : parserMethods) {
            if (method.getName().equals(methodName) && method.getDeclaringClass().getName().equals(declaringClassName)) {
                return parserMethods.remove(method);
            }
        }
        return false;
    }

    /**
     * @return parserMethods
     */
    /*package*/List<Method> getParserMethods() {
        return Collections.unmodifiableList(parserMethods);
    }

    /**
     * Parse the string form of a rule definition and return the rule
     * 
     * @param definition the definition of the rule that is to be parsed
     * @param context the environment in which this method is being executed; may not be null
     * @return the rule, or null if the definition could not be parsed
     */
    public Rule ruleFromString( String definition,
                                ExecutionContext context ) {
        CheckArg.isNotNull(context, "env");
        definition = definition != null ? definition.trim() : "";
        if (definition.length() == 0) return null;
        Logger logger = context.getLogger(getClass());
        for (Method method : parserMethods) {
            try {
                Rule rule = (Rule)method.invoke(null, definition, context);
                if (rule != null) {
                    if (logger.isTraceEnabled()) {
                        String msg = "Success parsing project rule definition \"{0}\" using {1}";
                        logger.trace(msg, definition, method);
                    }
                    return rule;
                } else if (logger.isTraceEnabled()) {
                    String msg = "Unable to parse project rule definition \"{0}\" using {1}";
                    logger.trace(msg, definition, method);
                }
            } catch (Throwable err) {
                String msg = "Error while parsing project rule definition \"{0}\" using {1}";
                logger.trace(err, msg, definition, method);
            }
        }
        return null;
    }

    /**
     * Parse string forms of an arry of rule definitions and return the rules
     * 
     * @param context the environment in which this method is being executed; may not be null
     * @param definitions the definition of the rules that are to be parsed
     * @return the rule, or null if the definition could not be parsed
     */
    public Rule[] rulesFromStrings( ExecutionContext context,
                                    String... definitions ) {
        List<Rule> rules = new LinkedList<Rule>();
        for (String definition : definitions) {
            Rule rule = ruleFromString(definition, context);
            if (rule != null) rules.add(rule);
        }
        return rules.toArray(new Rule[rules.size()]);
    }

    /**
     * Parse a single string containing one or more string forms of rule definitions, and return the rules. The string contains
     * each rule on a separate line.
     * 
     * @param context the environment in which this method is being executed; may not be null
     * @param definitions the definitions of the rules that are to be parsed, each definition separated by a newline character.
     * @return the rule, or null if the definition could not be parsed
     */
    public Rule[] rulesFromString( ExecutionContext context,
                                   String definitions ) {
        List<String> lines = StringUtil.splitLines(definitions);
        List<Rule> rules = new LinkedList<Rule>();
        for (String definition : lines) {
            Rule rule = ruleFromString(definition, context);
            if (rule != null) rules.add(rule);
        }
        return rules.toArray(new Rule[rules.size()]);
    }
}
