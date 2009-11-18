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
package org.jboss.dna.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.Between;
import org.jboss.dna.graph.query.model.BindVariableName;
import org.jboss.dna.graph.query.model.ChildNode;
import org.jboss.dna.graph.query.model.Comparison;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.DescendantNode;
import org.jboss.dna.graph.query.model.DynamicOperand;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.FullTextSearchScore;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.Literal;
import org.jboss.dna.graph.query.model.LowerCase;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Not;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.Or;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SameNode;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.SetCriteria;
import org.jboss.dna.graph.query.model.StaticOperand;
import org.jboss.dna.graph.query.model.UpperCase;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.model.FullTextSearch.NegationTerm;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.process.AbstractAccessComponent;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.SelectComponent;
import org.jboss.dna.graph.query.process.SelectComponent.Analyzer;
import org.jboss.dna.search.DualIndexSearchProvider.ContentIndex;

/**
 * 
 */
/**
 * The {@link ProcessingComponent} implementation that executes a single atomic access query against the Lucene indexes.
 */
public class LuceneQueryComponent extends AbstractAccessComponent {
    private final QueryCommand originalQuery;
    private final LuceneSession session;
    private final String sourceName;
    private final String workspaceName;

    protected LuceneQueryComponent( LuceneSession session,
                                    QueryCommand originalQuery,
                                    QueryContext context,
                                    Columns columns,
                                    PlanNode accessNode,
                                    Analyzer analyzer,
                                    String sourceName,
                                    String workspaceName ) {
        super(context, columns, accessNode);
        this.originalQuery = originalQuery;
        this.session = session;
        this.sourceName = sourceName;
        this.workspaceName = workspaceName;
    }

