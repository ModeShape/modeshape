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
package org.modeshape.common.text;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import org.modeshape.common.text.TokenStream.BasicTokenizer;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class TokenStreamTest {
    public static final int WORD = TokenStream.BasicTokenizer.WORD;
    public static final int SYMBOL = TokenStream.BasicTokenizer.SYMBOL;
    public static final int DECIMAL = TokenStream.BasicTokenizer.DECIMAL;
    public static final int SINGLE_QUOTED_STRING = TokenStream.BasicTokenizer.SINGLE_QUOTED_STRING;
    public static final int DOUBLE_QUOTED_STRING = TokenStream.BasicTokenizer.DOUBLE_QUOTED_STRING;
    public static final int COMMENT = TokenStream.BasicTokenizer.COMMENT;

    private Tokenizer tokenizer;
    private String content;
    private TokenStream tokens;

    @Before
    public void beforeEach() {
        tokenizer = TokenStream.basicTokenizer(false);
        content = "Select all columns from this table";
        makeCaseInsensitive();
    }

    public void makeCaseSensitive() {
        tokens = new TokenStream(content, tokenizer, true);
        tokens.start();
    }

    public void makeCaseInsensitive() {
        tokens = new TokenStream(content, tokenizer, false);
        tokens.start();
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowConsumeBeforeStartIsCalled() {
        tokens = new TokenStream(content, TokenStream.basicTokenizer(false), false);
        tokens.consume("Select");
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowHasNextBeforeStartIsCalled() {
        tokens = new TokenStream(content, TokenStream.basicTokenizer(false), false);
        tokens.hasNext();
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowMatchesBeforeStartIsCalled() {
        tokens = new TokenStream(content, TokenStream.basicTokenizer(false), false);
        tokens.matches("Select");
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowCanConsumeBeforeStartIsCalled() {
        tokens = new TokenStream(content, TokenStream.basicTokenizer(false), false);
        tokens.canConsume("Select");
    }

    @Test
    public void shouldReturnTrueFromHasNextIfThereIsACurrentToken() {
        content = "word";
        makeCaseSensitive();
        assertThat(tokens.currentToken().matches("word"), is(true));
        assertThat(tokens.hasNext(), is(true));
    }

    @Test
    public void shouldConsumeInCaseSensitiveMannerWithExpectedValuesWhenMatchingExactCase() {
        makeCaseSensitive();
        tokens.consume("Select");
        tokens.consume("all");
        tokens.consume("columns");
        tokens.consume("from");
        tokens.consume("this");
        tokens.consume("table");
        assertThat(tokens.hasNext(), is(false));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToConsumeInCaseSensitiveMannerWithExpectedValuesWhenMatchingIncorrectCase() {
        makeCaseSensitive();
        tokens.consume("Select");
        tokens.consume("all");
        tokens.consume("Columns");
    }

    @Test
    public void shouldConsumeInCaseInsensitiveMannerWithExpectedValuesWhenMatchingNonExactCase() {
        makeCaseInsensitive();
        tokens.consume("SELECT");
        tokens.consume("ALL");
        tokens.consume("COLUMNS");
        tokens.consume("FROM");
        tokens.consume("THIS");
        tokens.consume("TABLE");
        assertThat(tokens.hasNext(), is(false));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToConsumeInCaseInsensitiveMannerWithExpectedValuesWhenMatchingStringIsInLowerCase() {
        makeCaseInsensitive();
        tokens.consume("SELECT");
        tokens.consume("ALL");
        tokens.consume("columns");
    }

    @Test
    public void shouldReturnTrueFromCanConsumeWithCaseSensitiveTokenStreamIfMatchStringDoesMatchCaseExactly() {
        makeCaseSensitive();
        assertThat(tokens.canConsume("Select"), is(true));
        assertThat(tokens.canConsume("all"), is(true));
        assertThat(tokens.canConsume("columns"), is(true));
        assertThat(tokens.canConsume("from"), is(true));
        assertThat(tokens.canConsume("this"), is(true));
        assertThat(tokens.canConsume("table"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnFalseFromCanConsumeWithCaseSensitiveTokenStreamIfMatchStringDoesNotMatchCaseExactly() {
        makeCaseSensitive();
        assertThat(tokens.canConsume("Select"), is(true));
        assertThat(tokens.canConsume("all"), is(true));
        assertThat(tokens.canConsume("Columns"), is(false));
        assertThat(tokens.canConsume("COLUMNS"), is(false));
        assertThat(tokens.canConsume("columns"), is(true));
        assertThat(tokens.canConsume("from"), is(true));
        assertThat(tokens.canConsume("THIS"), is(false));
        assertThat(tokens.canConsume("table"), is(false));
        assertThat(tokens.canConsume("this"), is(true));
        assertThat(tokens.canConsume("table"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeWithCaseSensitiveTokenStreamIfSuppliedTypeDoesMatch() {
        makeCaseSensitive();
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnFalseFromCanConsumeWithCaseSensitiveTokenStreamIfSuppliedTypeDoesMatch() {
        makeCaseSensitive();
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(COMMENT), is(false));
        assertThat(tokens.canConsume(SINGLE_QUOTED_STRING), is(false));
        assertThat(tokens.canConsume(DOUBLE_QUOTED_STRING), is(false));
        assertThat(tokens.canConsume(DECIMAL), is(false));
        assertThat(tokens.canConsume(SYMBOL), is(false));

        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromMatchesWithCaseSensitiveTokenStreamIfMatchStringDoesMatchCaseExactly() {
        makeCaseSensitive();
        assertThat(tokens.matches("Select"), is(true));
        assertThat(tokens.matches("select"), is(false));
        assertThat(tokens.canConsume("Select"), is(true));
        assertThat(tokens.matches("all"), is(true));
        assertThat(tokens.canConsume("all"), is(true));
    }

    @Test
    public void shouldReturnFalseFromMatchesWithCaseSensitiveTokenStreamIfMatchStringDoesMatchCaseExactly() {
        makeCaseSensitive();
        assertThat(tokens.matches("select"), is(false));
        assertThat(tokens.matches("SElect"), is(false));
        assertThat(tokens.matches("Select"), is(true));
    }

    @Test
    public void shouldReturnFalseFromCanConsumeWithCaseInsensitiveTokenStreamIfMatchStringIsNotUppercase() {
        makeCaseInsensitive();
        assertThat(tokens.canConsume("Select"), is(false));
        assertThat(tokens.canConsume("SELECT"), is(true));
        assertThat(tokens.canConsume("aLL"), is(false));
        assertThat(tokens.canConsume("all"), is(false));
        assertThat(tokens.canConsume("ALL"), is(true));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeWithCaseInsensitiveTokenStreamIfMatchStringDoesNotMatchCaseExactly() {
        makeCaseInsensitive();
        assertThat(tokens.canConsume("SELECT"), is(true));
        assertThat(tokens.canConsume("ALL"), is(true));
        assertThat(tokens.canConsume("COLUMNS"), is(true));
        assertThat(tokens.canConsume("FROM"), is(true));
        assertThat(tokens.canConsume("THIS"), is(true));
        assertThat(tokens.canConsume("TABLE"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeWithCaseInsensitiveTokenStreamIfSuppliedTypeDoesMatch() {
        makeCaseInsensitive();
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnFalseFromCanConsumeWithCaseInsensitiveTokenStreamIfSuppliedTypeDoesMatch() {
        makeCaseInsensitive();
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(COMMENT), is(false));
        assertThat(tokens.canConsume(SINGLE_QUOTED_STRING), is(false));
        assertThat(tokens.canConsume(DOUBLE_QUOTED_STRING), is(false));
        assertThat(tokens.canConsume(DECIMAL), is(false));
        assertThat(tokens.canConsume(SYMBOL), is(false));

        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.canConsume(WORD), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromMatchesWithCaseInsensitiveTokenStreamIfMatchStringIsUppercaseAndMatches() {
        makeCaseInsensitive();
        assertThat(tokens.matches("SELECT"), is(true));
        assertThat(tokens.canConsume("SELECT"), is(true));
        assertThat(tokens.matches("ALL"), is(true));
        assertThat(tokens.canConsume("ALL"), is(true));
    }

    @Test
    public void shouldReturnFalseFromMatchesWithCaseInsensitiveTokenStreamIfMatchStringIsUppercaseAndDoesNotMatch() {
        makeCaseInsensitive();
        assertThat(tokens.matches("ALL"), is(false));
        assertThat(tokens.matches("SElect"), is(false));
        assertThat(tokens.matches("SELECT"), is(true));
    }

    @Test
    public void shouldConsumeMultipleTokensIfTheyMatch() {
        makeCaseInsensitive();
        tokens.consume("SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE");
        assertThat(tokens.hasNext(), is(false));
    }

    @Test( expected = ParsingException.class )
    public void shouldFailToConsumeMultipleTokensIfTheyDoNotMatch() {
        makeCaseInsensitive();
        tokens.consume("SELECT", "ALL", "COLUMNS", "FROM", "TABLE");
    }

    @Test
    public void shouldReturnTrueFromCanConsumeMultipleTokensIfTheyAllMatch() {
        makeCaseInsensitive();
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeArrayOfTokensIfTheyAllMatch() {
        makeCaseInsensitive();
        assertThat(tokens.matches(new String[] {"SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE"}), is(true));
        assertThat(tokens.canConsume(new String[] {"SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE"}), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeMultipleTokensIfTheyDoNotAllMatch() {
        makeCaseInsensitive();
        // Unable to consume unless they all match ...
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS", "FRM", "THIS", "TABLE"), is(false));
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE", "EXTRA"), is(false));
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS", "FROM", "EXTRA", "THIS", "TABLE"), is(false));
        assertThat(tokens.hasNext(), is(true));
        // Should have consumed nothing so far ...
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS"), is(true));
        assertThat(tokens.canConsume("FROM", "THIS", "TABLE"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromMatchAnyOfIfAnyOfTheTokenValuesMatch() {
        makeCaseInsensitive();
        // Unable to consume unless they all match ...
        assertThat(tokens.matchesAnyOf("ALL", "COLUMNS"), is(false));
        assertThat(tokens.matchesAnyOf("ALL", "COLUMNS", "SELECT"), is(true));
        tokens.consume("SELECT");
        assertThat(tokens.matchesAnyOf("ALL", "COLUMNS", "SELECT"), is(true));
        tokens.consume("ALL");
        assertThat(tokens.matchesAnyOf("ALL", "COLUMNS", "SELECT"), is(true));
        tokens.consume("COLUMNS");
        assertThat(tokens.canConsume("FROM", "THIS", "TABLE"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }
    
    @Test
    public void shouldReturnTrueFromMatchIfAllTypeValuesMatch() {
        makeCaseInsensitive();
        assertThat(tokens.matches(BasicTokenizer.WORD, BasicTokenizer.WORD), is(true));
    }
    
    @Test
    public void shouldReturnFalseFromMatchIfAllTypeValuesDoNotMatch() {
        makeCaseInsensitive();
        assertThat(tokens.matches(BasicTokenizer.WORD, BasicTokenizer.DECIMAL), is(false));
        assertThat(tokens.matches(BasicTokenizer.DECIMAL, BasicTokenizer.WORD), is(false));
    }

    @Test
    public void shouldConsumeMultipleTokensWithAnyValueConstant() {
        makeCaseInsensitive();
        // Unable to consume unless they all match ...
        tokens.consume("SELECT", "ALL", TokenStream.ANY_VALUE);
        tokens.consume("FROM", "THIS", "TABLE");
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldConsumeTokenWithAnyValueConstant() {
        makeCaseInsensitive();
        // Unable to consume unless they all match ...
        tokens.consume("SELECT", "ALL");
        tokens.consume(TokenStream.ANY_VALUE);
        tokens.consume("FROM", "THIS", "TABLE");
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldReturnTrueFromCanConsumeMultipleTokensWithAnyValueConstant() {
        makeCaseInsensitive();
        // Unable to consume unless they all match ...
        assertThat(tokens.canConsume("SELECT", "ALL", TokenStream.ANY_VALUE, "FRM", "THIS", "TABLE"), is(false));
        assertThat(tokens.canConsume("SELECT", "ALL", "COLUMNS", "FROM", TokenStream.ANY_VALUE, "TABLE"), is(true));
        assertThat(tokens.hasNext(), is(false));
    }

    @Test
    public void shouldCanConsumeSingleAfterTokensCompleteFromCanConsumeStringList() {
        makeCaseInsensitive();
        // consume ALL the tokens using canConsume()
        tokens.canConsume("SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE");
        // try to canConsume() single word
        assertThat(tokens.canConsume("SELECT"), is(false));
        assertThat(tokens.canConsume(TokenStream.ANY_VALUE), is(false));
        assertThat(tokens.canConsume(BasicTokenizer.SYMBOL), is(false));
    }

    @Test
    public void shouldCanConsumeStringAfterTokensCompleteFromCanConsumeStringArray() {
        makeCaseInsensitive();
        // consume ALL the tokens using canConsume()
        tokens.canConsume(new String[] {"SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE"});
        // try to canConsume() single word
        assertThat(tokens.canConsume("SELECT"), is(false));
        assertThat(tokens.canConsume(TokenStream.ANY_VALUE), is(false));
        assertThat(tokens.canConsume(BasicTokenizer.SYMBOL), is(false));
    }

    @Test
    public void shouldCanConsumeStringAfterTokensCompleteFromCanConsumeStringIterator() {
        makeCaseInsensitive();
        // consume ALL the tokens using canConsume()
        tokens.canConsume(Arrays.asList(new String[] {"SELECT", "ALL", "COLUMNS", "FROM", "THIS", "TABLE"}));
        // try to canConsume() single word
        assertThat(tokens.canConsume("SELECT"), is(false));
        assertThat(tokens.canConsume(TokenStream.ANY_VALUE), is(false));
        assertThat(tokens.canConsume(BasicTokenizer.SYMBOL), is(false));
    }
    
    @Test
    public void shouldFindNextPositionStartIndex() {
    	makeCaseInsensitive();
    	// "Select all columns from this table";
    	tokens.consume();
    	// Next position should be line 1, column 8
    	assertThat(tokens.nextPosition().getIndexInContent(), is(7));
    	assertThat(tokens.nextPosition().getColumn(), is(8));
    	assertThat(tokens.nextPosition().getLine(), is(1));
    }
    
    @Test
    public void shouldFindPreviousPositionStartIndex() {
    	makeCaseInsensitive();
    	// "Select all columns from this table";
    	tokens.consume();
    	tokens.consume();
    	// previous position should be line 1, column 8
    	assertThat(tokens.previousPosition().getIndexInContent(), is(7));
    	assertThat(tokens.previousPosition().getColumn(), is(8));
    	assertThat(tokens.previousPosition().getLine(), is(1));
    }
    
    @Test
    public void shouldParseMultiLineString() {
    	makeCaseInsensitive();
    	String content = "ALTER DATABASE \n"
    			+ "DO SOMETHING; \n"
    			+ "ALTER DATABASE \n"
    			+ "      SET DEFAULT BIGFILE TABLESPACE;";
        tokens = new TokenStream(content, tokenizer, true);
        tokens.start();

    	tokens.consume(); // LINE
    	tokens.consume(); // ONE
    	tokens.consume(); // DO
    	tokens.consume(); // SOMETHING
    	tokens.consume(); // ;
    	
    	assertThat(tokens.nextPosition().getIndexInContent(), is(31));
    	assertThat(tokens.nextPosition().getColumn(), is(1));
    	tokens.consume(); // ALTER
    	assertThat(tokens.nextPosition().getIndexInContent(), is(37));
    	assertThat(tokens.nextPosition().getColumn(), is(7));

    }
}
