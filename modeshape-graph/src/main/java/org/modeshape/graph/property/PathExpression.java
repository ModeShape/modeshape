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
package org.modeshape.graph.property;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.GraphI18n;

/**
 * An expression that defines an acceptable path using a regular-expression-like language. Path expressions can be used to
 * represent node paths or properties.
 * <p>
 * Let's first look at some simple examples of path expressions:
 * </p>
 * <table>
 * <tr>
 * <th>Path expression</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>/a/b</td>
 * <td>Match node "<code>b</code>" that is a child of the top level node "<code>a</code>". Neither node may have any
 * same-name-sibilings.</td>
 * </tr>
 * <tr>
 * <td>/a/*</td>
 * <td>Match any child node of the top level node "<code>a</code>".</td>
 * </tr>
 * <tr>
 * <td>/a/*.txt</td>
 * <td>Match any child node of the top level node "<code>a</code>" that also has a name ending in "<code>.txt</code>".</td>
 * </tr>
 * <tr>
 * <td>/a/b@c</td>
 * <td>Match the property "<code>c</code>" of node "<code>/a/b</code>".</td>
 * </tr>
 * <tr>
 * <td>/a/b[2]</td>
 * <td>The second child named "<code>b</code>" below the top level node "<code>a</code>".</td>
 * </tr>
 * <tr>
 * <td>/a/b[2,3,4]</td>
 * <td>The second, third or fourth child named "<code>b</code>" below the top level node "<code>a</code>".</td>
 * </tr>
 * <tr>
 * <td>/a/b[*]</td>
 * <td>Any (and every) child named "<code>b</code>" below the top level node "<code>a</code>".</td>
 * </tr>
 * <tr>
 * <td>//a/b</td>
 * <td>Any node named "<code>b</code>" that exists below a node named "<code>a</code>", regardless of where node "<code>a</code>"
 * occurs. Again, neither node may have any same-name-sibilings.</td>
 * </tr>
 * </table>
 * <p>
 * With these simple examples, you can probably discern the most important rules. First, the '<code>*</code>' is a wildcard
 * character that matches any character or sequence of characters in a node's name (or index if appearing in between square
 * brackets), and can be used in conjunction with other characters (e.g., "<code>*.txt</code>").
 * </p>
 * <p>
 * Second, square brackets (i.e., '<code>[</code>' and '<code>]</code>') are used to match a node's same-name-sibiling index. You
 * can put a single non-negative number or a comma-separated list of non-negative numbers. Use '0' to match a node that has no
 * same-name-sibilings, or any positive number to match the specific same-name-sibling.
 * </p>
 * <p>
 * Third, combining two delimiters (e.g., "<code>//</code>") matches any sequence of nodes, regardless of what their names are or
 * how many nodes. Often used with other patterns to identify nodes at any level matching other patterns. Three or more sequential
 * slash characters are treated as two.
 * </p>
 * <p>
 * Many path expressions can be created using just these simple rules. However, input paths can be more complicated. Here are some
 * more examples:
 * </p>
 * <table>
 * <tr>
 * <th>Path expressions</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>/a/(b|c|d)</td>
 * <td>Match children of the top level node "<code>a</code>" that are named "<code>a</code>", "<code>b</code>" or "<code>c</code>
 * ". None of the nodes may have same-name-sibling indexes.</td>
 * </tr>
 * <tr>
 * <td>/a/b[c/d]</td>
 * <td>Match node "<code>b</code>" child of the top level node "<code>a</code>", when node "<code>b</code>" has a child named "
 * <code>c</code>", and "<code>c</code>" has a child named "<code>d</code>". Node "<code>b</code>
 * " is the selected node, while nodes "<code>b</code>" and "<code>b</code>" are used as criteria but are not selected.</td>
 * </tr>
 * <tr>
 * <td>/a(/(b|c|d|)/e)[f/g/@something]</td>
 * <td>Match node "<code>/a/b/e</code>", "<code>/a/c/e</code>", "<code>/a/d/e</code>", or "<code>/a/e</code>
 * " when they also have a child "<code>f</code>" that itself has a child "<code>g</code>" with property "<code>something</code>".
 * None of the nodes may have same-name-sibling indexes.</td>
 * </tr>
 * </table>
 * <p>
 * These examples show a few more advanced rules. Parentheses (i.e., '<code>(</code>' and '<code>)</code>') can be used to define
 * a set of options for names, as shown in the first and third rules. Whatever part of the selected node's path appears between
 * the parentheses is captured for use within the output path. Thus, the first input path in the previous table would match node "
 * <code>/a/b</code>", and "b" would be captured and could be used within the output path using "<code>$1</code>", where the
 * number used in the output path identifies the parentheses.
 * </p>
 * <p>
 * Square brackets can also be used to specify criteria on a node's properties or children. Whatever appears in between the square
 * brackets does not appear in the selected node.
 * </p>
 * <h3>Repository and Workspace names</h3>
 * <p>
 * Path expressions can also specify restrictions on the repository name and workspace name, to constrain the path expression to
 * matching only paths from workspaces in repositories meeting the name criteria. Of course, if the path expression doesn't
 * include these restrictions, the repository and workspace names are not considered when matching paths.
 * </p>
 */
