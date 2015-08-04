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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.SecureHash.HashingInputStream;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} that stores files in a directory on the file system. The store does use file locks to prevent other
 * processes from concurrently writing the files, and it also uses an internal set of locks to prevent mulitple threads from
 * simultaneously writing to the persisted files.
 */
@ThreadSafe
public class FileSystemBinaryStore extends AbstractBinaryStore {

    private static final String EXTRACTED_TEXT_SUFFIX = "-extracted-text";
    private static final String MIME_TYPE_SUFFIX = "-mime-type";

    private static final ConcurrentHashMap<String, FileSystemBinaryStore> INSTANCES = new ConcurrentHashMap<String, FileSystemBinaryStore>();

    public static FileSystemBinaryStore create( File directory ) {
        String key = directory.getAbsolutePath();
        FileSystemBinaryStore store = INSTANCES.get(key);
        if (store == null) {
            store = new FileSystemBinaryStore(directory);
            FileSystemBinaryStore existing = INSTANCES.putIfAbsent(key, store);
            if (existing != null) {
                store = existing;
            }
        }
        return store;
    }

    private static final String TEMP_FILE_PREFIX = "ms-fs-binstore";
    private static final String TEMP_FILE_SUFFIX = "hashing";
    protected static final String TRASH_DIRECTORY_NAME = "trash";

    private final File directory;
    private final File trash;
    private final NamedLocks locks = new NamedLocks();
    private volatile boolean initialized = false;

    protected FileSystemBinaryStore( File directory ) {
        this.directory = directory;
        this.trash = new File(this.directory, TRASH_DIRECTORY_NAME);
    }

    public File getDirectory() {
        return directory;
    }

    @Override
    public BinaryValue storeValue( InputStream stream, boolean markAsUnused ) throws BinaryStoreException {
        File tmpFile = null;
        BinaryValue value = null;
        try {
            // Write the contents to a temporary file, and while we do grab the SHA-1 hash and the length ...
            HashingInputStream hashingStream = SecureHash.createHashingStream(Algorithm.SHA_1, stream);
            tmpFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            IoUtil.write(hashingStream, new BufferedOutputStream(new FileOutputStream(tmpFile)),
                         AbstractBinaryStore.MEDIUM_BUFFER_SIZE);
            hashingStream.close();
            byte[] sha1 = hashingStream.getHash();
            BinaryKey key = new BinaryKey(sha1);

            final long numberOfBytes = tmpFile.length();
            if (numberOfBytes < getMinimumBinarySizeInBytes()) {
                // The content is small enough to just store in-memory ...
                byte[] content = IoUtil.readBytes(tmpFile);
                tmpFile.delete();
                value = new InMemoryBinaryValue(this, key, content);
            } else {
                value = saveTempFileToStore(tmpFile, key, numberOfBytes);
                if (markAsUnused) {
                    markAsUnused(key);
                }
            }
            return value;
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        } finally {
            if (tmpFile != null) {
                try {
                    tmpFile.delete();
                } catch (Throwable t) {
                    Logger.getLogger(getClass()).warn(t, JcrI18n.unableToDeleteTemporaryFile, tmpFile.getAbsolutePath(),
                                                      t.getMessage());
                }
            }
        }
    }

    private BinaryValue saveTempFileToStore( File tmpFile,
                                             BinaryKey key,
                                             long numberOfBytes ) throws BinaryStoreException {
        // Now that we know the SHA-1, find the File object that corresponds to the existing persisted file ...
        File persistedFile = findFile(directory, key, true);

        // And before we do anything, obtain the lock for the SHA1 ...
        final Lock lock = locks.writeLock(key.toString());
        try {
            // Now that we know the SHA-1, see if there is already an existing file in storage ...
            if (persistedFile.exists()) {
                //if there's a trash file for this file remove it
                removeTrashFile(key);
                // There is an existing file, so go ahead and return a binary value that uses the existing file ...
                return new StoredBinaryValue(this, key, numberOfBytes);
            }

            // Otherwise, we need to persist the data, which we'll do by moving our temporary file ...
            moveFileExclusively(tmpFile, persistedFile, key);

        } finally {
            lock.unlock();
        }
        return new StoredBinaryValue(this, key, persistedFile.length());
    }

