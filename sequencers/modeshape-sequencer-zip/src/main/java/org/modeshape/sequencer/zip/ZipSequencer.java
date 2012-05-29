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

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("zip.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(binaryValue.getStream());

            ZipEntry entry = zipInputStream.getNextEntry();

            outputNode = createTopLevelNode(outputNode);

            while (entry != null) {
                entry = sequenceZipEntry(outputNode, context, zipInputStream, entry);
            }
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (Exception e) {
                    logger.warn("Cannot close zip input stream", e);
                }
            }
        }
        return true;
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
        String mimeType = context.mimeTypeDetector().mimeTypeOf(entry.getName(), contentBinary.getStream());
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
