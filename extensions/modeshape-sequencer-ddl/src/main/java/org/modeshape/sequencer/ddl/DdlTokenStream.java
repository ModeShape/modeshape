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
package org.modeshape.sequencer.ddl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;

/**
 * A TokenStream implementation designed around requirements for tokenizing and parsing DDL statements.
 * <p>
 * Because of the complexity of DDL, it was necessary to extend {@link TokenStream} in order to override the basic tokenizer to
 * tokenize the in-line comments prefixed with "--". In addition, because there is not a default ddl command (or statement)
 * terminator, an override method was added to {@link TokenStream} to allow re-tokenizing the initial tokens to re-type the
 * tokens, remove tokens, or any other operation to simplify parsing.
 * </p>
 * <p>
 * In this case, both reserved words (or key words) and statement start phrases can be registered prior to the {@link TokenStream}
 * 's start() method. Any resulting tokens that match the registered string values will be re-typed to identify them as key words
 * (DdlTokenizer.KEYWORD) or statement start phrases (DdlTokenizer.STATEMENT_KEY).
 * </p>
 */
public class DdlTokenStream extends TokenStream {

    protected List<String[]> registeredStatementStartPhrases = new ArrayList<String[]>();

    protected Set<String> registeredKeyWords = new HashSet<String>();

    private Position currentMarkedPosition = Position.EMPTY_CONTENT_POSITION;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TokenStream#initializeTokens(java.util.List)
     */
    @Override
    protected List<Token> initializeTokens( List<Token> tokens ) {
        // THIS IS WHERE WE DO THE WORK OF PRE-PARSING TOKENS AND REPLACING KEYWORDS AND STATEMENT STARTS WITH
        // APPLICABLE TOKEN TYPE BITMASK VALUES
        // MyClass[] array = (MyClass[])list.toArray(new MyClass[list.size()]);

        Token[] tokensArray = tokens.toArray(new Token[tokens.size()]);
        List<Token> reTypedTokens = new ArrayList<Token>(tokens.size());

        for (int i = 0; i < tokensArray.length; i++) {
            boolean isStatementStart = false;
            if (isKeyWord(tokensArray[i].value())) {
                Token retypedToken = tokensArray[i].withType(DdlTokenizer.KEYWORD);
                // Now we check to see if this keyword begins a registered statement start

                // Keep track of token increment (# of tokens for a phrase)
                // Need to increment iterator (i) in case phrases like "ALTER ROLLBACK" appear. ROLLBACK is also a statement
                // start phrase and we need to walk ignore ROLLBACK in this case.
                int tokenIncrement = 0;
                for (String[] nextStmtStart : registeredStatementStartPhrases) {
                    boolean matches = true;

                    for (int j = 0; j < nextStmtStart.length; j++) {
                        if (matches) {
                            matches = nextStmtStart[j].equalsIgnoreCase(tokensArray[i + j].value())
                                      || nextStmtStart[j].equals(ANY_VALUE);
                        }
                    }
                    if (matches) {
                        isStatementStart = true;
                        tokenIncrement = nextStmtStart.length - 1;
                        break;
                    }
                }
                if (isStatementStart) {
                    retypedToken = retypedToken.withType(DdlTokenizer.STATEMENT_KEY);
                }
                reTypedTokens.add(retypedToken);

                if (isStatementStart) {
                    // Copy any additional tokens used in the phrase
                    for (int k = 0; k < tokenIncrement; k++) {
                        i++;
                        reTypedTokens.add(tokensArray[i]);
                    }
                }
            } else {
                reTypedTokens.add(tokensArray[i]);
            }

        }

        return reTypedTokens;
    }

    /**
     * @param content
     * @param tokenizer
     * @param caseSensitive
     */
    public DdlTokenStream( String content,
                           Tokenizer tokenizer,
                           boolean caseSensitive ) {
        super(content, tokenizer, caseSensitive);
    }

    /**
     * Register a phrase representing the start of a DDL statement
     * <p>
     * Examples would be: {"CREATE", "TABLE"} {"CREATE", "OR", "REPLACE", "VIEW"}
     * </p>
     * see {@link DdlConstants} for the default SQL 92 representations.
     * 
     * @param phrase
     */
    public void registerStatementStartPhrase( String[] phrase ) {
        registeredStatementStartPhrases.add(phrase);
    }

    public void registerStatementStartPhrase( String[][] phrases ) {
        for (String[] phrase : phrases) {
            registeredStatementStartPhrases.add(phrase);
        }
    }

    /**
     * Register a single key word.
     * 
     * @param keyWord
     */
    public void registerKeyWord( String keyWord ) {
        registeredKeyWords.add(keyWord);
    }

