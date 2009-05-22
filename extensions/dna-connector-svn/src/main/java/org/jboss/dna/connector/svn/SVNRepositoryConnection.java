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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * The defaultRepository connection to a SVN Repository instance.
 * 
 * @author Serge Pagop
 */
public class SVNRepositoryConnection implements RepositoryConnection {

    private final String sourceName;
    private final CachePolicy cachePolicy;
    private final SVNRepository defaultWorkspace;
    private final boolean updatesAllowed;
    private final Set<String> availableWorkspaceNames;
    private final boolean creatingWorkspacesAllowed;
    private final RepositoryAccessData accessData;
    
    /**
     * default workspace must can be a root repository or any folders from the root directory.
     * available workspace names must consist of URLs from repository folders.
     * 
     * @param sourceName
     * @param defaultWorkspace
     * @param availableWorkspaceNames
     * @param creatingWorkspacesAllowed
     * @param cachePolicy
     * @param updatesAllowed
     * @param accessData
     */
    public SVNRepositoryConnection( String sourceName,
                                    SVNRepository defaultWorkspace,
                                    Set<String> availableWorkspaceNames,
                                    boolean creatingWorkspacesAllowed,
                                    CachePolicy cachePolicy,
                                    boolean updatesAllowed, RepositoryAccessData accessData ) {

        CheckArg.isNotNull(defaultWorkspace, "defaultWorkspace");
        CheckArg.isNotEmpty(sourceName, "sourceName");
        assert availableWorkspaceNames != null;
        assert accessData != null;
        
        // Check if the default workspace is a folder.
        SVNNodeKind nodeKind = null;
        try {
            nodeKind = defaultWorkspace.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "No entry at URL ''{0}''",
                                                               defaultWorkspace.getLocation().getPath());
                throw new SVNException(error);
            } else if (nodeKind == SVNNodeKind.UNKNOWN) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "Entry at URL ''{0}'' is a file while directory was expected",
                                                               defaultWorkspace.getLocation().getPath());
                throw new SVNException(error);
            } else if (nodeKind == SVNNodeKind.FILE) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                               "Entry at URL ''{0}'' is a file while directory was expected",
                                                               defaultWorkspace.getLocation().getPath());
                throw new SVNException(error);
            }
        } catch (SVNException e) {
            // deal with the exception
            throw new RuntimeException(e);
        }

        this.sourceName = sourceName;
        this.cachePolicy = cachePolicy;
        this.defaultWorkspace = defaultWorkspace;
        this.updatesAllowed = updatesAllowed;
        this.availableWorkspaceNames = availableWorkspaceNames;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        this.accessData = accessData;
    }

    SVNRepository getDefaultWorkspace() {
        return defaultWorkspace;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        try {
            this.defaultWorkspace.getRepositoryRoot(true);
        } catch (SVNException e) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
        // do not care about.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( final ExecutionContext context,
                         final Request request ) throws RepositorySourceException {


        RequestProcessor processor = new SVNRepositoryRequestProcessor(sourceName, defaultWorkspace,
                                                                       availableWorkspaceNames, creatingWorkspacesAllowed,
                                                                       context, updatesAllowed, accessData);
        try {
            processor.process(request);
        } finally {
            processor.close();
        }
    }

    /**
     * @return the accessData
     */
    public RepositoryAccessData getAccessData() {
        return accessData;
    }
}
