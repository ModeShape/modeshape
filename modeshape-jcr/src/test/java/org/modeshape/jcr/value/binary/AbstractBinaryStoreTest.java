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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.AbstractTransactionalTest;
import org.modeshape.jcr.TextExtractors;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Use this abstract class to realize test cases which can easily executed on different BinaryStores
 */
public abstract class AbstractBinaryStoreTest extends AbstractTransactionalTest {

    public static final byte[] LARGE_DATA = new byte[6 * 1024 * 1024];
    public static final BinaryKey LARGE_KEY;
    public static final byte[] SMALL_DATA = new byte[1024];
    public static final BinaryKey SMALL_KEY;
    public static final byte[] MEDIUM_DATA = new byte[1024 * 100];
    public static final BinaryKey MEDIUM_KEY;
    public static final byte[] ZERO_DATA = new byte[0];
    public static final BinaryKey ZERO_KEY;
    public static final String TEXT_DATA;

    private static final Random RANDOM = new Random();

    static {
        RANDOM.nextBytes(LARGE_DATA);
        LARGE_KEY = BinaryKey.keyFor(LARGE_DATA);
        RANDOM.nextBytes(SMALL_DATA);
        SMALL_KEY = BinaryKey.keyFor(SMALL_DATA);
        RANDOM.nextBytes(MEDIUM_DATA);
        MEDIUM_KEY = BinaryKey.keyFor(MEDIUM_DATA);
        ZERO_KEY = BinaryKey.keyFor(new byte[0]);
        TEXT_DATA = "Flash Gordon said: Ich bin Bärliner." + UUID.randomUUID().toString();
    }

    protected abstract BinaryStore getBinaryStore();

    @Test
    public void shouldAllowChangingTheMinimumBinarySize() throws Exception {
        BinaryStore binaryStore = getBinaryStore();
        long originalSize = binaryStore.getMinimumBinarySizeInBytes();
        assertTrue(originalSize > 0);
        long newSize = 12l;
        binaryStore.setMinimumBinarySizeInBytes(newSize);
        assertEquals(newSize, binaryStore.getMinimumBinarySizeInBytes());
        binaryStore.setMinimumBinarySizeInBytes(originalSize);
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldFailWhenGettingInvalidBinary() throws BinaryStoreException {
        getBinaryStore().getInputStream(invalidBinaryKey());
    }

    @Test
    public void shouldStoreLargeBinary() throws BinaryStoreException, IOException {
        storeAndValidate(LARGE_KEY, LARGE_DATA);
    }

    @Test
    public void shouldStoreMediumBinary() throws BinaryStoreException, IOException {
        storeAndValidate(MEDIUM_KEY, MEDIUM_DATA);
    }

    @Test
    public void shouldStoreSmallBinary() throws BinaryStoreException, IOException {
        storeAndValidate(SMALL_KEY, SMALL_DATA);
    }

    @Test
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        storeAndValidate(ZERO_KEY, ZERO_DATA);
    }

    private void storeAndValidate( BinaryKey key,
                                   byte[] data ) throws BinaryStoreException, IOException {
        BinaryValue res = getBinaryStore().storeValue(new ByteArrayInputStream(data));
        assertEquals(key, res.getKey());
        assertEquals(data.length, res.getSize());
        InputStream inputStream = getBinaryStore().getInputStream(key);
        BinaryKey currentKey = BinaryKey.keyFor(IoUtil.readBytes(inputStream));
        assertEquals(key, currentKey);
    }

    @Test
    public void shouldCleanupUnunsedValues() throws Exception {
        getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        List<BinaryKey> keys = new ArrayList<BinaryKey>();
        keys.add(SMALL_KEY);
        getBinaryStore().markAsUnused(keys);
        Thread.sleep(1000);
        // now remove and test if still there
        getBinaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

        try {
            // no annotation used here to differ from other BinaryStoreException
            getBinaryStore().getInputStream(SMALL_KEY);
            fail("Key was not removed");
        } catch (BinaryStoreException ex) {
        }
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldFailWhenGettingTheMimeTypeOfBinaryWhichIsntStored() throws IOException, RepositoryException {
        getBinaryStore().getMimeType(new StoredBinaryValue(getBinaryStore(), invalidBinaryKey(), 0), "foobar.txt");
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldFailWhenGettingTheTextOfBinaryWhichIsntStored() throws RepositoryException {
        getBinaryStore().getText(new StoredBinaryValue(getBinaryStore(), invalidBinaryKey(), 0));
    }

    private BinaryKey invalidBinaryKey() {
        return new BinaryKey(UUID.randomUUID().toString());
    }

    @Test
    public void shouldReturnAllStoredKeys() throws Exception {
        storeAndValidate(MEDIUM_KEY, MEDIUM_DATA);
        storeAndValidate(SMALL_KEY, SMALL_DATA);

        List<String> keys = new ArrayList<String>(Arrays.asList(MEDIUM_KEY.toString(), SMALL_KEY.toString()));
        for (BinaryKey key : getBinaryStore().getAllBinaryKeys()) {
            keys.remove(key.toString());
        }
        assertEquals(0, keys.size());
    }

    @Test
    public void shouldExtractAndStoreMimeTypeWhenDetectorConfigured() throws RepositoryException, IOException {
        getBinaryStore().setMimeTypeDetector(new DummyMimeTypeDetector());
        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        assertNull(((AbstractBinaryStore)getBinaryStore()).getStoredMimeType(binaryValue));
        // unclean stuff... a getter modifies silently data
        assertEquals(DummyMimeTypeDetector.DEFAULT_TYPE, getBinaryStore().getMimeType(binaryValue, "foobar.txt"));
        assertEquals(DummyMimeTypeDetector.DEFAULT_TYPE, ((AbstractBinaryStore)getBinaryStore()).getStoredMimeType(binaryValue));
    }

    @Test
    public void shouldExtractAndStoreTextWhenExtractorConfigured() throws Exception {
        TextExtractors extractors = new TextExtractors(Executors.newSingleThreadExecutor(), true,
                                                       Arrays.<TextExtractor>asList(new DummyTextExtractor()));
        getBinaryStore().setTextExtractors(extractors);

        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(SMALL_DATA));
        assertNull(((AbstractBinaryStore)getBinaryStore()).getExtractedText(binaryValue));
        assertEquals(DummyTextExtractor.EXTRACTED_TEXT, getBinaryStore().getText(binaryValue));
        assertEquals(DummyTextExtractor.EXTRACTED_TEXT, ((AbstractBinaryStore)getBinaryStore()).getExtractedText(binaryValue));
    }

    protected static final class DummyMimeTypeDetector implements MimeTypeDetector {

        public static final String DEFAULT_TYPE = "application/foobar";

        @Override
        public String mimeTypeOf( String name,
                                  Binary binaryValue ) {
            return DEFAULT_TYPE;
        }
    }

    protected static final class DummyTextExtractor extends TextExtractor {
        private static final String EXTRACTED_TEXT = "some text";

        @Override
        public void extractFrom( org.modeshape.jcr.api.Binary binary,
                                 Output output,
                                 Context context ) throws Exception {
            output.recordText(EXTRACTED_TEXT);
        }

        @Override
        public boolean supportsMimeType( String mimeType ) {
            return true;
        }
    }
}
