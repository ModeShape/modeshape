/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.sequencer.ddl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.modeshape.common.text.ParsingException;

/**
 *
 * @author kulikov
 */
public class StandardLexerTest {
    private Lexer lexer = new StandardLexer();
    
    public StandardLexerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testUnknownCommand() {
        String DDL = "Bla Bla Bla";
        try {
            lexer.parse(DDL);
            fail("Command unknown");
        } catch (ParsingException e) {
            assertEquals(0, e.getPosition().getColumn());
            assertEquals(1, e.getPosition().getLine());
            assertEquals("Wrong command name: Bla", e.getMessage());
        }
        
    }

    @Test
    public void digitAsFirstCharacter_Command() {
        String DDL = "1REATE";
        try {
            lexer.parse(DDL);
            fail("Command unknown");
        } catch (ParsingException e) {
            assertEquals(0, e.getPosition().getColumn());
            assertEquals(1, e.getPosition().getLine());
            assertEquals("Digits are not allowed here: 1", e.getMessage());
        }
        
    }
    
    /**
     * Test of commandAnalysis method, of class StandardLexer.
     */
    @Test
    public void testCreateCommand() {
        String DDL = "CREATE GLOBAL TEMPORARY TABLE EQPT_TYPE ( CAT_CODE CHAR(7) NOT NULL);";
        lexer.parse(DDL);
    }

    
    @Test
    public void testCreateStatement() {
        String DDL = "CREATE GLOBAL TEMPORARY TABLE EQPT_TYPE ( CAT_CODE CHAR(7) NOT NULL);";
        lexer.parse(DDL);
    }
    
}