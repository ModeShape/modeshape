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

import java.io.File;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.JcrI18n;

/**
 * A {@link BinaryStore} implementation that does not persist the binary values beyond the lifetime of the virtual machine. This
 * implementation extends {@link FileSystemBinaryStore} and uses a temporary directory. Thus, the binary values are not stored
 * in-memory.
 */
@ThreadSafe
public final class TransientBinaryStore extends FileSystemBinaryStore {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    private static final String JBOSS_SERVER_DATA_DIR = "jboss.server.data.dir";

    private static final TransientBinaryStore INSTANCE = new TransientBinaryStore();

    protected static final File TRANSIENT_STORE_DIRECTORY = INSTANCE.getDirectory();

    private static boolean printedLocation = false;

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
        String tempDirName = System.getProperty(JAVA_IO_TMPDIR);
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
        if (!printedLocation && System.getProperty(JBOSS_SERVER_DATA_DIR) == null) {
            // We're not running in JBoss AS (where we always specify the directory where the binaries are stored),
            // so log where the temporary directory is ...
            Logger logger = Logger.getLogger(getClass());
            logger.debug("ModeShape repositories will use the following directory for transient storage of binary values unless repository configurations specify otherwise: {0}",
                         getDirectory().getAbsolutePath());
            printedLocation = true;
        }
        super.start();
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
            Logger.getLogger(getClass()).debug("Creating temporary directory for transient binary store: {0}",
                                               directory.getAbsolutePath());
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

}
