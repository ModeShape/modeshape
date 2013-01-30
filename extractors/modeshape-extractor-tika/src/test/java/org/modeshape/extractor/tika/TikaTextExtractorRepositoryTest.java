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

package org.modeshape.extractor.tika;

import static junit.framework.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
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
    @FixFor( "MODE-1561" )
    public void shouldNotExtractPastWriteLimit() throws Exception {
        startRepositoryWithConfiguration(getResource("repo-config-text-extraction-limit.json"));
        //configured in the cfg file
        int configuredWriteLimit = 100;

        //generate a string the size of the configured limit and check that it's been indexed
        String randomString = TikaTextExtractorTest.randomString(configuredWriteLimit);
        jcrTools.uploadFile(session, "/testFile", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        //test text extraction via querying, since that's where it's actually used
        String sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        jcrTools.printQuery(session, sql, 1);

        //generate a string larger than the limit and check that it hasn't been indexed
        randomString = TikaTextExtractorTest.randomString(configuredWriteLimit + 1);
        jcrTools.uploadFile(session, "testFile1", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        jcrTools.printQuery(session, sql, 0);
    }

    private void assertExtractedTextHasBeenIndexed( String validationQuery ) throws RepositoryException {
        Query query = jcrSession().getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals("Node with text content not found", 1, result.getNodes().getSize());
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
