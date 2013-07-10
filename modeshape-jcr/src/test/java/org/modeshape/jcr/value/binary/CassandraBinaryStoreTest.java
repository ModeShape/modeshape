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
package org.modeshape.jcr.value.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.modeshape.common.junit.SkipOnOS;
import org.modeshape.common.junit.SkipTestRule;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Test class for binary store using cassandra as backend. Before running this test start cassandra instance and uncomment test
 * cases.
 * 
 * @author kulikov
 */
@SkipOnOS( value = SkipOnOS.WINDOWS, description = "java.nio does not support IPV6 in JDK6 on Windows" )
public class CassandraBinaryStoreTest {

    @Rule
    public TestRule skipTestRule = new SkipTestRule();

    private static Exception exceptionDuringCassandraStart;

    private CassandraBinaryStore store;
    private BinaryValue aliveValue, unusedValue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            Boolean ipV4 = Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack"));
            if (ipV4) {
                System.setProperty("cassandra.config", "cassandra/cassandra.yaml");
            } else {
                System.setProperty("cassandra.config", "cassandra/cassandra_ipv6.yaml");
            }
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
        aliveValue = store.storeValue(stream);

        ByteArrayInputStream stream2 = new ByteArrayInputStream("Binary value".getBytes());
        unusedValue = store.storeValue(stream2);
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
    public void shouldMarkUnused() throws BinaryStoreException {
        Set<BinaryKey> unused = new HashSet<BinaryKey>();
        unused.add(unusedValue.getKey());
        store.markAsUnused(unused);

        boolean res = true;
        try {
            store.getInputStream(unusedValue.getKey());
            res = false;
        } catch (BinaryStoreException e) {
        }

        assertTrue(res);
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
