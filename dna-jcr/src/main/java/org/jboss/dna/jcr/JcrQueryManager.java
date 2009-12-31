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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.parse.QueryParser;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * Place-holder implementation of {@link QueryManager} interface.
 */
@Immutable
class JcrQueryManager implements QueryManager {

    public static final int MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES = Integer.MAX_VALUE;

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
     * @param language the language obtained from the {@link QueryParser}; may not be null
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     * @return query the JCR query object; never null
     * @throws InvalidQueryException if expression is invalid or language is unsupported
     */
    public Query createQuery( String expression,
                              String language,
                              Path storedAtPath ) throws InvalidQueryException {
        // Look for a parser for the specified language ...
        QueryParser parser = session.repository().queryParsers().getParserFor(language);
        if (parser == null) {
            Set<String> languages = session.repository().queryParsers().getLanguages();
            throw new InvalidQueryException(JcrI18n.invalidQueryLanguage.text(language, languages));
        }
        if (parser.getLanguage().equals(FullTextSearchParser.LANGUAGE)) {
            // This is a full-text search ...
            return new JcrSearch(this.session, expression, parser.getLanguage(), storedAtPath);
        }
        TypeSystem typeSystem = session.executionContext.getValueFactories().getTypeSystem();
        try {
            // Parsing must be done now ...
            QueryCommand command = parser.parseQuery(expression, typeSystem);
            if (command == null) {
                // The query is not well-formed and cannot be parsed ...
                throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression));
            }
            PlanHints hints = new PlanHints();
            // If using XPath, we need to add a few hints ...
            if (Query.XPATH.equals(language)) {
                hints.hasFullTextSearch = true; // requires 'jcr:score' to exist
            }
            return new JcrQuery(this.session, expression, parser.getLanguage(), command, hints, storedAtPath);
        } catch (ParsingException e) {
            // The query is not well-formed and cannot be parsed ...
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryCannotBeParsedUsingLanguage.text(language, expression, reason));
        } catch (org.jboss.dna.graph.query.parse.InvalidQueryException e) {
            // The query was parsed, but there is an error in the query
            String reason = e.getMessage();
            throw new InvalidQueryException(JcrI18n.queryInLanguageIsNotValid.text(language, expression, reason));
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

    @NotThreadSafe
    protected static abstract class AbstractQuery implements Query {
        protected final JcrSession session;
        protected final String language;
        protected final String statement;
        private Path storedAtPath;

        /**
         * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
         * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
         * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
         * 
         * @param session the session that was used to create this query and that will be used to execute this query; may not be
         *        null
         * @param statement the original statement as supplied by the client; may not be null
         * @param language the language obtained from the {@link QueryParser}; may not be null
         * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
         */
        protected AbstractQuery( JcrSession session,
                                 String statement,
                                 String language,
                                 Path storedAtPath ) {
            assert session != null;
            assert statement != null;
            assert language != null;
            this.session = session;
            this.language = language;
            this.statement = statement;
            this.storedAtPath = storedAtPath;
        }

        protected final JcrSession session() {
            return this.session;
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
            return storedAtPath.getString(session.getExecutionContext().getNamespaceRegistry());
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Query#storeAsNode(java.lang.String)
         */
        public Node storeAsNode( String absPath ) throws PathNotFoundException, ConstraintViolationException, RepositoryException {
            NamespaceRegistry namespaces = this.session.namespaces();

            Path path;
            try {
                path = session.getExecutionContext().getValueFactories().getPathFactory().create(absPath);
            } catch (IllegalArgumentException iae) {
                throw new RepositoryException(JcrI18n.invalidPathParameter.text("absPath", absPath));
            }
            Path parentPath = path.getParent();

            Node parentNode = session.getNode(parentPath);
            Node queryNode = parentNode.addNode(path.relativeTo(parentPath).getString(namespaces),
                                                JcrNtLexicon.QUERY.getString(namespaces));

            queryNode.setProperty(JcrLexicon.LANGUAGE.getString(namespaces), this.language);
            queryNode.setProperty(JcrLexicon.STATEMENT.getString(namespaces), this.statement);

            this.storedAtPath = path;

            return queryNode;
        }
    }

    /**
     * Implementation of {@link Query} that represents a {@link QueryCommand} query.
     */
    @NotThreadSafe
    protected static class JcrQuery extends AbstractQuery {
        private final QueryCommand query;
        private final PlanHints hints;
        private final Map<String, Object> variables;

        /**
         * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
         * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
         * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
         * 
         * @param session the session that was used to create this query and that will be used to execute this query; may not be
         *        null
         * @param statement the original statement as supplied by the client; may not be null
         * @param language the language obtained from the {@link QueryParser}; may not be null
         * @param query the parsed query representation; may not be null
         * @param hints any hints that are to be used; may be null if there are no hints
         * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
         */
        protected JcrQuery( JcrSession session,
                            String statement,
                            String language,
                            QueryCommand query,
                            PlanHints hints,
                            Path storedAtPath ) {
            super(session, statement, language, storedAtPath);
            assert query != null;
            this.query = query;
            this.hints = hints;
            this.variables = null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Query#execute()
         */
        public QueryResult execute() throws RepositoryException {
            // Submit immediately to the workspace graph ...
            Schemata schemata = session.workspace().nodeTypeManager().schemata();
            QueryResults result = session.repository().queryManager().query(session.workspace(),
                                                                            query,
                                                                            schemata,
                                                                            hints,
                                                                            variables);
            if (Query.XPATH.equals(language)) {
                return new XPathQueryResult(session, result);
            }
            return new JcrQueryResult(session, result);
        }

    }

    @NotThreadSafe
    protected static class JcrSearch extends AbstractQuery {

        /**
         * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated,
         * the {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be
         * a string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
         * 
         * @param session the session that was used to create this query and that will be used to execute this query; may not be
         *        null
         * @param statement the original statement as supplied by the client; may not be null
         * @param language the language obtained from the {@link QueryParser}; may not be null
         * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
         */
        protected JcrSearch( JcrSession session,
                             String statement,
                             String language,
                             Path storedAtPath ) {
            super(session, statement, language, storedAtPath);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Query#execute()
         */
        public QueryResult execute() throws RepositoryException {
            // Submit immediately to the workspace graph ...
            QueryResults result = session.repository().queryManager().search(session.workspace(),
                                                                             statement,
                                                                             MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES,
                                                                             0);
            return new JcrQueryResult(session, result);
        }
    }

    protected static final String JCR_SCORE_COLUMN_NAME = "jcr:score";
    protected static final String JCR_PATH_COLUMN_NAME = "jcr:path";

    /**
     * The results of a query. This is not thread-safe because it relies upon JcrSession, which is not thread-safe. Also, although
     * the results of a query never change, the objects returned by the iterators may vary if the session information changes.
     */
    @NotThreadSafe
    protected static class JcrQueryResult implements QueryResult {
        protected final JcrSession session;
        protected final QueryResults results;

        protected JcrQueryResult( JcrSession session,
                                  QueryResults graphResults ) {
            this.session = session;
            this.results = graphResults;
            assert this.session != null;
            assert this.results != null;
        }

        protected QueryResults results() {
            return results;
        }

        public List<String> getColumnNameList() {
            return results.getColumns().getColumnNames();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.QueryResult#getColumnNames()
         */
        public String[] getColumnNames() /*throws RepositoryException*/{
            List<String> names = getColumnNameList();
            return names.toArray(new String[names.size()]); // make a defensive copy ...
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.QueryResult#getNodes()
         */
        public NodeIterator getNodes() throws RepositoryException {
            // Find all of the nodes in the results. We have to do this pre-emptively, since this
            // is the only method to throw RepositoryException ...
            final int numRows = results.getRowCount();
            if (numRows == 0) return new JcrEmptyNodeIterator();

            final List<AbstractJcrNode> nodes = new ArrayList<AbstractJcrNode>(numRows);
            final String selectorName = results.getColumns().getSelectorNames().get(0);
            final int locationIndex = results.getColumns().getLocationIndex(selectorName);
            for (Object[] tuple : results.getTuples()) {
                Location location = (Location)tuple[locationIndex];
                nodes.add(session.getNode(location.getPath()));
            }
            return new QueryResultNodeIterator(nodes);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.QueryResult#getRows()
         */
        public RowIterator getRows() /*throws RepositoryException*/{
            // We can actually delay the loading of the nodes until the rows are accessed ...
            final int numRows = results.getRowCount();
            final List<Object[]> tuples = results.getTuples();
            return new QueryResultRowIterator(session, results.getColumns(), tuples.iterator(), numRows);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return results.toString();
        }
    }

    /**
     * The {@link NodeIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getNodes()
     */
    @NotThreadSafe
    protected static class QueryResultNodeIterator implements NodeIterator {
        private final Iterator<? extends Node> nodes;
        private final int size;
        private long position = 0L;

        protected QueryResultNodeIterator( List<? extends Node> nodes ) {
            this.nodes = nodes.iterator();
            this.size = nodes.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.NodeIterator#nextNode()
         */
        public Node nextNode() {
            Node node = nodes.next();
            ++position;
            return node;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        public long getSize() {
            return size;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i)
                nextNode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return nodes.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return nextNode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The {@link RowIterator} implementation returned by the {@link JcrQueryResult}.
     * 
     * @see JcrQueryResult#getRows()
     */
    @NotThreadSafe
    protected static class QueryResultRowIterator implements RowIterator {
        protected final List<String> columnNames;
        private final Iterator<Object[]> tuples;
        protected final int locationIndex;
        protected final int scoreIndex;
        protected final JcrSession session;
        private long position = 0L;
        private final long numRows;

        protected QueryResultRowIterator( JcrSession session,
                                          Columns columns,
                                          Iterator<Object[]> tuples,
                                          long numRows ) {
            this.tuples = tuples;
            this.columnNames = columns.getColumnNames();
            String selectorName = columns.getSelectorNames().get(0);
            this.locationIndex = columns.getLocationIndex(selectorName);
            this.scoreIndex = columns.getFullTextSearchScoreIndexFor(selectorName);
            this.session = session;
            this.numRows = numRows;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.RowIterator#nextRow()
         */
        public Row nextRow() {
            final Object[] tuple = tuples.next();
            ++position;
            return createRow(tuple);
        }

        protected Row createRow( final Object[] tuple ) {
            return new QueryResultRow(this, tuple);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        public long getSize() {
            return numRows;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        public void skip( long skipNum ) {
            for (long i = 0L; i != skipNum; ++i) {
                tuples.next();
            }
            position += skipNum;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return tuples.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return nextRow();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected static class QueryResultRow implements Row {
        protected final QueryResultRowIterator iterator;
        protected final Object[] tuple;
        private Node node = null;
        private Value[] values = null;

        protected QueryResultRow( QueryResultRowIterator iterator,
                                  Object[] tuple ) {
            this.iterator = iterator;
            this.tuple = tuple;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValue(java.lang.String)
         */
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            return node().getProperty(columnName).getValue();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValues()
         */
        public Value[] getValues() throws RepositoryException {
            if (values == null) {
                int i = 0;
                values = new Value[iterator.columnNames.size()];
                for (String columnName : iterator.columnNames) {
                    values[i++] = getValue(columnName);
                }
            }
            return values;
        }

        /**
         * Load the node. The properties are <i>always</i> fetched from the session to ensure that any modifications to the nodes
         * within session are always used.
         * 
         * @return the node
         * @throws RepositoryException if the node could not be found
         */
        protected final Node node() throws RepositoryException {
            if (node == null) {
                Location location = (Location)tuple[iterator.locationIndex];
                node = iterator.session.getNode(location.getPath());
            }
            return node;
        }
    }

    protected static class XPathQueryResult extends JcrQueryResult {
        private final List<String> columnNames;

        protected XPathQueryResult( JcrSession session,
                                    QueryResults graphResults ) {
            super(session, graphResults);
            List<String> columnNames = new LinkedList<String>(graphResults.getColumns().getColumnNames());
            if (graphResults.getColumns().hasFullTextSearchScores() && !columnNames.contains(JCR_SCORE_COLUMN_NAME)) {
                columnNames.add(0, JCR_SCORE_COLUMN_NAME);
            }
            columnNames.add(0, JCR_PATH_COLUMN_NAME);
            this.columnNames = Collections.unmodifiableList(columnNames);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.JcrQueryManager.JcrQueryResult#getColumnNameList()
         */
        @Override
        public List<String> getColumnNameList() {
            return columnNames;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.JcrQueryManager.JcrQueryResult#getRows()
         */
        @Override
        public RowIterator getRows() {
            final int numRows = results.getRowCount();
            final List<Object[]> tuples = results.getTuples();
            return new XPathQueryResultRowIterator(session, results.getColumns(), tuples.iterator(), numRows);
        }
    }

    protected static class XPathQueryResultRowIterator extends QueryResultRowIterator {
        private final ValueFactories factories;
        private final SessionCache cache;

        protected XPathQueryResultRowIterator( JcrSession session,
                                               Columns columns,
                                               Iterator<Object[]> tuples,
                                               long numRows ) {
            super(session, columns, tuples, numRows);
            factories = session.executionContext.getValueFactories();
            cache = session.cache();
        }

        @Override
        protected Row createRow( final Object[] tuple ) {
            return new XPathQueryResultRow(this, tuple);
        }

        protected Value jcrPath( Path path ) {
            return new JcrValue(factories, cache, PropertyType.PATH, path);
        }

        protected Value jcrScore( Float score ) {
            return new JcrValue(factories, cache, PropertyType.DOUBLE, score);
        }
    }

    protected static class XPathQueryResultRow extends QueryResultRow {
        protected XPathQueryResultRow( XPathQueryResultRowIterator iterator,
                                       Object[] tuple ) {
            super(iterator, tuple);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.query.Row#getValue(java.lang.String)
         */
        @Override
        public Value getValue( String columnName ) throws ItemNotFoundException, RepositoryException {
            if (JCR_PATH_COLUMN_NAME.equals(columnName)) {
                Location location = (Location)tuple[iterator.locationIndex];
                return ((XPathQueryResultRowIterator)iterator).jcrPath(location.getPath());
            }
            if (JCR_SCORE_COLUMN_NAME.equals(columnName)) {
                Float score = (Float)tuple[iterator.scoreIndex];
                return ((XPathQueryResultRowIterator)iterator).jcrScore(score);
            }
            return super.getValue(columnName);
        }
    }
}