@Immutable
public class PathExpression implements Serializable {

    /**
     * Initial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Compile the supplied expression and return the resulting path expression instance.
     * 
     * @param expression the expression
     * @return the path expression; never null
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidPathExpressionException if the expression is blank or is not a valid expression
     */
    public static final PathExpression compile( String expression ) throws InvalidPathExpressionException {
        return new PathExpression(expression);
    }

    private static final String SEQUENCE_PATTERN_STRING = "\\[(\\d+(?:,\\d+)*)\\]"; // \[(\d+(,\d+)*)\]
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile(SEQUENCE_PATTERN_STRING);

    /**
     * Regular expression used to find unusable XPath predicates within an expression. This pattern results in unusable predicates
     * in group 1. Note that some predicates may be valid at the end but not valid elsewhere.
     * <p>
     * Currently, only index-like predicates (including sequences) are allowed everywhere. Predicates with paths and properties
     * are allowed only as the last predicate. Predicates with any operators are unused.
     * </p>
     * <p>
     * Nested predicates are not currently allowed.
     * </p>
     */
    // \[(?:(?:\d+(?:,\d+)*)|\*)\]|(?:\[[^\]\+\-\*=\!><'"\s]+\])$|(\[[^\]]+\])
    private static final String UNUSABLE_PREDICATE_PATTERN_STRING = "\\[(?:(?:\\d+(?:,\\d+)*)|\\*)\\]|(?:\\[[^\\]\\+\\-\\*=\\!><'\"\\s]+\\])$|(\\[[^\\]]+\\])";
    private static final Pattern UNUSABLE_PREDICATE_PATTERN = Pattern.compile(UNUSABLE_PREDICATE_PATTERN_STRING);

    /**
     * Regular expression used to find all XPath predicates except index and sequence patterns. This pattern results in the
     * predicates to be removed in group 1.
     */
    // \[(?:(?:\d+(?:,\d+)*)|\*)\]|(\[[^\]]+\])
    private static final String NON_INDEX_PREDICATE_PATTERN_STRING = "\\[(?:(?:\\d+(?:,\\d+)*)|\\*)\\]|(\\[[^\\]]+\\])";
    private static final Pattern NON_INDEX_PREDICATE_PATTERN = Pattern.compile(NON_INDEX_PREDICATE_PATTERN_STRING);

    /**
     * The regular expression that is used to extract the repository name, workspace name, and path from an path expression (or a
     * real path). The regular expression is <code>((([^:/]*):)?(([^:/]*):))?(.*)</code>. Group 3 will contain the repository
     * name, group 5 the workspace name, and group 6 the path.
     */
    private static final String REPOSITORY_AND_WORKSPACE_AND_PATH_PATTERN_STRING = "((([^:/]*):)?(([^:/]*):))?(.*)";
    private static final Pattern REPOSITORY_AND_WORKSPACE_AND_PATH_PATTERN = Pattern.compile(REPOSITORY_AND_WORKSPACE_AND_PATH_PATTERN_STRING);

