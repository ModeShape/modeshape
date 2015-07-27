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
import java.util.UUID;
import javax.annotation.Resource;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.JcrTools;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for a repository which uses Infinispan storage for both indexes and binary values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class InfinispanPersistenceIntegrationTest {
    private JcrTools jcrTools = new JcrTools();
    
    @Resource( mappedName = "/jcr/infinispanRepository" )
    private Repository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "infinispanRepository-test.war")
                                       .addAsResource(new File("src/test/resources/sequencer/BooksVDB.vdb"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Before
    public void setUp() {
        assertNotNull(repository);
    }

    @Test
    public void shouldStoreBinaryValues() throws Exception {
        Session session = repository.login();
        String testNodeName = "test_" + UUID.randomUUID().toString();
        String nodePath = "/" + testNodeName;
        
        jcrTools.uploadFile(session, nodePath, getClass().getClassLoader().getResourceAsStream("BooksVDB.vdb"));
        session.save();

        byte[] binaryBytes = IoUtil.readBytes(getClass().getClassLoader().getResourceAsStream("BooksVDB.vdb"));

        Node testNode = session.getNode(nodePath);
        Node contentNode = testNode.getNode("jcr:content");
        // mime type detection is name based and there is no entry for VDBs
        assertEquals("application/octet-stream", contentNode.getProperty("jcr:mimeType").getString());
        Binary binary = contentNode.getProperty("jcr:data").getBinary();
        assertNotNull(binary);
        byte[] storedBinaryBytes = IoUtil.readBytes(binary.getStream());

        assertArrayEquals(storedBinaryBytes, binaryBytes);
    }

    @Test
    public void shouldStoreIndexesAndAllowQuerying() throws Exception {
        Session session = repository.login();
        session.getRootNode().addNode("testNode_" + UUID.randomUUID().toString());
        session.save();

        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:path] FROM [nt:base] WHERE [jcr:name] LIKE '%testNode%'",
                                          Query.JCR_SQL2);
        assertTrue(query.execute().getNodes().getSize() >= 1);
    }
}
