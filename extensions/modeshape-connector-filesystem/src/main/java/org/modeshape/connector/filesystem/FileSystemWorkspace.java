package org.modeshape.connector.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.connector.base.PathWorkspace;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.property.basic.FileSystemBinary;
import org.modeshape.graph.request.Request;

/**
 * Workspace implementation for the file system connector.
 */
class FileSystemWorkspace extends PathWorkspace<PathNode> {
    private static final Map<Name, Property> NO_PROPERTIES = Collections.emptyMap();
    private static final String DEFAULT_MIME_TYPE = "application/octet";
    private static final Set<Name> VALID_PRIMARY_TYPES = new HashSet<Name>(Arrays.asList(new Name[] {JcrNtLexicon.FOLDER,
        JcrNtLexicon.FILE, JcrNtLexicon.RESOURCE, ModeShapeLexicon.RESOURCE}));

    private final FileSystemSource source;
    private final FileSystemRepository repository;
    private final ExecutionContext context;
    private final File workspaceRoot;
    private final boolean eagerLoading;
    private final boolean contentUsedToDetermineMimeType;
    private final Logger logger;
    private final ValueFactory<String> stringFactory;
    private final NameFactory nameFactory;

    public FileSystemWorkspace( String name,
                                FileSystemWorkspace originalToClone,
                                File workspaceRoot ) {
        super(name, originalToClone.getRootNodeUuid());

        this.source = originalToClone.source;
        this.context = originalToClone.context;
        this.workspaceRoot = workspaceRoot;
        this.repository = originalToClone.repository;
        this.eagerLoading = this.source.isEagerFileLoading();
        this.contentUsedToDetermineMimeType = this.source.isContentUsedToDetermineMimeType();
        this.logger = Logger.getLogger(getClass());
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();

        cloneWorkspace(originalToClone);
    }

    public FileSystemWorkspace( FileSystemRepository repository,
                                String name ) {
        super(name, repository.getRootNodeUuid());
        this.workspaceRoot = repository.getWorkspaceDirectory(name);
        this.repository = repository;
        this.context = repository.getContext();
        this.source = repository.source;
        this.eagerLoading = this.source.isEagerFileLoading();
        this.contentUsedToDetermineMimeType = this.source.isContentUsedToDetermineMimeType();
        this.logger = Logger.getLogger(getClass());
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
    }

