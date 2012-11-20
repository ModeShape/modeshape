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
package org.modeshape.connector.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.Repository;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.PageWriter;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.ReadOnlyConnector;

/**
 * A read-only {@link Connector} that accesses the content in a Git repository.
 */
public class GitConnector extends ReadOnlyConnector implements Pageable {

    private static final String GIT_CND_PATH = "org/modeshape/connector/git/git.cnd";

    private Repository repository;
    private Git git;
    private Map<String, GitFunction> functions;
    private Map<String, PageableGitFunction> pageableFunctions;
    private Values values;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        values = new Values(factories(), getContext().getBinaryStore());

        // Set up the repository instance ...
        // repository = new Repository();
        git = new Git(repository);

        // Register the different functions ...
        functions = new HashMap<String, GitFunction>();
        register(new GitRoot(this), new GitBranches(this), new GitTags(this), new GitHistory(this));

        // Register the Git-specific node types ...
        InputStream cndStream = getClass().getClassLoader().getResourceAsStream(GIT_CND_PATH);
        nodeTypeManager.registerNodeTypes(cndStream, true);

    }

    private void register( GitFunction... functions ) {
        for (GitFunction function : functions) {
            this.functions.put(function.getName(), function);
            if (function instanceof PageableGitFunction) {
                this.pageableFunctions.put(function.getName(), (PageableGitFunction)function);
            }
        }
    }

    protected DocumentWriter newDocumentWriter( String id ) {
        return super.newDocument(id);
    }

    @Override
    public void shutdown() {
        repository = null;
        git = null;
        functions = null;
    }

    @Override
    public Document getDocumentById( String id ) {
        CallSpecification callSpec = new CallSpecification(id);
        GitFunction function = functions.get(callSpec.getFunctionName());
        if (function == null) return null;
        try {
            // Set up the document writer ...
            DocumentWriter writer = newDocument(id);
            String parentId = callSpec.getParentId();
            assert parentId != null;
            writer.setParent(parentId);
            // Now call the function ...
            return function.execute(repository, git, callSpec, writer, values);
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public Document getChildren( PageKey pageKey ) {
        String id = pageKey.getParentId();
        CallSpecification callSpec = new CallSpecification(id);
        PageableGitFunction function = pageableFunctions.get(callSpec.getFunctionName());
        if (function == null) return null;
        try {
            // Set up the document writer ...
            PageWriter writer = newPageDocument(pageKey);
            // Now call the function ...
            return function.execute(repository, git, callSpec, writer, values, pageKey);
        } catch (Throwable e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public String getDocumentId( String path ) {
        // Our paths are basically used as IDs ...
        return path;
    }

    @Override
    public boolean hasDocument( String id ) {
        Document doc = getDocumentById(id);
        return doc != null;
    }

    protected final ListMode branchListMode() {
        return null;
    }

}
