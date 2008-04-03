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
package org.jboss.dna.services.sequencers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.observation.Event;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.services.ServicesI18n;

/**
 * An expression that defines the paths for which a sequencer should be run, and represent the first level of decision making for
 * sequencers. The objective for path expressions are to quickly match against {@link Event#getPath() event paths} to determine
 * which sequencers may be interested in the event and which sequencers can safely be skipped.
 * <p>
 * Each expression defines two pieces of information: the specification of the node where the sequencing output should be placed
 * (called the "select path"), and the criteria that should be used to match against node and property changes (called the
 * "criteria path"). Often the criteria is relative to the specification.
 * </p>
 * <p>
 * Path expressions adopt a restricted subset of the XPath notation, and consists of multiple steps separated by '/' or '//'. Path
 * expressions beginning with '/' are absolute paths. The '/' character is a simple delimiter, while '//' denotes any number of
 * steps (0 or more). Path expressions implicitly deal with the names of nodes and not the node types, but they are case
 * insensitive. The remaining details about expressions are described below.
 * </p>
 * <h3>Selecting a node that has new or changed properties</h3>
 * <p>
 * The most straightforward examples involve selecting a node for sequencing based on a new or changed property of that node. This
 * is the typical case, as the node is to be sequenced based upon some content stored as a property on the node. In this case, the
 * node selected for the output is also the node that will be sequenced. Here are some examples:
 * <ul>
 * <li><code>/a/b/c/d</code> - Selects the <code>/a/b/c/d</code> node when the node was created or when it contains any new
 * or changed property.</li>
 * <li><code>/a/b/c/d@title</code> - Selects the <code>/a/b/c/d</code> node that contains a <code>title</code> property has
 * been created or changed.</li>
 * <li><code>/a/b/c/d[@title]</code> - Selects the <code>/a/b/c/d</code> node that contains a <code>title</code> property
 * has been created or changed.</li>
 * <li><code>//[@title]</code> - Selects any node that contains a new or changed <code>title</code> property.</li>
 * <li><code>/a/b/c/d[@title]</code> - Selects the <code>/a/b/c/d</code> node that contains a <code>title</code> property
 * has been created or changed.</li>
 * <li><code>/a/b/c/d[@*name]</code> - Selects the <code>/a/b/c/d</code> node that contains a property whose name ends with
 * <code>name</code> has been created or changed.</li>
 * <li><code>/a/b/c/*[@title]</code> - Selects any node that is a child of the <code>/a/b/c</code> node where the child
 * contains a <code>title</code> property has been created or changed.</li>
 * <li><code>/a//d[@title]</code> - Selects any node named <code>d</code> that is a decendant of <code>/a</code> and that
 * contains a <code>title</code> property has been created or changed.</li>
 * <li><code>/a/&#42;/d[@title]</code> - Selects any node named <code>d</code> that is a grandchild of <code>/a</code> and
 * that contains a <code>title</code> property has been created or changed.</li>
 * <li><code>/a/(b|c)/d[@title]</code> - Selects either the <code>/a/b/d</code> or <code>/a/c/d</code> node that contains a
 * <code>title</code> property has been created or changed.</li>
 * <li><code>/a/(b|c|)/d[@title]</code> - Selects either the <code>/a/b/d</code>, <code>/a/c/d</code> or <code>/a/d</code>
 * node that contains a <code>title</code> property has been created or changed.</li>
 * </ul>
 * </p>
 * <h3>Selecting a node that contains a changed node</h3>
 * <p>
 * Other times it is desirable to select a node above another node that has new or changed properties. In this case, the output
 * node is higher in the tree than the changed node that will be sequenced. Here are some examples:
 * <ul>
 * <li><code>/a/b/c[d/@title]</code> - Selects the <code>/a/b/c</code> node that contains a child <code>d</code> with a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/b/c[&#42;/@title]</code> - Selects the <code>/a/b/c</code> node that contains any child with a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/b/c[d/e/@*name]</code> - Selects the <code>/a/b/c</code> node that contains a child <code>d</code> that
 * contains a child <code>e</code> on which is a new or changed property whose name ends with <code>name</code>.</li>
 * <li><code>/a/b/*[d/@title]</code> - Selects any node that is a child of the <code>/a/b</code> node where that node has a
 * child node named <code>d</code> on which is a new or changed <code>title</code> property.
 * <li><code>/a//[d/@title]</code> - Selects the node that is a decendant of <code>/a</code> and that is the parent of a node
 * named <code>d</code> on which is a new or changed <code>title</code> property.</li>
 * <li><code>/a/(b|c)[d/@title]</code> - Selects either the <code>/a/b</code> or <code>/a/c</code> node that contains a
 * node <code>d</code> on which is a new or changed <code>title</code> property.</li>
 * <li><code>/a/(b|c|)[d/@title]</code> - Selects either the <code>/a/b</code>, <code>/a/c</code> or <code>/a</code>
 * node <code>d</code> on which is a new or changed <code>title</code> property.</li>
 * </ul>
 * </p>
 * <h3>Selecting a node in a different location than the changed node</h3>
 * <p>
 * Some repositories contain data that is to be sequenced using structures that cannot hold the output of the sequencers. Or, it
 * is desirable that the sequenced output simply go in a different location than the nodes being sequenced. In these cases, the
 * path expression specify the criteria for sequencing as an absolute path, and the node to be sequenced as another absolute path.
 * Here are a few examples:
 * <ul>
 * <li><code>/a/b/c[/x/y/z/@title]</code> - Selects the <code>/a/b/c</code> node when the node <code>/x/y/z</code> has a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/b/c[&#42;/@title]</code> - Selects the <code>/a/b/c</code> node that contains any child with a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/b/c[d/e/@*name]</code> - Selects the <code>/a/b/c</code> node that contains a child <code>d</code> that
 * contains a child <code>e</code> on which is a new or changed property whose name ends with <code>name</code>.</li>
 * <li><code>/a/b/*[d/@title]</code> - Selects any node that is a child of the <code>/a/b</code> node where that node has a
 * child node named <code>d</code> on which is a new or changed <code>title</code> property.
 * <li><code>/a//[d/@title]</code> - Selects the node that is a decendant of <code>/a</code> and that is the parent of a node
 * named <code>d</code> on which is a new or changed <code>title</code> property.</li>
 * </ul>
 * </p>
 * <p>
 * This pattern becomes more powerful with the ability to capture portions of the criteria path and insert them into the select
 * path. This technique is similar to that of regular expressions: the portions to be captured are surrounded by matching
 * parenthesis, and they are used in the select by referencing the capture number preceded by a "$" character (e.g., "$1", "$2",
 * etc.). For example:
 * <ul>
 * <li><code>/a/$1[/x/(y/z)/@title]</code> - Selects the <code>/a/y/z</code> node when the node <code>/x/y/z</code> has a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/$1[(/x/y/z)/@title]</code> - Selects the <code>/a/x/y/z</code> node when the node <code>/x/y/z</code> has a
 * <code>title</code> property that has been created or changed.</li>
 * <li><code>/a/$1/$2/$3[/t/u/(v|w)/(x|y)/z/@title]</code> - Selects either the <code>/a/v/x</code>, <code>/a/w/x</code>,
 * <code>/a/v/y</code> or <code>/a/w/y</code> node when the <code>/t/u/v/x/z</code>, <code>/t/u/w/x/z</code>,
 * <code>/t/u/v/y/z</code> or <code>/t/u/w/y/z</code> nodes have a new or changed <code>title</code> property.</li>
 * <li><code>/a/$1[(/w//z)/@title]</code> - Selects the node below <code>/a</code> that has the same relative path as the
 * node named <code>z</code> with a <code>title</code> property that has been created or changed and below the <code>/w</code>
 * node. If the <code>/w/x/y/z</code> node is sequenced, the selected node will be <code>/a/w/x/y/z</code>.</li>
 * </ul>
 * </p>
 * <h3>Same-name sibling nodes</h3>
 * <p>
 * JCR makes it possible for sibling nodes to have the same name. Consider a repository with an existing node <code>/a/b/c</code>.
 * If another child of <code>/a/b</code> is created with the name <code>c</code>, then the pre-existing node will have a full
 * path of <code>/a/b/c[1]</code> and the new node will have a full path of <code>/a/b/c[2]</code>. (Note that this can cause
 * difficulty as the indexes are <em>changed</em> when nodes are inserted or deleted. For this reason, the JCR specification
 * often recommends against using same-name siblings.)
 * </p>
 * <p>
 * Sequencer path expressions are able to handle same-name siblings. If the sequencer doesn't care about same-name siblings or
 * indexes, then the path expressions don't have to be changed at all. Thus the path expression <code>/a/b/c[@title]</code>
 * implicitly will select the nodes regardless of whether they have indexes or what those indexes are.
 * </p>
 * <p>
 * If the same-name sibling indexes <em>are</em> important to the path expressions, then the indexes can be specified using a
 * variety of techniques. Adding "<code>[]</code>" to any segment says that the node may not have any same-name siblings
 * (e.g., there is no index), while adding "<code>[*]</code>" to any segment says that there must be an index, but that index
 * can have any value. A sequence of one or more indexes in the form "<code>[1,2,5,7]</code>" will ensure that the indexes
 * match one of those provided. Note that if "0" is included in the sequence, then a node without an index (i.e., a node without
 * same-name siblings) is allowed.
 * </p>
 * <p>
 * Here are some examples:
 * <ul>
 * <li><code>/a[]/b[*]/c[@title]</code> - Selects the <code>/a/b[n]/c</code> node where the <code>a</code> the node may not
 * have any same-name siblings, node <code>b</code> must have other same-name siblings, and node <code>c</code> may or may not
 * have same-name siblings, and where node <code>c</code> has a new or changed <code>title</code> property.</li>
 * <li><code>/a/b[1,2,4,7]/c[&#42;/@title]</code> - Selects either the <code>/a/b[1]/c</code>, <code>/a/b[2]/c</code>,
 * <code>/a/b[4]/c</code> or <code>/a/b[7]/c</code> node when it has a new or changed <code>title</code> property. The
 * <code>/a</code> and <code>c</code> nodes may or may not have same-name siblings and may have any index.</li>
 * <li><code>/a[]/b[0,1,2,4,7]/c[][&#42;/@title]</code> - Selects either the <code>/a/b/c</code>, <code>/a/b[1]/c</code>,
 * <code>/a/b[2]/c</code>, <code>/a/b[4]/c</code> or <code>/a/b[7]/c</code> node when it has a new or changed
 * <code>title</code> property. The <code>/a</code> and <code>c</code> nodes may not have same-name siblings.</li>
 * </ul>
 * </p>
 * <p>
 * Of course the same-name sibling criteria can be used with any other criteria or captures described above.
 * </p>
 * @author Randall Hauch
 */
