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
public class RefTest {
    
    public RefTest() {
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
     * Test of matches method, of class Ref.
     */
    @Test
    public void testMatches() {
        Sequence sequence = new Sequence("test", new Ref("CREATE"), new Ref("GLOBAL"), new Ref("TEMPORARY"));
        int offset = sequence.matches(new String[]{"CREATE", "GLOBAL", "TEMPORARY"}, 0);
        
        System.out.println("offset=" + offset);
    }
}