/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.shell.cmd;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author kulikov
 */
public class CommandParserTest {
    
    public CommandParserTest() {
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
     * Test of getValue method, of class CommandParser.
     */
    @Test
    public void testGetValue() {
        String cmd = "connect -r My Repository -u anonymous";
        String value = CommandParser.getOptionValue("-r", cmd);
        assertEquals("My", value);
        
        cmd = "connect -r \"My Repository\" -u anonymous";
        assertEquals("My Repository", CommandParser.getOptionValue("-r", cmd));

        cmd = "connect -r \'My Repository\' -u anonymous";
        assertEquals("My Repository", CommandParser.getOptionValue("-r", cmd));
        
        cmd = "connect -r \'My Repository\'";
        assertEquals("My Repository", CommandParser.getOptionValue("-r", cmd));

        cmd = "connect -r My Repository";
        assertEquals("My", CommandParser.getOptionValue("-r", cmd));
    }

}