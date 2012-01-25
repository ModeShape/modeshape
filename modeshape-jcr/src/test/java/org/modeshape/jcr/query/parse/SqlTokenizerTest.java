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
package org.modeshape.jcr.query.parse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream.CharacterArrayStream;
import org.modeshape.common.text.TokenStream.Tokens;
import org.modeshape.jcr.query.parse.BasicSqlQueryParser.SqlTokenizer;

public class SqlTokenizerTest {

    private SqlTokenizer tokenizer;
    private Tokens tokenFactory;
    private LinkedList<int[]> tokenValues;

    @Before
    public void beforeEach() {
        tokenizer = new SqlTokenizer(true);
        final LinkedList<int[]> tokenValues = new LinkedList<int[]>();
        tokenFactory = new Tokens() {
            @Override
            public void addToken( Position position,
                                  int index ) {
                int[] token = new int[] {index, index + 1, 0};
                tokenValues.add(token);
            }

            @Override
            public void addToken( Position position,
                                  int startIndex,
                                  int endIndex ) {
                int[] token = new int[] {startIndex, endIndex, 0};
                tokenValues.add(token);
            }

            @Override
            public void addToken( Position position,
                                  int startIndex,
                                  int endIndex,
                                  int type ) {
                int[] token = new int[] {startIndex, endIndex, type};
                tokenValues.add(token);
            }
        };
        this.tokenValues = tokenValues;
    }

    protected void tokenize( String input ) {
        tokenizer.tokenize(new CharacterArrayStream(input.toCharArray()), tokenFactory);
    }

    protected void assertNextTokenIs( int startIndex,
                                      int endIndex,
                                      int type ) {
        int[] token = tokenValues.removeFirst();
        assertThat(token[0], is(startIndex));
        assertThat(token[1], is(endIndex));
        assertThat(token[2], is(type));
    }

    protected void assertNoMoreTokens() {
        assertThat(tokenValues.isEmpty(), is(true));
    }

