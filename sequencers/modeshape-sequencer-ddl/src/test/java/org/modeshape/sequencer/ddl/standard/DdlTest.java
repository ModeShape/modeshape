/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.sequencer.ddl.standard;

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
public class DdlTest {
    
    private Ddl ddl = new Ddl();
    
    public DdlTest() {
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
    public void testCreateTable() {
        ddl.parse("CREATE GLOBAL TEMPORARY TABLE EQPT_TYPE ( CAT_CODE CHAR(7) NOT NULL)");
        assertTrue(ddl.isCompleted());
    }
}