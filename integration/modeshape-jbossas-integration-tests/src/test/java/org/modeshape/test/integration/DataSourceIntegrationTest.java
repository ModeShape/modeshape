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

package org.modeshape.test.integration;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.sql.DataSource;
import static junit.framework.Assert.assertNotNull;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jdbc.ConnectionResultsComparator;
import java.io.File;
import java.sql.Connection;

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
        MavenDependencyResolver mavenResolver = DependencyResolvers.use(MavenDependencyResolver.class)
                                                                   .goOffline()
                                                                   .loadMetadataFromPom("pom.xml")
                                                                   .artifact("org.modeshape:modeshape-jdbc-local:jar:tests")
                                                                   .artifact("org.modeshape:modeshape-jdbc-local:jar")
                                                                   .scope("test");
        return ShrinkWrap.create(WebArchive.class, "ds-test.war")
                         .addAsLibraries(mavenResolver.resolveAsFiles())
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
