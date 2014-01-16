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
public class DdlValidatorTest {
    
    private DdlValidator validator = new DdlValidator();
    
    public DdlValidatorTest() {
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
    public void testWrongCommand() {
        String DDL = "Bla Bla Blas";
        try {
            validator.validate(DDL);
            fail("Wrong command expected");
        } catch (ParsingException e) {
            System.out.println(e.getMessage() + ": " + e.getPosition());
        }
    }
}