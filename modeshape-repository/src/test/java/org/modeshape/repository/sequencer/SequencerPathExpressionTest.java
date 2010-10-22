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
package org.modeshape.repository.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.property.PathExpression;

/**
 * @author Randall Hauch
 */
public class SequencerPathExpressionTest {

    private SequencerPathExpression expr;

    @Before
    public void beforeEach() throws Exception {
        expr = new SequencerPathExpression(new PathExpression(".*"), "/output");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCompileNullExpression() {
        SequencerPathExpression.compile(null);
    }

    @Test( expected = InvalidSequencerPathExpression.class )
    public void shouldNotCompileZeroLengthExpression() {
        SequencerPathExpression.compile("");
    }

    @Test( expected = InvalidSequencerPathExpression.class )
    public void shouldNotCompileBlankExpression() {
        SequencerPathExpression.compile("    ");
    }

    @Test
    public void shouldCompileExpressionWithOnlySelectionExpression() {
        expr = SequencerPathExpression.compile("/a/b/c");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
        assertThat(expr.getOutputExpression(), is(SequencerPathExpression.DEFAULT_OUTPUT_EXPRESSION));
    }

    @Test( expected = InvalidSequencerPathExpression.class )
    public void shouldNotCompileExpressionWithSelectionExpressionAndDelimiterAndNoOutputExpression() {
        SequencerPathExpression.compile("/a/b/c=>");
    }

    @Test
    public void shouldCompileExpressionWithSelectionExpressionAndDelimiterAndOutputExpression() {
        expr = SequencerPathExpression.compile("/a/b/c=>.");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
        assertThat(expr.getOutputExpression(), is("."));

        expr = SequencerPathExpression.compile("/a/b/c=>/x/y");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
        assertThat(expr.getOutputExpression(), is("/x/y"));
    }

    @Test
    public void shouldCompileExpressionWithExtraWhitespace() {
        expr = SequencerPathExpression.compile(" /a/b/c => . ");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
        assertThat(expr.getOutputExpression(), is("."));

        expr = SequencerPathExpression.compile("  /a/b/c => /x/y ");
        assertThat(expr, is(notNullValue()));
        assertThat(expr.getSelectExpression(), is("/a/b/c"));
        assertThat(expr.getOutputExpression(), is("/x/y"));
    }

    @Test
    public void shouldCompileExpressionWithIndexes() {
        assertThat(SequencerPathExpression.compile("/a/b[0]/c[1]/d/e"), is(notNullValue()));
        assertThat(SequencerPathExpression.compile("/a/b[0]/c[1]/d/e[2]"), is(notNullValue()));
    }

    protected void assertNotMatches( SequencerPathExpression.Matcher matcher ) {
        assertThat(matcher, is(notNullValue()));
        assertThat(matcher.getSelectedPath(), is(nullValue()));
        assertThat(matcher.getOutputPath(), is(nullValue()));
        assertThat(matcher.matches(), is(false));
    }

    protected void assertMatches( SequencerPathExpression.Matcher matcher,
                                  String selectedPath,
                                  String outputPath ) {
        assertMatches(matcher, selectedPath, null, null, outputPath);
    }

    protected void assertMatches( SequencerPathExpression.Matcher matcher,
                                  String selectedPath,
                                  String outputRepository,
                                  String outputWorkspace,
                                  String outputPath ) {
        assertThat(matcher, is(notNullValue()));
        assertThat(matcher.getSelectedPath(), is(selectedPath));
        assertThat(matcher.getOutputPath(), is(outputPath));
        assertThat(matcher.getOutputRepositoryName(), is(outputRepository));
        assertThat(matcher.getOutputWorkspaceName(), is(outputWorkspace));
        if (selectedPath == null) {
            assertThat(matcher.matches(), is(false));
        } else {
            assertThat(matcher.matches(), is(true));
        }
    }

    @Test
    public void shouldMatchExpressionsWithoutRegardToCase() {
        expr = SequencerPathExpression.compile("/a/b/c/d/e[@something] => .");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c/d/e", "/a/b/c/d/e");
        assertMatches(expr.matcher("/a/b/c/d/E/@something"), "/a/b/c/d/E", "/a/b/c/d/E");
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPath() {
        expr = SequencerPathExpression.compile("/a/b/c/d/e[@something] => .");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c/d/e", "/a/b/c/d/e");
        assertNotMatches(expr.matcher("/a/b/c/d/E/@something2"));
        assertNotMatches(expr.matcher("/a/b/c/d/ex/@something"));
        assertNotMatches(expr.matcher("/a/b[1]/c/d/e/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPathAndExtraPathInsideMatch() {
        expr = SequencerPathExpression.compile("/a/b/c[d/e/@something] => .");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/a/b/c");
        assertNotMatches(expr.matcher("/a/b/c/d/E/@something2"));
        assertNotMatches(expr.matcher("/a/b/c/d/ex/@something"));
        assertNotMatches(expr.matcher("/a/b[1]/c/d/e/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithWildcardSelection() {
        expr = SequencerPathExpression.compile("/a/*/c[d/e/@something] => .");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/a/b/c");
        assertMatches(expr.matcher("/a/b[2]/c/d/e/@something"), "/a/b[2]/c", "/a/b[2]/c");
        assertMatches(expr.matcher("/a/rt/c/d/e/@something"), "/a/rt/c", "/a/rt/c");
        assertNotMatches(expr.matcher("/ac/d/e/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithFilenameLikeWildcardSelection() {
        expr = SequencerPathExpression.compile("/a/*.txt[@something] => .");
        assertMatches(expr.matcher("/a/b.txt/@something"), "/a/b.txt", "/a/b.txt");
        assertNotMatches(expr.matcher("/a/b.tx/@something"));

        expr = SequencerPathExpression.compile("/a/*.txt/c[@something] => .");
        assertMatches(expr.matcher("/a/b.txt/c/@something"), "/a/b.txt/c", "/a/b.txt/c");
        assertNotMatches(expr.matcher("/a/b.tx/c/@something"));

        expr = SequencerPathExpression.compile("//*.txt[*]/c[@something] => .");
        assertMatches(expr.matcher("/a/b.txt/c/@something"), "/a/b.txt/c", "/a/b.txt/c");
        assertNotMatches(expr.matcher("/a/b.tx/c/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithSegmentWildcardSelection() {
        expr = SequencerPathExpression.compile("/a//c[d/e/@something] => .");
        assertMatches(expr.matcher("/a/c/d/e/@something"), "/a/c", "/a/c");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/a/b/c");
        assertMatches(expr.matcher("/a/b[2]/c/d/e/@something"), "/a/b[2]/c", "/a/b[2]/c");
        assertMatches(expr.matcher("/a/rt/c/d/e/@something"), "/a/rt/c", "/a/rt/c");
        assertMatches(expr.matcher("/a/r/s/t/c/d/e/@something"), "/a/r/s/t/c", "/a/r/s/t/c");
        assertMatches(expr.matcher("/a/r[1]/s[2]/t[33]/c/d/e/@something"), "/a/r[1]/s[2]/t[33]/c", "/a/r[1]/s[2]/t[33]/c");
        assertNotMatches(expr.matcher("/a[3]/c/d/e/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithIndexesInSelectionPaths() {
        expr = SequencerPathExpression.compile("/a/b[2,3,4,5]/c/d/e[@something] => /x/y");
        assertMatches(expr.matcher("/a/b[2]/c/d/e/@something"), "/a/b[2]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[3]/c/d/e/@something"), "/a/b[3]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[4]/c/d/e/@something"), "/a/b[4]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[5]/c/d/e/@something"), "/a/b[5]/c/d/e", "/x/y");
        assertNotMatches(expr.matcher("/a/b[1]/c/d/e/@something"));
        assertNotMatches(expr.matcher("/a/b/c/d/e/@something"));
        assertNotMatches(expr.matcher("/a[1]/b/c/d/e/@something"));

        expr = SequencerPathExpression.compile("/a/b[0,2,3,4,5]/c/d/e[@something] => /x/y");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[2]/c/d/e/@something"), "/a/b[2]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[3]/c/d/e/@something"), "/a/b[3]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[4]/c/d/e/@something"), "/a/b[4]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[5]/c/d/e/@something"), "/a/b[5]/c/d/e", "/x/y");
        assertNotMatches(expr.matcher("/a/b[1]/c/d/e/@something"));
        assertNotMatches(expr.matcher("/a[1]/b/c/d/e/@something"));
    }

    @Test
    public void shouldMatchExpressionsWithAnyIndexesInSelectionPaths() {
        expr = SequencerPathExpression.compile("/a/b[*]/c[]/d/e[@something] => /x/y");
        assertMatches(expr.matcher("/a/b[2]/c/d/e/@something"), "/a/b[2]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[3]/c/d/e/@something"), "/a/b[3]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[4]/c/d/e/@something"), "/a/b[4]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[5]/c/d/e/@something"), "/a/b[5]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[1]/c/d/e/@something"), "/a/b[1]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[6]/c/d/e/@something"), "/a/b[6]/c/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b[6]/c[1]/d/e/@something"), "/a/b[6]/c[1]/d/e", "/x/y");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c/d/e", "/x/y");
    }

    @Test
    public void shouldMatchExpressionsWithFullOutputPath() {
        expr = SequencerPathExpression.compile("/a/b/c[d/e/@something] => /x/y");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/x/y");
    }

    @Test
    public void shouldMatchExpressionsWithRepositoryInSelectionPath() {
        expr = SequencerPathExpression.compile("reposA::/a/b/c[d/e/@something] => /x/y");
        assertMatches(expr.matcher("reposA::/a/b/c/d/e/@something"), "/a/b/c", "reposA", null, "/x/y");
    }

    @Test
    public void shouldMatchExpressionsWithRepositoryAndWorkspaceInSelectionPath() {
        expr = SequencerPathExpression.compile("reposA::/a/b/c[d/e/@something] => /x/y");
        assertMatches(expr.matcher("reposA:wsA:/a/b/c/d/e/@something"), "/a/b/c", "reposA", "wsA", "/x/y");
    }

    @Test
    public void shouldMatchExpressionsWithRepositoryInFullOutputPath() {
        expr = SequencerPathExpression.compile("/a/b/c[d/e/@something] => reposA::/x/y");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "reposA", null, "/x/y");
    }

    @Test
    public void shouldMatchExpressionsWithNamedGroupsInOutputPath() {
        expr = SequencerPathExpression.compile("/a(//c)[d/e/@something] => $1/y/z");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/b/c/y/z");

        expr = SequencerPathExpression.compile("/a(/(b|c|d|)/e)[f/g/@something] => $1/y/z");
        assertMatches(expr.matcher("/a/b/e/f/g/@something"), "/a/b/e", "/b/e/y/z");
        assertMatches(expr.matcher("/a/c/e/f/g/@something"), "/a/c/e", "/c/e/y/z");
        assertMatches(expr.matcher("/a/d/e/f/g/@something"), "/a/d/e", "/d/e/y/z");
        assertMatches(expr.matcher("/a/e/f/g/@something"), "/a/e", "/e/y/z");
        assertNotMatches(expr.matcher("/a/t/e/f/g/@something"));

        expr = SequencerPathExpression.compile("/a/(b/c)[(d|e)/(f|g)/@something] => /u/$1/y/z/$2/$3");
        assertMatches(expr.matcher("/a/b/c/d/f/@something"), "/a/b/c", "/u/b/c/y/z/d/f");
        assertMatches(expr.matcher("/a/b/c/e/f/@something"), "/a/b/c", "/u/b/c/y/z/e/f");
        assertMatches(expr.matcher("/a/b/c/d/g/@something"), "/a/b/c", "/u/b/c/y/z/d/g");
        assertMatches(expr.matcher("/a/b/c/e/g/@something"), "/a/b/c", "/u/b/c/y/z/e/g");

        expr = SequencerPathExpression.compile("/a/(b/c)/(d|e)/(f|g)/@something => /u/$1/y/z/$2/$3");
        assertMatches(expr.matcher("/a/b/c/d/f/@something"), "/a/b/c/d/f", "/u/b/c/y/z/d/f");
        assertMatches(expr.matcher("/a/b/c/e/f/@something"), "/a/b/c/e/f", "/u/b/c/y/z/e/f");
        assertMatches(expr.matcher("/a/b/c/d/g/@something"), "/a/b/c/d/g", "/u/b/c/y/z/d/g");
        assertMatches(expr.matcher("/a/b/c/e/g/@something"), "/a/b/c/e/g", "/u/b/c/y/z/e/g");
    }

    @Test
    public void shouldMatchExpressionWithReoccurringNamedGroupsDollarsInOutputPath() {
        expr = SequencerPathExpression.compile("/a/(b/c)[(d|e)/(f|g)/@something] => /u/$1/y/z/$2/$3/$1/$1");
        assertMatches(expr.matcher("/a/b/c/d/f/@something"), "/a/b/c", "/u/b/c/y/z/d/f/b/c/b/c");
    }

    @Test
    public void shouldMatchExpressionWithNamedGroupsAndEscapedDollarsInOutputPath() {
        expr = SequencerPathExpression.compile("/a/(b/c)[(d|e)/(f|g)/@something] => /\\$2u/$1/y/z/$2/$3");
        assertMatches(expr.matcher("/a/b/c/d/f/@something"), "/a/b/c", "/\\$2u/b/c/y/z/d/f");
    }

    @Test
    public void shouldMatchExpressionWithParentReferencesInOutputPath() {
        expr = SequencerPathExpression.compile("/a/b/c[d/e/@something] => /x/y/z/../..");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/x");

        expr = SequencerPathExpression.compile("/a/(b/c)[d/e/@something] => /x/$1/z/../../v");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/x/b/v");
    }

    @Test
    public void shouldMatchExpressionWithSelfReferencesInOutputPath() {
        expr = SequencerPathExpression.compile("/a/b/c[d/e/@something] => /x/y/./z/.");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/x/y/z");

        expr = SequencerPathExpression.compile("/a/(b/c)[d/e/@something] => /x/$1/./z");
        assertMatches(expr.matcher("/a/b/c/d/e/@something"), "/a/b/c", "/x/b/c/z");
    }

    @Test
    public void shouldMatchExpressionWithFilenamePatternAndChildProperty() {
        expr = SequencerPathExpression.compile("//(*.(jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd))[*]/jcr:content[@jcr:data]=>/images/$1");
        assertMatches(expr.matcher("/a/b/caution.png/jcr:content/@jcr:data"),
                      "/a/b/caution.png/jcr:content",
                      "/images/caution.png");
    }

}
