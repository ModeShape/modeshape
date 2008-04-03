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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencerPathExpressionTest {

    protected static final String NO_MATCH = null;
    private SequencerPathExpression expr;

    @Before
    public void beforeEach() throws Exception {
    }

    @Test
    public void shouldReplacePatterns() {
        assertThat(SequencerPathExpression.replacePatterns("/a/b/c/d/"), is("/a(?:\\[\\d+\\])?/b(?:\\[\\d+\\])?/c(?:\\[\\d+\\])?/d(?:\\[\\d+\\])?/"));
        assertThat(SequencerPathExpression.replacePatterns("/a/*/c/d/"), is("/a(?:\\[\\d+\\])?/[^/]*/c(?:\\[\\d+\\])?/d(?:\\[\\d+\\])?/"));
    }

    @Test
    public void shouldNotRequireMatchExpression() {
        expr = new SequencerPathExpression("/a/b/c/d/e/@something");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e/@something"));
        assertThat(expr.matches("/a/b/c/d/e"), is(NO_MATCH));

        expr = new SequencerPathExpression("/a/b/c/d/e/");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e/@something"));
        assertThat(expr.matches("/a/b/c/d/e"), is("/a/b/c/d/e"));
    }

    @Test
    public void shouldMatchExpressionsWithoutRegardToCase() {
        expr = new SequencerPathExpression("/a/b/c/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a/b/c/d/E/@something"), is("/a/b/c/d/E"));
        assertThat(expr.matches("/a/b/c[3]/d/E/@something"), is("/a/b/c[3]/d/E"));
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPath() {
        expr = new SequencerPathExpression("/a/b/c/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a/b/c/d/e/@something2"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/Ex/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithExactFullPathAndExtraPathInsideMatch() {
        expr = new SequencerPathExpression("/a/b/c[d/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c"));
        assertThat(expr.matches("/a/b/c/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/e/@something2"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithTrailingSlashesInPath() {
        expr = new SequencerPathExpression("/a/b/c/[d/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c"));
        assertThat(expr.matches("/a/b/c/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/e/@something2"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsUsingAbsolutePathsInMatches() {
        expr = new SequencerPathExpression("/a/b/c[/a/b/c/d/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c"));
        assertThat(expr.matches("/a/b/c/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/e/@something2"), is(NO_MATCH));

        expr = new SequencerPathExpression("/a/b/c/[/d/e/@something]");
        assertThat(expr.matches("/d/e/@something"), is("/a/b/c"));
        assertThat(expr.matches("/a/b/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/e/@something2"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithWildcard() {
        expr = new SequencerPathExpression("/a/*/*/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a/x/c/d/e/@something"), is("/a/x/c/d/e"));
        assertThat(expr.matches("/a/bbb/xxx/d/e/@something"), is("/a/bbb/xxx/d/e"));
        assertThat(expr.matches("/a/bbb/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithWildcardAndFilenameExtension() {
        expr = new SequencerPathExpression("/a/*/*.java/d/e[@something]");
        assertThat(expr.matches("/a/b/c.java/d/e/@something"), is("/a/b/c.java/d/e"));
        assertThat(expr.matches("/a/x/c.java/d/e/@something"), is("/a/x/c.java/d/e"));
        assertThat(expr.matches("/a/x/.java/d/e/@something"), is("/a/x/.java/d/e"));
        assertThat(expr.matches("/a/bbb/xxx.java/d/e/@something"), is("/a/bbb/xxx.java/d/e"));
        assertThat(expr.matches("/a/bbb/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithAnyDepthWildcard() {
        expr = new SequencerPathExpression("/a//d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is("/a/d/e"));
        assertThat(expr.matches("/a/b/d/e/@something"), is("/a/b/d/e"));
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a/b/c/d/d/d/e/@something"), is("/a/b/c/d/d/d/e"));
        assertThat(expr.matches("/a/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithOrs() {
        expr = new SequencerPathExpression("/a/(bcd|c)/d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/bcd/d/e/@something"), is("/a/bcd/d/e"));
        assertThat(expr.matches("/a/c/d/e/@something"), is("/a/c/d/e"));
        assertThat(expr.matches("/a/a/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithMultipleOrs() {
        expr = new SequencerPathExpression("/a/(bc|d|c)/d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/bc/d/e/@something"), is("/a/bc/d/e"));
        assertThat(expr.matches("/a/d/d/e/@something"), is("/a/d/d/e"));
        assertThat(expr.matches("/a/c/d/e/@something"), is("/a/c/d/e"));
        assertThat(expr.matches("/a/a/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithEmptySubexpressionInOr() {
        expr = new SequencerPathExpression("/a/(bcd|)/d/e[@something]");
        assertThat(expr.matches("/a/bcd/d/e/@something"), is("/a/bcd/d/e"));
        assertThat(expr.matches("/a//d/e/@something"), is("/a//d/e"));
        assertThat(expr.matches("/a/d/e/@something"), is("/a/d/e"));
    }

    @Test
    public void shouldMatchExpressionsWithMultipleEmptySubexpressionsInOr() {
        expr = new SequencerPathExpression("/a/(bcd|||)/d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is("/a/d/e"));
        assertThat(expr.matches("/a//d/e/@something"), is("/a//d/e"));
        assertThat(expr.matches("/a/bcd/d/e/@something"), is("/a/bcd/d/e"));

        expr = new SequencerPathExpression("/a/(|bcd|)/d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is("/a/d/e"));
        assertThat(expr.matches("/a//d/e/@something"), is("/a//d/e"));
        assertThat(expr.matches("/a/bcd/d/e/@something"), is("/a/bcd/d/e"));

        expr = new SequencerPathExpression("/a/(|bcd|||bcd|)/d/e[@something]");
        assertThat(expr.matches("/a/d/e/@something"), is("/a/d/e"));
        assertThat(expr.matches("/a//d/e/@something"), is("/a//d/e"));
        assertThat(expr.matches("/a/bcd/d/e/@something"), is("/a/bcd/d/e"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowMatchExpressionsThatAreNull() {
        new SequencerPathExpression(null);
    }

    @Test( expected = InvalidSequencerPathExpression.class )
    public void shouldNotAllowMatchExpressionsThatAreEmpty() {
        new SequencerPathExpression("");
    }

    @Test( expected = InvalidSequencerPathExpression.class )
    public void shouldNotAllowMatchExpressionsThatAreBlank() {
        new SequencerPathExpression("  ");
    }

    @Test
    public void shouldMatchExpressionsWithReplacementVariablesInPathAndRelativeMatchPath() {
        expr = new SequencerPathExpression("/a/b/$1/c/[d/(b|e)/@something]");
        assertThat(expr.matches("/a/b/$1/c/d/e/@something"), is("/a/b/$1/c"));
        assertThat(expr.matches("/a/b/$2/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/$a/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b/c/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteria() {
        expr = new SequencerPathExpression("/something/x/y/z[/a/b/c/d/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/x/y/z"));
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteriaAndRepositoryOutput() {
        expr = new SequencerPathExpression("reposA:/something/x/y/z[/a/b/c/d/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("reposA:/something/x/y/z"));
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteriaAndCapturedRepository() {
        expr = new SequencerPathExpression("$1_2:/something/x/y/z[(reposA):/a/b/c/d/e/@something]");
        assertThat(expr.matches("reposA:/a/b/c/d/e/@something"), is("reposA_2:/something/x/y/z"));
    }

    @Test(expected = InvalidSequencerPathExpression.class )
    public void shouldNotAllowMatchCriteriaWithCaptureSpanningRepositoryAndPath() {
        expr = new SequencerPathExpression("$1_2:/something/x/y/z[(reposA:/a/b)/c/d/e/@something]");
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteriaAndTransformOutputUsingSingleCapturingParentheses() {
        expr = new SequencerPathExpression("/something$1[(/a/b/c/d/e/)@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/a/b/c/d/e"));

        expr = new SequencerPathExpression("/something/$1[(/a/b/c/d/e/)@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something//a/b/c/d/e"));

        expr = new SequencerPathExpression("/something$1[/a/b(/c/d/e/)@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/c/d/e"));
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteriaAndTransformOutputUsingMultipleCapturingParentheses() {
        expr = new SequencerPathExpression("/something/$1/$2[/(a/b)/c/(d)/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/a/b/d"));
    }

    @Test
    public void shouldMatchExpressionsWithAbsoluteMatchCriteriaAndTransformOutputUsingCapturingParenthesesWithWildcards() {
        expr = new SequencerPathExpression("/something$1[(//d)/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/a/b/c/d"));

        expr = new SequencerPathExpression("/something$1[/a(//d)/e/@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/something/b/c/d"));
    }

    @Test
    public void shouldMatchExpressionsWithExplicitSiblingIndexesIfSuppliedPathUsesSameIndexes() {
        expr = new SequencerPathExpression("/a/b[2]/c/d/e[@something]");
        assertThat(expr.matches("/a/b[2]/c/d/e/@something"), is("/a/b[2]/c/d/e"));
    }

    @Test
    public void shouldMatchExpressionsWithExplicitSiblingIndexRangeIfSuppliedPathUsesIndexInRange() {
        expr = new SequencerPathExpression("/a/b[2,3,4,5]/c/d/e[@something]");
        assertThat(expr.matches("/a/b[1]/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b[2]/c/d/e/@something"), is("/a/b[2]/c/d/e"));
        assertThat(expr.matches("/a/b[3]/c/d/e/@something"), is("/a/b[3]/c/d/e"));
        assertThat(expr.matches("/a/b[4]/c/d/e/@something"), is("/a/b[4]/c/d/e"));
        assertThat(expr.matches("/a/b[5]/c/d/e/@something"), is("/a/b[5]/c/d/e"));
        assertThat(expr.matches("/a/b[6]/c/d/e/@something"), is(NO_MATCH));

        expr = new SequencerPathExpression("/a/b[0,1,2,3,4,5]/c/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a/b[0]/c/d/e/@something"), is("/a/b[0]/c/d/e"));
        assertThat(expr.matches("/a/b[1]/c/d/e/@something"), is("/a/b[1]/c/d/e"));
        assertThat(expr.matches("/a/b[2]/c/d/e/@something"), is("/a/b[2]/c/d/e"));
        assertThat(expr.matches("/a/b[3]/c/d/e/@something"), is("/a/b[3]/c/d/e"));
        assertThat(expr.matches("/a/b[4]/c/d/e/@something"), is("/a/b[4]/c/d/e"));
        assertThat(expr.matches("/a/b[5]/c/d/e/@something"), is("/a/b[5]/c/d/e"));
        assertThat(expr.matches("/a/b[6]/c/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldNotMatchExpressionsThatDontMatchExplicitSiblingIndexes() {
        expr = new SequencerPathExpression("/a/b[2]/c/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b[0]/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b[1]/c/d/e/@something"), is(NO_MATCH));
        assertThat(expr.matches("/a/b[3]/c/d/e/@something"), is(NO_MATCH));
    }

    @Test
    public void shouldMatchExpressionsWithExplicitlyExcludedSiblingIndexesIfSuppliedPathHasNoIndex() {
        expr = new SequencerPathExpression("/a/b[]/c/d/e[@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a[0]/b/c[1]/d[2]/e[3]/@something"), is("/a[0]/b/c[1]/d[2]/e[3]"));
        assertThat(expr.matches("/a/b[2]/c/d/e/@something"), is(NO_MATCH));

        expr = new SequencerPathExpression("/a/b[]/c/d/e[][@something]");
        assertThat(expr.matches("/a/b/c/d/e/@something"), is("/a/b/c/d/e"));
        assertThat(expr.matches("/a[0]/b/c[1]/d[2]/e/@something"), is("/a[0]/b/c[1]/d[2]/e"));
        assertThat(expr.matches("/a[0]/b/c[1]/d[2]/e[3]/@something"), is(NO_MATCH));
    }

}
