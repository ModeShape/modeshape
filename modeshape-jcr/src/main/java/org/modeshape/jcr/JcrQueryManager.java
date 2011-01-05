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
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jcr.api.query.qom.QueryObjectModelFactory;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.JcrSearch;
import org.modeshape.jcr.query.JcrTypeSystem;
import org.modeshape.jcr.query.qom.JcrQueryObjectModel;
import org.modeshape.jcr.query.qom.JcrQueryObjectModelFactory;
import org.modeshape.jcr.query.qom.JcrSelectQuery;
import org.modeshape.jcr.query.qom.JcrSetQuery;
import org.modeshape.jcr.query.qom.JcrSetQueryObjectModel;

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
        this.factory = new JcrQueryObjectModelFactory(this.context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#getQOMFactory()
     */
    @Override
    public javax.jcr.query.qom.QueryObjectModelFactory getQOMFactory() {
        return factory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#createQuery(java.lang.String, java.lang.String)
     */
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
    public Query createQuery( String expression,
                              String language,
                              Path storedAtPath ) throws InvalidQueryException, RepositoryException {
        session.checkLive();
        // Look for a parser for the specified language ...
        QueryParser parser = session.repository().queryParsers().getParserFor(language);
        if (parser == null) {
            Set<String> languages = session.repository().queryParsers().getLanguages();
            throw new InvalidQueryException(JcrI18n.invalidQueryLanguage.text(language, languages));
        }
        if (parser.getLanguage().equals(FullTextSearchParser.LANGUAGE)) {
            // This is a full-text search ...
            return new JcrSearch(this.context, expression, parser.getLanguage(), storedAtPath);
        }
        try {
            // Parsing must be done now ...
            QueryCommand command = parser.parseQuery(expression, typeSystem);
            if (command == null) {
                // The query is not well-formed and cannot be parsed ...
                throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression));
            }
            PlanHints hints = new PlanHints();
            hints.showPlan = true;
            hints.hasFullTextSearch = true; // always include the score
            hints.validateColumnExistance = false; // see MODE-1055
            return resultWith(expression, parser.getLanguage(), command, hints, storedAtPath);
        } catch (ParsingException e) {
            // The query is not well-formed and cannot be parsed ...
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression, reason));
        } catch (org.modeshape.graph.query.parse.InvalidQueryException e) {
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
            return resultWith(expression, QueryLanguage.JCR_SQL2, command, hints, null);
        } catch (org.modeshape.graph.query.parse.InvalidQueryException e) {
            // The query was parsed, but there is an error in the query
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryInLanguageIsNotValid.text(QueryLanguage.JCR_SQL2, expression, reason));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#getQuery(javax.jcr.Node)
     */
    public Query getQuery( Node node ) throws InvalidQueryException, RepositoryException {
        AbstractJcrNode jcrNode = CheckArg.getInstanceOf(node, AbstractJcrNode.class, "node");

        // Check the type of the node ...
        JcrNodeType nodeType = jcrNode.getPrimaryNodeType();
        if (!nodeType.getInternalName().equals(JcrNtLexicon.QUERY)) {
            NamespaceRegistry registry = session.getExecutionContext().getNamespaceRegistry();
            throw new InvalidQueryException(JcrI18n.notStoredQuery.text(jcrNode.path().getString(registry)));
        }

        // These are both mandatory properties for nodes of nt:query
        String statement = jcrNode.getProperty(JcrLexicon.STATEMENT).getString();
        String language = jcrNode.getProperty(JcrLexicon.LANGUAGE).getString();

        return createQuery(statement, language, jcrNode.path());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     */
    public String[] getSupportedQueryLanguages() {
        // Make a defensive copy ...
        Set<String> languages = session.repository().queryParsers().getLanguages();
        return languages.toArray(new String[languages.size()]);
    }

    protected Query resultWith( String expression,
                                String language,
                                QueryCommand command,
                                PlanHints hints,
                                Path storedAtPath ) {
        if (command instanceof JcrSelectQuery) {
            JcrSelectQuery query = (JcrSelectQuery)command;
            return new JcrQueryObjectModel(context, expression, language, query, hints, storedAtPath);
        }
        if (command instanceof JcrSetQuery) {
            JcrSetQuery query = (JcrSetQuery)command;
            return new JcrSetQueryObjectModel(this.context, expression, language, query, hints, storedAtPath);
        }
        return new JcrQuery(context, expression, language, command, hints, storedAtPath);
    }

    // protected void checkForProblems( Problems problems ) throws RepositoryException {
    // if (problems.hasErrors()) {
    // // Build a message with the problems ...
    // StringBuilder msg = new StringBuilder();
    // for (Problem problem : problems) {
    // if (problem.getStatus() != Status.ERROR) continue;
    // msg.append(problem.getMessageString()).append("\n");
    // }
    // throw new RepositoryException(msg.toString());
    // }
    // }
    //
    // @NotThreadSafe
    // protected static abstract class AbstractQuery implements Query {
    // protected final JcrSession session;
    // protected final String language;
    // protected final String statement;
    // private Path storedAtPath;
    //
    // /**
    // * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
    // * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
    // * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
    // *
    // * @param session the session that was used to create this query and that will be used to execute this query; may not be
    // * null
    // * @param statement the original statement as supplied by the client; may not be null
    // * @param language the language obtained from the {@link QueryParser}; may not be null
    // * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
    // */
    // protected AbstractQuery( JcrSession session,
    // String statement,
    // String language,
    // Path storedAtPath ) {
    // assert session != null;
    // assert statement != null;
    // assert language != null;
    // this.session = session;
    // this.language = language;
    // this.statement = statement;
    // this.storedAtPath = storedAtPath;
    // }
    //
    // protected final JcrSession session() {
    // return this.session;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#getLanguage()
    // */
    // public String getLanguage() {
    // return language;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#getStatement()
    // */
    // public String getStatement() {
    // return statement;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#getStoredQueryPath()
    // */
    // public String getStoredQueryPath() throws ItemNotFoundException {
    // if (storedAtPath == null) {
    // throw new ItemNotFoundException(JcrI18n.notStoredQuery.text());
    // }
    // return storedAtPath.getString(session.getExecutionContext().getNamespaceRegistry());
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#storeAsNode(java.lang.String)
    // */
    // public Node storeAsNode( String absPath ) throws PathNotFoundException, ConstraintViolationException, RepositoryException {
    // session.checkLive();
    // NamespaceRegistry namespaces = this.session.namespaces();
    //
    // Path path;
    // try {
    // path = session.getExecutionContext().getValueFactories().getPathFactory().create(absPath);
    // } catch (IllegalArgumentException iae) {
    // throw new RepositoryException(JcrI18n.invalidPathParameter.text("absPath", absPath));
    // }
    // Path parentPath = path.getParent();
    //
    // Node parentNode = session.getNode(parentPath);
    //
    // if (!parentNode.isCheckedOut()) {
    // throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
    // }
    //
    // Node queryNode = parentNode.addNode(path.relativeTo(parentPath).getString(namespaces),
    // JcrNtLexicon.QUERY.getString(namespaces));
    //
    // queryNode.setProperty(JcrLexicon.LANGUAGE.getString(namespaces), this.language);
    // queryNode.setProperty(JcrLexicon.STATEMENT.getString(namespaces), this.statement);
    //
    // this.storedAtPath = path;
    //
    // return queryNode;
    // }
    //
    // protected void checkForProblems( Problems problems ) throws RepositoryException {
    // if (problems.hasErrors()) {
    // // Build a message with the problems ...
    // StringBuilder msg = new StringBuilder();
    // for (Problem problem : problems) {
    // if (problem.getStatus() != Status.ERROR) continue;
    // msg.append(problem.getMessageString()).append("\n");
    // }
    // throw new RepositoryException(msg.toString());
    // }
    // }
    // }
    //
    // /**
    // * Implementation of {@link Query} that represents a {@link QueryCommand} query.
    // */
    // @NotThreadSafe
    // protected static class JcrQuery extends AbstractQuery {
    // private final QueryCommand query;
    // private final PlanHints hints;
    // private final Map<String, Object> variables;
    //
    // /**
    // * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
    // * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
    // * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
    // *
    // * @param session the session that was used to create this query and that will be used to execute this query; may not be
    // * null
    // * @param statement the original statement as supplied by the client; may not be null
    // * @param language the language obtained from the {@link QueryParser}; may not be null
    // * @param query the parsed query representation; may not be null
    // * @param hints any hints that are to be used; may be null if there are no hints
    // * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
    // */
    // protected JcrQuery( JcrSession session,
    // String statement,
    // String language,
    // QueryCommand query,
    // PlanHints hints,
    // Path storedAtPath ) {
    // super(session, statement, language, storedAtPath);
    // assert query != null;
    // this.query = query;
    // this.hints = hints;
    // this.variables = null;
    // }
    //
    // /**
    // * Get the underlying and immutable Abstract Query Model representation of this query.
    // *
    // * @return the AQM representation; never null
    // */
    // public QueryCommand getAbstractQueryModel() {
    // return query;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#execute()
    // */
    // public QueryResult execute() throws RepositoryException {
    // session.checkLive();
    // // Submit immediately to the workspace graph ...
    // Schemata schemata = session.workspace().nodeTypeManager().schemata();
    // QueryResults result = session.repository().queryManager().query(session.workspace().getName(),
    // query,
    // schemata,
    // hints,
    // variables);
    // checkForProblems(result.getProblems());
    // if (Query.XPATH.equals(language)) {
    // return new XPathQueryResult(session, statement, result, schemata);
    // } else if (Query.SQL.equals(language)) {
    // return new JcrSqlQueryResult(session, statement, result, schemata);
    // }
    // return new JcrQueryResult(session, statement, result, schemata);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.lang.Object#toString()
    // */
    // @Override
    // public String toString() {
    // return language + " -> " + statement + "\n" + StringUtil.createString(' ', Math.min(language.length() - 3, 0))
    // + "AQM -> " + query;
    // }
    //
    // @Override
    // public void bindValue( String varName,
    // Value value ) throws IllegalArgumentException, RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String[] getBindVariableNames() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public void setLimit( long limit ) {
    // throw new IllegalStateException();
    // }
    //
    // @Override
    // public void setOffset( long offset ) {
    // throw new IllegalStateException();
    // }
    // }
    //
    // @NotThreadSafe
    // protected static class JcrSearch extends AbstractQuery {
    //
    // /**
    // * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
    // * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
    // * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
    // *
    // * @param session the session that was used to create this query and that will be used to execute this query; may not be
    // * null
    // * @param statement the original statement as supplied by the client; may not be null
    // * @param language the language obtained from the {@link QueryParser}; may not be null
    // * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
    // */
    // protected JcrSearch( JcrSession session,
    // String statement,
    // String language,
    // Path storedAtPath ) {
    // super(session, statement, language, storedAtPath);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Query#execute()
    // */
    // public QueryResult execute() throws RepositoryException {
    // // Submit immediately to the workspace graph ...
    // Schemata schemata = session.workspace().nodeTypeManager().schemata();
    // QueryResults result = session.repository().queryManager().search(session.workspace().getName(),
    // statement,
    // MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES,
    // 0);
    // checkForProblems(result.getProblems());
    // return new JcrQueryResult(session, statement, result, schemata);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.lang.Object#toString()
    // */
    // @Override
    // public String toString() {
    // return language + " -> " + statement;
    // }
    //
    // @Override
    // public void bindValue( String varName,
    // Value value ) throws IllegalArgumentException, RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String[] getBindVariableNames() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public void setLimit( long limit ) {
    // throw new IllegalStateException();
    // }
    //
    // @Override
    // public void setOffset( long offset ) {
    // throw new IllegalStateException();
    // }
    // }
    //
    // protected static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    // protected static final String JCR_PATH_COLUMN_NAME = "jcr:path";
    //
    // /**
    // * The results of a query. This is not thread-safe because it relies upon JcrSession, which is not thread-safe. Also,
    // although
    // * the results of a query never change, the objects returned by the iterators may vary if the session information changes.
    // */
    // @NotThreadSafe
    // public static class JcrQueryResult implements QueryResult, org.modeshape.jcr.api.query.QueryResult {
    // protected final JcrSession session;
    // protected final QueryResults results;
    // protected final Schemata schemata;
    // protected final String queryStatement;
    // private List<String> columnTypes;
    // private List<String> columnTables;
    //
    // protected JcrQueryResult( JcrSession session,
    // String query,
    // QueryResults graphResults,
    // Schemata schemata ) {
    // this.session = session;
    // this.results = graphResults;
    // this.schemata = schemata;
    // this.queryStatement = query;
    // assert this.session != null;
    // assert this.results != null;
    // assert this.schemata != null;
    // assert this.queryStatement != null;
    // }
    //
    // protected QueryResults results() {
    // return results;
    // }
    //
    // public List<String> getColumnNameList() {
    // return results.getColumns().getColumnNames();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.QueryResult#getColumnNames()
    // */
    // public String[] getColumnNames() /*throws RepositoryException*/{
    // List<String> names = getColumnNameList();
    // return names.toArray(new String[names.size()]); // make a defensive copy ...
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.api.query.QueryResult#getColumnTypes()
    // */
    // @Override
    // public String[] getColumnTypes() {
    // if (columnTypes == null) {
    // // Discover the types ...
    // columnTypes = loadColumnTypes(results.getColumns());
    // }
    // return columnTypes.toArray(new String[columnTypes.size()]);
    // }
    //
    // protected List<String> loadColumnTypes( Columns columns ) {
    // List<String> types = new ArrayList<String>(columns.getColumnCount());
    // for (Column column : columns) {
    // String typeName = null;
    // Table table = schemata.getTable(column.selectorName());
    // if (table != null) {
    // Schemata.Column typedColumn = table.getColumn(column.propertyName());
    // typeName = typedColumn.getPropertyType();
    // }
    // if (typeName == null) {
    // // Might be fabricated column, so just assume string ...
    // typeName = PropertyType.nameFromValue(PropertyType.STRING);
    // }
    // types.add(typeName);
    // }
    //
    // return types;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.api.query.QueryResult#getSelectorNames()
    // */
    // @Override
    // public String[] getSelectorNames() {
    // if (columnTables == null) {
    // // Discover the types ...
    // Columns columns = results.getColumns();
    // List<String> tables = new ArrayList<String>(columns.getColumnCount());
    // for (Column column : columns) {
    // String tableName = "";
    // Table table = schemata.getTable(column.selectorName());
    // if (table != null) tableName = table.getName().name();
    // tables.add(tableName);
    // }
    // columnTables = tables;
    // }
    // return columnTables.toArray(new String[columnTables.size()]);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.QueryResult#getNodes()
    // */
    // public NodeIterator getNodes() throws RepositoryException {
    // // Find all of the nodes in the results. We have to do this pre-emptively, since this
    // // is the only method to throw RepositoryException ...
    // final int numRows = results.getRowCount();
    // if (numRows == 0) return new JcrEmptyNodeIterator();
    //
    // final List<AbstractJcrNode> nodes = new ArrayList<AbstractJcrNode>(numRows);
    // final String selectorName = results.getColumns().getSelectorNames().get(0);
    // final int locationIndex = results.getColumns().getLocationIndex(selectorName);
    // for (Object[] tuple : results.getTuples()) {
    // Location location = (Location)tuple[locationIndex];
    // if (!session.wasRemovedInSession(location)) {
    // nodes.add(session.getNode(location.getPath()));
    // }
    // }
    // return new QueryResultNodeIterator(nodes);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.QueryResult#getRows()
    // */
    // public RowIterator getRows() /*throws RepositoryException*/{
    // // We can actually delay the loading of the nodes until the rows are accessed ...
    // final int numRows = results.getRowCount();
    // final List<Object[]> tuples = results.getTuples();
    // if (results.getColumns().getLocationCount() == 1) {
    // return new SingleSelectorQueryResultRowIterator(session, queryStatement, results, tuples.iterator(), numRows);
    // }
    // return new QueryResultRowIterator(session, queryStatement, results, tuples.iterator(), numRows);
    // }
    //
    // /**
    // * Get a description of the query plan, if requested.
    // *
    // * @return the query plan, or null if the plan was not requested
    // */
    // public String getPlan() {
    // return results.getPlan();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.lang.Object#toString()
    // */
    // @Override
    // public String toString() {
    // return results.toString();
    // }
    // }
    //
    // /**
    // * The {@link NodeIterator} implementation returned by the {@link JcrQueryResult}.
    // *
    // * @see JcrQueryResult#getNodes()
    // */
    // @NotThreadSafe
    // protected static class QueryResultNodeIterator implements NodeIterator {
    // private final Iterator<? extends Node> nodes;
    // private final int size;
    // private long position = 0L;
    //
    // protected QueryResultNodeIterator( List<? extends Node> nodes ) {
    // this.nodes = nodes.iterator();
    // this.size = nodes.size();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.NodeIterator#nextNode()
    // */
    // public Node nextNode() {
    // Node node = nodes.next();
    // ++position;
    // return node;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#getPosition()
    // */
    // public long getPosition() {
    // return position;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#getSize()
    // */
    // public long getSize() {
    // return size;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#skip(long)
    // */
    // public void skip( long skipNum ) {
    // for (long i = 0L; i != skipNum; ++i)
    // nextNode();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#hasNext()
    // */
    // public boolean hasNext() {
    // return nodes.hasNext();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#next()
    // */
    // public Object next() {
    // return nextNode();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#remove()
    // */
    // public void remove() {
    // throw new UnsupportedOperationException();
    // }
    // }
    //
    // /**
    // * The {@link RowIterator} implementation returned by the {@link JcrQueryResult}.
    // *
    // * @see JcrQueryResult#getRows()
    // */
    // @NotThreadSafe
    // protected static class QueryResultRowIterator implements RowIterator {
    // protected final List<String> columnNames;
    // private final Iterator<Object[]> tuples;
    // private final Set<String> selectorNames;
    // protected final JcrSession session;
    // protected final Columns columns;
    // protected final String query;
    // private int[] locationIndexes;
    // private long position = 0L;
    // private long numRows;
    // private Row nextRow;
    //
    // protected QueryResultRowIterator( JcrSession session,
    // String query,
    // QueryResults results,
    // Iterator<Object[]> tuples,
    // long numRows ) {
    // this.tuples = tuples;
    // this.query = query;
    // this.columns = results.getColumns();
    // this.columnNames = this.columns.getColumnNames();
    // this.session = session;
    // this.numRows = numRows;
    // this.selectorNames = new HashSet<String>(columns.getSelectorNames());
    // int i = 0;
    // locationIndexes = new int[selectorNames.size()];
    // for (String selectorName : selectorNames) {
    // locationIndexes[i++] = columns.getLocationIndex(selectorName);
    // }
    // }
    //
    // public boolean hasSelector( String selectorName ) {
    // return this.selectorNames.contains(selectorName);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.RowIterator#nextRow()
    // */
    // public Row nextRow() {
    // if (nextRow == null) {
    // // Didn't call 'hasNext()' ...
    // if (!hasNext()) {
    // throw new NoSuchElementException();
    // }
    // }
    // assert nextRow != null;
    // Row result = nextRow;
    // nextRow = null;
    // return result;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#getPosition()
    // */
    // public long getPosition() {
    // return position;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#getSize()
    // */
    // public long getSize() {
    // return numRows;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.RangeIterator#skip(long)
    // */
    // public void skip( long skipNum ) {
    // for (long i = 0L; i != skipNum; ++i) {
    // tuples.next();
    // }
    // position += skipNum;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#hasNext()
    // */
    // public boolean hasNext() {
    // if (nextRow != null) {
    // return true;
    // }
    // while (tuples.hasNext()) {
    // final Object[] tuple = tuples.next();
    // ++position;
    // try {
    // // Get the next row ...
    // nextRow = getNextRow(tuple);
    // if (nextRow != null) return true;
    // } catch (RepositoryException e) {
    // // The node could not be found in this session, so skip it ...
    // }
    // --numRows;
    // }
    // return false;
    // }
    //
    // protected Row getNextRow( Object[] tuple ) throws RepositoryException {
    // // Make sure that each node referenced by the tuple exists and is accessible ...
    // Node[] nodes = new Node[locationIndexes.length];
    // int index = 0;
    // for (int locationIndex : locationIndexes) {
    // Location location = (Location)tuple[locationIndex];
    // if (session.wasRemovedInSession(location)) {
    // // Skip this record because one of the nodes no longer exists ...
    // return null;
    // }
    // try {
    // nodes[index++] = session.getNode(location.getPath());
    // } catch (AccessDeniedException e) {
    // // No access to this node, so skip the record ...
    // return null;
    // }
    // }
    // return new MultiSelectorQueryResultRow(this, nodes, locationIndexes, tuple);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#next()
    // */
    // public Object next() {
    // return nextRow();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see java.util.Iterator#remove()
    // */
    // public void remove() {
    // throw new UnsupportedOperationException();
    // }
    // }
    //
    // /**
    // * The {@link RowIterator} implementation returned by the {@link JcrQueryResult}.
    // *
    // * @see JcrQueryResult#getRows()
    // */
    // @NotThreadSafe
    // protected static class SingleSelectorQueryResultRowIterator extends QueryResultRowIterator {
    // protected final int locationIndex;
    // protected final int scoreIndex;
    //
    // protected SingleSelectorQueryResultRowIterator( JcrSession session,
    // String query,
    // QueryResults results,
    // Iterator<Object[]> tuples,
    // long numRows ) {
    // super(session, query, results, tuples, numRows);
    // String selectorName = columns.getSelectorNames().get(0);
    // locationIndex = columns.getLocationIndex(selectorName);
    // scoreIndex = columns.getFullTextSearchScoreIndexFor(selectorName);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.JcrQueryManager.QueryResultRowIterator#getNextRow(java.lang.Object[])
    // */
    // @Override
    // protected Row getNextRow( Object[] tuple ) throws RepositoryException {
    // Location location = (Location)tuple[locationIndex];
    // if (!session.wasRemovedInSession(location)) {
    // Node node = session.getNode(location.getPath());
    // return createRow(node, tuple);
    // }
    // return null;
    // }
    //
    // protected Row createRow( Node node,
    // Object[] tuple ) {
    // return new SingleSelectorQueryResultRow(this, node, tuple);
    // }
    // }
    //
    // protected static class SingleSelectorQueryResultRow implements Row, org.modeshape.jcr.api.query.Row {
    // protected final SingleSelectorQueryResultRowIterator iterator;
    // protected final Node node;
    // protected final Object[] tuple;
    // private Value[] values = null;
    //
    // protected SingleSelectorQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
    // Node node,
    // Object[] tuple ) {
    // this.iterator = iterator;
    // this.node = node;
    // this.tuple = tuple;
    // assert this.iterator != null;
    // assert this.node != null;
    // assert this.tuple != null;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.api.query.Row#getNode(java.lang.String)
    // */
    // @Override
    // public Node getNode( String selectorName ) throws RepositoryException {
    // if (iterator.hasSelector(selectorName)) {
    // throw new RepositoryException(JcrI18n.selectorNotUsedInQuery.text(selectorName, iterator.query));
    // }
    // return node;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValue(java.lang.String)
    // */
    // public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
    // return node.getProperty(columnName).getValue();
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValues()
    // */
    // public Value[] getValues() throws RepositoryException {
    // if (values == null) {
    // int i = 0;
    // values = new Value[iterator.columnNames.size()];
    // for (String columnName : iterator.columnNames) {
    // values[i++] = getValue(columnName);
    // }
    // }
    // return values;
    // }
    //
    // @Override
    // public Node getNode() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String getPath() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String getPath( String selectorName ) throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public double getScore() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public double getScore( String selectorName ) throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    // }
    //
    // protected static class MultiSelectorQueryResultRow implements Row, org.modeshape.jcr.api.query.Row {
    // protected final QueryResultRowIterator iterator;
    // protected final Object[] tuple;
    // private Value[] values = null;
    // private Node[] nodes;
    // private int[] locationIndexes;
    //
    // protected MultiSelectorQueryResultRow( QueryResultRowIterator iterator,
    // Node[] nodes,
    // int[] locationIndexes,
    // Object[] tuple ) {
    // this.iterator = iterator;
    // this.tuple = tuple;
    // this.nodes = nodes;
    // this.locationIndexes = locationIndexes;
    // assert this.iterator != null;
    // assert this.tuple != null;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.api.query.Row#getNode(java.lang.String)
    // */
    // @Override
    // public Node getNode( String selectorName ) throws RepositoryException {
    // try {
    // int locationIndex = iterator.columns.getLocationIndex(selectorName);
    // for (int i = 0; i != this.locationIndexes.length; ++i) {
    // if (this.locationIndexes[i] == locationIndex) {
    // return nodes[i];
    // }
    // }
    // } catch (NoSuchElementException e) {
    // throw new RepositoryException(e.getLocalizedMessage(), e);
    // }
    // assert false;
    // return null;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValue(java.lang.String)
    // */
    // public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
    // try {
    // int locationIndex = iterator.columns.getLocationIndexForColumn(columnName);
    // for (int i = 0; i != this.locationIndexes.length; ++i) {
    // if (this.locationIndexes[i] == locationIndex) {
    // Node node = nodes[i];
    // return node != null ? node.getProperty(columnName).getValue() : null;
    // }
    // }
    // } catch (NoSuchElementException e) {
    // throw new RepositoryException(e.getLocalizedMessage(), e);
    // }
    // assert false;
    // return null;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValues()
    // */
    // public Value[] getValues() throws RepositoryException {
    // if (values == null) {
    // int i = 0;
    // values = new Value[iterator.columnNames.size()];
    // for (String columnName : iterator.columnNames) {
    // values[i++] = getValue(columnName);
    // }
    // }
    // return values;
    // }
    //
    // @Override
    // public Node getNode() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String getPath() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public String getPath( String selectorName ) throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public double getScore() throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    //
    // @Override
    // public double getScore( String selectorName ) throws RepositoryException {
    // throw new UnsupportedRepositoryOperationException();
    // }
    // }
    //
    // protected static class XPathQueryResult extends JcrQueryResult {
    // private final List<String> columnNames;
    //
    // protected XPathQueryResult( JcrSession session,
    // String query,
    // QueryResults graphResults,
    // Schemata schemata ) {
    // super(session, query, graphResults, schemata);
    // List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
    // if (graphResults.getColumns().hasFullTextSearchScores() && !columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
    // columnNames.add(0, JCR_SCORE_COLUMN_NAME);
    // }
    // columnNames.add(0, JCR_PATH_COLUMN_NAME);
    // this.columnNames = Collections.unmodifiableList(columnNames);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.JcrQueryManager.JcrQueryResult#getColumnNameList()
    // */
    // @Override
    // public List<String> getColumnNameList() {
    // return columnNames;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.JcrQueryManager.JcrQueryResult#getRows()
    // */
    // @Override
    // public RowIterator getRows() {
    // final int numRows = results.getRowCount();
    // final List<Object[]> tuples = results.getTuples();
    // return new XPathQueryResultRowIterator(session, queryStatement, results, tuples.iterator(), numRows);
    // }
    // }
    //
    // protected static class XPathQueryResultRowIterator extends SingleSelectorQueryResultRowIterator {
    // private final ValueFactories factories;
    // private final SessionCache cache;
    //
    // protected XPathQueryResultRowIterator( JcrSession session,
    // String query,
    // QueryResults results,
    // Iterator<Object[]> tuples,
    // long numRows ) {
    // super(session, query, results, tuples, numRows);
    // factories = session.executionContext.getValueFactories();
    // cache = session.cache();
    // }
    //
    // @Override
    // protected Row createRow( Node node,
    // Object[] tuple ) {
    // return new XPathQueryResultRow(this, node, tuple);
    // }
    //
    // protected Value jcrPath( Path path ) {
    // return new JcrValue(factories, cache, PropertyType.PATH, path);
    // }
    //
    // protected Value jcrScore( Float score ) {
    // return new JcrValue(factories, cache, PropertyType.DOUBLE, score);
    // }
    // }
    //
    // protected static class XPathQueryResultRow extends SingleSelectorQueryResultRow {
    // protected XPathQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
    // Node node,
    // Object[] tuple ) {
    // super(iterator, node, tuple);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValue(java.lang.String)
    // */
    // @Override
    // public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
    // if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
    // Location location = (Location)tuple[iterator.locationIndex];
    // return ((XPathQueryResultRowIterator)iterator).jcrPath(location.getPath());
    // }
    // if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
    // Float score = (Float)tuple[iterator.scoreIndex];
    // return ((XPathQueryResultRowIterator)iterator).jcrScore(score);
    // }
    // return super.getValue(columnName);
    // }
    // }
    //
    // protected static class JcrSqlQueryResult extends JcrQueryResult {
    //
    // private final List<String> columnNames;
    // private boolean addedScoreColumn;
    // private boolean addedPathColumn;
    //
    // protected JcrSqlQueryResult( JcrSession session,
    // String query,
    // QueryResults graphResults,
    // Schemata schemata ) {
    // super(session, query, graphResults, schemata);
    // List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
    // if (!columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
    // columnNames.add(0, JCR_SCORE_COLUMN_NAME);
    // addedScoreColumn = true;
    // }
    // if (!columnNames.contains(JCR_PATH_COLUMN_NAME)) {
    // columnNames.add(0, JCR_PATH_COLUMN_NAME);
    // addedPathColumn = true;
    // }
    // this.columnNames = Collections.unmodifiableList(columnNames);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.JcrQueryManager.JcrQueryResult#getColumnNameList()
    // */
    // @Override
    // public List<String> getColumnNameList() {
    // return columnNames;
    // }
    //
    // @Override
    // protected List<String> loadColumnTypes( Columns columns ) {
    // List<String> types = new ArrayList<String>(columns.getColumnCount() + (addedScoreColumn ? 1 : 0)
    // + (addedPathColumn ? 1 : 0));
    // String stringtype = PropertyType.nameFromValue(PropertyType.STRING);
    // if (addedScoreColumn) types.add(0, stringtype);
    // if (addedPathColumn) types.add(0, stringtype);
    //
    // for (Column column : columns) {
    // String typeName = null;
    // Table table = schemata.getTable(column.selectorName());
    // if (table != null) {
    // Schemata.Column typedColumn = table.getColumn(column.propertyName());
    // typeName = typedColumn.getPropertyType();
    // }
    // if (typeName == null) {
    // // Might be fabricated column, so just assume string ...
    // typeName = stringtype;
    // }
    // types.add(typeName);
    // }
    //
    // return types;
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see org.modeshape.jcr.JcrQueryManager.JcrQueryResult#getRows()
    // */
    // @Override
    // public RowIterator getRows() {
    // final int numRows = results.getRowCount();
    // final List<Object[]> tuples = results.getTuples();
    // return new JcrSqlQueryResultRowIterator(session, queryStatement, results, tuples.iterator(), numRows);
    // }
    // }
    //
    // protected static class JcrSqlQueryResultRowIterator extends SingleSelectorQueryResultRowIterator {
    // private final ValueFactories factories;
    // private final SessionCache cache;
    //
    // protected JcrSqlQueryResultRowIterator( JcrSession session,
    // String query,
    // QueryResults results,
    // Iterator<Object[]> tuples,
    // long numRows ) {
    // super(session, query, results, tuples, numRows);
    // factories = session.executionContext.getValueFactories();
    // cache = session.cache();
    // }
    //
    // @Override
    // protected Row createRow( Node node,
    // Object[] tuple ) {
    // return new JcrSqlQueryResultRow(this, node, tuple);
    // }
    //
    // protected Value jcrPath( Path path ) {
    // return new JcrValue(factories, cache, PropertyType.PATH, path);
    // }
    //
    // protected Value jcrScore( Float score ) {
    // return new JcrValue(factories, cache, PropertyType.DOUBLE, score);
    // }
    // }
    //
    // protected static class JcrSqlQueryResultRow extends SingleSelectorQueryResultRow {
    // protected JcrSqlQueryResultRow( SingleSelectorQueryResultRowIterator iterator,
    // Node node,
    // Object[] tuple ) {
    // super(iterator, node, tuple);
    // }
    //
    // /**
    // * {@inheritDoc}
    // *
    // * @see javax.jcr.query.Row#getValue(java.lang.String)
    // */
    // @Override
    // public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
    // if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
    // Location location = (Location)tuple[iterator.locationIndex];
    // return ((JcrSqlQueryResultRowIterator)iterator).jcrPath(location.getPath());
    // }
    // if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
    // Float score = (Float)tuple[iterator.scoreIndex];
    // return ((JcrSqlQueryResultRowIterator)iterator).jcrScore(score);
    // }
    // return super.getValue(columnName);
    // }
    // }
    //
    // @Override
    // public QueryObjectModelFactory getQOMFactory() {
    // throw new IllegalStateException();
    // }

    protected static class SessionQueryContext implements JcrQueryContext {
        private final JcrSession session;
        private final ValueFactories factories;
        private final SessionCache cache;

        protected SessionQueryContext( JcrSession session ) {
            this.session = session;
            this.factories = session.getExecutionContext().getValueFactories();
            this.cache = session.cache();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#createValue(int, java.lang.Object)
         */
        @Override
        public Value createValue( int propertyType,
                                  Object value ) {
            return new JcrValue(factories, cache, propertyType, value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#emptyNodeIterator()
         */
        @Override
        public NodeIterator emptyNodeIterator() {
            return new JcrEmptyNodeIterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#execute(org.modeshape.graph.query.model.QueryCommand,
         *      org.modeshape.graph.query.plan.PlanHints, java.util.Map)
         */
        @Override
        public QueryResults execute( QueryCommand query,
                                     PlanHints hints,
                                     Map<String, Object> variables ) throws RepositoryException {
            session.checkLive();
            // Submit immediately to the workspace graph ...
            Schemata schemata = session.workspace().nodeTypeManager().schemata();
            return session.repository().queryManager().query(session.workspace().getName(), query, schemata, hints, variables);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#getExecutionContext()
         */
        @Override
        public ExecutionContext getExecutionContext() {
            return session.getExecutionContext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#getNode(Location)
         */
        @Override
        public Node getNode( Location location ) throws RepositoryException {
            if (!session.wasRemovedInSession(location)) {
                try {
                    return session.getNode(location.getPath());
                } catch (PathNotFoundException e) {
                    // Must have been deleted from storage but not yet from the indexes ...
                } catch (AccessDeniedException e) {
                    // No access to this node ...
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#getSchemata()
         */
        @Override
        public Schemata getSchemata() {
            return session.nodeTypeManager().schemata();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#isLive()
         */
        @Override
        public boolean isLive() {
            return session.isLive();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#search(java.lang.String, int, int)
         */
        @Override
        public QueryResults search( String searchExpression,
                                    int maxRowCount,
                                    int offset ) throws RepositoryException {
            return session.repository().queryManager().search(session.workspace().getName(),
                                                              searchExpression,
                                                              maxRowCount,
                                                              offset);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.query.JcrQueryContext#store(java.lang.String, org.modeshape.graph.property.Name,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public Node store( String absolutePath,
                           Name nodeType,
                           String language,
                           String statement ) throws RepositoryException {
            session.checkLive();
            NamespaceRegistry namespaces = session.namespaces();

            Path path;
            try {
                path = session.getExecutionContext().getValueFactories().getPathFactory().create(absolutePath);
            } catch (IllegalArgumentException iae) {
                throw new RepositoryException(JcrI18n.invalidPathParameter.text("absolutePath", absolutePath));
            }
            Path parentPath = path.getParent();

            Node parentNode = session.getNode(parentPath);

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            Node queryNode = parentNode.addNode(path.relativeTo(parentPath).getString(namespaces),
                                                JcrNtLexicon.QUERY.getString(namespaces));

            queryNode.setProperty(JcrLexicon.LANGUAGE.getString(namespaces), language);
            queryNode.setProperty(JcrLexicon.STATEMENT.getString(namespaces), statement);

            return queryNode;
        }

    }

    protected static class SessionTypeSystem implements JcrTypeSystem {
        protected final JcrSession session;
        protected final TypeSystem delegate;

        protected SessionTypeSystem( JcrSession session ) {
            this.session = session;
            this.delegate = session.getExecutionContext().getValueFactories().getTypeSystem();
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
        public ValueFactory getValueFactory() {
            return session.getValueFactory();
        }
    }
}
