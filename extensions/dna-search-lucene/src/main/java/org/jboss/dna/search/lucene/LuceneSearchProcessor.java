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
package org.jboss.dna.search.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.process.FullTextSearchResultColumns;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.FullTextSearchRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.LockBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.UnlockBranchRequest;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.search.SearchEngineProcessor;
import org.jboss.dna.graph.search.AbstractSearchEngine.Workspaces;
import org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor;

/**
 * Abstract {@link SearchEngineProcessor} implementation for the {@link LuceneSearchEngine}.
 */
@NotThreadSafe
public class LuceneSearchProcessor extends AbstractLuceneProcessor<LuceneSearchWorkspace, LuceneSearchSession> {

    protected static final Columns FULL_TEXT_RESULT_COLUMNS = new FullTextSearchResultColumns();

    protected LuceneSearchProcessor( String sourceName,
                                     ExecutionContext context,
                                     Workspaces<LuceneSearchWorkspace> workspaces,
                                     Observer observer,
                                     DateTime now,
                                     boolean readOnly ) {
        super(sourceName, context, workspaces, observer, now, readOnly);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor#createSessionFor(org.jboss.dna.graph.search.SearchEngineWorkspace)
     */
    @Override
    protected LuceneSearchSession createSessionFor( LuceneSearchWorkspace workspace ) {
        return new LuceneSearchSession(workspace, this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor#fullTextFieldName(java.lang.String)
     */
    @Override
    protected String fullTextFieldName( String propertyName ) {
        return LuceneSearchWorkspace.FULL_TEXT_PREFIX + propertyName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        LuceneSearchSession session = getSessionFor(request, request.workspace(), false);
        if (session == null) return;
        try {
            List<Object[]> results = new ArrayList<Object[]>();
            Statistics statistics = session.search(request.expression(), results, request.maxResults(), request.offset());
            request.setResults(FULL_TEXT_RESULT_COLUMNS, results, statistics);
        } catch (ParseException e) {
            request.setError(e);
        } catch (IOException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        LuceneSearchSession session = getSessionFor(request, request.workspaceName(), true);
        if (session == null) return;
        request.setActualWorkspaceName(session.getWorkspaceName());
        try {
            request.setActualRootLocation(session.getLocationForRoot());
        } catch (IOException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        Set<String> names = new HashSet<String>();
        for (LuceneSearchWorkspace workspace : workspaces.getWorkspaces()) {
            names.add(workspace.getWorkspaceName());
        }
        request.setAvailableWorkspaceNames(names);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        // Get the session to the workspace ...
        LuceneSearchSession session = getSessionFor(request, request.inWorkspace(), true);
        if (session == null) return;

        // We make a big assumption here: the CreateNodeRequests created by the SearchEngineProcessor have the
        // actual locations set ...
        Location location = request.getActualLocationOfNode();
        assert location != null;

        Logger logger = Logger.getLogger(getClass());
        if (logger.isTraceEnabled()) {
            logger.trace("indexing {0} in workspace \"{1}\"",
                         location.getString(getExecutionContext().getNamespaceRegistry()),
                         request.inWorkspace());
        }

        try {
            session.setOrReplaceProperties(location, request.properties());
            session.recordChange();
        } catch (IOException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        // Get the session to the workspace ...
        LuceneSearchSession session = getSessionFor(request, request.inWorkspace(), true);
        if (session == null) return;

        Location location = request.getActualLocationOfNode();
        assert location != null;

        try {
            // We make a big assumption here: the UpdatePropertiesRequest created by the SearchEngineProcessor have the
            // actual locations set ...
            session.setOrReplaceProperties(location, request.properties().values());
            session.recordChange();
        } catch (IOException e) {
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
        LuceneSearchSession session = getSessionFor(request, request.inWorkspace());
        if (session == null) return;

        Path path = request.at().getPath();
        assert !readOnly;
        try {
            // Create a query to find all the nodes at or below the specified path (this efficiently handles the root path) ...
            Query query = session.findAllNodesAtOrBelow(path);
            // Now delete the documents from each index using this query, which we can reuse ...
            session.getContentWriter().deleteDocuments(query);
            session.recordChanges(100);
        } catch (FileNotFoundException e) {
            // There are no index files yet, so nothing to delete ...
        } catch (IOException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        LuceneSearchWorkspace workspace = getWorkspace(request, request.workspaceName(), false);
        if (workspace == null) return;
        try {
            LuceneSearchSession session = getSessionFor(request, workspace.getWorkspaceName());
            request.setActualRootLocation(session.getLocationForRoot());
            workspace.destroy(getExecutionContext());
            session.recordChanges(LuceneSearchWorkspace.CHANGES_BEFORE_OPTIMIZATION + 100);
        } catch (IOException e) {
            request.setError(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.LockBranchRequest)
     */
    @Override
    public void process( LockBranchRequest request ) {
        request.setActualLocation(request.at());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UnlockBranchRequest)
     */
    @Override
    public void process( UnlockBranchRequest request ) {
        request.setActualLocation(request.at());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        super.processUnknownRequest(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        super.processUnknownRequest(request);
    }
}
