package org.modeshape.connector.svn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.i18n.I18n;
import org.modeshape.connector.scm.ScmAction;
import org.modeshape.connector.svn.mgnt.AddDirectory;
import org.modeshape.connector.svn.mgnt.AddFile;
import org.modeshape.connector.svn.mgnt.DeleteEntry;
import org.modeshape.connector.svn.mgnt.UpdateFile;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.ModeShapeIntLexicon;
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
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Workspace implementation for SVN repository connector
 */
public class SvnWorkspace extends PathWorkspace<PathNode> {
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private final Set<Name> ALLOWABLE_PRIMARY_TYPES = Collections.unmodifiableSet(new HashSet<Name>(Arrays.asList(new Name[] {
        JcrNtLexicon.FOLDER, JcrNtLexicon.FILE, JcrNtLexicon.RESOURCE, ModeShapeLexicon.RESOURCE, null})));

    /**
     * Only certain properties are tolerated when writing content (dna:resource or jcr:resource) nodes. These properties are
     * implicitly stored (primary type, data) or silently ignored (encoded, mimetype, last modified). The silently ignored
     * properties must be accepted to stay compatible with the JCR specification.
     */
    private final Set<Name> ALLOWABLE_PROPERTIES_FOR_CONTENT = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                             Arrays.asList(new Name[] {
                                                                                                                 JcrLexicon.PRIMARY_TYPE,
                                                                                                                 JcrLexicon.DATA,
                                                                                                                 JcrLexicon.ENCODING,
                                                                                                                 JcrLexicon.MIMETYPE,
                                                                                                                 JcrLexicon.LAST_MODIFIED,
                                                                                                                 JcrLexicon.LAST_MODIFIED_BY,
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
                                                                                                                        JcrLexicon.CREATED_BY,
                                                                                                                        JcrLexicon.UUID,
                                                                                                                        ModeShapeIntLexicon.NODE_DEFINITON})));

    // The SvnRepository is a reference to the ModeShape repository class
    private final SvnRepository repository;

    // The SVNRepository is a reference to the tmatesoft SVN repository class
    private final SVNRepository workspaceRoot;

    public SvnWorkspace( SvnRepository repository,
                         SVNRepository workspaceRoot,
                         String name,
                         UUID rootNodeUuid ) {
        super(name, rootNodeUuid);

        this.repository = repository;
        this.workspaceRoot = workspaceRoot;

        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(repository.source().getUsername(),
                                                                                             repository.source().getPassword());
        workspaceRoot.setAuthenticationManager(authManager);
    }

    public SvnWorkspace( String name,
                         SvnWorkspace originalToClone,
                         SVNRepository workspaceRoot ) {
        super(name, originalToClone.getRootNodeUuid());

        this.repository = originalToClone.repository;
        this.workspaceRoot = workspaceRoot;

        cloneWorkspace(originalToClone);
    }

    private void cloneWorkspace( SvnWorkspace original ) {
        I18n msg = SvnRepositoryConnectorI18n.sourceDoesNotSupportCloningWorkspaces;
        throw new UnsupportedOperationException(msg.text(original.source().getName()));
    }

    private final SvnRepositorySource source() {
        return repository.source();
    }

    private final String getSourceName() {
        return source().getName();
    }

    private final ExecutionContext context() {
        return source().getRepositoryContext().getExecutionContext();
    }

    private final NameFactory nameFactory() {
        return context().getValueFactories().getNameFactory();
    }

    private final PathFactory pathFactory() {
        return context().getValueFactories().getPathFactory();
    }

    private final Path pathTo( PathNode node ) {
        if (node.getParent() == null) {
            return pathFactory().createRootPath();
        }
        return pathFactory().create(node.getParent(), node.getName());
    }

    @Override
    public PathNode getRootNode() {
        return getNode(context().getValueFactories().getPathFactory().createRootPath());
    }

    @Override
    public PathNode getNode( Path path ) {
        PathNode node;

        ExecutionContext context = source().getRepositoryContext().getExecutionContext();
        List<Property> properties = new LinkedList<Property>();
        List<Segment> children = new LinkedList<Segment>();

        try {
            boolean result = readNode(context, this.getName(), path, properties, children);
            if (!result) return null;
        } catch (SVNException ex) {
            return null;
        }

        UUID uuid = path.isRoot() ? source().getRootNodeUuidObject() : null;
        Path parent = path.isRoot() ? null : path.getParent();
        Segment name = path.isRoot() ? null : path.getLastSegment();

        node = new PathNode(uuid, parent, name, properties, children);

        return node;
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
            SVNNodeKind kind = getNodeKind(context, requestedPath, source().getRepositoryRootUrl());
            if (kind == SVNNodeKind.NONE) {
                // The node doesn't exist
                return false;
            }
            if (kind == SVNNodeKind.DIR) {
                String directoryPath = requestedPath.getString(registry);
                if (!source().getRepositoryRootUrl().equals(workspaceName)) {
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
                    // SVNDirEntry entry = getEntryInfo(workspaceRoot, directoryPath);
                    SVNDirEntry entry = workspaceRoot.info(directoryPath, -1);
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
                        if (!source().getRepositoryRootUrl().equals(workspaceName)) {
                            contentPath = contentPath.substring(1);
                        }
                        SVNDirEntry entry = workspaceRoot.info(contentPath, -1);
                        if (entry != null) {
                            // The request is to get properties of the "jcr:content" child node ...
                            properties.add(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE));
                            properties.add(factory.create(JcrLexicon.LAST_MODIFIED, dateFactory.create(entry.getDate())));
                        }

                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        SVNProperties fileProperties = new SVNProperties();
                        workspaceRoot.getFile(contentPath, -1, fileProperties, os);
                        String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);
                        if (mimeType == null) {
                            // Try to determine the MIME type from the file name ...
                            String fileName = requestedPath.getParent().getLastSegment().getString(registry);
                            MimeTypeDetector mimeTypeDetector = context.getMimeTypeDetector();
                            try {
                                mimeType = mimeTypeDetector.mimeTypeOf(fileName, null);
                            } catch (IOException e) {
                                // do nothing ...
                            }
                        }
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
                    if (!source().getRepositoryRootUrl().equals(workspaceName)) {
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
                        workspaceRoot.getFile(filePath, -1, fileProperties, os);
                        String created = fileProperties.getStringValue(SVNProperty.COMMITTED_DATE);
                        properties.add(factory.create(JcrLexicon.CREATED, dateFactory.create(created)));
                    }
                }
            }
        }
        return true;
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

    private Name primaryTypeFor( PathNode node ) {
        Property primaryTypeProp = node.getProperty(JcrLexicon.PRIMARY_TYPE);
        Name primaryType = primaryTypeProp == null ? null : nameFactory().create(primaryTypeProp.getFirstValue());

        return primaryType;
    }

    protected void validate( PathNode node ) {
        Name primaryType = primaryTypeFor(node);

        if (!ALLOWABLE_PRIMARY_TYPES.contains(primaryType)) {
            I18n msg = SvnRepositoryConnectorI18n.unsupportedPrimaryType;
            NamespaceRegistry registry = context().getNamespaceRegistry();
            String path = pathTo(node).getString(registry);
            String primaryTypeName = primaryType.getString(registry);
            throw new RepositorySourceException(getSourceName(), msg.text(path, getName(), getSourceName(), primaryTypeName));
        }

        Set<Name> invalidPropertyNames = new HashSet<Name>(node.getProperties().keySet());
        if (JcrNtLexicon.RESOURCE.equals(primaryType) || ModeShapeLexicon.RESOURCE.equals(primaryType)) {
            invalidPropertyNames.removeAll(ALLOWABLE_PROPERTIES_FOR_CONTENT);
        } else {
            invalidPropertyNames.removeAll(ALLOWABLE_PROPERTIES_FOR_FILE_OR_FOLDER);
        }

        if (!invalidPropertyNames.isEmpty()) {
            I18n msg = SvnRepositoryConnectorI18n.invalidPropertyNames;
            throw new RepositorySourceException(getSourceName(), msg.text(invalidPropertyNames));

        }

    }

    @Override
    public ChangeCommand<PathNode> createMoveCommand( PathNode source,
                                                      PathNode target ) {
        // Manually create all of the commands needed to delete the source and recreate it in the target
        List<SvnCommand> commands = new LinkedList<SvnCommand>();
        LinkedList<Path> pathsToCopy = new LinkedList<Path>();

        Path sourceRoot = pathTo(source);
        Path targetRoot = pathTo(target);

        pathsToCopy.add(sourceRoot);

        while (!pathsToCopy.isEmpty()) {
            Path path = pathsToCopy.removeFirst();
            PathNode node = getNode(path);

            assert node != null : path;

            Path oldParent = node.getParent();
            Path newParent = oldParent.relativeTo(sourceRoot).resolveAgainst(targetRoot);

            PathNode newNode = node.clone().withParent(newParent);
            if (path.equals(sourceRoot)) {
                newNode = newNode.withName(target.getName());
            }
            commands.add(createPutCommand(null, newNode));

            for (Segment child : node.getChildren()) {
                pathsToCopy.add(pathFactory().create(path, child));
            }

        }

        commands.add(createRemoveCommand(pathTo(source)));
        return new SvnCompositeCommand(commands);
    }

    @Override
    public SvnCommand createPutCommand( PathNode previousNode,
                                        PathNode node ) {
        Name primaryType = primaryTypeFor(node);

        // Can't modify the root node
        if (node.getParent() == null) {
            return null;
        }

        NamespaceRegistry registry = context().getNamespaceRegistry();
        String parentPath = node.getParent().getString(registry);
        String name = node.getName().getString(registry);

        if (primaryType == null || JcrNtLexicon.FOLDER.equals(primaryType)) {
            if (previousNode != null) {
                return null;
            }
            return new SvnPutFolderCommand(parentPath, name);
        }

        if (JcrNtLexicon.FILE.equals(primaryType)) {
            if (previousNode != null) {
                return null;
            }
            return new SvnPutFileCommand(parentPath, name, EMPTY_BYTE_ARRAY);
        }

        byte[] oldContent;

        if (previousNode != null) {
            Property oldContentProp = previousNode.getProperty(JcrLexicon.DATA);
            Binary oldContentBin = oldContentProp == null ? null : context().getValueFactories()
                                                                            .getBinaryFactory()
                                                                            .create(oldContentProp.getFirstValue());
            oldContent = oldContentBin == null ? EMPTY_BYTE_ARRAY : oldContentBin.getBytes();
        } else {
            oldContent = EMPTY_BYTE_ARRAY;
        }

        Property contentProp = node.getProperty(JcrLexicon.DATA);
        Binary contentBin = contentProp == null ? null : context().getValueFactories()
                                                                  .getBinaryFactory()
                                                                  .create(contentProp.getFirstValue());
        byte[] newContent = contentBin == null ? EMPTY_BYTE_ARRAY : contentBin.getBytes();

        // The path for a content node ends with the /jcr:content. Need to go up one level to get the file name.
        Path filePath = node.getParent();
        String fileDir = filePath.isRoot() ? "/" : filePath.getParent().getString(registry);
        String fileName = filePath.getLastSegment().getString(registry);

        return new SvnPutContentCommand(fileDir, fileName, oldContent, newContent);
    }

    @Override
    public SvnCommand createRemoveCommand( Path path ) {
        String svnPath = path.getString(context().getNamespaceRegistry());
        return new SvnRemoveCommand(svnPath);
    }

    @Override
    public void commit( List<ChangeCommand<PathNode>> commands ) {
        ISVNEditor editor = null;
        boolean commit = true;

        try {
            editor = workspaceRoot.getCommitEditor("ModeShape commit", null);
            editor.openRoot(-1);

            for (ChangeCommand<PathNode> command : commands) {
                if (command == null) continue;
                SvnCommand svnCommand = (SvnCommand)command;
                svnCommand.setEditor(editor);
                svnCommand.apply();
            }
        } catch (SVNException ex) {
            commit = false;
            throw new IllegalStateException(ex);
        } finally {
            if (editor != null) {
                try {
                    editor.closeDir();
                } catch (SVNException ignore) {

                }
            }
        }
        assert editor != null;
        if (commit) {
            try {
                SVNCommitInfo info = editor.closeEdit();
                if (info.getErrorMessage() != null) {
                    throw new IllegalStateException(info.getErrorMessage().getFullMessage());
                }
            } catch (SVNException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    protected class SvnCommand implements ChangeCommand<PathNode> {
        protected ISVNEditor editor;
        private final ScmAction action;

        protected SvnCommand( ScmAction action ) {
            this.action = action;
        }

        public void setEditor( ISVNEditor editor ) {
            this.editor = editor;
        }

        @Override
        public void apply() {
            assert editor != null;
            try {
                action.applyAction(editor);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " for " + action.toString();
        }

    }

    protected class SvnPutFileCommand extends SvnCommand {
        public SvnPutFileCommand( String parentPath,
                                  String fileName,
                                  byte[] content ) {
            super(new AddFile(parentPath, fileName, content));
        }
    }

    protected class SvnPutContentCommand extends SvnCommand {
        public SvnPutContentCommand( String parentPath,
                                     String fileName,
                                     byte[] oldcontent,
                                     byte[] content ) {
            super(new UpdateFile(parentPath, fileName, oldcontent, content));
        }
    }

    protected class SvnPutFolderCommand extends SvnCommand {
        public SvnPutFolderCommand( String parentPath,
                                    String childPath ) {
            super(new AddDirectory(parentPath, childPath));
        }
    }

    protected class SvnRemoveCommand extends SvnCommand {
        public SvnRemoveCommand( String path ) {
            super(new DeleteEntry(path));
        }
    }

    protected class SvnCompositeCommand extends SvnCommand {
        List<SvnCommand> commands;

        protected SvnCompositeCommand( List<SvnCommand> commands ) {
            super(null);

            this.commands = commands;
        }

        @Override
        public void apply() {
            for (SvnCommand command : commands) {
                command.setEditor(editor);
                command.apply();
            }
        }

        @Override
        public String toString() {
            return commands.toString();
        }
    }
}
