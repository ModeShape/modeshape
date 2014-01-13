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
package org.modeshape.common.text;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.LinkedList;
import org.modeshape.common.text.TokenStream.BasicTokenizer;
import org.modeshape.common.text.TokenStream.CharacterArrayStream;
import org.modeshape.common.text.TokenStream.Tokens;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class TokenStreamBasicTokenizerTest {

    private BasicTokenizer tokenizer;
    private Tokens tokenFactory;
    private LinkedList<int[]> tokenValues;

    @Before
    public void beforeEach() {
        tokenizer = TokenStream.basicTokenizer(true);
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
        String content = "-(){}*,;+%?$[]!<>|=:";
        int numSymbols = content.length();
        tokenize(content);
        for (int i = 0; i != numSymbols; ++i) {
            assertNextTokenIs(i, i + 1, BasicTokenizer.SYMBOL);
        }
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForEachDecimalCharacter() {
        tokenize(".");
        assertNextTokenIs(0, 1, BasicTokenizer.DECIMAL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForEndOfLineComment() {
        String content = "--//this is a comment\n";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length() - 1, BasicTokenizer.COMMENT); // -1 because '\n' is not included
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForEndOfLineCommentThatEndsWithEndOfString() {
        String content = "--//this is a comment";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), BasicTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineComment() {
        String content = "--/*this is a comment*/-";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length() - 1, BasicTokenizer.COMMENT);
        assertNextTokenIs(content.length() - 1, content.length(), BasicTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentAtEndOfContent() {
        String content = "--/*this is a comment*/";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), BasicTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentWithoutTerminatingCharacters() {
        String content = "--/*this is a comment";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), BasicTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForMultiLineCommentWithoutAllTerminatingCharacters() {
        String content = "--/*this is a comment*";
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, content.length(), BasicTokenizer.COMMENT);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedString() {
        String content = "--'this is a single-quoted \n string'-";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(35), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, BasicTokenizer.SINGLE_QUOTED_STRING);
        assertNextTokenIs(36, 37, BasicTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedStringWithEscapedSingleQuoteCharacters() {
        String content = "--'this \"is\" a \\'single-quoted\\' \n string'-";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(41), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 42, BasicTokenizer.SINGLE_QUOTED_STRING);
        assertNextTokenIs(42, 43, BasicTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForSingleQuotedStringAtEndOfContent() {
        String content = "--'this is a single-quoted \n string'";
        assertThat(content.charAt(2), is('\''));
        assertThat(content.charAt(35), is('\''));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, BasicTokenizer.SINGLE_QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test( expected = ParsingException.class )
    public void shouldCreateTokenForSingleQuotedStringWithoutClosingQuote() {
        String content = "--'this is a single-quoted \n string";
        tokenize(content);
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedString() {
        String content = "--\"this is a double-quoted \n string\"-";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(35), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, BasicTokenizer.DOUBLE_QUOTED_STRING);
        assertNextTokenIs(36, 37, BasicTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedStringWithEscapedDoubleQuoteCharacters() {
        String content = "--\"this 'is' a \\\"double-quoted\\\" \n string\"-";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(41), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 42, BasicTokenizer.DOUBLE_QUOTED_STRING);
        assertNextTokenIs(42, 43, BasicTokenizer.SYMBOL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokenForDoubleQuotedStringAtEndOfContent() {
        String content = "--\"this is a double-quoted \n string\"";
        assertThat(content.charAt(2), is('"'));
        assertThat(content.charAt(35), is('"'));
        tokenize(content);
        assertNextTokenIs(0, 1, BasicTokenizer.SYMBOL);
        assertNextTokenIs(1, 2, BasicTokenizer.SYMBOL);
        assertNextTokenIs(2, 36, BasicTokenizer.DOUBLE_QUOTED_STRING);
        assertNoMoreTokens();
    }

    @Test( expected = ParsingException.class )
    public void shouldCreateTokenForDoubleQuotedStringWithoutClosingQuote() {
        String content = "--\"this is a double-quoted \n string";
        tokenize(content);
    }

    @Test
    public void shouldCreateTokensForWordsWithAlphabeticCharacters() {
        String content = "This is a series of words.";
        tokenize(content);
        assertNextTokenIs(0, 4, BasicTokenizer.WORD);
        assertNextTokenIs(5, 7, BasicTokenizer.WORD);
        assertNextTokenIs(8, 9, BasicTokenizer.WORD);
        assertNextTokenIs(10, 16, BasicTokenizer.WORD);
        assertNextTokenIs(17, 19, BasicTokenizer.WORD);
        assertNextTokenIs(20, 25, BasicTokenizer.WORD);
        assertNextTokenIs(25, 26, BasicTokenizer.DECIMAL);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokensForWordsWithNumericCharacters() {
        String content = "1234 4 5353.324";
        tokenize(content);
        assertNextTokenIs(0, 4, BasicTokenizer.WORD);
        assertNextTokenIs(5, 6, BasicTokenizer.WORD);
        assertNextTokenIs(7, 11, BasicTokenizer.WORD);
        assertNextTokenIs(11, 12, BasicTokenizer.DECIMAL);
        assertNextTokenIs(12, 15, BasicTokenizer.WORD);
        assertNoMoreTokens();
    }

    @Test
    public void shouldCreateTokensForWordsWithAlphaNumericCharacters() {
        String content = "123a 5353.324e100";
        tokenize(content);
        assertNextTokenIs(0, 4, BasicTokenizer.WORD);
        assertNextTokenIs(5, 9, BasicTokenizer.WORD);
        assertNextTokenIs(9, 10, BasicTokenizer.DECIMAL);
        assertNextTokenIs(10, 17, BasicTokenizer.WORD);
        assertNoMoreTokens();
    }
}
