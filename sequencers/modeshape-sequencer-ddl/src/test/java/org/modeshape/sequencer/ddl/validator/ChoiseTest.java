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
public class ChoiseTest {
    
    public ChoiseTest() {
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
     * Test of matches method, of class Choise.
     */
    @Test
    public void testMatches() {
        Choise choise = new Choise("choise", new Keyword("GLOBAL"), new Keyword("LOCAL"));
        int offset = choise.matches(new String[]{"GLOBAL"}, 0);
        assertEquals(1, offset);
    }
}