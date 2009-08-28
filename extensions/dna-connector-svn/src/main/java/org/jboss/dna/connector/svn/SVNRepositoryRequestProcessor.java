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
package org.jboss.dna.connector.svn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.scm.ScmAction;
import org.jboss.dna.connector.scm.ScmActionFactory;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.BinaryFactory;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactory;
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
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.RenameNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

/**
 * The {@link RequestProcessor} implementation for the file subversion repository connector. This is the class that does the bulk
 * of the work in the subversion repository connector, since it processes all requests.
 * 
 * @author Serge Pagop
 */
public class SVNRepositoryRequestProcessor extends RequestProcessor implements ScmActionFactory {

    protected static final String BACK_SLASH = "/";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final String defaultNamespaceUri;
    private final boolean updatesAllowed;
    private SVNRepository defaultWorkspace;
    protected final Logger logger;
    private final Set<String> availableWorkspaceNames;
    private final boolean creatingWorkspacesAllowed;
    private final RepositoryAccessData accessData;

    /**
     * @param sourceName
     * @param context
     * @param defaultWorkspace
     * @param availableWorkspaceNames
     * @param creatingWorkspacesAllowed
     * @param updatesAllowed true if this connector supports updating the subversion repository, or false if the connector is read
     *        only
     * @param accessData
     */
    protected SVNRepositoryRequestProcessor( String sourceName,
                                             SVNRepository defaultWorkspace,
                                             Set<String> availableWorkspaceNames,
                                             boolean creatingWorkspacesAllowed,
                                             ExecutionContext context,
                                             boolean updatesAllowed,
                                             RepositoryAccessData accessData ) {
        super(sourceName, context, null);
        assert defaultWorkspace != null;
        assert availableWorkspaceNames != null;
        this.defaultNamespaceUri = getExecutionContext().getNamespaceRegistry().getDefaultNamespaceUri();
        this.updatesAllowed = updatesAllowed;
        this.defaultWorkspace = defaultWorkspace;
        this.logger = getExecutionContext().getLogger(getClass());
        this.availableWorkspaceNames = availableWorkspaceNames;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        this.accessData = accessData;
    }

    protected void addProperty( List<Property> properties,
                                PropertyFactory factory,
                                Name propertyName,
                                Object value ) {
        if (value != null) {
            properties.add(factory.create(propertyName, value));
        }
    }

    protected boolean readNode( String workspaceName,
                                Location myLocation,
                                List<Property> properties,
                                List<Location> children,
                                Request request ) {

        // Get the SVNRepository object that represents the workspace ...
        SVNRepository workspaceRoot = getWorkspaceDirectory(workspaceName);
        if (workspaceRoot == null) {
            request.setError(new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(workspaceName)));
            return false;
        }
        Path requestedPath = getPathFor(myLocation, request);
        checkThePath(requestedPath, request); // same-name-sibling indexes are not supported

