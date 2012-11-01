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
package org.modeshape.jcr;

import javax.jcr.RepositoryException;
import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.InfinispanUtil.Sequence;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A service used to generate backups from content and restore repository content from backups.
 */
public class BackupService {

    protected static final Logger LOGGER = Logger.getLogger(BackupService.class);

    protected static final String CHANGED_AREA_DIR_NAME = "changes";
    protected static final String BINARY_AREA_DIR_NAME = "binaries";
    protected static final String DOCUMENTS_FILENAME_PREFIX = "documents";
    protected static final String SUMMARY_FILE_NAME = "summary_of_changes.json";
    protected static final String BINARY_EXTENSION = ".bin";
    protected static final int NUM_CHARS_IN_FILENAME_SUFFIX = 6;

    /**
     * By default, 100K nodes will be exported to a single backup file. So, if each node requied about 200 bytes (compressed), the
     * resulting files will be about 19 MB in size.
     */
    public static final long DEFAULT_NUMBER_OF_DOCUMENTS_IN_BACKUP_FILES = 100000L;
    public static final boolean DEFAULT_COMPRESS = true;

    protected static class FieldName {
        public static final String UNUSED_BINARY_KEYS = "unusedBinaryKeys";
    }

    private final RunningState runningState;
    private final LocalDocumentStore documentStore;
    private final BinaryStore binaryStore;
    private final RepositoryCache repositoryCache;

    protected BackupService( RunningState runningState ) {
        this.runningState = runningState;
        //backup restore should not care about federation, hence only the local store should be used
        documentStore = this.runningState.documentStore().localStore();
        binaryStore = this.runningState.binaryStore();
        repositoryCache = this.runningState.repositoryCache();
    }

    /**
     * Shut down this service and immediately terminate all currently-running backup operations.
     */
    protected void shutdown() {
    }

    /**
     * Start asynchronously backing up the repository.
     * 
     * @param backupDirectory the directory on the file system into which the backup should be placed; this directory should
     *        typically not exist
     * @return the problems that occurred during the backup process
     * @throws RepositoryException if the backup operation cannot be run
     */
    public org.modeshape.jcr.api.Problems backupRepository( File backupDirectory ) throws RepositoryException {
        return backupRepository(backupDirectory, DEFAULT_NUMBER_OF_DOCUMENTS_IN_BACKUP_FILES, DEFAULT_COMPRESS);
    }

    /**
     * Start asynchronously backing up the repository.
     * 
     * @param backupDirectory the directory on the file system into which the backup should be placed; this directory should
     *        typically not exist
     * @param documentsPerFile the maximum number of documents to place within a single backup file; must be positive
     * @param compress true if the backup files should be compressed, or false otherwise
     * @return the problems that occurred during the backup process
     * @throws RepositoryException if the backup operation cannot be run
     */
    public org.modeshape.jcr.api.Problems backupRepository( File backupDirectory,
                                                            long documentsPerFile,
                                                            boolean compress ) throws RepositoryException {
        // Create the activity ...
        final BackupActivity backupActivity = createBackupActivity(backupDirectory, documentsPerFile, compress);

        // Run the backup and return the problems ...
        return new JcrProblems(backupActivity.execute());
    }

    /**
     * Start asynchronously backing up the repository.
     * 
     * @param repository the JCR repository to be backed up; may not be null
     * @param backupDirectory the directory on the file system that contains the backup; this directory obviously must exist
     * @return the problems that occurred during the restore process
     * @throws RepositoryException if the restoration operation cannot be run
     */
    public org.modeshape.jcr.api.Problems restoreRepository( final JcrRepository repository,
                                                             final File backupDirectory ) throws RepositoryException {
        final String backupLocString = backupDirectory.getAbsolutePath();
        LOGGER.debug("Beginning restore of '{0}' repository from {1}", repository.getName(), backupLocString);
        // Put the repository into the 'restoring' state ...
        repository.prepareToRestore();

        // Create the activity ...
        final RestoreActivity restoreActivity = createRestoreActivity(backupDirectory);

        org.modeshape.jcr.api.Problems problems = new JcrProblems(restoreActivity.execute());
        if (!problems.hasProblems()) {
            // restart the repository ...
            try {
                repository.completeRestore();
            } catch (Throwable t) {
                restoreActivity.problems.addError(JcrI18n.repositoryCannotBeRestartedAfterRestore,
                                                  repository.getName(),
                                                  t.getMessage());
            }
        }
        LOGGER.debug("Completed restore of '{0}' repository from {1}", repository.getName(), backupLocString);
        return problems;
    }

