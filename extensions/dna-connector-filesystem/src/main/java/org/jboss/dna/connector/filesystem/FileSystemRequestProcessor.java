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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.BinaryFactory;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.RenameNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * The {@link RequestProcessor} implementation for the file systme connector. This is the class that does the bulk of the work in
 * the file system connector, since it processes all requests.
 * 
 * @author Randall Hauch
 */
public class FileSystemRequestProcessor extends RequestProcessor {

    private static final String DEFAULT_MIME_TYPE = "application/octet";

    private final String defaultNamespaceUri;
    private final Map<String, File> availableWorkspaces;
    private final boolean creatingWorkspacesAllowed;
    private final String defaultWorkspaceName;
    private final File workspaceRootPath;
    private final FilenameFilter filenameFilter;
    private final boolean updatesAllowed;
    private final MimeTypeDetector mimeTypeDetector;
    private final UUID rootNodeUuid;

    /**
     * @param sourceName
     * @param defaultWorkspaceName
     * @param availableWorkspaces
     * @param creatingWorkspacesAllowed
     * @param context
     * @param rootNodeUuid the UUID for the root node in this workspace; may be null. If not specified, a random UUID will be
     *        generated each time that the repository is started.
     * @param workspaceRootPath the path to the workspace root directory; may be null. If specified, all workspace names will be
     *        treated as relative paths from this directory.
     * @param filenameFilter the filename filter to use to restrict the allowable nodes, or null if all files/directories are to
     *        be exposed by this connector
     * @param updatesAllowed true if this connector supports updating the file system, or false if the connector is readonly
     */
    protected FileSystemRequestProcessor( String sourceName,
                                          String defaultWorkspaceName,
                                          Map<String, File> availableWorkspaces,
                                          boolean creatingWorkspacesAllowed,
                                          UUID rootNodeUuid,
                                          String workspaceRootPath,
                                          ExecutionContext context,
                                          FilenameFilter filenameFilter,
                                          boolean updatesAllowed ) {
        super(sourceName, context, null);
        assert defaultWorkspaceName != null;
        assert availableWorkspaces != null;
        assert rootNodeUuid != null;
        this.availableWorkspaces = availableWorkspaces;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        this.defaultNamespaceUri = getExecutionContext().getNamespaceRegistry().getDefaultNamespaceUri();
        this.rootNodeUuid = rootNodeUuid;
        this.filenameFilter = filenameFilter;
        this.defaultWorkspaceName = defaultWorkspaceName;
        this.updatesAllowed = updatesAllowed;
        this.mimeTypeDetector = context.getMimeTypeDetector();

        if (workspaceRootPath != null) {
            this.workspaceRootPath = new File(workspaceRootPath);
            if (!this.workspaceRootPath.exists()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootDoesNotExist.text(workspaceRootPath,
                                                                                                     sourceName));
            }
            if (!this.workspaceRootPath.isDirectory()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootIsNotDirectory.text(workspaceRootPath,
                                                                                                       sourceName));
            }
            if (!this.workspaceRootPath.canRead()) {
                throw new IllegalStateException(FileSystemI18n.pathForWorkspaceRootCannotBeRead.text(workspaceRootPath,
                                                                                                     sourceName));
            }
        } else {
            this.workspaceRootPath = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {

        // Get the java.io.File object that represents the workspace ...
        File workspaceRoot = getWorkspaceDirectory(request.inWorkspace());
        if (workspaceRoot == null) {
            request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(request.inWorkspace())));
            return;
        }

        // Find the existing file for the parent ...
        Location location = request.of();
        Path parentPath = getPathFor(location, request);

        if (parentPath.isRoot()) {
            if (!location.hasPath()) location = location.with(parentPath);
        }

        File parent = getExistingFileFor(workspaceRoot, parentPath, location, request);
        if (parent == null) {
            // An error was set on the request
            assert request.hasError();
            return;
        }
        // Decide how to represent the children ...
        if (parent.isDirectory()) {
            // Create a Location for each file and directory contained by the parent directory ...
            PathFactory pathFactory = pathFactory();
            NameFactory nameFactory = nameFactory();
            for (String localName : parent.list(filenameFilter)) {
                Name childName = nameFactory.create(defaultNamespaceUri, localName);
                Path childPath = pathFactory.create(parentPath, childName);
                request.addChild(Location.create(childPath));
            }
        } else {
            // The parent is a java.io.File, and the path may refer to the node that is either the "nt:file" parent
            // node, or the child "jcr:content" node...
            if (!parentPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                // This node represents the "nt:file" parent node, so the only child is the "jcr:content" node ...
                Path contentPath = pathFactory().create(parentPath, JcrLexicon.CONTENT);
                Location content = Location.create(contentPath);
                request.addChild(content);
            }
            // otherwise, the path ends in "jcr:content", and there are no children
        }
        request.setActualLocationOfNode(location);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {

        // Get the java.io.File object that represents the workspace ...
        File workspaceRoot = getWorkspaceDirectory(request.inWorkspace());
        if (workspaceRoot == null) {
            request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(request.inWorkspace())));
            return;
        }

        PropertyFactory factory = getExecutionContext().getPropertyFactory();

        // Find the existing file for the parent ...
        Location location = request.at();
        Path path = getPathFor(location, request);
        if (path.isRoot()) {
            // Root nodes can be requested by UUID, path, or both
            if (!location.hasPath()) location = location.with(path);
            if (location.getUuid() == null) location = location.with(rootNodeUuid);

            request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.ROOT));
            request.setActualLocationOfNode(location);
            setCacheableInfo(request);

            return;
        }

        File file = getExistingFileFor(workspaceRoot, path, location, request);
        if (file == null) {
            // An error was set on the request
            assert request.hasError();
            return;
        }
        // Generate the properties for this File object ...
        DateTimeFactory dateFactory = getExecutionContext().getValueFactories().getDateFactory();
        // Note that we don't have 'created' timestamps, just last modified, so we'll have to use them
        if (file.isDirectory()) {
            // Add properties for the directory ...
            request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER));
            request.addProperty(factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));

        } else {
            // It is a file, but ...
            if (path.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                // The request is to get properties of the "jcr:content" child node ...
                // ... use the dna:resource node type. This is the same as nt:resource, but is not referenceable
                // since we cannot assume that we control all access to this file and can track its movements
                request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE));
                request.addProperty(factory.create(JcrLexicon.LAST_MODIFIED, dateFactory.create(file.lastModified())));
                // Don't really know the encoding, either ...
                // request.addProperty(factory.create(JcrLexicon.ENCODED, stringFactory.create("UTF-8")));

                // Discover the mime type ...
                String mimeType = null;
                InputStream contents = null;
                boolean mimeTypeError = false;
                try {
                    contents = new BufferedInputStream(new FileInputStream(file));
                    mimeType = mimeTypeDetector.mimeTypeOf(file.getName(), contents);
                    if (mimeType == null) mimeType = DEFAULT_MIME_TYPE;
                    request.addProperty(factory.create(JcrLexicon.MIMETYPE, mimeType));
                } catch (IOException e) {
                    mimeTypeError = true;
                    request.setError(e);
                } finally {
                    if (contents != null) {
                        try {
                            contents.close();
                        } catch (IOException e) {
                            if (!mimeTypeError) request.setError(e);
                        }
                    }
                }

                // Now put the file's content into the "jcr:data" property ...
                BinaryFactory binaryFactory = getExecutionContext().getValueFactories().getBinaryFactory();
                request.addProperty(factory.create(JcrLexicon.DATA, binaryFactory.create(file)));

            } else {
                // The request is to get properties for the node representing the file
                request.addProperty(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE));
                request.addProperty(factory.create(JcrLexicon.CREATED, dateFactory.create(file.lastModified())));
            }

        }
        request.setActualLocationOfNode(location);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        if (updatesAllowed(request)) super.process(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        // If the request contains a null name, then we use the default ...
        String workspaceName = request.workspaceName();
        if (workspaceName == null) workspaceName = defaultWorkspaceName;

        if (!this.creatingWorkspacesAllowed) {
            // Then the workspace name must be one of the available names ...

            boolean found = false;
            for (String available : this.availableWorkspaces.keySet()) {
                if (workspaceName.equals(available)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(workspaceName)));
                return;
            }
            // We know it is an available workspace, so just continue ...
        }
        // Verify that there is a directory at the path given by the workspace name ...
        File directory = availableWorkspaces.get(workspaceName);
        if (directory.exists() && directory.isDirectory() && directory.canRead()) {
            request.setActualWorkspaceName(workspaceName);
            request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
        } else {
            request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(workspaceName)));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        // Return the set of available workspace names, even if new workspaces can be created ...
        Set<String> names = new HashSet<String>();
        for (Map.Entry<String, File> entry : this.availableWorkspaces.entrySet()) {
            File directory = entry.getValue();
            if (directory.exists() && directory.isDirectory() && directory.canRead()) {
                names.add(entry.getKey());
            }
        }

        request.setAvailableWorkspaceNames(Collections.unmodifiableSet(names));
    }

    /**
     * Utility method to return the canonical path (without "." and ".." segments) for a file.
     * 
     * @param directory the directory; may not be null
     * @return the canonical path, or if there is an error the absolute path
     */
    protected String getCanonicalWorkspaceName( File directory ) {
        try {
            if (this.workspaceRootPath != null) {
                String directoryCanonicalPath = directory.getCanonicalPath();
                String rootCanonicalPath = workspaceRootPath.getCanonicalPath();
                assert directoryCanonicalPath.startsWith(rootCanonicalPath);
                return directoryCanonicalPath.substring(rootCanonicalPath.length() + 1);
            }
            return directory.getCanonicalPath();
        } catch (IOException e) {
            return directory.getAbsolutePath();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        updatesAllowed(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        final String workspaceName = request.desiredNameOfNewWorkspace();
        if (!creatingWorkspacesAllowed) {
            String msg = FileSystemI18n.unableToCreateWorkspaces.text(getSourceName(), workspaceName);
            request.setError(new InvalidRequestException(msg));
            return;
        }
        // This doesn't create the directory representing the workspace (it must already exist), but it will add
        // the workspace name to the available names ...
        File directory = new File(workspaceName);
        if (directory.exists() && directory.isDirectory() && directory.canRead()) {
            request.setActualWorkspaceName(getCanonicalWorkspaceName(directory));
            request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
            availableWorkspaces.put(workspaceName, directory);
            recordChange(request);
        } else {
            request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(workspaceName)));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        final String workspaceName = request.workspaceName();
        if (!creatingWorkspacesAllowed) {
            String msg = FileSystemI18n.unableToCreateWorkspaces.text(getSourceName(), workspaceName);
            request.setError(new InvalidRequestException(msg));
        }
        // This doesn't delete the file/directory; rather, it just remove the workspace from the available set ...
        if (this.availableWorkspaces.remove(workspaceName) == null) {
            request.setError(new InvalidWorkspaceException(FileSystemI18n.workspaceDoesNotExist.text(workspaceName)));
        } else {
            request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
            recordChange(request);
        }
    }

    protected boolean updatesAllowed( Request request ) {
        if (!updatesAllowed) {
            request.setError(new InvalidRequestException(FileSystemI18n.sourceIsReadOnly.text(getSourceName())));
        }
        return !request.hasError();
    }

    protected NameFactory nameFactory() {
        return getExecutionContext().getValueFactories().getNameFactory();
    }

    protected PathFactory pathFactory() {
        return getExecutionContext().getValueFactories().getPathFactory();
    }

    protected Path getPathFor( Location location,
                               Request request ) {
        Path path = location.getPath();

        if (location.getUuid() != null && rootNodeUuid.equals(location.getUuid())) {
            return pathFactory().createRootPath();
        }

        if (path == null) {
            I18n msg = FileSystemI18n.locationInRequestMustHavePath;
            throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
        }
        return path;
    }

    protected File getWorkspaceDirectory( String workspaceName ) {
        if (workspaceName == null) workspaceName = defaultWorkspaceName;

        File directory = this.workspaceRootPath == null ? new File(workspaceName) : new File(workspaceRootPath, workspaceName);
        if (directory.exists() && directory.isDirectory() && directory.canRead()) return directory;
        return null;
    }

    /**
     * This utility files the existing {@link File} at the supplied path, and in the process will verify that the path is actually
     * valid.
     * <p>
     * Note that this connector represents a file as two nodes: a parent node with a name that matches the file and a "
     * <code>jcr:primaryType</code>" of "<code>nt:file</code>"; and a child node with the name "<code>jcr:content</code>" and a "
     * <code>jcr:primaryType</code>" of "<code>nt:resource</code>". The parent "<code>nt:file</code>" node and its properties
     * represents the file itself, whereas the child "<code>nt:resource</code>" node and its properties represent the content of
     * the file.
     * </p>
     * <p>
     * As such, this method will return the File object for paths representing both the parent "<code>nt:file</code>" and child "
     * <code>nt:resource</code>" node.
     * </p>
     * 
     * @param workspaceRoot
     * @param path
     * @param location the location containing the path; may not be null
     * @param request the request containing the path (and the location); may not be null
     * @return the existing {@link File file} for the path; or null if the path does not represent an existing file and a
     *         {@link PathNotFoundException} was set as the {@link Request#setError(Throwable) error} on the request
     */
    protected File getExistingFileFor( File workspaceRoot,
                                       Path path,
                                       Location location,
                                       Request request ) {
        assert path != null;
        assert location != null;
        assert request != null;
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
                throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            }
            if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
                I18n msg = FileSystemI18n.onlyTheDefaultNamespaceIsAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            }
            // The segment should exist as a child of the file ...
            file = new File(file, localName);
            if (!file.exists() || !file.canRead()) {
                // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
                Path lowest = path;
                while (lowest.getLastSegment() != segment) {
                    lowest = lowest.getParent();
                }
                lowest = lowest.getParent();
                request.setError(new PathNotFoundException(location, lowest));
                return null;
            }
        }
        assert file != null;
        return file;
    }
}
