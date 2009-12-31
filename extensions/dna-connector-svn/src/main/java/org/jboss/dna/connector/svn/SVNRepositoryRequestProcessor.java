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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.scm.ScmAction;
import org.jboss.dna.connector.scm.ScmActionFactory;
import org.jboss.dna.connector.svn.mgnt.AddDirectory;
import org.jboss.dna.connector.svn.mgnt.AddFile;
import org.jboss.dna.connector.svn.mgnt.DeleteEntry;
import org.jboss.dna.connector.svn.mgnt.UpdateFile;
import org.jboss.dna.graph.DnaIntLexicon;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.connector.RepositorySourceException;
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
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * The {@link RequestProcessor} implementation for the file subversion repository connector. This is the class that does the bulk
 * of the work in the subversion repository connector, since it processes all requests.
 */
public class SVNRepositoryRequestProcessor extends RequestProcessor implements ScmActionFactory {

    protected static final String BACK_SLASH = "/";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * Only certain properties are tolerated when writing content (dna:resource or jcr:resource) nodes. These properties are
     * implicitly stored (primary type, data) or silently ignored (encoded, mimetype, last modified). The silently ignored
     * properties must be accepted to stay compatible with the JCR specification.
     */
    private final Set<Name> ALLOWABLE_PROPERTIES_FOR_CONTENT = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                             Arrays.asList(new Name[] {
                                                                                                                 JcrLexicon.PRIMARY_TYPE,
                                                                                                                 JcrLexicon.DATA,
                                                                                                                 JcrLexicon.ENCODED,
                                                                                                                 JcrLexicon.MIMETYPE,
                                                                                                                 JcrLexicon.LAST_MODIFIED,
                                                                                                                 JcrLexicon.UUID,
                                                                                                                 DnaIntLexicon.NODE_DEFINITON})));
    /**
     * Only certain properties are tolerated when writing files (nt:file) or folders (nt:folder) nodes. These properties are
     * implicitly stored in the file or folder (primary type, created).
     */
    private final Set<Name> ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                                    Arrays.asList(new Name[] {
                                                                                                                        JcrLexicon.PRIMARY_TYPE,
                                                                                                                        JcrLexicon.CREATED,
                                                                                                                        JcrLexicon.UUID,
                                                                                                                        DnaIntLexicon.NODE_DEFINITON})));

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
                                // Do NOT use "nt:resource", since it extends "mix:referenceable". The JCR spec
                                // does not require that "jcr:content" is of type "nt:resource", but rather just
                                // suggests it. Therefore, we can use "dna:resource", which is identical to
                                // "nt:resource" except it does not extend "mix:referenceable"
                                addProperty(properties, factory, JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE);
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
        logger.trace(request.toString());
        if (!updatesAllowed(request)) return;

        // continue
        Path parentPath = getPathFor(request.under(), request);
        if (parentPath == null) return;

        // svn connector does not support same name sibling
        sameNameSiblingIsNotSupported(parentPath);

        SVNRepository workspaceRoot = getWorkspaceDirectory(request.inWorkspace());
        assert workspaceRoot != null;

        SVNNodeKind parent = getSVNNodeKindFor(workspaceRoot, parentPath, request.under(), request.inWorkspace(), request);
        if (parent == null) {
            return;
        }

        NamespaceRegistry registry = getExecutionContext().getNamespaceRegistry();
        // New name to commit into the svn repos workspace
        String newName = request.named().getString(registry);

        // Collect all the properties of the node in a hash map
        Map<Name, Property> properties = new HashMap<Name, Property>(request.properties().size());
        for (Property property : request.properties()) {
            properties.put(property.getName(), property);
        }

        Property primaryTypeProp = properties.get(JcrLexicon.PRIMARY_TYPE);
        Name primaryType = primaryTypeProp == null ? null : nameFactory().create(primaryTypeProp.getFirstValue());

        Path newPath = pathFactory().create(parentPath, request.named());
        Location actualLocation = Location.create(newPath);

        String newChildPath = null;

        // File
        if (JcrNtLexicon.FILE.equals(primaryType)) {
            ensureValidProperties(request.properties(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
            // Parent node already exist
            boolean skipWrite = false;

            if (request.under().getPath().isRoot()) {
                if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                    newChildPath = newName;
                } else {
                    newChildPath = "/" + newName;
                }
            } else {
                newChildPath = getPathAsString(request.under().getPath()) + "/" + newName;
                if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                    newChildPath = newChildPath.substring(1);
                }
            }

            // check if the new name already exist
            try {
                if (SVNRepositoryUtil.exists(workspaceRoot, newChildPath)) {
                    if (request.conflictBehavior().equals(NodeConflictBehavior.APPEND)) {
                        I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                        throw new InvalidRequestException(msg.text("SVN Connector does not support Same Name Sibling"));
                    } else if (request.conflictBehavior().equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
                        skipWrite = true;
                    }
                }
            } catch (SVNException e1) {
                throw new RepositorySourceException(getSourceName(), e1.getMessage());
            }

            // Don't try to write if the node conflict behavior is DO_NOT_REPLACE
            if (!skipWrite) {
                // create a new, empty file
                if (newChildPath != null) {
                    try {
                        String rootPath = null;
                        if (request.under().getPath().isRoot()) {
                            rootPath = "";
                        } else {
                            rootPath = getPathAsString(request.under().getPath());
                        }
                        newFile(rootPath, newName, "".getBytes(), null, request.inWorkspace(), workspaceRoot);
                    } catch (SVNException e) {
                        I18n msg = SVNRepositoryConnectorI18n.couldNotCreateFile;
                        request.setError(new RepositorySourceException(getSourceName(),
                                                                       msg.text(getPathAsString(request.under().getPath()),
                                                                                request.inWorkspace(),
                                                                                getSourceName(),
                                                                                e.getMessage()), e));
                        return;
                    }
                }
            }
        } else if (JcrNtLexicon.RESOURCE.equals(primaryType) || DnaLexicon.RESOURCE.equals(primaryType)) { // Resource
            ensureValidProperties(request.properties(), ALLOWABLE_PROPERTIES_FOR_CONTENT);
            if (request.under().getPath().isRoot()) {
                newChildPath = getPathAsString(parentPath);
                if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                    newChildPath = getPathAsString(parentPath).substring(1);
                }
            } else {
                newChildPath = getPathAsString(parentPath);
                if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                    newChildPath = newChildPath.substring(1);
                }
            }

            if (!JcrLexicon.CONTENT.equals(request.named())) {
                I18n msg = SVNRepositoryConnectorI18n.invalidNameForResource;
                String nodeName = request.named().getString(registry);
                request.setError(new RepositorySourceException(getSourceName(),
                                                               msg.text(getPathAsString(request.under().getPath()),
                                                                        request.inWorkspace(),
                                                                        getSourceName(),
                                                                        nodeName)));
                return;
            }

            if (parent != SVNNodeKind.FILE) {
                I18n msg = SVNRepositoryConnectorI18n.invalidPathForResource;
                request.setError(new RepositorySourceException(getSourceName(),
                                                               msg.text(getPathAsString(request.under().getPath()),
                                                                        request.inWorkspace(),
                                                                        getSourceName())));
                return;
            }

            boolean skipWrite = false;
            if (parent != SVNNodeKind.NONE || parent != SVNNodeKind.UNKNOWN) {
                if (request.conflictBehavior().equals(NodeConflictBehavior.APPEND)) {
                    I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                    throw new InvalidRequestException(msg.text("SVN Connector does not support Same Name Sibling"));
                } else if (request.conflictBehavior().equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
                    // TODO check if the file already has content
                    skipWrite = true;
                }
            }

            if (!skipWrite) {
                Property dataProperty = properties.get(JcrLexicon.DATA);
                if (dataProperty == null) {
                    I18n msg = SVNRepositoryConnectorI18n.missingRequiredProperty;
                    String dataPropName = JcrLexicon.DATA.getString(registry);
                    request.setError(new RepositorySourceException(getSourceName(),
                                                                   msg.text(getPathAsString(request.under().getPath()),
                                                                            request.inWorkspace(),
                                                                            getSourceName(),
                                                                            dataPropName)));
                    return;
                }

                BinaryFactory binaryFactory = getExecutionContext().getValueFactories().getBinaryFactory();
                Binary binary = binaryFactory.create(properties.get(JcrLexicon.DATA).getFirstValue());
                // get old data
                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                SVNProperties svnProperties = new SVNProperties();
                try {
                    workspaceRoot.getFile(newChildPath, -1, svnProperties, contents);
                    byte[] oldData = contents.toByteArray();
                    // modify the empty old data with the new resource
                    if (oldData != null) {
                        String rootPath = null;
                        String fileName = null;

                        Path p = request.under().getPath();
                        rootPath = getPathAsString(p.getAncestor(1));
                        fileName = p.getLastSegment().getString(registry);

                        if (request.under().getPath().isRoot()) {
                            rootPath = "";
                        }

                        modifyFile(rootPath, fileName, oldData, binary.getBytes(), null, request.inWorkspace(), workspaceRoot);
                    }
                } catch (SVNException e) {
                    I18n msg = SVNRepositoryConnectorI18n.couldNotReadData;
                    request.setError(new RepositorySourceException(getSourceName(),
                                                                   msg.text(getPathAsString(request.under().getPath()),
                                                                            request.inWorkspace(),
                                                                            getSourceName(),
                                                                            e.getMessage()), e));
                    return;
                }
            }

        } else if (JcrNtLexicon.FOLDER.equals(primaryType) || primaryType == null) { // Folder
            ensureValidProperties(request.properties(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
            try {
                String rootDirPath = getPathAsString(request.under().getPath());
                mkdir(rootDirPath, newName, null, request.inWorkspace(), workspaceRoot);
            } catch (SVNException e) {
                I18n msg = SVNRepositoryConnectorI18n.couldNotCreateFile;
                request.setError(new RepositorySourceException(getSourceName(),
                                                               msg.text(getPathAsString(request.under().getPath()),
                                                                        request.inWorkspace(),
                                                                        getSourceName(),
                                                                        e.getMessage()), e));
                return;
            }
        } else {
            I18n msg = SVNRepositoryConnectorI18n.unsupportedPrimaryType;
            request.setError(new RepositorySourceException(getSourceName(), msg.text(primaryType.getString(registry),
                                                                                     getPathAsString(request.under().getPath()),
                                                                                     request.inWorkspace(),
                                                                                     getSourceName())));
            return;
        }

        request.setActualLocationOfNode(actualLocation);
    }

    /**
     * @param workspaceRoot
     * @param path
     * @param location
     * @param inWorkspace
     * @param request
     * @return a svn node kind
     */
    protected SVNNodeKind getSVNNodeKindFor( SVNRepository workspaceRoot,
                                             Path path,
                                             Location location,
                                             String inWorkspace,
                                             Request request ) {
        assert path != null;
        assert location != null;
        assert request != null;

        SVNNodeKind rootNode = SVNRepositoryUtil.checkThePath(workspaceRoot, "", -1, getSourceName());

        if (rootNode != SVNNodeKind.DIR) return null;

        if (path.isRoot()) {
            return rootNode;
        }

        // See if the path is a "jcr:content" node ...
        if (path.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            // We only want to use the parent path to find the actual file ...
            path = path.getParent();
        }
        SVNNodeKind kind = rootNode;
        for (Path.Segment segment : path) {
            if (segment.getIndex() > 1) {
                I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text("SVN Connector does not support Same Name Sibling"));
            }
        }

        String currentPath = getPathAsString(path);
        if (!this.accessData.getRepositoryRootUrl().equals(inWorkspace)) {
            if (currentPath.startsWith("/")) {
                currentPath = currentPath.substring(1);
            }
        }
        kind = SVNRepositoryUtil.checkThePath(workspaceRoot, currentPath, -1, getSourceName());

        if (kind != null) {
            if (kind == SVNNodeKind.NONE || kind == SVNNodeKind.UNKNOWN) {
                // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
                request.setError(new RepositorySourceException(getSourceName(), " Node kind with path " + currentPath
                                                                                + " is missing or actually unknown"));
                return null;
            }
        }

        assert kind != null;
        return kind;
    }

    protected void sameNameSiblingIsNotSupported( Path path ) {
        for (Path.Segment segment : path) {
            // Verify the segment is valid ...
            if (segment.getIndex() > 1) {
                I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text("SVN Connector does not support Same Name Sibling"));
            }
        }
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
        logger.trace(request.toString());
        if (!updatesAllowed(request)) return;

        SVNRepository workspaceRoot = getWorkspaceDirectory(request.inWorkspace());
        assert workspaceRoot != null;
        
        NamespaceRegistry registry = getExecutionContext().getNamespaceRegistry();

        Path requestedPath = request.at().getPath();
        // svn connector does not support same name sibling
        sameNameSiblingIsNotSupported(requestedPath);

        if (!requestedPath.isRoot() && JcrLexicon.CONTENT.equals(requestedPath.getLastSegment().getName())) {
            Path p = requestedPath.getAncestor(1);
            if(p != null) {
                String itemPath = getPathAsString(p);
                if (itemPath.equals("") || itemPath.equals("/")) {
                    return;
                }
                String filePath = null;
                if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                    filePath = itemPath.substring(1);
                }
                try {
                    //check if the file exist
                    if (!SVNRepositoryUtil.exists(workspaceRoot, filePath)) return;
                    
                    //update the file
                    SVNProperties fileProperties = new SVNProperties();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    workspaceRoot.getFile(filePath, -1, fileProperties, baos);
                    
                    String rootPath = getPathAsString(p.getAncestor(1));
                    String fileName = p.getLastSegment().getString(registry);
                    modifyFile(rootPath, fileName, baos.toByteArray(), "".getBytes(), null, request.inWorkspace(), workspaceRoot);

                } catch (SVNException e) {
                    throw new RepositorySourceException(getSourceName(),
                                                        SVNRepositoryConnectorI18n.deleteFailed.text(itemPath, getSourceName()));

                }
            }


        } else {

            String nodePath = getPathAsString(requestedPath);

            if (!accessData.getRepositoryRootUrl().equals(request.inWorkspace())) {
                nodePath = nodePath.substring(1);
            }

            try {
                if (!SVNRepositoryUtil.exists(workspaceRoot, nodePath)) return;
                eraseEntry(nodePath, null, request.inWorkspace(), workspaceRoot);
            } catch (SVNException e) {
                throw new RepositorySourceException(getSourceName(),
                                                    SVNRepositoryConnectorI18n.deleteFailed.text(nodePath, getSourceName()));

            }
        }
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
     * Get the repository driver.
     * 
     * @return repository
     */
    public SVNRepository getDefaultWorkspace() {
        return defaultWorkspace;
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
     * Create a directory .
     * 
     * @param rootDirPath - the root directory where the created directory will reside
     * @param childDirPath - the name of the created directory.
     * @param comment - comment for the creation.
     * @param inWorkspace
     * @param currentRepository
     * @throws SVNException - if during the creation, there is an error.
     */
    private void mkdir( String rootDirPath,
                        String childDirPath,
                        String comment,
                        String inWorkspace,
                        SVNRepository currentRepository ) throws SVNException {

        String tempParentPath = rootDirPath;
        if (!this.accessData.getRepositoryRootUrl().equals(inWorkspace)) {
            if (!tempParentPath.equals("/") && tempParentPath.startsWith("/")) {
                tempParentPath = tempParentPath.substring(1);
            } else if (tempParentPath.equals("/")) {
                tempParentPath = "";
            }
        }
        String checkPath = tempParentPath.length() == 0 ? childDirPath : tempParentPath + "/" + childDirPath;
        SVNNodeKind nodeKind = null;
        try {
            nodeKind = currentRepository.checkPath(checkPath, -1);
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "May be a Connecting problem to the repository or a user's authentication failure: {0}",
                                                         e.getMessage());
            throw new SVNException(err);
        }

        if (nodeKind != null && nodeKind == SVNNodeKind.NONE) {
            ScmAction addNodeAction = addDirectory(rootDirPath, childDirPath);
            SVNActionExecutor executor = new SVNActionExecutor(currentRepository);
            comment = comment == null ? "Create a new file " + childDirPath : comment;
            executor.execute(addNodeAction, comment);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "Node with name '{0}' can't be created",
                                                         childDirPath);
            throw new SVNException(err);
        }
    }

    /**
     * Create a file.
     * 
     * @param rootDirPath
     * @param childFilePath
     * @param content
     * @param comment
     * @param inWorkspace
     * @param currentRepository
     * @throws SVNException
     */
    private void newFile( String rootDirPath,
                         String childFilePath,
                         byte[] content,
                         String comment,
                         String inWorkspace,
                         SVNRepository currentRepository ) throws SVNException {

        String tempParentPath = rootDirPath;
        if (!this.accessData.getRepositoryRootUrl().equals(inWorkspace)) {
            if (!tempParentPath.equals("/") && tempParentPath.startsWith("/")) {
                tempParentPath = tempParentPath.substring(1);
            }
        }
        String checkPath = tempParentPath + "/" + childFilePath;
        SVNNodeKind nodeKind = null;
        try {
            nodeKind = currentRepository.checkPath(checkPath, -1);
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "May be a Connecting problem to the repository or a user's authentication failure: {0}",
                                                         e.getMessage());
            throw new SVNException(err);
        }

        if (nodeKind != null && nodeKind == SVNNodeKind.NONE) {
            ScmAction addFileNodeAction = addFile(rootDirPath, childFilePath, content);
            SVNActionExecutor executor = new SVNActionExecutor(currentRepository);
            comment = comment == null ? "Create a new file " + childFilePath : comment;
            executor.execute(addFileNodeAction, comment);
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "Item with name '{0}' can't be created (already exist)",
                                                         childFilePath);
            throw new SVNException(err);
        }
    }

    /**
     * Modify a file
     * 
     * @param rootPath
     * @param fileName
     * @param oldData
     * @param newData
     * @param comment
     * @param inWorkspace
     * @param currentRepository
     * @throws SVNException
     */
    private void modifyFile( String rootPath,
                             String fileName,
                             byte[] oldData,
                             byte[] newData,
                             String comment,
                             String inWorkspace,
                             SVNRepository currentRepository ) throws SVNException {
        assert rootPath != null;
        assert fileName != null;
        assert oldData != null;
        assert inWorkspace != null;
        assert currentRepository != null;

        try {

            if (!this.accessData.getRepositoryRootUrl().equals(inWorkspace)) {
                if (rootPath.equals("/")) {
                    rootPath = "";
                } else {
                    rootPath = rootPath.substring(1) + "/";
                }
            } else {
                if (!rootPath.equals("/")) {
                    rootPath = rootPath + "/";
                }
            }
            String path = rootPath + fileName;

            SVNNodeKind nodeKind = currentRepository.checkPath(path, -1);
            if (nodeKind == SVNNodeKind.NONE || nodeKind == SVNNodeKind.UNKNOWN) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_NOT_FOUND,
                                                             "Item with name '{0}' can't be found",
                                                             path);
                throw new SVNException(err);
            }

            ScmAction modifyFileAction = updateFile(rootPath, fileName, oldData, newData);
            SVNActionExecutor executor = new SVNActionExecutor(currentRepository);
            comment = comment == null ? "modify the " + fileName : comment;
            executor.execute(modifyFileAction, comment);

        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "This error is appeared: '{0}'", e.getMessage());
            throw new SVNException(err);
        }

    }

    /**
     * Delete entry from the repository
     * 
     * @param path
     * @param comment
     * @param inWorkspace
     * @param currentRepository
     * @throws SVNException
     */
    private void eraseEntry( String path,
                             String comment,
                             String inWorkspace,
                             SVNRepository currentRepository ) throws SVNException {
        assert path != null;
        assert inWorkspace != null;
        if (path.equals("/") || path.equals("")) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, "The root directory cannot be deleted");
            throw new SVNException(err);
        }

        try {
            ScmAction deleteEntryAction = deleteEntry(path);
            SVNActionExecutor executor = new SVNActionExecutor(currentRepository);
            comment = comment == null ? "Delete the " + path : comment;
            executor.execute(deleteEntryAction, comment);
        } catch (SVNException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                         "unknow error during delete action: {0)",
                                                         e.getMessage());
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
     * @see org.jboss.dna.connector.scm.ScmActionFactory#updateFile(java.lang.String, java.lang.String, byte[], byte[])
     */
    public ScmAction updateFile( String rootPath,
                                 String fileName,
                                 byte[] oldData,
                                 byte[] newData ) {
        return new UpdateFile(rootPath, fileName, oldData, newData);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.scm.ScmActionFactory#deleteEntry(java.lang.String)
     */
    public ScmAction deleteEntry( String path ) {
        return new DeleteEntry(path);
    }

    protected void checkThePath( Path path,
                                 Request request ) {
        for (Path.Segment segment : path) {
            // Verify the segment is valid ...
            if (segment.getIndex() > 1) {
                I18n msg = SVNRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                throw new RepositorySourceException(getSourceName(), msg.text("SVN Connector does not support Same Name Sibling"));
            }
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

    /**
     * Checks that the collection of {@code properties} only contains properties with allowable names.
     * 
     * @param properties
     * @param validPropertyNames
     * @throws RepositorySourceException if {@code properties} contains a
     * @see #ALLOWABLE_PROPERTIES_FOR_CONTENT
     * @see #ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER
     */
    protected void ensureValidProperties( Collection<Property> properties,
                                          Set<Name> validPropertyNames ) {
        List<String> invalidNames = new LinkedList<String>();
        NamespaceRegistry registry = getExecutionContext().getNamespaceRegistry();

        for (Property property : properties) {
            if (!validPropertyNames.contains(property.getName())) {
                invalidNames.add(property.getName().getString(registry));
            }
        }

        if (!invalidNames.isEmpty()) {
            throw new RepositorySourceException(this.getSourceName(),
                                                SVNRepositoryConnectorI18n.invalidPropertyNames.text(invalidNames.toString()));
        }
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
    
    protected String getPathAsString( Path path ) {
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
}
