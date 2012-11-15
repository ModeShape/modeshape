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
package org.modeshape.connector.filesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.Connector;
import org.modeshape.jcr.federation.NoExtraPropertiesStorage;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.binary.AbstractBinary;

/**
 * {@link Connector} implementation that exposes a single directory on the local file system. This connector has several
 * properties that must be configured via the {@link RepositoryConfiguration}:
 * <ul>
 * <li><strong><code>directoryPath</code></strong> - The path to the file or folder that is to be accessed by this connector.</li>
 * <li><strong><code>readOnly</code></strong> - A boolean flag that specifies whether this source can create/modify/remove files
 * and directories on the file system to reflect changes in the JCR content. By default, sources are not read-only.</li>
 * <li><strong><code>addMimeTypeMixin</code></strong> - A boolean flag that specifies whether this connector should add the
 * 'mix:mimeType' mixin to the 'nt:resource' nodes to include the 'jcr:mimeType' property. If set to <code>true</code>, the MIME
 * type is computed immediately when the 'nt:resource' node is accessed, which might be expensive for larger files. This is
 * <code>false</code> by default.</li>
 * <li><strong><code>extraPropertyStorage</code></strong> - An optional string flag that specifies how this source handles "extra"
 * properties that are not stored via file system attributes. See {@link #extraPropertiesStorage} for details. By default, extra
 * properties are stored in the same Infinispan cache that the repository uses.</li>
 * <li><strong><code>exclusionPattern</code></strong> - Optional property that specifies a regular expression that is used to
 * determine which files and folders in the underlying file system are not exposed through this connector. Files and folders with
 * a name that matches the provided regular expression will <i>not</i> be exposed by this source.</li>
 * <li><strong><code>inclusionPattern</code></strong> - Optional property that specifies a regular expression that is used to
 * determine which files and folders in the underlying file system are exposed through this connector. Files and folders with a
 * name that matches the provided regular expression will be exposed by this source.</li>
 * </ul>
 * Inclusion and exclusion patterns can be used separately or in combination. For example, consider these cases:
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Inclusion Pattern</th>
 * <th>Exclusion Pattern</th>
 * <th>Examples</th>
 * </tr>
 * <tr>
 * <td>(.+)\\.txt$</td>
 * <td></td>
 * <td>Includes only files and directories whose names end in "<code>.txt</code>" (e.g., "<code>something.txt</code>" ), but does
 * not include files and other folders such as "<code>something.jar</code>" or "<code>something.txt.zip</code>".</td>
 * </tr>
 * <tr>
 * <td>(.+)\\.txt$</td>
 * <td>my.txt</td>
 * <td>Includes only files and directories whose names end in "<code>.txt</code>" (e.g., "<code>something.txt</code>" ) with the
 * exception of "<code>my.txt</code>", and does not include files and other folders such as "<code>something.jar</code>" or "
 * <code>something.txt.zip</code>".</td>
 * </tr>
 * <tr>
 * <td>my.txt</td>
 * <td>.+</td>
 * <td>Excludes all files and directories except any named "<code>my.txt</code>".</td>
 * </tr>
 * </table>
 */
public class FileSystemConnector extends Connector {

    private static final String DELIMITER = "/";
    private static final String NT_FOLDER = "nt:folder";
    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String MIX_MIME_TYPE = "mix:mimeType";
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
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

    private static final String EXTRA_PROPERTIES_JSON = "json";
    private static final String EXTRA_PROPERTIES_LEGACY = "legacy";
    private static final String EXTRA_PROPERTIES_NONE = "none";

    /**
     * The string path for a {@link File} object that represents the top-level directory accessed by this connector. This is set
     * via reflection and is required for this connector.
     */
    private String directoryPath;
    private File directory;

    /**
     * A string that is created in the {@link #initialize(NamespaceRegistry, NodeTypeManager)} method that represents the absolute
     * path to the {@link #directory}. This path is removed from an absolute path of a file to obtain the ID of the node.
     */
    private String directoryAbsolutePath;
    private int directoryAbsolutePathLength;

    /**
     * A boolean flag that specifies whether this connector should only read existing files and directories on the file system and
     * should never write, update, or create files and directories on the file systems. This is set via reflection and defaults to
     * "<code>false</code>".
     */
    private boolean readonly = false;

    /**
     * A boolean flag that specifies whether this connector should add the 'mix:mimeType' mixin to the 'nt:resource' nodes to
     * include the 'jcr:mimeType' property. If set to <code>true</code>, the MIME type is computed immediately when the
     * 'nt:resource' node is accessed, which might be expensive for larger files. This is <code>false</code> by default.
     */
    private boolean addMimeTypeMixin = false;

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

