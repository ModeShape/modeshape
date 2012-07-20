/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.jcr.value.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.*;
import static org.junit.Assert.*;
import org.modeshape.jcr.value.BinaryValue;

/**
 *
 * @author kulikov
 */
public class DatabaseBinaryStoreTest {

    private final String driver = "org.h2.Driver";
    private final String url = "jdbc:h2:~/test";
    private final String username = null;
    private final String password = null;

    private DatabaseBinaryStore store;
    private BinaryValue bValue;

    public DatabaseBinaryStoreTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws BinaryStoreException {
        store = new DatabaseBinaryStore(driver, url, username, password, null);
        store.start();

        InputStream content = new ByteArrayInputStream("Binary content".getBytes());
        bValue = store.storeValue(content);
    }

    @After
    public void tearDown() {
        store.shutdown();
    }

    /**
     * Test of storeValue method, of class DatabaseBinaryStore.
     */
    @Test
    public void testPutAndGet() throws Exception {
        byte[] data = new byte[]{1,2,3,4,5,6,7,8,9};

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        BinaryValue ref = store.storeValue(bin);

        InputStream is = store.getInputStream(ref.getKey());
        assertTrue("Failure to get stored value", is != null);

        byte[] val = read(is);
        
        assertEquals(data.length, val.length);
        for (int i = 0; i < data.length; i++) {
            assertEquals("Failure at pos " + i, data[i], val[i]);
        }
    }

    private byte[] read(InputStream is) throws IOException {
        ByteArrayOutputStream sb = new ByteArrayOutputStream();
        int b = 0;

        while (b != -1) {
            b = is.read();
            if (b != -1) {
                sb.write((byte)(b & 0xff));
            }
        }

        return sb.toByteArray();
    }

    /**
     * Test of markAsUnused method, of class DatabaseBinaryStore.
     */
    @Test
    public void testMarkAsUnused() throws Exception {
    }

    /**
     * Test of removeValuesUnusedLongerThan method, of class DatabaseBinaryStore.
     */
    @Test
    public void testRemoveValuesUnusedLongerThan() throws Exception {
    }

    /**
     * Test of getStoredMimeType method, of class DatabaseBinaryStore.
     */
    @Test
    public void testMimeType() throws Exception {
        store.storeMimeType(bValue, "plain/text");
        assertEquals("plain/text", store.getStoredMimeType(bValue));
    }

    /**
     * Test of getExtractedText method, of class DatabaseBinaryStore.
     */
    @Test
    public void testExtractedText() throws Exception {
        store.storeExtractedText(bValue, "hint");
        assertEquals("hint", store.getExtractedText(bValue));
    }

}
