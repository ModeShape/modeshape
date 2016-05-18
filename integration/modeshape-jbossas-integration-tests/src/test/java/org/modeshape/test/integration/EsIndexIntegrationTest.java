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
import java.io.InputStream;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ValidateQuery;
import org.modeshape.jcr.ValidateQuery.ValidationBuilder;
import org.modeshape.jcr.api.JcrTools;

/**
 * Arquillian integration tests that uses the predefined repository that
 * contains some index definitions, to test that query functionality is as
 * expected.
 */
@RunWith(Arquillian.class)
public class EsIndexIntegrationTest {

    @Resource(mappedName = "/jcr/repo-with-es-index")
    private JcrRepository repository;
    private Session session;
    private boolean print;
    private JcrTools tools;
    private static org.elasticsearch.node.Node esNode;

    @Deployment
    public static WebArchive createDeployment() {
        File[] testDeps = Maven.configureResolver()
                .workOffline()
                .loadPomFromFile("pom.xml")
                .resolve("org.modeshape:modeshape-jcr:test-jar:tests:?").withTransitivity().asFile();

        ClassLoader cl = EsIndexIntegrationTest.class.getClassLoader();
        return ShrinkWrap.create(WebArchive.class, "query-test.war").addAsLibraries(testDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsWebInfResource(cl.getResource("WEB-INF/jboss-deployment-structure.xml"), ArchivePaths.create("jboss-deployment-structure.xml"))
                .addAsResource(new File("src/test/resources/text-extractor/text-file.txt"))
                .setManifest(cl.getResource("META-INF/MANIFEST.MF"));
    }

    @BeforeClass
    public static void startES() throws Exception {
        FileUtil.delete("target/data");
        Settings localSettings = Settings.settingsBuilder()
                .put("http.enabled", true)
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                .put("path.home", "target/data")
                .build();

        //configure Elasticsearch node
        esNode = NodeBuilder.nodeBuilder().settings(localSettings).local(false).build().start();
        Thread.currentThread().sleep(1000);
    }

    @AfterClass
    public static void stopES() throws Exception {
        if (esNode != null) {
            esNode.close();
        }
        FileUtil.delete("target/data");
    }

    @Before
    public void beforeEach() throws Exception {
        session = repository.login("default");
        print = false;
        tools = new JcrTools();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
    }

    protected ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(print);
    }

    @Test
    public void shouldQueryUsingElasticsearchIndex() throws Exception {
        Node testRoot = session.getRootNode().addNode("root");
        Node node1 = testRoot.addNode("node5");
        node1.addMixin("mix:mimeType");
        node1.setProperty("jcr:mimeType", "5");

        Node node2 = testRoot.addNode("node6");
        node2.addMixin("mix:mimeType");
        node2.setProperty("jcr:mimeType", "6");

        session.save();

        try {
            String sql = "select [jcr:path] from [mix:mimeType] where [jcr:mimeType] = '5'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            validateQuery().rowCount(1).useIndex("mime-types").validate(query, query.execute());
        } finally {
            testRoot.remove();
            session.save();
        }
    }

    private InputStream resourceStream(String path) {
        return EsIndexIntegrationTest.class.getClassLoader().getResourceAsStream(path);
    }
}
