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

package org.modeshape.sequencer.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A sequencer that processes and extract the files and folders from ZIP archive files.
 * 
 * @author Horia Chiorean
 */
public class ZipSequencer extends Sequencer {

    public static final class MimeTypeConstants {
        public static final String JAR = "application/java-archive";
        public static final String ZIP = "application/zip";
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("zip.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.JAR, MimeTypeConstants.ZIP);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        try (ZipInputStream zipInputStream = new ZipInputStream(binaryValue.getStream())){
            ZipEntry entry = zipInputStream.getNextEntry();
            outputNode = createTopLevelNode(outputNode);
            while (entry != null) {
                entry = sequenceZipEntry(outputNode, context, zipInputStream, entry);
            }
            return true;
        }
    }

    private Node createTopLevelNode( Node outputNode ) throws RepositoryException {
        // Create top-level node
        if (!outputNode.isNew()) {
            outputNode = outputNode.addNode(ZipLexicon.CONTENT);
        }
        outputNode.setPrimaryType(ZipLexicon.FILE);
        return outputNode;
    }

    private ZipEntry sequenceZipEntry( Node outputNode,
                                       Context context,
                                       ZipInputStream zipInputStream,
                                       ZipEntry entry ) throws RepositoryException, IOException {
        Node zipEntryNode = createZipEntryPath(outputNode, entry);

        if (!entry.isDirectory()) {
            addFileContent(zipInputStream, entry, context, zipEntryNode);
        }
        return zipInputStream.getNextEntry();
    }

    private void addFileContent( ZipInputStream zipInputStream,
                                 ZipEntry entry,
                                 Context context,
                                 Node zipFileNode ) throws RepositoryException, IOException {
        Node contentNode = zipFileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        // on session pre-save the appropriate properties should be set automatically
        contentNode.addMixin(JcrConstants.MIX_LAST_MODIFIED);

        // set the content bytes
        byte[] contentBytes = readContent(zipInputStream);
        org.modeshape.jcr.api.Binary contentBinary = context.valueFactory().createBinary(contentBytes);
        contentNode.setProperty(JcrConstants.JCR_DATA, contentBinary);

        // Figure out the mime type ...
        String mimeType = contentBinary.getMimeType(entry.getName());
        if (mimeType != null) {
            contentNode.setProperty(JcrConstants.JCR_MIME_TYPE, mimeType);
        }
    }

    /**
     * Reads the content from the {@link ZipInputStream}, making sure it doesn't close the stream.
     * 
     * @param zipInputStream the input stream
     * @return the content
     * @throws IOException if there is a problem reading the stream
     */
    private byte[] readContent( ZipInputStream zipInputStream ) throws IOException {
        int bufferLength = 1024;
        byte[] buffer = new byte[bufferLength];
        int n;
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        while ((n = zipInputStream.read(buffer, 0, bufferLength)) > -1) {
            baout.write(buffer, 0, n);
        }
        return baout.toByteArray();
    }

    /**
     * Creates (if necessary) the path from the {@link Node parentNode} to the {@link ZipEntry zip entry}, based on the name of
     * the zip entry, which should contain its absolute path.
     * 
     * @param parentNode the parent node under which the node for the ZIP entry should be created
     * @param entry the ZIP file entry
     * @return the newly created node
     * @throws RepositoryException if there is a problem writing the content to the repository session
     */
    private Node createZipEntryPath( Node parentNode,
                                     ZipEntry entry ) throws RepositoryException {
        Node zipEntryNode = parentNode;
        String entryName = entry.getName();
        String[] segments = entryName.split("/");
        for (int i = 0; i < segments.length; i++) {
            String segmentName = segments[i];
            try {
                zipEntryNode = zipEntryNode.getNode(segmentName);
            } catch (PathNotFoundException e) {
                // the path does not exist yet - create it
                boolean isLastSegment = (i == segments.length - 1);
                String segmentPrimaryType = isLastSegment ? (entry.isDirectory() ? JcrConstants.NT_FOLDER : JcrConstants.NT_FILE) : JcrConstants.NT_FOLDER;
                zipEntryNode = zipEntryNode.addNode(segmentName, segmentPrimaryType);
            }
        }
        return zipEntryNode;
    }
}