        if (requestedPath.isRoot()) {
            // workspace root must be a directory
            if (children != null) {
                final Collection<SVNDirEntry> entries = SVNRepositoryUtil.getDir(workspaceRoot, "");
                for (SVNDirEntry entry : entries) {
                    // All of the children of a directory will be another directory or a file, but never a "jcr:content" node ...
                    String localName = entry.getName();
                    Name childName = nameFactory().create(defaultNamespaceUri, localName);
                    Path childPath = pathFactory().create(requestedPath, childName);
                    children.add(Location.create(childPath));
                }
            }
            // There are no properties on the root ...
        } else {
            try {
                // Generate the properties for this File object ...
                PropertyFactory factory = getExecutionContext().getPropertyFactory();
                DateTimeFactory dateFactory = getExecutionContext().getValueFactories().getDateFactory();

                // Figure out the kind of node this represents ...
                SVNNodeKind kind = getNodeKind(workspaceRoot, requestedPath, accessData.getRepositoryRootUrl(), workspaceName);
                if (kind == SVNNodeKind.DIR) {
                    String directoryPath = getPathAsString(requestedPath);
                    if (!accessData.getRepositoryRootUrl().equals(workspaceName)) {
                        directoryPath = directoryPath.substring(1);
                    }
                    if (children != null) {
                        // Decide how to represent the children ...
                        Collection<SVNDirEntry> dirEntries = SVNRepositoryUtil.getDir(workspaceRoot, directoryPath);
                        for (SVNDirEntry entry : dirEntries) {
                            // All of the children of a directory will be another directory or a file,
                            // but never a "jcr:content" node ...
                            String localName = entry.getName();
                            Name childName = nameFactory().create(defaultNamespaceUri, localName);
                            Path childPath = pathFactory().create(requestedPath, childName);
                            children.add(Location.create(childPath));
                        }
                    }
                    if (properties != null) {
                        // Load the properties for this directory ......
                        addProperty(properties, factory, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER);
                        SVNDirEntry entry = getEntryInfo(workspaceRoot, directoryPath);
                        if (entry != null) {
                            addProperty(properties, factory, JcrLexicon.LAST_MODIFIED, dateFactory.create(entry.getDate()));
                        }
                    }
                } else {
                    // It's not a directory, so must be a file; the only child of an nt:file is the "jcr:content" node
                    // ...
                    if (requestedPath.endsWith(JcrLexicon.CONTENT)) {
                        // There are never any children of these nodes, just properties ...
                        if (properties != null) {
                            String contentPath = getPathAsString(requestedPath.getParent());
                            if (!accessData.getRepositoryRootUrl().equals(workspaceName)) {
                                contentPath = contentPath.substring(1);
                            }
                            SVNDirEntry entry = getEntryInfo(workspaceRoot, contentPath);
                            if (entry != null) {
                                // The request is to get properties of the "jcr:content" child node ...
                                addProperty(properties, factory, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE);
                                addProperty(properties, factory, JcrLexicon.LAST_MODIFIED, dateFactory.create(entry.getDate()));
                            }

                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            SVNProperties fileProperties = new SVNProperties();
                            getData(contentPath, fileProperties, os);
                            String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
                            if (mimeType == null) mimeType = DEFAULT_MIME_TYPE;
                            addProperty(properties, factory, JcrLexicon.MIMETYPE, mimeType);

                            if (os.toByteArray().length > 0) {
                                // Now put the file's content into the "jcr:data" property ...
                                BinaryFactory binaryFactory = getExecutionContext().getValueFactories().getBinaryFactory();
                                addProperty(properties, factory, JcrLexicon.DATA, binaryFactory.create(os.toByteArray()));
                            }
                        }
                    } else {
                        // Determine the corresponding file path for this object ...
                        String filePath = getPathAsString(requestedPath);
                        if (!accessData.getRepositoryRootUrl().equals(workspaceName)) {
                            filePath = filePath.substring(1);
                        }
                        if (children != null) {
                            // Not a "jcr:content" child node but rather an nt:file node, so add the child ...
                            Path contentPath = pathFactory().create(requestedPath, JcrLexicon.CONTENT);
                            children.add(Location.create(contentPath));
                        }
                        if (properties != null) {
                            // Now add the properties to "nt:file" ...
                            addProperty(properties, factory, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE);
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            SVNProperties fileProperties = new SVNProperties();
                            getData(filePath, fileProperties, os);
                            String created = fileProperties.getStringValue(SVNProperty.COMMITTED_DATE);
                            addProperty(properties, factory, JcrLexicon.CREATED, dateFactory.create(created));
                        }
                    }
                }
            } catch (SVNException e) {
                request.setError(e);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        logger.trace(request.toString());
        List<Location> children = new LinkedList<Location>();
        List<Property> properties = new LinkedList<Property>();
        if (readNode(request.inWorkspace(), request.at(), properties, children, request)) {
            request.addChildren(children);
            request.addProperties(properties);
            request.setActualLocationOfNode(request.at());
            setCacheableInfo(request);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        logger.trace(request.toString());
        List<Location> children = new LinkedList<Location>();
        if (readNode(request.inWorkspace(), request.of(), null, children, request)) {
            request.addChildren(children);
            request.setActualLocationOfNode(request.of());
            setCacheableInfo(request);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        logger.trace(request.toString());
        List<Property> properties = new LinkedList<Property>();
        if (readNode(request.inWorkspace(), request.at(), properties, null, request)) {
            request.addProperties(properties);
            request.setActualLocationOfNode(request.at());
            setCacheableInfo(request);
        }
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
        logger.trace(request.toString());
        verifyUpdatesAllowed();
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
        if (workspaceName == null) workspaceName = defaultWorkspace.getLocation().toDecodedString();

        SVNRepository repository = null;
        if (!this.creatingWorkspacesAllowed) {
            // Then the workspace name must be one of the available names ...
            boolean found = false;
            for (String available : this.availableWorkspaceNames) {
                if (workspaceName.equals(available)) {
                    found = true;
                    break;
                }
                repository = SVNRepositoryUtil.createRepository(available, accessData.getUsername(), accessData.getPassword());
                // check if the workspace is conform
                if (SVNRepositoryUtil.isDirectory(repository, "")
                    && repository.getLocation().toDecodedString().equals(workspaceName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                request.setError(new InvalidWorkspaceException(
                                                               SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(workspaceName)));
                return;
            }
        }

        // Verify that there is a repos at the path given by the workspace name ...
        repository = SVNRepositoryUtil.createRepository(workspaceName, accessData.getUsername(), accessData.getPassword());
        if (SVNRepositoryUtil.isDirectory(repository, "")) {
            request.setActualWorkspaceName(repository.getLocation().toDecodedString());
            request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
        } else {
            request.setError(new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(workspaceName)));
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
        for (String name : this.availableWorkspaceNames) {
            SVNRepository repos = SVNRepositoryUtil.createRepository(name, accessData.getUsername(), accessData.getPassword());
            if (repos != null && SVNRepositoryUtil.isDirectory(repos, "")) {
                names.add(repos.getLocation().toDecodedString());
            } else {
                request.setError(new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(name)));
            }
        }
        request.setAvailableWorkspaceNames(Collections.unmodifiableSet(names));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        if (!updatesAllowed) {
            request.setError(new InvalidRequestException(
                                                         SVNRepositoryConnectorI18n.sourceDoesNotSupportCloningWorkspaces.text(getSourceName())));
        }
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
            String msg = SVNRepositoryConnectorI18n.unableToCreateWorkspaces.text(getSourceName(), workspaceName);
            request.setError(new InvalidRequestException(msg));
            return;
        }
        // This doesn't create the directory representing the workspace (it must already exist), but it will add
        // the workspace name to the available names ...
        SVNRepository repository = SVNRepositoryUtil.createRepository(workspaceName,
                                                                      accessData.getUsername(),
                                                                      accessData.getPassword());
        if (SVNRepositoryUtil.isDirectory(repository, "")) {
            request.setActualWorkspaceName(repository.getLocation().toDecodedString());
            request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
            availableWorkspaceNames.add(repository.getLocation().toDecodedString());
        } else {
            request.setError(new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(workspaceName)));
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
            String msg = SVNRepositoryConnectorI18n.unableToCreateWorkspaces.text(getSourceName(), workspaceName);
            request.setError(new InvalidRequestException(msg));
        }
        // This doesn't delete the file/directory; rather, it just remove the workspace from the available set ...
        if (!this.availableWorkspaceNames.remove(workspaceName)) {
            request.setError(new InvalidWorkspaceException(SVNRepositoryConnectorI18n.workspaceDoesNotExist.text(workspaceName)));
        }
    }

    /**
     * Verify if change is allowed on a specific source.
     * 
     * @throws RepositorySourceException if change on that repository source is not allowed.
     */
    protected void verifyUpdatesAllowed() {
        if (!updatesAllowed) {
            throw new InvalidRequestException(SVNRepositoryConnectorI18n.sourceIsReadOnly.text(getSourceName()));
        }
    }

    protected boolean updatesAllowed( Request request ) {
        if (!updatesAllowed) {
            request.setError(new InvalidRequestException(SVNRepositoryConnectorI18n.sourceIsReadOnly.text(getSourceName())));
        }
        return !request.hasError();
    }

    /**
     * Factory for sample name.
     * 
     * @return the name factory
     */
    protected NameFactory nameFactory() {
        return getExecutionContext().getValueFactories().getNameFactory();
    }

    /**
     * Factory for path creation.
     * 
     * @return a path factory.
     */
    protected PathFactory pathFactory() {
        return getExecutionContext().getValueFactories().getPathFactory();
    }

    /**
     * Factory for property creation.
     * 
     * @return the property factory.
     */
    protected PropertyFactory propertyFactory() {
        return getExecutionContext().getPropertyFactory();
    }

    /**
     * Factory for date creation.
     * 
     * @return the date factory.
     */
    protected DateTimeFactory dateFactory() {
        return getExecutionContext().getValueFactories().getDateFactory();
    }

    /**
     * Factory for binary creation.
     * 
     * @return the binary factory..
     */
    protected ValueFactory<Binary> binaryFactory() {
        return getExecutionContext().getValueFactories().getBinaryFactory();
    }

    /**
     * Get the path for a locarion and check if the path is null or not.
     * 
     * @param location - the location.
     * @param request - the requested path.
     * @return the path.
     * @throws RepositorySourceException if the path of a location is null.
     */
    protected Path getPathFor( Location location,
                               Request request ) {
        Path path = location.getPath();
        if (path == null) {
            I18n msg = SVNRepositoryConnectorI18n.locationInRequestMustHavePath;
            throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
        }
        return path;
    }

    /**
     * Get the content of a file.
     * 
     * @param path - the path to that file.
     * @param properties - the properties of the file.
     * @param os - the output stream where to store the content.
     * @throws SVNException - throws if such path is not at that revision or in case of a connection problem.
     */
    protected void getData( String path,
                            SVNProperties properties,
                            OutputStream os ) throws SVNException {
        getDefaultWorkspace().getFile(path, -1, properties, os);

    }

    /**
     * Get the repository driver.
     * 
     * @return repository
     */
    public SVNRepository getDefaultWorkspace() {
        return defaultWorkspace;
    }

    /**
     * Validate the kind of node and throws an exception if necessary.
     * 
     * @param repos
     * @param requestedPath
     * @return the kind.
     */
    protected SVNNodeKind validateNodeKind( SVNRepository repos,
                                            Path requestedPath ) {
        SVNNodeKind kind;
        String myPath;
        if (getPathAsString(requestedPath).trim().equals("/")) {
            myPath = getPathAsString(requestedPath);
        } else if (requestedPath.endsWith(JcrLexicon.CONTENT)) {
            myPath = getPathAsString(requestedPath.getParent());
        } else {
            // directory and file
            myPath = getPathAsString(requestedPath);
        }

        try {

            kind = repos.checkPath(myPath, -1);
            if (kind == SVNNodeKind.NONE) {
                // node does not exist or requested node is not correct.
                throw new PathNotFoundException(Location.create(requestedPath), null,
                                                SVNRepositoryConnectorI18n.nodeDoesNotExist.text(myPath));
            } else if (kind == SVNNodeKind.UNKNOWN) {
                // node is unknown
                throw new PathNotFoundException(Location.create(requestedPath), null,
                                                SVNRepositoryConnectorI18n.nodeIsActuallyUnknow.text(myPath));
            }
        } catch (SVNException e) {
            throw new RepositorySourceException(
                                                getSourceName(),
                                                SVNRepositoryConnectorI18n.connectingFailureOrUserAuthenticationProblem.text(getSourceName()));
        }

        return kind;
    }

    private String getPathAsString( Path path ) {
        return path.getString(getExecutionContext().getNamespaceRegistry());
    }

    /**
     * Get some important informations of a path
     * 
     * @param repos
     * @param path - the path
     * @return - the {@link SVNDirEntry}, or null if there is no such entry
     */
    protected SVNDirEntry getEntryInfo( SVNRepository repos,
                                        String path ) {
        assert path != null;
        SVNDirEntry entry = null;
        try {
            entry = repos.info(path, -1);
        } catch (SVNException e) {
            throw new RepositorySourceException(
                                                getSourceName(),
                                                SVNRepositoryConnectorI18n.connectingFailureOrUserAuthenticationProblem.text(getSourceName()));
        }
        return entry;
    }

    /**
     * Open the directories where change has to be made.
     * 
     * @param editor - abstract editor.
     * @param rootPath - the pa to open.
     * @throws SVNException when a error occur.
     */
    protected static void openDirectories( ISVNEditor editor,
                                           String rootPath ) throws SVNException {
        assert rootPath != null;
        int pos = rootPath.indexOf('/', 0);
        while (pos != -1) {
            String dir = rootPath.substring(0, pos);
            editor.openDir(dir, -1);
            pos = rootPath.indexOf('/', pos + 1);
        }
        String dir = rootPath.substring(0, rootPath.length());
        editor.openDir(dir, -1);
    }

    /**
     * Close the directories where change was made.
     * 
     * @param editor - the abstract editor.
     * @param path - the directories to open.
     * @throws SVNException when a error occur.
     */
    protected static void closeDirectories( ISVNEditor editor,
                                            String path ) throws SVNException {
        int length = path.length() - 1;
        int pos = path.lastIndexOf('/', length);
        editor.closeDir();
        while (pos != -1) {
            editor.closeDir();
            pos = path.lastIndexOf('/', pos - 1);
        }
    }

    /**
     * Get the last revision.
     * 
     * @param repos
     * @return the last revision number.
     * @throws Exception
     */
    public long getLatestRevision( SVNRepository repos ) throws Exception {
        try {
            return repos.getLatestRevision();
        } catch (SVNException e) {
            e.printStackTrace();
            // logger.error( "svn error: " );
            throw e;
        }
    }

    /**
     * Add directory in a repository
     * 
     * @param repository - the repository.
     * @param root - the root path has to exist.
     * @param child - new path to be added.
     * @param message - information about the change action.
     * @throws SVNException when a error occur.
     */
    protected void addDirEntry( SVNRepository repository,
                                String root,
                                String child,
                                String message ) throws SVNException {
        assert root.trim().length() != 0;
        SVNNodeKind rootKind = repository.checkPath(root, -1);
        if (rootKind == SVNNodeKind.UNKNOWN) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "path with name '{0}' is unknown in the repository",
                                                         root);
            throw new SVNException(err);
        } else if (rootKind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "path with name '{0}' is missing in the repository",
                                                         root);
            throw new SVNException(err);
        } else if (rootKind == SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "path with name '{0}' is a file, you need a directory",
                                                         root);
            throw new SVNException(err);
        } else if (rootKind == SVNNodeKind.DIR) {
            ISVNEditor editor = repository.getCommitEditor(message, null, true, null);
            if (root.length() == 1 && root.charAt(0) == '/') {
                addProcess(repository, editor, root, "", child);
            } else {
                String rootPath = root.substring(1);
                addProcess(repository, editor, rootPath, null, child);
            }
        }
    }

    private void addProcess( SVNRepository repos,
                             ISVNEditor editor,
                             String rootPath,
                             String editedRoot,
                             String childSegmentName ) throws SVNException {
        openDirectories(editor, editedRoot);
        // test if so a directory does not exist.
        SVNNodeKind childKind = repos.checkPath(childSegmentName, -1);
        if (childKind == SVNNodeKind.NONE) {
            editor.addDir(childSegmentName, null, -1);
            closeDirectories(editor, childSegmentName);
            if (editedRoot != null) {
                closeDirectories(editor, editedRoot);
            } else {
                closeDirectories(editor, rootPath);
            }

        } else {
            closeDirectories(editor, childSegmentName);
            if (editedRoot != null) {
                closeDirectories(editor, editedRoot);
            } else {
                closeDirectories(editor, rootPath);
            }
        }
    }

    /**
     * Create a directory .
     * 
     * @param repos
     * @param root - the root directory where the created directory will reside
     * @param childName - the name of the created directory.
     * @param message - comment for the creation.
     * @throws SVNException - if during the creation, there is an error.
     */
    @SuppressWarnings( "unused" )
    private void mkdir( SVNRepository repos,
                        String root,
                        String childName,
                        String message ) throws SVNException {
        SVNNodeKind childKind = repos.checkPath(childName, -1);
        if (childKind == SVNNodeKind.NONE) {
            ScmAction addNodeAction = addDirectory(root, childName);
            SVNActionExecutor executor = new SVNActionExecutor(repos);
            executor.execute(addNodeAction, message);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Item with name '{0}' can't be created", childName);
            throw new SVNException(err);
        }
    }

    /**
     * Create a file.
     * 
     * @param path
     * @param file
     * @param content
     * @param message
     * @throws SVNException
     */
    @SuppressWarnings( "unused" )
    private void newFile( String path,
                          String file,
                          byte[] content,
                          String message ) throws SVNException {
        SVNNodeKind childKind = defaultWorkspace.checkPath(file, -1);
        if (childKind == SVNNodeKind.NONE) {
            ScmAction addFileNodeAction = addFile(path, file, content);
            SVNActionExecutor executor = new SVNActionExecutor(defaultWorkspace);
            executor.execute(addFileNodeAction, message);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "Item with name '{0}' can't be created (already exist)",
                                                         file);
            throw new SVNException(err);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#addDirectory(java.lang.String, java.lang.String)
     */
    public ScmAction addDirectory( String root,
                                   String path ) {
        return new AddDirectory(root, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#addFile(java.lang.String, java.lang.String, byte[])
     */
    public ScmAction addFile( String path,
                              String file,
                              byte[] content ) {
        return new AddFile(path, file, content);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#copyDirectory(java.lang.String, java.lang.String, long)
     */
    public ScmAction copyDirectory( String path,
                                    String newPath,
                                    long revision ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#deleteDirectory(java.lang.String)
     */
    public ScmAction deleteDirectory( String path ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#deleteFile(java.lang.String, java.lang.String)
     */
    public ScmAction deleteFile( String path,
                                 String file ) {
        return null;
    }

    /**
     * root should be the last, previously created, parent folder. Each directory in the path will be created.
     */
    public static class AddDirectory implements ScmAction {
        private String root;
        private String path;

        public AddDirectory( String root,
                             String path ) {
            this.root = root;
            this.path = path;
        }

        public void applyAction( Object context ) throws SVNException {

            ISVNEditor editor = (ISVNEditor)context;

            openDirectories(editor, this.root);
            String[] paths = this.path.split("/");
            String newPath = this.root;
            for (int i = 0, length = paths.length; i < length; i++) {
                newPath = (newPath.length() != 0) ? newPath + "/" + paths[i] : paths[i];

                editor.addDir(newPath, null, -1);
            }

            closeDirectories(editor, path);
            closeDirectories(editor, this.root);
        }
    }

    public static class AddFile implements ScmAction {
        private String path;
        private String file;
        private byte[] content;

        public AddFile( String path,
                        String file,
                        byte[] content ) {
            this.path = path;
            this.file = file;
            this.content = content;
        }

        public void applyAction( Object context ) throws Exception {
            ISVNEditor editor = (ISVNEditor)context;
            openDirectories(editor, path);

            editor.addFile(path + "/" + file, null, -1);
            editor.applyTextDelta(path + "/" + file, null);
            SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
            String checksum = deltaGenerator.sendDelta(path + "/" + file, new ByteArrayInputStream(this.content), editor, true);
            editor.closeFile(path + "/" + file, checksum);

            closeDirectories(editor, path);

        }

    }

    @SuppressWarnings( "unused" )
    private byte[] getContent( Object[] objs ) {
        byte[] content = null;
        for (Object object : objs) {
            if (object != null && object instanceof Binary) {
                Binary buf = (Binary)object;
                content = buf.getBytes();
            }
        }
        return content;
    }

    @SuppressWarnings( "unused" )
    private Object[] values( Collection<Property> childNodeProperties ) {
        Set<Object> result = new HashSet<Object>();
        for (Property property : childNodeProperties) {
            result.add(property.getFirstValue());
        }
        return result.toArray();
    }

    private void checkThePath( Path path,
                               Request request ) {
        for (Path.Segment segment : path) {
            // Verify the segment is valid ...
            if (segment.getIndex() > 1) {
                I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            }
            // TODO
            // if (!segment.getName().getNamespaceUri().equals(defaultNamespaceUri)) {
            // I18n msg = SVNRepositoryConnectorI18n.onlyTheDefaultNamespaceIsAllowed;
            // throw new RepositorySourceException(getSourceName(), msg.text(getSourceName(), request));
            // }
        }
    }

    protected SVNRepository getWorkspaceDirectory( String workspaceName ) {
        SVNRepository repository = defaultWorkspace;
        if (workspaceName != null) {
            SVNRepository repos = SVNRepositoryUtil.createRepository(workspaceName,
                                                                     accessData.getUsername(),
                                                                     accessData.getPassword());
            if (SVNRepositoryUtil.isDirectory(repos, "")) {
                repository = repos;
            } else {
                return null;
            }
        }
        return repository;
    }

    protected SVNNodeKind getNodeKind( SVNRepository repository,
                                       Path path,
                                       String repositoryRootUrl,
                                       String inWorkspace ) throws SVNException {
        assert path != null;
        assert repositoryRootUrl != null;
        assert inWorkspace != null;
        // See if the path is a "jcr:content" node ...
        if (path.endsWith(JcrLexicon.CONTENT)) {
            // We only want to use the parent path to find the actual file ...
            path = path.getParent();
        }
        String pathAsString = getPathAsString(path);
        if (!repositoryRootUrl.equals(inWorkspace)) {
            pathAsString = pathAsString.substring(1);
        }
        SVNNodeKind kind = repository.checkPath(pathAsString, -1);
        if (kind == SVNNodeKind.NONE) {
            // node does not exist or requested node is not correct.
            throw new PathNotFoundException(Location.create(path), null,
                                            SVNRepositoryConnectorI18n.nodeDoesNotExist.text(pathAsString));
        } else if (kind == SVNNodeKind.UNKNOWN) {
            // node is unknown
            throw new PathNotFoundException(Location.create(path), null,
                                            SVNRepositoryConnectorI18n.nodeIsActuallyUnknow.text(pathAsString));
        }
        return kind;
    }
}
