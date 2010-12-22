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
package org.modeshape.search.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.Version;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.ChildNode;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DescendantNode;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.UpperCase;
import org.modeshape.graph.query.model.FullTextSearch.NegationTerm;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;
import org.modeshape.graph.query.process.ProcessingComponent;
import org.modeshape.graph.query.process.SelectComponent;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.search.AbstractSearchEngine;
import org.modeshape.graph.search.SearchEngine;
import org.modeshape.graph.search.SearchEngineProcessor;
import org.modeshape.graph.search.SearchEngineWorkspace;
import org.modeshape.search.lucene.query.CompareStringQuery;
import org.modeshape.search.lucene.query.HasValueQuery;
import org.modeshape.search.lucene.query.MatchNoneQuery;

/**
 * An abstract {@link SearchEngine} implementation that is set up to use the Lucene library. This provides an abstract
 * {@link SearchEngineProcessor Processor} base class that has some commonly-needed methods, simplifying the implementation.
 * However, this class does not presume any number or layout of the Lucene indexes, and requires a subclass to do that.
 * 
 * @param <WorkspaceType> the type of workspace
 * @param <ProcessorType> type type of processor
 */
public abstract class AbstractLuceneSearchEngine<WorkspaceType extends SearchEngineWorkspace, ProcessorType extends SearchEngineProcessor>
    extends AbstractSearchEngine<WorkspaceType, ProcessorType> {

    /**
     * Create a {@link SearchEngine} instance that uses Lucene.
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @throws IllegalArgumentException if any of the parameters are null
     */
    protected AbstractLuceneSearchEngine( String sourceName,
                                          RepositoryConnectionFactory connectionFactory,
                                          boolean verifyWorkspaceInSource ) {
        super(sourceName, connectionFactory, verifyWorkspaceInSource);
    }

    /**
     * Abstract {@link SearchEngineProcessor} implementation for the {@link AbstractLuceneSearchEngine}.
     * 
     * @param <SessionType> the type of session
     * @param <WorkspaceType> the type of workspace
     */
    protected static abstract class AbstractLuceneProcessor<WorkspaceType extends SearchEngineWorkspace, SessionType extends WorkspaceSession>
        extends SearchEngineProcessor {
        private final Map<String, SessionType> workspaceSessions = new HashMap<String, SessionType>();
        protected final boolean readOnly;
        protected final ValueFactories valueFactories;
        protected final ValueFactory<String> stringFactory;
        protected final DateTimeFactory dateFactory;
        protected final PathFactory pathFactory;
        protected final UuidFactory uuidFactory;
        protected final NameFactory nameFactory;
        protected final TypeSystem typeSystem;
        protected final PropertyFactory propertyFactory;
        protected final Workspaces<WorkspaceType> workspaces;
        protected final Logger logger = Logger.getLogger(getClass());

        protected AbstractLuceneProcessor( String sourceName,
                                           ExecutionContext context,
                                           Workspaces<WorkspaceType> workspaces,
                                           Observer observer,
                                           DateTime now,
                                           boolean readOnly ) {
            super(sourceName, context, observer, now);
            this.workspaces = workspaces;
            this.readOnly = readOnly;
            this.valueFactories = context.getValueFactories();
            this.stringFactory = valueFactories.getStringFactory();
            this.dateFactory = valueFactories.getDateFactory();
            this.pathFactory = valueFactories.getPathFactory();
            this.uuidFactory = valueFactories.getUuidFactory();
            this.nameFactory = valueFactories.getNameFactory();
            this.typeSystem = valueFactories.getTypeSystem();
            this.propertyFactory = context.getPropertyFactory();
            assert this.stringFactory != null;
            assert this.dateFactory != null;
            assert this.workspaces != null;
        }

        protected abstract SessionType createSessionFor( WorkspaceType workspace );

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.search.SearchEngineProcessor#commit()
         */
        @Override
        protected void commit() {
            for (SessionType session : getSessions()) {
                session.commit();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.search.SearchEngineProcessor#rollback()
         */
        @Override
        protected void rollback() {
            for (SessionType session : getSessions()) {
                session.rollback();
            }
        }

        protected WorkspaceType getWorkspace( String workspaceName,
                                              boolean createIfMissing ) {
            return getWorkspace(workspaceName, createIfMissing);
        }

        protected WorkspaceType getWorkspace( Request request,
                                              String workspaceName,
                                              boolean createIfMissing ) {
            WorkspaceType workspace = workspaces.getWorkspace(getExecutionContext(), workspaceName, createIfMissing);
            if (workspace == null) {
                if (request != null) {
                    String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, getSourceName());
                    request.setError(new InvalidWorkspaceException(msg));
                }
                return null;
            }
            return workspace;
        }

        protected SessionType getSessionFor( Request request,
                                             String workspaceName ) {
            return getSessionFor(request, workspaceName, true);
        }

        protected SessionType getSessionFor( Request request,
                                             String workspaceName,
                                             boolean createIfMissing ) {
            SessionType result = workspaceSessions.get(workspaceName);
            if (result == null) {
                // See if there is a workspace with the supplied name ...
                WorkspaceType workspace = getWorkspace(request, workspaceName, createIfMissing);
                if (workspace == null) return null;
                result = createSessionFor(workspace);
                workspaceSessions.put(workspaceName, result);
            }
            return result;
        }

        protected Collection<SessionType> getSessions() {
            return workspaceSessions.values();
        }

        protected final String serializeProperty( Property property ) {
            StringBuilder sb = new StringBuilder();
            sb.append(stringFactory.create(property.getName()));
            sb.append('=');
            Iterator<?> iter = property.getValues();
            if (iter.hasNext()) {
                sb.append(stringFactory.create(iter.next()));
            }
            while (iter.hasNext()) {
                sb.append('\n');
                sb.append(stringFactory.create(iter.next()));
            }
            return sb.toString();
        }

        protected final Property deserializeProperty( String propertyString ) {
            int index = propertyString.indexOf('=');
            assert index > -1;
            if (index == propertyString.length() - 1) return null;
            Name propName = nameFactory.create(propertyString.substring(0, index));
            String valueString = propertyString.substring(index + 1);
            // Break into multiple values if multiple lines ...
            String[] values = valueString.split("\\n");
            if (values.length == 0) return null;
            if (values.length == 1) {
                Object value = values[0];
                if (ModeShapeLexicon.UUID.equals(propName) || JcrLexicon.UUID.equals(propName)) {
                    value = uuidFactory.create(value);
                }
                return propertyFactory.create(propName, value);
            }
            List<String> propValues = new LinkedList<String>();
            for (String value : values) {
                propValues.add(value);
            }
            return propertyFactory.create(propName, propValues);
        }

        /**
         * Create the field name that will be used to store the full-text searchable property values.
         * 
         * @param propertyName the name of the property; may not null
         * @return the field name for the full-text searchable property values; never null
         */
        protected abstract String fullTextFieldName( String propertyName );

        /**
         * Return whether this session made changes to the indexed state.
         * 
         * @return true if change were made, or false otherwise
         */
        public boolean hasChanges() {
            for (SessionType session : getSessions()) {
                if (session.getChangeCount() > 0) return true;
            }
            return false;
        }

        public String pathAsString( Path path ) {
            assert path != null;
            if (path.isRoot()) return "/";
            StringBuilder sb = new StringBuilder();
            for (Path.Segment segment : path) {
                sb.append('/');
                sb.append(stringFactory.create(segment.getName()));
                sb.append('[');
                sb.append(segment.getIndex());
                sb.append(']');
            }
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Some kinds of constraints are not easily pushed down to Lucene as are of a Lucene Query, and instead are applied by
         * filtering the results. For example, a FullTextSearchScore applies to the score of the tuple, which cannot be (easily?)
         * applied as a Query.
         * </p>
         * <p>
         * Therefore, each of the AND-ed constraints of the query are evaluated separately. After all, each of the tuples returned
         * by the planned query must satisfy all of the AND-ed constraints. Or, to put it another way, if a tuple does not satisfy
         * one of the AND-ed constraints, the tuple should not be included in the query results.
         * </p>
         * <p>
         * Logically, any AND-ed criteria that cannot be pushed down to Lucene can of course be applied as a filter on the
         * results. Thus, each AND-ed constraint is processed to first determine if it can be represented as a Lucene query; all
         * other AND-ed constraints must be handled as a results filter. Since most queries will likely use one or more simple
         * constraints AND-ed together, this approach will likely work very well.
         * </p>
         * <p>
         * The only hairy case is when any AND-ed constraint is actually an OR-ed combination of multiple constraints of which at
         * least one cannot be pushed down to Lucene. In this case, the entire AND-ed constraint must be treated as a results
         * filter (even if many of those constraints that make up the OR-ed constraint can be pushed down). Hopefully, this will
         * not be a common case in actual queries.
         * </p>
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
         */
        @Override
        public void process( AccessQueryRequest request ) {
            SessionType session = getSessionFor(request, request.workspace());
            if (session == null) return;

            long planningNanos = System.nanoTime();
            // For each of the AND-ed constraints ...
            Query pushDownQuery = null;
            Constraint postProcessConstraint = null;
            try {
                QueryFactory queryFactory = null;
                for (Constraint andedConstraint : request.andedConstraints()) {
                    // Determine if it can be represented as a Lucene query ...
                    assert andedConstraint != null;
                    if (queryFactory == null) queryFactory = queryFactory(session, request.variables());
                    Query constraintQuery = queryFactory.createQuery(andedConstraint);
                    if (constraintQuery != null) {
                        if (constraintQuery instanceof MatchAllDocsQuery) {
                            // This constraint includes all values, so we can just skip it ...
                            continue;
                        }
                        if (constraintQuery instanceof MatchNoneQuery) {
                            // This constraint invalidates all of the other AND-ed constraints ...
                            pushDownQuery = constraintQuery;
                            break;
                        }
                        // The AND-ed constraint _can_ be represented as a push-down Lucene query ...
                        if (pushDownQuery == null) {
                            // This must be the first query ...
                            pushDownQuery = constraintQuery;
                        } else {
                            pushDownQuery = andQueries(pushDownQuery, constraintQuery);
                        }
                    } else {
                        // The AND-ed constraint _cannot_ be represented as a push-down Lucene query ...
                        if (postProcessConstraint == null) {
                            postProcessConstraint = andedConstraint;
                        } else {
                            postProcessConstraint = new And(postProcessConstraint, andedConstraint);
                        }
                    }
                }
            } catch (IOException e) {
                // There was a error working with the constraints (such as a ValueFormatException) ...
                request.setError(e);
                return;
            } catch (RuntimeException e) {
                // There was a error working with the constraints (such as a ValueFormatException) ...
                request.setError(e);
                return;
            }

            if (pushDownQuery == null) {
                // There are no constraints that can be pushed down, so return _all_ the nodes ...
                pushDownQuery = new MatchAllDocsQuery();
            }
            long executingNanos = System.nanoTime();
            planningNanos = executingNanos - planningNanos;

            // Get the results from Lucene ...
            List<Object[]> tuples = null;
            final Columns columns = request.resultColumns();
            if (pushDownQuery instanceof MatchNoneQuery) {
                // There are no results ...
                tuples = Collections.emptyList();
            } else {
                try {
                    // Execute the query against the content indexes ...
                    IndexSearcher searcher = session.getContentSearcher();
                    if (logger.isTraceEnabled()) {
                        logger.trace("query \"{0}\" workspace: {1}", session.getWorkspaceName(), pushDownQuery);
                    }
                    TupleCollector collector = session.createTupleCollector(columns);
                    searcher.search(pushDownQuery, collector);
                    tuples = collector.getTuples();
                } catch (IOException e) {
                    // There was a problem executing the Lucene query ...
                    request.setError(e);
                    return;
                }
            }

            if (!tuples.isEmpty()) {
                if (postProcessConstraint != null) {
                    // Create a delegate processing component that will return the tuples we've already found ...
                    final List<Object[]> allTuples = tuples;
                    QueryContext queryContext = new QueryContext(request.schemata(), typeSystem, null, new SimpleProblems(),
                                                                 request.variables());
                    ProcessingComponent tuplesProcessor = new ProcessingComponent(queryContext, columns) {
                        @Override
                        public List<Object[]> execute() {
                            return allTuples;
                        }
                    };
                    // Create a processing component that will apply these constraints to the tuples we already found ...
                    SelectComponent selector = new SelectComponent(tuplesProcessor, postProcessConstraint, request.variables());
                    tuples = selector.execute();
                }

                // Limit the tuples ...
                Limit limit = request.limit();
                if (!limit.isUnlimited()) {
                    int firstIndex = limit.offset();
                    int maxRows = Math.min(tuples.size(), limit.rowLimit());
                    if (firstIndex > 0) {
                        if (firstIndex > tuples.size()) {
                            tuples.clear();
                        } else {
                            tuples = tuples.subList(firstIndex, maxRows);
                        }
                    } else {
                        tuples = tuples.subList(0, maxRows);
                    }
                }
            }

            executingNanos = System.nanoTime() - executingNanos;
            Statistics stats = new Statistics(planningNanos, 0L, 0L, executingNanos);
            request.setResults(tuples, stats);
        }

        protected Query andQueries( Query first,
                                    Query second ) {
            if (first instanceof BooleanQuery) {
                BooleanQuery booleanQuery = (BooleanQuery)first;
                boolean canMerge = true;
                for (BooleanClause clause : booleanQuery.getClauses()) {
                    if (clause.getOccur() == BooleanClause.Occur.SHOULD) {
                        canMerge = false;
                        break;
                    }
                }
                if (canMerge) {
                    // The boolean query has all MUST occurs, so we can just add another one ...
                    booleanQuery.add(second, Occur.MUST);
                    return booleanQuery;
                }
            }
            // This is the second push-down query, so create a BooleanQuery ...
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(first, Occur.MUST);

            // If the second is a BooleanQuery, then it is probably a 'NOT(query)'
            boolean done = false;
            if (second instanceof BooleanQuery) {
                BooleanQuery booleanSecond = (BooleanQuery)second;
                if (booleanSecond.getClauses().length == 1) {
                    BooleanClause onlyClause = booleanSecond.getClauses()[0];
                    if (onlyClause.isProhibited()) {
                        booleanQuery.add(onlyClause.getQuery(), Occur.MUST_NOT);
                        done = true;
                    } else if (onlyClause.isRequired()) {
                        booleanQuery.add(onlyClause.getQuery(), Occur.MUST);
                        done = true;
                    }
                }
            }
            if (!done) {
                booleanQuery.add(second, Occur.MUST);
            }
            return booleanQuery;
        }

        protected QueryFactory queryFactory( WorkspaceSession session,
                                             Map<String, Object> variables ) {
            return new QueryFactory(session, variables);
        }

        protected class QueryFactory {
            private final WorkspaceSession session;
            private final Map<String, Object> variables;

            protected QueryFactory( WorkspaceSession session,
                                    Map<String, Object> variables ) {
                this.session = session;
                this.variables = variables;
            }

            public Query createQuery( Constraint constraint ) throws IOException {
                if (constraint instanceof And) {
                    And and = (And)constraint;
                    Query leftQuery = createQuery(and.left());
                    Query rightQuery = createQuery(and.right());
                    if (leftQuery == null || rightQuery == null) return null;
                    BooleanQuery booleanQuery = new BooleanQuery();
                    booleanQuery.add(leftQuery, Occur.MUST);
                    booleanQuery.add(rightQuery, Occur.MUST);
                    return booleanQuery;
                }
                if (constraint instanceof Or) {
                    Or or = (Or)constraint;
                    Query leftQuery = createQuery(or.left());
                    Query rightQuery = createQuery(or.right());
                    if (leftQuery == null) {
                        return rightQuery != null ? rightQuery : null;
                    } else if (rightQuery == null) {
                        return leftQuery;
                    }
                    BooleanQuery booleanQuery = new BooleanQuery();
                    booleanQuery.add(leftQuery, Occur.SHOULD);
                    booleanQuery.add(rightQuery, Occur.SHOULD);
                    return booleanQuery;
                }
                if (constraint instanceof Not) {
                    Not not = (Not)constraint;
                    Query notted = createQuery(not.constraint());
                    if (notted == null) return new MatchAllDocsQuery();
                    BooleanQuery query = new BooleanQuery();
                    // We need at least some positive match, so get all docs ...
                    query.add(new MatchAllDocsQuery(), Occur.SHOULD);
                    // Now apply the original query being 'NOT-ed' as a MUST_NOT occurrence ...
                    query.add(notted, Occur.MUST_NOT);
                    return query;
                }
                if (constraint instanceof SetCriteria) {
                    SetCriteria setCriteria = (SetCriteria)constraint;
                    DynamicOperand left = setCriteria.leftOperand();
                    int numRightOperands = setCriteria.rightOperands().size();
                    assert numRightOperands > 0;
                    if (numRightOperands == 1) {
                        StaticOperand rightOperand = setCriteria.rightOperands().iterator().next();
                        if (rightOperand instanceof Literal) {
                            return createQuery(left, Operator.EQUAL_TO, setCriteria.rightOperands().iterator().next());
                        }
                    }
                    BooleanQuery setQuery = new BooleanQuery();
                    for (StaticOperand right : setCriteria.rightOperands()) {
                        if (right instanceof BindVariableName) {
                            // This single value is a variable name, which may evaluate to a single value or multiple values ...
                            BindVariableName var = (BindVariableName)right;
                            Object value = variables.get(var.variableName());
                            if (value instanceof Iterable<?>) {
                                Iterator<?> iter = ((Iterable<?>)value).iterator();
                                while (iter.hasNext()) {
                                    Object resolvedValue = iter.next();
                                    if (resolvedValue == null) continue;
                                    StaticOperand elementInRight = null;
                                    if (resolvedValue instanceof Literal) {
                                        elementInRight = (Literal)resolvedValue;
                                    } else {
                                        elementInRight = new Literal(resolvedValue);
                                    }
                                    Query rightQuery = createQuery(left, Operator.EQUAL_TO, elementInRight);
                                    if (rightQuery == null) continue;
                                    setQuery.add(rightQuery, Occur.SHOULD);
                                }
                            }
                            if (value == null) {
                                throw new LuceneException(LuceneI18n.missingVariableValue.text(var.variableName()));
                            }
                        } else {
                            Query rightQuery = createQuery(left, Operator.EQUAL_TO, right);
                            if (rightQuery == null) return null;
                            setQuery.add(rightQuery, Occur.SHOULD);
                        }
                    }
                    return setQuery;
                }
                if (constraint instanceof PropertyExistence) {
                    PropertyExistence existence = (PropertyExistence)constraint;
                    return createQuery(existence);
                }
                if (constraint instanceof Between) {
                    Between between = (Between)constraint;
                    DynamicOperand operand = between.operand();
                    StaticOperand lower = between.lowerBound();
                    StaticOperand upper = between.upperBound();
                    return createQuery(operand,
                                       lower,
                                       upper,
                                       between.isLowerBoundIncluded(),
                                       between.isUpperBoundIncluded(),
                                       true);
                }
                if (constraint instanceof Comparison) {
                    Comparison comparison = (Comparison)constraint;
                    return createQuery(comparison.operand1(), comparison.operator(), comparison.operand2());
                }
                if (constraint instanceof FullTextSearch) {
                    FullTextSearch search = (FullTextSearch)constraint;
                    String propertyName = search.propertyName();
                    if (propertyName != null) propertyName = fieldNameFor(propertyName);
                    String fieldName = fullTextFieldName(propertyName);
                    return createQuery(fieldName, search.getTerm());
                }
                if (constraint instanceof SameNode) {
                    SameNode sameNode = (SameNode)constraint;
                    Path path = pathFactory.create(sameNode.path());
                    return session.findNodeAt(path);
                }
                if (constraint instanceof ChildNode) {
                    ChildNode childNode = (ChildNode)constraint;
                    Path path = pathFactory.create(childNode.parentPath());
                    return session.findChildNodes(path);
                }
                if (constraint instanceof DescendantNode) {
                    DescendantNode descendantNode = (DescendantNode)constraint;
                    Path path = pathFactory.create(descendantNode.ancestorPath());
                    return session.findAllNodesBelow(path);
                }
                // Should not get here ...
                assert false;
                return null;
            }

            public Query createQuery( DynamicOperand left,
                                      Operator operator,
                                      StaticOperand right ) throws IOException {
                return createQuery(left, operator, right, true);
            }

            public Query createQuery( DynamicOperand left,
                                      Operator operator,
                                      StaticOperand right,
                                      boolean caseSensitive ) throws IOException {
                // Handle the static operand ...
                Object value = createOperand(right, caseSensitive);
                assert value != null;

                // Address the dynamic operand ...
                if (left instanceof FullTextSearchScore) {
                    // This can only be represented as a filter ...
                    return null;
                } else if (left instanceof PropertyValue) {
                    return session.findNodesWith((PropertyValue)left, operator, value, caseSensitive);
                } else if (left instanceof ReferenceValue) {
                    return session.findNodesWith((ReferenceValue)left, operator, value);
                } else if (left instanceof Length) {
                    return session.findNodesWith((Length)left, operator, right);
                } else if (left instanceof LowerCase) {
                    LowerCase lowercase = (LowerCase)left;
                    return createQuery(lowercase.operand(), operator, right, false);
                } else if (left instanceof UpperCase) {
                    UpperCase lowercase = (UpperCase)left;
                    return createQuery(lowercase.operand(), operator, right, false);
                } else if (left instanceof NodeDepth) {
                    assert operator != Operator.LIKE;
                    // Could be represented as a result filter, but let's do this now ...
                    return session.findNodesWith((NodeDepth)left, operator, value);
                } else if (left instanceof NodePath) {
                    return session.findNodesWith((NodePath)left, operator, value, caseSensitive);
                } else if (left instanceof NodeName) {
                    return session.findNodesWith((NodeName)left, operator, value, caseSensitive);
                } else if (left instanceof NodeLocalName) {
                    return session.findNodesWith((NodeLocalName)left, operator, value, caseSensitive);
                } else {
                    assert false;
                    return null;
                }
            }

            public Object createOperand( StaticOperand operand,
                                         boolean caseSensitive ) {
                Object value = null;
                if (operand instanceof Literal) {
                    Literal literal = (Literal)operand;
                    value = literal.value();
                    if (!caseSensitive) value = lowerCase(value);
                } else if (operand instanceof BindVariableName) {
                    BindVariableName variable = (BindVariableName)operand;
                    String variableName = variable.variableName();
                    value = variables.get(variableName);
                    if (value instanceof Iterable<?>) {
                        // We can only return one value ...
                        Iterator<?> iter = ((Iterable<?>)value).iterator();
                        if (iter.hasNext()) return iter.next();
                        value = null;
                    }
                    if (value == null) {
                        throw new LuceneException(LuceneI18n.missingVariableValue.text(variableName));
                    }
                    if (!caseSensitive) value = lowerCase(value);
                } else {
                    assert false;
                }
                return value;
            }

            public Query createQuery( DynamicOperand left,
                                      StaticOperand lower,
                                      StaticOperand upper,
                                      boolean includesLower,
                                      boolean includesUpper,
                                      boolean caseSensitive ) throws IOException {
                // Handle the static operands ...
                Object lowerValue = createOperand(lower, caseSensitive);
                Object upperValue = createOperand(upper, caseSensitive);
                assert lowerValue != null;
                assert upperValue != null;

                // Only in the case of a PropertyValue and Depth will we need to do something special ...
                if (left instanceof NodeDepth) {
                    return session.findNodesWithNumericRange((NodeDepth)left,
                                                             lowerValue,
                                                             upperValue,
                                                             includesLower,
                                                             includesUpper);
                } else if (left instanceof PropertyValue) {
                    PropertyType lowerType = PropertyType.discoverType(lowerValue);
                    PropertyType upperType = PropertyType.discoverType(upperValue);
                    if (upperType == lowerType) {
                        switch (upperType) {
                            case DATE:
                            case LONG:
                            case DOUBLE:
                            case DECIMAL:
                                return session.findNodesWithNumericRange((PropertyValue)left,
                                                                         lowerValue,
                                                                         upperValue,
                                                                         includesLower,
                                                                         includesUpper);
                            default:
                                // continue on and handle as boolean query ...
                        }
                    }
                }

                // Otherwise, just create a boolean query ...
                BooleanQuery query = new BooleanQuery();
                Operator lowerOp = includesLower ? Operator.GREATER_THAN_OR_EQUAL_TO : Operator.GREATER_THAN;
                Operator upperOp = includesUpper ? Operator.LESS_THAN_OR_EQUAL_TO : Operator.LESS_THAN;
                Query lowerQuery = createQuery(left, lowerOp, lower, caseSensitive);
                Query upperQuery = createQuery(left, upperOp, upper, caseSensitive);
                if (lowerQuery == null || upperQuery == null) return null;
                query.add(lowerQuery, Occur.MUST);
                query.add(upperQuery, Occur.MUST);
                return query;
            }

            public Object lowerCase( Object value ) {
                if (value instanceof String) {
                    return ((String)value).toLowerCase();
                }
                assert !(value instanceof Binary);
                TypeFactory<String> stringFactory = typeSystem.getStringFactory();
                TypeFactory<?> valueFactory = typeSystem.getTypeFactory(value);
                return valueFactory.create(stringFactory.create(value).toLowerCase());
            }

            public Query createQuery( PropertyExistence existence ) {
                String propertyName = existence.propertyName();
                if ("jcr:primaryType".equals(propertyName)) {
                    // All nodes have a primary type, so therefore we can match all documents ...
                    return new MatchAllDocsQuery();
                }
                return new HasValueQuery(fieldNameFor(propertyName));
            }

            public Query createQuery( String fieldName,
                                      FullTextSearch.Term term ) throws IOException {
                assert fieldName != null;
                if (term instanceof FullTextSearch.Conjunction) {
                    FullTextSearch.Conjunction conjunction = (FullTextSearch.Conjunction)term;
                    BooleanQuery query = new BooleanQuery();
                    for (FullTextSearch.Term nested : conjunction) {
                        if (nested instanceof NegationTerm) {
                            query.add(createQuery(fieldName, ((NegationTerm)nested).getNegatedTerm()), Occur.MUST_NOT);
                        } else {
                            query.add(createQuery(fieldName, nested), Occur.MUST);
                        }
                    }
                    return query;
                }
                if (term instanceof FullTextSearch.Disjunction) {
                    FullTextSearch.Disjunction disjunction = (FullTextSearch.Disjunction)term;
                    BooleanQuery query = new BooleanQuery();
                    for (FullTextSearch.Term nested : disjunction) {
                        if (nested instanceof NegationTerm) {
                            query.add(createQuery(fieldName, ((NegationTerm)nested).getNegatedTerm()), Occur.MUST_NOT);
                        } else {
                            query.add(createQuery(fieldName, nested), Occur.SHOULD);
                        }
                    }
                    return query;
                }
                if (term instanceof FullTextSearch.SimpleTerm) {
                    FullTextSearch.SimpleTerm simple = (FullTextSearch.SimpleTerm)term;
                    Analyzer analyzer = session.getAnalyzer();
                    if (simple.containsWildcards()) {
                        // Use the ComplexPhraseQueryParser, but instead of wildcard queries (which don't work with leading
                        // wildcards) we should use our like queries (which often use RegexQuery where applicable) ...
                        ComplexPhraseQueryParser parser = new ComplexPhraseQueryParser(session.getVersion(), fieldName, analyzer) {
                            @Override
                            protected Query getWildcardQuery( String field,
                                                              String termStr ) {
                                return CompareStringQuery.createQueryForNodesWithFieldLike(termStr, field, valueFactories, false);
                            }
                        };
                        try {
                            String expression = simple.getValue();
                            // The ComplexPhraseQueryParser only understands the '?' and '*' as being wildcards ...
                            expression = expression.replaceAll("(?<![\\\\])_", "?");
                            expression = expression.replaceAll("(?<![\\\\])%", "*");
                            // // Replace any '-' between tokens, except when preceded or followed by a digit, '*', or '?' ...
                            expression = expression.replaceAll("((?<![\\d*?]))[-]((?![\\d*?]))", "$1 $2");
                            // Then use the parser ...
                            Query query = parser.parse(expression);
                            return query;
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                    PhraseQuery query = new PhraseQuery();
                    query.setSlop(0); // terms must be adjacent
                    String expression = simple.getValue();
                    // Run the expression through the Lucene analyzer to extract the terms ...
                    TokenStream stream = session.getAnalyzer().tokenStream(fieldName, new StringReader(expression));
                    TermAttribute termAttribute = stream.addAttribute(TermAttribute.class);
                    while (stream.incrementToken()) {
                        // The term attribute object has been modified to contain the next term ...
                        String analyzedTerm = termAttribute.term();
                        query.add(new Term(fieldName, analyzedTerm));
                    }
                    return query;
                }
                // Should not get here ...
                assert false;
                return null;
            }

            public String fieldNameFor( String name ) {
                // Convert to a name and then to a string, so that the namespaces are resolved
                return stringFactory.create(nameFactory.create(name));
            }
        }
    }

    @NotThreadSafe
    protected static interface WorkspaceSession {

        String getWorkspaceName();

        boolean hasWriters();

        /**
         * Subclasses should implement this method to throw away any work that has been done with this processor.
         */
        void rollback();

        /**
         * Subclasses should implement this method to commit and save any work that has been done with this processor.
         */
        void commit();

        /**
         * Get the number of changes that have been made to the workspace using this session.
         * 
         * @return the number of changes; never negative
         */
        int getChangeCount();

        IndexSearcher getContentSearcher() throws IOException;

        Analyzer getAnalyzer();

        Version getVersion();

        /**
         * Create a {@link TupleCollector} instance that collects the results from the index(es).
         * 
         * @param columns the column definitions; never null
         * @return the collector; never null
         */
        TupleCollector createTupleCollector( Columns columns );

        Query findAllNodesBelow( Path ancestorPath ) throws IOException;

        Query findAllNodesAtOrBelow( Path ancestorPath ) throws IOException;

        /**
         * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
         * supplied path.
         * 
         * @param parentPath the path of the parent node.
         * @return the query; never null
         * @throws IOException if there is an error creating the query
         */
        Query findChildNodes( Path parentPath ) throws IOException;

        /**
         * Create a query that can be used to find the one document (or node) that exists at the exact path supplied.
         * 
         * @param path the path of the node
         * @return the query; never null
         * @throws IOException if there is an error creating the query
         */
        Query findNodeAt( Path path ) throws IOException;

        /**
         * Create a query that can be used to find documents (or nodes) that have a field value that satisfies the supplied LIKE
         * expression.
         * 
         * @param fieldName the name of the document field to search
         * @param likeExpression the JCR like expression
         * @param caseSensitive true if the evaluation should be performed in a case sensitive manner, or false otherwise
         * @return the query; never null
         * @throws IOException if there is an error creating the query
         */
        Query findNodesLike( String fieldName,
                             String likeExpression,
                             boolean caseSensitive ) throws IOException;

        Query findNodesWith( Length propertyLength,
                             Operator operator,
                             Object value ) throws IOException;

        Query findNodesWith( PropertyValue propertyValue,
                             Operator operator,
                             Object value,
                             boolean caseSensitive ) throws IOException;

        Query findNodesWith( ReferenceValue referenceValue,
                             Operator operator,
                             Object value ) throws IOException;

        Query findNodesWithNumericRange( PropertyValue propertyValue,
                                         Object lowerValue,
                                         Object upperValue,
                                         boolean includesLower,
                                         boolean includesUpper ) throws IOException;

        Query findNodesWithNumericRange( NodeDepth depth,
                                         Object lowerValue,
                                         Object upperValue,
                                         boolean includesLower,
                                         boolean includesUpper ) throws IOException;

        Query findNodesWith( NodePath nodePath,
                             Operator operator,
                             Object value,
                             boolean caseSensitive ) throws IOException;

        Query findNodesWith( NodeName nodeName,
                             Operator operator,
                             Object value,
                             boolean caseSensitive ) throws IOException;

        Query findNodesWith( NodeLocalName nodeName,
                             Operator operator,
                             Object value,
                             boolean caseSensitive ) throws IOException;

        Query findNodesWith( NodeDepth depthConstraint,
                             Operator operator,
                             Object value ) throws IOException;
    }

    public static abstract class TupleCollector extends Collector {

        /**
         * Get the tuples.
         * 
         * @return the tuples; never null
         */
        public abstract List<Object[]> getTuples();
    }
}