    private void sleep( long millis ) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            // do nothing
        }
    }

    private File getTrashFile( BinaryKey key, boolean createIfAbsent ) throws BinaryStoreException {
        File trashFile = findFile(trash, key, createIfAbsent);
        if (trashFile.exists() && trashFile.canRead()) {
            // we found an existing trash file
            return trashFile;
        }

        if (!createIfAbsent) {
            // there is no trash file and we shouldn't create one
            return null;
        }

        // create a trash file only if there is a valid corresponding persisted file
        File persistedFile = findFile(directory, key, false);
        if (!persistedFile.exists()) {
            // there is no persistent file, so it doesn't make sense to create a trash file
            return null;
        }
        Lock writeLock = locks.writeLock(key.toString());
        try {
            if (!trashFile.exists() || !trashFile.canRead()) {
                IoUtil.write("", new BufferedOutputStream(new FileOutputStream(trashFile)));
            }
            return trashFile;
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } finally {
            writeLock.unlock();
        }
    }

    private boolean removeTrashFile(BinaryKey key) throws BinaryStoreException {
        File trashFile = getTrashFile(key, false);
        if (trashFile == null) {
            return false;
        }

        final Lock lock = locks.writeLock(key.toString());
        try {
            if (trashFile.exists()) {
                //try to remove the trash file first
                if (!trashFile.delete()) {
                    //we weren't able to remove it for some reason, so at least touch it
                    touch(trashFile);
                    return false;
                }
                //we successfully removed the file
                return true;
            }
            //some other thread already removed the file
            return false;
        } finally {
            lock.unlock();
        }
    }

    protected void moveFileExclusively( File original,
                                        File destination,
                                        BinaryKey key ) throws BinaryStoreException {
        try {
            // Make any missing directories, and try repeatedly (on Windows, this might fail the first few times) ...
            for (int i = 0; i != 5; ++i) {
                destination.getParentFile().mkdirs();
                if (destination.getParentFile().exists()) break;
                sleep(500L); // wait 500 seconds before trying again
            }
            if (!destination.getParentFile().exists()) {
                String path = destination.getParentFile().getAbsolutePath();
                throw new BinaryStoreException(JcrI18n.unableToCreateDirectoryForBinaryStore.text(path, key));
            }

            // First, obtain an exclusive lock on the original file ...
            FileLocks.WrappedLock fileLock = FileLocks.get().writeLock(original);
            try {
                // The perform the move/rename (which may not work on all platforms) ...
                if (original.renameTo(destination)) {
                    // This worked, so simply return ...
                    return;
                }
            } finally {
                fileLock.unlock();
            }

            // The move/rename didn't work, so we have to copy from the original ...

            // Create the new file and obtain an exclusive lock on it ...
            final int bufferSize = AbstractBinaryStore.bestBufferSize(original.length());
            fileLock = FileLocks.get().writeLock(destination);
            try {
                // Create a buffered output stream to the destination file ...
                // (Note that the Channels.newOutputStream does not create a buffered stream)
                FileChannel destinationChannel = fileLock.lockedFileChannel();
                OutputStream output = Channels.newOutputStream(destinationChannel);
                output = new BufferedOutputStream(output, bufferSize);

                // Create an input stream to the original file ...
                // (Note that the Channels.newInputStream does not create a buffered stream)
                RandomAccessFile originalRaf = new RandomAccessFile(original, "r");
                FileChannel originalChannel = originalRaf.getChannel();
                InputStream input = Channels.newInputStream(originalChannel);
                input = new BufferedInputStream(input, bufferSize);

                // Copy the content ...
                IoUtil.write(input, output, bufferSize);

                // Close the file ...
                originalRaf.close();
            } finally {
                try {
                    fileLock.unlock();
                } finally {
                    original.delete();
                }
            }
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        }
    }

    protected final File findFile( File directory,
                                   BinaryKey key,
                                   boolean createParentDirsIfMissing ) throws BinaryStoreException {
        if (!initialized) {
            initializeStorage(directory);
            initialized = true;
        }
        String sha1 = key.toString();
        File first = new File(directory, sha1.substring(0, 2));
        File second = new File(first, sha1.substring(2, 4));
        File third = new File(second, sha1.substring(4, 6));
        if (createParentDirsIfMissing) {
            third.mkdirs();
        }
        File file = new File(third, sha1);
        return file;
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        // Now that we know the SHA-1, find the File object that corresponds to the existing persisted file ...
        File persistedFile = findFile(directory, key, false);
        if (!persistedFile.exists() || !persistedFile.canRead()) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, directory.getPath()));
        }

        // We now know that the file (which does exist) is not being written by this process, but another
        // process might be actively writing to it. So use an InputStream that lazily obtains a shared lock
        // when the stream is used, and always releases the lock (even in the case of exceptions).
        return new SharedLockingInputStream(key, persistedFile, locks);
    }

    @SuppressWarnings( "unused" )
    protected void initializeStorage( File directory ) throws BinaryStoreException {
        // do nothing by default
    }

    @Override
    public void markAsUsed( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        if (keys == null) {
            return;
        }
        for (BinaryKey key : keys) {
            removeAllTrashFilesFor(key);
        }
    }

    /**
     * Upgrades the contents of the trash directory to use the new storage format, since MODE-2302.
     * This is only meant to be used as an upgrade function.
     *
     * @throws BinaryStoreException if anything unexpected fails
     */
    public void upgradeTrashContentFormat() throws BinaryStoreException {
        moveTrashFilesToMainStorage(trash);
    }

    private void moveTrashFilesToMainStorage( File trash ) throws BinaryStoreException  {
        File[] files = trash.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                moveTrashFilesToMainStorage(file);
            } else {
                if (file.canRead() && file.isFile() && file.length() > 0) {
                    // this is an old style file which we need to convert
                    BinaryKey key = new BinaryKey(file.getName());
                    // create the empty shell first
                    File persistedFile = findFile(directory, key, true);
                    // move it to main storage
                    moveFileExclusively(file, persistedFile, key);
                    // and write out a new trash file
                    getTrashFile(key, true);
                }
            }
        }
    }

    protected boolean removeAllTrashFilesFor( BinaryKey key ) throws BinaryStoreException {
        // remove the trash file for the main binary, extracted text and mime-type
        return removeTrashFile(key) |
               removeTrashFile(createKeyFromSourceWithSuffix(key, EXTRACTED_TEXT_SUFFIX)) |
               removeTrashFile(createKeyFromSourceWithSuffix(key, MIME_TYPE_SUFFIX));
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        if (keys == null) {
            return;
        }
        for (BinaryKey key : keys) {
            markAsUnused(key);
        }
    }

    protected void markAsUnused( BinaryKey key ) throws BinaryStoreException {
        File persistedFile = findFile(directory, key, false);
        if (!persistedFile.exists()) {
            // if the persisted file doesn't exist, there's nothing to do
            return;
        }
        // create a trash file for the main binary
        getTrashFile(key, true);

        BinaryKey textExtractionKey = createKeyFromSourceWithSuffix(key, EXTRACTED_TEXT_SUFFIX);
        File textFile = findFile(directory, textExtractionKey, false);
        if (textFile.exists()) {
            // create a trash file for the extracted text binary
            getTrashFile(textExtractionKey, true);
        }

        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(key, MIME_TYPE_SUFFIX);
        File mimeTypeFile = findFile(directory, mimeTypeKey, false);
        if (mimeTypeFile.exists()) {
            // create a trash file for the mime-type binary
            getTrashFile(mimeTypeKey, true);
        }
    }

    protected void touch( File file ) throws BinaryStoreException {
        try {
            // We could just set the last modified time, but we should obtain a lock on the file ...
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            Lock fileLock = FileLocks.get().writeLock(file);
            try {
                // Change the length to the current value, which updates the last modified timestamp ...
                raf.setLength(raf.length());
            } finally {
                try {
                    raf.close();
                } finally {
                    fileLock.unlock();
                }
            }
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Remove any empty directories above <code>removeable</code> but below <code>directory</code>
     *
     * @param directory the top-level directory to keep; may not be null and must be an ancestor of <code>removeable</code>
     * @param removeable the file or directory above which any empty directories can be removed; may not be null
     */
    protected void pruneEmptyDirectories( File directory,
                                          File removeable ) {
        assert directory != null;
        assert removeable != null;
        if (directory.equals(removeable)) {
            return;
        }

        assert isAncestor(directory, removeable);
        while (!removeable.equals(directory)) {
            if (removeable.exists()) {
                // It exists, so try to delete it...
                if (!removeable.delete()) {
                    // Couldn't delete it, so stop
                    return;
                }
            }
            removeable = removeable.getParentFile();
        }
    }

    private boolean isAncestor( File ancestor,
                                File descendant ) {
        File parent = descendant;
        while (parent != null) {
            if (parent.equals(ancestor)) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        long oldestTimestamp = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(minimumAge, unit);
        try {
            removeFilesOlderThan(oldestTimestamp, trash);
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } catch (BinaryStoreException bse) {
            throw bse;
        }
    }

    private void removeFilesOlderThan( long oldestTimestamp,
                                       File parentDirectory ) throws IOException, BinaryStoreException {
        if (parentDirectory == null || !parentDirectory.exists() || parentDirectory.isFile()) {
            return;
        }
        boolean pruneTrashRequired = false;
        boolean pruneMainRequired = false;
        File[] files = parentDirectory.listFiles();
        if (files == null) {
            return;
        }
        File mainDirectoryToPrune = null;
        for (File fileOrDir : files) {
            if (fileOrDir == null || !fileOrDir.exists()) {
                continue;
            }
            // The file or directory should exist at this point (at least for now) ...
            if (fileOrDir.isDirectory()) {
                removeFilesOlderThan(oldestTimestamp, fileOrDir);
            } else if (fileOrDir.isFile()) {
                File file = fileOrDir;
                if (file.lastModified() < oldestTimestamp) {
                    // we know that the files in the trash have the name as sha1
                    String sha1 = file.getName();
                    BinaryKey key = new BinaryKey(sha1);
                    File persistedFile = findFile(directory, key, false);
                    if (persistedFile.exists() && persistedFile.canRead()) {
                        Lock lock = locks.writeLock(sha1);
                        try {
                            if (persistedFile.exists()) {
                                // only remove the trash files if we successfully deleted the main file
                                // otherwise we'll try this again later on
                                if (persistedFile.delete() && removeTrashFile(key)) {
                                    pruneTrashRequired = true;
                                    pruneMainRequired = true;
                                    mainDirectoryToPrune = persistedFile.getParentFile();
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        // the persisted file doesn't exist anymore, so remove all trash files
                        if (removeAllTrashFilesFor(key)) {
                            pruneTrashRequired = true;
                        }
                    }
                }
            }
        }
        if (pruneTrashRequired) {
            // at least one file was removed, so cleanup the dir structure
            pruneEmptyDirectories(trash, parentDirectory);
        }
        if (pruneMainRequired && mainDirectoryToPrune != null) {
            pruneEmptyDirectories(directory, mainDirectoryToPrune);
        }
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        if (!binaryValueExists(source)) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), directory));
        }
        BinaryKey extractedTextKey = createKeyFromSourceWithSuffix(source.getKey(), EXTRACTED_TEXT_SUFFIX);
        return storedStringAtKey(extractedTextKey);
    }

    private String storedStringAtKey( BinaryKey key ) throws BinaryStoreException {
        InputStream is = null;
        try {
            is = getInputStream(key);
        } catch (BinaryStoreException e) {
            // means the file wasn't found (isn't available yet) in the store
            return null;
        }

        try {
            return IoUtil.read(is);
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                       String extractedText ) throws BinaryStoreException {
        // Look for an existing file ...
        if (!binaryValueExists(source)) {
            return;
        }
        BinaryKey extractedTextKey = createKeyFromSourceWithSuffix(source.getKey(), EXTRACTED_TEXT_SUFFIX);
        storeStringAtKey(extractedText, extractedTextKey);
    }

    private void storeStringAtKey( String string,
                                   BinaryKey key) throws BinaryStoreException {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX + EXTRACTED_TEXT_SUFFIX);
            IoUtil.write(string, new BufferedOutputStream(new FileOutputStream(tmpFile)));
            saveTempFileToStore(tmpFile, key, tmpFile.length());
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    @Override
    protected String getStoredMimeType( BinaryValue binaryValue ) throws BinaryStoreException {
        if (!binaryValueExists(binaryValue)) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(binaryValue.getKey(), directory));
        }
        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(binaryValue.getKey(), MIME_TYPE_SUFFIX);
        return storedStringAtKey(mimeTypeKey);
    }

    @Override
    protected void storeMimeType( BinaryValue binaryValue,
                                  String mimeType ) throws BinaryStoreException {
        if (!binaryValueExists(binaryValue)) {
            return;
        }
        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(binaryValue.getKey(), MIME_TYPE_SUFFIX);
        storeStringAtKey(mimeType, mimeTypeKey);
    }

    private boolean binaryValueExists( BinaryValue binaryValue ) throws BinaryStoreException {
        File fileInMainStorage = findFile(directory, binaryValue.getKey(), false);
        return fileInMainStorage.exists() && fileInMainStorage.canRead();
    }

    private BinaryKey createKeyFromSourceWithSuffix( BinaryKey sourceKey,
                                                     String suffix ) {
        String extractTextKeyContent = sourceKey.toString() + suffix;
        return BinaryKey.keyFor(extractTextKeyContent.getBytes());
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        // We could do this lazily, but doing so is more complicated than just grabbing them all at once.
        // So we'll implement the simple approach now ...
        Set<BinaryKey> keys = new HashSet<>();
        Set<BinaryKey> keysToExclude = new HashSet<>();
        // Iterate over all of the files in the directory structure (excluding trash) and assemble the results ...
        if (isReadableDir(directory)) {
            for (File first : directory.listFiles()) {
                if (isReadableDir(first)) {
                    for (File second : first.listFiles()) {
                        if (isReadableDir(second)) {
                            for (File third : second.listFiles()) {
                                if (isReadableDir(third)) {
                                    for (File file : third.listFiles()) {
                                        if (!file.canRead() || !file.isFile()) continue;
                                        String filename = file.getName();
                                        // SHA-1s should be 40 characters ...
                                        if (filename.length() != 40) continue;
                                        BinaryKey key = new BinaryKey(file.getName());
                                        // There is a trash file for this key, meaning the file is unused
                                        if (getTrashFile(key, false) != null) continue;

                                        keys.add(key);

                                        // exclude mime types (which will be seen as binaries)
                                        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(key, MIME_TYPE_SUFFIX);
                                        keysToExclude.add(mimeTypeKey);

                                        // exclude extracted text
                                        BinaryKey textKey = createKeyFromSourceWithSuffix(key, EXTRACTED_TEXT_SUFFIX);
                                        keysToExclude.add(textKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        keys.removeAll(keysToExclude);
        return keys;
    }

    private boolean isReadableDir( File dir ) {
        return dir != null && dir.isDirectory() && dir.canRead();
    }
}
