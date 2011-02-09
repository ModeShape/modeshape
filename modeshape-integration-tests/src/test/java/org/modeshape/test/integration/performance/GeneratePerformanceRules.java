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
package org.modeshape.test.integration.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.CheckArg;

/**
 * A utility that generates the Byteman rules that can be used for performance tracing.
 */
public class GeneratePerformanceRules {

    /**
     * The pattern that splits a specification into the constituent parts. Group 1 is the name of the target class, and group 3 is
     * the (optional) method specification.
     */
    protected static final String SPECIFICATION_PATTERN_STRING = "([^\\(]*)([\\(]([^\\)]*)[\\)])?";
    protected static final Pattern SPECIFICATION_PATTERN = Pattern.compile(SPECIFICATION_PATTERN_STRING);

    /**
     * The pattern that splits a method specification into the constituent parts. Group 1 is the name of the target class, and
     * group 3 is the (optional) method specification.
     */
    protected static final String METHOD_SPECIFICATION_PATTERN_STRING = "([^\\(]*)([\\(]([^\\)]*)[\\)])?";
    protected static final Pattern METHOD_SPECIFICATION_PATTERN = Pattern.compile(METHOD_SPECIFICATION_PATTERN_STRING);

    public static void main( String[] args ) throws Exception {
        GeneratePerformanceRules generator = new GeneratePerformanceRules();
        String outputFile = "target/byteman/jcr-performance-generated.txt";
        generator.setOutputFile(outputFile);
        for (String arg : args) {
            arg = arg.trim();
            if (arg.equals("--verbose")) generator.setVerbose(true);
            if (arg.startsWith("--class=") && arg.length() > 8) {
                String classSpecification = arg.substring(8).trim();
                generator.instrumentClass(classSpecification);
            }
            if (arg.startsWith("--file=") && arg.length() > 7) outputFile = arg.substring(7).trim();
        }

        generator.generateRules();
    }

    public static final String DEFAULT_OUTPUT_FILE = "jcr-performance-generated.txt";

    private String outputFile = DEFAULT_OUTPUT_FILE;
    private ClassLoader classLoader;
    private List<Specification> specs = new ArrayList<Specification>();
    private boolean verbose = false;
    private String helperClass;

    public GeneratePerformanceRules() {
        this.classLoader = getClass().getClassLoader();
    }

    /**
     * @param outputFile Sets outputFile to the specified value.
     */
    public void setOutputFile( String outputFile ) {
        if (outputFile == null || outputFile.trim().length() == 0) outputFile = DEFAULT_OUTPUT_FILE;
        this.outputFile = outputFile;
    }

    /**
     * @return outputFile
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * @return helperClass
     */
    public String getHelperClass() {
        return helperClass;
    }

    /**
     * @param helperClass Sets helperClass to the specified value.
     */
    public void setHelperClass( String helperClass ) {
        this.helperClass = helperClass;
    }

    /**
     * @param classLoader Sets classLoader to the specified value.
     */
    public void setClassLoader( ClassLoader classLoader ) {
        if (classLoader == null) classLoader = getClass().getClassLoader();
        this.classLoader = classLoader;
    }

    /**
     * @return classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @return verbose
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @param verbose Sets verbose to the specified value.
     */
    public void setVerbose( boolean verbose ) {
        this.verbose = verbose;
    }

    public void instrumentClass( String specification ) throws ClassNotFoundException {
        // Parse the specification ...
        Matcher matcher = SPECIFICATION_PATTERN.matcher(specification);
        if (matcher.find()) {
            String targetClass = matcher.group(1);
            String methodSpecs = matcher.group(3);
            if (methodSpecs != null && methodSpecs.trim().length() != 0) {
                for (String methodSpec : methodSpecs.split(";")) {
                    String[] parts = methodSpec.split(":");
                    if (parts.length > 1) {
                        String visibilities = parts[0];
                        String methodClass = parts[1];
                        if (methodClass == null) continue;
                        methodClass = methodClass.trim();
                        if (methodClass.length() == 0) continue;
                        Set<Visibility> visibles = new HashSet<Visibility>();
                        for (String visibility : visibilities.split(",")) {
                            visibles.add(Visibility.parse(visibility));
                        }
                        Visibility[] vizzes = visibles.toArray(new Visibility[visibles.size()]);
                        instrumentClass(targetClass, methodClass, vizzes);
                    }
                }
            }
        }
    }

