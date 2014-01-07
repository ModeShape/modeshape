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
public class ValidatorTest {
    
    public ValidatorTest() {
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
        Validator v = new Validator();
        v.process("table-definition", "CREATE TABLE EQPT_TBL (CAT_CODE CHAR(7) NOT NULL)");
    }
    
    @Test
    public void testMatch() {
        boolean res = "EQPT_TBL".matches("\\w+");
        System.out.println(res);
    }
}