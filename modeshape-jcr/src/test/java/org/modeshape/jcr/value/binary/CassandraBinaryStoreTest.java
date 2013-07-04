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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Test class for binary store using cassandra as backend.
 *
 * Before running this test start cassandra instance and uncomment test cases.
 *
 * @author kulikov
 */
public class CassandraBinaryStoreTest {

    private CassandraBinaryStore store;
    private BinaryValue aliveValue, unusedValue;
    private static EmbeddedCassandraService cassandra;

    private static Boolean isIpV4;
    
    public CassandraBinaryStoreTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        isIpV4 = Boolean.parseBoolean(System.getProperty("java.net.preferIPv4Stack"));
        if (isIpV4) {
            System.setProperty("cassandra.config", "cassandra/cassandra.yaml");
        } else {
            System.setProperty("cassandra.config", "cassandra/cassandra_ipv6.yaml");            
        }   
        cassandra = new EmbeddedCassandraService();
        cassandra.start();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        store = new CassandraBinaryStore(ClusteringHelper.getLocalHost().getHostAddress());
        store.start();

        ByteArrayInputStream stream = new ByteArrayInputStream("Binary value".getBytes());
        aliveValue = store.storeValue(stream);

        ByteArrayInputStream stream2 = new ByteArrayInputStream("Binary value".getBytes());
        unusedValue = store.storeValue(stream2);
    }

    @After
    public void tearDown() {
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
        Set<BinaryKey> unused = new HashSet();
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
        Set<BinaryKey> unused = new HashSet();
        unused.add(unusedValue.getKey());
        store.markAsUnused(unused);

        Thread.currentThread().sleep(500);

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
