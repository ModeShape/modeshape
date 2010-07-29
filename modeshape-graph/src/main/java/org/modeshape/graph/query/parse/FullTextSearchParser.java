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

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.TokenStream.CharacterStream;
import org.modeshape.common.text.TokenStream.Token;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.text.TokenStream.Tokens;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.query.model.FullTextSearch.Conjunction;
import org.modeshape.graph.query.model.FullTextSearch.Disjunction;
import org.modeshape.graph.query.model.FullTextSearch.NegationTerm;
import org.modeshape.graph.query.model.FullTextSearch.SimpleTerm;
import org.modeshape.graph.query.model.FullTextSearch.Term;

/**
 * A {@link QueryParser} implementation that parses a full-text search expression. This grammar is based on the full-text search
 * grammar as defined by the JCR 2.0 specification.
 * <p>
 * </p>
 * <h3>Grammar</h3>
 * <p>
 * The grammar for the full-text expression is taken from the JCR 2.0 specification, and is as follows:
 * </p>
 * 
 * <pre>
 * FulltextSearch ::= Disjunct {Space 'OR' Space Disjunct}
 * Disjunct ::= Term {Space Term}
 * Term ::= ['-'] SimpleTerm
 * SimpleTerm ::= Word | '&quot;' Word {Space Word} '&quot;'
 * Word ::= NonSpaceChar {NonSpaceChar}
 * Space ::= SpaceChar {SpaceChar}
 * NonSpaceChar ::= Char - SpaceChar /* Any Char except SpaceChar &#42;/
 * SpaceChar ::= ' '
 * Char ::= /* Any character &#42;/
 * </pre>
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
        Tokenizer tokenizer = new TermTokenizer();
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
        if (!negated) tokens.canConsume('+');
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

    /**
     * A basic {@link Tokenizer} implementation that ignores whitespace but includes tokens for individual symbols, the period
     * ('.'), single-quoted strings, double-quoted strings, whitespace-delimited words, and optionally comments.
     * <p>
     * Note this Tokenizer may not be appropriate in many situations, but is provided merely as a convenience for those situations
     * that happen to be able to use it.
     * </p>
     */
    public static class TermTokenizer implements Tokenizer {
        /**
         * The {@link Token#type() token type} for tokens that represent an unquoted string containing a character sequence made
         * up of non-whitespace and non-symbol characters.
         */
        public static final int WORD = 1;
        /**
         * The {@link Token#type() token type} for tokens that consist of an individual '+' or '-' characters. The set of
         * characters includes: <code>-+</code>
         */
        public static final int PLUS_MINUS = 2;
        /**
         * The {@link Token#type() token type} for tokens that consist of all the characters within single-quotes. Single quote
         * characters are included if they are preceded (escaped) by a '\' character.
         */
        public static final int SINGLE_QUOTED_STRING = 4;
        /**
         * The {@link Token#type() token type} for tokens that consist of all the characters within double-quotes. Double quote
         * characters are included if they are preceded (escaped) by a '\' character.
         */
        public static final int DOUBLE_QUOTED_STRING = 8;

        protected TermTokenizer() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.text.TokenStream.Tokenizer#tokenize(CharacterStream, Tokens)
         */
        public void tokenize( CharacterStream input,
                              Tokens tokens ) throws ParsingException {
            while (input.hasNext()) {
                char c = input.next();
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // Just skip these whitespace characters ...
                        break;
                    case '-':
                    case '+':
                        tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, PLUS_MINUS);
                        break;
                    case '\"':
                        int startIndex = input.index();
                        Position startingPosition = input.position(startIndex);
                        boolean foundClosingQuote = false;
                        while (input.hasNext()) {
                            c = input.next();
                            if (c == '\\' && input.isNext('"')) {
                                c = input.next(); // consume the ' character since it is escaped
                            } else if (c == '"') {
                                foundClosingQuote = true;
                                break;
                            }
                        }
                        if (!foundClosingQuote) {
                            String msg = CommonI18n.noMatchingDoubleQuoteFound.text(startingPosition.getLine(),
                                                                                    startingPosition.getColumn());
                            throw new ParsingException(startingPosition, msg);
                        }
                        int endIndex = input.index() + 1; // beyond last character read
                        tokens.addToken(startingPosition, startIndex, endIndex, DOUBLE_QUOTED_STRING);
                        break;
                    case '\'':
                        startIndex = input.index();
                        startingPosition = input.position(startIndex);
                        foundClosingQuote = false;
                        while (input.hasNext()) {
                            c = input.next();
                            if (c == '\\' && input.isNext('\'')) {
                                c = input.next(); // consume the ' character since it is escaped
                            } else if (c == '\'') {
                                foundClosingQuote = true;
                                break;
                            }
                        }
                        if (!foundClosingQuote) {
                            String msg = CommonI18n.noMatchingSingleQuoteFound.text(startingPosition.getLine(),
                                                                                    startingPosition.getColumn());
                            throw new ParsingException(startingPosition, msg);
                        }
                        endIndex = input.index() + 1; // beyond last character read
                        tokens.addToken(startingPosition, startIndex, endIndex, SINGLE_QUOTED_STRING);
                        break;
                    default:
                        startIndex = input.index();
                        startingPosition = input.position(startIndex);
                        // Read until another whitespace is found
                        while (input.hasNext() && !(input.isNextWhitespace())) {
                            c = input.next();
                        }
                        endIndex = input.index() + 1; // beyond last character that was included
                        tokens.addToken(startingPosition, startIndex, endIndex, WORD);
                }
            }
        }
    }

}
