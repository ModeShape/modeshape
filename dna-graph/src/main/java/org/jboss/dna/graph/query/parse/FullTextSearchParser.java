/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.parse;

import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.TokenStream;
import org.jboss.dna.common.text.TokenStream.Tokenizer;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.query.model.FullTextSearch.Conjunction;
import org.jboss.dna.graph.query.model.FullTextSearch.Disjunction;
import org.jboss.dna.graph.query.model.FullTextSearch.NegationTerm;
import org.jboss.dna.graph.query.model.FullTextSearch.SimpleTerm;
import org.jboss.dna.graph.query.model.FullTextSearch.Term;

/**
 * A {@link QueryParser} implementation that parses a full-text search expression. This grammar is based on the full-text search
 * grammar as defined by the JCR 2.0 specification.
 */
public class FullTextSearchParser {

    /**
     * Parse the full-text search criteria given in the supplied string.
     * 
     * @param fullTextSearchExpression the full-text search expression; may not be null
     * @return the term representation of the full-text search, or null if there are no terms
     * @throws ParsingException if there is an error parsing the supplied string
     * @throws IllegalArgumentException if the expression is null
     */
    public Term parse( String fullTextSearchExpression ) {
        CheckArg.isNotNull(fullTextSearchExpression, "fullTextSearchExpression");
        Tokenizer tokenizer = TokenStream.basicTokenizer(false);
        TokenStream stream = new TokenStream(fullTextSearchExpression, tokenizer, false);
        return parse(stream.start());
    }

    /**
     * Parse the full-text search criteria from the supplied token stream. This method is useful when the full-text search
     * expression is included in other content.
     * 
     * @param tokens the token stream containing the full-text search starting on the next token
     * @return the term representation of the full-text search, or null if there are no terms
     * @throws ParsingException if there is an error parsing the supplied string
     * @throws IllegalArgumentException if the token stream is null
     */
    public Term parse( TokenStream tokens ) {
        CheckArg.isNotNull(tokens, "tokens");
        List<Term> terms = new ArrayList<Term>();
        do {
            Term term = parseDisjunctedTerms(tokens);
            if (term == null) break;
            terms.add(term);
        } while (tokens.canConsume("OR"));
        if (terms.isEmpty()) return null;
        return terms.size() > 1 ? new Disjunction(terms) : terms.iterator().next();
    }

    protected Term parseDisjunctedTerms( TokenStream tokens ) {
        List<Term> terms = new ArrayList<Term>();
        do {
            Term term = parseTerm(tokens);
            if (term == null) break;
            terms.add(term);
        } while (tokens.hasNext() && !tokens.matches("OR"));
        if (terms.isEmpty()) return null;
        return terms.size() > 1 ? new Conjunction(terms) : terms.iterator().next();
    }

    protected Term parseTerm( TokenStream tokens ) {
        boolean negated = tokens.canConsume('-');
        Term result = new SimpleTerm(removeQuotes(tokens.consume()));
        return negated ? new NegationTerm(result) : result;
    }

    /**
     * Remove any leading and trailing single- or double-quotes from the supplied text.
     * 
     * @param text the input text; may not be null
     * @return the text without leading and trailing quotes, or <code>text</code> if there were no quotes
     */
    protected String removeQuotes( String text ) {
        return text.replaceFirst("^['\"]+", "").replaceAll("['\"]+$", "");
    }
}
