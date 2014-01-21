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
package org.modeshape.jcr.sequencer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.RepositoryI18n;
import org.modeshape.jcr.sequencer.PathExpression.WorkspacePath;

/**
 * An expression that defines a selection of some change in the repository that signals a sequencing operation should be run, and
 * the location where the sequencing output should be placed. Sequencer path expressions are used to determine whether information
 * in the repository needs to be sequenced.
 * <p>
 * A simple example is the following:
 * 
 * <pre>
 *     /a/b/c@title =&gt; /d/e/f
 * </pre>
 * 
 * which means that a sequencer (that uses this expression in its configuration) should be run any time there is a new or modified
 * <code>title</code> property on the <code>/a/b/c</code> node, and that the output of the sequencing should be placed at
 * <code>/d/e/f</code>.
 * </p>
 */
@Immutable
public class SequencerPathExpression implements Serializable {

    /**
     */
    private static final long serialVersionUID = 229464314137494765L;

    /**
     * The pattern used to break the initial input string into the two major parts, the selection and output expressions. Group 1
     * contains the selection expression, and group 2 contains the output expression.
     */
    private static final Pattern TWO_PART_PATTERN = Pattern.compile("((?:[^=]|=(?!>))+)(?:=>(.+))?");

    protected static final String DEFAULT_OUTPUT_EXPRESSION = ".";

    private static final String PARENT_PATTERN_STRING = "[^/]+/\\.\\./"; // [^/]+/\.\./
    private static final Pattern PARENT_PATTERN = Pattern.compile(PARENT_PATTERN_STRING);

    private static final String REPLACEMENT_VARIABLE_PATTERN_STRING = "(?<!\\\\)\\$(\\d+)"; // (?<!\\)\$(\d+)
    private static final Pattern REPLACEMENT_VARIABLE_PATTERN = Pattern.compile(REPLACEMENT_VARIABLE_PATTERN_STRING);

