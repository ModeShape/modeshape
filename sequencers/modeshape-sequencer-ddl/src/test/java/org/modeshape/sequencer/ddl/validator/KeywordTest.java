/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.sequencer.ddl.validator;

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
public class KeywordTest {
    
    public KeywordTest() {
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
     * Test of matches method, of class Keyword.
     */
    @Test
    public void testMatches() {
        Keyword kwd = new Keyword("CREATE");
        int offset = kwd.matches(new String[] {"CREATE", "TABLE", "EQPT"}, 0);
        assertEquals(1, offset);
    }
}