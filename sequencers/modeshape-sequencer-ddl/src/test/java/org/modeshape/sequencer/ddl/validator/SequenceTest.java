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
public class SequenceTest {
    
    public SequenceTest() {
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
     * Test of matches method, of class Sequence.
     */
    @Test
    public void testMatches() {
        Sequence seq = new Sequence("module", new Keyword("CREATE"), new Keyword("TABLE"));
        int offset = seq.matches(new String[]{"CREATE", "TABLE"}, 0);
        assertEquals(2, offset);
    }
}