    protected String fieldNameFor( Name name ) {
        return session.stringFactory.create(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.process.ProcessingComponent#execute()
     */
    @Override
    public List<Object[]> execute() {

        // Some kinds of constraints are not easily pushed down to Lucene as are of a Lucene Query, and
        // instead are applied by filtering the results. For example, a FullTextSearchScore applies
        // to the score of the tuple, which cannot be (easily?) applied as a Query.
        //
        // Therefore, each of the AND-ed constraints of the query are evaluated separately. After all,
        // each of the tuples returned by the planned query must satisfy all of the AND-ed constraints.
        // Or, to put it another way, if a tuple does not satisfy one of the AND-ed constraints, the
        // tuple should not be included in the query results.
        //
        // Logically, any AND-ed criteria that cannot be pushed down to Lucene can of course be applied
        // as a filter on the results. Thus, each AND-ed constraint is processed to first determine if
        // it can be represented as a Lucene query; all other AND-ed constraints must be handled as
        // a results filter. Since most queries will likely use one or more simple constraints AND-ed
        // together, this approach will likely work very well.
        //
        // The only hairy case is when any AND-ed constraint is actually an OR-ed combination of multiple
        // constraints of which at least one cannot be pushed down to Lucene. In this case, the entire
        // AND-ed constraint must be treated as a results filter (even if many of those constraints that
        // make up the OR-ed constraint can be pushed down). Hopefully, this will not be a common case
        // in actual queries.

        // For each of the AND-ed constraints ...
        Query pushDownQuery = null;
        Constraint postProcessConstraint = null;
        try {
            for (Constraint andedConstraint : this.andedConstraints) {
                // Determine if it can be represented as a Lucene query ...
                Query constraintQuery = createQuery(andedConstraint);
                if (constraintQuery != null) {
                    // The AND-ed constraint _can_ be represented as a push-down Lucene query ...
                    if (pushDownQuery == null) {
                        // This must be the first query ...
                        pushDownQuery = constraintQuery;
                    } else if (pushDownQuery instanceof BooleanQuery) {
                        // We have to add the constraint query to the existing boolean ...
                        BooleanQuery booleanQuery = (BooleanQuery)pushDownQuery;
                        booleanQuery.add(constraintQuery, Occur.MUST);
                    } else {
                        // This is the second push-down query, so create a BooleanQuery ...
                        BooleanQuery booleanQuery = new BooleanQuery();
                        booleanQuery.add(pushDownQuery, Occur.MUST);
                        booleanQuery.add(constraintQuery, Occur.MUST);
                        pushDownQuery = booleanQuery;
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
            QueryContext context = getContext();
            I18n msg = SearchI18n.errorWhilePerformingQuery;
            String origQueryString = Visitors.readable(originalQuery, context.getExecutionContext());
            context.getProblems().addError(e, msg, origQueryString, workspaceName, sourceName, e.getMessage());
            return emptyTuples();
        } catch (RuntimeException e) {
            // There was a error working with the constraints (such as a ValueFormatException) ...
            QueryContext context = getContext();
            I18n msg = SearchI18n.errorWhilePerformingQuery;
            String origQueryString = Visitors.readable(originalQuery, context.getExecutionContext());
            context.getProblems().addError(e, msg, origQueryString, workspaceName, sourceName, e.getMessage());
            return emptyTuples();
        }

        if (pushDownQuery == null) {
            // There are no constraints that can be pushed down, so return _all_ the nodes ...
            pushDownQuery = new MatchAllDocsQuery();
        }

        // Get the results from Lucene ...
        List<Object[]> tuples = null;
        final Columns columns = getColumns();
        final QueryContext context = getContext();
        final ExecutionContext execContext = context.getExecutionContext();
        try {
            // Execute the query against the content indexes ...
            IndexSearcher searcher = session.getContentSearcher();
            TupleCollector collector = new TupleCollector(columns, execContext.getValueFactories().getUuidFactory());
            searcher.search(pushDownQuery, collector);
            tuples = collector.getTuples();
        } catch (IOException e) {
            // There was a problem executing the Lucene query ...
            I18n msg = SearchI18n.errorWhilePerformingLuceneQuery;
            String origQueryString = Visitors.readable(originalQuery, execContext);
            context.getProblems().addError(e, msg, pushDownQuery, origQueryString, workspaceName, sourceName, e.getMessage());
            return emptyTuples();
        }

        if (postProcessConstraint != null && !tuples.isEmpty()) {
            // Create a delegate processing component that will return the tuples we've already found ...
            final List<Object[]> allTuples = tuples;
            ProcessingComponent tuplesProcessor = new ProcessingComponent(context, columns) {
                @Override
                public List<Object[]> execute() {
                    return allTuples;
                }
            };
            // Create a processing component that will apply these constraints to the tuples we already found ...
            return new SelectComponent(tuplesProcessor, postProcessConstraint, context.getVariables()).execute();
        }
        return tuples;
    }

    protected Query createQuery( Constraint constraint ) throws IOException {
        if (constraint instanceof And) {
            And and = (And)constraint;
            Query leftQuery = createQuery(and.getLeft());
            Query rightQuery = createQuery(and.getRight());
            if (leftQuery == null || rightQuery == null) return null;
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(createQuery(and.getLeft()), Occur.MUST);
            booleanQuery.add(createQuery(and.getRight()), Occur.MUST);
            return booleanQuery;
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Query leftQuery = createQuery(or.getLeft());
            Query rightQuery = createQuery(or.getRight());
            if (leftQuery == null) {
                return rightQuery != null ? rightQuery : null;
            } else if (rightQuery == null) {
                return leftQuery;
            }
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(createQuery(or.getLeft()), Occur.SHOULD);
            booleanQuery.add(createQuery(or.getRight()), Occur.SHOULD);
            return booleanQuery;
        }
        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            Query notted = createQuery(not.getConstraint());
            if (notted == null) return new MatchAllDocsQuery();
        }
        if (constraint instanceof SetCriteria) {
            SetCriteria setCriteria = (SetCriteria)constraint;
            DynamicOperand left = setCriteria.getLeftOperand();
            int numRightOperands = setCriteria.getRightOperands().size();
            assert numRightOperands > 0;
            if (numRightOperands == 1) {
                return createQuery(left, Operator.EQUAL_TO, setCriteria.getRightOperands().iterator().next());
            }
            BooleanQuery setQuery = new BooleanQuery();
            for (StaticOperand right : setCriteria.getRightOperands()) {
                Query rightQuery = createQuery(left, Operator.EQUAL_TO, right);
                if (rightQuery == null) return null;
                setQuery.add(rightQuery, Occur.SHOULD);
            }
            return setQuery;
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence existence = (PropertyExistence)constraint;
            return createQuery(existence.getSelectorName(), existence.getPropertyName());
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            return createQuery(between);
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            return createQuery(comparison.getOperand1(), comparison.getOperator(), comparison.getOperand2());
        }
        if (constraint instanceof FullTextSearch) {
            FullTextSearch search = (FullTextSearch)constraint;
            String fieldName = ContentIndex.FULL_TEXT;
            Name propertyName = search.getPropertyName();
            if (propertyName != null) {
                fieldName = session.fullTextFieldName(fieldNameFor(propertyName));
            }
            return createQuery(fieldName, search.getTerm());
        }
        try {
            if (constraint instanceof SameNode) {
                SameNode sameNode = (SameNode)constraint;
                return session.findNodeAt(sameNode.getPath());
            }
            if (constraint instanceof ChildNode) {
                ChildNode childNode = (ChildNode)constraint;
                return session.findChildNodes(childNode.getParentPath());
            }
            if (constraint instanceof DescendantNode) {
                DescendantNode descendantNode = (DescendantNode)constraint;
                return session.findAllNodesBelow(descendantNode.getAncestorPath());
            }
        } catch (IOException e) {
            I18n msg = SearchI18n.errorWhilePerformingQuery;
            getContext().getProblems().addError(e,
                                                msg,
                                                Visitors.readable(originalQuery),
                                                workspaceName,
                                                sourceName,
                                                e.getMessage());
            return null;
        }
        // Should not get here ...
        assert false;
        return null;
    }

    protected Query createQuery( DynamicOperand left,
                                 Operator operator,
                                 StaticOperand right ) throws IOException {
        return createQuery(left, operator, right, true);
    }

    protected Query createQuery( DynamicOperand left,
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
        } else if (left instanceof Length) {
            return session.findNodesWith((Length)left, operator, right);
        } else if (left instanceof LowerCase) {
            LowerCase lowercase = (LowerCase)left;
            return createQuery(lowercase.getOperand(), operator, right, false);
        } else if (left instanceof UpperCase) {
            UpperCase lowercase = (UpperCase)left;
            return createQuery(lowercase.getOperand(), operator, right, false);
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

    protected Object createOperand( StaticOperand operand,
                                    boolean caseSensitive ) {
        Object value = null;
        if (operand instanceof Literal) {
            Literal literal = (Literal)operand;
            value = literal.getValue();
            if (!caseSensitive) value = lowerCase(value);
        } else if (operand instanceof BindVariableName) {
            BindVariableName variable = (BindVariableName)operand;
            String variableName = variable.getVariableName();
            value = getContext().getVariables().get(variableName);
            if (!caseSensitive) value = lowerCase(value);
        } else {
            assert false;
        }
        return value;
    }

    protected Query createQuery( DynamicOperand left,
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
            return session.findNodesWithNumericRange((NodeDepth)left, lowerValue, upperValue, includesLower, includesUpper);
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

    protected Object lowerCase( Object value ) {
        if (value instanceof String) {
            return ((String)value).toLowerCase();
        }
        assert !(value instanceof Binary);
        ValueFactory<String> stringFactory = getContext().getExecutionContext().getValueFactories().getStringFactory();
        ValueFactory<?> valueFactory = getContext().getExecutionContext().getValueFactories().getValueFactory(value);
        return valueFactory.create(stringFactory.create(value).toLowerCase());
    }

    protected Query createQuery( SelectorName selectorName,
                                 Name propertyName ) {
        Term term = new Term(fieldNameFor(propertyName));
        return new TermQuery(term);
    }

    protected Query createQuery( String fieldName,
                                 FullTextSearch.Term term ) {
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
            if (simple.isQuotingRequired()) {
                PhraseQuery query = new PhraseQuery();
                query.setSlop(0); // terms must be adjacent
                for (String value : simple.getValues()) {
                    query.add(new Term(fieldName, value));
                }
                return query;
            }
            return new TermQuery(new Term(fieldName, simple.getValue()));
        }
        // Should not get here ...
        assert false;
        return null;
    }

    /**
     * This collector is responsible for loading the value for each of the columns into each tuple array.
     */
    protected static class TupleCollector extends Collector {
        private final LinkedList<Object[]> tuples = new LinkedList<Object[]>();
        private final Columns columns;
        private final int numValues;
        private final boolean recordScore;
        private final int scoreIndex;
        private final FieldSelector fieldSelector;
        private final int locationIndex;
        private final ValueFactory<UUID> uuidFactory;
        private Scorer scorer;
        private IndexReader currentReader;
        private int docOffset;

        protected TupleCollector( Columns columns,
                                  ValueFactory<UUID> uuidFactory ) {
            this.columns = columns;
            this.uuidFactory = uuidFactory;
            assert this.columns != null;
            assert this.uuidFactory != null;
            this.numValues = this.columns.getTupleSize();
            assert this.numValues >= 0;
            assert this.columns.getSelectorNames().size() == 1;
            final String selectorName = this.columns.getSelectorNames().get(0);
            this.locationIndex = this.columns.getLocationIndex(selectorName);
            this.recordScore = this.columns.hasFullTextSearchScores();
            this.scoreIndex = this.recordScore ? this.columns.getFullTextSearchScoreIndexFor(selectorName) : -1;
            final Set<String> columnNames = new HashSet<String>(this.columns.getColumnNames());
            columnNames.add(ContentIndex.UUID); // add the UUID, which we'll put into the Location ...
            this.fieldSelector = new FieldSelector() {
                private static final long serialVersionUID = 1L;

                public FieldSelectorResult accept( String fieldName ) {
                    return columnNames.contains(fieldName) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
                }
            };
        }

        /**
         * @return tuples
         */
        public LinkedList<Object[]> getTuples() {
            return tuples;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#acceptsDocsOutOfOrder()
         */
        @Override
        public boolean acceptsDocsOutOfOrder() {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setNextReader(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public void setNextReader( IndexReader reader,
                                   int docBase ) {
            this.currentReader = reader;
            this.docOffset = docBase;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#setScorer(org.apache.lucene.search.Scorer)
         */
        @Override
        public void setScorer( Scorer scorer ) {
            this.scorer = scorer;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Collector#collect(int)
         */
        @Override
        public void collect( int doc ) throws IOException {
            int docId = doc + docOffset;
            Object[] tuple = new Object[numValues];
            Document document = currentReader.document(docId, fieldSelector);
            for (String columnName : columns.getColumnNames()) {
                int index = columns.getColumnIndexForName(columnName);
                // We just need to retrieve the first value if there is more than one ...
                tuple[index] = document.get(columnName);
            }

            // Set the score column if required ...
            if (recordScore) {
                assert scorer != null;
                tuple[scoreIndex] = scorer.score();
            }

            // Load the UUID into a Location object ...
            UUID uuid = uuidFactory.create(document.get(ContentIndex.UUID));
            tuple[locationIndex] = Location.create(uuid);
            tuples.add(tuple);
        }
    }
}
