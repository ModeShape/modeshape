/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.query.QueryManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.JcrTypeSystem;
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
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;

/**
 * Place-holder implementation of {@link QueryManager} interface.
 */
@Immutable
class JcrQueryManager implements QueryManager {

    protected static final Logger LOGGER = Logger.getLogger(JcrQueryManager.class);

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
    public org.modeshape.jcr.api.query.Query createQuery( String statement,
                                                          String language ) throws InvalidQueryException, RepositoryException {
        CheckArg.isNotNull(statement, "statement");
        CheckArg.isNotNull(language, "language");
        return createQuery(statement, language, null, null);
    }

    @Override
    public org.modeshape.jcr.api.query.Query createQuery( String statement, 
                                                          String language,
                                                          Locale locale ) throws InvalidQueryException, RepositoryException {
        CheckArg.isNotNull(statement, "statement");
        CheckArg.isNotNull(language, "language");
        CheckArg.isNotNull(locale, "locale");
        return createQuery(statement, language, null, locale);
    }

    /**
     * Creates a new JCR {@link Query} by specifying the query expression itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param expression the original query expression as supplied by the client; may not be null
     * @param language the language in which the expression is represented; may not be null
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     * @param locale an optional {@link Locale} instance or null if no specific locale is to be used.
     * @return query the JCR query object; never null
     * @throws InvalidQueryException if expression is invalid or language is unsupported
     * @throws RepositoryException if the session is no longer live
     */
    public org.modeshape.jcr.api.query.Query createQuery( String expression,
                                                          String language,
                                                          Path storedAtPath, 
                                                          Locale locale ) throws InvalidQueryException, RepositoryException {
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
            return resultWith(expression, parser.getLanguage(), command, hints, storedAtPath, locale);
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
     * {@link QueryCommand} representation. This method is more efficient than {@link #createQuery(String, String, Path, Locale)} if the
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
            return resultWith(expression, QueryLanguage.JCR_SQL2, command, hints, null, null);
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

        return createQuery(statement, language, jcrNode.path(), null);
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
                                                            Path storedAtPath, 
                                                            Locale locale ) {
        if (command instanceof SelectQuery) {
            SelectQuery query = (SelectQuery)command;
            return new QueryObjectModel(context, expression, language, query, hints, storedAtPath);
        }
        if (command instanceof SetQuery) {
            SetQuery query = (SetQuery)command;
            return new SetQueryObjectModel(this.context, expression, language, query, hints, storedAtPath);
        }
        JcrQueryContext context = new SessionQueryContext(session, locale);
        return new JcrQuery(context, expression, language, command, hints, storedAtPath);
    }

    protected static class SessionQueryContext implements JcrQueryContext {
        private final JcrSession session;
        private final ValueFactories factories;
        private final ExecutionContext executionContext; 
        
        protected SessionQueryContext( JcrSession session ) {
            this(session, null);
        }

        protected SessionQueryContext( JcrSession session, Locale locale ) {
            this.session = session;  
            this.executionContext = (locale == null) ? session.context() : session.context().with(locale);
            this.factories = executionContext.getValueFactories();
        }

        @Override
        public BufferManager getBufferManager() {
            return session.bufferManager();
        }

        @Override
        public String getWorkspaceName() {
            return session.workspaceName();
        }

        @Override
        public Value createValue( int propertyType,
                                  Object value ) {
            return value == null ? null : new JcrValue(factories, propertyType, value);
        }

        @Override
        public CancellableQuery createExecutableQuery( QueryCommand query,
                                                       PlanHints hints,
                                                       Map<String, Object> variables ) throws RepositoryException {
            session.checkLive();
            // Submit immediately to the workspace graph ...
            Schemata schemata = session.workspace().nodeTypeManager().schemata();
            NodeTypes nodeTypes = session.repository().nodeTypeManager().getNodeTypes();
            RepositoryIndexes indexDefns = session.repository().queryManager().getIndexes();
            String workspaceName = session.workspaceName();
            JcrRepository.RunningState state = session.repository().runningState();
            RepositoryQueryManager queryManager = state.queryManager();
            RepositoryCache repoCache = state.repositoryCache();
            NodeCache nodeCache = hints.useSessionContent ? session.cache() : session.cache().getWorkspace();
            Map<String, NodeCache> overriddenNodeCaches = new HashMap<String, NodeCache>();
            overriddenNodeCaches.put(workspaceName, nodeCache);
            Set<String> workspaceNames = null;
            if (hints.includeSystemContent) {
                workspaceNames = new LinkedHashSet<String>();
                workspaceNames.add(workspaceName);
                workspaceNames.add(repoCache.getSystemWorkspaceName());
            } else {
                workspaceNames = Collections.singleton(workspaceName);
            }
            return queryManager.query(executionContext, repoCache, workspaceNames, overriddenNodeCaches, query, schemata, indexDefns,
                                      nodeTypes, hints, variables);
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return session.context();
        }

        @Override
        public void checkValid() throws RepositoryException {
            session.checkLive();
        }

        @Override
        public Node getNode( CachedNode node ) {
            if (node == null) {
                return null;
            }
            // this *does not* check permissions because it is expected that the correct sequence already wraps the results and
            // therefore it should not be possible to return a Node/Row from a batch on which there aren't any read permissions
            return session.node(node, (AbstractJcrNode.Type)null);
        }

        @Override
        public boolean canRead( CachedNode node ) {
            if (node == null) {
                return false;
            }
            Path path = getPath(node);
            try {
                session.checkPermission(path, ModeShapePermissions.READ);
                return true;
            } catch (AccessDeniedException ade) {
                LOGGER.debug("READ access denied on '{0}'", path);
                return false;
            }
        }

        @SuppressWarnings( "deprecation" )
        @Override
        public String getUuid( CachedNode node ) {
            Node jcrNode = getNode(node);
            if (jcrNode != null) {
                try {
                    return jcrNode.getUUID();
                } catch (UnsupportedRepositoryOperationException e) {
                    return null; // it's not referenceable
                } catch (RepositoryException e) {
                    Logger.getLogger(getClass()).debug(e, "Error obtaining UUID from node");
                }
            }
            return null;
        }

        @Override
        public Path getPath( CachedNode node ) {
            return node.getPath(session.cache());
        }

        @Override
        public Name getName( CachedNode node ) {
            return node.getName(session.cache());
        }

        @Override
        public long getDepth( CachedNode node ) {
            assert node != null;
            return node.getDepth(session.cache());
        }

        @Override
        public long getChildCount( CachedNode node ) {
            assert node != null;
            return node.getChildReferences(session.cache()).size();
        }

        @Override
        public String getIdentifier( CachedNode node ) {
            // the identifier format varies depending upon the node ...
            return session.nodeIdentifier(node.getKey());
        }

        @Override
        public Node storeQuery( String absolutePath,
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

    protected static class SessionTypeSystem extends JcrTypeSystem {
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
        public TypeFactory<Reference> getReferenceFactory() {
            return delegate.getReferenceFactory();
        }

        @Override
        public TypeFactory<Path> getPathFactory() {
            return delegate.getPathFactory();
        }

        @Override
        public TypeFactory<Name> getNameFactory() {
            return delegate.getNameFactory();
        }

        @Override
        public TypeFactory<NodeKey> getNodeKeyFactory() {
            return delegate.getNodeKeyFactory();
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
        public TypeFactory<?> getCompatibleType( TypeFactory<?> type1,
                                                 TypeFactory<?> type2 ) {
            return delegate.getCompatibleType(type1, type2);
        }

        @Override
        public TypeFactory<Boolean> getBooleanFactory() {
            return delegate.getBooleanFactory();
        }

        @Override
        public TypeFactory<BinaryValue> getBinaryFactory() {
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
