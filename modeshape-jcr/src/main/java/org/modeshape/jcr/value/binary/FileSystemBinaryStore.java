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
import org.modeshape.jcr.text.TextExtractorContext;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.FileLocks.WrappedLock;

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

    private static final boolean LOCK_WHEN_REMOVING_UNUSED_FILES;

    static {
        String osName = System.getProperty("os.name");
        LOCK_WHEN_REMOVING_UNUSED_FILES = (osName == null) || !osName.toLowerCase().contains("windows");
    }

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
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        File tmpFile = null;
        BinaryValue value = null;
        try {
            // Write the contents to a temporary file, and while we do grab the SHA-1 hash and the length ...
            HashingInputStream hashingStream = SecureHash.createHashingStream(Algorithm.SHA_1, stream);
            tmpFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            IoUtil.write(hashingStream,
                         new BufferedOutputStream(new FileOutputStream(tmpFile)),
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
            }

            if (extractors() != null && !(value instanceof InMemoryBinaryValue)) {
                // We never store the text for in-memory values ...
                extractors().extract(this, value, new TextExtractorContext());
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
                    Logger.getLogger(getClass()).warn(t,
                                                      JcrI18n.unableToDeleteTemporaryFile,
                                                      tmpFile.getAbsolutePath(),
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
                // There is an existing file, so go ahead and return a binary value that uses the existing file ...
                return new StoredBinaryValue(this, key, numberOfBytes);
            }

            // Otherwise, we need to persist the data, which we'll do by moving our temporary file ...
            moveFileExclusively(tmpFile, persistedFile);

        } finally {
            lock.unlock();
        }
        return new StoredBinaryValue(this, key, persistedFile.length());
    }

    protected final void moveFileExclusively( File original,
                                              File destination ) throws BinaryStoreException {
        try {
            // Make any missing directories ...
            destination.getParentFile().mkdirs();

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
            // Try to find it in the trash ...
            File trashedFile = findFile(trash, key, true);
            if (!trashedFile.exists() || !trashedFile.canRead()) {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, directory.getPath()));
            }
            // Otherwise, we found it in the trash, so move it from the trash into the regular storage ...
            moveFileExclusively(trashedFile, persistedFile);

            // Clean up any empty directories in the trash ...
            pruneEmptyDirectories(trash, trashedFile);
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
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        if (keys == null) {
            return;
        }
        for (BinaryKey key : keys) {
            markAsUnused(key);
            // mark the corresponding extracted text file as unused
            markAsUnused(createKeyFromSourceWithSuffix(key, EXTRACTED_TEXT_SUFFIX));
            // mark the corresponding stored mime-type file as unused
            markAsUnused(createKeyFromSourceWithSuffix(key, MIME_TYPE_SUFFIX));
        }
    }

    protected void markAsUnused( BinaryKey key ) throws BinaryStoreException {
        // Look for an existing file ...
        File persisted = findFile(directory, key, false);
        if (persisted == null || !persisted.exists()) {
            return;
        }

        // Find where it should live in the trash ...
        File trashed = findFile(trash, key, true);

        // Move the file into the trash ...
        moveFileExclusively(persisted, trashed);

        // And change the timestamp of the trashed file ...
        touch(trashed);

        // Clean up any empty directories in the trash ...
        pruneEmptyDirectories(directory, persisted);
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
                raf.close();
                fileLock.unlock();
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
        }
    }

    private void removeFilesOlderThan( long oldestTimestamp,
                                       File parentDirectory ) throws IOException {
        if (parentDirectory == null || !parentDirectory.exists() || parentDirectory.isFile()) {
            return;
        }
        boolean removed = false;
        for (File fileOrDir : parentDirectory.listFiles()) {
            if (fileOrDir == null || !fileOrDir.exists()) {
                continue;
            }
            // The file or directory should exist at this point (at least for now) ...
            if (fileOrDir.isDirectory()) {
                removeFilesOlderThan(oldestTimestamp, fileOrDir);
            } else if (fileOrDir.isFile()) {
                File file = fileOrDir;
                if (file.lastModified() < oldestTimestamp) {
                    if (LOCK_WHEN_REMOVING_UNUSED_FILES) {
                        // Get a write lock on the file we want to delete ...
                        WrappedLock fileLock = FileLocks.get().tryWriteLock(file);
                        if (fileLock != null) {
                            try {
                                // And then delete the file ...
                                file.delete();
                                removed = true;
                            } finally {
                                fileLock.unlock();
                            }
                        }
                        // otherwise it was locked, so just skip the file and we'll get it next time round
                    } else {
                        if (file.delete()) {
                            removed = true;
                        }
                    }
                }
            }
        }
        if (removed) {
            pruneEmptyDirectories(trash, parentDirectory);
        }
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
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
        BinaryKey extractedTextKey = createKeyFromSourceWithSuffix(source.getKey(), EXTRACTED_TEXT_SUFFIX);
        storeStringAtKey(extractedText, extractedTextKey);
    }

    private void storeStringAtKey( String string,
                                   BinaryKey key ) throws BinaryStoreException {
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
        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(binaryValue.getKey(), MIME_TYPE_SUFFIX);
        return storedStringAtKey(mimeTypeKey);
    }

    @Override
    protected void storeMimeType( BinaryValue binaryValue,
                                  String mimeType ) throws BinaryStoreException {
        BinaryKey mimeTypeKey = createKeyFromSourceWithSuffix(binaryValue.getKey(), MIME_TYPE_SUFFIX);
        storeStringAtKey(mimeType, mimeTypeKey);
    }

    private BinaryKey createKeyFromSourceWithSuffix( BinaryKey sourceKey,
                                                     String suffix ) {
        String extractTextKeyContent = sourceKey.toString() + suffix;
        return BinaryKey.keyFor(extractTextKeyContent.getBytes());
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() {
        // We could do this lazily, but doing so is more complicated than just grabbing them all at once.
        // So we'll implement the simple approach now ...
        Set<BinaryKey> keys = new HashSet<BinaryKey>();

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
                                        keys.add(key);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    private boolean isReadableDir( File file ) {
        return directory != null && directory.isDirectory() && directory.canRead();
    }
}
