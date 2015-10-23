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

import java.io.File;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link BinaryStore} implementation that does not persist the binary values beyond the lifetime of the virtual machine. This
 * implementation extends {@link FileSystemBinaryStore} and uses a temporary directory. Thus, the binary values are not stored
 * in-memory.
 */
@ThreadSafe
public final class TransientBinaryStore extends FileSystemBinaryStore {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String JBOSS_SERVER_TMPDIR = "jboss.server.temp.dir";
    private static final TransientBinaryStore INSTANCE = new TransientBinaryStore();

    protected static final File TRANSIENT_STORE_DIRECTORY = INSTANCE.getDirectory();

    /**
     * Obtain a shared {@link TransientBinaryStore} instance.
     * 
     * @return the instance; never null
     */
    public static TransientBinaryStore get() {
        return INSTANCE;
    }

    /**
     * Obtain a new temporary directory that can be used by a transient binary store. Note that none of the directories are
     * actually created at this time, but are instead created (if needed) during {@link #initializeStorage(File)}.
     * 
     * @return the new directory; never null
     */
    private static File newTempDirectory() {
        String tempDirName = System.getProperty(JBOSS_SERVER_TMPDIR);
        if (tempDirName == null) {
            tempDirName = System.getProperty(JAVA_IO_TMPDIR);
        }
        if (tempDirName == null) {
            throw new SystemFailureException(JcrI18n.tempDirectorySystemPropertyMustBeSet.text(JAVA_IO_TMPDIR));
        }
        File tempDir = new File(tempDirName);

        // Create a temporary directory in the "java.io.tmpdir" directory ...
        return new File(tempDir, "modeshape-binary-store");
    }

    /**
     * Create a new transient binary store.
     */
    private TransientBinaryStore() {
        super(newTempDirectory());
    }

    @Override
    public void start() {
        logger.debug("ModeShape repositories will use the following directory for transient storage of binary values unless repository configurations specify otherwise: {0}",
                     getDirectory().getAbsolutePath());
        // request the folder be deleted on VM exit; this may or may not work, which is why we also try to clear it on initialize
        getDirectory().deleteOnExit();
    }

    /**
     * Ensures that the directory used by this binary store exists and can be both read and written to.
     * 
     * @throws BinaryStoreException if the directory cannot be written to, read, or (if needed) created
     */
    @Override
    protected void initializeStorage( File directory ) throws BinaryStoreException {
        // make sure the directory doesn't exist
        FileUtil.delete(directory);
        if (!directory.exists()) {
            logger.debug("Creating temporary directory for transient binary store: {0}", directory.getAbsolutePath());
            directory.mkdirs();
        }
        if (!directory.canRead()) {
            throw new BinaryStoreException(JcrI18n.unableToReadTemporaryDirectory.text(directory.getAbsolutePath(),
                                                                                       JAVA_IO_TMPDIR));
        }
        if (!directory.canWrite()) {
            throw new BinaryStoreException(JcrI18n.unableToWriteTemporaryDirectory.text(directory.getAbsolutePath(),
                                                                                        JAVA_IO_TMPDIR));
        }
    }

    @Override
    protected void moveFileExclusively( File original, File destination, BinaryKey key ) throws BinaryStoreException {
        super.moveFileExclusively(original, destination, key);
        // on certain OSes there is no guarantee that the files from java.io.tmpdir are cleaned up, so we need to make sure of that here
        destination.deleteOnExit();
    }
}