    private void cloneWorkspace( FileSystemWorkspace original ) {
        File originalRoot = repository.getWorkspaceDirectory(original.getName());
        File newRoot = repository.getWorkspaceDirectory(this.getName());

        try {
            FileUtil.copy(originalRoot, newRoot, source.filenameFilter(false));
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private void moveFile( File originalFileOrDirectory,
                           File newFileOrDirectory ) {
        if (originalFileOrDirectory.renameTo(newFileOrDirectory)) return;

        /*
         * This could fail if the originalFile and newFile are on different file systems.  See
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4073756.  Try to do a copy and delete to
         * work around this potential issue. 
         */
        try {
            FileUtil.copy(originalFileOrDirectory, newFileOrDirectory);
            FileUtil.delete(originalFileOrDirectory);
        } catch (IOException ioe) {
            throw new RepositorySourceException(FileSystemI18n.couldNotCopyData.text(source.getName(),
                                                                                 originalFileOrDirectory.getAbsolutePath(),
                                                                                 newFileOrDirectory.getAbsolutePath()), ioe);
        }

    }

    @Override
    public PathNode moveNode( PathNode node,
                              PathNode newNode ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path newPath = pathFactory.create(newNode.getParent(), newNode.getName());
        Path oldPath = pathFactory.create(node.getParent(), node.getName());
        File originalFile = fileFor(oldPath);
        File newFile = fileFor(newPath, false);

        if (newFile.exists()) {
            newFile.delete();
        }

        // Read the custom properties ...
        CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();
        Collection<Property> existingProps = null;
        Collection<Property> existingResourceProps = null;
        String sourceName = source.getName();
        Location originalLocation = Location.create(oldPath);
        if (originalFile.isDirectory()) {
            existingProps = customPropertiesFactory.getDirectoryProperties(context, originalLocation, originalFile);
            customPropertiesFactory.recordDirectoryProperties(context, sourceName, originalLocation, originalFile, NO_PROPERTIES);
        } else {
            Path resourcePath = pathFactory.create(oldPath, JcrLexicon.CONTENT);
            Location originalResourceLocation = Location.create(resourcePath);
            existingProps = customPropertiesFactory.getFileProperties(context, originalLocation, originalFile);
            existingResourceProps = customPropertiesFactory.getResourceProperties(context,
                                                                                  originalResourceLocation,
                                                                                  originalFile,
                                                                                  null);
            customPropertiesFactory.recordFileProperties(context, sourceName, originalLocation, originalFile, NO_PROPERTIES);
            customPropertiesFactory.recordResourceProperties(context,
                                                             sourceName,
                                                             originalResourceLocation,
                                                             originalFile,
                                                             NO_PROPERTIES);
        }

        moveFile(originalFile, newFile);

        // Set the custom properties on the new location ...
        Location newLocation = Location.create(newPath);
        if (originalFile.isDirectory()) {
            customPropertiesFactory.recordDirectoryProperties(context,
                                                              sourceName,
                                                              newLocation,
                                                              newFile,
                                                              extraFolder(mapOf(existingProps)));
        } else {
            Path resourcePath = pathFactory.create(newPath, JcrLexicon.CONTENT);
            Location resourceLocation = Location.create(resourcePath);
            customPropertiesFactory.recordFileProperties(context,
                                                         sourceName,
                                                         newLocation,
                                                         newFile,
                                                         extraFile(mapOf(existingProps)));
            customPropertiesFactory.recordResourceProperties(context,
                                                             sourceName,
                                                             resourceLocation,
                                                             newFile,
                                                             extraResource(mapOf(existingResourceProps)));
        }

        return getNode(newPath);
    }

    protected Map<Name, Property> mapOf( Collection<Property> properties ) {
        if (properties == null || properties.isEmpty()) return Collections.emptyMap();
        Map<Name, Property> result = new HashMap<Name, Property>();
        for (Property property : properties) {
            result.put(property.getName(), property);
        }
        return result;
    }

    @Override
    public PathNode putNode( PathNode node ) {
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        NamespaceRegistry registry = context.getNamespaceRegistry();
        CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();

        Map<Name, Property> properties = node.getProperties();

        if (node.getParent() == null) {
            // Root node
            Path rootPath = pathFactory.createRootPath();
            Location rootLocation = Location.create(rootPath, repository.getRootNodeUuid());
            customPropertiesFactory.recordDirectoryProperties(context,
                                                              source.getName(),
                                                              rootLocation,
                                                              workspaceRoot,
                                                              extraFolder(node.getProperties()));
            return getNode(rootPath);
        }

        /*
         * Get references to java.io.Files
         */
        Path parentPath = node.getParent();
        boolean isRoot = parentPath == null;
        File parentFile = fileFor(parentPath);

        Path newPath = isRoot ? pathFactory.createRootPath() : pathFactory.create(parentPath, node.getName());
        Name name = node.getName().getName();
        String newName = name.getString(registry);
        File newFile = new File(parentFile, newName);

        /*
         * Determine the node primary type
         */
        Property primaryTypeProp = properties.get(JcrLexicon.PRIMARY_TYPE);

        // Default primary type to nt:folder
        Name primaryType = primaryTypeProp == null ? JcrNtLexicon.FOLDER : nameFactory.create(primaryTypeProp.getFirstValue());

        if (JcrNtLexicon.FILE.equals(primaryType)) {

            // The FILE node is represented by the existence of the file
            if (!parentFile.canWrite()) {
                I18n msg = FileSystemI18n.parentIsReadOnly;
                throw new RepositorySourceException(source.getName(), msg.text(parentPath, this.getName(), source.getName()));
            }

            try {
                ensureValidPathLength(newFile);

                // Don't try to write if the node conflict behavior is DO_NOT_REPLACE
                if (!newFile.exists() && !newFile.createNewFile()) {
                    I18n msg = FileSystemI18n.fileAlreadyExists;
                    throw new RepositorySourceException(source.getName(), msg.text(parentPath, getName(), source.getName()));
                }
            } catch (IOException ioe) {
                I18n msg = FileSystemI18n.couldNotCreateFile;
                throw new RepositorySourceException(source.getName(), msg.text(parentPath,
                                                                               getName(),
                                                                               source.getName(),
                                                                               ioe.getMessage()), ioe);
            }

            customPropertiesFactory.recordFileProperties(context,
                                                         source.getName(),
                                                         Location.create(newPath),
                                                         newFile,
                                                         extraFile(properties));
        } else if (JcrNtLexicon.RESOURCE.equals(primaryType) || ModeShapeLexicon.RESOURCE.equals(primaryType)) {
            assert parentFile != null;

            if (!JcrLexicon.CONTENT.equals(name)) {
                I18n msg = FileSystemI18n.invalidNameForResource;
                String nodeName = name.getString();
                throw new RepositorySourceException(source.getName(), msg.text(parentPath, getName(), source.getName(), nodeName));
            }

            if (!parentFile.isFile()) {
                I18n msg = FileSystemI18n.invalidPathForResource;
                throw new RepositorySourceException(source.getName(), msg.text(parentPath, getName(), source.getName()));
            }

            if (!parentFile.canWrite()) {
                I18n msg = FileSystemI18n.parentIsReadOnly;
                throw new RepositorySourceException(source.getName(), msg.text(parentPath, getName(), source.getName()));
            }

            // Copy over data into a temp file, then move it to the correct location
            FileOutputStream fos = null;
            try {
                File temp = File.createTempFile("modeshape", null);
                fos = new FileOutputStream(temp);

                Property dataProp = properties.get(JcrLexicon.DATA);
                BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                Binary binary = null;
                if (dataProp == null) {
                    // There is no content, so make empty content ...
                    binary = binaryFactory.create(new byte[] {});
                    dataProp = context.getPropertyFactory().create(JcrLexicon.DATA, new Object[] {binary});
                } else {
                    // Must read the value ...
                    binary = binaryFactory.create(properties.get(JcrLexicon.DATA).getFirstValue());
                }

                IoUtil.write(binary.getStream(), fos);

                if (!FileUtil.delete(parentFile)) {
                    I18n msg = FileSystemI18n.deleteFailed;
                    throw new RepositorySourceException(source.getName(), msg.text(parentPath, getName(), source.getName()));
                }

                moveFile(temp, parentFile);
            } catch (IOException ioe) {
                I18n msg = FileSystemI18n.couldNotWriteData;
                throw new RepositorySourceException(source.getName(), msg.text(parentPath,
                                                                               getName(),
                                                                               source.getName(),
                                                                               ioe.getMessage()), ioe);

            } finally {
                try {
                    if (fos != null) fos.close();
                } catch (Exception ex) {
                }
            }
            customPropertiesFactory.recordResourceProperties(context,
                                                             source.getName(),
                                                             Location.create(parentPath),
                                                             parentFile,
                                                             extraResource(properties));

        } else if (JcrNtLexicon.FOLDER.equals(primaryType) || primaryType == null) {
            ensureValidPathLength(newFile);

            if (!newFile.exists() && !newFile.mkdir()) {
                I18n msg = FileSystemI18n.couldNotCreateFile;
                throw new RepositorySourceException(source.getName(),
                                                    msg.text(parentPath,
                                                             getName(),
                                                             source.getName(),
                                                             primaryType == null ? "null" : primaryType.getString(registry)));
            }
            customPropertiesFactory.recordDirectoryProperties(context,
                                                              source.getName(),
                                                              Location.create(newPath),
                                                              newFile,
                                                              extraFolder(properties));

        } else {
            // Set error and return
            I18n msg = FileSystemI18n.unsupportedPrimaryType;
            throw new RepositorySourceException(source.getName(), msg.text(primaryType.getString(registry),
                                                                           parentPath,
                                                                           getName(),
                                                                           source.getName()));
        }

        node = getNode(newPath);

        return node;
    }

    @Override
    public PathNode removeNode( Path nodePath ) {
        File nodeFile;

        CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();

        if (!nodePath.isRoot() && JcrLexicon.CONTENT.equals(nodePath.getLastSegment().getName())) {
            nodeFile = fileFor(nodePath.getParent());

            // Have the custom property factory remote all properties ...
            customPropertiesFactory.recordResourceProperties(context,
                                                             source.getName(),
                                                             Location.create(nodePath),
                                                             nodeFile,
                                                             NO_PROPERTIES);
            if (!nodeFile.exists()) return null;

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(nodeFile);
                IoUtil.write("", fos);
            } catch (IOException ioe) {
                throw new RepositorySourceException(source.getName(), FileSystemI18n.deleteFailed.text(nodePath,
                                                                                                       getName(),
                                                                                                       source.getName()));
            } finally {
                if (fos != null) try {
                    fos.close();
                } catch (IOException ioe) {
                }
            }
        } else {
            nodeFile = fileFor(nodePath);
            // Have the custom property factory remote all properties ...
            customPropertiesFactory.recordResourceProperties(context,
                                                             source.getName(),
                                                             Location.create(nodePath),
                                                             nodeFile,
                                                             NO_PROPERTIES);
            if (!nodeFile.exists()) return null;

            FileUtil.delete(nodeFile);
        }

        return null;
    }

    @Override
    public PathNode getRootNode() {
        return getNode(context.getValueFactories().getPathFactory().createRootPath());
    }

    @Override
    public PathNode getNode( Path path ) {
        Map<Name, Property> properties = new HashMap<Name, Property>();

        long startTime = System.nanoTime();
        Name nodeType = null;
        try {

            PropertyFactory factory = context.getPropertyFactory();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();
            MimeTypeDetector mimeTypeDetector = context.getMimeTypeDetector();
            CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();
            NamespaceRegistry registry = context.getNamespaceRegistry();
            Location location = Location.create(path);

            if (!path.isRoot() && JcrLexicon.CONTENT.equals(path.getLastSegment().getName())) {
                File file = fileFor(path.getParent());
                if (file == null) return null;

                // First add any custom properties ...
                Collection<Property> customProps = customPropertiesFactory.getResourceProperties(context, location, file, null);
                for (Property customProp : customProps) {
                    properties.put(customProp.getName(), customProp);
                }

                if (!properties.containsKey(JcrLexicon.MIMETYPE)) {
                    // Discover the mime type ...
                    String mimeType = null;
                    InputStream contents = null;
                    try {
                        // First try the file name (so we don't have to create an input stream,
                        // which may have too much latency if a remote network file) ...
                        mimeType = mimeTypeDetector.mimeTypeOf(file.getName(), null);
                        if (mimeType == null && contentUsedToDetermineMimeType) {
                            // Try to find the mime type using the content ...
                            contents = new BufferedInputStream(new FileInputStream(file));
                            mimeType = mimeTypeDetector.mimeTypeOf(null, contents);
                        }
                        if (mimeType == null) mimeType = DEFAULT_MIME_TYPE;
                        properties.put(JcrLexicon.MIMETYPE, factory.create(JcrLexicon.MIMETYPE, mimeType));
                    } catch (IOException e) {
                        I18n msg = FileSystemI18n.couldNotReadData;
                        throw new RepositorySourceException(source.getName(), msg.text(source.getName(),
                                                                                       getName(),
                                                                                       path.getString(registry)));
                    } finally {
                        if (contents != null) {
                            try {
                                contents.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }

                // The request is to get properties of the "jcr:content" child node ...
                // ... use the dna:resource node type. This is the same as nt:resource, but is not referenceable
                // since we cannot assume that we control all access to this file and can track its movements
                Property primaryType = properties.get(JcrLexicon.PRIMARY_TYPE);
                if (primaryType == null) {
                    nodeType = JcrNtLexicon.RESOURCE;
                    properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE));
                } else {
                    nodeType = nameValueFor(primaryType);
                }
                properties.put(JcrLexicon.LAST_MODIFIED, factory.create(JcrLexicon.LAST_MODIFIED,
                                                                        dateFactory.create(file.lastModified())));

                // Now put the file's content into the "jcr:data" property ...
                Binary binary = binaryForContent(file);
                properties.put(JcrLexicon.DATA, factory.create(JcrLexicon.DATA, binary));

                // Don't really know the encoding, either ...
                // properties.put(JcrLexicon.ENCODED, factory.create(JcrLexicon.ENCODED, "UTF-8"));

                // return new PathNode(path, null, properties, Collections.<Segment>emptyList());
                return new PathNode(null, path.getParent(), path.getLastSegment(), properties, Collections.<Segment>emptyList());
            }

            File file = fileFor(path);
            if (file == null) return null;

            if (file.isDirectory()) {
                String[] childNames = file.list(source.filenameFilter(true));
                Arrays.sort(childNames);

                List<Segment> childSegments = new ArrayList<Segment>(childNames.length);
                for (String childName : childNames) {
                    childSegments.add(pathFactory.createSegment(childName));
                }

                Collection<Property> customProps = customPropertiesFactory.getDirectoryProperties(context, location, file);
                for (Property customProp : customProps) {
                    properties.put(customProp.getName(), customProp);
                }

                if (path.isRoot()) {
                    nodeType = ModeShapeLexicon.ROOT;
                    properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT));
                    // return new DefaultPathNode(path, source.getRootNodeUuidObject(), properties, childSegments);
                    return new PathNode(source.getRootNodeUuidObject(), path.getParent(), path.getLastSegment(), properties,
                                        childSegments);

                }
                Property primaryType = properties.get(JcrLexicon.PRIMARY_TYPE);
                if (primaryType == null) {
                    nodeType = JcrNtLexicon.FOLDER;
                    properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER));
                } else {
                    nodeType = nameValueFor(primaryType);
                }
                // return new DefaultPathNode(path, source.getRootNodeUuidObject(), properties, childSegments);
                return new PathNode(null, path.getParent(), path.getLastSegment(), properties, childSegments);

            }

