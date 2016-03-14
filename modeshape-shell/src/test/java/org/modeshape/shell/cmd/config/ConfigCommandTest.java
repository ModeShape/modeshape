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
package org.modeshape.shell.cmd.config;

import java.lang.management.ManagementFactory;
import java.net.URL;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.cmd.JcrSessionInterpreterTest;

/**
 *
 * @author kulikov
 */
public class ConfigCommandTest {
    
    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();
    private static ObjectName mBeanName;
    private static ModeShapeEngine engine;
    private static JcrRepository repository;
    
    public ConfigCommandTest() {
    }
    
    @BeforeClass
    public static void before() throws Exception {
        engine = new ModeShapeEngine();
        engine.start();

        // Load the configuration for a repository via the classloader (can also use path to a file)...
        repository = null;
        String repositoryName = null;

        URL url = JcrSessionInterpreterTest.class.getClassLoader().getResource("my-repository-config.json");
        RepositoryConfiguration config = RepositoryConfiguration.read(url);

        // Deploy the repository ...
        repository = engine.deploy(config);
        repositoryName = config.getName();

    }

    @AfterClass
    public static void stopRepository() throws Exception {
        System.out.println("Shutting down engine ...");
        engine.shutdown().get();
        System.out.println("Success!");
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testConfigurationCommand() throws Exception {
        ShellSession session = new ShellSession(engine);
        session.setRepositoryConfiguration(repository.getConfiguration());
        session.setInterpreter(ShellSession.CONFIGURATION_INTERPRETER);
        String res = ShellSession.CONFIGURATION_INTERPRETER.execute(session, "show workspaces default");
        assertEquals("default", res);
        
        ShellSession.CONFIGURATION_INTERPRETER.execute(session, "set workspaces default xxx");
        res = ShellSession.CONFIGURATION_INTERPRETER.execute(session, "show workspaces default");
        assertEquals("xxx", res);
    }
    
//    @Test
    public void testSomeMethod() throws Exception {
        RepositoryConfiguration cfg = engine.getRepositoryConfiguration("My Repository");
        System.out.println(cfg);
/*        EditableDocument cfgEditor = cfg.edit().editable();
        System.out.println(cfgEditor.get("name"));
        System.out.println(cfgEditor.get("shell"));
        
        Object e2 = cfgEditor.get("workspaces");
        EditableDocument ee2 = null;
        if (e2 instanceof Document) {
            ee2 = ((Document)e2).editable();
        }
  */      
        String[] segments = new String[]{"workspaces", "predefined"};
        EditableDocument doc0 = cfg.edit().editable();
        EditableDocument doc = doc0;
        for (int i = 0; i < segments.length - 1; i++) {
            Object obj = doc.getDocument(segments[i]);
            if (obj == null) {
                obj = DocumentFactory.newDocument();
            }
            doc.set(segments[i], obj);
            doc = ((Document) obj).editable();
        }
        
//        doc.set(segments[segments.length - 1], "true");
//        RepositoryConfiguration cfg1 = new RepositoryConfiguration(doc0, cfg.getName());
        String s = doc.get(segments[segments.length - 1]).toString();
        System.out.println(s);
    }
}