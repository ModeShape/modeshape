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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.modeshape.common.text.TokenStream.Tokenizer;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DdlTokenStreamTest extends DdlParserTestHelper {
    public static final int WORD = DdlTokenStream.DdlTokenizer.WORD;
    public static final int SYMBOL = DdlTokenStream.DdlTokenizer.SYMBOL;
    public static final int DECIMAL = DdlTokenStream.DdlTokenizer.DECIMAL;
    public static final int SINGLE_QUOTED_STRING = DdlTokenStream.DdlTokenizer.SINGLE_QUOTED_STRING;
    public static final int DOUBLE_QUOTED_STRING = DdlTokenStream.DdlTokenizer.DOUBLE_QUOTED_STRING;
    public static final int COMMENT = DdlTokenStream.DdlTokenizer.COMMENT;
    public static final int KEYWORD = DdlTokenStream.DdlTokenizer.KEYWORD;
    public static final int STATEMENT_KEY = DdlTokenStream.DdlTokenizer.STATEMENT_KEY;
    
    public static final String CREATE = "CREATE";
    public static final String ALTER = "ALTER";
    public static final String DROP = "DROP";
    public static final String PUNT = "PUNT";
    public static final String FOOBAR = "FOOBAR";
    public static final String[] STMT_CREATE_FOOBAR = {CREATE, FOOBAR};
    public static final String[] STMT_ALTER_FOOBAR = {ALTER, FOOBAR};
    public static final String[] STMT_DROP_FOOBAR = {DROP, FOOBAR};
    public static final String[] STMT_PUNT_FOOBAR = {PUNT, FOOBAR};
    
    
    private final static String[] CUSTOM_KEYWORDS = {
		"CREATE",
		"ALTER",
		"DROP",
		"PUNT",
		"FOOBAR"
	};
    
    private Tokenizer tokenizer;

	@Before
	public void beforeEach() {
		tokenizer = DdlTokenStream.ddlTokenizer(true);
		setPrintToConsole(false);
	}
    
    private DdlTokenStream getTokens(String content) {
    	DdlTokenStream tokens = new DdlTokenStream(content, tokenizer, false);
    	
    	return tokens;
    }
    
    @Test
    public void shouldRegisterKeyWords() {
    	printTest("shouldRegisterKeyWords()");
    	String content = "CREATE FOOBAR with NO WORDS";
    	
    	DdlTokenStream tokens = getTokens(content);
    	tokens.registerKeyWords(CUSTOM_KEYWORDS);
    	
    	// Do the work
    	tokens.start();
    	
    	// Check results
    	assertTrue(tokens.isKeyWord(CREATE));
    	assertTrue(tokens.isKeyWord(ALTER));
    	assertTrue(tokens.isKeyWord(DROP));
    	assertTrue(tokens.isKeyWord(PUNT));
    	assertTrue(tokens.isKeyWord(FOOBAR));
    	assertFalse(tokens.isKeyWord("BARNYARD"));
    	
    	assertTrue(tokens.matches(CREATE, FOOBAR));
    	assertFalse(tokens.matches(PUNT, FOOBAR));
    	assertTrue(tokens.matches(KEYWORD));
    	assertTrue(tokens.matches(WORD));
    	
    	assertTrue(tokens.isNextKeyWord());
    	assertTrue(tokens.canConsume(CREATE)); // CREATE
    	
    	assertTrue(tokens.isNextKeyWord());
    	assertTrue(tokens.canConsume(FOOBAR)); // FOOBAR
    	assertThat(tokens.nextPosition().getIndexInContent(), is(14));
    	assertFalse(tokens.isNextKeyWord()); // "with"

    	
    }
    
    @Test
    public void shouldRegisterStatementStarts() {
    	printTest("shouldRegisterStatementStarts()");
    	String content = "CREATE FOOBAR with NO WORDS";
    	
    	DdlTokenStream tokens = getTokens(content);
    	
    	// SET UP
    	tokens.registerKeyWords(CUSTOM_KEYWORDS);
    	tokens.registerStatementStartPhrase(STMT_CREATE_FOOBAR);
    	tokens.registerStatementStartPhrase(STMT_ALTER_FOOBAR);
    	tokens.registerStatementStartPhrase(STMT_DROP_FOOBAR);
    	tokens.registerStatementStartPhrase(STMT_PUNT_FOOBAR);

    	// Do the work
    	tokens.start();
    	
    	
    	// Check Results
    	assertTrue(tokens.isNextStatementStart());
    	
    	assertTrue(tokens.matches(CREATE, FOOBAR));
    	assertTrue(tokens.matches(STMT_CREATE_FOOBAR));
    	assertFalse(tokens.matches(PUNT, FOOBAR));
    	assertFalse(tokens.matches(STMT_ALTER_FOOBAR));
    	assertTrue(tokens.matches(KEYWORD));
    	assertTrue(tokens.matches(STATEMENT_KEY));
    	assertTrue(tokens.matches(WORD));
    }
    
    
    @Test
    public void shouldMarkPosition() {
    	printTest("shouldMarkPosition()");
    	String content = "CREATE FOOBAR with NO WORDS";
    	
    	DdlTokenStream tokens = getTokens(content);
    	// Do the work
    	tokens.start();
    	
    	tokens.consume("CREATE");
    	tokens.mark();
    	tokens.consume();
    	tokens.consume();
    	tokens.consume();
    	String stringFragment = tokens.getMarkedContent();
    	
    	assertThat(stringFragment, is("FOOBAR with NO "));
    }
    
    @Test
    public void shouldMarkPositionWithMultilineContent() {
    	printTest("shouldMarkPositionWithMultilineContent()");
    	String content = "CREATE FOOBAR with NO WORDS;\n"
    		+ "CREATE BARFOO with lots of words after it;\n"
    		+ "CREATE TABLE myTable (column definition data);";
    	
    	String fragment =  "with lots of words after it;\n"
    			+ "CREATE TABLE myTable (column definition data);";
    	
    	DdlTokenStream tokens = getTokens(content);
    	// Do the work
    	tokens.start();
    	
    	while( tokens.hasNext() ) {
    		String lastToken = tokens.consume();
    		if( lastToken.equalsIgnoreCase("BARFOO")) {
    			tokens.mark();
    		}
    	}
    	
    	String stringFragment = tokens.getMarkedContent();
    	
    	assertThat(stringFragment, is(fragment));
    }
    
    @Test
    public void shouldFindNextPositionStartIndex() {
    	printTest("shouldFindNextPositionStartIndex()");
    	String content = "Select all columns from this table";
    	
    	DdlTokenStream tokens = getTokens(content);
    	// Do the work
    	tokens.start();
    	// "Select all columns from this table";
    	assertThat(tokens.nextPosition().getIndexInContent(), is(0));
    	assertThat(tokens.nextPosition().getColumn(), is(1));
    	assertThat(tokens.nextPosition().getLine(), is(1));
    	tokens.consume();
    	// Next position should be line 1, column 8
    	assertThat(tokens.nextPosition().getIndexInContent(), is(7));
    	assertThat(tokens.nextPosition().getColumn(), is(8));
    	assertThat(tokens.nextPosition().getLine(), is(1));
    }
    
    @Test
    public void shouldParseMultiLineString() {
    	printTest("shouldParseMultiLineString()");
    	
    	String content = "ALTER DATABASE \n"
    			+ "DO SOMETHING; \n"
    			+ "ALTER DATABASE \n"
    			+ "      SET DEFAULT BIGFILE TABLESPACE;";
    	DdlTokenStream tokens = getTokens(content);
    	// Do the work
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
