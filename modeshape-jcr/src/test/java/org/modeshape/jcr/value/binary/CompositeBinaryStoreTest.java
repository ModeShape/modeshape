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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.junit.SkipOnOS;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Unit test for {@link CompositeBinaryStore}
 */
@ThreadSafe
public class CompositeBinaryStoreTest extends AbstractBinaryStoreTest {

    private static final int MIN_BINARY_SIZE = 20;
    private static final Random RANDOM = new Random();

    static CompositeBinaryStore store;
    static BinaryStore defaultStore;
    static BinaryStore alternativeStore;
    static BinaryStore anotherAlternativeStore;
    static String defaultHint;
    static String alternativeHint;
    static File directory;
    static File altDirectory;
    static File altDirectory2;

    @BeforeClass
    public static void beforeClass() {
        Map<String, BinaryStore> stores = new LinkedHashMap<String, BinaryStore>();

        directory = new File("target/cfsbs/");
        FileUtil.delete(directory);
        directory.mkdirs();
        defaultStore = new FileSystemBinaryStore(directory);

        stores.put("default", defaultStore);
        defaultHint = "default";

        altDirectory = new File("target/afsbs/");
        FileUtil.delete(altDirectory);
        altDirectory.mkdirs();
        alternativeStore = new FileSystemBinaryStore(altDirectory);

        altDirectory2 = new File("target/afsbs2/");
        FileUtil.delete(altDirectory2);
        altDirectory2.mkdirs();
        anotherAlternativeStore = new FileSystemBinaryStore(altDirectory2);

        stores.put("alternative", alternativeStore);
        alternativeHint = "alternative";

        store = new CompositeBinaryStore(stores);
        store.setMinimumBinarySizeInBytes(MIN_BINARY_SIZE);
        store.setMimeTypeDetector(DEFAULT_DETECTOR);

        store.start();
    }

    @AfterClass
    public static void afterClass() {
        store.shutdown();
        FileUtil.delete(directory);
        FileUtil.delete(altDirectory);
        FileUtil.delete(altDirectory2);
    }

    @Override
    @Test( expected = BinaryStoreException.class )
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        // the file system binary store will not store a 0 byte size content
        super.shouldStoreZeroLengthBinary();
    }

    @Test
    public void shouldAggregateBinaryKeysFromAllStores() throws BinaryStoreException {
        byte[] content = randomContent();
        defaultStore.storeValue(new ByteArrayInputStream(content), false);

        byte[] content1 = randomContent();
        alternativeStore.storeValue(new ByteArrayInputStream(content1), false);

        final Iterable<BinaryKey> allBinaryKeys = store.getAllBinaryKeys();
        final List<BinaryKey> expected = new ArrayList<BinaryKey>();

        for (BinaryKey binaryKey : defaultStore.getAllBinaryKeys()) {
            expected.add(binaryKey);
        }

        for (BinaryKey binaryKey : alternativeStore.getAllBinaryKeys()) {
            expected.add(binaryKey);
        }

        assertTrue(expected.size() > 1);
        assertThat(allBinaryKeys, hasItems(expected.toArray(new BinaryKey[expected.size()])));

    }

    @Test
    public void shouldPersistContentIntoTheDefaultStore() throws BinaryStoreException, IOException {
        byte[] content = randomContent();

        BinaryValue v = store.storeValue(new ByteArrayInputStream(content), false);

        InputStream is = store.getInputStream(v.getKey());
        byte[] storeContent = IoUtil.readBytes(is);

        InputStream is1 = defaultStore.getInputStream(v.getKey());
        byte[] defaultStoreContent = IoUtil.readBytes(is1);

        assertArrayEquals(content, storeContent);
        assertArrayEquals(content, defaultStoreContent);
    }

    @Test
    public void shouldLookInAllBinaryStoresForAKey() throws BinaryStoreException, IOException {
        byte[] content = randomContent();

        BinaryValue v = alternativeStore.storeValue(new ByteArrayInputStream(content), false);

        InputStream is = store.getInputStream(v.getKey());
        byte[] storedContent = IoUtil.readBytes(is);

        assertArrayEquals(content, storedContent);
        assertThat(store.hasBinary(v.getKey()), is(true));
    }

    @Test
    public void shouldStoreThingsInTheDefaultStoreWhenTheStrategyFails() throws BinaryStoreException {
        BinaryValue v = store.storeValue(new ByteArrayInputStream(randomContent()), "this-hint-doesnt-reference-a-store", false);
        assertTrue(defaultStore.hasBinary(v.getKey()));
    }

    @Test
    public void shouldKnowWhatBinaryStoreAKeyIsIn() throws BinaryStoreException {
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(randomContent()), false);
        BinaryValue v1 = alternativeStore.storeValue(new ByteArrayInputStream(randomContent()), false);

        assertEquals(defaultStore, store.findBinaryStoreContainingKey(v.getKey()));
        assertEquals(alternativeStore, store.findBinaryStoreContainingKey(v1.getKey()));
        assertNull(store.findBinaryStoreContainingKey(new BinaryKey("this-is-not-a-key")));
    }

    @Test
    public void shouldMoveBinaryKeysBetweenStores() throws BinaryStoreException {
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(randomContent()), false);
        assertFalse(alternativeStore.hasBinary(v.getKey()));

        store.moveValue(v.getKey(), defaultHint, alternativeHint);

        assertTrue(alternativeStore.hasBinary(v.getKey()));
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldRaiseAnExceptionWhenMovingAKeyThatDoesntExist() throws BinaryStoreException {
        store.moveValue(new BinaryKey("this-doesnt-exist"), alternativeHint);
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldRaiseAnExceptionWhenMovingAKeyThatDoesntExistInTheSourceStore() throws BinaryStoreException {
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(randomContent()), false);
        store.moveValue(v.getKey(), alternativeHint, defaultHint);
    }

    @Test
    public void shouldStoreThingsInTheFirstStoreWhenTheStrategyFailsAndNoDefaultStoreProvided() throws BinaryStoreException {
        BinaryStore localStoreWithoutADefaultStore;
        Map<String, BinaryStore> stores = new LinkedHashMap<String, BinaryStore>();
        stores.put("alt", alternativeStore);
        stores.put("also-alt", anotherAlternativeStore);

        localStoreWithoutADefaultStore = new CompositeBinaryStore(stores);
        localStoreWithoutADefaultStore.setMinimumBinarySizeInBytes(MIN_BINARY_SIZE);
        localStoreWithoutADefaultStore.start();

        BinaryValue v = localStoreWithoutADefaultStore.storeValue(new ByteArrayInputStream(randomContent()),
                                                                  "this-hint-doesnt-reference-a-store", false);

        assertTrue(alternativeStore.hasBinary(v.getKey()));
    }

    @Override
    @SkipOnOS(value = SkipOnOS.WINDOWS, description = "Sometimes file locks prevent the cleanup thread from removing values")
    @Test
    public void shouldCleanupUnunsedValues() throws Exception {
        super.shouldCleanupUnunsedValues();
    }

    private byte[] randomContent() {
        byte[] content = new byte[MIN_BINARY_SIZE + 1];
        RANDOM.nextBytes(content);
        return content;
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return store;
    }
}
