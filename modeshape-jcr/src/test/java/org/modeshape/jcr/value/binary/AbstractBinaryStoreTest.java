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

import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

/**
 * Use this abstract class to realize test cases which can easily executed on different BinaryStores
 *
 */
public abstract class AbstractBinaryStoreTest {

    public static final byte[] LARGE_DATA = new byte[6 * 1024 * 1024];
    public static final BinaryKey LARGE_KEY;
    public static final byte[] SMALL_DATA = new byte[1012];
    public static final BinaryKey SMALL_KEY;
    public static final byte[] ZERO_DATA = new byte[0];
    public static final BinaryKey ZERO_KEY;
    public static final String TEXT_DATA;

    static {
        new Random().nextBytes(LARGE_DATA);
        LARGE_KEY = BinaryKey.keyFor(LARGE_DATA);
        new Random().nextBytes(SMALL_DATA);
        SMALL_KEY = BinaryKey.keyFor(SMALL_DATA);
        ZERO_KEY = BinaryKey.keyFor(new byte[0]);
        TEXT_DATA = "Flash Gordon said: Ich bin Bärliner.";
    }

    protected abstract BinaryStore getBinaryStore();


    @Test(expected = BinaryStoreException.class)
    public void testNoBinaryValue() throws BinaryStoreException {
        getBinaryStore().getInputStream(new BinaryKey("nonsuch"));
    }

    @Test
    public void testLargeData() throws BinaryStoreException, IOException {
        validateDataInStore(LARGE_KEY, LARGE_DATA);
    }

    @Test
    public void testSmallData() throws BinaryStoreException, IOException {
        validateDataInStore(SMALL_KEY, SMALL_DATA);
    }

    @Test
    public void testZeroData() throws BinaryStoreException, IOException {
        validateDataInStore(ZERO_KEY, ZERO_DATA);
    }

    private void validateDataInStore(BinaryKey key, byte[] data) throws BinaryStoreException, IOException {
        BinaryValue res = getBinaryStore().storeValue(new ByteArrayInputStream(data));
        assertEquals(key, res.getKey());
        assertEquals(data.length, res.getSize());
        InputStream inputStream = getBinaryStore().getInputStream(key);
        BinaryKey currentKey = BinaryKey.keyFor(IoUtil.readBytes(inputStream));
        assertEquals(key, currentKey);
    }

    @Test
    public void testMarkAndRemoveUnused() throws BinaryStoreException, IOException {
        getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        List<BinaryKey> keys = new ArrayList<BinaryKey>();
        keys.add(SMALL_KEY);
        getBinaryStore().markAsUnused(keys);
        try { Thread.sleep(1000); } catch (Throwable t){}
        // now remove and test if still there
        getBinaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

        try {
            // no annotation used here to differ from other BinaryStoreException
            getBinaryStore().getInputStream(SMALL_KEY);
            fail("Key was not removed");
        } catch (BinaryStoreException ex){}
    }

    @Test(expected=BinaryStoreException.class)
    public void testGetMimeTypeWithoutExistingValue() throws BinaryStoreException, IOException, RepositoryException {
        getBinaryStore().getMimeType(new StoredBinaryValue(getBinaryStore(), new BinaryKey("nonsuch"), 0), "foobar.txt");
    }

    @Test
    public void testMimeType() throws RepositoryException, IOException {
        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        assertNull(((AbstractBinaryStore)getBinaryStore()).getStoredMimeType(binaryValue));
        // unclean stuff... a getter modifies silently data
        assertEquals(DummyMimeTypeDetector.DEFAULT_TYPE, getBinaryStore().getMimeType(binaryValue, "foobar.txt"));
        assertEquals(DummyMimeTypeDetector.DEFAULT_TYPE, ((AbstractBinaryStore)getBinaryStore()).getStoredMimeType(binaryValue));
    }

    @Test
    public void testTextExtraction() throws BinaryStoreException {
        assertNull(getBinaryStore().getExtractedText(new StoredBinaryValue(getBinaryStore(), new BinaryKey("nonsuch"), 0)));

        // test store + get
        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        getBinaryStore().storeExtractedText(binaryValue, TEXT_DATA);

        String text = getBinaryStore().getExtractedText(binaryValue);
        assertEquals(TEXT_DATA, text);

        // TextExtractors have no interface and are final :-o ... how to test w/o repository? :-)
    }

    public class DummyMimeTypeDetector extends MimeTypeDetector {

        public static final String DEFAULT_TYPE = "application/foobar";

        @Override
        public String mimeTypeOf(String name, Binary binaryValue) throws RepositoryException, IOException {
            return DEFAULT_TYPE;
        }
    }

}