    public void instrumentClass( String clazz,
                                 String methodsIn,
                                 Visibility... visibilities ) throws ClassNotFoundException {
        CheckArg.isNotEmpty(clazz, "clazz");
        Class<?> target = classLoader.loadClass(clazz);
        Class<?> methods = methodsIn != null ? classLoader.loadClass(methodsIn) : null;
        instrumentClass(target, methods, visibilities);
    }

    public void instrumentClass( Class<?> target,
                                 Class<?> methodsIn,
                                 Visibility... visibilities ) {
        CheckArg.isNotNull(target, "target");
        specs.add(new Specification(target, methodsIn, visibilities));
    }

    public void generateRules() throws IOException {
        PrintStream stream = null;
        if ("System.out".equals(outputFile)) {
            stream = System.out;
        } else {
            File file = new File(outputFile);
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            stream = new PrintStream(file);
        }

        try {
            Set<Method> methods = new HashSet<Method>();
            for (Specification spec : specs) {
                boolean written = false;
                Class<?> targetClass = spec.getTargetClass();
                String targetClassName = targetClass.getCanonicalName();
                Class<?> methodClass = spec.getMethodsIn();
                for (Method method : methodClass.getDeclaredMethods()) {
                    if (!methods.add(method)) continue;
                    if (!spec.includes(method)) continue;
                    String signature = signatureFor(method);
                    if (!written) {
                        writeHeader(stream, "Rules for " + spec);
                        written = true;
                    }
                    writeMethodEnterRule(stream, targetClassName, method.getName(), signature);
                    writeMethodExitRule(stream, targetClassName, method.getName(), signature);
                }
            }
        } finally {
            if (stream != System.out) {
                stream.flush();
                stream.close();
            }
        }
    }

