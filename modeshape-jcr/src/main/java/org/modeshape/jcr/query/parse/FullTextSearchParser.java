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
package org.modeshape.jcr.query.parse;

import java.util.ArrayList;
import java.util.Collections;
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
import org.modeshape.jcr.api.query.Query;
import org.modeshape.jcr.query.model.AllNodes;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.FullTextSearch.Conjunction;
import org.modeshape.jcr.query.model.FullTextSearch.Disjunction;
import org.modeshape.jcr.query.model.FullTextSearch.NegationTerm;
import org.modeshape.jcr.query.model.FullTextSearch.SimpleTerm;
import org.modeshape.jcr.query.model.FullTextSearch.Term;
import org.modeshape.jcr.query.model.Limit;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.SelectQuery;
import org.modeshape.jcr.query.model.Selector;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;

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
public class FullTextSearchParser implements QueryParser {

    public static final String LANGUAGE = Query.FULL_TEXT_SEARCH;

    private static Selector FULL_TEXT_SOURCE = new AllNodes();
    private static SelectorName FULL_TEXT_SELECTOR_NAME = FULL_TEXT_SOURCE.name();
    private static String SCORE_COLUMN_NAME = "jcr:score";
    protected static List<? extends Column> FULL_TEXT_COLUMNS = Collections.singletonList(new Column(FULL_TEXT_SELECTOR_NAME,
                                                                                                     SCORE_COLUMN_NAME,
                                                                                                     SCORE_COLUMN_NAME));
    private static List<? extends Ordering> FULL_TEXT_ORDERING = Collections.singletonList(new Ordering(
                                                                                                        new PropertyValue(
                                                                                                                          FULL_TEXT_SELECTOR_NAME,
                                                                                                                          SCORE_COLUMN_NAME),
                                                                                                        Order.DESCENDING,
                                                                                                        NullOrder.NULLS_LAST));
    private static boolean FULL_TEXT_DISTINCT = true;

    private static FullTextSearchParser PARSER = new FullTextSearchParser();

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    @Override
    public QueryCommand parseQuery( String query,
                                    TypeSystem typeSystem ) throws InvalidQueryException {
        // Parse the terms ...
        try {
            PARSER.parse(query);
        } catch (ParsingException e) {
            throw new InvalidQueryException(query, e.getMessage());
        }
        // Now create a query that represents this full-text search ...
        Constraint constraint = new FullTextSearch(FULL_TEXT_SELECTOR_NAME, query);
        return new SelectQuery(FULL_TEXT_SOURCE, constraint, FULL_TEXT_ORDERING, FULL_TEXT_COLUMNS, Limit.NONE,
                               FULL_TEXT_DISTINCT);
    }

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

        @Override
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
