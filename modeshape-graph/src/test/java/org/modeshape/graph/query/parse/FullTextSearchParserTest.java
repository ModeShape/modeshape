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
package org.modeshape.graph.query.parse;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.graph.query.model.FullTextSearch.CompoundTerm;
import org.modeshape.graph.query.model.FullTextSearch.Conjunction;
import org.modeshape.graph.query.model.FullTextSearch.Disjunction;
import org.modeshape.graph.query.model.FullTextSearch.NegationTerm;
import org.modeshape.graph.query.model.FullTextSearch.SimpleTerm;
import org.modeshape.graph.query.model.FullTextSearch.Term;

public class FullTextSearchParserTest {

    private FullTextSearchParser parser;

    @Before
    public void beforeEach() {
        parser = new FullTextSearchParser();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToParseNullString() {
        parser.parse((String)null);
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseEmptyString() {
        parser.parse("");
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToParseBlankString() {
        parser.parse("   ");
    }

    @Test
    public void shouldParseStringWithOneUnquotedTerm() {
        Term result = parser.parse("term1");
        assertSimpleTerm(result, "term1", false, false);
    }

    @Test
    public void shouldParseStringWithOneSingleQuotedTermWithOneWord() {
        Term result = parser.parse("'term1'");
        assertSimpleTerm(result, "term1", false, false);
    }

    @Test
    public void shouldParseStringWithOneSingleQuotedTermWithMultipleWords() {
        Term result = parser.parse("'term1 has two words'");
        assertSimpleTerm(result, "term1 has two words", false, true);
    }

    @Test
    public void shouldParseStringWithOneDoubleQuotedTermWithOneWord() {
        Term result = parser.parse("\"term1\"");
        assertSimpleTerm(result, "term1", false, false);
    }

    @Test
    public void shouldParseStringWithOneDoubleQuotedTermWithMultipleWords() {
        Term result = parser.parse("\"term1 has two words\"");
        assertSimpleTerm(result, "term1 has two words", false, true);
    }

    @Test
    public void shouldParseStringWithMultipleUnquotedTerms() {
        Term result = parser.parse("term1 term2 term3");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Conjunction.class)));
        Conjunction conjunction = (Conjunction)result;
        assertHasSimpleTerms(conjunction, "term1", "term2", "term3");
    }