@Immutable
public class SequencerPathExpression {

    /**
     * The regular expression for grabbing (as group 1) the node pattern and (as group 12) the match pattern, of the form
     * 
     * <pre>
     * pathToSelectNode[pathToMatch]
     * </pre>
     * 
     * Since square brackets ('<code>[</code>' and '<code>]</code>') are used to delimit the two patterns, the path
     * pattern and match pattern must each escape any square brackets that are used in the patterns except where .
     */
    // (([^:/\[\]\(\)\@]+:)?(((/(([^\:/\[]+:)?[^\:/\[]+(\[(\*|\d+(,\d+)*)?\])?))+/?))+/?)(\[(((([^:/\[\]\(\)\@]+)|(\([^:/\[\]\(\)\@]+\))):)?(((/?(([^\:/\[]+:)?[^\:/\[]+(\[(\*|\d+(,\d+)*)?\])?))+/?)|(\(((/(([^\:/\[]+:)?[^\:/\[]+(\[(\*|\d+(,\d+)*)?\])?))+/?)\)))+/?)\])?
    private static final String EXPRESSION_PATTERN_STRING =
        "(([^:/\\[\\]\\(\\)\\@]+:)?(((/(([^\\:/\\[]+:)?[^\\:/\\[]+(\\[(\\*|\\d+(,\\d+)*)?\\])?))+/?))+/?)(\\[(((([^:/\\[\\]\\(\\)\\@]+)|(\\([^:/\\[\\]\\(\\)\\@]+\\))):)?(((/?(([^\\:/\\[]+:)?[^\\:/\\[]+(\\[(\\*|\\d+(,\\d+)*)?\\])?))+/?)|(\\(((/(([^\\:/\\[]+:)?[^\\:/\\[]+(\\[(\\*|\\d+(,\\d+)*)?\\])?))+/?)\\)))+/?)\\])?";

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(EXPRESSION_PATTERN_STRING);