    private final String expression;

    /**
     * This is the pattern that is used to determine if the particular path is from a particular repository. This pattern will be
     * null if the expression does not constrain the repository name.
     */
    private final Pattern repositoryPattern;

    /**
     * This is the pattern that is used to determine if the particular path is from a particular workspace. This pattern will be
     * null if the expression does not constrain the workspace name.
     */
    private final Pattern workspacePattern;
    /**
     * This is the pattern that is used to determine if there is a match with particular paths.
     */
    private final Pattern matchPattern;
    /**
     * This is the pattern that is used to determine which parts of the particular input paths are included in the
     * {@link Matcher#getSelectedNodePath() selected path}, only after the input path has already matched.
     */
    private final Pattern selectPattern;

    /**
     * Create the supplied expression.
     * 
     * @param expression the expression
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidPathExpressionException if the expression is blank or is not a valid expression
     */
    public PathExpression( String expression ) throws InvalidPathExpressionException {
        CheckArg.isNotNull(expression, "path expression");
        this.expression = expression.trim();
        if (this.expression.length() == 0) {
            throw new InvalidPathExpressionException(GraphI18n.pathExpressionMayNotBeBlank.text());
        }

        // Separate out the repository name, workspace name, and path fragments into separate match patterns ...
        RepositoryPath repoPath = parseRepositoryPath(this.expression);
        if (repoPath == null) {
            throw new InvalidPathExpressionException(GraphI18n.pathExpressionHasInvalidMatch.text(this.expression,
                                                                                                  this.expression));
        }
        String repoPatternStr = repoPath.repositoryName != null ? repoPath.repositoryName : ".*";
        String workPatternStr = repoPath.workspaceName != null ? repoPath.workspaceName : ".*";
        String pathPatternStr = repoPath.path;
        this.repositoryPattern = Pattern.compile(repoPatternStr);
        this.workspacePattern = Pattern.compile(workPatternStr);

        // Build the repository match pattern ...

        // Build the match pattern, which determines whether a path matches the condition ...
        String matchString = pathPatternStr;
        try {
            matchString = removeUnusedPredicates(matchString);
            matchString = replaceXPathPatterns(matchString);
            this.matchPattern = Pattern.compile(matchString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = GraphI18n.pathExpressionHasInvalidMatch.text(matchString, this.expression);
            throw new InvalidPathExpressionException(msg, e);
        }
        // Build the select pattern, which determines the path that will be selected ...
        String selectString = pathPatternStr;
        try {
            selectString = removeAllPredicatesExceptIndexes(selectString);
            selectString = replaceXPathPatterns(selectString);
            selectString = "(" + selectString + ").*"; // group 1 will have selected path ...
            this.selectPattern = Pattern.compile(selectString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = GraphI18n.pathExpressionHasInvalidSelect.text(selectString, this.expression);
            throw new InvalidPathExpressionException(msg, e);
        }
    }

    /**
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Replace certain XPath patterns that are not used or understood.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with all unused XPath patterns removed; never null
     */
    protected String removeUnusedPredicates( String expression ) {
        assert expression != null;
        java.util.regex.Matcher matcher = UNUSABLE_PREDICATE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            do {
                // Remove those predicates that show up in group 1 ...
                String predicateStr = matcher.group(0);
                String unusablePredicateStr = matcher.group(1);
                if (unusablePredicateStr != null) {
                    predicateStr = "";
                }
                matcher.appendReplacement(sb, predicateStr);
            } while (matcher.find());
            matcher.appendTail(sb);
            expression = sb.toString();
        }
        return expression;
    }

    /**
     * Remove all XPath predicates from the supplied regular expression string.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with all XPath predicates removed; never null
     */
    protected String removeAllPredicatesExceptIndexes( String expression ) {
        assert expression != null;
        java.util.regex.Matcher matcher = NON_INDEX_PREDICATE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            do {
                // Remove those predicates that show up in group 1 ...
                String predicateStr = matcher.group(0);
                String unusablePredicateStr = matcher.group(1);
                if (unusablePredicateStr != null) {
                    predicateStr = "";
                }
                matcher.appendReplacement(sb, predicateStr);
            } while (matcher.find());
            matcher.appendTail(sb);
            expression = sb.toString();
        }
        return expression;
    }

