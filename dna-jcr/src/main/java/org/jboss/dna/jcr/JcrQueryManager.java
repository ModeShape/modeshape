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
package org.jboss.dna.jcr;

import java.util.Arrays;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;

/**
 * Place-holder implementation of {@link QueryManager} interface.
 */
@Immutable
class JcrQueryManager implements QueryManager {

    private final JcrSession session;

    JcrQueryManager( JcrSession session ) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#createQuery(java.lang.String, java.lang.String)
     */
    public Query createQuery( String statement,
                              String language ) throws InvalidQueryException {
        return this.createQuery(statement, language, null);
    }

    /**
     * Creates a new query by specifying the query statement itself, the language in which the query is stated, and, optionally,
     * the node from which the query was loaded. If the query statement is syntactically invalid, given the language specified, an
     * {@code InvalidQueryException} is thrown. The language must be a string from among those returned by {@code
     * QueryManager#getSupportedQueryLanguages()}; if it is not, then an {@code InvalidQueryException} is thrown.
     * 
     * @param statement
     * @param language
     * @param storedNode
     * @return A {@code Query} object
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @see javax.jcr.query.QueryManager#createQuery(java.lang.String, java.lang.String)
     */
    private Query createQuery( String statement,
                               String language,
                               AbstractJcrNode storedNode ) throws InvalidQueryException {
        if (Query.XPATH.equals(language)) {
            return new XPathQuery(this.session, statement, storedNode);
        }
        throw new InvalidQueryException(JcrI18n.invalidQueryLanguage.text(language, Arrays.asList(getSupportedQueryLanguages())));

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#getQuery(javax.jcr.Node)
     */
    public Query getQuery( Node node ) throws InvalidQueryException, RepositoryException {
        assert node instanceof AbstractJcrNode;

        JcrNodeType nodeType = (JcrNodeType)node.getPrimaryNodeType();
        if (!nodeType.getInternalName().equals(JcrNtLexicon.QUERY)) {
            throw new InvalidQueryException(JcrI18n.notStoredQuery.text());
        }

        // These are both mandatory properties for nodes of nt:query
        NamespaceRegistry registry = session.getExecutionContext().getNamespaceRegistry();
        String statement = node.getProperty(JcrLexicon.STATEMENT.getString(registry)).getString();
        String language = node.getProperty(JcrLexicon.LANGUAGE.getString(registry)).getString();

        return createQuery(statement, language, (AbstractJcrNode)node);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages() {
        return new String[] {Query.XPATH};
    }

    @Immutable
    protected abstract class AbstractJcrQuery implements Query {
        private final JcrSession session;
        private final String language;
        private final String statement;
        private final Path storedPath;

        protected AbstractJcrQuery( JcrSession session,
                                    String statement,
                                    String language,
                                    AbstractJcrNode storedNode ) {
            assert session != null;
            assert statement != null;
            assert language != null;

            this.session = session;
            this.language = language;
            this.statement = statement;

            try {
                this.storedPath = storedNode != null ? storedNode.path() : null;
            } catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Query#execute()
         */
        public abstract QueryResult execute();

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
            if (storedPath == null) {
                throw new ItemNotFoundException(JcrI18n.notStoredQuery.text());
            }
            return storedPath.getString(session.getExecutionContext().getNamespaceRegistry());
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Query#storeAsNode(java.lang.String)
         */
        public Node storeAsNode( java.lang.String absPath )
            throws PathNotFoundException, ConstraintViolationException, RepositoryException {
            NamespaceRegistry namespaces = this.session.namespaces();
            
            Path path = session.getExecutionContext().getValueFactories().getPathFactory().create(absPath);
            Path parentPath = path.getParent();

            Node parentNode = session.getNode(parentPath);
            Node queryNode = parentNode.addNode(path.relativeTo(parentPath).getString(namespaces),
                               JcrNtLexicon.QUERY.getString(namespaces));
            
            queryNode.setProperty(JcrLexicon.LANGUAGE.getString(namespaces), this.language);
            queryNode.setProperty(JcrLexicon.STATEMENT.getString(namespaces), this.statement);
            
            return queryNode;
        }

    }

    @Immutable
    protected class XPathQuery extends AbstractJcrQuery {

        XPathQuery( JcrSession session,
                    String statement,
                    AbstractJcrNode storedNode ) {
            super(session, statement, Query.XPATH, storedNode);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.JcrQueryManager.AbstractJcrQuery#execute()
         */
        @Override
        public QueryResult execute() {
            throw new UnsupportedOperationException();
        }
    }

}
