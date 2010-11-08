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

import java.io.File;
import java.util.LinkedList;
import java.util.UUID;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.connector.base.PathTransaction;
import org.modeshape.graph.connector.base.Processor;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.connector.base.Transaction;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * Implementation of {@code Repository} that provides access to an underlying file system. This repository only natively supports
 * nodes of primary types {@link JcrNtLexicon#FOLDER nt:folder}, {@link JcrNtLexicon#FILE nt:file}, and
 * {@link JcrNtLexicon#RESOURCE mode:resource}, although the {@link CustomPropertiesFactory} allows for the addition of mixin
 * types to any and all primary types.
 */
public class FileSystemRepository extends Repository<PathNode, FileSystemWorkspace> {

    protected final FileSystemSource source;
    private File repositoryRoot;

    public FileSystemRepository( FileSystemSource source ) {
        super(source);

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

        super.initialize();
    }

    private void createDirectory( File directory ) {
        File parent = directory.getParentFile();

        if (!parent.exists()) {
            createDirectory(parent);
        } else if (parent.isFile()) {
            I18n msg = FileSystemI18n.ancestorInPathIsFile;
            throw new InvalidWorkspaceException(msg.text(source.getName(), parent.getPath()));
        }

        if (!directory.mkdir()) {
            I18n msg = FileSystemI18n.couldNotCreateDirectory;
            throw new InvalidWorkspaceException(msg.text(source.getName(), directory.getPath()));
        }
    }

    /**
     * @param workspaceName the name of the workspace for which the root directory should be returned
     * @return the directory that maps to the root node in the named workspace; may be null if the directory does not exist, is a
     *         file, or cannot be read.
     */
    protected File getWorkspaceDirectory( String workspaceName ) {
        if (workspaceName == null) workspaceName = source.getDefaultWorkspaceName();

        File directory = this.repositoryRoot == null ? new File(workspaceName) : new File(repositoryRoot, workspaceName);
        if (!directory.exists()) {
            createDirectory(directory.getAbsoluteFile());
        }

        if (!directory.canRead()) {
            I18n msg = FileSystemI18n.pathForWorkspaceCannotBeRead;
            throw new InvalidWorkspaceException(msg.text(getSourceName(), directory.getAbsolutePath(), workspaceName));
        }

        if (!directory.isDirectory()) {
            I18n msg = FileSystemI18n.pathForWorkspaceIsNotDirectory;
            throw new InvalidWorkspaceException(msg.text(getSourceName(), directory.getAbsolutePath(), workspaceName));
        }

        return directory;
    }

    @Override
    public FileSystemTransaction startTransaction( ExecutionContext context,
                                                   boolean readonly ) {
        return new FileSystemTransaction(this, source.getRootNodeUuidObject());
    }

    @Override
    public RequestProcessor createRequestProcessor( Transaction<PathNode, FileSystemWorkspace> txn ) {
        RepositoryContext repositoryContext = this.source.getRepositoryContext();
        Observer observer = repositoryContext != null ? repositoryContext.getObserver() : null;
        return new FileSystemProcessor(txn, this, observer, source.areUpdatesAllowed());
    }

    /**
     * Implementation of the {@link PathTransaction} interface for the file system connector
     */
    class FileSystemTransaction extends PathTransaction<PathNode, FileSystemWorkspace> {

        public FileSystemTransaction( FileSystemRepository repository,
                                      UUID rootNodeUuid ) {
            super(repository, rootNodeUuid);
        }

        @Override
        protected PathNode createNode( Segment name,
                                       Path parentPath,
                                       Iterable<Property> properties ) {
            return new PathNode(null, parentPath, name, properties, new LinkedList<Segment>());
        }

        @Override
        public boolean destroyWorkspace( FileSystemWorkspace workspace ) throws InvalidWorkspaceException {
            return true;
        }

        @Override
        public FileSystemWorkspace getWorkspace( String name,
                                                 FileSystemWorkspace originalToClone ) throws InvalidWorkspaceException {
            FileSystemRepository repository = FileSystemRepository.this;

            if (originalToClone != null) {
                return new FileSystemWorkspace(name, originalToClone, repository.getWorkspaceDirectory(name));
            }
            return new FileSystemWorkspace(repository, name);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.base.PathTransaction#validateNode(org.modeshape.graph.connector.base.PathWorkspace,
         *      org.modeshape.graph.connector.base.PathNode)
         */
        @Override
        protected void validateNode( FileSystemWorkspace workspace,
                                     PathNode node ) {
            workspace.validate(node);
        }


        @Override
        public PathNode addChild( FileSystemWorkspace workspace,
                                  PathNode parent,
                                  Name name,
                                  int index,
                                  UUID uuid,
                                  Iterable<Property> properties ) {
            String newFileName = name.getLocalName();
            if (!source.filenameFilter(true).accept(null, newFileName)) {
                throw new InvalidPathException(FileSystemI18n.cannotCreateFileAsExcludedPattern.text(newFileName,
                                                                                                     workspace.getName()));
            }

            return super.addChild(workspace, parent, name, index, uuid, properties);
        }

        @Override
        public Location addChild( FileSystemWorkspace workspace,
                                  PathNode parent,
                                  PathNode originalChild,
                                  PathNode beforeOtherChild,
                                  Name desiredName ) {

            if (desiredName != null) {
                String newFileName = desiredName.getLocalName();
                if (!source.filenameFilter(true).accept(null, newFileName)) {
                    String oldFileName = originalChild.getName().getString(this.context.getNamespaceRegistry());
                    throw new InvalidPathException(FileSystemI18n.cannotRenameFileToExcludedPattern.text(oldFileName,
                                                                                                         newFileName,
                                                                                                         workspace.getName()));
                }
            }

            return super.addChild(workspace, parent, originalChild, beforeOtherChild, desiredName);
        }
    }

    /**
     * Custom {@link Processor} for the file system connector. This processor throws accurate exceptions on attempts to reorder
     * nodes, since the file system connector does not support node ordering. Otherwise, it provides default behavior.
     */
    class FileSystemProcessor extends Processor<PathNode, FileSystemWorkspace> {

        private FileSystemTransaction txn;

        public FileSystemProcessor( Transaction<PathNode, FileSystemWorkspace> txn,
                                    Repository<PathNode, FileSystemWorkspace> repository,
                                    Observer observer,
                                    boolean updatesAllowed ) {
            super(txn, repository, observer, updatesAllowed);
            this.txn = (FileSystemTransaction)txn;
        }

        @Override
        public void process( MoveBranchRequest request ) {
            if (request.before() != null) {
                I18n msg = FileSystemI18n.nodeOrderingNotSupported;
                throw new InvalidRequestException(msg.text(source.getName()));
            }
            super.process(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.base.Processor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
         */
        @Override
        public void process( VerifyWorkspaceRequest request ) {
            FileSystemWorkspace workspace = getWorkspace(request, request.workspaceName());
            if (workspace != null) {
                request.setActualRootLocation(txn.getRootLocation());
                request.setActualWorkspaceName(workspace.getName());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#absoluteMaximumDepthForBranchReads()
         */
        @Override
        protected int absoluteMaximumDepthForBranchReads() {
            // never read more than two levels from a file system repository, as the file content can get too big
            return 1;
        }
    }
}
