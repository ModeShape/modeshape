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

import org.modeshape.shell.cmd.jcrsession.JcrSessionInterpreter;
import java.net.URL;
import javax.jcr.Repository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.shell.ShellI18n;
import org.modeshape.shell.ShellSession;

/**
 * Unit test for shell interpreter
 *
 * @author okulikov
 */
public class JcrSessionInterpreterTest {

    private static ModeShapeEngine engine;
    private static Repository repository;
    private static ShellSession shellSession;
   
    private String sid;
    private JcrSessionInterpreter interpreter = new JcrSessionInterpreter();
    
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

        shellSession = new ShellSession(engine);
        shellSession.setRepository((JcrRepository) repository);
        
        engine.startRepository(repositoryName);
    }

    @AfterClass
    public static void stopRepository() throws Exception {
        System.out.println("Shutting down engine ...");
        engine.shutdown().get();
        System.out.println("Success!");
    }

    @Before
    public void connect() throws Exception{
        shellSession.setJcrSession(shellSession.getRepository().login());
        shellSession.setPath("/");
    }

    @After
    public void disconnect() throws Exception{
//        exec("session exit");
    }
    
    @Test
    public void testChangeNodeCommand() throws Exception {
        String res = exec("cd --help");
        assertEquals(ShellI18n.changeNodeHelp.text(), res);

        res = exec("cd jcr:system");
        assertEquals("", res);

        res = exec("pwd");
        assertEquals("/jcr:system", res);
        System.out.println("Sid=" + sid);
        
        exec("cd ..");
        res = exec("pwd");
        assertEquals("/", res);
    }

    @Test
    public void testPwdCommand() throws Exception {
        String res = exec("pwd --help");
        assertEquals(ShellI18n.pwdHelp.text(), res);

        exec("cd jcr:system");
        res = exec("pwd");
        assertEquals("/jcr:system", res);
    }

    @Test
    public void testQueryCommand() throws Exception {
        String res = exec("query --help");
        assertEquals(ShellI18n.queryHelp.text(), res);

        res = exec("query --lang jcr-sql2 'select * from [nt:base]");
        assertTrue(res.startsWith("nt:base.jcr:primaryType"));
    }

    @Test
    public void testNodeAddMixinCommand() throws Exception {
        String res = exec("node add mixin --help");
        assertEquals(ShellI18n.nodeAddMixinHelp.text(), res);
        
        res = exec("node add node test-node");
        assertEquals("", res);

        res = exec("cd test-node");
        assertEquals("", res);
        
        res = exec("pwd");
        assertEquals("/test-node", res);
        
        res = exec("node add mixin mix:versionable");
        assertEquals("", res);
        
        res = exec("show type mixin").trim();
        assertEquals("mix:versionable", res);
        
    }

    @Test
    public void testNodeAddNodeCommand() throws Exception {
        String res = exec("node add node --help");
        assertEquals(ShellI18n.nodeAddNodeHelp.text(), res);

        res = exec("node add node test-folder --primary-type nt:folder");

        assertEquals("", res);
        res = exec("cd test-folder");
        assertEquals("", res);
        
        res = exec("pwd");
        assertEquals("/test-folder", res);

        res = exec("show type primary");
        assertEquals("nt:folder", res);
    }

    @Test
    public void testNodeShowIndexCommand() throws Exception {
        String res = exec("node show index --help");
        assertEquals(ShellI18n.nodeShowIndexHelp.text(), res);

        res = exec("node show index");
        assertEquals("1", res);
    }

    @Test
    public void testNodeShowIdentifierCommand() throws Exception {
        String res = exec("node show identifier --help");
        assertEquals(ShellI18n.nodeShowIdentifierHelp.text(), res);

        res = exec("node show identifier");
        assertEquals("1", res);
    }

    @Test
    public void testNodeShowPrimaryTypeCommand() throws Exception {
        String res = exec("node show primary-type --help");
        assertEquals("Usage: node show primary-type", res);

        res = exec("node show primary-type");
        assertEquals("mode:root", res);
    }

    @Test
    public void testNodeShowMixinsCommand() throws Exception {
        String res = exec("node show mixins --help");
        assertEquals("Usage: node show mixins", res);

        res = exec("node show mixins");
        assertEquals("<empty set>", res);
    }

    @Test
    public void testNodeShowNodesCommand() throws Exception {
        String res = exec("node show nodes --help");
        assertEquals("Usage: node show nodes", res);

        res = exec("node show nodes");
        assertTrue(res.contains("jcr:system"));
    }

    @Test
    public void testNodeShowPropertiesCommand() throws Exception {
        String res = exec("node show properties --help");
        assertEquals("Usage: node show properties", res);

        res = exec("node show properties");
        assertTrue(res.contains("jcr:uuid"));
    }

    @Test
    public void testNodeShowPropertyCommand() throws Exception {
        String res = exec("node show property --help");
        assertEquals("Usage: node show property <property-name>", res);

        res = exec("node show property jcr:uuid");
        assertTrue(res.length() > 0);
    }

    @Test
    public void testNodeSetPrimaryTypeCommand() throws Exception {
        String res = exec("node set primary-type --help");
        assertEquals("Usage: node set primary-type <type-name>", res);

        exec("node add node f1");
        exec("cd f1");
        exec("node set primary-type nt:folder");
        res = exec("node show primary-type");
        assertEquals("nt:folder", res);
    }

    @Test
    public void testNodeSetPropertyValueCommand() throws Exception {
        String res = exec("node set property-value --help");
        assertEquals("Usage: node set property-value --name <property-name> --value|--values <'property-value'>[,<property-value>]", res);

        exec("node add node f2 --primary-type nt:folder");
        exec("cd f2");
        exec("node add mixin mix:mimeType");
        exec("node set property-value --name jcr:mimeType --value 'text/plain'");
        res = exec("node show property jcr:mimeType");
        assertEquals("text/plain", res);
    }

    @Test
    public void testUploadCommand() throws Exception {
        String res = exec("node upload --help");
        assertEquals("Usage: node upload --property-name <name> --file <binary-data-source>", res);
    }

    @Test
    public void testDownloadCommand() throws Exception {
        String res = exec("node download --help");
        assertEquals("Usage: node download --property-name <name> --file <binary-data-destination>", res);
    }

    @Test
    public void testRepositoryBackupCommand() throws Exception {
        String res = exec("repository backup --help");
        assertEquals("Usage: repository backup [--include-binaries] [--documents-per-file <num>] path", res);

        res = exec("repository backup target/repo.bak");
        assertEquals("", res);        
    }

    @Test
    public void testRepositoryRestoreCommand() throws Exception {
        String res = exec("repository restore --help");
        assertEquals("Usage: repository restore [--include-binaries] [--reindex-on-finish] path", res);

        res = exec("repository backup target/repo-1.bak");
        assertEquals("", res);        

        res = exec("repository restore target/repo-1.bak");
        assertEquals("", res);        
    }

    @Test
    public void testScriptCommand() throws Exception {
        String res = exec("script --help");
        assertEquals("Usage: script -f file-name", res);
    }

    @Test
    public void testSessionRefreshCommand() throws Exception {
        String res = exec("session refresh --help");
        assertEquals("Usage: session refresh [--keep-changes]", res);
        
        exec("node add node f3");
        res = exec("node show nodes");
        assertTrue(res.contains("f3"));
        
        exec("session refresh");
        res = exec("node show nodes");
        assertTrue(!res.contains("f3"));
    }

    @Test
    public void testTypeRegisterCommand() throws Exception {
        String res = exec("ntm register --help");
        assertEquals("Usage: ntm register [--allow-updates] --url <url>", res);

        URL url = JcrSessionInterpreterTest.class.getResource("/cars.cnd");
        res = exec("ntm register --allow-updates --url " + url);
        assertEquals("", res);
        
        res = exec("ntm show types primary");
        assertTrue(res.contains("Car"));
    }

    @Test
    public void testTypeUnregisterCommand() throws Exception {
        String res = exec("ntm unregister --help");
        assertEquals("Usage: ntm unregister --type-name <type-name>", res);

        URL url = JcrSessionInterpreterTest.class.getResource("/cars.cnd");
        res = exec("ntm register --allow-updates --url " + url);
        assertEquals("", res);
        
        res = exec("ntm show types primary");
        assertTrue(res.contains("Car"));
        
        res = exec("ntm unregister --type-name car:Car");
        assertEquals("", res);
        
        res = exec("ntm show types primary");
        assertTrue(!res.contains("Car"));
    }
    

    @Test
    public void testTypeShowPropertyDefsCommand() throws Exception {
        String res = exec("ntm show property-definitions --help");
        assertEquals("Usage: ntm show property-definitions --type-name <name>", res);

        res = exec("ntm show property-definitions --type-name nt:folder");
        assertTrue(res.contains("jcr:created"));
    }

    @Test
    public void testTypeShowNodeDefsCommand() throws Exception {
        String res = exec("ntm show node-definitions --help");
        assertEquals("Usage: ntm show node-definitions --type-name <name>", res);

        res = exec("ntm show node-definitions --type-name nt:folder");
        assertTrue(res.contains("*"));
    }

    @Test
    public void testTypeAcmCommand() throws Exception {
        String res = exec("node add f-acl");
        res = exec("cd f-acl");
        res = exec("acl add --principal ms-user --permissions jcr:all");
        res = exec("acl show");
    }
    
    private String exec(String cmd) throws Exception {
        return interpreter.execute(shellSession, cmd);
    }
}
