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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;

/**
 * A projection of content from a source into the integrated/federated repository. Each project consists of a set of {@link Rule
 * rules} for a particular source, where each rule defines how content within a source is
 * {@link Rule#getPathInRepository(Path, PathFactory) is project into the repository} and how the repository content is
 * {@link Rule#getPathInSource(Path, PathFactory) projected into the source}. Different rule subclasses are used for different
 * types.
 */
@Immutable
public class Projection implements Comparable<Projection>, Serializable {

    /**
     * Initial version
     */
    private static final long serialVersionUID = 1L;
    protected static final List<Method> parserMethods;
    private static final Logger LOGGER = Logger.getLogger(Projection.class);

    static {
        parserMethods = new CopyOnWriteArrayList<Method>();
        try {
            parserMethods.add(Projection.class.getDeclaredMethod("parsePathRule", String.class, ExecutionContext.class));
        } catch (Throwable err) {
            LOGGER.error(err, GraphI18n.errorAddingProjectionRuleParseMethod);
        }
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
    public static void addRuleParser( Method method ) {
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
    public static void addRuleParser( ClassLoader classLoader,
                                      String className,
                                      String methodName ) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
        CheckArg.isNotNull(classLoader, "classLoader");
        CheckArg.isNotEmpty(className, "className");
        CheckArg.isNotEmpty(methodName, "methodName");
        Class<?> clazz = Class.forName(className, true, classLoader);
        parserMethods.add(clazz.getMethod(className, String.class, ExecutionContext.class));
    }

    /**
     * Remove the rule parser method.
     * 
     * @param method the method to remove
     * @return true if the method was removed, or false if the method was not a registered rule parser method
     */
    public static boolean removeRuleParser( Method method ) {
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
    public static boolean removeRuleParser( String declaringClassName,
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
     * Parse the string form of a rule definition and return the rule
     * 
     * @param definition the definition of the rule that is to be parsed
     * @param context the environment in which this method is being executed; may not be null
     * @return the rule, or null if the definition could not be parsed
     */
    public static Rule fromString( String definition,
                                   ExecutionContext context ) {
        CheckArg.isNotNull(context, "env");
        definition = definition != null ? definition.trim() : "";
        if (definition.length() == 0) return null;
        for (Method method : parserMethods) {
            try {
                Rule rule = (Rule)method.invoke(null, definition, context);
                if (rule != null) return rule;
            } catch (Throwable err) {
                String msg = "Error while parsing project rule definition \"{0}\" using {1}";
                context.getLogger(Projection.class).trace(err, msg, definition, method);
            }
        }
        return null;
    }

    /**
     * Pattern that identifies the form:
     * 
     * <pre>
     *    repository_path =&gt; source_path [$ exception ]*
     * </pre>
     * 
     * where the following groups are captured on the first call to {@link Matcher#find()}:
     * <ol>
     * <li><code>repository_path</code></li>
     * <li><code>source_path</code></li>
     * </ol>
     * and the following groups are captured on subsequent calls to {@link Matcher#find()}:
     * <ol>
     * <li>exception</code></li>
     * </ol>
     * <p>
     * The regular expression is:
     * 
     * <pre>
     * ((?:[&circ;=$]|=(?!&gt;))+)(?:(?:=&gt;((?:[&circ;=$]|=(?!&gt;))+))( \$ (?:(?:[&circ;=]|=(?!&gt;))+))*)?
     * </pre>
     * 
     * </p>
     */
    protected static final String PATH_RULE_PATTERN_STRING = "((?:[^=$]|=(?!>))+)(?:(?:=>((?:[^=$]|=(?!>))+))( \\$ (?:(?:[^=]|=(?!>))+))*)?";
    protected static final Pattern PATH_RULE_PATTERN = Pattern.compile(PATH_RULE_PATTERN_STRING);

    /**
     * Parse the string definition of a {@link PathRule}. This method is automatically registered in the {@link #parserMethods
     * parser methods} by the static initializer of {@link Projection}.
     * 
     * @param definition the definition
     * @param context the environment
     * @return the path rule, or null if the definition is not in the right form
     */
    public static PathRule parsePathRule( String definition,
                                          ExecutionContext context ) {
        definition = definition != null ? definition.trim() : "";
        if (definition.length() == 0) return null;
        Matcher matcher = PATH_RULE_PATTERN.matcher(definition);
        if (!matcher.find()) return null;
        String reposPathStr = matcher.group(1);
        String sourcePathStr = matcher.group(2);
        if (reposPathStr == null || sourcePathStr == null) return null;
        reposPathStr = reposPathStr.trim();
        sourcePathStr = sourcePathStr.trim();
        if (reposPathStr.length() == 0 || sourcePathStr.length() == 0) return null;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path repositoryPath = pathFactory.create(reposPathStr);
        Path sourcePath = pathFactory.create(sourcePathStr);

        // Grab the exceptions ...
        List<Path> exceptions = new LinkedList<Path>();
        while (matcher.find()) {
            String exceptionStr = matcher.group(1);
            Path exception = pathFactory.create(exceptionStr);
            exceptions.add(exception);
        }
        return new PathRule(repositoryPath, sourcePath, exceptions);
    }

    private final String sourceName;
    private final String workspaceName;
    private final List<Rule> rules;
    private final boolean simple;
    private final boolean readOnly;
    private final int hc;

    /**
     * Create a new federated projection for the supplied source, using the supplied rules.
     * 
     * @param sourceName the name of the source
     * @param workspaceName the name of the workspace in the source; may be null if the default workspace is to be used
     * @param readOnly true if this projection is considered read-only, or false if the content of the projection may be modified
     *        by the federated clients
     * @param rules the projection rules
     * @throws IllegalArgumentException if the source name or rule array is null, empty, or contains all nulls
     */
    public Projection( String sourceName,
                       String workspaceName,
                       boolean readOnly,
                       Rule... rules ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotEmpty(rules, "rules");
        this.sourceName = sourceName;
        this.workspaceName = workspaceName;
        List<Rule> rulesList = new ArrayList<Rule>();
        for (Rule rule : rules) {
            if (rule != null) rulesList.add(rule);
        }
        this.readOnly = readOnly;
        this.rules = Collections.unmodifiableList(rulesList);
        CheckArg.isNotEmpty(this.rules, "rules");
        this.simple = computeSimpleProjection(this.rules);
        this.hc = HashCode.compute(this.sourceName, this.workspaceName);
    }

    /**
     * Get the name of the source to which this projection applies.
     * 
     * @return the source name
     * @see RepositorySource#getName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the name of the workspace in the source to which this projection applies.
     * 
     * @return the workspace name, or null if the default workspace of the {@link #getSourceName() source} is to be used
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Get the rules that define this projection.
     * 
     * @return the unmodifiable list of immutable rules; never null
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Get the paths in the source that correspond to the supplied path within the repository. This method computes the paths
     * given all of the rules. In general, most sources will probably project a node onto a single repository node. However, some
     * sources may be configured such that the same node in the repository is a projection of multiple nodes within the source.
     * 
     * @param canonicalPathInRepository the canonical path of the node within the repository; may not be null
     * @param factory the path factory; may not be null
     * @return the set of unique paths in the source projected from the repository path; never null
     * @throws IllegalArgumentException if the factory reference is null
     */
    public Set<Path> getPathsInSource( Path canonicalPathInRepository,
                                       PathFactory factory ) {
        CheckArg.isNotNull(factory, "factory");
        assert canonicalPathInRepository == null ? true : canonicalPathInRepository.equals(canonicalPathInRepository.getCanonicalPath());
        Set<Path> paths = new HashSet<Path>();
        for (Rule rule : getRules()) {
            Path pathInSource = rule.getPathInSource(canonicalPathInRepository, factory);
            if (pathInSource != null) paths.add(pathInSource);
        }
        return paths;
    }

    /**
     * Get the paths in the repository that correspond to the supplied path within the source. This method computes the paths
     * given all of the rules. In general, most sources will probably project a node onto a single repository node. However, some
     * sources may be configured such that the same node in the source is projected into multiple nodes within the repository.
     * 
     * @param canonicalPathInSource the canonical path of the node within the source; may not be null
     * @param factory the path factory; may not be null
     * @return the set of unique paths in the repository projected from the source path; never null
     * @throws IllegalArgumentException if the factory reference is null
     */
    public Set<Path> getPathsInRepository( Path canonicalPathInSource,
                                           PathFactory factory ) {
        CheckArg.isNotNull(factory, "factory");
        assert canonicalPathInSource == null ? true : canonicalPathInSource.equals(canonicalPathInSource.getCanonicalPath());
        Set<Path> paths = new HashSet<Path>();
        for (Rule rule : getRules()) {
            Path pathInRepository = rule.getPathInRepository(canonicalPathInSource, factory);
            if (pathInRepository != null) paths.add(pathInRepository);
        }
        return paths;
    }

    /**
     * Get the paths in the repository that serve as top-level nodes exposed by this projection.
     * 
     * @param factory the path factory that can be used to create new paths; may not be null
     * @return the list of top-level paths, in the proper order and containing no duplicates; never null
     */
    public List<Path> getTopLevelPathsInRepository( PathFactory factory ) {
        CheckArg.isNotNull(factory, "factory");
        List<Rule> rules = getRules();
        Set<Path> uniquePaths = new HashSet<Path>();
        List<Path> paths = new ArrayList<Path>(rules.size());
        for (Rule rule : getRules()) {
            for (Path path : rule.getTopLevelPathsInRepository(factory)) {
                if (!uniquePaths.contains(path)) {
                    paths.add(path);
                    uniquePaths.add(path);
                }
            }
        }
        return paths;
    }

    /**
     * Determine whether the supplied repositoryPath is considered one of the top-level nodes in this projection.
     * 
     * @param repositoryPath path in the repository; may not be null
     * @return true if the supplied repository path is one of the top-level nodes exposed by this projection, or false otherwise
     */
    public boolean isTopLevelPath( Path repositoryPath ) {
        for (Rule rule : getRules()) {
            if (rule.isTopLevelPath(repositoryPath)) return true;
        }
        return false;
    }

    /**
     * Determine whether this project is a simple projection that only involves for any one repository path no more than a single
     * source path.
     * 
     * @return true if this projection is a simple projection, or false if the projection is not simple (or it cannot be
     *         determined if it is simple)
     */
    public boolean isSimple() {
        return simple;
    }

    /**
     * Determine whether the content projected by this projection is read-only.
     * 
     * @return true if the content is read-only, or false if it can be modified
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    protected boolean computeSimpleProjection( List<Rule> rules ) {
        // Get the set of repository paths for the rules, and see if they overlap ...
        Set<Path> repositoryPaths = new HashSet<Path>();
        for (Rule rule : rules) {
            if (rule instanceof PathRule) {
                PathRule pathRule = (PathRule)rule;
                Path repoPath = pathRule.getPathInRepository();
                if (!repositoryPaths.isEmpty()) {
                    if (repositoryPaths.contains(repoPath)) return false;
                    for (Path path : repositoryPaths) {
                        if (path.isAtOrAbove(repoPath)) return false;
                        if (repoPath.isAtOrAbove(path)) return false;
                    }
                }
                repositoryPaths.add(repoPath);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Projection) {
            Projection that = (Projection)obj;
            if (this.hashCode() != that.hashCode()) return false;
            if (!this.getSourceName().equals(that.getSourceName())) return false;
            if (!this.getWorkspaceName().equals(that.getWorkspaceName())) return false;
            if (!this.getRules().equals(that.getRules())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Projection that ) {
        if (this == that) return 0;
        int diff = this.getSourceName().compareTo(that.getSourceName());
        if (diff != 0) return diff;
        diff = this.getWorkspaceName().compareTo(that.getWorkspaceName());
        if (diff != 0) return diff;
        Iterator<Rule> thisIter = this.getRules().iterator();
        Iterator<Rule> thatIter = that.getRules().iterator();
        while (thisIter.hasNext() && thatIter.hasNext()) {
            diff = thisIter.next().compareTo(thatIter.next());
            if (diff != 0) return diff;
        }
        if (thisIter.hasNext()) return 1;
        if (thatIter.hasNext()) return -1;
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.workspaceName != null) {
            sb.append(this.workspaceName);
            sb.append('@');
        }
        sb.append(this.sourceName);
        sb.append(" { ");
        boolean first = true;
        for (Rule rule : this.getRules()) {
            if (!first) sb.append(" ; ");
            sb.append(rule.toString());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * A rule used within a project do define how content within a source is projected into the federated repository. This mapping
     * is bi-directional, meaning it's possible to determine
     * <ul>
     * <li>the path in repository given a path in source; and</li>
     * <li>the path in source given a path in repository.</li>
     * </ul>
     * 
     * @author Randall Hauch
     */
    @Immutable
    public static abstract class Rule implements Comparable<Rule> {

        /**
         * Get the paths in the repository that serve as top-level nodes exposed by this rule.
         * 
         * @param factory the path factory that can be used to create new paths; may not be null
         * @return the list of top-level paths, which are ordered and which must be unique; never null
         */
        public abstract List<Path> getTopLevelPathsInRepository( PathFactory factory );

        /**
         * Determine if the supplied path is the same as one of the top-level nodes exposed by this rule.
         * 
         * @param path the path; may not be null
         * @return true if the supplied path is also one of the {@link #getTopLevelPathsInRepository(PathFactory) top-level paths}
         *         , or false otherwise
         */
        public abstract boolean isTopLevelPath( Path path );

        /**
         * Get the path in source that is projected from the supplied repository path, or null if the supplied repository path is
         * not projected into the source.
         * 
         * @param pathInRepository the path in the repository; may not be null
         * @param factory the path factory; may not be null
         * @return the path in source if it is projected by this rule, or null otherwise
         */
        public abstract Path getPathInSource( Path pathInRepository,
                                              PathFactory factory );

        /**
         * Get the path in repository that is projected from the supplied source path, or null if the supplied source path is not
         * projected into the repository.
         * 
         * @param pathInSource the path in the source; may not be null
         * @param factory the path factory; may not be null
         * @return the path in repository if it is projected by this rule, or null otherwise
         */
        public abstract Path getPathInRepository( Path pathInSource,
                                                  PathFactory factory );

        public abstract String getString( NamespaceRegistry registry,
                                          TextEncoder encoder );

        public abstract String getString( TextEncoder encoder );

        public abstract String getString();
    }

    /**
     * A rule that is defined with a single {@link #getPathInSource() path in source} and a single {@link #getPathInRepository()
     * path in repository}, and which has a set of {@link #getExceptionsToRule() path exceptions} (relative paths below the path
     * in source).
     * 
     * @author Randall Hauch
     */
    @Immutable
    public static class PathRule extends Rule {
        /** The path of the content as known to the source */
        private final Path sourcePath;
        /** The path where the content is to be placed ("projected") into the repository */
        private final Path repositoryPath;
        /** The paths (relative to the source path) that identify exceptions to this rule */
        private final List<Path> exceptions;
        private final int hc;
        private final List<Path> topLevelRepositoryPaths;

        public PathRule( Path repositoryPath,
                         Path sourcePath ) {
            this(repositoryPath, sourcePath, (Path[])null);
        }

        public PathRule( Path repositoryPath,
                         Path sourcePath,
                         Path... exceptions ) {
            CheckArg.isNotNull(sourcePath, "sourcePath");
            CheckArg.isNotNull(repositoryPath, "repositoryPath");
            this.sourcePath = sourcePath;
            this.repositoryPath = repositoryPath;
            if (exceptions == null || exceptions.length == 0) {
                this.exceptions = Collections.emptyList();
            } else {
                List<Path> exceptionList = new ArrayList<Path>();
                for (Path exception : exceptions) {
                    if (exception != null) exceptionList.add(exception);
                }
                this.exceptions = Collections.unmodifiableList(exceptionList);
            }
            this.hc = HashCode.compute(sourcePath, repositoryPath, exceptions);
            if (this.exceptions != null) {
                for (Path path : this.exceptions) {
                    if (path.isAbsolute()) {
                        throw new IllegalArgumentException(GraphI18n.pathIsNotRelative.text(path));
                    }
                }
            }
            this.topLevelRepositoryPaths = Collections.singletonList(getPathInRepository());
        }

        public PathRule( Path repositoryPath,
                         Path sourcePath,
                         List<Path> exceptions ) {
            CheckArg.isNotNull(sourcePath, "sourcePath");
            CheckArg.isNotNull(repositoryPath, "repositoryPath");
            this.sourcePath = sourcePath;
            this.repositoryPath = repositoryPath;
            if (exceptions == null || exceptions.isEmpty()) {
                this.exceptions = Collections.emptyList();
            } else {
                this.exceptions = Collections.unmodifiableList(new ArrayList<Path>(exceptions));
            }
            this.hc = HashCode.compute(sourcePath, repositoryPath, exceptions);
            if (this.exceptions != null) {
                for (Path path : this.exceptions) {
                    if (path.isAbsolute()) {
                        throw new IllegalArgumentException(GraphI18n.pathIsNotRelative.text(path));
                    }
                }
            }
            this.topLevelRepositoryPaths = Collections.singletonList(getPathInRepository());
        }

        /**
         * The path where the content is to be placed ("projected") into the repository.
         * 
         * @return the projected path of the content in the repository; never null
         */
        public Path getPathInRepository() {
            return repositoryPath;
        }

        /**
         * The path of the content as known to the source
         * 
         * @return the source-specific path of the content; never null
         */
        public Path getPathInSource() {
            return sourcePath;
        }

        /**
         * Get whether this rule has any exceptions.
         * 
         * @return true if this rule has exceptions, or false if it has none.
         */
        public boolean hasExceptionsToRule() {
            return exceptions.size() != 0;
        }

        /**
         * Get the paths that define the exceptions to this rule. These paths are always relative to the
         * {@link #getPathInSource() path in source}.
         * 
         * @return the unmodifiable exception paths; never null but possibly empty
         */
        public List<Path> getExceptionsToRule() {
            return exceptions;
        }

        /**
         * @param pathInSource
         * @return true if the source path is included by this rule
         */
        protected boolean includes( Path pathInSource ) {
            // Check whether the path is outside the source-specific path ...
            if (pathInSource != null && this.sourcePath.isAtOrAbove(pathInSource)) {

                // The path is inside the source-specific region, so check the exceptions ...
                List<Path> exceptions = getExceptionsToRule();
                if (exceptions.size() != 0) {
                    Path subpathInSource = pathInSource.relativeTo(this.sourcePath);
                    if (subpathInSource.size() != 0) {
                        for (Path exception : exceptions) {
                            if (subpathInSource.isAtOrBelow(exception)) return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getTopLevelPathsInRepository(org.modeshape.graph.property.PathFactory)
         */
        @Override
        public List<Path> getTopLevelPathsInRepository( PathFactory factory ) {
            return topLevelRepositoryPaths;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.federation.Projection.Rule#isTopLevelPath(org.modeshape.graph.property.Path)
         */
        @Override
        public boolean isTopLevelPath( Path path ) {
            for (Path topLevel : topLevelRepositoryPaths) {
                if (topLevel.equals(path)) return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This method considers a path that is at or below the rule's {@link #getPathInSource() source path} to be included,
         * except if there are {@link #getExceptionsToRule() exceptions} that explicitly disallow the path.
         * </p>
         * 
         * @see Rule#getPathInSource(Path, PathFactory)
         */
        @Override
        public Path getPathInSource( Path pathInRepository,
                                     PathFactory factory ) {
            assert pathInRepository.equals(pathInRepository.getCanonicalPath());
            // Project the repository path into the equivalent source path ...
            Path pathInSource = projectPathInRepositoryToPathInSource(pathInRepository, factory);

            // Check whether the source path is included by this rule ...
            return includes(pathInSource) ? pathInSource : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getPathInRepository(org.modeshape.graph.property.Path, org.modeshape.graph.property.PathFactory)
         */
        @Override
        public Path getPathInRepository( Path pathInSource,
                                         PathFactory factory ) {
            assert pathInSource.equals(pathInSource.getCanonicalPath());
            // Check whether the source path is included by this rule ...
            if (!includes(pathInSource)) return null;

            // Project the repository path into the equivalent source path ...
            return projectPathInSourceToPathInRepository(pathInSource, factory);
        }

        /**
         * Convert a path defined in the source system into an equivalent path in the repository system.
         * 
         * @param pathInSource the path in the source system, which may include the {@link #getPathInSource()}
         * @param factory the path factory; may not be null
         * @return the path in the repository system, which will be normalized and absolute (including the
         *         {@link #getPathInRepository()}), or null if the path is not at or under the {@link #getPathInSource()}
         */
        protected Path projectPathInSourceToPathInRepository( Path pathInSource,
                                                              PathFactory factory ) {
            if (this.sourcePath.equals(pathInSource)) return this.repositoryPath;
            if (!this.sourcePath.isAncestorOf(pathInSource)) return null;
            // Remove the leading source path ...
            Path relativeSourcePath = pathInSource.relativeTo(this.sourcePath);
            // Prepend the region's root path ...
            Path result = factory.create(this.repositoryPath, relativeSourcePath);
            return result.getNormalizedPath();
        }

        /**
         * Convert a path defined in the repository system into an equivalent path in the source system.
         * 
         * @param pathInRepository the path in the repository system, which may include the {@link #getPathInRepository()}
         * @param factory the path factory; may not be null
         * @return the path in the source system, which will be normalized and absolute (including the {@link #getPathInSource()}
         *         ), or null if the path is not at or under the {@link #getPathInRepository()}
         */
        protected Path projectPathInRepositoryToPathInSource( Path pathInRepository,
                                                              PathFactory factory ) {
            if (this.repositoryPath.equals(pathInRepository)) return this.sourcePath;
            if (!this.repositoryPath.isAncestorOf(pathInRepository)) return null;
            // Find the relative path from the root of this region ...
            Path pathInRegion = pathInRepository.relativeTo(this.repositoryPath);
            // Prepend the path in source ...
            Path result = factory.create(this.sourcePath, pathInRegion);
            return result.getNormalizedPath();
        }

        @Override
        public String getString( NamespaceRegistry registry,
                                 TextEncoder encoder ) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getPathInRepository().getString(registry, encoder));
            sb.append(" => ");
            sb.append(this.getPathInSource().getString(registry, encoder));
            if (this.getExceptionsToRule().size() != 0) {
                for (Path exception : this.getExceptionsToRule()) {
                    sb.append(" $ ");
                    sb.append(exception.getString(registry, encoder));
                }
            }
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getString(org.modeshape.common.text.TextEncoder)
         */
        @Override
        public String getString( TextEncoder encoder ) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getPathInRepository().getString(encoder));
            sb.append(" => ");
            sb.append(this.getPathInSource().getString(encoder));
            if (this.getExceptionsToRule().size() != 0) {
                for (Path exception : this.getExceptionsToRule()) {
                    sb.append(" $ ");
                    sb.append(exception.getString(encoder));
                }
            }
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getString()
         */
        @Override
        public String getString() {
            return getString(Path.JSR283_ENCODER);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hc;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathRule) {
                PathRule that = (PathRule)obj;
                if (!this.getPathInRepository().equals(that.getPathInRepository())) return false;
                if (!this.getPathInSource().equals(that.getPathInSource())) return false;
                if (!this.getExceptionsToRule().equals(that.getExceptionsToRule())) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( Rule other ) {
            if (other == this) return 0;
            if (other instanceof PathRule) {
                PathRule that = (PathRule)other;
                int diff = this.getPathInRepository().compareTo(that.getPathInRepository());
                if (diff != 0) return diff;
                diff = this.getPathInSource().compareTo(that.getPathInSource());
                if (diff != 0) return diff;
                Iterator<Path> thisIter = this.getExceptionsToRule().iterator();
                Iterator<Path> thatIter = that.getExceptionsToRule().iterator();
                while (thisIter.hasNext() && thatIter.hasNext()) {
                    diff = thisIter.next().compareTo(thatIter.next());
                    if (diff != 0) return diff;
                }
                if (thisIter.hasNext()) return 1;
                if (thatIter.hasNext()) return -1;
                return 0;
            }
            return other.getClass().getName().compareTo(this.getClass().getName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getString();
        }
    }
}
