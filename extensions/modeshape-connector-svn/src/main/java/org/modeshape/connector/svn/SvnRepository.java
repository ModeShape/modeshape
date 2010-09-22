package org.modeshape.connector.svn;

import java.util.LinkedList;
import java.util.Map;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.connector.base.PathTransaction;
import org.modeshape.graph.connector.base.Processor;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.connector.base.Transaction;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.MoveBranchRequest;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

public class SvnRepository extends Repository<PathNode, SvnWorkspace> {

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

    final SvnRepositorySource source() {
        return this.source;
    }

    @Override
    public SvnTransaction startTransaction( ExecutionContext context,
                                            boolean readonly ) {
        return new SvnTransaction();
    }

    /**
     * Implementation of the {@link PathTransaction} interface for the Subversion connector
     */
    class SvnTransaction extends PathTransaction<PathNode, SvnWorkspace> {

        public SvnTransaction() {
            super(SvnRepository.this, source.getRootNodeUuidObject());
        }

        @Override
        protected PathNode createNode( Segment name,
                                       Path parentPath,
                                       Iterable<Property> properties ) {
            return new PathNode(null, parentPath, name, properties, new LinkedList<Segment>());
        }

        @Override
        public boolean destroyWorkspace( SvnWorkspace workspace ) throws InvalidWorkspaceException {
            return true;
        }

        @Override
        public SvnWorkspace getWorkspace( String name,
                                          SvnWorkspace originalToClone ) throws InvalidWorkspaceException {
            SvnRepository repository = SvnRepository.this;

            if (originalToClone != null) {
                return new SvnWorkspace(name, originalToClone, repository.getWorkspaceDirectory(name));
            }
            return new SvnWorkspace(repository, getWorkspaceDirectory(name), name, source.getRootNodeUuidObject());
        }

        @Override
        protected void validateNode( SvnWorkspace workspace,
                                     PathNode node ) {
            workspace.validate(node);
        }
    }

    protected SVNRepository getWorkspaceDirectory( String workspaceName ) {
        if (workspaceName == null) workspaceName = source().getDefaultWorkspaceName();

        if (source().getRepositoryRootUrl().endsWith("/")) {
            workspaceName = source().getRepositoryRootUrl() + workspaceName;
        } else {
            workspaceName = source().getRepositoryRootUrl() + "/" + workspaceName;
        }

        SVNRepository repository = null;
        SVNRepository repos = SvnRepositoryUtil.createRepository(workspaceName, source().getUsername(), source().getPassword());
        if (SvnRepositoryUtil.isDirectory(repos, "")) {
            repository = repos;
        } else {
            return null;
        }
        return repository;
    }

    /**
     * Custom {@link Processor} for the file system connector. This processor throws accurate exceptions on attempts to reorder
     * nodes, since the file system connector does not support node ordering. Otherwise, it provides default behavior.
     */
    class SvnProcessor extends Processor<PathNode, SvnWorkspace> {

        private final NameFactory nameFactory;

        public SvnProcessor( Transaction<PathNode, SvnWorkspace> txn,
                             Repository<PathNode, SvnWorkspace> repository,
                             Observer observer,
                             boolean updatesAllowed ) {
            super(txn, repository, observer, updatesAllowed);
            this.nameFactory = txn.getContext().getValueFactories().getNameFactory();
        }

        @Override
        public void process( MoveBranchRequest request ) {
            if (request.before() != null) {
                I18n msg = SvnRepositoryConnectorI18n.nodeOrderingNotSupported;
                throw new InvalidRequestException(msg.text(source.getName()));
            }
            super.process(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#absoluteMaximumDepthForBranchReads()
         */
        @Override
        protected int absoluteMaximumDepthForBranchReads() {
            // never read more than one level from SVN, as the file content can get too big and the costs too expensive ...
            return 2;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#includeChildrenInSubgraph(org.modeshape.graph.Location,
         *      java.util.Map, boolean)
         */
        @Override
        protected boolean includeChildrenInSubgraph( Location location,
                                                     Map<Name, Property> properties,
                                                     boolean topOfSubgraph ) {
            if (topOfSubgraph) return true;
            // We never want to include the children of 'nt:file' nodes in a subgraph ...
            Property prop = properties.get(JcrLexicon.PRIMARY_TYPE);
            if (prop == null || prop.isEmpty()) return true;
            Name primaryType = nameFactory.create(prop.getFirstValue());
            if (JcrNtLexicon.FILE.equals(primaryType)) {
                // Include no children of an 'nt:file' ...
                return false;
            }
            return true;
        }
    }

}
