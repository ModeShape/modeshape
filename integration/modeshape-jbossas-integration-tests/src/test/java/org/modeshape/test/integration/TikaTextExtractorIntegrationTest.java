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
import java.io.File;
import java.io.InputStream;
import javax.annotation.Resource;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
        return ShrinkWrap.create(WebArchive.class, "tika-extractor-test.war")
                         .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                         .addAsResource(new File("src/test/resources/text-extractor"))
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Resource( mappedName = "/jcr/artifacts" )
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        session = repository.login("default");
    }

    @After
    public void afterEach() throws Exception {
        session.logout();
    }

    @Test
    public void shouldExtractAndIndexContentFromPlainTextFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'The Quick')";
        uploadFileAndCheckExtraction("text-extractor/text-file.txt", queryString);
    }

    @Test
    public void shouldExtractAndIndexContentFromDocFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')";
        uploadFileAndCheckExtraction("text-extractor/modeshape.doc", queryString);
    }


    @Test
    @FixFor( "MODE-1810" )
    public void shouldExtractAndIndexContentFromXlsxFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'Operations')";
        uploadFileAndCheckExtraction("text-extractor/sample-file.xlsx", queryString);
    }

    private void uploadFileAndCheckExtraction( String filepath,
                                               String validationQuery ) throws Exception {
        // this will create jcr:content of type nt:resource with the jcr:data property
        jcrTools.uploadFile(session, "/" + filepath, getResource(filepath));
        session.save();
        long numRows = 0;
        for (int i = 0; i != 10; ++i) {
            // wait a bit to make sure the text extraction has happened
            Thread.sleep(500);
            Query query = session.getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
            QueryResult result = query.execute();
            numRows = result.getNodes().getSize();
            if (numRows > 0) break;
        }
        assertEquals("Node with text content not found", 1, numRows);
    }

    private InputStream getResource( String path ) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
