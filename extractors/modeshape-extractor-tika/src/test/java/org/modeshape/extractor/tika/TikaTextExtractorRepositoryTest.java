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

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import static junit.framework.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.query.JcrQuery;
import java.io.InputStream;

/**
 * Integration test which configures a repository to use a Tika-based extractor, creates and saves a node which has a binary
 * value from a text file and using the query mechanism tests that the text is extracted from the binary value and stored
 * in the indexes.
 *
 * @author Horia Chiorean
 */
@Ignore("Enable this once MODE-1419 is fixed")
public class TikaTextExtractorRepositoryTest extends SingleUseAbstractTest {

    private JcrTools jcrTools = new JcrTools();

    @Override
    public void beforeEach() throws Exception {
       startRepositoryWithConfiguration(getResource("repo-config.json"));
    }

    @Test
    public void shouldExtractAndIndexContentFromPlainTextFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'The Quick')";
        uploadFileAndCheckExtraction("text-file.txt", queryString);
    }

    @Test
    public void shouldExtractAndIndexContentFromDocFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')";
        uploadFileAndCheckExtraction("modeshape.doc", queryString);
    }

    @Test
    public void shouldExtractAndIndexContentFromPdfGSFile() throws Exception {
        String queryString = "select [jcr:path] from [nt:resource] as res where contains(res.*, 'ModeShape supports')";
        uploadFileAndCheckExtraction("modeshape_gs.pdf", queryString);
    }

    private void uploadFileAndCheckExtraction(String filepath, String validationQuery) throws Exception {
        //this will create jcr:content of type nt:resource with the jcr:data property
        jcrTools.uploadFile(session, "/" + filepath, getResource(filepath));
        session.save();
        //wait a bit to make sure the text extraction has happened
        Thread.sleep(500);
        Query query = jcrSession().getWorkspace().getQueryManager().createQuery(validationQuery, JcrQuery.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals("Node with text content not found", 1, result.getNodes().getSize());
    }

    private InputStream getResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
