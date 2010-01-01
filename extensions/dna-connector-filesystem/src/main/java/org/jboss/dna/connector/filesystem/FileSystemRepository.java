/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.filesystem;

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
import java.util.List;
import java.util.Map;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.path.AbstractWritablePathWorkspace;
import org.jboss.dna.graph.connector.path.DefaultPathNode;
import org.jboss.dna.graph.connector.path.PathNode;
import org.jboss.dna.graph.connector.path.WritablePathRepository;
import org.jboss.dna.graph.connector.path.WritablePathWorkspace;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.BinaryFactory;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.Request;

/**
 * Implementation of {@code WritablePathRepository} that provides access to an underlying file system. This repository only
 * natively supports nodes of primary types {@link JcrNtLexicon#FOLDER nt:folder}, {@link JcrNtLexicon#FILE nt:file}, and
 * {@link DnaLexicon#RESOURCE dna:resource}, although the {@link CustomPropertiesFactory} allows for the addition of mixin types
 * to any and all primary types.
 */
public class FileSystemRepository extends WritablePathRepository {
    private static final String DEFAULT_MIME_TYPE = "application/octet";

    protected final FileSystemSource source;
    private File repositoryRoot;

    public FileSystemRepository( FileSystemSource source ) {
        super(source.getName(), source.getRootNodeUuid(), source.getDefaultWorkspaceName());

        this.source = source;
        initialize();
    }