    @Test
    public void shouldParseStringWithMultipleUnquotedTermsWithNegatedTerms() {
        Term result = parser.parse("term1 term2 -term3");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Conjunction.class)));
        Conjunction conjunction = (Conjunction)result;
        assertHasSimpleTerms(conjunction, "term1", "term2", "-term3");
    }

    @Test
    public void shouldParseStringWithMultipleQuotedAndUnquotedTermsWithNegatedTerms() {
        Term result = parser.parse("term1 \"term2 and 2a\" -term3 -'term 4'");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Conjunction.class)));
        Conjunction conjunction = (Conjunction)result;
        assertHasSimpleTerms(conjunction, "term1", "term2 and 2a", "-term3", "-term 4");
    }

    @Test
    public void shouldParseStringWithMultipleUnquotedORedTerms() {
        Term result = parser.parse("term1 OR term2 OR term3");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertHasSimpleTerms(disjunction, "term1", "term2", "term3");
    }

    @Test
    public void shouldParseStringWithMultipleUnquotedORedTermsWithNegatedTerms() {
        Term result = parser.parse("term1 OR term2 OR -term3");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertHasSimpleTerms(disjunction, "term1", "term2", "-term3");
    }

    @Test
    public void shouldParseStringWithMultipleUnquotedANDedTermsORedTogether() {
        Term result = parser.parse("term1 term2 OR -term3 -term4");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertThat(disjunction.getTerms().size(), is(2));
        Conjunction conjunction1 = (Conjunction)disjunction.getTerms().get(0);
        Conjunction conjunction2 = (Conjunction)disjunction.getTerms().get(1);
        assertHasSimpleTerms(conjunction1, "term1", "term2");
        assertHasSimpleTerms(conjunction2, "-term3", "term4");
    }

    @Test
    public void shouldParseStringWithTwoANDedUnquotedTermsORedWithMultipleUnquotedTerms() {
        Term result = parser.parse("term1 term2 OR -term3 OR -term4 OR term5");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Disjunction.class)));
        Disjunction disjunction = (Disjunction)result;
        assertThat(disjunction.getTerms().size(), is(4));
        Conjunction conjunction1 = (Conjunction)disjunction.getTerms().get(0);
        Term term3 = disjunction.getTerms().get(1);
        Term term4 = disjunction.getTerms().get(2);
        Term term5 = disjunction.getTerms().get(3);
        assertHasSimpleTerms(conjunction1, "term1", "term2");
        assertSimpleTerm(term3, "term3", true, false);
        assertSimpleTerm(term4, "term4", true, false);
        assertSimpleTerm(term5, "term5", false, false);
    }

    @Test
    public void shouldParseStringWithWildcardCharactersAtEndOfSingleTerm() {
        Term result = parser.parse("term*");
        assertSimpleTerm(result, "term*", false, false);
    }

    @Test
    public void shouldParseStringWithWildcardCharactersAtBeginningOfSingleTerm() {
        Term result = parser.parse("*term");
        assertSimpleTerm(result, "*term", false, false);
    }

    @Test
    public void shouldParseStringWithWildcardCharactersInsideSingleTerm() {
        Term result = parser.parse("te*rm");
        assertSimpleTerm(result, "te*rm", false, false);
    }

    @Test
    public void shouldParseStringWithWildcardCharactersInMultipleTerms() {
        Term result = parser.parse("term* term? *term*");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Conjunction.class)));
        Conjunction conjunction = (Conjunction)result;
        assertHasSimpleTerms(conjunction, "term*", "term?", "*term*");
    }

    @Test
    public void shouldParseStringWithWildcardCharactersInSingleQuotedTerm() {
        Term result = parser.parse("'term* term? *term*'");
        assertSimpleTerm(result, "term* term? *term*", false, true);
    }

    @Test
    public void shouldParseStringWithSqlStyleWildcardCharactersInMultipleTerms() {
        Term result = parser.parse("term% term_ %term_");
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(Conjunction.class)));
        Conjunction conjunction = (Conjunction)result;
        assertHasSimpleTerms(conjunction, "term%", "term_", "%term_");
    }

    @Test
    public void shouldParseStringWithSqlStyleWildcardCharactersInSingleQuotedTerm() {
        Term result = parser.parse("'term% term_ %term_'");
        assertSimpleTerm(result, "term% term_ %term_", false, true);
    }

    @Test
    public void shouldParseStringWithEscapedWildcardCharacterInSingleTerm() {
        Term result = parser.parse("term\\*");
        assertSimpleTerm(result, "term\\*", false, false);
    }

    public static void assertHasSimpleTerms( CompoundTerm compoundTerm,
                                             String... terms ) {
        List<Term> expectedTerms = new ArrayList<Term>();
        for (String term : terms) {
            if (term.startsWith("-")) {
                term = term.substring(1);
                expectedTerms.add(new NegationTerm(new SimpleTerm(term)));
            } else {
                expectedTerms.add(new SimpleTerm(term));
            }
        }
        assertHasTerms(compoundTerm, expectedTerms.toArray(new Term[expectedTerms.size()]));
    }

    public static void assertSimpleTerm( Term term,
                                         String value,
                                         boolean excluded,
                                         boolean quotingRequired ) {
        assertThat(term, is(notNullValue()));
        if (excluded) {
            assertThat(term, is(instanceOf(NegationTerm.class)));
            NegationTerm negationTerm = (NegationTerm)term;
            Term negated = negationTerm.getNegatedTerm();
            assertThat(negated, is(instanceOf(SimpleTerm.class)));
            SimpleTerm simpleTerm = (SimpleTerm)negated;
            assertThat(simpleTerm.getValue(), is(value));
            assertThat(simpleTerm.isQuotingRequired(), is(quotingRequired));
        } else {
            assertThat(term, is(instanceOf(SimpleTerm.class)));
            SimpleTerm simpleTerm = (SimpleTerm)term;
            assertThat(simpleTerm.getValue(), is(value));
            assertThat(simpleTerm.isQuotingRequired(), is(quotingRequired));
        }
    }

    public static void assertHasTerms( CompoundTerm compoundTerm,
                                       Term... terms ) {
        Iterator<Term> iterator = compoundTerm.iterator();
        for (int i = 0; i != 0; i++) {
            Term expected = terms[i];
            assertThat(iterator.hasNext(), is(true));
            Term term = iterator.next();
            assertThat(term, is(expected));
        }
    }
}
