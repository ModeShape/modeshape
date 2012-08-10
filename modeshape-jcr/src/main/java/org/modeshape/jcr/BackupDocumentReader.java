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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.DocumentSequence;
import org.infinispan.schematic.document.Json;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

/**
 * A utility that writes {@link Document} instances to one or more sequential files in a backup directory.
 */
@NotThreadSafe
public final class BackupDocumentReader {

    public static final String GZIP_EXTENSION = BackupDocumentWriter.GZIP_EXTENSION;
    public static final String DOCUMENTS_EXTENSION = BackupDocumentWriter.DOCUMENTS_EXTENSION;

    private final File parentDirectory;
    private final String filenamePrefix;
    private final Problems problems;
    protected InputStream stream;
    protected DocumentSequence documents;
    protected long fileCount = 0L;
    private File currentFile;

    public BackupDocumentReader( File parentDirectory,
                                 String filenamePrefix,
                                 Problems problems ) {
        CheckArg.isNotNull(parentDirectory, "parentDirectory");
        CheckArg.isNotEmpty(filenamePrefix, "filenamePrefix");
        this.parentDirectory = parentDirectory;
        this.filenamePrefix = filenamePrefix;
        this.problems = problems;
    }

    /**
     * Read the next document from the files.
     * 
     * @return the document, or null if there are no more documents
     */
    public Document read() {
        try {
            if (stream == null) {
                // Open the stream to the next file ...
                stream = openNextFile();
                if (stream == null) {
                    // No more files to read ...
                    return null;
                }
                documents = Json.readMultiple(stream);
            }
            try {
                return documents.nextDocument();
            } catch (IOException e) {
                // Close the stream and try opening the next stream ...
                close(stream);
                stream = openNextFile();
                if (stream == null) {
                    // No more files to read ...
                    return null;
                }
                // And try reading this new stream ...
                documents = Json.readMultiple(stream);
                return documents.nextDocument();
            }
        } catch (IOException e) {
            problems.addError(JcrI18n.problemsWritingDocumentToBackup, currentFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    protected InputStream openNextFile() throws IOException {
        // Open the stream to the next file ...
        ++fileCount;
        String suffix = StringUtil.justifyRight(Long.toString(fileCount), BackupService.NUM_CHARS_IN_FILENAME_SUFFIX, '0');
        String filename = filenamePrefix + "_" + suffix + DOCUMENTS_EXTENSION + GZIP_EXTENSION;
        currentFile = new File(parentDirectory, filename);
        boolean compressed = true;
        if (!currentFile.exists()) {
            // Try the uncompressed form ...
            filename = filenamePrefix + "_" + suffix + DOCUMENTS_EXTENSION;
            currentFile = new File(parentDirectory, filename);
            if (!currentFile.exists()) return null;
            compressed = false;
        }
        if (!currentFile.canRead() || !currentFile.isFile()) return null;
        InputStream fileStream = new FileInputStream(currentFile);
        if (compressed) fileStream = new GZIPInputStream(fileStream);
        return new BufferedInputStream(fileStream);
    }

    protected void close( InputStream stream ) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                problems.addError(JcrI18n.problemsClosingBackupFiles, parentDirectory.getAbsolutePath(), e.getMessage());
            } finally {
                stream = null;
            }
        }
    }

    /**
     * Close this writer, which flushes and closes any currently-open streams. Even after this is called, additional documents can
     * be written to additional files.
     */
    public void close() {
        close(stream);
    }
}