    @Test
    public void shouldCreateNoTokensForEmptyContent() {
        tokenize("");
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateNoTokensForContentWithOnlyWhitespace() {
        tokenize("  \t   \n   \r\n  \r  ");
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForEachSymbolCharacter() {
        String content = "(){}*,:+%?$]!<>|=:";
        int numSymbols = content.length();
        tokenize(content);
        for (int i = 0; i != numSymbols; ++i) {
            assertNextTokenIs(i, i + 1, SqlTokenizer.SYMBOL);
        }
        assertNoMoreTokens();
    }

    @Test
    public void shouldNotConsiderDoubleSlashAsComment() {
        String content = "++//this\n";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 3, SqlTokenizer.SYMBOL);
        assertNextTokenIs(3, 4, SqlTokenizer.SYMBOL);
        assertNextTokenIs(4, content.length() - 1, SqlTokenizer.WORD); // -1 because '\n' is not included
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForEndOfLineCommentUsingDoubleDash() {
        String content = "++--this is a comment\n";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length() - 1, SqlTokenizer.COMMENT); // -1 because '\n' is not included
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineComment() {
        String content = "==/*this is a comment*/-";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length() - 1, SqlTokenizer.COMMENT);
        assertNextTokenIs(content.length() - 1, content.length(), SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentAtEndOfContent() {
        String content = "==/*this is a comment*/";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), SqlTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentWithoutTerminatingCharacters() {
        String content = "==/*this is a comment";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), SqlTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentWithoutAllTerminatingCharacters() {
        String content = "==/*this is a comment*";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), SqlTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedString() {
        String content = "=='this is a single-quoted \n string'-";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(35), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, SqlTokenizer.QUOTED_STRING);
        assertNextTokenIs(36, 37, SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedStringWithEscapedSingleQuoteCharacters() {
        String content = "=='this \"is\" a \\'single-quoted\\' \n string'-";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(41), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 42, SqlTokenizer.QUOTED_STRING);
        assertNextTokenIs(42, 43, SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedStringAtEndOfContent() {
        String content = "=='this is a single-quoted \n string'";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(35), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test( expected = ParsingException.class )
    public void shouldCreateTokenForSingleQuotedStringWithoutClosingQuote() {
        String content = "=='this is a single-quoted \n string";
        tokenize(content);
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedString() {
        String content = "==\"this is a double-quoted \n string\"-";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(35), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, SqlTokenizer.QUOTED_STRING);
        assertNextTokenIs(36, 37, SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedStringWithEscapedDoubleQuoteCharacters() {
        String content = "==\"this 'is' a \\\"double-quoted\\\" \n string\"-";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(41), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 42, SqlTokenizer.QUOTED_STRING);
        assertNextTokenIs(42, 43, SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedStringAtEndOfContent() {
        String content = "==\"this is a double-quoted \n string\"";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(35), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test( expected = ParsingException.class )
    public void shouldCreateTokenForDoubleQuotedStringWithoutClosingQuote() {
        String content = "==\"this is a double-quoted \n string";
        tokenize(content);
    }

    @Test
    public void shouldCreateTokenForSquareBracketQuotedString() {
        String content = "[/foo/bar/baz]";
        tokenize(content);
        assertNextTokenIs(0, content.length(), SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSquareBracketQuotedStringWithEmbeddedUnquotedSquareBrackets() {
        String content = "[/foo/bar[12]/baz[3]]";
        tokenize(content);
        assertNextTokenIs(0, content.length(), SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSquareBracketQuotedStringWithUnrealisticEmbeddedUnquotedSquareBrackets() {
        String content = "[/foo/bar[12]/baz[[[3]]]]";
        tokenize(content);
        assertNextTokenIs(0, content.length(), SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokensForWordsWithAlphabeticCharacters() {
        String content = "This is a series of words.";
        tokenize(content);
        assertNextTokenIs(0, 4, SqlTokenizer.WORD);
        assertNextTokenIs(5, 7, SqlTokenizer.WORD);
        assertNextTokenIs(8, 9, SqlTokenizer.WORD);
        assertNextTokenIs(10, 16, SqlTokenizer.WORD);
        assertNextTokenIs(17, 19, SqlTokenizer.WORD);
        assertNextTokenIs(20, 25, SqlTokenizer.WORD);
        assertNextTokenIs(25, 26, SqlTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokensForWordsWithNumericCharacters() {
        String content = "1234 4 5353.324";
        tokenize(content);
        assertNextTokenIs(0, 4, SqlTokenizer.WORD);
        assertNextTokenIs(5, 6, SqlTokenizer.WORD);
        assertNextTokenIs(7, 11, SqlTokenizer.WORD);
        assertNextTokenIs(11, 12, SqlTokenizer.SYMBOL);
        assertNextTokenIs(12, 15, SqlTokenizer.WORD);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokensForWordsWithAlphaNumericCharacters() {
        String content = "123a 5353.324e100";
        tokenize(content);
        assertNextTokenIs(0, 4, SqlTokenizer.WORD);
        assertNextTokenIs(5, 9, SqlTokenizer.WORD);
        assertNextTokenIs(9, 10, SqlTokenizer.SYMBOL);
        assertNextTokenIs(10, 17, SqlTokenizer.WORD);
        assertNoMoreTokens();
    }

    @Test
    public void shouldParseAlphaNumericAndUnderscoreCharactersAsOneWordToken() {
        String content = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";
        tokenize(content);
        assertNextTokenIs(0, 63, SqlTokenizer.WORD);
        assertNoMoreTokens();
    }

    @Test
    public void shouldParseUnquotedNameWithPrefixAsSeparateTokens() {
        String content = "dna:name";
        tokenize(content);
        assertNextTokenIs(0, 3, SqlTokenizer.WORD);
        assertNextTokenIs(3, 4, SqlTokenizer.SYMBOL);
        assertNextTokenIs(4, 8, SqlTokenizer.WORD);
        assertNoMoreTokens();
    }

    @Test
    public void shouldParseQuotedNameWithPrefixAsOneToken() {
        String content = "'dna:name'";
        tokenize(content);
        assertNextTokenIs(0, 10, SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test
    public void shouldParseUnquotedPathAsSeparateTokens() {
        String content = "/a/b/c/dna:name[1]/e/f";
        tokenize(content);
        assertNextTokenIs(0, 1, SqlTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, SqlTokenizer.WORD); // a
        assertNextTokenIs(2, 3, SqlTokenizer.SYMBOL);
        assertNextTokenIs(3, 4, SqlTokenizer.WORD); // b
        assertNextTokenIs(4, 5, SqlTokenizer.SYMBOL);
        assertNextTokenIs(5, 6, SqlTokenizer.WORD); // c
        assertNextTokenIs(6, 7, SqlTokenizer.SYMBOL);
        assertNextTokenIs(7, 10, SqlTokenizer.WORD); // dna
        assertNextTokenIs(10, 11, SqlTokenizer.SYMBOL);
        assertNextTokenIs(11, 15, SqlTokenizer.WORD); // name
        assertNextTokenIs(15, 18, SqlTokenizer.QUOTED_STRING); // [1]
        assertNextTokenIs(18, 19, SqlTokenizer.SYMBOL);
        assertNextTokenIs(19, 20, SqlTokenizer.WORD); // e
        assertNextTokenIs(20, 21, SqlTokenizer.SYMBOL);
        assertNextTokenIs(21, 22, SqlTokenizer.WORD); // f
        assertNoMoreTokens();
    }

    @Test
    public void shouldParseQuotedPathAsOneToken() {
        String content = "'/a/b/c/dna:name[1]/e/f'";
        tokenize(content);
        assertNextTokenIs(0, 24, SqlTokenizer.QUOTED_STRING);
        assertNoMoreTokens();
    }
}