    /**
     * Create a new {@link BackupActivity activity} instance that can back up the content of the repository as it exists at the
     * time the activity is executed.
     * 
     * @param backupDirectory the directory on the file system into which the backup should be placed; this directory should
     *        typically not exist
     * @param documentsPerFile the maximum number of documents to place within a single backup file; must be positive
     * @param compress true if the backup files should be compressed, or false otherwise
     * @return the backup activity; never null
     */
    public BackupActivity createBackupActivity( File backupDirectory,
                                                long documentsPerFile,
                                                boolean compress ) {
        return new BackupActivity(backupDirectory, documentStore, binaryStore, repositoryCache, documentsPerFile, compress);
    }

    /**
     * Create a new {@link RestoreActivity activity} instance that can restore the content of the repository to the state as it
     * exists in the specified backup directory.
     * 
     * @param backupDirectory the directory on the file system that contains the backup; this directory obviously must exist
     * @return the restore activity; never null
     */
    public RestoreActivity createRestoreActivity( File backupDirectory ) {
        return new RestoreActivity(backupDirectory, documentStore, binaryStore, repositoryCache);
    }

    /**
     * An abstract activity used for the various backup and restore operations.
     */
    @NotThreadSafe
    public static abstract class Activity {
        protected final RepositoryCache repositoryCache;
        protected final File backupDirectory;
        protected final File changeDirectory;
        protected final File binaryDirectory;
        protected final org.modeshape.jcr.cache.document.LocalDocumentStore documentStore;
        protected final BinaryStore binaryStore;
        protected final SimpleProblems problems;
        private final String backupLocation;

        protected Activity( File backupDirectory,
                            org.modeshape.jcr.cache.document.LocalDocumentStore documentStore,
                            BinaryStore binaryStore,
                            RepositoryCache repositoryCache ) {
            this.backupDirectory = backupDirectory;
            this.changeDirectory = new File(this.backupDirectory, CHANGED_AREA_DIR_NAME);
            this.binaryDirectory = new File(this.backupDirectory, BINARY_AREA_DIR_NAME);
            this.backupLocation = this.backupDirectory.getAbsolutePath();
            this.documentStore = documentStore;
            this.binaryStore = binaryStore;
            this.repositoryCache = repositoryCache;
            this.problems = new SimpleProblems();
        }

        /**
         * Execute the activity, using the repository state as it currently exists.
         * 
         * @return the problems describing any issues or exceptions that occur during the activity's execution; never null
         */
        public abstract Problems execute();

        protected final String repositoryName() {
            return repositoryCache.getName();
        }

        protected final String backupLocation() {
            return backupLocation;
        }
    }

    /**
     * The {@link Activity} subclass that performs content backup operations.
     */
    @NotThreadSafe
    public static class BackupActivity extends Activity {

        private final BackupObserver observer;
        protected final ExecutorService changedDocumentWorker;
        protected final BlockingQueue<NodeKey> changedDocumentQueue;
        private final long documentsPerFile;
        private final boolean compress;
        private BackupDocumentWriter contentWriter;
        private BackupDocumentWriter changesWriter;

        protected BackupActivity( File backupDirectory,
                                  org.modeshape.jcr.cache.document.LocalDocumentStore documentStore,
                                  BinaryStore binaryStore,
                                  RepositoryCache repositoryCache,
                                  long documentsPerFile,
                                  boolean compress ) {
            super(backupDirectory, documentStore, binaryStore, repositoryCache);
            CheckArg.isPositive(documentsPerFile, "documentsPerFile");
            this.documentsPerFile = documentsPerFile;
            this.compress = compress;
            this.changedDocumentQueue = new LinkedBlockingQueue<NodeKey>();
            ThreadFactory threadFactory = new NamedThreadFactory("modeshape-backup");
            this.changedDocumentWorker = Executors.newSingleThreadExecutor(threadFactory);
            this.observer = new BackupObserver(changedDocumentQueue);
        }

        /**
         * Initialize the backup area on disk, ensuring that the backup location does exist.
         * 
         * @return true if initialization was successful, or false if there was a problem
         */
        protected boolean initializeAreaOnDisk() {
            try {
                if (backupDirectory.exists()) {
                    if (!backupDirectory.isDirectory() || !backupDirectory.canWrite()) {
                        problems.addError(JcrI18n.existsAndMustBeWritableDirectory, backupLocation());
                        return false;
                    }
                    // Otherwise, the backup directory already exists and is writable ...
                } else {
                    // Create the backup directory ...
                    backupDirectory.mkdirs();
                }
                // Always make sure the changes and binary areas exist ...
                changeDirectory.mkdirs();
                binaryDirectory.mkdirs();
            } catch (RuntimeException e) {
                problems.addError(e, JcrI18n.problemInitializingBackupArea, backupLocation(), e.getMessage());
            }
            return true;
        }

