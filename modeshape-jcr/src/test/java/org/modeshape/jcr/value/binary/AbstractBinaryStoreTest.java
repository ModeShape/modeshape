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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.modeshape.common.junit.SkipTestRule;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.TextExtractors;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Use this abstract class to realize test cases which can easily executed on different BinaryStores
 */
public abstract class AbstractBinaryStoreTest {

    @Rule
    public TestRule skipTestRule = new SkipTestRule();

    /**
     * We need to generate the test byte arrays based on the minimum binary size, because that controls the distinction between
     * Stored/In Memory binary values.
     */
    public static final byte[] STORED_LARGE_BINARY = new byte[(int)(AbstractBinaryStore.DEFAULT_MINIMUM_BINARY_SIZE_IN_BYTES * 4)];
    public static final BinaryKey STORED_LARGE_KEY;
    public static final byte[] IN_MEMORY_BINARY = new byte[(int)(AbstractBinaryStore.DEFAULT_MINIMUM_BINARY_SIZE_IN_BYTES / 2)];
    public static final BinaryKey IN_MEMORY_KEY;
    public static final byte[] STORED_MEDIUM_BINARY = new byte[(int)(AbstractBinaryStore.DEFAULT_MINIMUM_BINARY_SIZE_IN_BYTES * 2)];
    public static final BinaryKey STORED_MEDIUM_KEY;
    public static final byte[] EMPTY_BINARY = new byte[0];
    public static final BinaryKey EMPTY_BINARY_KEY;
    public static final String TEXT_DATA;

    private static final Random RANDOM = new Random();

    static {
        RANDOM.nextBytes(STORED_LARGE_BINARY);
        STORED_LARGE_KEY = BinaryKey.keyFor(STORED_LARGE_BINARY);

        RANDOM.nextBytes(IN_MEMORY_BINARY);
        IN_MEMORY_KEY = BinaryKey.keyFor(IN_MEMORY_BINARY);

        RANDOM.nextBytes(STORED_MEDIUM_BINARY);
        STORED_MEDIUM_KEY = BinaryKey.keyFor(STORED_MEDIUM_BINARY);

        EMPTY_BINARY_KEY = BinaryKey.keyFor(EMPTY_BINARY);

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
        storeAndValidate(STORED_LARGE_KEY, STORED_LARGE_BINARY);
    }

    @Test
    public void shouldStoreMediumBinary() throws BinaryStoreException, IOException {
        storeAndValidate(STORED_MEDIUM_KEY, STORED_MEDIUM_BINARY);
    }

    @Test
    public void shouldStoreSmallBinary() throws BinaryStoreException, IOException {
        storeAndValidate(IN_MEMORY_KEY, IN_MEMORY_BINARY);
    }

    @Test
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        storeAndValidate(EMPTY_BINARY_KEY, EMPTY_BINARY);
    }

    @Test
    public void shouldHaveKey() throws BinaryStoreException, IOException {
        storeAndValidate(STORED_MEDIUM_KEY, STORED_MEDIUM_BINARY);
        assertTrue("Expected BinaryStore to contain the key", getBinaryStore().hasBinary(STORED_MEDIUM_KEY));
    }

    @Test
    public void shouldNotHaveKey() {
        assertTrue("Did not expect BinaryStore to contain the key", !getBinaryStore().hasBinary(invalidBinaryKey()));
    }

    private BinaryValue storeAndValidate( BinaryKey key,
                                          byte[] data ) throws BinaryStoreException, IOException {
        BinaryValue res = getBinaryStore().storeValue(new ByteArrayInputStream(data));
        assertNotNull(res);
        assertEquals(key, res.getKey());
        assertEquals(data.length, res.getSize());
        InputStream inputStream = getBinaryStore().getInputStream(key);
        BinaryKey currentKey = BinaryKey.keyFor(IoUtil.readBytes(inputStream));
        assertEquals(key, currentKey);
        return res;
    }

    @Test
    public void shouldCleanupUnunsedValues() throws Exception {
        getBinaryStore().storeValue(new ByteArrayInputStream(IN_MEMORY_BINARY));
        List<BinaryKey> keys = new ArrayList<BinaryKey>();
        keys.add(IN_MEMORY_KEY);
        getBinaryStore().markAsUnused(keys);
        Thread.sleep(100);
        // now remove and test if still there
        getBinaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

        try {
            // no annotation used here to differ from other BinaryStoreException
            getBinaryStore().getInputStream(IN_MEMORY_KEY);
            fail("Key was not removed");
        } catch (BinaryStoreException ex) {
        }
    }

    @Test
    public void shouldAcceptStrategyHintsForStoringValues() throws Exception {
        BinaryValue res = getBinaryStore().storeValue(new ByteArrayInputStream(STORED_MEDIUM_BINARY), null);
        assertTrue(getBinaryStore().hasBinary(res.getKey()));
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
        storeAndValidate(STORED_MEDIUM_KEY, STORED_MEDIUM_BINARY);
        storeAndValidate(IN_MEMORY_KEY, IN_MEMORY_BINARY);

        List<String> keys = new ArrayList<String>(Arrays.asList(STORED_MEDIUM_KEY.toString(), IN_MEMORY_KEY.toString()));
        for (BinaryKey key : getBinaryStore().getAllBinaryKeys()) {
            keys.remove(key.toString());
        }
        assertEquals(0, keys.size());
    }

    @Test
    public void shouldExtractAndStoreMimeTypeWhenDetectorConfigured() throws RepositoryException, IOException {
        getBinaryStore().setMimeTypeDetector(new DummyMimeTypeDetector());
        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(IN_MEMORY_BINARY));
        // unclean stuff... a getter modifies silently data
        assertEquals(DummyMimeTypeDetector.DEFAULT_TYPE, getBinaryStore().getMimeType(binaryValue, "foobar.txt"));
    }

    @Test
    public void shouldExtractAndStoreTextWhenExtractorConfigured() throws Exception {
        TextExtractors extractors = new TextExtractors(Executors.newSingleThreadExecutor(), true,
                                                       Arrays.<TextExtractor>asList(new DummyTextExtractor()));
        BinaryStore binaryStore = getBinaryStore();
        binaryStore.setTextExtractors(extractors);

        BinaryValue binaryValue = getBinaryStore().storeValue(new ByteArrayInputStream(STORED_LARGE_BINARY));
        String extractedText = binaryStore.getText(binaryValue);
        if (extractedText == null) {
            // if nothing is found the first time, sleep and try again - Mongo on Windows seems to exibit this problem for some
            // reason
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            extractedText = binaryStore.getText(binaryValue);
        }
        assertEquals(DummyTextExtractor.EXTRACTED_TEXT, extractedText);
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