    /**
     * Creates any predefined workspaces, including the default workspace.
     */
    @Override
    protected void initialize() {
        String repositoryRootPath = source.getWorkspaceRootPath();
        String sourceName = this.getSourceName();

        if (repositoryRootPath != null) {
            this.repositoryRoot = new File(repositoryRootPath);
            if (!this.repositoryRoot.exists()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootDoesNotExist.text(repositoryRootPath,
                                                                                                     sourceName));
            }
            if (!this.repositoryRoot.isDirectory()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootIsNotDirectory.text(repositoryRootPath,
                                                                                                       sourceName));
            }
            if (!this.repositoryRoot.canRead()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootCannotBeRead.text(repositoryRootPath,
                                                                                                     sourceName));
            }
        }

        if (!this.workspaces.isEmpty()) return;

        String defaultWorkspaceName = getDefaultWorkspaceName();
        ExecutionContext context = source.getRepositoryContext().getExecutionContext();

        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            doCreateWorkspace(context, workspaceName);

        }

        if (!workspaces.containsKey(defaultWorkspaceName)) {
            doCreateWorkspace(context, defaultWorkspaceName);
        }
    }

    /**
     * Internal method that creates a workspace and adds it to the map of active workspaces without checking to see if
     * {@link FileSystemSource#isCreatingWorkspacesAllowed() the source allows creating workspaces}. This is useful when setting
     * up predefined workspaces.
     * 
     * @param context the current execution context; may not be null
     * @param name the name of the workspace to create; may not be null
     * @return the newly created workspace; never null
     */
    private WritablePathWorkspace doCreateWorkspace( ExecutionContext context,
                                                     String name ) {
        // This doesn't create the directory representing the workspace (it must already exist), but it will add
        // the workspace name to the available names ...
        File directory = getWorkspaceDirectory(name);
        FileSystemWorkspace workspace = new FileSystemWorkspace(name, context, directory);

        workspaces.putIfAbsent(name, workspace);
        return (WritablePathWorkspace)workspaces.get(name);

    }

    @Override
    protected WritablePathWorkspace createWorkspace( ExecutionContext context,
                                                     String name ) {
        if (!source.isCreatingWorkspacesAllowed()) {
            String msg = FileSystemI18n.unableToCreateWorkspaces.text(getSourceName(), name);
            throw new InvalidRequestException(msg);
        }

        return doCreateWorkspace(context, name);
    }

    /**
     * @param workspaceName the name of the workspace for which the root directory should be returned
     * @return the directory that maps to the root node in the named workspace; may be null if the directory does not exist, is a
     *         file, or cannot be read.
     */
    protected File getWorkspaceDirectory( String workspaceName ) {
        if (workspaceName == null) workspaceName = source.getDefaultWorkspaceName();

        File directory = this.repositoryRoot == null ? new File(workspaceName) : new File(repositoryRoot, workspaceName);
        if (directory.exists() && directory.isDirectory() && directory.canRead()) return directory;
        return null;
    }

    /**
     * Writable workspace implementation for file system-backed workspaces
     */
    public class FileSystemWorkspace extends AbstractWritablePathWorkspace {

        private final ExecutionContext context;
        private final File workspaceRoot;

        public FileSystemWorkspace( String name,
                                    ExecutionContext context,
                                    File workspaceRoot ) {
            super(name, source.getRootNodeUuid());
            this.workspaceRoot = workspaceRoot;
            this.context = context;
        }

        public PathNode createNode( ExecutionContext context,
                                    PathNode parentNode,
                                    Name name,
                                    Map<Name, Property> properties,
                                    NodeConflictBehavior conflictBehavior ) {
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            NamespaceRegistry registry = context.getNamespaceRegistry();
            /*
             * Get references to java.io.Files
             */

            Path parentPath = parentNode.getPath();
            File parentFile = fileFor(parentPath);

            Path newPath = pathFactory.create(parentPath, name);
            String newName = name.getString(registry);
            File newFile = new File(parentFile, newName);

            /*
             * Determine the node primary type
             */
            Property primaryTypeProp = properties.get(JcrLexicon.PRIMARY_TYPE);

            // Default primary type to nt:folder
            Name primaryType = primaryTypeProp == null ? JcrNtLexicon.FOLDER : nameFactory.create(primaryTypeProp.getFirstValue());
            CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();

            if (JcrNtLexicon.FILE.equals(primaryType)) {

                // The FILE node is represented by the existence of the file
                if (!parentFile.canWrite()) {
                    I18n msg = FileSystemI18n.parentIsReadOnly;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPath, this.getName(), getSourceName()));
                }

                try {
                    ensureValidPathLength(newFile);
                    boolean skipWrite = false;

                    if (newFile.exists()) {
                        if (conflictBehavior.equals(NodeConflictBehavior.APPEND)) {
                            I18n msg = FileSystemI18n.sameNameSiblingsAreNotAllowed;
                            throw new InvalidRequestException(msg.text(getSourceName(), newName));
                        } else if (conflictBehavior.equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
                            skipWrite = true;
                        }
                    }

                    // Don't try to write if the node conflict behavior is DO_NOT_REPLACE
                    if (!skipWrite) {
                        if (!newFile.createNewFile()) {
                            I18n msg = FileSystemI18n.fileAlreadyExists;
                            throw new RepositorySourceException(getSourceName(), msg.text(parentPath, getName(), getSourceName()));
                        }
                    }
                } catch (IOException ioe) {
                    I18n msg = FileSystemI18n.couldNotCreateFile;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPath,
                                                                                  getName(),
                                                                                  getSourceName(),
                                                                                  ioe.getMessage()), ioe);
                }

                customPropertiesFactory.recordFileProperties(context,
                                                             getSourceName(),
                                                             Location.create(newPath),
                                                             newFile,
                                                             properties);
            } else if (JcrNtLexicon.RESOURCE.equals(primaryType) || DnaLexicon.RESOURCE.equals(primaryType)) {
                if (!JcrLexicon.CONTENT.equals(name)) {
                    I18n msg = FileSystemI18n.invalidNameForResource;
                    String nodeName = name.getString();
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPath,
                                                                                  getName(),
                                                                                  getSourceName(),
                                                                                  nodeName));
                }

                if (!parentFile.isFile()) {
                    I18n msg = FileSystemI18n.invalidPathForResource;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPath, getName(), getSourceName()));
                }

                if (!parentFile.canWrite()) {
                    I18n msg = FileSystemI18n.parentIsReadOnly;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPath, getName(), getSourceName()));
                }

                boolean skipWrite = false;

                if (parentFile.exists()) {
                    if (conflictBehavior.equals(NodeConflictBehavior.APPEND)) {
                        I18n msg = FileSystemI18n.sameNameSiblingsAreNotAllowed;
                        throw new InvalidRequestException(msg.text(getSourceName(), newName));
                    } else if (conflictBehavior.equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
                        // The content node logically maps to the file contents. If there are file contents, don't replace them.
                        FileInputStream checkForContents = null;
                        try {
                            checkForContents = new FileInputStream(parentFile);
                            if (-1 != checkForContents.read()) skipWrite = true;

                        } catch (IOException ignore) {

                        } finally {
                            try {
                                if (checkForContents != null) checkForContents.close();
                            } catch (Exception ignore) {
                            }
                        }
                        skipWrite = true;
                    }
                }

                if (!skipWrite) {
                    // Copy over data into a temp file, then move it to the correct location
                    FileOutputStream fos = null;
                    try {
                        File temp = File.createTempFile("dna", null);
                        fos = new FileOutputStream(temp);

                        Property dataProp = properties.get(JcrLexicon.DATA);
                        if (dataProp == null) {
                            I18n msg = FileSystemI18n.missingRequiredProperty;
                            String dataPropName = JcrLexicon.DATA.getString();
                            throw new RepositorySourceException(getSourceName(), msg.text(parentPath,
                                                                                          getName(),
                                                                                          getSourceName(),
                                                                                          dataPropName));
                        }

                        BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                        Binary binary = binaryFactory.create(properties.get(JcrLexicon.DATA).getFirstValue());

                        IoUtil.write(binary.getStream(), fos);

                        if (!FileUtil.delete(parentFile)) {
                            I18n msg = FileSystemI18n.deleteFailed;
                            throw new RepositorySourceException(getSourceName(), msg.text(parentPath, getName(), getSourceName()));
                        }

                        if (!temp.renameTo(parentFile)) {
                            I18n msg = FileSystemI18n.couldNotUpdateData;
                            throw new RepositorySourceException(getSourceName(), msg.text(parentPath, getName(), getSourceName()));
                        }
                    } catch (IOException ioe) {
                        I18n msg = FileSystemI18n.couldNotWriteData;
                        throw new RepositorySourceException(getSourceName(), msg.text(parentPath,
                                                                                      getName(),
                                                                                      getSourceName(),
                                                                                      ioe.getMessage()), ioe);

                    } finally {
                        try {
                            if (fos != null) fos.close();
                        } catch (Exception ex) {
                        }
                    }
                }
                customPropertiesFactory.recordResourceProperties(context,
                                                                 getSourceName(),
                                                                 Location.create(parentPath),
                                                                 newFile,
                                                                 properties);

            } else if (JcrNtLexicon.FOLDER.equals(primaryType) || primaryType == null) {
                ensureValidPathLength(newFile);

                if (!newFile.mkdir()) {
                    I18n msg = FileSystemI18n.couldNotCreateFile;
                    throw new RepositorySourceException(getSourceName(),
                                                        msg.text(parentPath,
                                                                 getName(),
                                                                 getSourceName(),
                                                                 primaryType == null ? "null" : primaryType.getString(registry)));
                }
                customPropertiesFactory.recordDirectoryProperties(context,
                                                                  getSourceName(),
                                                                  Location.create(newPath),
                                                                  newFile,
                                                                  properties);

            } else {
                // Set error and return
                I18n msg = FileSystemI18n.unsupportedPrimaryType;
                throw new RepositorySourceException(getSourceName(), msg.text(primaryType.getString(registry),
                                                                              parentPath,
                                                                              getName(),
                                                                              getSourceName()));
            }
            return getNode(newPath);
        }

        public boolean removeNode( ExecutionContext context,
                                   Path nodePath ) {
            File nodeFile;

            if (!nodePath.isRoot() && JcrLexicon.CONTENT.equals(nodePath.getLastSegment().getName())) {
                nodeFile = fileFor(nodePath.getParent());
                if (!nodeFile.exists()) return false;

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(nodeFile);
                    IoUtil.write("", fos);
                } catch (IOException ioe) {
                    throw new RepositorySourceException(getSourceName(), FileSystemI18n.deleteFailed.text(nodePath,
                                                                                                          getName(),
                                                                                                          getSourceName()));
                } finally {
                    if (fos != null) try {
                        fos.close();
                    } catch (IOException ioe) {
                    }
                }
            } else {
                nodeFile = fileFor(nodePath);
                if (!nodeFile.exists()) return false;

                FileUtil.delete(nodeFile);
            }

            return true;
        }

        public PathNode removeProperties( ExecutionContext context,
                                          Path nodePath,
                                          Iterable<Name> propertyNames ) {

            PathNode targetNode = getNode(nodePath);
            if (targetNode == null) return null;
            if (source.getCustomPropertiesFactory() == null) return targetNode;

            Property primaryTypeProp = targetNode.getProperty(JcrLexicon.PRIMARY_TYPE);
            Name primaryTypeName = (Name)primaryTypeProp.getFirstValue();
            Map<Name, Property> properties = new HashMap<Name, Property>(targetNode.getProperties());

            for (Name propertyName : propertyNames) {
                properties.remove(propertyName);
            }

            CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();
            Location location = Location.create(nodePath, targetNode.getUuid());

            /*
             * You can't remove any of the protected properties that the repository provides by default, but you could
             * remove custom properties.
             */
            if (JcrNtLexicon.FILE.equals(primaryTypeName)) {
                customPropertiesFactory.recordFileProperties(context, getSourceName(), location, fileFor(nodePath), properties);
            } else if (DnaLexicon.RESOURCE.equals(primaryTypeName)) {
                File file = fileFor(nodePath.getParent());
                customPropertiesFactory.recordResourceProperties(context, getSourceName(), location, file, properties);
            } else {
                File file = fileFor(nodePath);
                customPropertiesFactory.recordDirectoryProperties(context, getSourceName(), location, file, properties);
            }

            return getNode(nodePath);
        }

        public PathNode setProperties( ExecutionContext context,
                                       Path nodePath,
                                       Iterable<Property> newProperties ) {
            PathNode targetNode = getNode(nodePath);
            if (targetNode == null) return null;
            if (source.getCustomPropertiesFactory() == null) return targetNode;

            Property primaryTypeProp = targetNode.getProperty(JcrLexicon.PRIMARY_TYPE);
            Name primaryTypeName = (Name)primaryTypeProp.getFirstValue();
            Map<Name, Property> properties = new HashMap<Name, Property>(targetNode.getProperties());

            for (Property newProperty : newProperties) {
                properties.put(newProperty.getName(), newProperty);
            }

            CustomPropertiesFactory customPropertiesFactory = source.customPropertiesFactory();
            Location location = Location.create(nodePath, targetNode.getUuid());

            /*
             * You can't remove any of the protected properties that the repository provides by default, but you could
             * remove custom properties.
             */
            if (JcrNtLexicon.FILE.equals(primaryTypeName)) {
                customPropertiesFactory.recordFileProperties(context, getSourceName(), location, fileFor(nodePath), properties);
            } else if (DnaLexicon.RESOURCE.equals(primaryTypeName)) {
                File file = fileFor(nodePath.getParent());
                customPropertiesFactory.recordResourceProperties(context, getSourceName(), location, file, properties);
            } else {
                File file = fileFor(nodePath);
                customPropertiesFactory.recordDirectoryProperties(context, getSourceName(), location, file, properties);
            }

            return getNode(nodePath);
        }

        @Override
        public PathNode moveNode( ExecutionContext context,
                                  PathNode node,
                                  Name desiredNewName,
                                  WritablePathWorkspace originalWorkspace,
                                  PathNode newParent,
                                  PathNode beforeNode ) {
            if (beforeNode != null) {
                throw new InvalidRequestException(FileSystemI18n.nodeOrderingNotSupported.text(getSourceName()));
            }
            return super.moveNode(context, node, desiredNewName, originalWorkspace, newParent, beforeNode);
        }

        public Path getLowestExistingPath( Path path ) {
            File file = workspaceRoot;
            for (Path.Segment segment : path) {
                String localName = segment.getName().getLocalName();
                // Verify the segment is valid ...
                if (segment.getIndex() > 1) {
                    break;
                }

                String defaultNamespaceUri = context.getNamespaceRegistry().getDefaultNamespaceUri();
                if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
                    break;
                }

                // The segment should exist as a child of the file ...
                file = new File(file, localName);
                if (!file.exists() || !file.canRead()) {
                    // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
                    Path lowest = path;
                    while (lowest.getLastSegment() != segment) {
                        lowest = lowest.getParent();
                    }
                    return lowest.getParent();
                }
            }
            // Shouldn't be able to get this far is path is truly invalid
            return path;
        }

        public PathNode getNode( Path path ) {
            Map<Name, Property> properties = new HashMap<Name, Property>();

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
                // Discover the mime type ...
                String mimeType = null;
                InputStream contents = null;
                try {
                    contents = new BufferedInputStream(new FileInputStream(file));
                    mimeType = mimeTypeDetector.mimeTypeOf(file.getName(), contents);
                    if (mimeType == null) mimeType = DEFAULT_MIME_TYPE;
                    properties.put(JcrLexicon.MIMETYPE, factory.create(JcrLexicon.MIMETYPE, mimeType));
                } catch (IOException e) {
                    I18n msg = FileSystemI18n.couldNotReadData;
                    throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(),
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

                // First add any custom properties ...
                Collection<Property> customProps = customPropertiesFactory.getResourceProperties(context,
                                                                                                 location,
                                                                                                 file,
                                                                                                 mimeType);
                for (Property customProp : customProps) {
                    properties.put(customProp.getName(), customProp);
                }

                // The request is to get properties of the "jcr:content" child node ...
                // ... use the dna:resource node type. This is the same as nt:resource, but is not referenceable
                // since we cannot assume that we control all access to this file and can track its movements
                properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE));
                properties.put(JcrLexicon.LAST_MODIFIED, factory.create(JcrLexicon.LAST_MODIFIED,
                                                                        dateFactory.create(file.lastModified())));
                // Don't really know the encoding, either ...
                // request.addProperty(factory.create(JcrLexicon.ENCODED, stringFactory.create("UTF-8")));

                // Now put the file's content into the "jcr:data" property ...
                BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                properties.put(JcrLexicon.DATA, factory.create(JcrLexicon.DATA, binaryFactory.create(file)));
                return new DefaultPathNode(path, null, properties, Collections.<Segment>emptyList());
            }

            File file = fileFor(path);
            if (file == null) return null;

            if (file.isDirectory()) {
                String[] childNames = file.list(source.filenameFilter());
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
                    properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.ROOT));
                    return new DefaultPathNode(path, source.getRootNodeUuid(), properties, childSegments);
                }
                properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER));
                return new DefaultPathNode(path, source.getRootNodeUuid(), properties, childSegments);
            }

            Collection<Property> customProps = customPropertiesFactory.getFileProperties(context, location, file);
            for (Property customProp : customProps) {
                properties.put(customProp.getName(), customProp);
            }
            properties.put(JcrLexicon.PRIMARY_TYPE, factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE));
            properties.put(JcrLexicon.CREATED, factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));
            return new DefaultPathNode(path, null, properties,
                                       Collections.singletonList(pathFactory.createSegment(JcrLexicon.CONTENT)));

        }

        /**
         * This utility files the existing {@link File} at the supplied path, and in the process will verify that the path is
         * actually valid.
         * <p>
         * Note that this connector represents a file as two nodes: a parent node with a name that matches the file and a "
         * <code>jcr:primaryType</code>" of "<code>nt:file</code>"; and a child node with the name "<code>jcr:content</code>
         * " and a " <code>jcr:primaryType</code>" of "<code>nt:resource</code>". The parent "<code>nt:file</code>" node and its
         * properties represents the file itself, whereas the child "<code>nt:resource</code>" node and its properties represent
         * the content of the file.
         * </p>
         * <p>
         * As such, this method will return the File object for paths representing both the parent "<code>nt:file</code>
         * " and child " <code>nt:resource</code>" node.
         * </p>
         * 
         * @param path
         * @return the existing {@link File file} for the path; or null if the path does not represent an existing file and a
         *         {@link PathNotFoundException} was set as the {@link Request#setError(Throwable) error} on the request
         */
        protected File fileFor( Path path ) {
            assert path != null;
            if (path.isRoot()) {
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
                    throw new RepositorySourceException(getSourceName(), msg.text(getSourceName()));
                }

                String defaultNamespaceUri = context.getNamespaceRegistry().getDefaultNamespaceUri();
                if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
                    I18n msg = FileSystemI18n.onlyTheDefaultNamespaceIsAllowed;
                    throw new RepositorySourceException(getSourceName(), msg.text(getSourceName()));
                }

                // The segment should exist as a child of the file ...
                file = new File(file, localName);
                if (!file.exists() || !file.canRead()) {
                    return null;
                }
            }
            assert file != null;
            return file;
        }

        protected void ensureValidPathLength( File root ) {
            ensureValidPathLength(root, 0);
        }

        /**
         * Recursively checks if any of the files in the tree rooted at {@code root} would exceed the
         * {@link FileSystemSource#getMaxPathLength() maximum path length for the processor} if their paths were {@code delta}
         * characters longer. If any files would exceed this length, a {@link RepositorySourceException} is thrown.
         * 
         * @param root the root of the tree to check; may be a file or directory but may not be null
         * @param delta the change in the length of the path to check. Used to preemptively check whether moving a file or
         *        directory to a new path would violate path length rules
         * @throws RepositorySourceException if any files in the tree rooted at {@code root} would exceed this
         *         {@link FileSystemSource#getMaxPathLength() the maximum path length for this processor}
         */
        protected void ensureValidPathLength( File root,
                                              int delta ) {
            try {
                int len = root.getCanonicalPath().length();
                if (len > source.getMaxPathLength() - delta) {
                    String msg = FileSystemI18n.maxPathLengthExceeded.text(source.getMaxPathLength(),
                                                                           getSourceName(),
                                                                           root.getCanonicalPath(),
                                                                           delta);
                    throw new RepositorySourceException(getSourceName(), msg);
                }

                if (root.isDirectory()) {
                    for (File child : root.listFiles(source.filenameFilter())) {
                        ensureValidPathLength(child, delta);
                    }

                }
            } catch (IOException ioe) {
                throw new RepositorySourceException(getSourceName(), FileSystemI18n.getCanonicalPathFailed.text(), ioe);
            }
        }

    }
}