    protected String signatureFor( Method method ) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName());
        signature.append('(');
        boolean firstParam = true;
        for (Class<?> paramType : method.getParameterTypes()) {
            if (firstParam) firstParam = false;
            else signature.append(',');
            signature.append(paramType.getCanonicalName());
        }
        signature.append(')');
        return signature.toString();
    }

    /**
     * <p>
     * By default, this method generates the following:
     * 
     * <pre>
     * RULE tracing injection
     * CLASS &lt;targetClass> 
     * METHOD &lt;methodSignature>
     * HELPER &lt;helperClass>
     * AT ENTRY
     * IF TRUE
     * DO enterMethod(&lt;methodName>,$*)
     * ENDRULE
     * </pre>
     * 
     * </p>
     * 
     * @param out
     * @param targetClass
     * @param methodName
     * @param methodSignature
     */
    protected void writeMethodEnterRule( PrintStream out,
                                         String targetClass,
                                         String methodName,
                                         String methodSignature ) {
        out.println("RULE trace on enter for " + targetClass + "." + methodSignature);
        out.println("CLASS " + targetClass);
        out.println("METHOD " + methodSignature);
        if (getHelperClass() != null && getHelperClass().trim().length() != 0) out.println("HELPER " + getHelperClass());
        out.println("AT ENTRY");
        out.println("IF TRUE");
        out.println("DO enterMethod(\"" + methodName + "\",$*)");
        out.println("ENDRULE");
        out.println();
    }

    /**
     * <p>
     * By default, this method generates the following:
     * 
     * <pre>
     * RULE tracing injection
     * CLASS &lt;targetClass> 
     * METHOD &lt;methodSignature>
     * HELPER &lt;helperClass>
     * AT EXIT
     * IF TRUE
     * DO exitMethod(&lt;methodName>,$*)
     * ENDRULE
     * </pre>
     * 
     * </p>
     * 
     * @param out
     * @param targetClass
     * @param methodName
     * @param methodSignature
     */
    protected void writeMethodExitRule( PrintStream out,
                                        String targetClass,
                                        String methodName,
                                        String methodSignature ) {
        out.println("RULE trace on exit for " + targetClass + "." + methodSignature);
        out.println("CLASS " + targetClass);
        out.println("METHOD " + methodSignature);
        if (getHelperClass() != null && getHelperClass().trim().length() != 0) out.println("HELPER " + getHelperClass());
        out.println("AT EXIT");
        out.println("IF TRUE");
        out.println("DO exitMethod(\"" + methodName + "\",$*)");
        out.println("ENDRULE");
        out.println();
        out.println();
    }

    protected void writeHeader( PrintStream out,
                                String msg ) {
        out.println("# *****************************************************************************************************************");
        out.println("# " + msg);
        out.println("# *****************************************************************************************************************");
    }

    public static enum Visibility {
        PUBLIC,
        PROTECTED,
        PACKAGE,
        PRIVATE;
        public static Visibility parse( String value ) {
            if ("public".equalsIgnoreCase(value)) return PUBLIC;
            if ("protected".equalsIgnoreCase(value)) return PROTECTED;
            if ("package".equalsIgnoreCase(value)) return PACKAGE;
            if ("private".equalsIgnoreCase(value)) return PACKAGE;
            return PUBLIC;
        }
    }

    @Immutable
    public static class Specification {
        private final Class<?> clazz;
        private final Class<?> methodsIn;
        private final Set<Visibility> visibilities;

        public Specification( Class<?> clazz,
                              Class<?> methodsIn,
                              Visibility... visibilities ) {
            assert clazz != null;
            this.clazz = clazz;
            this.methodsIn = methodsIn != null ? methodsIn : clazz;
            this.visibilities = visibilities.length == 0 ? Collections.unmodifiableSet(EnumSet.of(Visibility.PUBLIC)) : Collections.unmodifiableSet(EnumSet.of(visibilities[0],
                                                                                                                                                               visibilities));
        }

        /**
         * Get the target of the rules.
         * 
         * @return the target class; never null
         */
        public Class<?> getTargetClass() {
            return clazz;
        }

        /**
         * Get the class that will be used to find methods.
         * 
         * @return the class containing the methods; never null
         */
        public Class<?> getMethodsIn() {
            return methodsIn;
        }

        /**
         * The visibilities for the methods that should be turned into rules.
         * 
         * @return the immutable set of visibilities; never null
         */
        public Set<Visibility> getVisibilities() {
            return visibilities;
        }

        public boolean includes( Method method ) {
            int modifiers = method.getModifiers();
            return includes(modifiers);
        }

        public boolean includes( int modifiers ) {
            if (Modifier.isPrivate(modifiers)) {
                if (visibilities.contains(Visibility.PRIVATE)) return true;
            } else if (Modifier.isProtected(modifiers)) {
                if (visibilities.contains(Visibility.PROTECTED)) return true;
            } else if (Modifier.isPublic(modifiers)) {
                if (visibilities.contains(Visibility.PUBLIC)) return true;
            } else {
                return visibilities.contains(Visibility.PACKAGE);
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
            if (visibilities.contains(Visibility.PUBLIC)) {
                sb.append("public");
            }
            if (visibilities.contains(Visibility.PROTECTED)) {
                if (sb.length() != 0) sb.append("/");
                sb.append("protected");
            }
            if (visibilities.contains(Visibility.PACKAGE)) {
                if (sb.length() != 0) sb.append("/");
                sb.append("package");
            }
            if (visibilities.contains(Visibility.PRIVATE)) {
                if (sb.length() != 0) sb.append("/");
                sb.append("private");
            }
            if (methodsIn != clazz && methodsIn != null) {
                sb.append(methodsIn.getCanonicalName());
                sb.append(" methods in ");
            }
            sb.append(' ');
            sb.append(clazz.getCanonicalName());
            return sb.toString();
        }
    }

}