    /**
     * Register an {@link List} of key words.
     * 
     * @param keyWords
     */
    public void registerKeyWords( List<String> keyWords ) {
        registeredKeyWords.addAll(keyWords);
    }

    /**
     * Register an array of key words.
     * 
     * @param keyWords
     */
    public void registerKeyWords( String[] keyWords ) {
        registeredKeyWords.addAll(Arrays.asList(keyWords));
    }

    /**
     * @param word
     * @return is Key Word
     */
    protected boolean isKeyWord( String word ) {
        return registeredKeyWords.contains(word.toUpperCase());
    }

    /**
     * Method to determine if the next token is of type {@link DdlTokenizer} KEYWORD.
     * 
     * @return is Key Word
     */
    public boolean isNextKeyWord() {
        return this.matches(DdlTokenizer.KEYWORD);
    }

    /**
     * Method to determine if next tokens match a registered statement start phrase.
     * 
     * @return true if next tokens match a registered statement start phrase
     */
    public boolean isNextStatementStart() {
        boolean result = false;

        if (isNextKeyWord()) {
            for (String[] nextStmtStart : registeredStatementStartPhrases) {
                if (this.matches(nextStmtStart)) {
                    return true;
                }
            }
        }

        return result;
    }

    /**
     * Marks the current position (line & column number) of the currentToken
     */
    public void mark() {
        if (this.hasNext()) {
            currentMarkedPosition = this.nextPosition();
        } else {
            currentMarkedPosition = null;
        }

    }

    /**
     * Returns the string content for characters bounded by the previous marked position and the position of the currentToken
     * (inclusive). Method also marks() the new position the the currentToken.
     * 
     * @return the string content for characters bounded by the previous marked position and the position of the currentToken
     *         (inclusive).
     */
    public String getMarkedContent() {
        Position startPosition = new Position(currentMarkedPosition.getIndexInContent(), currentMarkedPosition.getLine(),
                                              currentMarkedPosition.getColumn());

        mark();

        return getContentBetween(startPosition, currentMarkedPosition);
    }

    /**
     * Obtain a ddl {@link DdlTokenizer} implementation that ignores whitespace but includes tokens for individual symbols, the
     * period ('.'), single-quoted strings, double-quoted strings, whitespace-delimited words, and optionally comments.
     * <p>
     * Note that the resulting Tokenizer may not be appropriate in many situations, but is provided merely as a convenience for
     * those situations that happen to be able to use it.
     * </p>
     * 
     * @param includeComments true if the comments should be retained and be included in the token stream, or false if comments
     *        should be stripped and not included in the token stream
     * @return the tokenizer; never null
     */
    public static DdlTokenizer ddlTokenizer( boolean includeComments ) {
        return new DdlTokenizer(includeComments);
    }

    public static class DdlTokenizer implements Tokenizer {
        public static final String PARSER_ID = "PARSER_ID";

        /**
         * The {@link TokenStream.Token#type() token type} for tokens that represent an unquoted string containing a character
         * sequence made up of non-whitespace and non-symbol characters.
         */
        public static final int WORD = 1;
        /**
         * The {@link TokenStream.Token#type() token type} for tokens that consist of an individual "symbol" character. The set of
         * characters includes: <code>-(){}*,;+%?$[]!<>|=:</code>
         */
        public static final int SYMBOL = 2;
        /**
         * The {@link TokenStream.Token#type() token type} for tokens that consist of an individual '.' character.
         */
        public static final int DECIMAL = 4;
        /**
         * The {@link TokenStream.Token#type() token type} for tokens that consist of all the characters within single-quotes.
         * Single quote characters are included if they are preceded (escaped) by a '\' character.
         */
        public static final int SINGLE_QUOTED_STRING = 8;
        /**
         * The {@link TokenStream.Token#type() token type} for tokens that consist of all the characters within double-quotes.
         * Double quote characters are included if they are preceded (escaped) by a '\' character.
         */
        public static final int DOUBLE_QUOTED_STRING = 16;
        /**
         * The {@link TokenStream.Token#type() token type} for tokens that consist of all the characters between "/*" and
         * "&#42;/", between "//" and the next line terminator (e.g., '\n', '\r' or "\r\n"), or between "--" and the next line
         * terminator (e.g., '\n', '\r' or "\r\n").
         */
        public static final int COMMENT = 32;

        private final boolean useComments;

        /**
         * The {@link TokenStream.Token#type() token type} for tokens that represent key words or reserved words for a given DDL
         * dialect.
         * <p>
         * Examples would be: "CREATE", "TABLE", "ALTER", "SCHEMA", "DROP", etc...
         * </p>
         * see {@link DdlConstants} for the default SQL 92 representations.
         */
        public static final int KEYWORD = 64;

