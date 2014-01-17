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
import java.util.Random;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.ValueFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertArrayEquals;

/**
 * Test which verifies that the ModeShape composite binary store configuration inside of AS7 is correct.
 *
 * @author Chris Beer
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class)
public class CompositeBinaryStoreIntegrationTest {

    private static final Random RANDOM = new Random();

    @Resource( mappedName = "java:/jcr/compositeBinaryStoreRepository" )
    private JcrRepository repository;

    private Session session;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "composite-binary-store-test.war")
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Before
    public void before() throws Exception {
        assertNotNull("repository should not be null", repository);
    }

    @Test
    public void shouldStoreDataInEachOfTheNamedBinaryStores() throws Exception {
        assertEquals(RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_COMPOSITE,
                     repository.getConfiguration().getBinaryStorage().getType());
        session = repository.login();
        long minBinarySize = repository.getConfiguration().getBinaryStorage().getMinimumBinarySizeInBytes() + 1;

        byte[] defaultBinary = createNodeWithBinaryProperty("default", minBinarySize, "default", session);
        assertBinaryPropertyStored(defaultBinary, "/default", session);

        byte[] fs1Binary = createNodeWithBinaryProperty("fs1", minBinarySize, "fs1", session);
        assertBinaryPropertyStored(fs1Binary, "/fs1", session);

        byte[] fs2Binary = createNodeWithBinaryProperty("fs2", minBinarySize, "fs2", session);
        assertBinaryPropertyStored(fs2Binary, "/fs2", session);

        byte[] anotherBinary = createNodeWithBinaryProperty("non-existent", minBinarySize, "non-existent", session);
        assertBinaryPropertyStored(anotherBinary, "/non-existent", session);
    }

    private byte[] createNodeWithBinaryProperty(String nodeName, long minBinarySize, String binaryStoreName, Session session) throws Exception {
        Node node = session.getNode("/").addNode(nodeName);
        byte[] randomBytes = new byte[(int)minBinarySize];
        RANDOM.nextBytes(randomBytes);
        Binary binary = ((ValueFactory) session.getValueFactory()).createBinary(new ByteArrayInputStream(randomBytes), binaryStoreName);
        node.setProperty("binary", binary);
        session.save();
        return randomBytes;
    }

    private void assertBinaryPropertyStored(byte[] expectedBinary, String nodePath, Session session) throws Exception {
        Node node = session.getNode(nodePath);
        javax.jcr.Binary binary = node.getProperty("binary").getBinary();
        assertNotNull("Binary property not found", binary);
        byte[] actualBinary = IoUtil.readBytes(binary.getStream());
        assertArrayEquals("Expected binary content not retrieved from the property", expectedBinary, actualBinary);
    }
}
