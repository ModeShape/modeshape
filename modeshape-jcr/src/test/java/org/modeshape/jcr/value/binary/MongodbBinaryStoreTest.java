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
import java.util.Properties;
import org.junit.*;
import static org.junit.Assert.*;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Setup mongodb env and uncomment tests to run.
 * 
 * @author kulikov
 */
public class MongodbBinaryStoreTest {

    private MongodbBinaryStore binaryStore;

    public MongodbBinaryStoreTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        Properties props = new Properties();
        props.setProperty("mongodb.database", "test");

        binaryStore = new MongodbBinaryStore("test", null);
        binaryStore.setChunkSize(3);
        binaryStore.start();
    }

    @After
    public void tearDown() {
    }


    /**
     * Test of storeValue method, of class MongodbBinaryStore.
     */
//    @Test
    public void testStoreAndRetriveValue() throws Exception {
        ByteArrayInputStream content = new ByteArrayInputStream("Test message".getBytes());
        BinaryValue bv = binaryStore.storeValue(content);

        InputStream is = binaryStore.getInputStream(bv.getKey());
        String s = read(is);

        assertEquals("Test message", s);
    }

//    @Test
    public void testExtractedText() throws Exception {
        //store content
        ByteArrayInputStream content = new ByteArrayInputStream("Test message".getBytes());
        BinaryValue bv = binaryStore.storeValue(content);

        //store text
        binaryStore.storeExtractedText(bv, "Text");
        assertEquals("Text", binaryStore.getExtractedText(bv));
    }

    private String read(InputStream is) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b = 0;
        int len = 0;
        while (b != -1) {
            b = is.read();
            if (b != -1) {
                bout.write((byte)b);
                len++;
            }
        }
        return new String(bout.toByteArray(), 0, len);
    }

}