        /**
         * The {@link TokenStream.Token#type() token type} for tokens that represent the start of a DDL statement.
         * <p>
         * Examples would be: {"CREATE", "TABLE"} {"CREATE", "OR", "REPLACE", "VIEW"}
         * </p>
         * see {@link DdlConstants} for the default SQL 92 representations.
         */
        public static final int STATEMENT_KEY = 128;

        public DdlTokenizer( boolean useComments ) {
            this.useComments = useComments;
        }

        /**
         * @return useComments
         */
        public boolean includeComments() {
            return useComments;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.text.TokenStream.Tokenizer#tokenize(CharacterStream, Tokens)
         */
        public void tokenize( CharacterStream input,
                              Tokens tokens ) throws ParsingException {
            int startIndex;
            int endIndex;
            while (input.hasNext()) {
                char c = input.next();
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // Just skip these whitespace characters ...
                        break;
                    // ==============================================================================================
                    // DDL Comments token = "--"
                    // ==============================================================================================
                    case '-': {
                        startIndex = input.index();
                        Position startPosition = input.position(startIndex);
                        if (input.isNext('-')) {
                            // -- END OF LINE comment ...
                            boolean foundLineTerminator = false;
                            while (input.hasNext()) {
                                c = input.next();
                                if (c == '\n' || c == '\r') {
                                    foundLineTerminator = true;
                                    break;
                                }
                            }
                            endIndex = input.index(); // the token won't include the '\n' or '\r' character(s)
                            if (!foundLineTerminator) ++endIndex; // must point beyond last char
                            if (c == '\r' && input.isNext('\n')) input.next();

                            // Check for PARSER_ID

                            if (useComments) {
                                tokens.addToken(startPosition, startIndex, endIndex, COMMENT);
                            }

                        } else {
                            // just a regular dash ...
                            tokens.addToken(startPosition, startIndex, startIndex + 1, SYMBOL);
                        }
                        break;
                    }
                        // ==============================================================================================
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                    case '*':
                    case ',':
                    case ';':
                    case '+':
                    case '%':
                    case '?':
                    case '$':
                    case '[':
                    case ']':
                    case '!':
                    case '<':
                    case '>':
                    case '|':
                    case '=':
                    case ':':
                        tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                        break;
                    case '.':
                        tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, DECIMAL);
                        break;
                    case '\"':
                        startIndex = input.index();
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
                        endIndex = input.index() + 1; // beyond last character read
                        tokens.addToken(startingPosition, startIndex, endIndex, DOUBLE_QUOTED_STRING);
                        break;
                    case '\u2019': // '’':
                    case '\'':
                        char quoteChar = c;
                        startIndex = input.index();
                        startingPosition = input.position(startIndex);
                        foundClosingQuote = false;
                        while (input.hasNext()) {
                            c = input.next();
                            if (c == '\\' && input.isNext(quoteChar)) {
                                c = input.next(); // consume the ' character since it is escaped
                            } else if (c == quoteChar) {
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
                    case '/':
                        startIndex = input.index();
                        startingPosition = input.position(startIndex);
                        if (input.isNext('/')) {
                            // End-of-line comment ...
                            boolean foundLineTerminator = false;
                            while (input.hasNext()) {
                                c = input.next();
                                if (c == '\n' || c == '\r') {
                                    foundLineTerminator = true;
                                    break;
                                }
                            }
                            endIndex = input.index(); // the token won't include the '\n' or '\r' character(s)
                            if (!foundLineTerminator) ++endIndex; // must point beyond last char
                            if (c == '\r' && input.isNext('\n')) input.next();
                            if (useComments) {
                                tokens.addToken(startingPosition, startIndex, endIndex, COMMENT);
                            }

                        } else if (input.isNext('*')) {
                            // Multi-line comment ...
                            while (input.hasNext() && !input.isNext('*', '/')) {
                                c = input.next();
                            }
                            if (input.hasNext()) input.next(); // consume the '*'
                            if (input.hasNext()) input.next(); // consume the '/'

                            endIndex = input.index() + 1; // the token will include the '/' and '*' characters
                            if (useComments) {
                                tokens.addToken(startingPosition, startIndex, endIndex, COMMENT);
                            }

                        } else {
                            // just a regular slash ...
                            tokens.addToken(startingPosition, startIndex, startIndex + 1, SYMBOL);
                        }
                        break;
                    default:
                        startIndex = input.index();
                        Position startPosition = input.position(startIndex);
                        // Read until another whitespace/symbol/decimal/slash is found
                        while (input.hasNext() && !(input.isNextWhitespace() || input.isNextAnyOf("/.-(){}*,;+%?$[]!<>|=:"))) {
                            c = input.next();
                        }
                        endIndex = input.index() + 1; // beyond last character that was included
                        tokens.addToken(startPosition, startIndex, endIndex, WORD);
                }
            }
        }
    }
}
