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
            do {
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
                    Document doc = documents.nextDocument();
                    if (doc != null) return doc;
                } catch (IOException e) {
                    // We'll just continue ...
                }
                // Close the stream and try opening the next stream ...
                close(stream);
                stream = null;
            } while (true);
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
