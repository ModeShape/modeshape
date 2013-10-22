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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.NoExtraPropertiesStorage;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.ConnectorException;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.WritableConnector;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;

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
public class FileSystemConnector extends WritableConnector implements Pageable {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
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
     * The maximum number of children a folder will expose at any given time.
     */
    private int pageSize = 20;

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

    /**
     * A boolean which determines whether for external binary values (i.e. {@link UrlBinaryValue}) the SHA1 is computed based
     * on the content of the file itself or whether it's computed based on the URL string. This is {@code true} by default, but
     * if the connector needs to deal with very large values it might be worth turning off.
     */
    private boolean contentBasedSha1 = true;

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
        if (!directoryAbsolutePath.endsWith(FILE_SEPARATOR)) directoryAbsolutePath = directoryAbsolutePath + FILE_SEPARATOR;
        directoryAbsolutePathLength = directoryAbsolutePath.length() - FILE_SEPARATOR.length(); // does NOT include the separtor

        // Initialize the filename filter ...
        filenameFilter = new InclusionExclusionFilenameFilter();
        if (exclusionPattern != null) filenameFilter.setExclusionPattern(exclusionPattern);
        if (inclusionPattern != null) filenameFilter.setInclusionPattern(inclusionPattern);

        // Set up the extra properties storage ...
        if (EXTRA_PROPERTIES_JSON.equalsIgnoreCase(extraPropertiesStorage)) {
            JsonSidecarExtraPropertyStore store = new JsonSidecarExtraPropertyStore(this, translator());
            setExtraPropertiesStore(store);
            filenameFilter.setExtraPropertiesExclusionPattern(store.getExclusionPattern());
        } else if (EXTRA_PROPERTIES_LEGACY.equalsIgnoreCase(extraPropertiesStorage)) {
            LegacySidecarExtraPropertyStore store = new LegacySidecarExtraPropertyStore(this);
            setExtraPropertiesStore(store);
            filenameFilter.setExtraPropertiesExclusionPattern(store.getExclusionPattern());
        } else if (EXTRA_PROPERTIES_NONE.equalsIgnoreCase(extraPropertiesStorage)) {
            setExtraPropertiesStore(new NoExtraPropertiesStorage(this));
        }
        // otherwise use the default extra properties storage
    }

    /**
     * Get the namespace registry.
     * 
     * @return the namespace registry; never null
     */
    NamespaceRegistry registry() {
        return registry;
    }

    /**
     * Utility method for determining if the supplied identifier is for the "jcr:content" child node of a file. * Subclasses may
     * override this method to change the format of the identifiers, but in that case should also override the
     * {@link #fileFor(String)}, {@link #isRoot(String)}, and {@link #idFor(File)} methods.
     * 
     * @param id the identifier; may not be null
     * @return true if the identifier signals the "jcr:content" child node of a file, or false otherwise
     * @see #isRoot(String)
     * @see #fileFor(String)
     * @see #idFor(File)
     */
    protected boolean isContentNode( String id ) {
        return id.endsWith(JCR_CONTENT_SUFFIX);
    }

    /**
     * Utility method for obtaining the {@link File} object that corresponds to the supplied identifier. Subclasses may override
     * this method to change the format of the identifiers, but in that case should also override the {@link #isRoot(String)},
     * {@link #isContentNode(String)}, and {@link #idFor(File)} methods.
     * 
     * @param id the identifier; may not be null
     * @return the File object for the given identifier
     * @see #isRoot(String)
     * @see #isContentNode(String)
     * @see #idFor(File)
     */
    protected File fileFor( String id ) {
        assert id.startsWith(DELIMITER);
        if (id.endsWith(DELIMITER)) {
            id = id.substring(0, id.length() - DELIMITER.length());
        }
        if (isContentNode(id)) {
            id = id.substring(0, id.length() - JCR_CONTENT_SUFFIX_LENGTH);
        }
        return new File(directory, id);
    }

    /**
     * Utility method for determining if the node identifier is the identifier of the root node in this external source.
     * Subclasses may override this method to change the format of the identifiers, but in that case should also override the
     * {@link #fileFor(String)}, {@link #isContentNode(String)}, and {@link #idFor(File)} methods.
     * 
     * @param id the identifier; may not be null
     * @return true if the identifier is for the root of this source, or false otherwise
     * @see #isContentNode(String)
     * @see #fileFor(String)
     * @see #idFor(File)
     */
    protected boolean isRoot( String id ) {
        return DELIMITER.equals(id);
    }

    /**
     * Utility method for determining the node identifier for the supplied file. Subclasses may override this method to change the
     * format of the identifiers, but in that case should also override the {@link #fileFor(String)},
     * {@link #isContentNode(String)}, and {@link #isRoot(String)} methods.
     * 
     * @param file the file; may not be null
     * @return the node identifier; never null
     * @see #isRoot(String)
     * @see #isContentNode(String)
     * @see #fileFor(String)
     */
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
        id = id.replaceAll(Pattern.quote(FILE_SEPARATOR), DELIMITER);
        assert id.startsWith(DELIMITER);
        return id;
    }

    /**
     * Utility method for creating a {@link BinaryValue} for the given {@link File} object. Subclasses should rarely override this
     * method.
     * 
     * @param file the file; may not be null
     * @return the BinaryValue; never null
     */
    protected ExternalBinaryValue binaryFor( File file ) {
        try {
            return createBinaryValue(file);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to create a {@link BinaryValue} object for the given file. Subclasses should rarely override this method,
     * since the {@link UrlBinaryValue} will be applicable in most situations.
     * 
     * @param file the file for which the {@link BinaryValue} is to be created; never null
     * @return the binary value; never null
     * @throws IOException if there is an error creating the value
     */
    protected ExternalBinaryValue createBinaryValue( File file ) throws IOException {
        URL content = createUrlForFile(file);
        return new UrlBinaryValue(sha1(file), getSourceName(), content, file.length(), file.getName(), getMimeTypeDetector());
    }

    /**
     * Computes the SHA1 for the given file. By default, this method will look at the {@link FileSystemConnector#contentBasedSha1()}
     * flag and either take the URL of the file (using @see java.util.File#toURI().toURL() and return the SHA1 of the URL string
     * or return the SHA1 of the entire file content.
     *
     * @param file a {@link File} instance; never null
     * @return the SHA1 of the file.
     */
    protected String sha1(File file) {
        try {
            if (contentBasedSha1()) {
                byte[] hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, file);
                return StringUtil.getHexString(hash);
            } else {
                return SecureHash.sha1(createUrlForFile(file).toString());
            }
        } catch (Exception e) {
            throw new ConnectorException(e);
        }
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

    protected File createFileForUrl( URL url ) throws URISyntaxException {
        return new File(url.toURI());
    }

    protected boolean contentBasedSha1() {
        return contentBasedSha1;
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
    protected void checkFileNotExcluded( String id,
                                         File file ) {
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
        if (isExcluded(file) || !file.exists()) return null;
        boolean isRoot = isRoot(id);
        boolean isResource = isContentNode(id);
        DocumentWriter writer = null;
        File parentFile = file.getParentFile();
        if (isResource) {
            writer = newDocument(id);
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

            // make these binary not queryable. If we really want to query them, we need to switch to external binaries
            writer.setNotQueryable();
            parentFile = file;
        } else if (file.isFile()) {
            writer = newDocument(id);
            writer.setPrimaryType(NT_FILE);
            writer.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
            writer.addProperty(JCR_CREATED_BY, null); // ignored
            String childId = isRoot ? JCR_CONTENT_SUFFIX : id + JCR_CONTENT_SUFFIX;
            writer.addChild(childId, JCR_CONTENT);
        } else {
            writer = newFolderWriter(id, file, 0);
        }

        if (!isRoot) {
            // Set the reference to the parent ...
            String parentId = idFor(parentFile);
            writer.setParents(parentId);
        }

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

    private DocumentWriter newFolderWriter( String id,
                                            File file,
                                            int offset ) {
        boolean root = isRoot(id);
        DocumentWriter writer = newDocument(id);
        writer.setPrimaryType(NT_FOLDER);
        writer.addProperty(JCR_CREATED, factories().getDateFactory().create(file.lastModified()));
        writer.addProperty(JCR_CREATED_BY, null); // ignored
        File[] children = file.listFiles(filenameFilter);
        long totalChildren = 0;
        int nextOffset = 0;
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            // Only include as a child if we can access and read the file. Permissions might prevent us from
            // reading the file, and the file might not exist if it is a broken symlink (see MODE-1768 for details).
            if (child.exists() && child.canRead() && (child.isFile() || child.isDirectory())) {
                // we need to count the total accessible children
                totalChildren++;
                // only add a child if it's in the current page
                if (i >= offset && i < offset + pageSize) {
                    // We use identifiers that contain the file/directory name ...
                    String childName = child.getName();
                    String childId = root ? DELIMITER + childName : id + DELIMITER + childName;
                    writer.addChild(childId, childName);
                    nextOffset = i + 1;
                }
            }
        }
        // if there are still accessible children add the next page
        if (nextOffset < totalChildren) {
            writer.addPage(id, nextOffset, pageSize, totalChildren);
        }
        return writer;
    }

    @Override
    public String getDocumentId( String path ) {
        String id = path; // this connector treats the ID as the path
        File file = fileFor(id);
        return file.exists() ? id : null;
    }

    @Override
    public Collection<String> getDocumentPathsById( String id ) {
        // this connector treats the ID as the path
        return Collections.singletonList(id);
    }

    @Override
    public ExternalBinaryValue getBinaryValue( String id ) {
        try {
            File f = createFileForUrl(new URL(id));
            return binaryFor(f);
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        } catch (URISyntaxException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public boolean removeDocument( String id ) {
        File file = fileFor(id);
        checkFileNotExcluded(id, file);
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
        checkFileNotExcluded(id, file);
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
    public String newDocumentId( String parentId,
                                 Name newDocumentName,
                                 Name newDocumentPrimaryType ) {
        StringBuilder id = new StringBuilder(parentId);
        if (!parentId.endsWith(DELIMITER)) {
            id.append(DELIMITER);
        }

        // We're only using the name to check, which can be a bit dangerous if users don't follow the JCR conventions.
        // However, it matches what "isContentNode(...)" does.
        String childNameStr = getContext().getValueFactories().getStringFactory().create(newDocumentName);
        if (JCR_CONTENT.equals(childNameStr)) {
            // This is for the "jcr:content" node underneath a file node. Since this doesn't actually result in a file or folder
            // on the file system (it's merged into the file for the parent 'nt:file' node), we'll keep the "jcr" namespace
            // prefix in the ID so that 'isContentNode(...)' works properly ...
            id.append(childNameStr);
        } else {
            // File systems don't universally deal well with ':' in the names, and when they do it can be a bit awkward. Since we
            // don't often expect the node NAMES to contain namespaces (at leat with this connector), we'll just
            // use the local part for the ID ...
            id.append(newDocumentName.getLocalName());
            if (!StringUtil.isBlank(newDocumentName.getNamespaceUri())) {
                // the FS connector does not support namespaces in names
                String ns = newDocumentName.getNamespaceUri();
                getLogger().warn(JcrI18n.fileConnectorNamespaceIgnored, getSourceName(), ns, id, childNameStr, parentId);
            }
        }
        return id.toString();
    }

    @Override
    public void updateDocument( DocumentChanges documentChanges ) {
        String id = documentChanges.getDocumentId();

        Document document = documentChanges.getDocument();
        DocumentReader reader = readDocument(document);

        File file = fileFor(id);
        String idOrig = id;

        // if we're dealing with the root of the connector, we can't process any moves/removes because that would go "outside" the
        // connector scope
        if (!isRoot(id)) {
            String parentId = reader.getParentIds().get(0);
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
        }

        // children renames have to be processed in the parent
        DocumentChanges.ChildrenChanges childrenChanges = documentChanges.getChildrenChanges();
        Map<String, Name> renamedChildren = childrenChanges.getRenamed();
        for (String renamedChildId : renamedChildren.keySet()) {
            File child = fileFor(renamedChildId);
            Name newName = renamedChildren.get(renamedChildId);
            String newNameStr = getContext().getValueFactories().getStringFactory().create(newName);
            File renamedChild = new File(file, newNameStr);
            if (!child.renameTo(renamedChild)) {
                getLogger().debug("Cannot rename {0} to {1}", child, renamedChild);
            }
        }

        String primaryType = reader.getPrimaryTypeName();
        Map<Name, Property> properties = reader.getProperties();
        id = idOrig;
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

    @Override
    public Document getChildren( PageKey pageKey ) {
        String parentId = pageKey.getParentId();
        File folder = fileFor(parentId);
        assert folder.isDirectory();
        if (!folder.canRead()) {
            getLogger().debug("Cannot read the {0} folder", folder.getAbsolutePath());
            return null;
        }
        return newFolderWriter(parentId, folder, pageKey.getOffsetInt()).document();
    }
}
