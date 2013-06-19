package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.TextExtractors;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.NullMimeTypeDetector;
import org.modeshape.jcr.text.TextExtractorContext;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that stores files in other BinaryStores. This store is initialized with a map of number of
 * BinaryStores. On retrieval, the CompositeBinaryStore will look in all the other BinaryStores for the value. When storing a
 * value, the CompositeBinaryStore may receive a StorageHint that MAY be used when determining which named BinaryStore to write
 * to. If a storage hint is not provided (or doesn't match a store), the value will be stored in the default store.
 */
public class CompositeBinaryStore implements BinaryStore {

    private static final String DEFAULT_STRATEGY_HINT = "default";
    private volatile TextExtractors extractors;
    private volatile MimeTypeDetector detector = NullMimeTypeDetector.INSTANCE;

    protected Logger logger = Logger.getLogger(getClass());

    private Map<String, BinaryStore> namedStores;
    private BinaryStore defaultBinaryStore;

    /**
     * Initialize a new CompositeBinaryStore using a Map of other BinaryKeys that are keyed by an implementer-provided key. The
     * named stores must include a default BinaryStore that will be used in the absence of storage hints.
     * 
     * @param namedStores a {@code Map} of inner stores, grouped by the hint.
     */
    public CompositeBinaryStore( Map<String, BinaryStore> namedStores ) {
        this.namedStores = namedStores;
        this.defaultBinaryStore = null;
    }

    /**
     * Initialize the store, and initialize all the named stores.
     */
    @Override
    public void start() {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            bs.start();
        }

    }

    /**
     * Shut down all the named stores
     */
    @Override
    public void shutdown() {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            bs.shutdown();
        }
    }

    @Override
    public long getMinimumBinarySizeInBytes() {
        long minimumBinarySize = Long.MAX_VALUE;

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            if (minimumBinarySize > bs.getMinimumBinarySizeInBytes()) {
                minimumBinarySize = bs.getMinimumBinarySizeInBytes();
            }
        }

        return minimumBinarySize;
    }

    @Override
    public void setMinimumBinarySizeInBytes( long minSizeInBytes ) {

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            bs.setMinimumBinarySizeInBytes(minSizeInBytes);
        }
    }

    @Override
    public void setTextExtractors( TextExtractors textExtractors ) {
        CheckArg.isNotNull(textExtractors, "textExtractors");
        this.extractors = textExtractors;

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();
        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            bs.setTextExtractors(textExtractors);
        }
    }

    @Override
    public void setMimeTypeDetector( MimeTypeDetector mimeTypeDetector ) {
        this.detector = mimeTypeDetector != null ? mimeTypeDetector : NullMimeTypeDetector.INSTANCE;

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            bs.setMimeTypeDetector(mimeTypeDetector);
        }
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        return storeValue(stream, DEFAULT_STRATEGY_HINT);
    }

    @Override
    public BinaryValue storeValue( InputStream stream,
                                   String hint ) throws BinaryStoreException {
        BinaryStore binaryStore = selectBinaryStore(hint);
        BinaryValue bv = binaryStore.storeValue(stream);
        logger.debug("Stored binary " + bv.getKey() + " into binary store " + binaryStore);
        return bv;
    }

    /**
     * Move a value from one named store to another store
     * 
     * @param key Binary key to transfer from the source store to the destination store
     * @param source a hint for discovering the source repository; may be null
     * @param destination a hint for discovering the destination repository
     * @return the {@link BinaryKey} value of the moved binary, never {@code null}
     * @throws BinaryStoreException if a source store cannot be found or the source store does not contain the binary key
     */
    public BinaryKey moveValue( BinaryKey key,
                                String source,
                                String destination ) throws BinaryStoreException {
        final BinaryStore sourceStore;

        if (source == null) {
            sourceStore = findBinaryStoreContainingKey(key);
        } else {
            sourceStore = selectBinaryStore(source);
        }

        // could not find source store, or
        if (sourceStore == null || !sourceStore.hasBinary(key)) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, sourceStore));
        }

        BinaryStore destinationStore = selectBinaryStore(destination);

        // key is already in the destination store
        if (sourceStore.equals(destinationStore)) {
            return key;
        }

        final BinaryValue binaryValue = storeValue(sourceStore.getInputStream(key), destination);
        sourceStore.markAsUnused(java.util.Collections.singleton(key));

        return binaryValue.getKey();
    }

    /**
     * Move a BinaryKey to a named store
     * 
     * @param key Binary key to transfer from the source store to the destination store
     * @param destination a hint for discovering the destination repository
     * @throws BinaryStoreException if anything unexpected fails
     */
    public void moveValue( BinaryKey key,
                           String destination ) throws BinaryStoreException {
        moveValue(key, null, destination);
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            final Map.Entry<String, BinaryStore> entry = it.next();

            final String binaryStoreKey = entry.getKey();

            BinaryStore binaryStore = entry.getValue();
            logger.trace("Checking binary store " + binaryStoreKey + " for key " + key);
            try {
                return binaryStore.getInputStream(key);
            } catch (BinaryStoreException e) {
                // this exception is "normal", and is thrown
                logger.trace(e, "The named store " + binaryStoreKey + " raised exception");
            }
        }

        throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, this.toString()));
    }

    @Override
    public boolean hasBinary( BinaryKey key ) {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            BinaryStore bs = it.next().getValue();
            if (bs.hasBinary(key)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings( "unused" )
    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String, BinaryStore> entry = it.next();

            final String binaryStoreKey = entry.getKey();
            BinaryStore bs = entry.getValue();

            try {
                bs.markAsUnused(keys);
            } catch (BinaryStoreException e) {
                logger.debug(e, "The named store " + binaryStoreKey + " raised exception");
            }
        }
    }

    @SuppressWarnings( "unused" )
    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String, BinaryStore> entry = it.next();

            final String binaryStoreKey = entry.getKey();
            BinaryStore bs = entry.getValue();

            try {
                bs.removeValuesUnusedLongerThan(minimumAge, unit);
            } catch (BinaryStoreException e) {
                logger.debug(e, "The named store " + binaryStoreKey + " raised exception");
            }
        }
    }

    @Override
    public String getText( BinaryValue binary ) throws BinaryStoreException {

        if (binary instanceof InMemoryBinaryValue) {
            if (extractors == null || !extractors.extractionEnabled()) {
                return null;
            }

            // The extracted text will never be stored, so try directly using the text extractors ...
            return extractors.extract((InMemoryBinaryValue)binary, new TextExtractorContext(detector));
        }

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String, BinaryStore> entry = it.next();

            final String binaryStoreKey = entry.getKey();
            BinaryStore bs = entry.getValue();
            try {
                if (bs.hasBinary(binary.getKey())) {
                    return bs.getText(binary);
                }
            } catch (BinaryStoreException e) {
                logger.debug(e, "The named store " + binaryStoreKey + " raised exception");
                if (!it.hasNext()) {
                    throw e;
                }
            }
        }

        throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(binary.getKey(), this));
    }

    @Override
    public String getMimeType( BinaryValue binary,
                               String name ) throws IOException, RepositoryException {
        if (detector == null) {
            return null;
        }

        String detectedMimeType = detector.mimeTypeOf(name, binary);
        if (binary instanceof InMemoryBinaryValue) {
            return detectedMimeType;
        }

        Iterator<Map.Entry<String, BinaryStore>> it = getNamedStoreIterator();

        while (it.hasNext()) {
            Map.Entry<String, BinaryStore> entry = it.next();

            final String binaryStoreKey = entry.getKey();
            BinaryStore bs = entry.getValue();

            try {
                if (bs.hasBinary(binary.getKey())) {
                    return bs.getMimeType(binary, name);
                }
            } catch (BinaryStoreException e) {
                logger.debug(e, "The named store " + binaryStoreKey + " raised exception");
                if (!it.hasNext()) {
                    throw e;
                }
            }
        }

        throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(binary.getKey(), this));
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {

        Iterable<BinaryKey> generatedIterable = new HashSet<BinaryKey>();
        Iterator<Map.Entry<String, BinaryStore>> binaryStoreIterator = getNamedStoreIterator();

        while (binaryStoreIterator.hasNext()) {
            BinaryStore bs = binaryStoreIterator.next().getValue();

            generatedIterable = Collections.concat(generatedIterable, bs.getAllBinaryKeys());
        }

        return generatedIterable;
    }

    /**
     * Get an iterator over all the named stores
     * 
     * @return an iterator over the map of binary stores and their given names
     */
    public Iterator<Map.Entry<String, BinaryStore>> getNamedStoreIterator() {
        return namedStores.entrySet().iterator();
    }

    /**
     * Get the named binary store that contains the key
     * 
     * @param key the key to the binary content; never null
     * @return the BinaryStore that contains the given key
     */
    public BinaryStore findBinaryStoreContainingKey( BinaryKey key ) {
        Iterator<Map.Entry<String, BinaryStore>> binaryStoreIterator = getNamedStoreIterator();

        while (binaryStoreIterator.hasNext()) {
            BinaryStore bs = binaryStoreIterator.next().getValue();
            if (bs.hasBinary(key)) {
                return bs;
            }
        }

        return null;
    }

    /**
     * Select a named binary store for the given hint
     * 
     * @param hint a hint to a binary store; possibly null
     * @return a named BinaryStore from the hint, or the default store
     */
    private BinaryStore selectBinaryStore( String hint ) {

        BinaryStore namedBinaryStore = null;

        if (hint != null) {
            logger.trace("Selecting named binary store for hint: " + hint);
            namedBinaryStore = namedStores.get(hint);
        }

        if (namedBinaryStore == null) {
            namedBinaryStore = getDefaultBinaryStore();
        }

        logger.trace("Selected binary store: " + namedBinaryStore.toString());

        return namedBinaryStore;
    }

    private BinaryStore getDefaultBinaryStore() {
        if (defaultBinaryStore == null) {
            if (namedStores.containsKey(DEFAULT_STRATEGY_HINT)) {
                defaultBinaryStore = namedStores.get(DEFAULT_STRATEGY_HINT);
            } else {
                logger.trace("Did not find a named binary store with the key 'default', picking the first binary store in the list");
                final Iterator<BinaryStore> iterator = namedStores.values().iterator();

                if (iterator.hasNext()) {
                    defaultBinaryStore = iterator.next();
                }
            }
        }

        return defaultBinaryStore;
    }

}
