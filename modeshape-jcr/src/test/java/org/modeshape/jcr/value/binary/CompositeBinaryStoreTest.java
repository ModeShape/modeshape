package org.modeshape.jcr.value.binary;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

/**
 * A {@link BinaryStore} that chains multiple BinaryStore implementation together.
 */
@ThreadSafe
public class CompositeBinaryStoreTest extends AbstractBinaryStoreTest {

    static CompositeBinaryStore store;
    static BinaryStore defaultStore;
    static BinaryStore alternativeStore;
    static BinaryStore anotherAlternativeStore;
    static String defaultHint;
    static String alternativeHint;
    static File directory;
    static File altDirectory;
    static File altDirectory2;

    protected static final int MIN_BINARY_SIZE = 20;


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
    @Test(expected = BinaryStoreException.class)
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        //the file system binary store will not store a 0 byte size content
        super.shouldStoreZeroLengthBinary();
    }

    @Test
    public void shouldAggregateBinaryKeysFromAllStores() throws BinaryStoreException, IOException {
        String text = randomString();
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(text.getBytes()));

        String text1 = randomString();
        BinaryValue v1 = alternativeStore.storeValue(new ByteArrayInputStream(text1.getBytes()));


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

        String text = randomString();

        BinaryValue v = store.storeValue(new ByteArrayInputStream(text.getBytes()));

        InputStream is = store.getInputStream(v.getKey());
        String s = IoUtil.read(is);

        InputStream is1 = defaultStore.getInputStream(v.getKey());
        String s1 = IoUtil.read(is1);

        assertThat(s, is(text));
        assertThat(s1, is(text));
    }

    @Test
    public void shouldLookInAllBinaryStoresForAKey() throws BinaryStoreException, IOException {
        String text = randomString();

        BinaryValue v = alternativeStore.storeValue(new ByteArrayInputStream(text.getBytes()));

        InputStream is = store.getInputStream(v.getKey());
        String s = IoUtil.read(is);

        assertThat(s, is(text));
        assertThat(store.hasBinary(v.getKey()), is(true));
    }

    @Test
    public void shouldStoreThingsInTheDefaultStoreWhenTheStrategyFails() throws BinaryStoreException, IOException {
        String text = randomString();

        BinaryValue v = store.storeValue(new ByteArrayInputStream(text.getBytes()), "this-hint-doesnt-reference-a-store");

        assertTrue(defaultStore.hasBinary(v.getKey()));

    }

    @Test
    public void shouldKnowWhatBinaryStoreAKeyIsIn() throws BinaryStoreException {

        String text = randomString();
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(text.getBytes()));

        String text1 = randomString();
        BinaryValue v1 = alternativeStore.storeValue(new ByteArrayInputStream(text1.getBytes()));

        assertEquals(defaultStore, store.findBinaryStoreContainingKey(v.getKey()));
        assertEquals(alternativeStore, store.findBinaryStoreContainingKey(v1.getKey()));
        assertNull(store.findBinaryStoreContainingKey(new BinaryKey("this-is-not-a-key")));
    }

    @Test
    public void shouldMoveBinaryKeysBetweenStores() throws BinaryStoreException {

        String text = randomString();
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(text.getBytes()));

        assertFalse(alternativeStore.hasBinary(v.getKey()));

        store.moveValue(v.getKey(), defaultHint, alternativeHint);

        assertTrue(alternativeStore.hasBinary(v.getKey()));
    }

    @Test( expected = BinaryStoreException.class )
    public void shouldRaiseAnExceptionWhenMovingAKeyThatDoesntExist() throws BinaryStoreException{
        store.moveValue(new BinaryKey("this-doesnt-exist"), alternativeHint);
    }


    @Test( expected = BinaryStoreException.class )
    public void shouldRaiseAnExceptionWhenMovingAKeyThatDoesntExistInTheSourceStore() throws BinaryStoreException{
        String text = randomString();
        BinaryValue v = defaultStore.storeValue(new ByteArrayInputStream(text.getBytes()));

        store.moveValue(v.getKey(), alternativeHint, defaultHint);
    }


    @Test
    public void shouldStoreThingsInTheFirstStoreWhenTheStrategyFailsAndNoDefaultStoreProvided() throws BinaryStoreException, IOException {

        BinaryStore localStoreWithoutADefaultStore;
        Map<String, BinaryStore> stores = new LinkedHashMap<String, BinaryStore>();
        stores.put("alt", alternativeStore);
        stores.put("also-alt", anotherAlternativeStore);

        localStoreWithoutADefaultStore = new CompositeBinaryStore(stores);
        localStoreWithoutADefaultStore.setMinimumBinarySizeInBytes(MIN_BINARY_SIZE);
        localStoreWithoutADefaultStore.start();

        String text = randomString();

        BinaryValue v = localStoreWithoutADefaultStore.storeValue(new ByteArrayInputStream(text.getBytes()), "this-hint-doesnt-reference-a-store");

        assertTrue(alternativeStore.hasBinary(v.getKey()));

    }


    private String randomString() {

        final String textBase = "The quick brown fox jumps over the lazy dog";
        StringBuilder builder = new StringBuilder();
        Random rand = new Random();
        while (builder.length() <= MIN_BINARY_SIZE) {
            builder.append(textBase.substring(0, rand.nextInt(textBase.length())));
        }

        return builder.toString();
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return store;
    }
}
