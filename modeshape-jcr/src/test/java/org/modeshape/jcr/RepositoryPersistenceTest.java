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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.JcrTools;

/**
 * A test the verifies that a repository will persist content (including binaries).
 */
public class RepositoryPersistenceTest extends MultiPassAbstractTest {

    @Test
    public void shouldPersistBinariesAcrossRestart() throws Exception {
        String repositoryConfigFile = "config/repo-config-persistent-cache.json";
        File persistentFolder = new File("target/persistent_repository");
        // remove all persisted content ...
        FileUtil.delete(persistentFolder);
        assertDataPersistenceAcrossRestarts(repositoryConfigFile);
    }

    private void assertDataPersistenceAcrossRestarts( String repositoryConfigFile ) throws Exception {
        final List<File> testFiles = new ArrayList<>();
        final Map<String, Long> testFileSizesInBytes = new HashMap<>();
        testFiles.add(getFile("mimetype/test.xml"));
        testFiles.add(getFile("mimetype/modeshape.doc"));
        testFiles.add(getFile("mimetype/log4j.properties"));
        for (File testFile : testFiles) {
            assertThat(testFile.getPath() + " should exist", testFile.exists(), is(true));
            assertThat(testFile.getPath() + " should be a file", testFile.isFile(), is(true));
            assertThat(testFile.getPath() + " should be readable", testFile.canRead(), is(true));
            testFileSizesInBytes.put(testFile.getName(), testFile.length());
        }


        final JcrTools tools = new JcrTools();

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {
                Session session = repository.login();

                // Add some content ...
                session.getRootNode().addNode("testNode");
                for (File testFile : testFiles) {
                    String name = testFile.getName();
                    Node fileNode = tools.uploadFile(session, "/testNode/" + name, testFile);
                    Binary binary = fileNode.getNode("jcr:content").getProperty("jcr:data").getBinary();
                    assertThat(binary.getSize(), is(testFileSizesInBytes.get(name)));
                }

                session.save();

                Node testNode = session.getNode("/testNode");
                for (File testFile : testFiles) {
                    String name = testFile.getName();
                    Node fileNode = testNode.getNode(name);
                    assertThat(fileNode, is(notNullValue()));
                    Binary binary = fileNode.getNode("jcr:content").getProperty("jcr:data").getBinary();
                    assertThat(binary.getSize(), is(testFileSizesInBytes.get(name)));
                }

                Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:file]", Query.JCR_SQL2);
                QueryResult results = query.execute();
                NodeIterator iter = results.getNodes();
                while (iter.hasNext()) {
                    Node fileNode = iter.nextNode();
                    assertThat(fileNode, is(notNullValue()));
                    String name = fileNode.getName();
                    Binary binary = fileNode.getNode("jcr:content").getProperty("jcr:data").getBinary();
                    assertThat(binary.getSize(), is(testFileSizesInBytes.get(name)));
                }

                session.logout();
                return null;
            }
        }, repositoryConfigFile);

        startRunStop(new RepositoryOperation() {
            @Override
            public Void call() throws Exception {

                Session session = repository.login();
                assertNotNull(session.getNode("/testNode"));

                for (File testFile : testFiles) {
                    String name = testFile.getName();
                    Node fileNode = session.getNode("/testNode/" + name);
                    assertNotNull(fileNode);
                    Binary binary = fileNode.getNode("jcr:content").getProperty("jcr:data").getBinary();
                    assertThat(binary.getSize(), is(testFileSizesInBytes.get(name)));
                }

                Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:file]", Query.JCR_SQL2);
                QueryResult results = query.execute();
                NodeIterator iter = results.getNodes();
                while (iter.hasNext()) {
                    Node fileNode = iter.nextNode();
                    String name = fileNode.getName();
                    Binary binary = fileNode.getNode("jcr:content").getProperty("jcr:data").getBinary();
                    assertThat(binary.getSize(), is(testFileSizesInBytes.get(name)));
                }

                session.logout();

                return null;
            }
        }, repositoryConfigFile);
    }

    @Test
    @Ignore("Should only be used manually when needed")
    public void shouldPersistDataInSQLServer2008() throws Exception {
        //make sure the DB is clean (empty) when running this test; there is no effective teardown
        assertDataPersistenceAcrossRestarts("config/repo-config-sqlserver2008.json");
    }

    protected File getFile( String resourcePath ) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        assertNotNull(resourcePath + " not found", resource);
        return new File(resource.toURI());
    }
}
