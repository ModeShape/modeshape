/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.ddl.standard;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.modeshape.sequencer.ddl.Lexer;
import org.modeshape.sequencer.ddl.Tester;

/**
 *
 * @author kulikov
 */
public class ColumnTypeTest {
    
    private Tester tester = new Tester();
    private ColumnType colType = new ColumnType(tester);
    
    public ColumnTypeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        tester.subordinate(colType);
        tester.reset();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of name method, of class ColumnName.
     */
    @Test
    public void testCharacterType() {
        tester.parse("CHARACTER(30)");
        assertTrue(tester.isSuccess());
        assertEquals("CHARACTER(30)", colType.type());
    }

//    @Test
    public void testInt() {
        tester.parse("int");
        assertTrue(tester.isSuccess());
        assertEquals("int", colType.type());
    }

//    @Test
    public void testInteger() {
        tester.parse("integer");
        assertTrue(tester.isSuccess());
        assertEquals("integer", colType.type());
    }

//    @Test
    public void testSamllInt() {
        tester.parse("smallint");
        assertTrue(tester.isSuccess());
        assertEquals("smallint", colType.type());
    }

//    @Test
    public void testReal() {
        tester.parse("real");
        assertTrue(tester.isSuccess());
        assertEquals("real", colType.type());
    }

//    @Test
    public void testDate() {
        tester.parse("date");
        assertTrue(tester.isSuccess());
        assertEquals("date", colType.type());
    }
    
//    @Test
    public void testTime() {
        tester.parse("time");
        assertTrue(tester.isSuccess());
        assertEquals("time", colType.type());
    }
    
//    @Test
    public void testBoolean() {
        tester.parse("boolean");
        assertTrue(tester.isSuccess());
        assertEquals("boolean", colType.type());
    }
    
//    @Test
    public void testBlob() {
        tester.parse("blob");
        assertTrue(tester.isSuccess());
        assertEquals("blob", colType.type());
    }
    
    
}