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

package org.modeshape.test.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
