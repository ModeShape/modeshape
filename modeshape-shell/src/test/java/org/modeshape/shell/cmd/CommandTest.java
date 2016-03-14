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

import org.modeshape.shell.cmd.jcrsession.SessionCommand;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.modeshape.shell.ShellSession;

/**
 *
 * @author kulikov
 */
public class CommandTest {
    
    private final ConnectCmd cmd = new ConnectCmd();
    
    public CommandTest() {
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
     * Test of findParameter method, of class Command.
     */
    @Test
    public void testPrepare() throws Exception {
        cmd.prepare("connect -u user -p password 'My Repository'");
        assertEquals("My Repository", cmd.args(0));

        cmd.prepare("connect 'My Repository'");
        assertEquals("My Repository", cmd.args(0));
        
        cmd.prepare("connect My Repository");
        assertEquals("My", cmd.args(0));
        
        AddCmd addCmd = new AddCmd();
        addCmd.prepare("add node test");

        SessionCommand c = new SessionCommand();
        c.prepare("session refresh -help");

        cmd.prepare("connect --user-name usr --password paswd 'My Repository'");
        
        System.out.println(cmd.optionValue("--user"));
    }

    public class ConnectCmd extends ShellCommand {

        public ConnectCmd() {
            super("connect");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String help() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }


    public class AddCmd extends ShellCommand {

        public AddCmd() {
            super("add");
            addChild(new AddNodeCmd());
            addChild(new AddPropertyCmd());            
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String help() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

    public class AddNodeCmd extends ShellCommand {

        public AddNodeCmd() {
            super("node");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String help() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public class AddPropertyCmd extends ShellCommand {

        public AddPropertyCmd() {
            super("property");
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String help() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
}