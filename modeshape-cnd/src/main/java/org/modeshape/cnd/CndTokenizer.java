package org.modeshape.cnd;

import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream.CharacterStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.text.TokenStream.Tokens;

/**
 * A {@link Tokenizer} implementation that adheres to the CND format by ignoring whitespace while including tokens for individual
 * symbols, the period ('.'), single-quoted strings, double-quoted strings, whitespace-delimited words, and optionally comments.
 * This tokenizer optionally includes comments and vendor extensions.
 */
public class CndTokenizer implements Tokenizer {
    /**
     * The token type for tokens that represent an unquoted string containing a character sequence made up of non-whitespace and
     * non-symbol characters.
     */
    public static final int WORD = 1;
    /**
     * The token type for tokens that consist of an individual "symbol" character. The set of characters includes:
     * <code>[]<>=-+(),</code>
     */
    public static final int SYMBOL = 2;
    /**
     * The token type for tokens that consist of an individual '.' character.
     */
    public static final int DECIMAL = 3;
    /**
     * The token type for tokens that consist of all the characters within single-quotes. Single quote characters are included if
     * they are preceded (escaped) by a '\' character.
     */
    public static final int SINGLE_QUOTED_STRING = 4;
    /**
     * The token type for tokens that consist of all the characters within double-quotes. Double quote characters are included if
     * they are preceded (escaped) by a '\' character.
     */
    public static final int DOUBLE_QUOTED_STRING = 5;
    /**
     * The token type for tokens that consist of all the characters between "/*" and "&#42;/" or between "//" and the next line
     * terminator (e.g., '\n', '\r' or "\r\n").
     */
    public static final int COMMENT = 6;
    /**
     * The token type for the token containing a vendor extension block.
     */
    public static final int VENDOR_EXTENSION = 7;

    private final boolean useComments;
    private final boolean useVendorExtensions;

    public CndTokenizer( boolean useComments,
                         boolean useVendorExtensions ) {
        this.useComments = useComments;
        this.useVendorExtensions = useVendorExtensions;
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
                case '[':
                case ']':
                case '<':
                case '>':
                case '=':
                case '-':
                case '+':
                case '(':
                case ')':
                case ',':
                    tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                    break;
                // case '.':
                // tokens.addToken(input.position(), input.index(), input.index() + 1, DECIMAL);
                // break;
                case '{':
                    // Vendor extension, meant to be excluded
                    int startIndex = input.index();
                    Position startingPosition = input.position(startIndex);
                    boolean foundClosingBrace = false;
                    String vendorName = "";
                    while (input.hasNext()) {
                        c = input.next();
                        if (c == '\\' && input.isNext('}')) {
                            c = input.next(); // consume the '}' character since it is escaped
                        } else if (c == '}') {
                            foundClosingBrace = true;
                            break;
                        }
                    }
                    if (!foundClosingBrace) {
                        String msg = CndI18n.vendorBlockWasNotClosed.text(startingPosition.getLine(),
                                                                          startingPosition.getColumn());
                        throw new ParsingException(startingPosition, msg);
                    }
                    int endIndex = input.index() + 1; // beyond last character read
                    if (useVendorExtensions) {
                        tokens.addToken(startingPosition, startIndex, endIndex, VENDOR_EXTENSION);
                    }
                    break;
                case '\"':
                    startIndex = input.index();
                    startingPosition = input.position(startIndex);
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
                        if (useComments) {
                            endIndex = input.index() + 1; // the token will include the '/' and '*' characters
                            tokens.addToken(startingPosition, startIndex, endIndex, COMMENT);
                        }
                    } else {
                        continue;
                    }
                    break;
                default:
                    // The JCR 2.0 Public Final Draft is very unclear about what exactly a string is defined to be,
                    // and in fact the reference implementation (all versions) basically just treat an unquoted string
                    // to be defined as
                    // - unquoted_string ::= [A-Za-z0-9:_]+
                    // But this doesn't really seem to align very well with the spec, which alludes to any number
                    // of XmlChar:
                    // - unquoted_string ::= XmlChar { XmlChar }
                    // - XmlChar ::= /* see §3.2.2 Local Names */
                    // Then in Section 3.2.2, there is this rule:
                    // - XmlChar ::= /* Any character that matches the Char production at http://www.w3.org/TR/xml/#NT-Char */
                    // This doesn't really make sense, because even whitespace is valid in Char.
                    //
                    // Could the CND grammar instead reference 3.2.5.2 (rather than 3.2.2)? This refers to qualified
                    // names, and appears to be much closer to the examples and reference implementation.
                    //
                    // What we're doing is basically reading all subsequent characters until we find a whitespace,
                    // one of the SYMBOL characters, a single- or double-quote character, a slash, or an open brace
                    // (since these are all the basis for other tokenization rules above). Also, the '*' and '|'
                    // characters terminate a WORD token, since these cannot appear unescaped within local names;
                    // since these do not appear in other rules above, they will result in one-character tokens.
                    //
                    startIndex = input.index();
                    startingPosition = input.position(startIndex);
                    // Read as long as there is a valid XML character ...
                    while (input.hasNext() && !(input.isNextWhitespace() || input.isNextAnyOf("[]<>=-+(),\"'/{*|"))) {
                        c = input.next();
                    }
                    endIndex = input.index() + 1; // beyond last character that was included
                    tokens.addToken(startingPosition, startIndex, endIndex, WORD);
            }
        }
    }
}
