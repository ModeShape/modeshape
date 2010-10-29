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
package org.modeshape.web.jcr.rest.client;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jcr.nodetype.NodeType;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>MockRestExecutor</code> class is a test <code>IRestClient</code> implementation that does nothing.
 */
public final class MockRestClient implements IRestClient {

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getRepositories(org.modeshape.web.jcr.rest.client.domain.Server)
     */
    public Collection<Repository> getRepositories( Server server ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getUrl(java.io.File, java.lang.String,
     *      org.modeshape.web.jcr.rest.client.domain.Workspace)
     */
    public URL getUrl( File file,
                       String path,
                       Workspace workspace ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getWorkspaces(org.modeshape.web.jcr.rest.client.domain.Repository)
     */
    public Collection<Workspace> getWorkspaces( Repository repository ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#publish(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status publish( Workspace workspace,
                           String path,
                           File file ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#unpublish(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     */
    public Status unpublish( Workspace workspace,
                             String path,
                             File file ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#query(Workspace, String, String)
     */
    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement,
                                 int offset,
                                 int limit ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#query(Workspace, String, String, int, int)
     */
    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#query(org.modeshape.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.lang.String, int, int, java.util.Map)
     */
    @Override
    public List<QueryRow> query( Workspace workspace,
                                 String language,
                                 String statement,
                                 int offset,
                                 int limit,
                                 Map<String, String> variables ) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.IRestClient#getNodeTypes(org.modeshape.web.jcr.rest.client.domain.Repository)
     */
    @Override
    public Map<String, NodeType> getNodeTypes( Repository repository ) throws Exception {
        return null;
    }
}
