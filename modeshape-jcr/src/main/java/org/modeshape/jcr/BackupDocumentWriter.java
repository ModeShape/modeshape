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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

/**
 * A utility that writes {@link Document} instances to one or more sequential files in a backup directory.
 */
@NotThreadSafe
public final class BackupDocumentWriter {

    public static final String GZIP_EXTENSION = ".gz";
    public static final String DOCUMENTS_EXTENSION = ".bin";

    private final File parentDirectory;
    private final String filenamePrefix;
    private final boolean compress;
    protected final long maxDocumentsPerFile;
    protected OutputStream stream;
    protected long count = 0L;
    protected long totalCount = 0L;
    protected long fileCount = 0L;
    private final Problems problems;
    private File currentFile;

    public BackupDocumentWriter( File parentDirectory,
                                 String filenamePrefix,
                                 long documentsPerFile,
                                 boolean compress,
                                 Problems problems ) {
        CheckArg.isNotNull(parentDirectory, "parentDirectory");
        CheckArg.isNotEmpty(filenamePrefix, "filenamePrefix");
        CheckArg.isPositive(documentsPerFile, "documentsPerFile");
        this.parentDirectory = parentDirectory;
        this.filenamePrefix = filenamePrefix;
        this.maxDocumentsPerFile = documentsPerFile;
        this.problems = problems;
        this.compress = compress;
    }

    /**
     * Append the supplied document to the files.
     * 
     * @param document the document to be written; may not be null
     */
    public void write( Document document ) {
        assert document != null;
        ++count;
        ++totalCount;
        if (count > maxDocumentsPerFile) {
            // Close the stream (we'll open a new one later in the method) ...
            close();
            count = 1;
        }
        try {
            if (stream == null) {
                // Open the stream to the next file ...
                ++fileCount;
                String suffix = StringUtil.justifyRight(Long.toString(fileCount), BackupService.NUM_CHARS_IN_FILENAME_SUFFIX, '0');
                String filename = filenamePrefix + "_" + suffix + DOCUMENTS_EXTENSION;
                if (compress) filename = filename + GZIP_EXTENSION;
                currentFile = new File(parentDirectory, filename);
                OutputStream fileStream = new FileOutputStream(currentFile);
                if (compress) fileStream = new GZIPOutputStream(fileStream);
                stream = new BufferedOutputStream(fileStream);
            }
            Json.write(document, stream);
            // Need to append a non-consumable character so that we can read multiple JSON documents per file
            stream.write((byte)'\n');
        } catch (IOException e) {
            problems.addError(JcrI18n.problemsWritingDocumentToBackup, currentFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Close this writer, which flushes and closes any currently-open streams. Even after this is called, additional documents can
     * be written to additional files.
     */
    public void close() {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (IOException e) {
                problems.addError(JcrI18n.problemsClosingBackupFiles, parentDirectory.getAbsolutePath(), e.getMessage());
            } finally {
                stream = null;
            }
        }
    }

    /**
     * Return the number of documents that have been written so far.
     * 
     * @return the number of documents written; never negative
     */
    public long getDocumentCount() {
        return totalCount;
    }

    /**
     * Return the number of files that have been written so far.
     * 
     * @return the number of files; never negative
     */
    public long getFileCount() {
        return fileCount;
    }
}