        protected void writeToContentArea( SchematicEntry document ) {
            contentWriter.write(document.asDocument());
        }

        protected void writeToContentArea( BinaryKey key,
                                           InputStream binaryContent ) {
            String sha1 = key.toString();
            // Create directories for the first three segments from the binary key, where each segment is two characters ...
            File first = new File(binaryDirectory, sha1.substring(0, 2));
            File second = new File(first, sha1.substring(2, 4));
            File third = new File(second, sha1.substring(4, 6));
            third.mkdirs();
            File file = new File(third, sha1 + BINARY_EXTENSION);

            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                try {
                    IoUtil.write(binaryContent, outputStream);
                    outputStream.flush();
                } finally {
                    outputStream.close();
                }
            } catch (Throwable t) {
                problems.addError(JcrI18n.problemsWritingDocumentToBackup, file.getAbsolutePath(), t.getMessage());
            }
        }

        protected void writeToChangedArea( SchematicEntry document ) {
            LOGGER.debug("Writing document to change area of backup for {0} repository at {1}",
                         repositoryName(),
                         backupLocation());
            changesWriter.write(document.asDocument());
        }

        protected void writeToChangedArea( Iterable<BinaryKey> unusedBinaries ) {
            LOGGER.debug("Writing unused binaries to change area of backup for {0} repository at {1}",
                         repositoryName(),
                         backupLocation());
            File file = new File(changeDirectory, SUMMARY_FILE_NAME);
            try {
                EditableDocument doc = Schematic.newDocument();
                EditableArray keys = doc.setArray(FieldName.UNUSED_BINARY_KEYS);
                for (BinaryKey key : unusedBinaries) {
                    if (key != null) keys.add(key.toString());
                }
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    Json.write(doc, outputStream);
                    outputStream.flush();
                } finally {
                    outputStream.close();
                }
            } catch (Throwable t) {
                problems.addError(JcrI18n.problemsWritingDocumentToBackup, file.getAbsolutePath(), t.getMessage());
            }
        }

        @Override
        public Problems execute() {
            // initialize the area on disk where we'll be writing ...
            if (!initializeAreaOnDisk()) return problems;

            LOGGER.debug("Starting backup of '{0}' repository into {1}", repositoryName(), backupLocation());

            this.contentWriter = new BackupDocumentWriter(backupDirectory, DOCUMENTS_FILENAME_PREFIX, documentsPerFile, compress,
                                                          problems);
            this.changesWriter = new BackupDocumentWriter(changeDirectory, DOCUMENTS_FILENAME_PREFIX, documentsPerFile, compress,
                                                          problems);
            long numBinaryValues = 0L;

            try {
                final AtomicBoolean continueWritingChangedDocuments = new AtomicBoolean(true);

                // Create the runnable that watches the changedDocumentQueue (which can be populated by multiple threads)
                // and writes out the changed documents. Note that we only use a single thread to pull from the queue
                final CountDownLatch changesLatch = new CountDownLatch(1);
                this.changedDocumentWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (continueWritingChangedDocuments.get()) {
                                // Poll for a changed document, but wait at most 1 second ...
                                NodeKey key = changedDocumentQueue.poll(1L, TimeUnit.SECONDS);
                                if (key != null) {
                                    // Write out the document to the changed area ...
                                    SchematicEntry entry = documentStore.get(key.toString());
                                    writeToChangedArea(entry);
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }

                        // Continue to drain whatever is still in the queue, but never block ...
                        while (!changedDocumentQueue.isEmpty()) {
                            // Poll for a changed document, but at most only
                            NodeKey key = changedDocumentQueue.poll();
                            if (key != null) {
                                // Write out the document to the changed area ...
                                SchematicEntry entry = documentStore.get(key.toString());
                                writeToChangedArea(entry);
                            }
                        }
                        changesLatch.countDown();
                    }
                });
                // PHASE 0:
                // Register a listener with the repository to start start recording the documents as they exist when the
                // changes are made while this execution is proceeding. It's possible not all of these will be needed,
                // but by doing this we make sure that we include the latest changes in the backup (at least those
                // changes made before the observer is disconnected)...
                repositoryCache.register(observer);

                try {
                    // PHASE 1:
                    // Perform the backup of the repository cache content ...
                    int counter = 0;
                    Sequence<String> sequence = InfinispanUtil.getAllKeys(documentStore.localCache());
                    while (true) {
                        String key = sequence.next();
                        if (key == null) break;
                        SchematicEntry entry = documentStore.get(key);
                        if (entry != null) {
                            writeToContentArea(entry);
                            ++counter;
                        }
                    }
                    LOGGER.debug("Wrote {0} documents to {1}", counter, backupDirectory.getAbsolutePath());

                    // PHASE 2:
                    // Write out the repository metadata document (which may have not changed) ...
                    NodeKey metadataKey = repositoryCache.getRepositoryMetadataDocumentKey();
                    SchematicEntry entry = documentStore.get(metadataKey.toString());
                    writeToContentArea(entry);
                } catch (Exception e) {
                    I18n msg = JcrI18n.problemObtainingDocumentsToBackup;
                    this.problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
                } finally {
                    // Now that we're done with the backup, unregister the listener ...
                    try {
                        repositoryCache.unregister(observer);
                    } finally {
                        // Now stop the worker that is writing changed documents ...
                        continueWritingChangedDocuments.set(false);
                        // Shutdown the worker (gracefully, which will complete if 'continueWritingChangedDocuments' is false ...
                        changedDocumentWorker.shutdown();
                    }
                }

                // PHASE 3:
                // Perform the backup of the binary store ...
                try {
                    int counter = 0;
                    for (BinaryKey binaryKey : binaryStore.getAllBinaryKeys()) {
                        try {
                            writeToContentArea(binaryKey, binaryStore.getInputStream(binaryKey));
                            ++counter;
                        } catch (BinaryStoreException e) {
                            problems.addError(JcrI18n.problemsWritingBinaryToBackup, binaryKey, backupLocation(), e.getMessage());
                        }
                    }
                    LOGGER.debug("Wrote {0} binary values to {1}", counter, binaryDirectory.getAbsolutePath());
                } catch (BinaryStoreException e) {
                    I18n msg = JcrI18n.problemsGettingBinaryKeysFromBinaryStore;
                    problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
                }

                // PHASE 4:
                // Write all of the binary files that were added during the changes made while we worked ...
                int counter = 0;
                for (BinaryKey binaryKey : observer.getUsedBinaryKeys()) {
                    try {
                        writeToContentArea(binaryKey, binaryStore.getInputStream(binaryKey));
                        ++counter;
                    } catch (BinaryStoreException e) {
                        problems.addError(JcrI18n.problemsWritingBinaryToBackup, binaryKey, backupLocation(), e.getMessage());
                    }
                }
                LOGGER.debug("Wrote {0} recent binary values to {1}", counter, binaryDirectory.getAbsolutePath());

                // PHASE 5:
                // And now write all binary keys for the binaries that were recorded as unused by the observer ...
                writeToChangedArea(observer.getUnusedBinaryKeys());

                // Wait for the changes to be written
                changesLatch.await(30, TimeUnit.SECONDS);

                LOGGER.debug("Completed backup of '{0}' repository into {1} (contains {2} nodes and {3} binary values)",
                             repositoryName(),
                             backupLocation(),
                             contentWriter.getDocumentCount() + changesWriter.getDocumentCount(),
                             numBinaryValues);

            } catch (InterruptedException e) {
                Thread.interrupted();
                I18n msg = JcrI18n.interruptedWhilePerformingBackup;
                this.problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
            } catch (CancellationException e) {
                this.problems.addError(JcrI18n.backupOperationWasCancelled, repositoryName(), backupLocation(), e.getMessage());
            } finally {
                // PHASE 5:
                // Close all open writers ...
                try {
                    contentWriter.close();
                } finally {
                    contentWriter = null;
                    try {
                        changesWriter.close();
                    } finally {
                        changesWriter = null;
                    }
                }
            }

            return problems;
        }
    }

    /**
     * The {@link Activity} subclass that performs content restore operations.
     */
    @NotThreadSafe
    public static final class RestoreActivity extends Activity {

        protected RestoreActivity( File backupDirectory,
                                   org.modeshape.jcr.cache.document.LocalDocumentStore documentStore,
                                   BinaryStore binaryStore,
                                   RepositoryCache repositoryCache ) {
            super(backupDirectory, documentStore, binaryStore, repositoryCache);
        }

        @Override
        public Problems execute() {
            removeExistingBinaryFiles();
            restoreBinaryFiles();

            removeExistingDocuments();
            restoreDocuments(backupDirectory); // first pass of documents
            restoreDocuments(changeDirectory); // documents changed while backup was being made
            return problems;
        }

        public void removeExistingBinaryFiles() {
            // simply mark all of the existing binary values as unused; if an unused binary value is restored,
            // it will simply be kept without having store it ...
            try {
                Iterable<BinaryKey> keys = binaryStore.getAllBinaryKeys();
                binaryStore.markAsUnused(keys);
            } catch (BinaryStoreException e) {
                I18n msg = JcrI18n.problemsGettingBinaryKeysFromBinaryStore;
                problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
            }
        }

        public void removeExistingDocuments() {
            Cache<String, SchematicEntry> cache = documentStore.localCache();
            try {
                // Try a simple clear ...
                cache.clear();
            } catch (UnsupportedOperationException e) {
                // Otherwise, we have to do it by key ...
                try {
                    Sequence<String> keySequence = InfinispanUtil.getAllKeys(cache);
                    while (true) {
                        String key = keySequence.next();
                        if (key == null) break;
                        cache.remove(key);
                    }
                } catch (InterruptedException e2) {
                    Thread.interrupted();
                    I18n msg = JcrI18n.interruptedWhilePerformingBackup;
                    this.problems.addError(msg, repositoryName(), backupLocation(), e2.getMessage());
                } catch (CancellationException e2) {
                    this.problems.addError(JcrI18n.backupOperationWasCancelled,
                                           repositoryName(),
                                           backupLocation(),
                                           e2.getMessage());
                } catch (CacheLoaderException e2) {
                    I18n msg = JcrI18n.problemObtainingDocumentsToBackup;
                    this.problems.addError(msg, repositoryName(), backupLocation(), e2.getMessage());
                } catch (ExecutionException e2) {
                    I18n msg = JcrI18n.problemObtainingDocumentsToBackup;
                    this.problems.addError(msg, repositoryName(), backupLocation(), e2.getMessage());
                }
            }
        }

        public void restoreBinaryFiles() {
            for (File segment1Dir : binaryDirectory.listFiles()) {
                for (File segment2Dir : segment1Dir.listFiles()) {
                    for (File segment3Dir : segment2Dir.listFiles()) {
                        for (File binaryFile : segment3Dir.listFiles()) {
                            restoreBinaryFile(binaryFile);
                        }
                    }
                }
            }
        }

        public void restoreBinaryFile( File binaryFile ) {
            if (!binaryFile.exists()) return;
            if (!binaryFile.canRead()) {
                I18n msg = JcrI18n.problemsReadingBinaryFromBackup;
                BinaryKey key = binaryKeyFor(binaryFile);
                problems.addError(msg, key.toString(), repositoryName(), backupLocation());
            }
            try {
                InputStream stream = new FileInputStream(binaryFile);
                try {
                    BinaryValue stored = binaryStore.storeValue(stream);
                    assert stored.getKey().equals(binaryKeyFor(binaryFile));
                } finally {
                    stream.close();
                }
            } catch (FileNotFoundException e) {
                // We already checked that it exists and is readable, so this shouldn't happen. But ...
                I18n msg = JcrI18n.problemsReadingBinaryFromBackup;
                BinaryKey key = binaryKeyFor(binaryFile);
                problems.addError(e, msg, key.toString(), repositoryName(), backupLocation());
            } catch (BinaryStoreException e) {
                I18n msg = JcrI18n.problemsRestoringBinaryFromBackup;
                BinaryKey key = binaryKeyFor(binaryFile);
                problems.addError(e, msg, key.toString(), repositoryName(), backupLocation(), e.getMessage());
            } catch (IOException e) {
                I18n msg = JcrI18n.problemsRestoringBinaryFromBackup;
                BinaryKey key = binaryKeyFor(binaryFile);
                problems.addError(e, msg, key.toString(), repositoryName(), backupLocation(), e.getMessage());
            }
        }

        protected BinaryKey binaryKeyFor( File binaryFile ) {
            String filename = binaryFile.getName();
            String sha1 = filename.replace(BINARY_EXTENSION, "");
            return new BinaryKey(sha1);
        }

        protected void restoreDocuments( File directory ) {
            BackupDocumentReader reader = new BackupDocumentReader(directory, DOCUMENTS_FILENAME_PREFIX, problems);
            LOGGER.debug("Restoring documents from {0}", directory.getAbsolutePath());
            int count = 0;
            while (true) {
                Document doc = reader.read();
                if (doc == null) break;
                documentStore.put(doc);

                ++count;
                LOGGER.debug("restoring {0} doc {1}", (count + 1), doc);
            }
            LOGGER.debug("Restored {0} documents from {1}", count, directory.getAbsolutePath());
        }
    }
}
