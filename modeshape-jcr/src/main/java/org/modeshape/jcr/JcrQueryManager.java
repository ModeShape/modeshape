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
package org.modeshape.jcr;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.query.QueryManager;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.JcrTypeSystem;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.QueryObjectModel;
import org.modeshape.jcr.query.model.QueryObjectModelFactory;
import org.modeshape.jcr.query.model.SelectQuery;
import org.modeshape.jcr.query.model.SetQuery;
import org.modeshape.jcr.query.model.SetQueryObjectModel;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.parse.QueryParser;
import org.modeshape.jcr.query.parse.QueryParsers;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFactories;

/**
 * Place-holder implementation of {@link QueryManager} interface.
 */
@Immutable
class JcrQueryManager implements QueryManager {

    public static final int MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES = Integer.MAX_VALUE;

    private final JcrSession session;
    private final JcrQueryContext context;
    private final JcrTypeSystem typeSystem;
    private final QueryObjectModelFactory factory;

    JcrQueryManager( JcrSession session ) {
        this.session = session;
        this.context = new SessionQueryContext(this.session);
        this.typeSystem = new SessionTypeSystem(this.session);
        this.factory = new QueryObjectModelFactory(this.context);
    }

    @Override
    public org.modeshape.jcr.api.query.qom.QueryObjectModelFactory getQOMFactory() {
        return factory;
    }

    @Override
    public Query createQuery( String statement,
                              String language ) throws InvalidQueryException, RepositoryException {
        CheckArg.isNotNull(statement, "statement");
        CheckArg.isNotNull(language, "language");
        return createQuery(statement, language, null);
    }

