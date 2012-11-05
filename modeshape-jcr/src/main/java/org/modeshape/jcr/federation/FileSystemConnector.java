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
package org.modeshape.jcr.federation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.AbstractBinary;

/**
 * {@link Connector} implementation that exposes a single directory on the local file system.
 */
public class FileSystemConnector extends Connector {

    private static final String DELIMITER = "/";
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private static final String NT_FOLDER = "nt:folder";
    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_MIME_TYPE = "jcr:mimeType";
    private static final String JCR_ENCODING = "jcr:encoding";
    private static final String JCR_CREATED = "jcr:created";
    private static final String JCR_CREATED_BY = "jcr:createdBy";
    private static final String JCR_LAST_MODIFIED = "jcr:lastModified";
    private static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
    private static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX.length();

    /**
     * The string path for a {@link File} object that represents the top-level directory accessed by this connector. This is set
     * via reflection and is required for this connector.
     */
    private String directoryPath;
    private File directory;
    private String leadingPath;

    /**
     * The regular expression that, if matched by a file or folder, indicates that the file or folder should be included.
     */
    private String inclusionPattern;

    /**
     * The regular expression that, if matched by a file or folder, indicates that the file or folder should be ignored.
     */
    private String exclusionPattern;

    /**
     * The {@link FilenameFilter} implementation that is instantiated in the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
     */
    private InclusionExclusionFilenameFilter filenameFilter;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        // Validate the connector-specific fields ...
        checkFieldNotNull(directoryPath, "directoryPath");
        directory = new File(directoryPath);
        if (!directory.exists() || !directory.canRead() || !directory.isDirectory()) {
            throw new RepositoryException(
                                          "The file system connector expects a readable, existing directory for the 'directoryPath' property.");
        }
        leadingPath = directory.getAbsolutePath();

        // Initialize the filename filter ...
        filenameFilter = new InclusionExclusionFilenameFilter();
        if (exclusionPattern != null) filenameFilter.setExclusionPattern(exclusionPattern);
        if (inclusionPattern != null) filenameFilter.setInclusionPattern(exclusionPattern);
    }

    protected boolean isContentNode( String id ) {
        return id.endsWith(JCR_CONTENT_SUFFIX);
    }

    protected File fileFor( String id ) {
        if (isContentNode(id)) {
            id = id.substring(0, id.length() - JCR_CONTENT_SUFFIX_LENGTH);
        }
        return new File(leadingPath + "/" + id);
    }

    protected FileSystemBinaryValue binaryFor( File file ) {
        try {
            byte[] sha1 = SecureHash.getHash(Algorithm.SHA_1, file);
            BinaryKey key = new BinaryKey(sha1);
            FileSystemBinaryValue value = new FileSystemBinaryValue(key, file);
            String mimeType = getMimeTypeDetector().mimeTypeOf(file.getName(), value);
            value.setMimeType(mimeType);
            return value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    @Override
    public boolean hasDocument( String id ) {
        return fileFor(id).exists();
    }

    @Override
    public EditableDocument getDocumentById( String id ) {
        return getDocumentAtLocation(id);
    }

    @Override
    public EditableDocument getDocumentAtLocation( String location ) {
        String id = location; // this connector treats the ID as the path
        File file = fileFor(id);
        boolean isResource = isContentNode(id);
        DocumentBuilder builder = newDocument(id, file.getName());
        if (isResource) {
            FileSystemBinaryValue binaryValue = binaryFor(file);
            builder.addProperty(JCR_PRIMARY_TYPE, NT_RESOURCE);
            builder.addProperty(JCR_DATA, binaryValue);
            builder.addProperty(JCR_MIME_TYPE, binaryValue.getMimeType());
            builder.addProperty(JCR_ENCODING, null); // We don't really know this
            builder.addProperty(JCR_LAST_MODIFIED, factories().getDateFactory().create(file.lastModified()));
            builder.addProperty(JCR_LAST_MODIFIED_BY, null); // ignored
        } else if (file.isFile()) {
            builder.addProperty(JCR_PRIMARY_TYPE, NT_FILE);
            builder.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
            builder.addProperty(JCR_CREATED_BY, null); // ignored
            builder.addChild(id + JCR_CONTENT_SUFFIX, JCR_CONTENT);
        } else {
            builder.addProperty(JCR_PRIMARY_TYPE, NT_FOLDER);
            builder.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
            builder.addProperty(JCR_CREATED_BY, null); // ignored
            for (String childName : file.list(filenameFilter)) {
                builder.addChild(id + DELIMITER + childName, childName);
            }
        }
        return builder.build();
    }

    @Override
    public void removeDocument( String id ) {
        File file = fileFor(id);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void storeDocument( Document document ) {
    }

    @Override
    public void updateDocument( String id,
                                Document document ) {
    }

    @Override
    public void setParent( String federatedNodeId,
                           String documentId ) {
    }

    protected static class InclusionExclusionFilenameFilter implements java.io.FilenameFilter {
        private String inclusionPattern = null;
        private String exclusionPattern = null;
        private Pattern inclusion;
        private Pattern exclusion;

        public void setExclusionPattern( String exclusionPattern ) {
            this.exclusionPattern = exclusionPattern;
            if (exclusionPattern == null) {
                this.exclusion = null;
            } else {
                this.exclusion = Pattern.compile(exclusionPattern);
            }
        }

        public void setInclusionPattern( String inclusionPattern ) {
            this.inclusionPattern = inclusionPattern;
            if (inclusionPattern == null) {
                this.inclusion = null;
            } else {
                this.inclusion = Pattern.compile(inclusionPattern);
            }
        }

        public String getExclusionPattern() {
            return exclusionPattern;
        }

        public String getInclusionPattern() {
            return inclusionPattern;
        }

        @Override
        public boolean accept( File file,
                               String string ) {
            if (inclusionPattern == null && exclusionPattern == null) {
                return true;
            } else if (inclusionPattern == null && exclusionPattern != null) {
                return !exclusion.matcher(string).matches();
                // return !string.matches(exclusionPattern);
            } else {
                if (exclusionPattern == null) {
                    return inclusion.matcher(string).matches();
                    // return string.matches(inclusionPattern);
                }
                return inclusion.matcher(string).matches() && !exclusion.matcher(string).matches();
                // return string.matches(inclusionPattern) && !string.matches(exclusionPattern);
            }
        }
    }

    protected static class FileSystemBinaryValue extends AbstractBinary {
        private static final long serialVersionUID = 1L;

        private transient File file;
        private final String path;
        private String mimeType;

        protected FileSystemBinaryValue( BinaryKey binaryKey,
                                         File file ) {
            super(binaryKey);
            this.file = file;
            this.path = file.getAbsolutePath();
        }

        protected void setMimeType( String mimeType ) {
            this.mimeType = mimeType;
        }

        protected File file() {
            if (this.file == null) {
                this.file = new File(this.path);
            }
            return this.file;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getMimeType( String name ) {
            return getMimeType();
        }

        @Override
        public long getSize() {
            return file.getTotalSpace();
        }

        @Override
        public InputStream getStream() throws RepositoryException {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new RepositoryException(e);
            }
        }
    }

}
