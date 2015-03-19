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
package org.modeshape.extractor.tika;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.query.JcrQuery;

/**
 * Integration test which configures a repository to use a Tika-based extractor, creates and saves a node which has a binary value
 * from a text file and using the query mechanism tests that the text is extracted from the binary value and stored in the
 * indexes.
 * 
 * @author Horia Chiorean
 */
public class TikaTextExtractorRepositoryTest extends SingleUseAbstractTest {

    private JcrTools jcrTools = new JcrTools();

    @Test
    public void shouldExtractAndIndexContentFromPlainTextFile() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config.json"));
        uploadFile("text-file.txt");
        assertExtractedTextHasBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'The Quick Red Fox Jumps Over the Lazy Brown Dog')");
    }

    @Test
    public void shouldExtractAndIndexContentFromDocFile() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config.json"));
        uploadFile("modeshape.doc");
        assertExtractedTextHasBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')");
    }

    @Test
    public void shouldExtractAndIndexContentFromPdfGSFile() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config.json"));
        uploadFile("modeshape_gs.pdf");
        assertExtractedTextHasBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')");
    }

    @Test
    public void shouldExtractAndIndexContentFromXMLFile() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config.json"));
        uploadFile("cars.xml");
        assertExtractedTextHasBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'sports')");
    }

    @Test
    @FixFor( "MODE-1561" )
    public void shouldExtractPartiallyPastWriteLimit() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config-text-extraction-limit.json"));
        // configured in the cfg file
        int configuredWriteLimit = 100;

        // generate a string the size of the configured limit and check that it's been indexed
        String randomString = TikaTextExtractorTest.randomString(configuredWriteLimit);
        jcrTools.uploadFile(session, "/testFile", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        // test text extraction via querying, since that's where it's actually used
        String sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        queryAndExpectResults(sql, 1);

        // generate a string larger than the limit and check that it hasn't been indexed
        randomString = TikaTextExtractorTest.randomString(configuredWriteLimit + 1);
        jcrTools.uploadFile(session, "testFile1", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        queryAndExpectResults(sql, 1);
    }

    private void queryAndExpectResults( String queryString,
                                        int howMany ) throws RepositoryException {
        QueryManager queryManager = ((javax.jcr.Workspace)session.getWorkspace()).getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        Assert.assertEquals(howMany, nodes.getSize());
    }

    @Test
    public void shouldIgnoreMissingTikaDefaultDependendcy() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config.json"));
        uploadFile("image_file.jpg");
    }

    @Test
    @FixFor( "MODE-2107" )
    public void shouldSupportMimeTypeInclusionsAndExclusions() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config-exclusions-inclusions.json"));

        uploadFile("text-file.txt");
        assertExtractedTextHasNotBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'The Quick Red Fox Jumps Over the Lazy Brown Dog')");

        uploadFile("modeshape_gs.pdf");
        assertExtractedTextHasBeenIndexed("select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')");
    }

    private void assertExtractedTextHasBeenIndexed( String validationQuery ) throws RepositoryException {
        Query query = jcrSession().getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals("Node with text content not found", 1, result.getNodes().getSize());
    }

    private void assertExtractedTextHasNotBeenIndexed( String validationQuery ) throws RepositoryException {
        Query query = jcrSession().getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals("Node with text content was found", 0, result.getNodes().getSize());
    }

    private void uploadFile( String filepath ) throws RepositoryException, IOException, InterruptedException {
        // this will create jcr:content of type nt:resource with the jcr:data property
        jcrTools.uploadFile(session, "/" + filepath, getResource(filepath));
        session.save();
        // wait a bit to make sure the text extraction has happened
        Thread.sleep(500);
    }

    private InputStream getResource( String path ) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
