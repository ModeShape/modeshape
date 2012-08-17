/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.jcr.value.binary;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.store.DataSourceConfig;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @author kulikov
 */
public class DatabaseBinaryStoreTest {

    private DatabaseBinaryStore store;
    private BinaryValue bValue;
    private DataSourceConfig dbConfig;

    public DatabaseBinaryStoreTest() {
    }

    @Before
    public void setUp() throws BinaryStoreException {
        dbConfig = new DataSourceConfig();
        String driver = dbConfig.getDriverClassName();
        String url = dbConfig.getUrl();
        String username = dbConfig.getUsername();
        String password = dbConfig.getPassword();
        store = new DatabaseBinaryStore(driver, url, username, password);
        store.start();

        InputStream content = new ByteArrayInputStream("Binary content".getBytes());
        bValue = store.storeValue(content);
    }

    @After
    public void tearDown() {
        store.shutdown();
    }

    @Test
    public void shouldPutAndGetBinaryContent() throws Exception {
        byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        BinaryValue ref = store.storeValue(bin);

        InputStream istream = store.getInputStream(ref.getKey());
        assertThat("Failure to get stored value", istream, is(notNullValue()));

        byte[] val = read(istream);

        assertThat(val.length, is(data.length));
        for (int i = 0; i < data.length; i++) {
            assertThat("Failure at pos " + i, val[i], is(data[i]));
        }

        // Put it again ...
        ByteArrayInputStream bin2 = new ByteArrayInputStream(data);
        BinaryValue ref2 = store.storeValue(bin2);

        // And the BinaryValue should have the same key ...
        assertThat(ref.getKey(), is(ref2.getKey()));
        // and should be equal ...
        assertThat(ref, is(ref2));
    }

    private byte[] read( InputStream is ) throws IOException {
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

    @Test
    public void shouldMarkAsUnused() throws Exception {
        String testMessage = "This is a test message";
        InputStream content = genContent(testMessage);

        BinaryValue ref = store.storeValue(content);

        //checking that it was succesfuly stored
        InputStream is = store.getInputStream(ref.getKey());
        assertThat("Failure to get stored value", is, is(notNullValue()));

        //convert content into string for checking
        byte[] data = read(is);
        String msg = new String(data, 0, data.length);

        assertThat("Content distortion", msg, is(testMessage));

        //put into garbage and mark as unused
        ArrayList<BinaryKey> garbage = new ArrayList<BinaryKey>();
        garbage.add(ref.getKey());
        store.markAsUnused(garbage);

        //check content avaiability again
        is = store.getInputStream(ref.getKey());
        assertThat("Content still available", is, is(nullValue()));

        //allow to expire
        Thread.sleep(500);

        //removing expired value
        store.removeValuesUnusedLongerThan(300, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldRemoveValuesUnusedLongerThan() throws Exception {
    }

    @Test
    public void testMimeType() throws Exception {
        store.storeMimeType(bValue, "plain/text");
        assertThat(store.getStoredMimeType(bValue), is("plain/text"));
    }

    @Test
    public void testExtractedText() throws Exception {
        store.storeExtractedText(bValue, "hint");
        assertThat(store.getExtractedText(bValue), is("hint"));
    }

    private InputStream genContent(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}
