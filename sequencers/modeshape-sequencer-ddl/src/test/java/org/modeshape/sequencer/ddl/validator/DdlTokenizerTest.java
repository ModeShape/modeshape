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
public class DdlTokenizerTest {
    
    public DdlTokenizerTest() {
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
     * Test of stream method, of class DdlTokenizer.
     */
    @Test
    public void testStream() {
        DdlTokenizer tokenizer = new DdlTokenizer();
        String[] tokens = tokenizer.stream("create table eqpt_tbl (cat_code char(7) not null)");
        for (int i = 0; i < tokens.length; i++) {
            System.out.println(tokens[i]);
        }
        
    }
}