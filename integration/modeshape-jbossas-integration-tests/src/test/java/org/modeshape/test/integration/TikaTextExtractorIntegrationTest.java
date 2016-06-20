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

import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.ValidateQuery.validateQuery;
import java.io.File;
import java.io.InputStream;
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
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.query.JcrQuery;

/**
 * Arquillian test which checks that text-extraction works in an AS7 container.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class )
public class TikaTextExtractorIntegrationTest {

    private JcrTools jcrTools = new JcrTools();
    private Session session;

    @Deployment
    public static WebArchive createDeployment() {
        File[] testDeps = Maven.configureResolver()
                               .workOffline()
                               .loadPomFromFile("pom.xml")
                               .resolve("org.modeshape:modeshape-jcr:test-jar:tests:?").withTransitivity().asFile();

        return ShrinkWrap.create(WebArchive.class, "tika-extractor-test.war")
                         .addAsLibraries(testDeps)
                         .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                         .addAsResource(new File("src/test/resources/text-extractor"))
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Resource( mappedName = "/jcr/query" )
    private JcrRepository repository;
   
    @Before
    public void beforeEach() throws Exception {
        session = repository.login("default");
    }

    @After
    public void afterEach() throws Exception {
        if (session == null) {
            return;
        }
        try {
            Node testRoot = session.getNode("/text-extractor");
            if (testRoot != null) {
                testRoot.remove();
                session.save();
            }
        } finally {
            session.logout();
        }
    }

    @Test
    public void shouldExtractAndIndexContentFromPlainTextFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'The Quick')";
        uploadFileAndCheckExtraction("text-extractor/text-file.txt", "text/plain", queryString);
    }

    @Test
    public void shouldExtractAndIndexContentFromDocFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')";
        // upload under a nodename without extension, to check mime-type detection is content based (as configured)
        uploadFileAndCheckExtraction("text-extractor/modeshape.doc", "application/msword", queryString);
    }


    @Test
    @FixFor( "MODE-1810" )
    public void shouldExtractAndIndexContentFromXlsxFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'Operations')";
        uploadFileAndCheckExtraction("text-extractor/sample-file.xlsx",
                                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                     queryString);
    }

    private void uploadFileAndCheckExtraction(String filepath,
                                              String expectedMimeType,
                                              String validationQuery) throws Exception {
        String nodePath = "/" + filepath;
        // this will create jcr:content of type nt:resource with the jcr:data property
        jcrTools.uploadFile(session, nodePath, getResource(filepath));
        session.save();
        // wait a bit to make sure the text extraction has happened
        Thread.sleep(1000);
        String mimeType = session.getNode(nodePath).getNode("jcr:content").getProperty("jcr:mimeType").getString();
        assertEquals("Expected mime-type has not been detected", expectedMimeType, mimeType);
        Query query = session.getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
        validateQuery().hasNodesAtPaths(nodePath + "/jcr:content").useIndex("textFromFiles").validate(query, query.execute());
    }

    private InputStream getResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