    private static final String SEQUENCE_PATTERN_STRING = "\\[(\\d+(,\\d+)*)\\]"; // \[(\d+(,\d+)*)\]
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile(SEQUENCE_PATTERN_STRING);

    private final String expression;
    private final String transformPattern;
    private final Pattern nodePattern;
    private final Pattern matchPattern;
    private final boolean matchPatternIsAbsolute;

    public SequencerPathExpression( String expression ) throws InvalidSequencerPathExpression {
        ArgCheck.isNotNull(expression, "sequencer path expression");
        this.expression = expression;
        expression = expression.trim();
        if (expression.length() == 0) {
            throw new InvalidSequencerPathExpression(ServicesI18n.pathExpressionMayNotBeBlank.text());
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new InvalidSequencerPathExpression(ServicesI18n.pathExpressionIsInvalid.text(expression));
        }
        String nodePatternPart = matcher.group(1);
        assert nodePatternPart != null;
        if (!nodePatternPart.endsWith("/")) nodePatternPart = nodePatternPart + "/";
        nodePatternPart = replacePatterns(nodePatternPart);
        transformPattern = removePatterns(nodePatternPart);
        nodePatternPart = nodePatternPart.replaceAll("(\\$[1-9][0-9]*)", "\\\\$1");
        nodePattern = Pattern.compile("(" + nodePatternPart + ").*", Pattern.CASE_INSENSITIVE); // add a group 1

        String matchPatternPart = matcher.group(12);
        if (matchPatternPart != null) {
            String matchPatternPartWithoutRepository = matcher.group(17);
            matchPatternIsAbsolute = matchPatternPartWithoutRepository.matches("^[\\(]*/.*");
            if (!matchPatternPart.endsWith("/")) matchPatternPart = matchPatternPart + "/";
            matchPatternPart = replacePatterns(matchPatternPart);
            if (matchPatternIsAbsolute == false) {
                matchPatternPart = nodePatternPart + matchPatternPart;
            }
            matchPattern = Pattern.compile(matchPatternPart, Pattern.CASE_INSENSITIVE);
        } else {
            matchPattern = nodePattern;
            matchPatternIsAbsolute = true;
        }
    }