            Collection<Property> customProps = customPropertiesFactory.getFileProperties(context, location, file);
            for (Property customProp : customProps) {
                properties.put(customProp.getName(), customProp);
            }

            Property primaryType = properties.get(JcrLexicon.PRIMARY_TYPE);
            if (primaryType == null) {
                nodeType = JcrNtLexicon.FILE;
                properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE));
            } else {
                nodeType = nameValueFor(primaryType);
            }
            if (!properties.containsKey(JcrLexicon.CREATED)) {
                properties.put(JcrLexicon.CREATED, factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));
            }

            // node = new DefaultPathNode(path, null, properties,
            // Collections.singletonList(pathFactory.createSegment(JcrLexicon.CONTENT)));

            return new PathNode(null, path.getParent(), path.getLastSegment(), properties,
                                Collections.singletonList(pathFactory.createSegment(JcrLexicon.CONTENT)));
        } finally {
            if (nodeType != null && logger.isTraceEnabled()) {
                long stopTime = System.nanoTime();
                long ms = TimeUnit.MICROSECONDS.convert(stopTime - startTime, TimeUnit.NANOSECONDS);
                String pathStr = stringFactory.create(path);
                String typeStr = stringFactory.create(nodeType);
                logger.trace("Loaded '{0}' node '{1}' in {2}microsec", typeStr, pathStr, ms);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.PathWorkspace#verifyNodeExists(org.modeshape.graph.property.Path)
     */
    @Override
    public Location verifyNodeExists( Path path ) {
        File file = fileFor(path, true);
        return file != null ? Location.create(path) : null;
    }

    /**
     * Create the Binary object used as the value for the "jcr:data" property where the file's content is stored on a
     * "nt:resource" node.
     * 
     * @param file the file
     * @return the binary representation
     */
    protected Binary binaryForContent( File file ) {
        if (file == null) return null;
        if (eagerLoading) {
            BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
            return binaryFactory.create(file);
        }
        // Not eager, so use the non-eager binary value implementation ...
        return new FileSystemBinary(file);
    }

    /**
     * This utility files the existing {@link File} at the supplied path, and in the process will verify that the path is actually
     * valid.
     * <p>
     * Note that this connector represents a file as two nodes: a parent node with a name that matches the file and a "
     * <code>jcr:primaryType</code>" of "<code>nt:file</code>"; and a child node with the name "<code>jcr:content</code> " and a "
     * <code>jcr:primaryType</code>" of "<code>nt:resource</code>". The parent "<code>nt:file</code>" node and its properties
     * represents the file itself, whereas the child "<code>nt:resource</code>" node and its properties represent the content of
     * the file.
     * </p>
     * <p>
     * As such, this method will return the File object for paths representing both the parent "<code>nt:file</code> " and child "
     * <code>nt:resource</code>" node.
     * </p>
     * 
     * @param path
     * @return the existing {@link File file} for the path; or null if the path does not represent an existing file and a
     *         {@link PathNotFoundException} was set as the {@link Request#setError(Throwable) error} on the request
     */
    protected File fileFor( Path path ) {
        return fileFor(path, true);
    }

    /**
     * This utility files the existing {@link File} at the supplied path, and in the process will verify that the path is actually
     * valid.
     * <p>
     * Note that this connector represents a file as two nodes: a parent node with a name that matches the file and a "
     * <code>jcr:primaryType</code>" of "<code>nt:file</code>"; and a child node with the name "<code>jcr:content</code> " and a "
     * <code>jcr:primaryType</code>" of "<code>nt:resource</code>". The parent "<code>nt:file</code>" node and its properties
     * represents the file itself, whereas the child "<code>nt:resource</code>" node and its properties represent the content of
     * the file.
     * </p>
     * <p>
     * As such, this method will return the File object for paths representing both the parent "<code>nt:file</code> " and child "
     * <code>nt:resource</code>" node.
     * </p>
     * 
     * @param path
     * @param existingFilesOnly
     * @return the existing {@link File file} for the path; or null if the path does not represent an existing file and a
     *         {@link PathNotFoundException} was set as the {@link Request#setError(Throwable) error} on the request
     */
    protected File fileFor( Path path,
                            boolean existingFilesOnly ) {
        if (path == null || path.isRoot()) {
            return workspaceRoot;
        }
        // See if the path is a "jcr:content" node ...
        if (path.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            // We only want to use the parent path to find the actual file ...
            path = path.getParent();
        }
        File file = workspaceRoot;
        for (Path.Segment segment : path) {
            String localName = segment.getName().getLocalName();
            // Verify the segment is valid ...
            if (segment.getIndex() > 1) {
                I18n msg = FileSystemI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(source.getName(), msg.text(source.getName()));
            }

            String defaultNamespaceUri = context.getNamespaceRegistry().getDefaultNamespaceUri();
            if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
                I18n msg = FileSystemI18n.onlyTheDefaultNamespaceIsAllowed;
                throw new RepositorySourceException(source.getName(), msg.text(source.getName()));
            }

            // The segment should exist as a child of the file ...
            file = new File(file, localName);

            if (existingFilesOnly && (!file.canRead() || !file.exists())) {
                return null;
            }
        }
        assert file != null;
        return file;
    }

    protected void validate( PathNode node ) {
        // Don't validate the root node
        if (node.getParent() == null) return;

        Map<Name, Property> properties = node.getProperties();
        Property primaryTypeProp = properties.get(JcrLexicon.PRIMARY_TYPE);
        Name primaryType = primaryTypeProp == null ? JcrNtLexicon.FOLDER : nameFactory.create(primaryTypeProp.getFirstValue());

        if (!VALID_PRIMARY_TYPES.contains(primaryType)) {
            // Set error and return
            I18n msg = FileSystemI18n.unsupportedPrimaryType;
            NamespaceRegistry registry = context.getNamespaceRegistry();
            Path parentPath = node.getParent();
            throw new RepositorySourceException(source.getName(), msg.text(primaryType.getString(registry),
                                                                           parentPath,
                                                                           getName(),
                                                                           source.getName()));

        }

        Path nodePath = context.getValueFactories().getPathFactory().create(node.getParent(), node.getName());
        ensureValidPathLength(fileFor(nodePath, false));
    }

    protected void ensureValidPathLength( File file ) {
        ensureValidPathLength(file, 0);
    }

    /**
     * Recursively checks if any of the files in the tree rooted at {@code root} would exceed the
     * {@link FileSystemSource#getMaxPathLength() maximum path length for the processor} if their paths were {@code delta}
     * characters longer. If any files would exceed this length, a {@link RepositorySourceException} is thrown.
     * 
     * @param root the root of the tree to check; may be a file or directory but may not be null
     * @param delta the change in the length of the path to check. Used to preemptively check whether moving a file or directory
     *        to a new path would violate path length rules
     * @throws RepositorySourceException if any files in the tree rooted at {@code root} would exceed this
     *         {@link FileSystemSource#getMaxPathLength() the maximum path length for this processor}
     */
    protected void ensureValidPathLength( File root,
                                          int delta ) {
        try {
            int len = root.getCanonicalPath().length();
            if (len > source.getMaxPathLength() - delta) {
                String msg = FileSystemI18n.maxPathLengthExceeded.text(source.getMaxPathLength(),
                                                                       source.getName(),
                                                                       root.getCanonicalPath(),
                                                                       delta);
                throw new RepositorySourceException(source.getName(), msg);
            }

            if (root.isDirectory()) {
                for (File child : root.listFiles(source.filenameFilter(false))) {
                    ensureValidPathLength(child, delta);
                }

            }
        } catch (IOException ioe) {
            throw new RepositorySourceException(source.getName(), FileSystemI18n.getCanonicalPathFailed.text(), ioe);
        }
    }

    /**
     * Determine the 'extra' properties for a folder that should be stored by the CustomPropertiesFactory.
     * 
     * @param properties
     * @return the extra properties, or null if the supplied properties reference is null
     */
    protected Map<Name, Property> extraFolder( Map<Name, Property> properties ) {
        if (properties == null) return null;
        if (properties.isEmpty()) return properties;
        Map<Name, Property> extra = new HashMap<Name, Property>();
        for (Property property : properties.values()) {
            Name name = property.getName();
            if (name.equals(JcrLexicon.PRIMARY_TYPE) && primaryTypeIs(property, JcrNtLexicon.FOLDER)) continue;
            extra.put(name, property);
        }
        return extra;
    }

    /**
     * Determine the 'extra' properties for a file node that should be stored by the CustomPropertiesFactory.
     * 
     * @param properties
     * @return the extra properties, or null if the supplied properties reference is null
     */
    protected Map<Name, Property> extraFile( Map<Name, Property> properties ) {
        if (properties == null) return null;
        if (properties.isEmpty()) return properties;
        Map<Name, Property> extra = new HashMap<Name, Property>();
        for (Property property : properties.values()) {
            Name name = property.getName();
            if (name.equals(JcrLexicon.PRIMARY_TYPE) && primaryTypeIs(property, JcrNtLexicon.FILE)) continue;
            extra.put(name, property);
        }
        return extra;
    }

    /**
     * Determine the 'extra' properties for a resource node that should be stored by the CustomPropertiesFactory.
     * 
     * @param properties
     * @return the extra properties, or null if the supplied properties reference is null
     */
    protected Map<Name, Property> extraResource( Map<Name, Property> properties ) {
        if (properties == null) return null;
        if (properties.isEmpty()) return properties;
        Map<Name, Property> extra = new HashMap<Name, Property>();
        for (Property property : properties.values()) {
            Name name = property.getName();
            if (name.equals(JcrLexicon.PRIMARY_TYPE) && primaryTypeIs(property, JcrNtLexicon.RESOURCE)) continue;
            else if (name.equals(JcrLexicon.DATA)) continue;
            extra.put(name, property);
        }
        return extra;
    }

    protected boolean primaryTypeIs( Property property,
                                     Name primaryType ) {
        Name actualPrimaryType = nameValueFor(property);
        return actualPrimaryType.equals(primaryType);
    }

    protected Name nameValueFor( Property property ) {
        return nameFactory.create(property.getFirstValue());
    }
}