    /**
     * Creates a new JCR {@link Query} by specifying the query expression itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param expression the original query expression as supplied by the client; may not be null
     * @param language the language in which the expression is represented; may not be null
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     * @return query the JCR query object; never null
     * @throws InvalidQueryException if expression is invalid or language is unsupported
     * @throws RepositoryException if the session is no longer live
     */
    public org.modeshape.jcr.api.query.Query createQuery( String expression,
                                                          String language,
                                                          Path storedAtPath ) throws InvalidQueryException, RepositoryException {
        session.checkLive();
        // Look for a parser for the specified language ...
        QueryParsers queryParsers = session.repository().runningState().queryParsers();
        QueryParser parser = queryParsers.getParserFor(language);
        if (parser == null) {
            Set<String> languages = queryParsers.getLanguages();
            throw new InvalidQueryException(JcrI18n.invalidQueryLanguage.text(language, languages));
        }
        try {
            // Parsing must be done now ...
            QueryCommand command = parser.parseQuery(expression, typeSystem);
            if (command == null) {
                // The query is not well-formed and cannot be parsed ...
                throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression));
            }
            // Set up the hints ...
            PlanHints hints = new PlanHints();
            hints.showPlan = true;
            hints.hasFullTextSearch = true; // always include the score
            hints.validateColumnExistance = false; // see MODE-1055
            if (parser.getLanguage().equals(QueryLanguage.JCR_SQL2)) {
                hints.qualifyExpandedColumnNames = true;
            }
            return resultWith(expression, parser.getLanguage(), command, hints, storedAtPath);
        } catch (ParsingException e) {
            // The query is not well-formed and cannot be parsed ...
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression, reason));
        } catch (org.modeshape.jcr.query.parse.InvalidQueryException e) {
            // The query was parsed, but there is an error in the query
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryInLanguageIsNotValid.text(language, expression, reason));
        }
    }

    /**
     * Creates a new JCR {@link Query} by specifying the query expression itself, the language in which the query is stated, the
     * {@link QueryCommand} representation. This method is more efficient than {@link #createQuery(String, String, Path)} if the
     * QueryCommand is created directly.
     * 
     * @param command the query command; may not be null
     * @return query the JCR query object; never null
     * @throws InvalidQueryException if expression is invalid or language is unsupported
     * @throws RepositoryException if the session is no longer live
     */
    public Query createQuery( QueryCommand command ) throws InvalidQueryException, RepositoryException {
        session.checkLive();
        if (command == null) {
            // The query is not well-formed and cannot be parsed ...
            throw new InvalidQueryException(JcrI18n.queryInLanguageIsNotValid.text(QueryLanguage.JCR_SQL2, command));
        }
        // Produce the expression string ...
        String expression = Visitors.readable(command);
        try {
            // Parsing must be done now ...
            PlanHints hints = new PlanHints();
            hints.showPlan = true;
            hints.hasFullTextSearch = true; // always include the score
            hints.qualifyExpandedColumnNames = true; // always qualify expanded names with the selector name in JCR-SQL2
            return resultWith(expression, QueryLanguage.JCR_SQL2, command, hints, null);
        } catch (org.modeshape.jcr.query.parse.InvalidQueryException e) {
            // The query was parsed, but there is an error in the query
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryInLanguageIsNotValid.text(QueryLanguage.JCR_SQL2, expression, reason));
        }
    }

    @Override
    public org.modeshape.jcr.api.query.Query getQuery( Node node ) throws InvalidQueryException, RepositoryException {
        AbstractJcrNode jcrNode = CheckArg.getInstanceOf(node, AbstractJcrNode.class, "node");

        // Check the type of the node ...
        JcrNodeType nodeType = jcrNode.getPrimaryNodeType();
        if (!nodeType.getInternalName().equals(JcrNtLexicon.QUERY)) {
            NamespaceRegistry registry = session.context().getNamespaceRegistry();
            throw new InvalidQueryException(JcrI18n.notStoredQuery.text(jcrNode.path().getString(registry)));
        }

        // These are both mandatory properties for nodes of nt:query
        String statement = jcrNode.getProperty(JcrLexicon.STATEMENT).getString();
        String language = jcrNode.getProperty(JcrLexicon.LANGUAGE).getString();

        return createQuery(statement, language, jcrNode.path());
    }

    @Override
    public String[] getSupportedQueryLanguages() {
        // Make a defensive copy ...
        Set<String> languages = session.repository().runningState().queryParsers().getLanguages();
        return languages.toArray(new String[languages.size()]);
    }

    protected org.modeshape.jcr.api.query.Query resultWith( String expression,
                                                            String language,
                                                            QueryCommand command,
                                                            PlanHints hints,
                                                            Path storedAtPath ) {
        if (command instanceof SelectQuery) {
            SelectQuery query = (SelectQuery)command;
            return new QueryObjectModel(context, expression, language, query, hints, storedAtPath);
        }
        if (command instanceof SetQuery) {
            SetQuery query = (SetQuery)command;
            return new SetQueryObjectModel(this.context, expression, language, query, hints, storedAtPath);
        }
        JcrQueryContext context = new SessionQueryContext(session);
        return new JcrQuery(context, expression, language, command, hints, storedAtPath);
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

    protected static class SessionQueryContext implements JcrQueryContext {
        private final JcrSession session;
        private final ValueFactories factories;

        protected SessionQueryContext( JcrSession session ) {
            this.session = session;
            this.factories = session.context().getValueFactories();
        }

        @Override
        public Value createValue( int propertyType,
                                  Object value ) {
            return new JcrValue(factories, propertyType, value);
        }

        @Override
        public NodeIterator emptyNodeIterator() {
            return JcrEmptyNodeIterator.INSTANCE;
        }

        @Override
        public CancellableQuery createExecutableQuery( QueryCommand query,
                                                       PlanHints hints,
                                                       Map<String, Object> variables ) throws RepositoryException {
            session.checkLive();
            // Submit immediately to the workspace graph ...
            Schemata schemata = session.workspace().nodeTypeManager().schemata();
            ExecutionContext context = session.context();
            String workspaceName = session.workspaceName();
            JcrRepository.RunningState state = session.repository().runningState();
            RepositoryQueryManager queryManager = state.queryManager();
            RepositoryCache repoCache = state.repositoryCache();
            NodeCache nodeCache = hints.useSessionContent ? session.cache() : session.cache().getWorkspace();
            Map<String, NodeCache> overriddenNodeCaches = new HashMap<String, NodeCache>();
            overriddenNodeCaches.put(workspaceName, nodeCache);
            Set<String> workspaceNames = new HashSet<String>();
            workspaceNames.add(workspaceName);
            if (hints.includeSystemContent) workspaceNames.add(repoCache.getSystemWorkspaceName());
            return queryManager.query(context, repoCache, workspaceNames, overriddenNodeCaches, query, schemata, hints, variables);
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return session.context();
        }

        @Override
        public Node getNode( Location location ) {
            if (location != null) {
                try {
                    return session.node(location.getKey(), null);
                } catch (ItemNotFoundException e) {
                    // Must have been deleted from storage but not yet from the indexes ...
                }
            }
            return null;
        }

        @Override
        public Schemata getSchemata() {
            return session.nodeTypeManager().schemata();
        }

        @Override
        public boolean isLive() {
            return session.isLive();
        }

        @Override
        public Node store( String absolutePath,
                           Name nodeType,
                           String language,
                           String statement ) throws RepositoryException {
            session.checkLive();
            NamespaceRegistry namespaces = session.namespaces();

            Path path;
            try {
                path = session.pathFactory().create(absolutePath);
            } catch (IllegalArgumentException iae) {
                throw new RepositoryException(JcrI18n.invalidPathParameter.text("absolutePath", absolutePath));
            }
            Path parentPath = path.getParent();

            Node parentNode = session.node(parentPath);

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            Node queryNode = parentNode.addNode(path.relativeTo(parentPath).getString(namespaces),
                                                JcrNtLexicon.QUERY.getString(namespaces));

            queryNode.setProperty(JcrLexicon.LANGUAGE.getString(namespaces), language);
            queryNode.setProperty(JcrLexicon.STATEMENT.getString(namespaces), statement);

            return queryNode;
        }

        @Override
        public void recordDuration( long nanos,
                                    TimeUnit unit,
                                    String query,
                                    String language ) {
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("query", query);
            payload.put("language", language);
            session.repository().statistics().recordDuration(DurationMetric.QUERY_EXECUTION_TIME, nanos, unit, payload);
        }

    }

    protected static class SessionTypeSystem implements JcrTypeSystem {
        protected final JcrSession session;
        protected final TypeSystem delegate;

        protected SessionTypeSystem( JcrSession session ) {
            this.session = session;
            this.delegate = session.context().getValueFactories().getTypeSystem();
        }

        @Override
        public Set<String> getTypeNames() {
            return delegate.getTypeNames();
        }

        @Override
        public TypeFactory<?> getTypeFactory( Object prototype ) {
            return delegate.getTypeFactory(prototype);
        }

        @Override
        public TypeFactory<?> getTypeFactory( String typeName ) {
            return delegate.getTypeFactory(typeName);
        }

        @Override
        public TypeFactory<String> getStringFactory() {
            return delegate.getStringFactory();
        }

        @Override
        public TypeFactory<?> getReferenceFactory() {
            return delegate.getReferenceFactory();
        }

        @Override
        public TypeFactory<?> getPathFactory() {
            return delegate.getPathFactory();
        }

        @Override
        public TypeFactory<Long> getLongFactory() {
            return delegate.getLongFactory();
        }

        @Override
        public TypeFactory<Double> getDoubleFactory() {
            return delegate.getDoubleFactory();
        }

        @Override
        public String getDefaultType() {
            return delegate.getDefaultType();
        }

        @Override
        public Comparator<Object> getDefaultComparator() {
            return delegate.getDefaultComparator();
        }

        @Override
        public TypeFactory<BigDecimal> getDecimalFactory() {
            return delegate.getDecimalFactory();
        }

        @Override
        public TypeFactory<?> getDateTimeFactory() {
            return delegate.getDateTimeFactory();
        }

        @Override
        public String getCompatibleType( String type1,
                                         String type2 ) {
            return delegate.getCompatibleType(type1, type2);
        }

        @Override
        public TypeFactory<Boolean> getBooleanFactory() {
            return delegate.getBooleanFactory();
        }

        @Override
        public TypeFactory<?> getBinaryFactory() {
            return delegate.getBinaryFactory();
        }

        @Override
        public String asString( Object value ) {
            return delegate.asString(value);
        }

        @Override
        public JcrValueFactory getValueFactory() {
            return session.valueFactory();
        }
    }
}
