package org.modeshape.connector.svn;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.i18n.I18n;
import org.modeshape.connector.scm.ScmAction;
import org.modeshape.connector.svn.mgnt.AddDirectory;
import org.modeshape.connector.svn.mgnt.AddFile;
import org.modeshape.connector.svn.mgnt.DeleteEntry;
import org.modeshape.connector.svn.mgnt.UpdateFile;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.path.AbstractWritablePathWorkspace;
import org.modeshape.graph.connector.path.DefaultPathNode;
import org.modeshape.graph.connector.path.PathNode;
import org.modeshape.graph.connector.path.WritablePathRepository;
import org.modeshape.graph.connector.path.WritablePathWorkspace;
import org.modeshape.graph.connector.path.cache.WorkspaceCache;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidRequestException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnRepository extends WritablePathRepository {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    protected final SvnRepositorySource source;

    static {
        // for DAV (over http and https)
        DAVRepositoryFactory.setup();
        // For File
        FSRepositoryFactory.setup();
        // for SVN (over svn and svn+ssh)
        SVNRepositoryFactoryImpl.setup();
    }

    public SvnRepository( SvnRepositorySource source ) {
        super(source);

        this.source = source;
        initialize();
    }

    @Override
    protected void initialize() {
        ExecutionContext context = source.getRepositoryContext().getExecutionContext();
        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            doCreateWorkspace(context, workspaceName);
        }

        String defaultWorkspaceName = source.getDefaultWorkspaceName();
        if (defaultWorkspaceName != null && !workspaces.containsKey(defaultWorkspaceName)) {
            doCreateWorkspace(context, defaultWorkspaceName);
        }

    }

    public WorkspaceCache getCache( String workspaceName ) {
        return source.getPathRepositoryCache().getCache(workspaceName);
    }

    /**
     * Internal method that creates a workspace and adds it to the map of active workspaces without checking to see if the source
     * allows creating workspaces. This is useful when setting up predefined workspaces.
     * 
     * @param context the current execution context; may not be null
     * @param name the name of the workspace to create; may not be null
     * @return the newly created workspace; never null
     */
    private WritablePathWorkspace doCreateWorkspace( ExecutionContext context,
                                                     String name ) {
        SvnWorkspace workspace = new SvnWorkspace(name, source.getRootNodeUuid());

        workspaces.putIfAbsent(name, workspace);
        return (WritablePathWorkspace)workspaces.get(name);

    }

    @Override
    protected WritablePathWorkspace createWorkspace( ExecutionContext context,
                                                     String name ) {
        if (!source.isCreatingWorkspacesAllowed()) {
            String msg = SvnRepositoryConnectorI18n.unableToCreateWorkspaces.text(getSourceName(), name);
            throw new InvalidRequestException(msg);
        }

        return doCreateWorkspace(context, name);
    }

    class SvnWorkspace extends AbstractWritablePathWorkspace {

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
                                                                                                                     ModeShapeIntLexicon.NODE_DEFINITON})));
        /**
         * Only certain properties are tolerated when writing files (nt:file) or folders (nt:folder) nodes. These properties are
         * implicitly stored in the file or folder (primary type, created).
         */
        private final Set<Name> ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                                        Arrays.asList(new Name[] {
                                                                                                                            JcrLexicon.PRIMARY_TYPE,
                                                                                                                            JcrLexicon.CREATED,
                                                                                                                            JcrLexicon.UUID,
                                                                                                                            ModeShapeIntLexicon.NODE_DEFINITON})));

        private final SVNRepository workspaceRoot;

        public SvnWorkspace( String name,
                             UUID rootNodeUuid ) {
            super(name, rootNodeUuid);

            workspaceRoot = getWorkspaceDirectory(name);

            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(source.getUsername(),
                                                                                                 source.getPassword());
            workspaceRoot.setAuthenticationManager(authManager);
        }

        public Path getLowestExistingPath( Path path ) {
            do {
                path = path.getParent();

                if (getNode(path) != null) {
                    return path;
                }
            } while (path != null);

            assert false : "workspace root path was not a valid path";
            return null;
        }

        public PathNode getNode( Path path ) {
            WorkspaceCache cache = getCache(getName());

            PathNode node = cache.get(path);
            if (node != null) return node;

            ExecutionContext context = source.getRepositoryContext().getExecutionContext();
            List<Property> properties = new LinkedList<Property>();
            List<Segment> children = new LinkedList<Segment>();

            try {
                boolean result = readNode(context, this.getName(), path, properties, children);
                if (!result) return null;
            } catch (SVNException ex) {
                return null;
            }

            UUID uuid = path.isRoot() ? source.getRootNodeUuid() : null;
            node = new DefaultPathNode(path, uuid, properties, children);

            cache.set(node);
            return node;
        }

        public PathNode createNode( ExecutionContext context,
                                    PathNode parentNode,
                                    Name name,
                                    Map<Name, Property> properties,
                                    NodeConflictBehavior conflictBehavior ) {

            NamespaceRegistry registry = context.getNamespaceRegistry();
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();

            // New name to commit into the svn repos workspace
            String newName = name.getString(registry);

            Property primaryTypeProp = properties.get(JcrLexicon.PRIMARY_TYPE);
            Name primaryType = primaryTypeProp == null ? null : nameFactory.create(primaryTypeProp.getFirstValue());

            Path parentPath = parentNode.getPath();
            String parentPathAsString = parentPath.getString(registry);
            Path newPath = pathFactory.create(parentPath, name);

            String newChildPath = null;

            // File
            if (JcrNtLexicon.FILE.equals(primaryType)) {
                ensureValidProperties(context, properties.values(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
                // Parent node already exist
                boolean skipWrite = false;

                if (parentPath.isRoot()) {
                    if (!source.getRepositoryRootUrl().equals(getName())) {
                        newChildPath = newName;
                    } else {
                        newChildPath = "/" + newName;
                    }
                } else {
                    newChildPath = newPath.getString(registry);
                    if (!source.getRepositoryRootUrl().equals(getName())) {
                        newChildPath = newChildPath.substring(1);
                    }
                }

                // check if the new name already exist
                try {
                    if (SvnRepositoryUtil.exists(workspaceRoot, newChildPath)) {
                        if (conflictBehavior.equals(NodeConflictBehavior.APPEND)) {
                            I18n msg = SvnRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                            throw new InvalidRequestException(msg.text("SVN Connector does not support Same Name Sibling"));
                        } else if (conflictBehavior.equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
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
                            if (parentPath.isRoot()) {
                                rootPath = "";
                            } else {
                                rootPath = parentPathAsString;
                            }
                            newFile(rootPath, newName, EMPTY_BYTE_ARRAY, null, getName(), workspaceRoot);
                        } catch (SVNException e) {
                            I18n msg = SvnRepositoryConnectorI18n.couldNotCreateFile;
                            throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString,
                                                                                          getName(),
                                                                                          getSourceName(),
                                                                                          e.getMessage()), e);
                        }
                    }
                }
            } else if (JcrNtLexicon.RESOURCE.equals(primaryType) || ModeShapeLexicon.RESOURCE.equals(primaryType)) { // Resource
                ensureValidProperties(context, properties.values(), ALLOWABLE_PROPERTIES_FOR_CONTENT);
                if (parentPath.isRoot()) {
                    newChildPath = parentPathAsString;
                    if (!source.getRepositoryRootUrl().equals(getName())) {
                        newChildPath = parentPathAsString.substring(1);
                    }
                } else {
                    newChildPath = parentPathAsString;
                    if (!source.getRepositoryRootUrl().equals(getName())) {
                        newChildPath = newChildPath.substring(1);
                    }
                }

                if (!JcrLexicon.CONTENT.equals(name)) {
                    I18n msg = SvnRepositoryConnectorI18n.invalidNameForResource;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString,
                                                                                  getName(),
                                                                                  getSourceName(),
                                                                                  newName));
                }

                Property parentPrimaryType = parentNode.getProperty(JcrLexicon.PRIMARY_TYPE);
                Name parentPrimaryTypeName = parentPrimaryType == null ? null : nameFactory.create(parentPrimaryType.getFirstValue());
                if (!JcrNtLexicon.FILE.equals(parentPrimaryTypeName)) {
                    I18n msg = SvnRepositoryConnectorI18n.invalidPathForResource;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString, getName(), getSourceName()));
                }

                boolean skipWrite = false;
                if (conflictBehavior.equals(NodeConflictBehavior.APPEND)) {
                    I18n msg = SvnRepositoryConnectorI18n.sameNameSiblingsAreNotAllowed;
                    throw new InvalidRequestException(msg.text("SVN Connector does not support Same Name Sibling"));
                } else if (conflictBehavior.equals(NodeConflictBehavior.DO_NOT_REPLACE)) {
                    // TODO check if the file already has content
                    skipWrite = true;
                }

                if (!skipWrite) {
                    Property dataProperty = properties.get(JcrLexicon.DATA);
                    if (dataProperty == null) {
                        I18n msg = SvnRepositoryConnectorI18n.missingRequiredProperty;
                        String dataPropName = JcrLexicon.DATA.getString(registry);
                        throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString,
                                                                                      getName(),
                                                                                      getSourceName(),
                                                                                      dataPropName));
                    }

                    BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                    Binary binary = binaryFactory.create(properties.get(JcrLexicon.DATA).getFirstValue());
                    // get old data
                    ByteArrayOutputStream contents = new ByteArrayOutputStream();
                    SVNProperties svnProperties = new SVNProperties();
                    try {
                        workspaceRoot.getFile(newChildPath, -1, svnProperties, contents);
                        byte[] oldData = contents.toByteArray();

                        // modify the empty old data with the new resource
                        if (oldData != null) {
                            String pathToFile;
                            if (parentPath.isRoot()) {
                                pathToFile = "";
                            } else {
                                pathToFile = parentPath.getParent().getString(registry);
                            }
                            String fileName = parentPath.getLastSegment().getString(registry);

                            modifyFile(pathToFile, fileName, oldData, binary.getBytes(), null, getName(), workspaceRoot);
                        }
                    } catch (SVNException e) {
                        I18n msg = SvnRepositoryConnectorI18n.couldNotReadData;
                        throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString,
                                                                                      getName(),
                                                                                      getSourceName(),
                                                                                      e.getMessage()), e);
                    }
                }

            } else if (JcrNtLexicon.FOLDER.equals(primaryType) || primaryType == null) { // Folder
                ensureValidProperties(context, properties.values(), ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
                try {
                    mkdir(parentPathAsString, newName, null, getName(), workspaceRoot);
                } catch (SVNException e) {
                    I18n msg = SvnRepositoryConnectorI18n.couldNotCreateFile;
                    throw new RepositorySourceException(getSourceName(), msg.text(parentPathAsString,
                                                                                  getName(),
                                                                                  getSourceName(),
                                                                                  e.getMessage()), e);
                }
            } else {
                I18n msg = SvnRepositoryConnectorI18n.unsupportedPrimaryType;
                throw new RepositorySourceException(getSourceName(), msg.text(primaryType.getString(registry),
                                                                              parentPathAsString,
                                                                              getName(),
                                                                              getSourceName()));
            }

            PathNode node = getNode(newPath);

            List<Segment> newChildren = new ArrayList<Segment>(parentNode.getChildSegments().size() + 1);
            newChildren.addAll(parentNode.getChildSegments());
            newChildren.add(node.getPath().getLastSegment());

            WorkspaceCache cache = getCache(getName());
            cache.set(new DefaultPathNode(parentNode.getPath(), parentNode.getUuid(), parentNode.getProperties(), newChildren));
            cache.set(node);

            return node;
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
            if (!source.getRepositoryRootUrl().equals(inWorkspace)) {
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
                ScmAction addNodeAction = new AddDirectory(rootDirPath, childDirPath);
                SvnActionExecutor executor = new SvnActionExecutor(currentRepository);
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
            if (!source.getRepositoryRootUrl().equals(inWorkspace)) {
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
                ScmAction addFileNodeAction = new AddFile(rootDirPath, childFilePath, content);
                SvnActionExecutor executor = new SvnActionExecutor(currentRepository);
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

                if (!source.getRepositoryRootUrl().equals(inWorkspace)) {
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

                ScmAction modifyFileAction = new UpdateFile(rootPath, fileName, oldData, newData);
                SvnActionExecutor executor = new SvnActionExecutor(currentRepository);
                comment = comment == null ? "modify the " + fileName : comment;
                executor.execute(modifyFileAction, comment);

            } catch (SVNException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "This error is appeared: " + e.getMessage());
                throw new SVNException(err, e);
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
                ScmAction deleteEntryAction = new DeleteEntry(path);
                SvnActionExecutor executor = new SvnActionExecutor(currentRepository);
                comment = comment == null ? "Delete the " + path : comment;
                executor.execute(deleteEntryAction, comment);
            } catch (SVNException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                             "unknow error during delete action: {0)",
                                                             e.getMessage());
                throw new SVNException(err);
            }
        }

        public boolean removeNode( ExecutionContext context,
                                   Path nodePath ) {

            NamespaceRegistry registry = context.getNamespaceRegistry();

            boolean isContentNode = !nodePath.isRoot() && JcrLexicon.CONTENT.equals(nodePath.getLastSegment().getName());
            Path actualPath = isContentNode ? nodePath.getParent() : nodePath;

            try {
                SVNNodeKind kind = getNodeKind(context, actualPath, source.getRepositoryRootUrl());

                if (kind == SVNNodeKind.NONE) {
                    return false;
                }

                if (isContentNode) {
                    String rootPath = actualPath.getParent().getString(registry);
                    String fileName = actualPath.getLastSegment().getString(registry);
                    modifyFile(rootPath, fileName, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY, null, getName(), workspaceRoot);
                } else {
                    eraseEntry(actualPath.getString(registry), null, getName(), workspaceRoot);
                }
            } catch (SVNException e) {
                throw new RepositorySourceException(getSourceName(),
                                                    SvnRepositoryConnectorI18n.deleteFailed.text(nodePath, getSourceName()));
            }

            getCache(getName()).invalidate(nodePath);

            return true;
        }

        public PathNode setProperties( ExecutionContext context,
                                       Path nodePath,
                                       Map<Name, Property> properties ) {
            PathNode targetNode = getNode(nodePath);
            if (targetNode == null) return null;

            /*
             * You can't really remove any properties from SVN nodes.
             * You can clear the data of a dna:resource though
             */

            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            Property primaryTypeProperty = targetNode.getProperty(JcrLexicon.PRIMARY_TYPE);
            Name primaryTypeName = primaryTypeProperty == null ? null : nameFactory.create(primaryTypeProperty.getFirstValue());
            if (ModeShapeLexicon.RESOURCE.equals(primaryTypeName)) {

                for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                    if (JcrLexicon.DATA.equals(entry.getKey())) {
                        NamespaceRegistry registry = context.getNamespaceRegistry();
                        byte[] data;
                        if (entry.getValue() == null) {
                            data = EMPTY_BYTE_ARRAY;
                        } else {
                            BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                            data = binaryFactory.create(entry.getValue().getFirstValue()).getBytes();

                        }

                        try {
                            Path actualPath = nodePath.getParent();
                            modifyFile(actualPath.getParent().getString(registry),
                                       actualPath.getLastSegment().getString(registry),
                                       EMPTY_BYTE_ARRAY,
                                       data,
                                       "",
                                       getName(),
                                       workspaceRoot);

                            PathNode node = getNode(nodePath);
                            getCache(getName()).set(node);

                            return node;
                        } catch (SVNException ex) {
                            throw new RepositorySourceException(getSourceName(),
                                                                SvnRepositoryConnectorI18n.deleteFailed.text(nodePath,
                                                                                                             getSourceName()), ex);
                        }
                    }
                }
            }

            return targetNode;
        }

        protected boolean readNode( ExecutionContext context,
                                    String workspaceName,
                                    Path requestedPath,
                                    List<Property> properties,
                                    List<Segment> children ) throws SVNException {
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            NamespaceRegistry registry = context.getNamespaceRegistry();

            if (requestedPath.isRoot()) {
                // workspace root must be a directory
                if (children != null) {
                    final Collection<SVNDirEntry> entries = SvnRepositoryUtil.getDir(workspaceRoot, "");
                    for (SVNDirEntry entry : entries) {
                        // All of the children of a directory will be another directory or a file, but never a "jcr:content" node
                        // ...
                        children.add(pathFactory.createSegment(entry.getName()));
                    }
                }
                // There are no properties on the root ...
            } else {
                // Generate the properties for this File object ...
                PropertyFactory factory = context.getPropertyFactory();
                DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();

                // Figure out the kind of node this represents ...
                SVNNodeKind kind = getNodeKind(context, requestedPath, source.getRepositoryRootUrl());
                if (kind == SVNNodeKind.NONE) {
                    // The node doesn't exist
                    return false;
                }
                if (kind == SVNNodeKind.DIR) {
                    String directoryPath = requestedPath.getString(registry);
                    if (!source.getRepositoryRootUrl().equals(workspaceName)) {
                        directoryPath = directoryPath.substring(1);
                    }
                    if (children != null) {
                        // Decide how to represent the children ...
                        Collection<SVNDirEntry> dirEntries = SvnRepositoryUtil.getDir(workspaceRoot, directoryPath);
                        for (SVNDirEntry entry : dirEntries) {
                            // All of the children of a directory will be another directory or a file,
                            // but never a "jcr:content" node ...
                            children.add(pathFactory.createSegment(entry.getName()));
                        }
                    }
                    if (properties != null) {
                        // Load the properties for this directory ......
                        properties.add(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FOLDER));
                        SVNDirEntry entry = getEntryInfo(workspaceRoot, directoryPath);
                        if (entry != null) {
                            properties.add(factory.create(JcrLexicon.CREATED, dateFactory.create(entry.getDate())));
                        }
                    }
                } else {
                    // It's not a directory, so must be a file; the only child of an nt:file is the "jcr:content" node
                    // ...
                    if (requestedPath.endsWith(JcrLexicon.CONTENT)) {
                        // There are never any children of these nodes, just properties ...
                        if (properties != null) {
                            String contentPath = requestedPath.getParent().getString(registry);
                            if (!source.getRepositoryRootUrl().equals(workspaceName)) {
                                contentPath = contentPath.substring(1);
                            }
                            SVNDirEntry entry = getEntryInfo(workspaceRoot, contentPath);
                            if (entry != null) {
                                // The request is to get properties of the "jcr:content" child node ...
                                // Do NOT use "nt:resource", since it extends "mix:referenceable". The JCR spec
                                // does not require that "jcr:content" is of type "nt:resource", but rather just
                                // suggests it. Therefore, we can use "dna:resource", which is identical to
                                // "nt:resource" except it does not extend "mix:referenceable"
                                properties.add(factory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.RESOURCE));
                                properties.add(factory.create(JcrLexicon.LAST_MODIFIED, dateFactory.create(entry.getDate())));
                            }

                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            SVNProperties fileProperties = new SVNProperties();
                            getData(contentPath, fileProperties, os);
                            String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
                            if (mimeType == null) mimeType = DEFAULT_MIME_TYPE;
                            properties.add(factory.create(JcrLexicon.MIMETYPE, mimeType));

                            if (os.toByteArray().length > 0) {
                                // Now put the file's content into the "jcr:data" property ...
                                BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
                                properties.add(factory.create(JcrLexicon.DATA, binaryFactory.create(os.toByteArray())));
                            }
                        }
                    } else {
                        // Determine the corresponding file path for this object ...
                        String filePath = requestedPath.getString(registry);
                        if (!source.getRepositoryRootUrl().equals(workspaceName)) {
                            filePath = filePath.substring(1);
                        }
                        if (children != null) {
                            // Not a "jcr:content" child node but rather an nt:file node, so add the child ...
                            children.add(pathFactory.createSegment(JcrLexicon.CONTENT));
                        }
                        if (properties != null) {
                            // Now add the properties to "nt:file" ...
                            properties.add(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE));
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            SVNProperties fileProperties = new SVNProperties();
                            getData(filePath, fileProperties, os);
                            String created = fileProperties.getStringValue(SVNProperty.COMMITTED_DATE);
                            properties.add(factory.create(JcrLexicon.CREATED, dateFactory.create(created)));
                        }
                    }
                }
            }
            return true;
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
                                                    SvnRepositoryConnectorI18n.connectingFailureOrUserAuthenticationProblem.text(getSourceName()));
            }
            return entry;
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
            workspaceRoot.getFile(path, -1, properties, os);

        }

        protected SVNNodeKind getNodeKind( ExecutionContext context,
                                           Path path,
                                           String repositoryRootUrl ) throws SVNException {
            assert path != null;
            assert repositoryRootUrl != null;

            // See if the path is a "jcr:content" node ...
            if (path.endsWith(JcrLexicon.CONTENT)) {
                // We only want to use the parent path to find the actual file ...
                path = path.getParent();
            }
            String pathAsString = path.getString(context.getNamespaceRegistry());
            if (!repositoryRootUrl.equals(getName())) {
                pathAsString = pathAsString.substring(1);
            }

            String absolutePath = pathAsString;
            SVNNodeKind kind = workspaceRoot.checkPath(absolutePath, -1);
            if (kind == SVNNodeKind.UNKNOWN) {
                // node is unknown
                throw new RepositorySourceException(getSourceName(),
                                                    SvnRepositoryConnectorI18n.nodeIsActuallyUnknow.text(pathAsString));
            }
            return kind;
        }

        protected SVNRepository getWorkspaceDirectory( String workspaceName ) {
            if (workspaceName == null) workspaceName = source.getDefaultWorkspaceName();

            workspaceName = source.getRepositoryRootUrl() + workspaceName;

            SVNRepository repository = null;
            SVNRepository repos = SvnRepositoryUtil.createRepository(workspaceName, source.getUsername(), source.getPassword());
            if (SvnRepositoryUtil.isDirectory(repos, "")) {
                repository = repos;
            } else {
                return null;
            }
            return repository;
        }

        /**
         * Checks that the collection of {@code properties} only contains properties with allowable names.
         * 
         * @param context
         * @param properties
         * @param validPropertyNames
         * @throws RepositorySourceException if {@code properties} contains a
         * @see #ALLOWABLE_PROPERTIES_FOR_CONTENT
         * @see #ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER
         */
        protected void ensureValidProperties( ExecutionContext context,
                                              Collection<Property> properties,
                                              Set<Name> validPropertyNames ) {
            List<String> invalidNames = new LinkedList<String>();
            NamespaceRegistry registry = context.getNamespaceRegistry();

            for (Property property : properties) {
                if (!validPropertyNames.contains(property.getName())) {
                    invalidNames.add(property.getName().getString(registry));
                }
            }

            if (!invalidNames.isEmpty()) {
                throw new RepositorySourceException(getSourceName(),
                                                    SvnRepositoryConnectorI18n.invalidPropertyNames.text(invalidNames.toString()));
            }
        }

    }

}