    /**
     * Replace certain XPath patterns, including some predicates, with substrings that are compatible with regular expressions.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with XPath patterns replaced with regular expression fragments; never null
     */
    protected String replaceXPathPatterns( String expression ) {
        assert expression != null;
        // replace 2 or more sequential '|' characters in an OR expression
        expression = expression.replaceAll("[\\|]{2,}", "|");
        // if there is an empty expression in an OR expression, make the whole segment optional ...
        // (e.g., "/a/b/(c|)/d" => "a/b(/(c))?/d"
        expression = expression.replaceAll("/(\\([^|]+)(\\|){2,}([^)]+\\))", "(/$1$2$3)?");
        expression = expression.replaceAll("/\\(\\|+([^)]+)\\)", "(?:/($1))?");
        expression = expression.replaceAll("/\\((([^|]+)(\\|[^|]+)*)\\|+\\)", "(?:/($1))?");

        // // Allow any path (that doesn't contain an explicit counter) to contain a counter,
        // // done by replacing any '/' or '|' that isn't preceded by ']' or '*' or '/' or '(' with '(\[\d+\])?/'...
        // input = input.replaceAll("(?<=[^\\]\\*/(])([/|])", "(?:\\\\[\\\\d+\\\\])?$1");

        // Does the path contain any '[]' or '[*]' or '[0]' or '[n]' (where n is any positive integers)...
        // '[*]/' => '(\[\d+\])?/'
        expression = expression.replaceAll("\\[\\]", "(?:\\\\[\\\\d+\\\\])?"); // index is optional
        // '[]/' => '(\[\d+\])?/'
        expression = expression.replaceAll("\\[[*]\\]", "(?:\\\\[\\\\d+\\\\])?"); // index is optional
        // '[0]/' => '(\[0\])?/'
        expression = expression.replaceAll("\\[0\\]", "(?:\\\\[0\\\\])?"); // index is optional
        // '[n]/' => '\[n\]/'
        expression = expression.replaceAll("\\[([1-9]\\d*)\\]", "\\\\[$1\\\\]"); // index is required

        // Change any other end predicates to not be wrapped by braces but to begin with a slash ...
        // ...'[x]' => ...'/x'
        expression = expression.replaceAll("(?<!\\\\)\\[([^\\]]*)\\]$", "/$1");

        // Replace all '[n,m,o,p]' type sequences with '[(n|m|o|p)]'
        java.util.regex.Matcher matcher = SEQUENCE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        boolean result = matcher.find();
        if (result) {
            do {
                String sequenceStr = matcher.group(1);
                boolean optional = false;
                if (sequenceStr.startsWith("0,")) {
                    sequenceStr = sequenceStr.replaceFirst("^0,", "");
                    optional = true;
                }
                if (sequenceStr.endsWith(",0")) {
                    sequenceStr = sequenceStr.replaceFirst(",0$", "");
                    optional = true;
                }
                if (sequenceStr.contains(",0,")) {
                    sequenceStr = sequenceStr.replaceAll(",0,", ",");
                    optional = true;
                }
                sequenceStr = sequenceStr.replaceAll(",", "|");
                String replacement = "\\\\[(?:" + sequenceStr + ")\\\\]";
                if (optional) {
                    replacement = "(?:" + replacement + ")?";
                }
                matcher.appendReplacement(sb, replacement);
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            expression = sb.toString();
        }

        // Order is important here
        expression = expression.replaceAll("[*]([^/(\\\\])", "[^/$1]*$1"); // '*' not followed by '/', '\\', or '('
        expression = expression.replaceAll("(?<!\\[\\^/\\])[*]", "[^/]*");
        expression = expression.replaceAll("[/]{2,}$", "(?:/[^/]*)*"); // ending '//'
        expression = expression.replaceAll("[/]{2,}", "(?:/[^/]*)*/"); // other '//'
        return expression;
    }

    /**
     * @return the expression
     */
    public String getSelectExpression() {
        return this.expression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.expression.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PathExpression) {
            PathExpression that = (PathExpression)obj;
            if (!this.expression.equalsIgnoreCase(that.expression)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.expression;
    }

    /**
     * Obtain a Matcher that can be used to convert the supplied absolute path (with repository name and workspace name) into an
     * output repository, and output workspace name, and output path.
     * 
     * @param absolutePath the path, of the form <code>{repoName}:{workspaceName}:{absPath}</code>, where
     *        <code>{repoName}:{workspaceName}:</code> is optional
     * @return the matcher; never null
     */
    public Matcher matcher( String absolutePath ) {
        // Extra the repository name, workspace name and absPath from the supplied path ...
        RepositoryPath repoPath = parseRepositoryPath(absolutePath);
        if (repoPath == null) {
            // No match, so return immediately ...
            return new Matcher(null, absolutePath, null, null, null);
        }
        String repoName = repoPath.repositoryName != null ? repoPath.repositoryName : "";
        String workspaceName = repoPath.workspaceName != null ? repoPath.workspaceName : "";
        String path = repoPath.path;

        // Determine if the input repository matches the repository name pattern ...
        if (!repositoryPattern.matcher(repoName).matches() || !workspacePattern.matcher(workspaceName).matches()) {
            // No match, so return immediately ...
            return new Matcher(null, path, null, null, null);
        }

        // Determine if the input path match the select expression ...
        String originalAbsolutePath = path;
        // if (!absolutePath.endsWith("/")) absolutePath = absolutePath + "/";
        // Remove all trailing '/' ...
        path = path.replaceAll("/+$", "");

        // See if the supplied absolute path matches the pattern ...
        final java.util.regex.Matcher matcher = this.matchPattern.matcher(path);
        if (!matcher.matches()) {
            // No match, so return immediately ...
            return new Matcher(matcher, originalAbsolutePath, null, null, null);
        }

        // The absolute path does match the pattern, so use the select pattern and try to grab the selected path ...
        final java.util.regex.Matcher selectMatcher = this.selectPattern.matcher(path);
        if (!selectMatcher.matches()) {
            // Nothing can be selected, so return immediately ...
            return new Matcher(matcher, null, null, null, null);
        }
        // Grab the selected path ...
        String selectedPath = selectMatcher.group(1);

        // Remove the trailing '/@property' ...
        selectedPath = selectedPath.replaceAll("/@[^/\\[\\]]+$", "");

        return new Matcher(matcher, originalAbsolutePath, repoName, workspaceName, selectedPath);
    }

    @Immutable
    public static class Matcher {

        private final String inputPath;
        private final String selectedRepository;
        private final String selectedWorkspace;
        private final String selectedPath;
        private final java.util.regex.Matcher inputMatcher;
        private final int hc;

        protected Matcher( java.util.regex.Matcher inputMatcher,
                           String inputPath,
                           String selectedRepository,
                           String selectedWorkspace,
                           String selectedPath ) {
            this.inputMatcher = inputMatcher;
            this.inputPath = inputPath;
            this.selectedRepository = selectedRepository == null || selectedRepository.length() == 0 ? null : selectedRepository;
            this.selectedWorkspace = selectedWorkspace == null || selectedWorkspace.length() == 0 ? null : selectedWorkspace;
            this.selectedPath = selectedPath;
            this.hc = HashCode.compute(this.inputPath, this.selectedPath);
        }

        public boolean matches() {
            return this.inputMatcher != null && this.selectedPath != null;
        }

        /**
         * @return inputPath
         */
        public String getInputPath() {
            return this.inputPath;
        }

        /**
         * @return selectPattern
         */
        public String getSelectedNodePath() {
            return this.selectedPath;
        }

        /**
         * Get the name of the selected repository.
         * 
         * @return the repository name, or null if there is none specified
         */
        public String getSelectedRepositoryName() {
            return this.selectedRepository;
        }

        /**
         * Get the name of the selected workspace.
         * 
         * @return the workspace name, or null if there is none specified
         */
        public String getSelectedWorkspaceName() {
            return this.selectedWorkspace;
        }

        public int groupCount() {
            if (this.inputMatcher == null) return 0;
            return this.inputMatcher.groupCount();
        }

        public String group( int groupNumber ) {
            return this.inputMatcher.group(groupNumber);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathExpression.Matcher) {
                PathExpression.Matcher that = (PathExpression.Matcher)obj;
                if (!this.inputPath.equalsIgnoreCase(that.inputPath)) return false;
                if (!this.selectedPath.equalsIgnoreCase(that.selectedPath)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return this.selectedPath;
        }
    }

    /**
     * Regular expression used to determine if the expression matches any single-level wildcard.
     */
    // /*(?:[*.](?:\[\*?\])?/*)*
    private static final String ANYTHING_PATTERN_STRING = "/*(?:[*.](?:\\[\\*?\\])?/*)*";
    private static final Pattern ANYTHING_PATTERN = Pattern.compile(ANYTHING_PATTERN_STRING);

    /**
     * Return whether this expression matches anything and therefore is not restrictive. These include expressions of any nodes ("
     * <code>/</code>"), any sequence of nodes ("<code>//</code>"), the self reference ("<code>.</code>"), or wildcard ("
     * <code>*</code>", "<code>*[]</code>" or "<code>*[*]</code>"). Combinations of these individual expressions are also
     * considered to match anything.
     * 
     * @return true if the expression matches anything, or false otherwise
     */
    public boolean matchesAnything() {
        return ANYTHING_PATTERN.matcher(expression).matches();
    }

    public static PathExpression all() {
        return ALL_PATHS_EXPRESSION;
    }

    private static final PathExpression ALL_PATHS_EXPRESSION = PathExpression.compile("//");

    /**
     * Parse a path of the form <code>{repoName}:{workspaceName}:{absolutePath}</code> or <code>{absolutePath}</code>.
     * 
     * @param path the path
     * @return the repository path, or null if the supplied path doesn't match any of the path patterns
     */
    public static RepositoryPath parseRepositoryPath( String path ) {
        // Extra the repository name, workspace name and absPath from the supplied path ...
        java.util.regex.Matcher pathMatcher = REPOSITORY_AND_WORKSPACE_AND_PATH_PATTERN.matcher(path);
        if (!pathMatcher.matches()) {
            // No match ...
            return null;
        }
        String repoName = pathMatcher.group(3);
        String workspaceName = pathMatcher.group(5);
        String absolutePath = pathMatcher.group(6);
        if (repoName == null || repoName.length() == 0 || repoName.trim().length() == 0) repoName = null;
        if (workspaceName == null || workspaceName.length() == 0 || workspaceName.trim().length() == 0) workspaceName = null;
        return new RepositoryPath(repoName, workspaceName, absolutePath);
    }

    @Immutable
    public static class RepositoryPath {
        public final String repositoryName;
        public final String workspaceName;
        public final String path;

        public RepositoryPath( String repositoryName,
                               String workspaceName,
                               String path ) {
            this.repositoryName = repositoryName;
            this.workspaceName = workspaceName;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return path.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof RepositoryPath) {
                RepositoryPath that = (RepositoryPath)obj;
                if (!ObjectUtil.isEqualWithNulls(this.repositoryName, that.repositoryName)) return false;
                if (!ObjectUtil.isEqualWithNulls(this.workspaceName, that.workspaceName)) return false;
                return this.path.equals(that.path);
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
            return (repositoryName != null ? repositoryName : "") + ":" + (workspaceName != null ? workspaceName : "") + ":"
                   + path;
        }

        public RepositoryPath withRepositoryName( String repositoryName ) {
            return new RepositoryPath(repositoryName, workspaceName, path);
        }

        public RepositoryPath withWorkspaceName( String workspaceName ) {
            return new RepositoryPath(repositoryName, workspaceName, path);
        }

        public RepositoryPath withPath( String path ) {
            return new RepositoryPath(repositoryName, workspaceName, path);
        }
    }

}
