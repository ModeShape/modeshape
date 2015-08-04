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
package org.modeshape.test.integration;

import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.sql.Connection;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.sql.DataSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jdbc.ConnectionResultsComparator;

/**
 * Test which verifies that the ModeShape datasource configuration inside of AS7 is correct.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class)
public class DataSourceIntegrationTest {

    /**
     * The name of the WS used by default by the datasource
     */
    private static final String DS_DEFAULT_WORKSPACE = "extra";

    @Resource( mappedName = "datasources/ModeShapeDS" )
    private DataSource modeshapeDS;

    @Resource( mappedName = "/jcr/artifacts" )
    private JcrRepository repository;

    private Connection connection;

    private Session session;
    private Node testRoot;

    @Deployment
    public static WebArchive createDeployment() {
        File[] testDeps = Maven.configureResolver()
                               .workOffline()
                               .loadPomFromFile("pom.xml")
                               .resolve("org.modeshape:modeshape-jdbc-local",
                                        "org.modeshape:modeshape-jdbc-local:test-jar:tests:?")                 
                               .withTransitivity()
                               .asFile();
        return ShrinkWrap.create(WebArchive.class, "ds-test.war")
                         .addAsLibraries(testDeps)
                         .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Before
    public void before() throws Exception {
        assertNotNull(modeshapeDS);
        connection = modeshapeDS.getConnection();
        assertNotNull(connection);
        assertNotNull(repository);

        insertTestData();
    }

    @After
    public void after() throws Exception {
        testRoot.remove();
        session.save();
    }

    private void insertTestData() throws RepositoryException {
        session = repository.login(DS_DEFAULT_WORKSPACE);
        testRoot = session.getRootNode().addNode("testRoot", "nt:unstructured");
        testRoot.addNode("NodeA", "nt:unstructured").setProperty("something", "value3 quick brown fox");
        testRoot.addNode("NodeA", "nt:unstructured").setProperty("something", "value2 quick brown cat");
        testRoot.addNode("NodeB", "nt:unstructured").setProperty("something", "value1 quick black dog");
        session.save();
    }

    @Test
    public void shouldRetrieveDataFromRepositoryAndConfiguredWorkspace() throws Exception {
        assertNotNull(connection.getMetaData());
        String[] expected = {
                "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]",
                "nt:unstructured    null    /testRoot    testRoot",
                "nt:unstructured    null    /testRoot/NodeA    NodeA",
                "nt:unstructured    null    /testRoot/NodeA[2]    NodeA",
                "nt:unstructured    null    /testRoot/NodeB    NodeB"};
        String query = "SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name] FROM [nt:unstructured] ORDER BY [jcr:path]";
        ConnectionResultsComparator.executeTest(connection, query, expected, 5);
    }

}
