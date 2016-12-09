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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import javax.annotation.Resource;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.schematic.document.Document;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Smoke tests persistence in a repository that store all its information in a Database
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class JDBCRepositoryIntegrationTest {

    private static final JcrTools JCR_TOOLS = new JcrTools();
    
    @Resource (mappedName =  "/jcr/dbBinaryJDBCRepository")
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "jdbcRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Test
    @FixFor( {"MODE-2194", "MODE-2636"} )
    public void shouldPersistBinaryDataInDBBinaryStore() throws Exception {
        assertConfiguration();
        assertDataPersisted();
        persistBinaryContent();
    }
    
    private void assertConfiguration() throws Exception {
        Document persistenceConfiguration = repository.getConfiguration().getPersistenceConfiguration();
        assertEquals("MODESHAPE_IT_REPO", persistenceConfiguration.getString("tableName"));        
    }
    
    private void assertDataPersisted() throws RepositoryException {
        assertNotNull(repository);
        Session session = repository.login();
        try {
            //check predefined content was imported
            assertNotNull(session.getNode("/files"));
            assertNotNull(session.getNode("/cars"));
            
            //add a node and check it can be queried
            session.getRootNode().addNode("testNode");
            session.save();
            
            Query query = session.getWorkspace()
                                 .getQueryManager()
                                 .createQuery("select [jcr:path] FROM [nt:base] WHERE [jcr:name] LIKE '%testNode%'",
                                              Query.JCR_SQL2);
            assertEquals(1, query.execute().getNodes().getSize());
        } finally {
            session.logout();
        }
    }

    private void persistBinaryContent() throws RepositoryException, IOException {
        assertNotNull(repository);

        long minimumBinarySize = repository.getConfiguration().getBinaryStorage().getMinimumBinarySizeInBytes();
        long binarySize = 2 * minimumBinarySize;

        Session session = repository.login();
        InputStream binaryValueStream = null;
        try {
            byte[] content = new byte[(int)binarySize];
            new Random().nextBytes(content);
            JCR_TOOLS.uploadFile(session, "folder/file", new ByteArrayInputStream(content));
            session.save();

            Node nodeWithBinaryContent = session.getNode("/folder/file/jcr:content");
            assertEquals("nt:resource", nodeWithBinaryContent.getPrimaryNodeType().getName());
            try {
                nodeWithBinaryContent.getProperty("jcr:mimeType");
                fail("Mimetype detection is disabled; there shouldn't be any mime type property");
            } catch (PathNotFoundException e) {
                // expected
            }
            Binary binaryValue = nodeWithBinaryContent.getProperty("jcr:data").getBinary();
            binaryValueStream = binaryValue.getStream();
            byte[] retrievedContent = IoUtil.readBytes(binaryValueStream);
            assertArrayEquals(content, retrievedContent);
        } finally {
            if (binaryValueStream != null) {
                binaryValueStream.close();
            }
            session.logout();
        }
    }
}