    /**
     * Compile the supplied expression and return the resulting SequencerPathExpression instance.
     * 
     * @param expression the expression
     * @return the path expression; never null
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidSequencerPathExpression if the expression is blank or is not a valid expression
     */
    public static final SequencerPathExpression compile( String expression ) throws InvalidSequencerPathExpression {
        CheckArg.isNotNull(expression, "sequencer path expression");
        expression = expression.trim();
        if (expression.length() == 0) {
            throw new InvalidSequencerPathExpression(RepositoryI18n.pathExpressionMayNotBeBlank.text());
        }
        java.util.regex.Matcher matcher = TWO_PART_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new InvalidSequencerPathExpression(RepositoryI18n.pathExpressionIsInvalid.text(expression));
        }
        String selectExpression = matcher.group(1);
        String outputExpression = matcher.group(2);
        return new SequencerPathExpression(PathExpression.compile(selectExpression), outputExpression);
    }

    private final PathExpression selectExpression;
    private final String outputExpression;
    private final int hc;

    protected SequencerPathExpression( PathExpression selectExpression,
                                       String outputExpression ) {
        CheckArg.isNotNull(selectExpression, "select expression");
        this.selectExpression = selectExpression;
        this.outputExpression = outputExpression != null ? outputExpression.trim() : DEFAULT_OUTPUT_EXPRESSION;
        this.hc = HashCode.compute(this.selectExpression, this.outputExpression);
    }

    /**
     * @return selectExpression
     */
    public String getSelectExpression() {
        return this.selectExpression.getSelectExpression();
    }

    /**
     * @return outputExpression
     */
    public String getOutputExpression() {
        return this.outputExpression;
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
        if (obj instanceof SequencerPathExpression) {
            SequencerPathExpression that = (SequencerPathExpression)obj;
            if (!this.selectExpression.equals(that.selectExpression)) return false;
            if (!this.outputExpression.equalsIgnoreCase(that.outputExpression)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.selectExpression + "=>" + this.outputExpression;
    }

    /**
     * Determine if this path expression applies to content within the supplied workspace name.
     * 
     * @param workspaceName the name of the workspace; may not be null
     * @return true if this path expression matches the workspace name, or false otherwise
     */
    public final boolean appliesToWorkspace( String workspaceName ) {
        return selectExpression.matchesWorkspace(workspaceName);
    }

    /**
     * Obtain a Matcher that can be used to convert the supplied workspace key and absolute path into an output workspace name and
     * and output path.
     * 
     * @param absolutePath the path in the workspace; may not be null
     * @return the matcher; never null
     */
    public Matcher matcher( String absolutePath ) {
        PathExpression.Matcher inputMatcher = selectExpression.matcher(absolutePath);
        String outputPath = null;
        WorkspacePath wsPath = null;
        if (inputMatcher.matches()) {
            // Grab the named groups ...
            Map<Integer, String> replacements = new HashMap<Integer, String>();
            for (int i = 0, count = inputMatcher.groupCount(); i <= count; ++i) {
                replacements.put(i, inputMatcher.group(i));
            }

            // Grab the selected path ...
            String selectedPath = inputMatcher.getSelectedNodePath();

            // Find the output path using the groups from the match pattern ...
            wsPath = PathExpression.parsePathInWorkspace(this.outputExpression);
            if (wsPath != null) {
                if (wsPath.workspaceName == null) wsPath = wsPath.withWorkspaceName(inputMatcher.getSelectedWorkspaceName());
                outputPath = wsPath.path;
                if (!DEFAULT_OUTPUT_EXPRESSION.equals(outputPath)) {
                    java.util.regex.Matcher replacementMatcher = REPLACEMENT_VARIABLE_PATTERN.matcher(outputPath);
                    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
                    StringBuffer sb = new StringBuffer();
                    if (replacementMatcher.find()) {
                        do {
                            String variable = replacementMatcher.group(1);
                            String replacement = replacements.get(Integer.valueOf(variable));
                            if (replacement == null) replacement = replacementMatcher.group(0);
                            replacementMatcher.appendReplacement(sb, replacement);
                        } while (replacementMatcher.find());
                        replacementMatcher.appendTail(sb);
                        outputPath = sb.toString();
                    }
                    // Make sure there is a trailing '/' ...
                    if (!outputPath.endsWith("/")) outputPath = outputPath + "/";

                    // Replace all references to "/./" with "/" ...
                    outputPath = outputPath.replaceAll("/\\./", "/");

                    // Remove any path segment followed by a parent reference ...
                    java.util.regex.Matcher parentMatcher = PARENT_PATTERN.matcher(outputPath);
                    while (parentMatcher.find()) {
                        outputPath = parentMatcher.replaceAll("");
                        // Make sure there is a trailing '/' ...
                        if (!outputPath.endsWith("/")) outputPath = outputPath + "/";
                        parentMatcher = PARENT_PATTERN.matcher(outputPath);
                    }

                    // Remove all multiple occurrences of '/' ...
                    outputPath = outputPath.replaceAll("/{2,}", "/");

                    // Remove the trailing '/@property' ...
                    outputPath = outputPath.replaceAll("/@[^/\\[\\]]+$", "");

                    // Remove a trailing '/' ...
                    outputPath = outputPath.replaceAll("/$", "");

                    // If the output path is blank, then use the default output expression ...
                    if (outputPath.length() == 0) outputPath = DEFAULT_OUTPUT_EXPRESSION;

                }
                if (DEFAULT_OUTPUT_EXPRESSION.equals(outputPath)) {
                    // The output path is the default expression, so use the selected path ...
                    outputPath = selectedPath;
                }
                wsPath = wsPath.withPath(outputPath);
            }
        }

        return new Matcher(inputMatcher, wsPath);
    }

    @Immutable
    public static class Matcher {

        private final PathExpression.Matcher inputMatcher;
        private final WorkspacePath outputPath;
        private final int hc;

        protected Matcher( PathExpression.Matcher inputMatcher,
                           WorkspacePath outputPath ) {
            this.inputMatcher = inputMatcher;
            this.outputPath = outputPath;
            this.hc = HashCode.compute(super.hashCode(), this.outputPath);
        }

        public boolean matches() {
            return inputMatcher.matches() && this.outputPath != null;
        }

        /**
         * @return inputPath
         */
        public String getInputPath() {
            return inputMatcher.getInputPath();
        }

        /**
         * Get the {@link #getInputPath() input path} in standard JCR form.
         * 
         * @return the JCR form of the input path; null only if {@link #getInputPath()} is null
         */
        public String getJcrInputPath() {
            // Conver the input path (which has a '@' to denote a property) to a standard JCR path ...
            String inputPath = getInputPath();
            if (inputPath == null) return null;
            int index = inputPath.lastIndexOf("/@");
            if (index != -1) {
                inputPath = inputPath.substring(0, index) + '/' + inputPath.substring(index + 2);
            }
            return inputPath;
        }

        /**
         * @return selectPattern
         */
        public String getSelectedPath() {
            return inputMatcher.getSelectedNodePath();
        }

        /**
         * Get the path in the repository where the sequenced content should be placed.
         * 
         * @return outputPath the output path, or null if this matcher does not match the input
         */
        public String getOutputPath() {
            return this.outputPath != null ? this.outputPath.path : null;
        }

        /**
         * Get the name of the workspace where the sequenced content should be placed.
         * 
         * @return outputPath the output path, or null if this matcher does not match the input
         */
        public String getOutputWorkspaceName() {
            return this.outputPath != null ? this.outputPath.workspaceName : null;
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
            if (obj instanceof SequencerPathExpression.Matcher) {
                SequencerPathExpression.Matcher that = (SequencerPathExpression.Matcher)obj;
                if (!super.equals(that)) return false;
                return ObjectUtil.isEqualWithNulls(this.outputPath, that.outputPath);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return inputMatcher + " => " + this.outputPath;
        }
    }

}