    protected static String replacePatterns( String input ) {
        // replace 3 or more sequential '|' characters in an OR expression
        input = input.replaceAll("[\\|]{2,}", "|");
        // if there is an empty expression in an OR expression, make the whole segment optional ...
        // (e.g., "/a/b/(c|)/d" => "a/b(/(c))?/d"
        input = input.replaceAll("/(\\([^|]+)(\\|){2,}([^)]+\\))/", "(/$1$2$3)?/");
        input = input.replaceAll("/\\(\\|+([^)]+)\\)/", "(/(|$1))?/");
        input = input.replaceAll("/\\(([^|]+)\\|+\\)/", "(/($1|))?/");

        // Allow any path (that doesn't contain an explicit counter) to contain a counter,
        // done by replacing any '/' or '|' that isn't preceded by ']' or '*' or '/' or '(' with '(\[\d+\])?/'...
        input = input.replaceAll("(?<=[^\\]\\*/(])([/|])", "(?:\\\\[\\\\d+\\\\])?$1");

        // Remove any uses of '\[\]', since it means that no explicit sibling indexes are allowed ...
        input = input.replaceAll("\\\\\\[\\\\\\]", "");

        // Does the path contain any '[*]' or '[n]' (where n is any positive integers)...
        // '[*]/' => '(\[\d+\])?/'
        input = input.replaceAll("\\[[*]\\]/", "\\\\[\\\\d+\\\\]/"); // index is optional
        // '[0]/' => '(\[0\])?/'
        input = input.replaceAll("\\[0\\]/", "(?:\\\\[0\\\\])?/"); // index is optional
        // '[n]/' => '\[n\]/'
        input = input.replaceAll("\\[([1-9]\\d*)\\]/", "\\\\[$1\\\\]/"); // index is required
        // '[]/' => ''
        input = input.replaceAll("\\[\\]/", "/"); // index is not allowed

        // Replace all '[n,m,o,p]' type sequences with '[(n|m|o|p)]'
        Matcher matcher = SEQUENCE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        boolean result = matcher.find();
        if (result) {
            do {
                String sequenceStr = matcher.group(1).replaceAll(",", "|");
                String replacement = "\\\\[(?:" + sequenceStr + ")\\\\]";
                if (sequenceStr.startsWith("0|") || sequenceStr.contains("|0|") || sequenceStr.endsWith("|0")) {
                    replacement = "(?:" + replacement + ")?";
                }
                matcher.appendReplacement(sb, replacement);
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            input = sb.toString();
        }

        // Order is important here
        input = input.replaceAll("[*]", "[^/]*");
        input = input.replaceAll("//", "(/[^/]*)*/");
        return input;
    }

    protected static String removePatterns( String input ) {
        input = input.replaceAll("\\(\\?:\\\\\\[\\\\d\\+\\\\\\]\\)\\?/", "/");
        return input;
    }

    /**
     * @return expression
     */
    public String getExpression() {
        return this.expression;
    }

    public String matches( String absolutePath ) {
        String originalAbsolutePath = absolutePath;
        if (!absolutePath.endsWith("/")) absolutePath = absolutePath + "/";
        // See if the supplied absolute path matches the pattern ...
        Pattern pattern = this.matchPattern;
        assert pattern != null;
        final Matcher matcher = pattern.matcher(absolutePath);
        if (!matcher.matches()) {
            return null;
        }
        String result = null;
        if (matchPattern == nodePattern) {
            // Return the original path ...
            result = originalAbsolutePath;
        }
        if (result == null) {
            // It does match, so return the node part ...
            Matcher nodeMatcher = this.nodePattern.matcher(absolutePath);
            if (nodeMatcher.matches()) {
                result = nodeMatcher.group(1); // we added group 1 to the node pattern in the constructor
            }
        }
        // The path doesn't match the node selection...
        if (result == null && matchPatternIsAbsolute) {
            // The match pattern is absolute, so use the transformation path to get the result ...
            result = this.transformPattern;
            // Removing any groups used to allow any index ...
            if (this.transformPattern.indexOf('$') != -1) {
                result = matcher.replaceAll(this.transformPattern);
            }
        }

        // Remove all trailing '/' ...
        if (result != null) result = result.replaceAll("/+$", "");
        return result;
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
        if (obj instanceof SequencerPathExpression) {
            SequencerPathExpression that = (SequencerPathExpression)obj;
            if (this.expression.equals(that.expression)) return true;
            return false;
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

}
