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
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * A sequencer that processes and extract the files and folders from ZIP archive files.
 */
public class ZipSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
        DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        NameFactory nameFactory = context.getValueFactories().getNameFactory();

        // Figure out the name of the archive file ...
        Path pathToArchiveFile = context.getInputPath();
        Name zipFileName = null;
        if (pathToArchiveFile != null && !pathToArchiveFile.isRoot()) {
            // Remove the 'jcr:content' node (of type 'nt:resource'), if it is there ...
            if (pathToArchiveFile.getLastSegment().getName().equals(JcrLexicon.CONTENT)) pathToArchiveFile = pathToArchiveFile.getParent();
            if (!pathToArchiveFile.isRoot()) zipFileName = pathToArchiveFile.getLastSegment().getName();
        }
        if (zipFileName == null) {
            zipFileName = ZipLexicon.CONTENT;
        }

        DateTime now = dateFactory.create();
        String username = context.getSecurityContext().getUserName();
        MimeTypeDetector mimeDetector = context.getMimeTypeDetector();

        ZipInputStream in = null;
        try {
            in = new ZipInputStream(stream);
            ZipEntry entry = in.getNextEntry();
            byte[] buf = new byte[1024];

            // Create top-level node
            Path zipPath = pathFactory.createRelativePath(zipFileName);
            output.setProperty(zipPath, JcrLexicon.PRIMARY_TYPE, ZipLexicon.FILE);
            while (entry != null) {
                Path entryPath = zipPath;
                String entryName = entry.getName();
                for (String segment : entryName.split(File.separator)) {
                    entryPath = pathFactory.create(entryPath, nameFactory.create(segment));
                }

                // Set the timestamp and username ....
                long time = entry.getTime();
                DateTime dateTime = time != -1 ? dateFactory.create(time) : now;
                output.setProperty(entryPath, JcrLexicon.CREATED, dateTime);
                if (username != null) output.setProperty(entryPath, JcrLexicon.CREATED_BY, username);

                if (entry.isDirectory()) { // If entry is directory, create nt:folder node
                    output.setProperty(entryPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER);
                } else { // If entry is File, create nt:file
                    output.setProperty(entryPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE);

                    Path contentPath = pathFactory.create(entryPath, JcrLexicon.CONTENT);
                    output.setProperty(contentPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE);
                    int n;
                    ByteArrayOutputStream baout = new ByteArrayOutputStream();
                    while ((n = in.read(buf, 0, 1024)) > -1) {
                        baout.write(buf, 0, n);
                    }
                    byte[] bytes = baout.toByteArray();
                    output.setProperty(contentPath, JcrLexicon.DATA, binaryFactory.create(bytes));
                    // all other nt:file properties should be generated by other sequencers (mimetype, encoding,...) but we'll
                    // default them here
                    // output.setProperty(contentPath, JcrLexicon.ENCODING, "binary");
                    output.setProperty(contentPath, JcrLexicon.LAST_MODIFIED, dateTime);
                    if (username != null) output.setProperty(contentPath, JcrLexicon.LAST_MODIFIED_BY, username);

                    // Figure out the mime type ...
                    String mimeType = mimeDetector.mimeTypeOf(entryName, null);
                    if (mimeType != null) output.setProperty(contentPath, JcrLexicon.MIMETYPE, mimeType);

                }
                in.closeEntry();
                entry = in.getNextEntry();
            }
        } catch (Exception e) {
            String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
            context.getProblems().addError(e, ZipI18n.errorReadingZipFile, location, e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
                    context.getProblems().addError(e, ZipI18n.errorClosingZipFile, location, e.getMessage());
                }
            }
        }
    }
}
