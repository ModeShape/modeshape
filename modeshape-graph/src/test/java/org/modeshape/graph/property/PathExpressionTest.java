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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class PathExpressionTest {

    private PathExpression expr;

    @Before
    public void beforeEach() throws Exception {
        expr = new PathExpression(".*");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCompileNullExpression() {
        PathExpression.compile(null);
    }

    @Test( expected = InvalidPathExpressionException.class )
    public void shouldNotCompileZeroLengthExpression() {
        PathExpression.compile("");
    }

    @Test( expected = InvalidPathExpressionException.class )
    public void shouldNotCompileBlankExpression() {
        PathExpression.compile("    ");
    }

    @Test
    public void shouldCompileExpressionWithAnyNode() {
        expr = PathExpression.compile("/*");
        assertThat(expr.getSelectExpression(), is("/*"));
        assertThat(expr.matcher("/a").matches(), is(true));
        assertThat(expr.matcher("/a").getInputPath(), is("/a"));
        assertThat(expr.matcher("/a").getSelectedNodePath(), is("/a"));
        assertThat(expr.matcher("/a").groupCount(), is(0));
    }

    @Test
    public void shouldCompileExpressionWithAnySequenceOfNodes() {
        expr = PathExpression.compile("//");
        assertThat(expr.getSelectExpression(), is("//"));
        assertThat(expr.matcher("/a").matches(), is(true));
        assertThat(expr.matcher("/a").getInputPath(), is("/a"));
        assertThat(expr.matcher("/a").getSelectedNodePath(), is("/a"));
        assertThat(expr.matcher("/a").groupCount(), is(0));

        assertThat(expr.matcher("/a/b").matches(), is(true));
        assertThat(expr.matcher("/a/b").getInputPath(), is("/a/b"));
        assertThat(expr.matcher("/a/b").getSelectedNodePath(), is("/a/b"));
        assertThat(expr.matcher("/a/b").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c").matches(), is(true));
        assertThat(expr.matcher("/a/b/c").getInputPath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").groupCount(), is(0));
    }

    @Test
    public void shouldCompileExpressionWithExtraWhitespace() {
        expr = PathExpression.compile(" /a/b/c ");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));

        expr = PathExpression.compile("  /a/b/c ");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
    }

    @Test
    public void shouldCompileExpressionWithIndexes() {
        assertThat(PathExpression.compile("/a/b[0]/c[1]/d/e"), is(notNullValue()));
        assertThat(PathExpression.compile("/a/b[0]/c[1]/d/e[2]"), is(notNullValue()));
    }

    @Test
    public void shouldCompileExpressionWithRepositoryAndWorkspaceNames() {
        assertThat(PathExpression.compile("repo:ws:/a/b/c"), is(notNullValue()));
    }

    @Test
    public void shouldCompileExpressionWithRepositoryAndNoWorkspaceNames() {
        assertThat(PathExpression.compile("repo::/a/b/c"), is(notNullValue()));
    }

    @Test
    public void shouldCompileExpressionWithNoRepositoryAndNoWorkspaceNames() {
        assertThat(PathExpression.compile("::/a/b/c"), is(notNullValue()));
    }

    @Test
    public void shouldNotRemoveUsedPredicates() {
        assertThat(expr.removeUnusedPredicates("/a/b/c"), is("/a/b/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[0]/c"), is("/a/b[0]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[1]/c"), is("/a/b[1]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[10]/c"), is("/a/b[10]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[100]/c"), is("/a/b[100]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[1000]/c"), is("/a/b[1000]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[]/c"), is("/a/b[]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[*]/c"), is("/a/b[*]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[1,2]/c"), is("/a/b[1,2]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[1,2,3,4,5]/c"), is("/a/b[1,2,3,4,5]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b/c[@title]"), is("/a/b/c[@title]"));
        assertThat(expr.removeUnusedPredicates("/a/b/c[d/e/@title]"), is("/a/b/c[d/e/@title]"));
        assertThat(expr.removeUnusedPredicates("/a/(b/c)[(d|e)/(f|g)/@something]"), is("/a/(b/c)[(d|e)/(f|g)/@something]"));
        assertThat(expr.removeUnusedPredicates("/*"), is("/*"));
        assertThat(expr.removeUnusedPredicates("/*[]"), is("/*[]"));
        assertThat(expr.removeUnusedPredicates("/*[3]"), is("/*[3]"));
        // These are legal, but aren't really useful ...
        assertThat(expr.removeUnusedPredicates("/a/b[1][2][3]/c"), is("/a/b[1][2][3]/c"));
    }

    @Test
    public void shouldRemoveUnusedPredicates() {
        assertThat(expr.removeUnusedPredicates("/a/b[-1]/c"), is("/a/b/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[@name='wacky']/c"), is("/a/b/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[3][@name='wacky']/c"), is("/a/b[3]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[3][@name]/c"), is("/a/b[3]/c"));
        assertThat(expr.removeUnusedPredicates("/a/b[length(@name)=3]/c"), is("/a/b/c"));
    }

    @Test
    public void shouldRemoveAllPredicates() {
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b/c"), is("/a/b/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[0]/c"), is("/a/b[0]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[1]/c"), is("/a/b[1]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[10]/c"), is("/a/b[10]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[100]/c"), is("/a/b[100]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[1000]/c"), is("/a/b[1000]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[]/c"), is("/a/b[]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[*]/c"), is("/a/b[*]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b/c[@title]"), is("/a/b/c"));
        // These are legal, but aren't really useful ...
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[1][2][3]/c"), is("/a/b[1][2][3]/c"));

        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[-1]/c"), is("/a/b/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[@name='wacky']/c"), is("/a/b/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[3][@name='wacky']/c"), is("/a/b[3]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[3][@name]/c"), is("/a/b[3]/c"));
        assertThat(expr.removeAllPredicatesExceptIndexes("/a/b[length(@name)=3]/c"), is("/a/b/c"));
    }

    @Test
    public void shouldReplaceAllXPathPatterns() {
        assertThat(expr.replaceXPathPatterns("/a/b[3]/c"), is("/a/b\\[3\\]/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[*]/c"), is("/a/b(?:\\[\\d+\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[]/c"), is("/a/b(?:\\[\\d+\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[0]/c"), is("/a/b(?:\\[0\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[0,1,2,4]/c"), is("/a/b(?:\\[(?:1|2|4)\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[1,2,4,0]/c"), is("/a/b(?:\\[(?:1|2|4)\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[1,2,0,4]/c"), is("/a/b(?:\\[(?:1|2|4)\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[0,1,2,0,4,0]/c"), is("/a/b(?:\\[(?:1|2|4)\\])?/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[1,2,4]/c"), is("/a/b\\[(?:1|2|4)\\]/c"));
        assertThat(expr.replaceXPathPatterns("/a/b[@param]"), is("/a/b/@param"));
        assertThat(expr.replaceXPathPatterns("/a/b[3][@param]"), is("/a/b\\[3\\]/@param"));
        assertThat(expr.replaceXPathPatterns("/a/b[c/d/@param]"), is("/a/b/c/d/@param"));

        assertThat(expr.replaceXPathPatterns("/a/(b|c|d)/e"), is("/a/(b|c|d)/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b||c|d)/e"), is("/a/(b|c|d)/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b|||c|d)/e"), is("/a/(b|c|d)/e"));
        assertThat(expr.replaceXPathPatterns("/a/(|b|c|d)/e"), is("/a(?:/(b|c|d))?/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b|c|d|)/e"), is("/a(?:/(b|c|d))?/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b|c|d)[]/e"), is("/a/(b|c|d)(?:\\[\\d+\\])?/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b|c[2]|d[])/e"), is("/a/(b|c\\[2\\]|d(?:\\[\\d+\\])?)/e"));
        assertThat(expr.replaceXPathPatterns("/a/(b|c/d|e)/f"), is("/a/(b|c/d|e)/f"));
        assertThat(expr.replaceXPathPatterns("/a/(b/c)[(d|e)/(f|g)/@something]"), is("/a/(b/c)/(d|e)/(f|g)/@something"));

        assertThat(expr.replaceXPathPatterns("/a/*/f"), is("/a/[^/]*/f"));
        assertThat(expr.replaceXPathPatterns("/a//f"), is("/a(?:/[^/]*)*/f"));
        assertThat(expr.replaceXPathPatterns("/a///f"), is("/a(?:/[^/]*)*/f"));
        assertThat(expr.replaceXPathPatterns("/a/////f"), is("/a(?:/[^/]*)*/f"));

        assertThat(expr.replaceXPathPatterns("/*"), is("/[^/]*"));
        assertThat(expr.replaceXPathPatterns("/*[]"), is("/[^/]*(?:\\[\\d+\\])?"));
        assertThat(expr.replaceXPathPatterns("/*[3]"), is("/[^/]*\\[3\\]"));
    }

    @Test
    public void shouldDetermineIfPatternMatchesAnything() {
        assertThat(PathExpression.compile("/.").matchesAnything(), is(true));
        assertThat(PathExpression.compile("//").matchesAnything(), is(true));
        assertThat(PathExpression.compile("///").matchesAnything(), is(true));
        assertThat(PathExpression.compile("///").matchesAnything(), is(true));
        assertThat(PathExpression.compile("/*").matchesAnything(), is(true));
        assertThat(PathExpression.compile("*").matchesAnything(), is(true));
        assertThat(PathExpression.compile("*[*]").matchesAnything(), is(true));
        assertThat(PathExpression.compile("*[]").matchesAnything(), is(true));

        assertThat(PathExpression.compile("/a").matchesAnything(), is(false));
        assertThat(PathExpression.compile("/*[3]").matchesAnything(), is(false));
        assertThat(PathExpression.compile("/a/b/c").matchesAnything(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithoutRegardToCase() {
        expr = PathExpression.compile("/a/b/c/d/e[@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c/d/e"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/E/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/E/@something").getInputPath(), is("/a/b/c/d/E/@something"));
        assertThat(expr.matcher("/a/b/c/d/E/@something").getSelectedNodePath(), is("/a/b/c/d/E"));
        assertThat(expr.matcher("/a/b/c/d/E/@something").groupCount(), is(0));
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPath() {
        expr = PathExpression.compile("/a/b/c/d/e[@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c/d/e"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/E/@something2").matches(), is(false));
        assertThat(expr.matcher("/a/b/c/d/ex/@something").matches(), is(false));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPathAndExtraPathInsideMatch() {
        expr = PathExpression.compile("/a/b/c[d/e/@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/E/@something2").matches(), is(false));
        assertThat(expr.matcher("/a/b/c/d/ex/@something").matches(), is(false));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithWildcardSelection() {
        expr = PathExpression.compile("/a/*/c[d/e/@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getInputPath(), is("/a/b[2]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getSelectedNodePath(), is("/a/b[2]/c"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/rt/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").getInputPath(), is("/a/rt/c/d/e/@something"));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").getSelectedNodePath(), is("/a/rt/c"));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/ac/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a/b/b2/b3/d/e/@something").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithFilenameLikeWildcardSelection() {
        expr = PathExpression.compile("/a/*.txt[@something]");
        assertThat(expr.matcher("/a/b.txt/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b.txt/@something").getInputPath(), is("/a/b.txt/@something"));
        assertThat(expr.matcher("/a/b.txt/@something").getSelectedNodePath(), is("/a/b.txt"));
        assertThat(expr.matcher("/a/b.txt/@something").groupCount(), is(0));
        assertThat(expr.matcher("/a/b.tx/@something").matches(), is(false));

        expr = PathExpression.compile("/a/*.txt/c[@something]");
        assertThat(expr.matcher("/a/b.txt/c/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b.txt/c/@something").getInputPath(), is("/a/b.txt/c/@something"));
        assertThat(expr.matcher("/a/b.txt/c/@something").getSelectedNodePath(), is("/a/b.txt/c"));
        assertThat(expr.matcher("/a/b.txt/c/@something").groupCount(), is(0));
        assertThat(expr.matcher("/a/b.tx/c/@something").matches(), is(false));

        expr = PathExpression.compile("//*.txt[*]/c[@something]");
        assertThat(expr.matcher("/a/b.txt/c/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b.txt/c/@something").getInputPath(), is("/a/b.txt/c/@something"));
        assertThat(expr.matcher("/a/b.txt/c/@something").getSelectedNodePath(), is("/a/b.txt/c"));
        assertThat(expr.matcher("/a/b.txt/c/@something").groupCount(), is(0));
        assertThat(expr.matcher("/a/b.tx/c/@something").matches(), is(false));

        assertThat(expr.matcher("/z/a/b.txt/c/@something").matches(), is(true));
        assertThat(expr.matcher("/z/a/b.txt/c/@something").getInputPath(), is("/z/a/b.txt/c/@something"));
        assertThat(expr.matcher("/z/a/b.txt/c/@something").getSelectedNodePath(), is("/z/a/b.txt/c"));
        assertThat(expr.matcher("/z/a/b.txt/c/@something").groupCount(), is(0));
    }

    @Test
    public void shouldMatchExpressionsWithSegmentWildcardSelection() {
        expr = PathExpression.compile("/a//c[d/e/@something]");
        assertThat(expr.matcher("/a/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/c/d/e/@something").getInputPath(), is("/a/c/d/e/@something"));
        assertThat(expr.matcher("/a/c/d/e/@something").getSelectedNodePath(), is("/a/c"));
        assertThat(expr.matcher("/a/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getInputPath(), is("/a/b[2]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getSelectedNodePath(), is("/a/b[2]/c"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/rt/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").getInputPath(), is("/a/rt/c/d/e/@something"));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").getSelectedNodePath(), is("/a/rt/c"));
        assertThat(expr.matcher("/a/rt/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/r/s/t/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/r/s/t/c/d/e/@something").getInputPath(), is("/a/r/s/t/c/d/e/@something"));
        assertThat(expr.matcher("/a/r/s/t/c/d/e/@something").getSelectedNodePath(), is("/a/r/s/t/c"));
        assertThat(expr.matcher("/a/r/s/t/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/r[1]/s[2]/t[33]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/r[1]/s[2]/t[33]/c/d/e/@something").getInputPath(), is("/a/r[1]/s[2]/t[33]/c/d/e/@something"));
        assertThat(expr.matcher("/a/r[1]/s[2]/t[33]/c/d/e/@something").getSelectedNodePath(), is("/a/r[1]/s[2]/t[33]/c"));
        assertThat(expr.matcher("/a/r[1]/s[2]/t[33]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a[3]/c/d/e/@something").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithSegmentWildcardAtEnd() {
        expr = PathExpression.compile("/a/b/c//");
        assertThat(expr.matcher("/a/b/c").matches(), is(true));
        assertThat(expr.matcher("/a/b/c").getInputPath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d").getInputPath(), is("/a/b/c/d"));
        assertThat(expr.matcher("/a/b/c/d").getSelectedNodePath(), is("/a/b/c/d"));
        assertThat(expr.matcher("/a/b/c/d").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/a").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/a").getInputPath(), is("/a/b/c/a"));
        assertThat(expr.matcher("/a/b/c/a").getSelectedNodePath(), is("/a/b/c/a"));
        assertThat(expr.matcher("/a/b/c/a").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/e/f/g/h").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/f/g/h").getInputPath(), is("/a/b/c/d/e/f/g/h"));
        assertThat(expr.matcher("/a/b/c/d/e/f/g/h").getSelectedNodePath(), is("/a/b/c/d/e/f/g/h"));
        assertThat(expr.matcher("/a/b/c/d/e/f/g/h").groupCount(), is(0));

        assertThat(expr.matcher("/a/b").matches(), is(false));
        assertThat(expr.matcher("/a/b/d").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithExtraLargeSegmentWildcardAtEnd() {
        expr = PathExpression.compile("/a/b/c////");
        assertThat(expr.matcher("/a/b/c").matches(), is(true));
        assertThat(expr.matcher("/a/b/c").getInputPath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c").groupCount(), is(0));

    }

    @Test
    public void shouldMatchExpressionsWithIndexesInSelectionPaths() {
        expr = PathExpression.compile("/a/b[2,3,4,5]/c/d/e[@something]");
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getInputPath(), is("/a/b[2]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getSelectedNodePath(), is("/a/b[2]/c/d/e"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getInputPath(), is("/a/b[3]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getSelectedNodePath(), is("/a/b[3]/c/d/e"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getInputPath(), is("/a/b[4]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getSelectedNodePath(), is("/a/b[4]/c/d/e"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getInputPath(), is("/a/b[5]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getSelectedNodePath(), is("/a/b[5]/c/d/e"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a[1]/b/c/d/e/@something").matches(), is(false));

        expr = PathExpression.compile("/a/b[0,2,3,4,5]/c/d/e[@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c/d/e"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getInputPath(), is("/a/b[2]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getSelectedNodePath(), is("/a/b[2]/c/d/e"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getInputPath(), is("/a/b[3]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getSelectedNodePath(), is("/a/b[3]/c/d/e"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getInputPath(), is("/a/b[4]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getSelectedNodePath(), is("/a/b[4]/c/d/e"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getInputPath(), is("/a/b[5]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getSelectedNodePath(), is("/a/b[5]/c/d/e"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[0]/c/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").matches(), is(false));
        assertThat(expr.matcher("/a[1]/b/c/d/e/@something").matches(), is(false));
    }

    @Test
    public void shouldMatchExpressionsWithAnyIndexesInSelectionPaths() {
        expr = PathExpression.compile("/a/b[*]/c[]/d/e[@something]");
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getInputPath(), is("/a/b[2]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").getSelectedNodePath(), is("/a/b[2]/c/d/e"));
        assertThat(expr.matcher("/a/b[2]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getInputPath(), is("/a/b[3]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").getSelectedNodePath(), is("/a/b[3]/c/d/e"));
        assertThat(expr.matcher("/a/b[3]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getInputPath(), is("/a/b[4]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").getSelectedNodePath(), is("/a/b[4]/c/d/e"));
        assertThat(expr.matcher("/a/b[4]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getInputPath(), is("/a/b[5]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").getSelectedNodePath(), is("/a/b[5]/c/d/e"));
        assertThat(expr.matcher("/a/b[5]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").getInputPath(), is("/a/b[1]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").getSelectedNodePath(), is("/a/b[1]/c/d/e"));
        assertThat(expr.matcher("/a/b[1]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[6]/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[6]/c/d/e/@something").getInputPath(), is("/a/b[6]/c/d/e/@something"));
        assertThat(expr.matcher("/a/b[6]/c/d/e/@something").getSelectedNodePath(), is("/a/b[6]/c/d/e"));
        assertThat(expr.matcher("/a/b[6]/c/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b[6]/c[1]/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b[6]/c[1]/d/e/@something").getInputPath(), is("/a/b[6]/c[1]/d/e/@something"));
        assertThat(expr.matcher("/a/b[6]/c[1]/d/e/@something").getSelectedNodePath(), is("/a/b[6]/c[1]/d/e"));
        assertThat(expr.matcher("/a/b[6]/c[1]/d/e/@something").groupCount(), is(0));

        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c/d/e"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(0));
    }

    @Test
    public void shouldMatchExpressionsWithRepositoryInSelectionPath() {
        expr = PathExpression.compile("reposA::/a/b/c[d/e/@something]");
        assertThat(expr.matcher("reposA::/a/b/c/d/e/@something").matches(), is(true));
    }

    @Test
    public void shouldMatchExpressionsWithNamedGroups() {
        expr = PathExpression.compile("/a(//c)[d/e/@something]");
        assertThat(expr.matcher("/a/b/c/d/e/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getInputPath(), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").groupCount(), is(1));
        assertThat(expr.matcher("/a/b/c/d/e/@something").group(0), is("/a/b/c/d/e/@something"));
        assertThat(expr.matcher("/a/b/c/d/e/@something").group(1), is("/b/c"));

        expr = PathExpression.compile("/a(/(b|c|d|)/e)[f/g/@something]");
        assertThat(expr.matcher("/a/b/e/f/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/e/f/g/@something").getInputPath(), is("/a/b/e/f/g/@something"));
        assertThat(expr.matcher("/a/b/e/f/g/@something").getSelectedNodePath(), is("/a/b/e"));
        assertThat(expr.matcher("/a/b/e/f/g/@something").groupCount(), is(2));
        assertThat(expr.matcher("/a/b/e/f/g/@something").group(0), is("/a/b/e/f/g/@something"));
        assertThat(expr.matcher("/a/b/e/f/g/@something").group(1), is("/b/e"));
        assertThat(expr.matcher("/a/b/e/f/g/@something").group(2), is("b"));

        assertThat(expr.matcher("/a/c/e/f/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/c/e/f/g/@something").getInputPath(), is("/a/c/e/f/g/@something"));
        assertThat(expr.matcher("/a/c/e/f/g/@something").getSelectedNodePath(), is("/a/c/e"));
        assertThat(expr.matcher("/a/c/e/f/g/@something").groupCount(), is(2));
        assertThat(expr.matcher("/a/c/e/f/g/@something").group(0), is("/a/c/e/f/g/@something"));
        assertThat(expr.matcher("/a/c/e/f/g/@something").group(1), is("/c/e"));
        assertThat(expr.matcher("/a/c/e/f/g/@something").group(2), is("c"));

        assertThat(expr.matcher("/a/d/e/f/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/d/e/f/g/@something").getInputPath(), is("/a/d/e/f/g/@something"));
        assertThat(expr.matcher("/a/d/e/f/g/@something").getSelectedNodePath(), is("/a/d/e"));
        assertThat(expr.matcher("/a/d/e/f/g/@something").groupCount(), is(2));
        assertThat(expr.matcher("/a/d/e/f/g/@something").group(0), is("/a/d/e/f/g/@something"));
        assertThat(expr.matcher("/a/d/e/f/g/@something").group(1), is("/d/e"));
        assertThat(expr.matcher("/a/d/e/f/g/@something").group(2), is("d"));

        assertThat(expr.matcher("/a/e/f/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/e/f/g/@something").getInputPath(), is("/a/e/f/g/@something"));
        assertThat(expr.matcher("/a/e/f/g/@something").getSelectedNodePath(), is("/a/e"));
        assertThat(expr.matcher("/a/e/f/g/@something").groupCount(), is(2));
        assertThat(expr.matcher("/a/e/f/g/@something").group(0), is("/a/e/f/g/@something"));
        assertThat(expr.matcher("/a/e/f/g/@something").group(1), is("/e"));
        assertThat(expr.matcher("/a/e/f/g/@something").group(2), is(nullValue()));

        assertThat(expr.matcher("/a/t/e/f/g/@something").matches(), is(false));

        expr = PathExpression.compile("/a/(b/c)[(d|e)/(f|g)/@something]");
        assertThat(expr.matcher("/a/b/c/d/f/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/f/@something").getInputPath(), is("/a/b/c/d/f/@something"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").groupCount(), is(3));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(0), is("/a/b/c/d/f/@something"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(1), is("b/c"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(2), is("d"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(3), is("f"));

        assertThat(expr.matcher("/a/b/c/e/f/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/e/f/@something").getInputPath(), is("/a/b/c/e/f/@something"));
        assertThat(expr.matcher("/a/b/c/e/f/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/e/f/@something").groupCount(), is(3));
        assertThat(expr.matcher("/a/b/c/e/f/@something").group(0), is("/a/b/c/e/f/@something"));
        assertThat(expr.matcher("/a/b/c/e/f/@something").group(1), is("b/c"));
        assertThat(expr.matcher("/a/b/c/e/f/@something").group(2), is("e"));
        assertThat(expr.matcher("/a/b/c/e/f/@something").group(3), is("f"));

        assertThat(expr.matcher("/a/b/c/d/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/g/@something").getInputPath(), is("/a/b/c/d/g/@something"));
        assertThat(expr.matcher("/a/b/c/d/g/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/d/g/@something").groupCount(), is(3));
        assertThat(expr.matcher("/a/b/c/d/g/@something").group(0), is("/a/b/c/d/g/@something"));
        assertThat(expr.matcher("/a/b/c/d/g/@something").group(1), is("b/c"));
        assertThat(expr.matcher("/a/b/c/d/g/@something").group(2), is("d"));
        assertThat(expr.matcher("/a/b/c/d/g/@something").group(3), is("g"));

        assertThat(expr.matcher("/a/b/c/e/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/e/g/@something").getInputPath(), is("/a/b/c/e/g/@something"));
        assertThat(expr.matcher("/a/b/c/e/g/@something").getSelectedNodePath(), is("/a/b/c"));
        assertThat(expr.matcher("/a/b/c/e/g/@something").groupCount(), is(3));
        assertThat(expr.matcher("/a/b/c/e/g/@something").group(0), is("/a/b/c/e/g/@something"));
        assertThat(expr.matcher("/a/b/c/e/g/@something").group(1), is("b/c"));
        assertThat(expr.matcher("/a/b/c/e/g/@something").group(2), is("e"));
        assertThat(expr.matcher("/a/b/c/e/g/@something").group(3), is("g"));

        expr = PathExpression.compile("/a/(b/c)/(d|e)/(f|g)/@something");
        assertThat(expr.matcher("/a/b/c/d/f/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/f/@something").getInputPath(), is("/a/b/c/d/f/@something"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").getSelectedNodePath(), is("/a/b/c/d/f"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").groupCount(), is(3));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(0), is("/a/b/c/d/f/@something"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(1), is("b/c"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(2), is("d"));
        assertThat(expr.matcher("/a/b/c/d/f/@something").group(3), is("f"));

        assertThat(expr.matcher("/a/b/c/e/f/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/d/g/@something").matches(), is(true));
        assertThat(expr.matcher("/a/b/c/e/g/@something").matches(), is(true));
    }

    @Test
    public void shouldMatchExpressionWithFilenamePatternAndChildProperty() {
        expr = PathExpression.compile("//(*.(jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd))[*]/jcr:content[@jcr:data]");
        assertThat(expr.matcher("/a/b/caution.png/jcr:content/@jcr:data").matches(), is(true));
    }

    @Test
    public void shouldMatchStringWithRepositoryAndWorkspaceUsingExpressionWithoutRepositoryOrWorkspace() {
        expr = PathExpression.compile("//a/b");
        assertThat(expr.matcher("repo:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/x/y/a/b").matches(), is(true));
    }

    @Test
    public void shouldMatchStringWithRepositoryAndWorkspaceUsingExpressionWithBlankRepositoryAndBlankWorkspace() {
        expr = PathExpression.compile(":://a/b");
        assertThat(expr.matcher("repo:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/x/y/a/b").matches(), is(true));
    }

    @Test
    public void shouldMatchStringWithRepositoryAndWorkspaceUsingExpressionWithRepositoryAndBlankWorkspace() {
        expr = PathExpression.compile("repo:://a/b");
        assertThat(expr.matcher("repo:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace2:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace2:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/x/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/x/y/a/b").matches(), is(false));
    }

    @Test
    public void shouldMatchStringWithRepositoryAndWorkspaceUsingExpressionWithRepositoryAndWorkspace() {
        expr = PathExpression.compile("repo:workspace://a/b");
        assertThat(expr.matcher("repo:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/a/b").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/x/a/b").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/x/y/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/x/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace2:/x/y/a/b").matches(), is(false));
    }

    @Test
    public void shouldMatchStringWithRepositoryAndWorkspaceUsingExpressionWithBlankRepositoryAndWorkspace() {
        expr = PathExpression.compile(":workspace://a/b");
        assertThat(expr.matcher("repo:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace:/x/y/a/b").matches(), is(true));
        assertThat(expr.matcher("repo:workspace2:/a").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/a/b").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/x/a/b").matches(), is(false));
        assertThat(expr.matcher("repo:workspace2:/x/y/a/b").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace:/a").matches(), is(false));
        assertThat(expr.matcher("repo1:workspace:/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace:/x/a/b").matches(), is(true));
        assertThat(expr.matcher("repo1:workspace:/x/y/a/b").matches(), is(true));
    }
}
