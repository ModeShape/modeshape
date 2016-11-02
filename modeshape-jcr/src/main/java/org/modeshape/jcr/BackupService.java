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
package org.modeshape.jcr;

import static org.modeshape.jcr.BackupDocumentWriter.GZIP_EXTENSION;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.jcr.RepositoryException;
import javax.transaction.SystemException;
import org.infinispan.Cache;
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
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.InfinispanUtil.Sequence;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.api.BackupOptions;
import org.modeshape.jcr.api.RestoreOptions;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

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

    protected static class FieldName {
        public static final String UNUSED_BINARY_KEYS = "unusedBinaryKeys";
    }

    private final RunningState runningState;
    private final LocalDocumentStore documentStore;
    private final BinaryStore binaryStore;
    private final RepositoryCache repositoryCache;

    protected BackupService( RunningState runningState ) {
        this.runningState = runningState;
        // backup restore should not care about federation, hence only the local store should be used
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
     * Start backing up the repository.
     * 
     * @param backupDirectory the directory on the file system into which the backup should be placed; this directory should
     *        typically not exist
     * @param options a {@link org.modeshape.jcr.api.BackupOptions} instance controlling the behavior of the backup; may not be
     * {@code null}
     * @return the problems that occurred during the backup process
     * @throws RepositoryException if the backup operation cannot be run
     */
    public org.modeshape.jcr.api.Problems backupRepository( File backupDirectory, BackupOptions options ) throws RepositoryException {
        // Create the activity ...
        final BackupActivity backupActivity = createBackupActivity(backupDirectory, options);

        //suspend any existing transactions
        try {
            if (runningState.suspendExistingUserTransaction()) {
                LOGGER.debug("Suspended existing active user transaction before the backup operation starts");
            }
            try {
                // Run the backup and return the problems ...
                return new JcrProblems(backupActivity.execute());
            } finally {
                runningState.resumeExistingUserTransaction();
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start asynchronously backing up the repository.
     * 
     * @param repository the JCR repository to be backed up; may not be null
     * @param backupDirectory the directory on the file system that contains the backup; this directory obviously must exist
     * @param options a {@link org.modeshape.jcr.api.RestoreOptions} instance which controls the restore behavior; may not be null
     * @return the problems that occurred during the restore process
     * @throws RepositoryException if the restoration operation cannot be run
     */
    public org.modeshape.jcr.api.Problems restoreRepository( final JcrRepository repository,
                                                             final File backupDirectory,
                                                             final RestoreOptions options) throws RepositoryException {
        final String backupLocString = backupDirectory.getAbsolutePath();
        LOGGER.debug("Beginning restore of '{0}' repository from {1}", repository.getName(), backupLocString);
        // Put the repository into the 'restoring' state ...
        repository.prepareToRestore();

        // Create the activity ...
        final RestoreActivity restoreActivity = createRestoreActivity(backupDirectory, options);

        org.modeshape.jcr.api.Problems problems = null;
        try {
            if (runningState.suspendExistingUserTransaction()) {
                LOGGER.debug("Suspended existing active user transaction before the restore operation starts");
            }

            problems = new JcrProblems(restoreActivity.execute());
            if (!problems.hasProblems()) {
                // restart the repository ...
                try {
                    repository.completeRestore(options);
                } catch (Throwable t) {
                    restoreActivity.problems.addError(t, JcrI18n.repositoryCannotBeRestartedAfterRestore, repository.getName(),
                                                      t.getMessage());
                } finally {
                    runningState.resumeExistingUserTransaction();
                }
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
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
     * @param options the {@link org.modeshape.jcr.api.BackupOptions} which customize the behavior of the backup.
     * @return the backup activity; never null
     */
    public BackupActivity createBackupActivity( File backupDirectory,
                                                BackupOptions options ) {
        return new BackupActivity(backupDirectory, documentStore, binaryStore, repositoryCache, options);
    }

    /**
     * Create a new {@link RestoreActivity activity} instance that can restore the content of the repository to the state as it
     * exists in the specified backup directory.
     * 
     * @param options a {@link org.modeshape.jcr.api.RestoreOptions} instance; may no be null.
     * @param backupDirectory the directory on the file system that contains the backup; this directory obviously must exist
     * @return the restore activity; never null
     */
    public RestoreActivity createRestoreActivity( File backupDirectory, RestoreOptions options ) {
        return new RestoreActivity(backupDirectory, documentStore, binaryStore, repositoryCache, options);
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
        protected final BackupOptions options;

        protected BackupActivity( File backupDirectory,
                                  org.modeshape.jcr.cache.document.LocalDocumentStore documentStore,
                                  BinaryStore binaryStore,
                                  RepositoryCache repositoryCache,
                                  BackupOptions options) {
            super(backupDirectory, documentStore, binaryStore, repositoryCache);
            CheckArg.isNotNull(options, "options");
            CheckArg.isPositive(options.documentsPerFile(), "documentsPerFile");
            this.options = options;
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
                if (options.includeBinaries()) {
                    binaryDirectory.mkdirs();
                }
            } catch (RuntimeException e) {
                problems.addError(e, JcrI18n.problemInitializingBackupArea, backupLocation(), e.getMessage());
            }
            return true;
        }

        protected void writeToContentArea( SchematicEntry document, BackupDocumentWriter contentWriter ) {
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

            String filename = sha1 + BINARY_EXTENSION;
            if (options.compress()) {
                filename = filename + GZIP_EXTENSION;
            }
            File file = new File(third, filename);

            try {
                OutputStream outputStream = new FileOutputStream(file);
                if (options.compress()) {
                    outputStream = new GZIPOutputStream(outputStream);
                }
                outputStream = new BufferedOutputStream(outputStream);
                IoUtil.write(binaryContent, outputStream);
            } catch (Throwable t) {
                problems.addError(JcrI18n.problemsWritingDocumentToBackup, file.getAbsolutePath(), t.getMessage());
            }
        }

        protected void writeToChangedArea( SchematicEntry document, BackupDocumentWriter changesWriter ) {
            LOGGER.debug("Writing document to change area of backup for {0} repository at {1}", repositoryName(),
                         backupLocation());
            changesWriter.write(document.asDocument());
        }

        protected void writeToChangedArea( Iterable<BinaryKey> unusedBinaries ) {
            LOGGER.debug("Writing unused binaries to change area of backup for {0} repository at {1}", repositoryName(),
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

            final BackupDocumentWriter contentWriter = new BackupDocumentWriter(backupDirectory, DOCUMENTS_FILENAME_PREFIX, 
                                                                                options.documentsPerFile(), 
                                                          options.compress(),
                                                          problems);
            final BackupDocumentWriter changesWriter = new BackupDocumentWriter(changeDirectory, DOCUMENTS_FILENAME_PREFIX, 
                                                                                options.documentsPerFile(), 
                                                          options.compress(),
                                                          problems);
            long numBinaryValues = 0L;
            final NodeKey metadataKey = repositoryCache.getRepositoryMetadataDocumentKey();

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
                                if (key != null && !key.equals(metadataKey)) {
                                    // Write out the document to the changed area ...
                                    SchematicEntry entry = documentStore.get(key.toString());
                                    writeToChangedArea(entry, changesWriter);
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }

                        // Continue to drain whatever is still in the queue, but never block ...
                        while (!changedDocumentQueue.isEmpty()) {
                            // Poll for a changed document, but at most only
                            NodeKey key = changedDocumentQueue.poll();
                            if (key != null && !key.equals(metadataKey)) {
                                // Write out the document to the changed area ...
                                SchematicEntry entry = documentStore.get(key.toString());
                                writeToChangedArea(entry, changesWriter);
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
                repositoryCache.changeBus().register(observer);

                try {
                    // PHASE 1:
                    // Perform the backup of the repository cache content ...
                    int counter = 0;
                    // remove the metadata key since we want that to always export that last
                    Sequence<String> sequence = InfinispanUtil.getAllKeys(documentStore.localCache());
                    while (true) {
                        String key = sequence.next();
                        if (key == null) break;

                        if (!key.equals(metadataKey.toString())) {
                            SchematicEntry entry = documentStore.get(key);
                            if (entry != null) {
                                writeToContentArea(entry, contentWriter);
                                ++counter;
                            }
                        }
                    }
                    LOGGER.debug("Wrote {0} documents to {1}", counter, backupDirectory.getAbsolutePath());

                    // PHASE 2:
                    // Write out the repository metadata document (which may have not changed) ...
                    SchematicEntry entry = documentStore.get(metadataKey.toString());
                    writeToContentArea(entry, contentWriter);
                } catch (Exception e) {
                    I18n msg = JcrI18n.problemObtainingDocumentsToBackup;
                    this.problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
                } finally {
                    // Now that we're done with the backup, unregister the listener ...
                    try {
                        repositoryCache.changeBus().unregister(observer);
                    } finally {
                        // Now stop the worker that is writing changed documents ...
                        continueWritingChangedDocuments.set(false);
                        // Shutdown the worker (gracefully, which will complete if 'continueWritingChangedDocuments' is false ...
                        changedDocumentWorker.shutdown();
                    }
                }

                if (options.includeBinaries()) {
                    // PHASE 3:
                    // Perform the backup of the binary store ...
                    try {
                        int counter = 0;
                        for (BinaryKey binaryKey : binaryStore.getAllBinaryKeys()) {
                            try {
                                writeToContentArea(binaryKey, binaryStore.getInputStream(binaryKey));
                                ++counter;
                            } catch (BinaryStoreException e) {
                                problems.addError(JcrI18n.problemsWritingBinaryToBackup, binaryKey, backupLocation(),
                                                  e.getMessage());
                            }
                        }
                        LOGGER.debug("Wrote {0} binary values to {1}", counter, binaryDirectory.getAbsolutePath());
                        numBinaryValues += counter;
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
                            problems.addError(JcrI18n.problemsWritingBinaryToBackup, binaryKey, backupLocation(),
                                              e.getMessage());
                        }
                    }
                    LOGGER.debug("Wrote {0} recent binary values to {1}", counter, binaryDirectory.getAbsolutePath());
                    numBinaryValues += counter;

                    // PHASE 5:
                    // And now write all binary keys for the binaries that were recorded as unused by the observer ...
                    writeToChangedArea(observer.getUnusedBinaryKeys());
                }
                // Wait for the changes to be written
                changesLatch.await(30, TimeUnit.SECONDS);

                LOGGER.debug("Completed backup of '{0}' repository into {1} (contains {2} nodes and {3} binary values)",
                             repositoryName(), backupLocation(),
                             contentWriter.getDocumentCount() + changesWriter.getDocumentCount(), numBinaryValues);

            } catch (InterruptedException e) {
                Thread.interrupted();
                I18n msg = JcrI18n.interruptedWhilePerformingBackup;
                this.problems.addError(msg, repositoryName(), backupLocation(), e.getMessage());
            } catch (CancellationException e) {
                this.problems.addError(JcrI18n.backupOperationWasCancelled, repositoryName(), backupLocation(), e.getMessage());
            } finally {
                // PHASE 5:
                // Close all open writers ...
                contentWriter.close();
                changesWriter.close();
            }

            return problems;
        }
    }

    /**
     * The {@link Activity} subclass that performs content restore operations.
     */
    @NotThreadSafe
    public static final class RestoreActivity extends Activity {
        private final RestoreOptions options;

        protected RestoreActivity( File backupDirectory,
                                   org.modeshape.jcr.cache.document.LocalDocumentStore documentStore,
                                   BinaryStore binaryStore,
                                   RepositoryCache repositoryCache,
                                   RestoreOptions options) {
            super(backupDirectory, documentStore, binaryStore, repositoryCache);
            CheckArg.isNotNull(options, "restoreOptions");
            this.options = options;
        }

        @Override
        public Problems execute() {
            // run the restore as a transactional unit so that if anything fails the entire changes are rolled back...            
            repositoryCache.runInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    boolean includeBinaries = binaryDirectory.exists() && binaryDirectory.canRead() && options.includeBinaries();
                    if (includeBinaries) {
                        removeExistingBinaryFiles();
                        restoreBinaryFiles();
                    }

                    removeExistingDocuments();
                    restoreDocuments(backupDirectory); // first pass of documents
                    restoreDocuments(changeDirectory); // documents changed while backup was being made
                    return null;
                }
            }, 0);
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
                    this.problems.addError(JcrI18n.backupOperationWasCancelled, repositoryName(), backupLocation(),
                                           e2.getMessage());
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
                BinaryKey key = binaryKeyFor(binaryFile, false);
                problems.addError(msg, key.toString(), repositoryName(), backupLocation());
            }
            boolean isCompressed = FileUtil.getExtension(binaryFile.getAbsolutePath()).equals(GZIP_EXTENSION);
            try {
                InputStream stream = new FileInputStream(binaryFile);
                if(isCompressed){
                    stream = new GZIPInputStream(stream);
                }
                stream = new BufferedInputStream(stream);
                try {
                    BinaryValue stored = binaryStore.storeValue(stream, isCompressed);
                    assert stored.getKey().equals(binaryKeyFor(binaryFile, isCompressed));
                } finally {
                    stream.close();
                }
            } catch (Exception e) {
                // We already checked that it exists and is readable, so this shouldn't happen. But ...
                I18n msg = JcrI18n.problemsReadingBinaryFromBackup;
                BinaryKey key = binaryKeyFor(binaryFile, isCompressed);
                problems.addError(e, msg, key.toString(), repositoryName(), backupLocation());
            } 
        }

        protected BinaryKey binaryKeyFor(File binaryFile, boolean isCompressed) {
            String filename = binaryFile.getName();
            String sha1 = filename.replace(BINARY_EXTENSION, "");
            if (isCompressed) {
                sha1 = sha1.replace(GZIP_EXTENSION, "");
            }
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