    /**
     * A string that specifies how the "extra" properties are to be stored, where an "extra" property is any JCR property that
     * cannot be stored natively on the file system as file attributes. This field is set via reflection, and the value is
     * expected to be one of these valid values:
     * <ul>
     * <li>"<code>store</code>" - Any extra properties are stored in the same Infinispan cache where the content is stored. This
     * is the default and is used if the actual value doesn't match any of the other accepted values.</li>
     * <li>"<code>json</code>" - Any extra properties are stored in a JSON file next to the file or directory.</li>
     * <li>"<code>legacy</code>" - Any extra properties are stored in a ModeShape 2.x-compatible file next to the file or
     * directory. This is generally discouraged unless you were using ModeShape 2.x and have a directory structure that already
     * contains these files.</li>
     * <li>"<code>none</code>" - An extra properties that prevents the storage of extra properties by throwing an exception when
     * such extra properties are defined.</li>
     * </ul>
     */
    private String extraPropertiesStorage;

    private NamespaceRegistry registry;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);
        this.registry = registry;

        // Initialize the directory path field that has been set via reflection when this method is called...
        checkFieldNotNull(directoryPath, "directoryPath");
        directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            String msg = JcrI18n.fileConnectorTopLevelDirectoryMissingOrCannotBeRead.text(getSourceName(), "directoryPath");
            throw new RepositoryException(msg);
        }
        if (!directory.canRead() && !directory.setReadable(true)) {
            String msg = JcrI18n.fileConnectorTopLevelDirectoryMissingOrCannotBeRead.text(getSourceName(), "directoryPath");
            throw new RepositoryException(msg);
        }
        directoryAbsolutePath = directory.getAbsolutePath();
        if (!directoryAbsolutePath.endsWith(DELIMITER)) directoryAbsolutePath = directoryAbsolutePath + DELIMITER;
        directoryAbsolutePathLength = directoryAbsolutePath.length() - 1; // does NOT include the delimiter

        // Initialize the filename filter ...
        filenameFilter = new InclusionExclusionFilenameFilter();
        if (exclusionPattern != null) filenameFilter.setExclusionPattern(exclusionPattern);
        if (inclusionPattern != null) filenameFilter.setInclusionPattern(exclusionPattern);

        // Set up the extra properties storage ...
        if (EXTRA_PROPERTIES_JSON.equalsIgnoreCase(extraPropertiesStorage)) {
            FileSystemConnectorJsonSidecarStorage store = new FileSystemConnectorJsonSidecarStorage(this);
            setExtraPropertiesStore(store);
            filenameFilter.setExtraPropertiesExclusionPattern(store.getExclusionPattern());
        } else if (EXTRA_PROPERTIES_LEGACY.equalsIgnoreCase(extraPropertiesStorage)) {
            FileSystemConnectorLegacySidecarStorage store = new FileSystemConnectorLegacySidecarStorage(this);
            setExtraPropertiesStore(store);
            filenameFilter.setExtraPropertiesExclusionPattern(store.getExclusionPattern());
        } else if (EXTRA_PROPERTIES_NONE.equalsIgnoreCase(extraPropertiesStorage)) {
            setExtraPropertiesStore(new NoExtraPropertiesStorage(this));
        }
        // otherwise use the default extra properties storage
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    DocumentTranslator getTranslator() {
        return translator();
    }

    NamespaceRegistry registry() {
        return registry;
    }

    protected boolean isContentNode( String id ) {
        return id.endsWith(JCR_CONTENT_SUFFIX);
    }

    protected File fileFor( String id ) {
        assert id.startsWith(DELIMITER);
        if (isContentNode(id)) {
            id = id.substring(0, id.length() - JCR_CONTENT_SUFFIX_LENGTH);
        }
        return new File(directory, id);
    }

    protected boolean isRoot( String id ) {
        return DELIMITER.equals(id);
    }

    protected String idFor( File file ) {
        String path = file.getAbsolutePath();
        if (!path.startsWith(directoryAbsolutePath)) {
            if (directory.getAbsolutePath().equals(path)) {
                // This is the root
                return DELIMITER;
            }
            String msg = JcrI18n.fileConnectorNodeIdentifierIsNotWithinScopeOfConnector.text(getSourceName(), directoryPath, path);
            throw new DocumentStoreException(path, msg);
        }
        String id = path.substring(directoryAbsolutePathLength);
        assert id.startsWith(DELIMITER);
        return id;
    }

    protected BinaryValue binaryFor( File file ) {
        try {
            byte[] sha1 = SecureHash.getHash(Algorithm.SHA_1, file);
            BinaryKey key = new BinaryKey(sha1);
            return createBinaryValue(key, file);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to create a {@link BinaryValue} object for the given file. Subclasses should rarely override this method,
     * since the {@link FileSystemBinaryValue} will be applicable in most situations.
     * 
     * @param key the binary key; never null
     * @param file the file for which the {@link BinaryValue} is to be created; never null
     * @return the binary value; never null
     * @throws IOException if there is an error creating the value
     */
    protected BinaryValue createBinaryValue( BinaryKey key,
                                             File file ) throws IOException {
        URL content = createUrlForFile(file);
        return new FileSystemBinaryValue(key, content, file.getTotalSpace(), getMimeTypeDetector());
    }

    /**
     * Construct a {@link URL} object for the given file, to be used within the {@link Binary} value representing the "jcr:data"
     * property of a 'nt:resource' node.
     * <p>
     * Subclasses can override this method to transform the URL into something different. For example, if the files are being
     * served by a web server, the overridden method might transform the file-based URL into the corresponding HTTP-based URL.
     * </p>
     * 
     * @param file the file for which the URL is to be created; never null
     * @return the URL for the file; never null
     * @throws IOException if there is an error creating the URL
     */
    protected URL createUrlForFile( File file ) throws IOException {
        return file.toURI().toURL();
    }

    /**
     * Utility method to determine if the file is excluded by the inclusion/exclusion filter.
     * 
     * @param file the file
     * @return true if the file is excluded, or false if it is to be included
     */
    protected boolean isExcluded( File file ) {
        return !filenameFilter.accept(file.getParentFile(), file.getName());
    }

    /**
     * Utility method to ensure that the file is writable by this connector.
     * 
     * @param id the identifier of the node
     * @param file the file
     * @throws DocumentStoreException if the file is expected to be writable but is not or is excluded, or if the connector is
     *         readonly
     */
    protected void checkWritable( String id,
                                  File file ) {
        if (readonly) {
            String msg = JcrI18n.fileConnectorIsReadOnly.text(getSourceName(), id, file.getAbsolutePath());
            throw new DocumentStoreException(id, msg);
        }
        if (isExcluded(file)) {
            String msg = JcrI18n.fileConnectorCannotStoreFileThatIsExcluded.text(getSourceName(), id, file.getAbsolutePath());
            throw new DocumentStoreException(id, msg);
        }
    }

    @Override
    public boolean hasDocument( String id ) {
        return fileFor(id).exists();
    }

    @Override
    public Document getDocumentById( String id ) {
        File file = fileFor(id);
        if (isExcluded(file)) return null;
        boolean isRoot = isRoot(id);
        boolean isResource = isContentNode(id);
        DocumentWriter writer = newDocument(id);
        File parentFile = file.getParentFile();
        if (isResource) {
            BinaryValue binaryValue = binaryFor(file);
            writer.setPrimaryType(NT_RESOURCE);
            writer.addProperty(JCR_DATA, binaryValue);
            if (addMimeTypeMixin) {
                String mimeType = null;
                String encoding = null; // We don't really know this
                try {
                    mimeType = binaryValue.getMimeType();
                } catch (Throwable e) {
                    getLogger().error(e, JcrI18n.couldNotGetMimeType, getSourceName(), id, e.getMessage());
                }
                writer.addProperty(JCR_ENCODING, encoding);
                writer.addProperty(JCR_MIME_TYPE, mimeType);
            }
            writer.addProperty(JCR_LAST_MODIFIED, factories().getDateFactory().create(file.lastModified()));
            writer.addProperty(JCR_LAST_MODIFIED_BY, null); // ignored
            parentFile = file;
        } else if (file.isFile()) {
            writer.setPrimaryType(NT_FILE);
            writer.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
            writer.addProperty(JCR_CREATED_BY, null); // ignored
            String childId = isRoot ? JCR_CONTENT_SUFFIX : id + JCR_CONTENT_SUFFIX;
            writer.addChild(childId, JCR_CONTENT);
        } else {
            writer.setPrimaryType(NT_FOLDER);
            writer.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
            writer.addProperty(JCR_CREATED_BY, null); // ignored
            for (String childName : file.list(filenameFilter)) {
                // We use identifiers that contain the file/directory name ...
                String childId = isRoot ? DELIMITER + childName : id + DELIMITER + childName;
                writer.addChild(childId, childName);
            }
        }
        // Set the reference to the parent ...
        String parentId = idFor(parentFile);
        writer.setParents(parentId);

        // Add the extra properties (if there are any), overwriting any properties with the same names
        // (e.g., jcr:primaryType, jcr:mixinTypes, jcr:mimeType, etc.) ...
        writer.addProperties(extraPropertiesStore().getProperties(id));

        // Add the 'mix:mixinType' mixin; if other mixins are stored in the extra properties, this will append ...
        if (addMimeTypeMixin) {
            writer.addMixinType(MIX_MIME_TYPE);
        }

        // Return the document ...
        return writer.document();
    }

    @Override
    public String getDocumentId( String path ) {
        String id = path; // this connector treats the ID as the path
        File file = fileFor(id);
        return file.exists() ? id : null;
    }

    @Override
    public boolean removeDocument( String id ) {
        File file = fileFor(id);
        checkWritable(id, file);
        // Remove the extra properties at the old location ...
        extraPropertiesStore().removeProperties(id);
        // Now remove the file (if it is there) ...
        if (!file.exists()) return false;
        FileUtil.delete(file); // recursive delete
        return true;
    }

    @Override
    public void storeDocument( Document document ) {
        // Create a new directory or file described by the document ...
        DocumentReader reader = readDocument(document);
        String id = reader.getDocumentId();
        File file = fileFor(id);
        checkWritable(id, file);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!parent.canWrite()) {
            String parentPath = parent.getAbsolutePath();
            String msg = JcrI18n.fileConnectorCannotWriteToDirectory.text(getSourceName(), getClass(), parentPath);
            throw new DocumentStoreException(id, msg);
        }
        String primaryType = reader.getPrimaryTypeName();
        Map<Name, Property> properties = reader.getProperties();
        ExtraProperties extraProperties = extraPropertiesFor(id, false);
        extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE, JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
        try {
            if (NT_FILE.equals(primaryType)) {
                file.createNewFile();
            } else if (NT_FOLDER.equals(primaryType)) {
                file.mkdirs();
            } else if (isContentNode(id)) {
                Property content = properties.get(JcrLexicon.DATA);
                BinaryValue binary = factories().getBinaryFactory().create(content.getFirstValue());
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                IoUtil.write(binary.getStream(), ostream);
                if (!NT_RESOURCE.equals(primaryType)) {
                    // This is the "jcr:content" child, but the primary type is non-standard so record it as an extra property
                    extraProperties.add(properties.get(JcrLexicon.PRIMARY_TYPE));
                }
            }
            extraProperties.save();
        } catch (RepositoryException e) {
            throw new DocumentStoreException(id, e);
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public void updateDocument( String id,
                                Document document ) {
        // Create a new directory or file described by the document ...
        DocumentReader reader = readDocument(document);
        String parentId = reader.getParentIds().get(0);
        File file = fileFor(id);
        checkWritable(id, file);
        File parent = file.getParentFile();
        String newParentId = idFor(parent);
        if (!parentId.equals(newParentId)) {
            // The node has a new parent (via the 'update' method), meaning it was moved ...
            File newParent = fileFor(newParentId);
            File newFile = new File(newParent, file.getName());
            file.renameTo(newFile);
            if (!parent.exists()) {
                parent.mkdirs(); // in case they were removed since we created them ...
            }
            if (!parent.canWrite()) {
                String parentPath = newParent.getAbsolutePath();
                String msg = JcrI18n.fileConnectorCannotWriteToDirectory.text(getSourceName(), getClass(), parentPath);
                throw new DocumentStoreException(id, msg);
            }
            parent = newParent;
            // Remove the extra properties at the old location ...
            extraPropertiesStore().removeProperties(id);
            // Set the id to the new location ...
            id = idFor(newFile);
        } else {
            // It is the same parent as before ...
            if (!parent.exists()) {
                parent.mkdirs(); // in case they were removed since we created them ...
            }
            if (!parent.canWrite()) {
                String parentPath = parent.getAbsolutePath();
                String msg = JcrI18n.fileConnectorCannotWriteToDirectory.text(getSourceName(), getClass(), parentPath);
                throw new DocumentStoreException(id, msg);
            }
        }
        String primaryType = reader.getPrimaryTypeName();
        Map<Name, Property> properties = reader.getProperties();
        ExtraProperties extraProperties = extraPropertiesFor(id, true);
        extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE, JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
        try {
            if (NT_FILE.equals(primaryType)) {
                file.createNewFile();
            } else if (NT_FOLDER.equals(primaryType)) {
                file.mkdir();
            } else if (isContentNode(id)) {
                Property content = reader.getProperty(JCR_DATA);
                BinaryValue binary = factories().getBinaryFactory().create(content.getFirstValue());
                OutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
                IoUtil.write(binary.getStream(), ostream);
                if (!NT_RESOURCE.equals(primaryType)) {
                    // This is the "jcr:content" child, but the primary type is non-standard so record it as an extra property
                    extraProperties.add(properties.get(JcrLexicon.PRIMARY_TYPE));
                }
            }
            extraProperties.save();
        } catch (RepositoryException e) {
            throw new DocumentStoreException(id, e);
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    /**
     * A {@link FilenameFilter} implementation that supports an inclusion and exclusion pattern.
     */
    protected static class InclusionExclusionFilenameFilter implements java.io.FilenameFilter {
        private String inclusionPattern = null;
        private String exclusionPattern = null;
        private Pattern inclusion;
        private Pattern exclusion;
        private Pattern extraPropertiesExclusion;

        public void setExclusionPattern( String exclusionPattern ) {
            this.exclusionPattern = exclusionPattern;
            if (exclusionPattern == null) {
                this.exclusion = null;
            } else {
                this.exclusion = Pattern.compile(exclusionPattern);
            }
        }

        public void setExtraPropertiesExclusionPattern( String exclusionPattern ) {
            if (exclusionPattern == null) {
                this.extraPropertiesExclusion = null;
            } else {
                this.extraPropertiesExclusion = Pattern.compile(exclusionPattern);
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
            if (inclusionPattern == null) {
                // Include unless it matches an exclusion ...
                if (exclusionPattern != null && exclusion.matcher(string).matches()) return false;
                if (extraPropertiesExclusion != null && extraPropertiesExclusion.matcher(string).matches()) return false;
                return true;
            }
            // Include ONLY if it matches the inclusion AND not matched by the exclusions ...
            if (!inclusion.matcher(string).matches()) return false;
            if (exclusionPattern != null && exclusion.matcher(string).matches()) return false;
            if (extraPropertiesExclusion != null && extraPropertiesExclusion.matcher(string).matches()) return false;
            return true;
        }
    }

    /**
     * A {@link BinaryValue} implementation used to read the content of files exposed through this connector. This class computes
     * the {@link AbstractBinary#getMimeType() MIME type} lazily or upon serialization.
     */
    protected static class FileSystemBinaryValue extends AbstractBinary implements Externalizable {
        private static final long serialVersionUID = 1L;

        private transient MimeTypeDetector mimeTypeDetector;
        private transient String nameHint; // only needed for MIME type detection; not needed once MIME type is known
        private String mimeType;
        private URL url;
        private long size;
        private boolean detectedMimeType = false;

        protected FileSystemBinaryValue( BinaryKey binaryKey,
                                         URL content,
                                         long size,
                                         MimeTypeDetector mimeTypeDetector ) {
            super(binaryKey);
            this.url = content;
            this.size = size;
        }

        protected URL toUrl() {
            return url;
        }

        protected void setMimeType( String mimeType ) {
            this.mimeType = mimeType;
        }

        protected boolean hasMimeType() {
            return mimeType != null;
        }

        @Override
        public String getMimeType() {
            if (!detectedMimeType && mimeTypeDetector != null) {
                try {
                    mimeType = mimeTypeDetector.mimeTypeOf(nameHint, this);
                } catch (Throwable t) {
                    Logger.getLogger(getClass()).debug("Unable to compute MIME Type for file at {0}", toUrl());
                    throw new RuntimeException(t);
                } finally {
                    detectedMimeType = true;
                }
            }
            return mimeType;
        }

        @Override
        public String getMimeType( String name ) {
            return getMimeType();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public InputStream getStream() throws RepositoryException {
            try {
                return new BufferedInputStream(url.openStream());
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        @Override
        public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
            this.mimeType = in.readUTF();
            this.detectedMimeType = in.readBoolean();
            this.url = (URL)in.readObject();
            this.size = in.readLong();
        }

        @Override
        public void writeExternal( ObjectOutput out ) throws IOException {
            out.writeUTF(getMimeType());
            out.writeBoolean(detectedMimeType);
            out.writeObject(this.url);
            out.writeLong(this.size);
        }
    }
}
