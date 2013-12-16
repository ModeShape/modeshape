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

/**
 *
 * @author kulikov
 */
public class StandardLexerTest {
    private Checker checker = new Checker();
    private Lexer lexer = new StandardLexer(checker);
    
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

    /**
     * Test of commandAnalysis method, of class StandardLexer.
     */
    @Test
    public void testNoErrors() {
        String DDL = "CREATE GLOBAL TEMPORARY TABLE EQPT_TYPE ( CAT_CODE CHAR(7) NOT NULL);";
        checker.message = null;
        lexer.parse(DDL);
        
        assertTrue(checker.message == null);
    }

    public class Checker implements Lexer.ErrorListener {

        private String message;
        
        @Override
        public void onError(String message) {
            this.message = message;
        }
        
    }
}