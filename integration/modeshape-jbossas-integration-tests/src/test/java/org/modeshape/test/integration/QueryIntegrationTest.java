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

import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
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
import org.modeshape.jcr.ValidateQuery;
import org.modeshape.jcr.ValidateQuery.ValidationBuilder;

/**
 * Arquillian integration tests that uses the predefined repository that
 * contains some index definitions, to test that query functionality is as
 * expected.
 */
@RunWith(Arquillian.class)
public class QueryIntegrationTest {

    @Resource(mappedName = "/jcr/query")
    private JcrRepository repository;
    private Session session;
    private Node testRoot;
    private boolean print;
   
    @Deployment
    public static WebArchive createDeployment() {
        File[] testDeps = Maven.configureResolver()
                .workOffline()
                .loadPomFromFile("pom.xml")
                .resolve("org.modeshape:modeshape-jcr:test-jar:tests:?").withTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "query-test.war").addAsLibraries(testDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsResource(new File("src/test/resources/text-extractor/text-file.txt"))
                .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Before
    public void beforeEach() throws Exception {
        session = repository.login("default");
        print = false;
    }

    @After
    public void afterEach() throws Exception {
        if (session == null) {
            return;
        }
        try {
            testRoot.remove();
            session.save();
        } finally {
            session.logout();
        }
    }

    protected ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(print);
    }

    @Test
    public void shouldQueryForAllUnstructuredNodes() throws Exception {
        testRoot = session.getRootNode().addNode("query_test");
        session.save();
        String sql = "select [jcr:path] from [nt:base] where [jcr:name] = 'query_test'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(1).useIndex("names").validate(query, query.execute());
    }

    @Test
    public void shouldQueryForMixTitle() throws Exception {
        testRoot = session.getRootNode().addNode("root");
        Node node1 = testRoot.addNode("node1");
        node1.addMixin("mix:title");
        node1.setProperty("jcr:title", "title_1");
        Node node2 = testRoot.addNode("node2");
        node2.addMixin("mix:title");
        node2.setProperty("jcr:title", "title_2");
        session.save();
     
        String sql = "select [jcr:path] from [mix:title] where [jcr:title] LIKE 'ti%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(2).hasNodesAtPaths("/root/node1", "/root/node2").useIndex("titles").validate(query, query.execute());
    }
}
