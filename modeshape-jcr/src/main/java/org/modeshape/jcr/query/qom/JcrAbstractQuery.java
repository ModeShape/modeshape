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
package org.modeshape.jcr.query.qom;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.query.JcrQueryContext;

/**
 * Abstract implementation of JCR's {@link Query} interface.
 */
public abstract class JcrAbstractQuery implements Query {

    protected final JcrQueryContext context;
    protected final String language;
    protected final String statement;
    private Path storedAtPath;

    /**
     * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param context the context that was used to create this query and that will be used to execute this query; may not be null
     * @param statement the original statement as supplied by the client; may not be null
     * @param language the language obtained from the {@link QueryParser}; may not be null
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     */
    protected JcrAbstractQuery( JcrQueryContext context,
                                String statement,
                                String language,
                                Path storedAtPath ) {
        assert context != null;
        assert statement != null;
        assert language != null;
        this.context = context;
        this.language = language;
        this.statement = statement;
        this.storedAtPath = storedAtPath;
    }

    protected final JcrQueryContext context() {
        return this.context;
    }

    protected final Path pathFor( String path ) {
        return this.context.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#getLanguage()
     */
    public String getLanguage() {
        return language;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#getStatement()
     */
    public String getStatement() {
        return statement;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#getStoredQueryPath()
     */
    public String getStoredQueryPath() throws ItemNotFoundException {
        if (storedAtPath == null) {
            throw new ItemNotFoundException(JcrI18n.notStoredQuery.text());
        }
        return storedAtPath.getString(context.getExecutionContext().getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#storeAsNode(java.lang.String)
     */
    public Node storeAsNode( String absPath ) throws PathNotFoundException, ConstraintViolationException, RepositoryException {
        context.isLive();
        Node queryNode = context.store(absPath, JcrNtLexicon.QUERY, this.language, this.statement);
        this.storedAtPath = pathFor(queryNode.getPath());
        return queryNode;
    }

    protected void checkForProblems( Problems problems ) throws RepositoryException {
        if (problems.hasErrors()) {
            // Build a message with the problems ...
            StringBuilder msg = new StringBuilder();
            for (Problem problem : problems) {
                if (problem.getStatus() != Status.ERROR) continue;
                msg.append(problem.getMessageString()).append("\n");
            }
            throw new RepositoryException(msg.toString());
        }
    }
}
