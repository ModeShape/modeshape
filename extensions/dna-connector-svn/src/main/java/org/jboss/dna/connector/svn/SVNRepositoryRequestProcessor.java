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
import java.util.Date;
import java.util.HashSet;
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
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
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
 * @author Serge Emmanuel Pagop
 */
public class SVNRepositoryRequestProcessor extends RequestProcessor implements ScmActionFactory {

    protected static final String BACK_SLASH = "/";

    private final String defaultNamespaceUri;
    private final boolean updatesAllowed;
    private SVNRepository repository;
    protected final Logger logger;

    /**
     * @param sourceName
     * @param context
     * @param repository
     * @param updatesAllowed true if this connector supports updating the subversion repository, or false if the connector is read
     *        only
     */
    protected SVNRepositoryRequestProcessor( String sourceName,
                                             ExecutionContext context,
                                             SVNRepository repository,
                                             boolean updatesAllowed ) {
        super(sourceName, context, null);
        this.defaultNamespaceUri = getExecutionContext().getNamespaceRegistry().getDefaultNamespaceUri();
        this.updatesAllowed = updatesAllowed;
        this.repository = repository;
        this.logger = getExecutionContext().getLogger(getClass());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        logger.trace(request.toString());
        verifyUpdatesAllowed();

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        logger.trace(request.toString());
        verifyUpdatesAllowed();
        // get the parent location of the new node
        Location myLocation = request.under();
        Path parent = getPathFor(myLocation, request);
        try {
            String root = parent.getString(getExecutionContext().getNamespaceRegistry());
            SVNNodeKind rootKind = repository.checkPath(root, -1);
            if (rootKind == SVNNodeKind.UNKNOWN) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                             "path with name '{0}' is unknown in the repository",
                                                             root);
                SVNException ex = new SVNException(err);
                request.setError(ex);
            } else if (rootKind == SVNNodeKind.NONE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                             "path with name '{0}' is missing in the repository",
                                                             root);
                SVNException ex = new SVNException(err);
                request.setError(ex);
            } else if (rootKind == SVNNodeKind.FILE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                             "pretended root item with name '{0}' is a file",
                                                             root);
                SVNException ex = new SVNException(err);
                request.setError(ex);
            } else if (rootKind == SVNNodeKind.DIR) {
                Collection<Property> childNodeProperties = request.properties();
                Object[] objs = values(childNodeProperties);
                for (Object object : objs) {
                    if (object instanceof Name && ((Name)object).compareTo(JcrNtLexicon.FOLDER) == 0) {
                        // process folder creation
                        // if the node is a directory
                        String folderName = request.named().getString(getExecutionContext().getNamespaceRegistry());
                        if (root.length() == 1 && root.charAt(0) == '/') {
                            // test if so a directory does not exist.
                            mkdir("", folderName, request.toString());
                        } else {
                            if (root.length() > 1 && root.charAt(0) == '/') {
                                // test if so a directory does not exist.
                                mkdir(root.substring(1), folderName, request.toString());
                            }
                        }
                    } else if (object instanceof Name && ((Name)object).compareTo(JcrNtLexicon.FILE) == 0) {
                        String fileName = request.named().getString(getExecutionContext().getNamespaceRegistry());
                        byte[] content = getContent(objs);
                        // TODO: what is with the created on
                        // Date createdOn = getCreatedOn(objs);
                        // commit in to the repository
                        newFile(root, fileName, content, request.toString());
                    }
                }
            }

        } catch (SVNException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        logger.trace(request.toString());
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        logger.trace(request.toString());
        verifyUpdatesAllowed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void process( ReadAllChildrenRequest request ) {
        logger.trace(request.toString());
        Location myLocation = request.of();
        Path nodePath = getPathFor(myLocation, request);
        try {
            SVNNodeKind kind = validateNodeKind(nodePath);
            String requestedNodePath = nodePath.getString(getExecutionContext().getNamespaceRegistry());
            if (kind == SVNNodeKind.FILE) { // the requested node is a file.
                SVNDirEntry entry = getEntryInfo(requestedNodePath);
                if (!nodePath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                    String localName = entry.getName();
                    Name childName = nameFactory().create(defaultNamespaceUri, localName);
                    String url = entry.getURL().toString();
                    Property idProperty = propertyFactory().create(childName, url);
                    request.addChild(Location.create(pathFactory().create(nodePath, JcrLexicon.CONTENT), idProperty));
                }
            } else if (kind == SVNNodeKind.DIR) { // the requested node is a directory.
                final Collection<SVNDirEntry> dirEntries = getRepository().getDir(requestedNodePath,
                                                                                  -1,
                                                                                  null,
                                                                                  (Collection<SVNDirEntry>)null);
                for (SVNDirEntry dirEntry : dirEntries) {
                    if (dirEntry.getKind() == SVNNodeKind.FILE) {
                        String localName = dirEntry.getName();
                        Path newPath = pathFactory().create(requestedNodePath + BACK_SLASH + localName);
                        if (!newPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                            Name childName = nameFactory().create(defaultNamespaceUri, localName);
                            String url = dirEntry.getURL().toString();
                            Property idProperty = propertyFactory().create(childName, url);
                            Location location = Location.create(pathFactory().create(newPath, JcrLexicon.CONTENT), idProperty);
                            request.addChild(location);
                        }
                    } else if (dirEntry.getKind() == SVNNodeKind.DIR) {
                        String localName = dirEntry.getName();
                        Name childName = nameFactory().create(defaultNamespaceUri, localName);
                        Path childPath = pathFactory().create(nodePath, childName);
                        String url = dirEntry.getURL().toString();
                        Property idProperty = propertyFactory().create(childName, url);
                        request.addChild(childPath, idProperty);
                    }
                }
            }
            request.setActualLocationOfNode(myLocation);
        } catch (SVNException e) {
            request.setError(e);
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
        Location myLocation = request.at();
        Path nodePath = getPathFor(myLocation, request);
        if (nodePath.isRoot()) {
            // There are no properties on the root ...
            request.setActualLocationOfNode(myLocation);
            return;
        }
        try {
            // See if the path is a "jcr:content" node ...
            if (nodePath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
                // //"jcr:primaryType" property value of "nt:resource",
                // "jcr:data" property whose value are the contents of the file
                // and a few other properties, like "jcr:encoding", "jcr:mimeType" and "jcr:lastModified" and
                // also "jcr:created" property
                Path parent = nodePath.getParent();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                SVNProperties fileProperties = new SVNProperties();
                getData(parent.getString(getExecutionContext().getNamespaceRegistry()), fileProperties, os);
                Property ntResourceproperty = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE);
                request.addProperty(ntResourceproperty);
                String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
                if (mimeType != null) {
                    Property jcrMimeTypeProperty = propertyFactory().create(JcrLexicon.MIMETYPE, mimeType);
                    request.addProperty(jcrMimeTypeProperty);
                }
                SVNDirEntry entry = getEntryInfo(parent.getString(getExecutionContext().getNamespaceRegistry()));
                Date lastModified = entry.getDate();
                if (lastModified != null) {
                    Property jcrLastModifiedProperty = propertyFactory().create(JcrLexicon.LAST_MODIFIED,
                                                                                dateFactory().create(lastModified));
                    request.addProperty(jcrLastModifiedProperty);
                }
                if (os.toByteArray().length > 0) {
                    Property jcrDataProperty = propertyFactory().create(JcrLexicon.DATA, binaryFactory().create(os.toByteArray()));
                    request.addProperty(jcrDataProperty);
                }
            } else {
                SVNNodeKind kind = validateNodeKind(nodePath);
                if (kind == SVNNodeKind.FILE) {
                    // "jcr:primaryType" property whose value is "nt:file".
                    Property ntFileProperty = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE);
                    request.addProperty(ntFileProperty);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    SVNProperties fileProperties = new SVNProperties();
                    getData(nodePath.getString(getExecutionContext().getNamespaceRegistry()), fileProperties, os);
                    String created = fileProperties.getStringValue(SVNProperty.COMMITTED_DATE);
                    if (created != null) {
                        Property jcrCreatedProperty = propertyFactory().create(JcrLexicon.CREATED, created);
                        request.addProperty(jcrCreatedProperty);
                    }

                } else if (kind == SVNNodeKind.DIR) {
                    // A directory maps to a single node with a name that represents the name of the directory and a
                    // "jcr:primaryType" property whose value is "nt:folder"
                    Property jcrPrimaryTypeProp = propertyFactory().create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER);
                    request.addProperty(jcrPrimaryTypeProp);
                    SVNDirEntry dirEntry = getEntryInfo(nodePath.getString(getExecutionContext().getNamespaceRegistry()));
                    Property jcrCreatedProp = propertyFactory().create(JcrLexicon.CREATED,
                                                                       dateFactory().create(dirEntry.getDate()));
                    request.addProperty(jcrCreatedProp);
                }
            }
            request.setActualLocationOfNode(myLocation);

        } catch (SVNException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.RenameNodeRequest)
     */
    @Override
    public void process( RenameNodeRequest request ) {
        logger.trace(request.toString());
        verifyUpdatesAllowed();
        super.process(request);
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        // This does the job of converting a null workspace name to a valid workspace
        String workspaceName = request.workspaceName();
        if (workspaceName == null) workspaceName = "default";
        request.setActualRootLocation(Location.create(pathFactory().createRootPath()));
        request.setActualWorkspaceName(workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        request.setAvailableWorkspaceNames(Collections.singleton("default"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        String msg = SVNRepositoryConnectorI18n.sourceDoesNotSupportCreatingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        String msg = SVNRepositoryConnectorI18n.sourceDoesNotSupportCloningWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        String msg = SVNRepositoryConnectorI18n.sourceDoesNotSupportDeletingWorkspaces.text(getSourceName());
        request.setError(new InvalidRequestException(msg));
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
        getRepository().getFile(path, -1, properties, os);

    }

    /**
     * Get the repository driver.
     * 
     * @return repository
     */
    public SVNRepository getRepository() {
        return repository;
    }

    /**
     * Validate the kind of node and throws an exception if necessary.
     * 
     * @param requestedPath
     * @return the kind.
     */
    protected SVNNodeKind validateNodeKind( final Path requestedPath ) {
        String myPath = requestedPath.getString(getExecutionContext().getNamespaceRegistry());
        SVNNodeKind kind = null;
        try {
            kind = getRepository().checkPath(myPath, -1);
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

    /**
     * Get some important informations of a path
     * 
     * @param path - the path
     * @return - the {@link SVNDirEntry}.
     */
    protected SVNDirEntry getEntryInfo( String path ) {
        assert path != null;
        SVNDirEntry entry = null;
        try {
            entry = getRepository().info(path, -1);
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
     * @return the last revision number.
     * @throws Exception
     */
    public long getLatestRevision() throws Exception {
        try {
            return repository.getLatestRevision();
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
                addProcess(editor, root, "", child);
            } else {
                String rootPath = root.substring(1);
                addProcess(editor, rootPath, null, child);
            }
        }
    }

    private void addProcess( ISVNEditor editor,
                             String rootPath,
                             String editedRoot,
                             String childSegmentName ) throws SVNException {
        openDirectories(editor, editedRoot);
        // test if so a directory does not exist.
        SVNNodeKind childKind = repository.checkPath(childSegmentName, -1);
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
     * @param root - the root directory where the created directory will reside
     * @param childName - the name of the created directory.
     * @param message - comment for the creation.
     * @throws SVNException - if during the creation, there is an error.
     */
    private void mkdir( String root,
                        String childName,
                        String message ) throws SVNException {
        SVNNodeKind childKind = repository.checkPath(childName, -1);
        if (childKind == SVNNodeKind.NONE) {
            ScmAction addNodeAction = addDirectory(root, childName);
            SVNActionExecutor executor = new SVNActionExecutor(repository);
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
    private void newFile( String path,
                          String file,
                          byte[] content,
                          String message ) throws SVNException {
        SVNNodeKind childKind = repository.checkPath(file, -1);
        if (childKind == SVNNodeKind.NONE) {
            ScmAction addFileNodeAction = addFile(path, file, content);
            SVNActionExecutor executor = new SVNActionExecutor(repository);
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

    // private Date getCreatedOn( Object[] objs ) {
    // Date createdOn = null;
    // for (Object object : objs) {
    // if (object instanceof Date) {
    // createdOn = (Date)object;
    //
    // }
    // }
    // return createdOn;
    // }

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

    private Object[] values( Collection<Property> childNodeProperties ) {
        Set<Object> result = new HashSet<Object>();
        for (Property property : childNodeProperties) {
            result.add(property.getFirstValue());
        }
        return result.toArray();
    }
}
