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
package org.modeshape.jcr.value.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Test class for binary store using cassandra as backend. Before running this test start cassandra instance and uncomment test
 * cases.
 * 
 * @author kulikov
 */
public class CassandraBinaryStoreTest {

    private static Exception exceptionDuringCassandraStart;

    private CassandraBinaryStore store;
    private BinaryValue aliveValue, unusedValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        FileUtil.delete("target/cassandra");
        try {
            System.setProperty("cassandra.config", "cassandra/cassandra.yaml");
            EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
            cassandra.start();
        } catch (Exception e) {
            // catch any exception here, because otherwise the test rule would not get a chance to execute
            exceptionDuringCassandraStart = e;
        }
    }

    @Before
    public void setUp() throws Exception {
        if (exceptionDuringCassandraStart != null) {
            throw exceptionDuringCassandraStart;
        }
        store = new CassandraBinaryStore(ClusteringHelper.getLocalHost().getHostAddress());
        store.start();

        ByteArrayInputStream stream = new ByteArrayInputStream("Binary value".getBytes());
        aliveValue = store.storeValue(stream, false);

        ByteArrayInputStream stream2 = new ByteArrayInputStream("Binary value".getBytes());
        unusedValue = store.storeValue(stream2, false);
    }

    @Test
    public void shouldReadWriteMimeType() throws BinaryStoreException {
        store.storeMimeType(aliveValue, "text/plain");
        assertEquals("text/plain", store.getStoredMimeType(aliveValue));
    }

    @Test
    public void shouldReadWriteExtractedText() throws BinaryStoreException {
        store.storeExtractedText(aliveValue, "extracted text");
        assertEquals("extracted text", store.getExtractedText(aliveValue));
    }

    @Test
    public void shouldMarkUnused() throws Exception {
        Set<BinaryKey> unused = new HashSet<BinaryKey>();
        unused.add(unusedValue.getKey());
        store.markAsUnused(unused);
        Thread.sleep(1300);
        store.removeValuesUnusedLongerThan(1, TimeUnit.SECONDS);
        try {
            store.getInputStream(unusedValue.getKey());
            fail("Unused binary not removed");
        } catch (BinaryStoreException e) {
            //expected
        }
    }

    @Test
    public void shouldAccessStream() throws BinaryStoreException, IOException {
        InputStream stream = store.getInputStream(aliveValue.getKey());
        assertTrue(stream != null);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(stream, bout);

        String s = new String(bout.toByteArray());
        assertEquals("Binary value", s);
    }

    @Test
    public void shoudlRemoveExpiredContent() throws Exception {
        Set<BinaryKey> unused = new HashSet<BinaryKey>();
        unused.add(unusedValue.getKey());
        store.markAsUnused(unused);

        Thread.sleep(500);

        store.removeValuesUnusedLongerThan(100, TimeUnit.MILLISECONDS);

        boolean res = true;
        try {
            store.getInputStream(unusedValue.getKey());
            res = false;
        } catch (BinaryStoreException e) {
        }

        assertTrue(res);
    }
}
