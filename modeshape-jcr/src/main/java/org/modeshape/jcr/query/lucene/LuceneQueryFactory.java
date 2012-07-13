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
package org.modeshape.jcr.query.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.NodeDepth;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.lucene.CaseOperations.CaseOperation;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.FullTextSearch.NegationTerm;
import org.modeshape.jcr.query.model.FullTextSearchScore;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.SameNode;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.query.validate.Schemata.Column;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * The factory that creates a Lucene {@link Query} object from a Query Object Model {@link Constraint} object.
 */
@NotThreadSafe
public abstract class LuceneQueryFactory {

    protected final QueryContext context;
    protected final SearchFactory searchFactory;
    protected final ValueFactories factories;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final ValueFactory<String> stringFactory;
    protected final Version version;
    private Schemata schemata;

    protected LuceneQueryFactory( QueryContext context,
                                  SearchFactory searchFactory,
                                  Version version ) {
        this.context = context;
        this.searchFactory = searchFactory;
        this.version = version;
        this.factories = this.context.getExecutionContext().getValueFactories();
        this.pathFactory = factories.getPathFactory();
        this.nameFactory = factories.getNameFactory();
        this.stringFactory = factories.getStringFactory();
    }

    /**
     * Create a Lucene {@link Query} that represents the supplied Query Object Model {@link Constraint}.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param constraint the QOM constraint; never null
     * @return the corresponding Query object; never null
     * @throws IOException if there is an error creating the query
     */
    public Query createQuery( SelectorName selectorName,
                              Constraint constraint ) throws IOException {
        if (constraint instanceof And) {
            And and = (And)constraint;
            Query leftQuery = createQuery(selectorName, and.left());
            Query rightQuery = createQuery(selectorName, and.right());
            if (leftQuery == null || rightQuery == null) return null;
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(leftQuery, Occur.MUST);
            booleanQuery.add(rightQuery, Occur.MUST);
            return booleanQuery;
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Query leftQuery = createQuery(selectorName, or.left());
            Query rightQuery = createQuery(selectorName, or.right());
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
            return not(createQuery(selectorName, not.getConstraint()));
        }
        if (constraint instanceof SetCriteria) {
            SetCriteria setCriteria = (SetCriteria)constraint;
            DynamicOperand left = setCriteria.leftOperand();
            int numRightOperands = setCriteria.rightOperands().size();
            assert numRightOperands > 0;
            if (numRightOperands == 1) {
                StaticOperand rightOperand = setCriteria.rightOperands().iterator().next();
                if (rightOperand instanceof Literal) {
                    return createQuery(selectorName, left, Operator.EQUAL_TO, setCriteria.rightOperands().iterator().next());
                }
            }
            BooleanQuery setQuery = new BooleanQuery();
            for (StaticOperand right : setCriteria.rightOperands()) {
                if (right instanceof BindVariableName) {
                    // This single value is a variable name, which may evaluate to a single value or multiple values ...
                    BindVariableName var = (BindVariableName)right;
                    Object value = context.getVariables().get(var.getBindVariableName());
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
                            Query rightQuery = createQuery(selectorName, left, Operator.EQUAL_TO, elementInRight);
                            if (rightQuery == null) continue;
                            setQuery.add(rightQuery, Occur.SHOULD);
                        }
                    }
                    if (value == null) {
                        throw new LuceneException(JcrI18n.missingVariableValue.text(var.getBindVariableName()));
                    }
                } else {
                    Query rightQuery = createQuery(selectorName, left, Operator.EQUAL_TO, right);
                    if (rightQuery == null) return null;
                    setQuery.add(rightQuery, Occur.SHOULD);
                }
            }
            return setQuery;
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence existence = (PropertyExistence)constraint;
            return createQuery(selectorName, existence);
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand operand = between.getOperand();
            StaticOperand lower = between.getLowerBound();
            StaticOperand upper = between.getUpperBound();
            return createQuery(selectorName,
                               operand,
                               lower,
                               upper,
                               between.isLowerBoundIncluded(),
                               between.isUpperBoundIncluded(),
                               null);
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            return createQuery(selectorName, comparison.getOperand1(), comparison.operator(), comparison.getOperand2());
        }
        if (constraint instanceof FullTextSearch) {
            FullTextSearch search = (FullTextSearch)constraint;
            String propertyName = search.getPropertyName();
            if (propertyName != null) propertyName = fieldNameFor(propertyName);
            String fieldName = fullTextFieldName(propertyName);
            return createQuery(selectorName, fieldName, search.getTerm());
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            Path path = pathFactory.create(sameNode.getPath());
            return findNodeAt(path);
        }
        if (constraint instanceof ChildNode) {
            ChildNode childNode = (ChildNode)constraint;
            Path path = pathFactory.create(childNode.getParentPath());
            return findChildNodes(path);
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            Path path = pathFactory.create(descendantNode.getAncestorPath());
            return findAllNodesBelow(path);
        }
        // Should not get here ...
        assert false : "Unexpected Constraint instance: class=" + (constraint != null ? constraint.getClass() : "null")
                       + " and instance=" + constraint;
        return null;
    }

    public Query createQuery( SelectorName selectorName,
                              DynamicOperand left,
                              Operator operator,
                              StaticOperand right ) throws IOException {
        return createQuery(selectorName, left, operator, right, null);
    }

    /**
     * Create a comparison query
     * 
     * @param selectorName
     * @param left
     * @param operator
     * @param right
     * @param caseOperation the case operation if known, or null if not known. Normally a non-null value would be required, but in
     *        the case of chained functions such as UPPER(LOWER(...)) we only want to use the first one, so passing null allows us
     *        to easily use the outer one
     * @return the query
     * @throws IOException
     */
    protected Query createQuery( SelectorName selectorName,
                                 DynamicOperand left,
                                 Operator operator,
                                 StaticOperand right,
                                 CaseOperation caseOperation ) throws IOException {
        // Handle the static operand ...
        Object value = createOperand(selectorName, right, caseOperation);
        assert value != null;

        // Address the dynamic operand ...
        if (left instanceof FullTextSearchScore) {
            // This can only be represented as a filter ...
            return null;
        } else if (left instanceof PropertyValue) {
            return findNodesWith(selectorName, (PropertyValue)left, operator, value, caseOperation);
        } else if (left instanceof ReferenceValue) {
            return findNodesWith(selectorName, (ReferenceValue)left, operator, value);
        } else if (left instanceof Length) {
            return findNodesWith(selectorName, (Length)left, operator, value);
        } else if (left instanceof LowerCase) {
            LowerCase lowercase = (LowerCase)left;
            if (caseOperation == null) caseOperation = CaseOperations.LOWERCASE;
            return createQuery(selectorName, lowercase.getOperand(), operator, right, caseOperation);
        } else if (left instanceof UpperCase) {
            UpperCase uppercase = (UpperCase)left;
            if (caseOperation == null) caseOperation = CaseOperations.UPPERCASE;
            return createQuery(selectorName, uppercase.getOperand(), operator, right, caseOperation);
        } else if (left instanceof NodeDepth) {
            assert operator != Operator.LIKE;
            // Could be represented as a result filter, but let's do this now ...
            return findNodesWith(selectorName, (NodeDepth)left, operator, value);
        } else if (left instanceof NodePath) {
            return findNodesWith(selectorName, (NodePath)left, operator, value, caseOperation);
        } else if (left instanceof NodeName) {
            return findNodesWith(selectorName, (NodeName)left, operator, value, caseOperation);
        } else if (left instanceof NodeLocalName) {
            return findNodesWith(selectorName, (NodeLocalName)left, operator, value, caseOperation);
        }

        assert false : "Unexpected DynamicOperand instance: class=" + (left != null ? left.getClass() : "null")
                       + " and instance=" + left;
        return null;
    }

    public Object createOperand( SelectorName selectorName,
                                 StaticOperand operand,
                                 CaseOperation caseOperation ) {
        Object value = null;
        if (caseOperation == null) caseOperation = CaseOperations.AS_IS;
        if (operand instanceof Literal) {
            Literal literal = (Literal)operand;
            value = literal.value();
            // if (value instanceof String || value instanceof Binary) {
            // value = caseOperation.execute(stringValueFrom(value));
            // }
        } else if (operand instanceof BindVariableName) {
            BindVariableName variable = (BindVariableName)operand;
            String variableName = variable.getBindVariableName();
            value = context.getVariables().get(variableName);
            if (value instanceof Iterable<?>) {
                // We can only return one value ...
                Iterator<?> iter = ((Iterable<?>)value).iterator();
                if (iter.hasNext()) return iter.next();
                value = null;
            }
            if (value == null) {
                throw new LuceneException(JcrI18n.missingVariableValue.text(variableName));
            }
            // if (value instanceof String || value instanceof Binary) {
            // value = caseOperation.execute(stringValueFrom(value));
            // }
        } else {
            assert false;
        }
        return value;
    }

    public Query createQuery( SelectorName selectorName,
                              DynamicOperand left,
                              StaticOperand lower,
                              StaticOperand upper,
                              boolean includesLower,
                              boolean includesUpper,
                              CaseOperation caseOperation ) throws IOException {
        // Handle the static operands ...
        Object lowerValue = createOperand(selectorName, lower, caseOperation);
        Object upperValue = createOperand(selectorName, upper, caseOperation);
        assert lowerValue != null;
        assert upperValue != null;

        // Only in the case of a PropertyValue and Depth will we need to do something special ...
        if (left instanceof NodeDepth) {
            return findNodesWithNumericRange(selectorName, (NodeDepth)left, lowerValue, upperValue, includesLower, includesUpper);
        } else if (left instanceof PropertyValue) {
            PropertyType lowerType = PropertyType.discoverType(lowerValue);
            PropertyType upperType = PropertyType.discoverType(upperValue);
            if (upperType == lowerType) {
                switch (upperType) {
                    case DATE:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                        return findNodesWithNumericRange(selectorName,
                                                         (PropertyValue)left,
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
        Query lowerQuery = createQuery(selectorName, left, lowerOp, lower, caseOperation);
        Query upperQuery = createQuery(selectorName, left, upperOp, upper, caseOperation);
        if (lowerQuery == null || upperQuery == null) return null;
        query.add(lowerQuery, Occur.MUST);
        query.add(upperQuery, Occur.MUST);
        return query;
    }

    protected String stringValueFrom( Object value ) {
        if (value == null) return null;
        if (value instanceof String) {
            return (String)value;
        }
        return stringFactory.create(value);
    }

    public Query createQuery( SelectorName selectorName,
                              PropertyExistence existence ) {
        String propertyName = existence.getPropertyName();
        if ("jcr:primaryType".equals(propertyName)) {
            // All nodes have a primary type, so therefore we can match all documents ...
            return new MatchAllDocsQuery();
        }
        return new HasValueQuery(fieldNameFor(propertyName));
    }

    public Query createQuery( final SelectorName selectorName,
                              String fieldName,
                              FullTextSearch.Term term ) throws IOException {
        assert fieldName != null;
        if (term instanceof FullTextSearch.Conjunction) {
            FullTextSearch.Conjunction conjunction = (FullTextSearch.Conjunction)term;
            BooleanQuery query = new BooleanQuery();
            for (FullTextSearch.Term nested : conjunction) {
                if (nested instanceof NegationTerm) {
                    query.add(createQuery(selectorName, fieldName, ((NegationTerm)nested).getNegatedTerm()), Occur.MUST_NOT);
                } else {
                    query.add(createQuery(selectorName, fieldName, nested), Occur.MUST);
                }
            }
            return query;
        }
        if (term instanceof FullTextSearch.Disjunction) {
            FullTextSearch.Disjunction disjunction = (FullTextSearch.Disjunction)term;
            BooleanQuery query = new BooleanQuery();
            for (FullTextSearch.Term nested : disjunction) {
                if (nested instanceof NegationTerm) {
                    query.add(createQuery(selectorName, fieldName, ((NegationTerm)nested).getNegatedTerm()), Occur.MUST_NOT);
                } else {
                    query.add(createQuery(selectorName, fieldName, nested), Occur.SHOULD);
                }
            }
            return query;
        }
        if (term instanceof FullTextSearch.SimpleTerm) {
            FullTextSearch.SimpleTerm simple = (FullTextSearch.SimpleTerm)term;
            Analyzer analyzer = getFullTextSearchAnalyzer();
            if (simple.containsWildcards()) {
                // Use the ComplexPhraseQueryParser, but instead of wildcard queries (which don't work with leading
                // wildcards) we should use our like queries (which often use RegexQuery where applicable) ...
                QueryParser parser = new QueryParser(version, fieldName, analyzer) {
                    @Override
                    protected org.apache.lucene.search.Query getWildcardQuery( String field,
                                                                               String termStr ) {
                        return findNodesLike(selectorName, termStr.toLowerCase(), field, CaseOperations.LOWERCASE);
                    }
                };
                parser.setAllowLeadingWildcard(true);
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
            TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(expression));
            CharTermAttribute termAttribute = stream.addAttribute(CharTermAttribute.class);
            while (stream.incrementToken()) {
                // The term attribute object has been modified to contain the next term ...
                String analyzedTerm = termAttribute.toString();
                query.add(new Term(fieldName, analyzedTerm));
            }
            return query;
        }
        // Should not get here ...
        assert false;
        return null;
    }

    protected Query not( Query notted ) {
        BooleanQuery query = new BooleanQuery();
        // We need at least some positive match, so get all docs ...
        query.add(new MatchAllDocsQuery(), Occur.SHOULD);
        // Now apply the original query being 'NOT-ed' as a MUST_NOT occurrence ...
        query.add(notted, Occur.MUST_NOT);
        return query;
    }

    protected String fieldNameFor( String name ) {
        // Convert to a name and then to a string, so that the namespaces are resolved
        return stringFactory.create(nameFactory.create(name));
    }

    /**
     * Create the field name that will be used to store the full-text searchable property values.
     * 
     * @param propertyName the name of the property; may not null
     * @return the field name for the full-text searchable property values; never null
     */
    protected abstract String fullTextFieldName( String propertyName );

    protected abstract Analyzer getFullTextSearchAnalyzer();

    /**
     * Return a query that will find all documents representing nodes <i>below</i> the supplied path.
     * 
     * @param ancestorPath the path of the ancestor node; never null
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findAllNodesBelow( Path ancestorPath ) throws IOException;

    /**
     * Return a query that will find all documents representing nodes <i>at or below</i> the supplied path.
     * 
     * @param ancestorPath the path of the ancestor node; never null
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findAllNodesAtOrBelow( Path ancestorPath ) throws IOException;

    /**
     * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
     * supplied path.
     * 
     * @param parentPath the path of the parent node.
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findChildNodes( Path parentPath ) throws IOException;

    /**
     * Create a query that can be used to find the one document (or node) that exists at the exact path supplied.
     * 
     * @param path the path of the node
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodeAt( Path path ) throws IOException;

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is LIKE the supplied
     * constraint value, where the LIKE expression contains the SQL wildcard characters '%' and '_' or the regular expression
     * wildcard characters '*' and '?'.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param fieldName the name of the document field to search
     * @param likeExpression the JCR like expression
     * @param caseOperation the operation that should be performed on the indexed string before being used; may not be null
     * @return the query; never null
     */
    protected abstract Query findNodesLike( SelectorName selectorName,
                                            String fieldName,
                                            String likeExpression,
                                            CaseOperation caseOperation );

    /**
     * Create a query that finds documents with fields whose lengths fit the supplied operator/value criteria.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param propertyLength the property specification
     * @param operator the comparison operator
     * @param value the length value
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodesWith( SelectorName selectorName,
                                            Length propertyLength,
                                            Operator operator,
                                            Object value ) throws IOException;

    /**
     * Create a query that finds documents with fields whose values fit the supplied operator/value criteria.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param propertyValue the property specification
     * @param operator the comparison operator
     * @param value the value
     * @param caseOperation the operation that should be used against the string values in the indexes before applying the
     *        criteria, or null if the operation is not known
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodesWith( SelectorName selectorName,
                                            PropertyValue propertyValue,
                                            Operator operator,
                                            Object value,
                                            CaseOperation caseOperation ) throws IOException;

    /**
     * Create a query that finds documents with fields that are references that fit the supplied operator/value criteria.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param referenceValue the property specification
     * @param operator the comparison operator
     * @param value the reference value
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodesWith( SelectorName selectorName,
                                            ReferenceValue referenceValue,
                                            Operator operator,
                                            Object value ) throws IOException;

    /**
     * Create a query that finds documents with fields with values that are in the supplied range.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param propertyValue the property specification
     * @param lowerValue the lower value
     * @param upperValue the upper value
     * @param includesLower true if the range should include the lower value, or false if the lower value should be excluded
     * @param includesUpper true if the range should include the upper value, or false if the upper value should be excluded
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodesWithNumericRange( SelectorName selectorName,
                                                        PropertyValue propertyValue,
                                                        Object lowerValue,
                                                        Object upperValue,
                                                        boolean includesLower,
                                                        boolean includesUpper ) throws IOException;

    /**
     * Create a query that finds documents with fields with depths that are in the supplied range.
     * 
     * @param selectorName the name of the selector (or node type) being queried; may not be null
     * @param depth the property specification
     * @param lowerValue the lower value
     * @param upperValue the upper value
     * @param includesLower true if the range should include the lower value, or false if the lower value should be excluded
     * @param includesUpper true if the range should include the upper value, or false if the upper value should be excluded
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    protected abstract Query findNodesWithNumericRange( SelectorName selectorName,
                                                        NodeDepth depth,
                                                        Object lowerValue,
                                                        Object upperValue,
                                                        boolean includesLower,
                                                        boolean includesUpper ) throws IOException;

    protected abstract Query findNodesWith( SelectorName selectorName,
                                            NodePath nodePath,
                                            Operator operator,
                                            Object value,
                                            CaseOperation caseOperation ) throws IOException;

    protected abstract Query findNodesWith( SelectorName selectorName,
                                            NodeName nodeName,
                                            Operator operator,
                                            Object value,
                                            CaseOperation caseOperation ) throws IOException;

    protected abstract Query findNodesWith( SelectorName selectorName,
                                            NodeLocalName nodeName,
                                            Operator operator,
                                            Object value,
                                            CaseOperation caseOperation ) throws IOException;

    protected abstract Query findNodesWith( SelectorName selectorName,
                                            NodeDepth depthConstraint,
                                            Operator operator,
                                            Object value ) throws IOException;

    protected Schemata schemata() {
        if (schemata == null) {
            schemata = this.context.getSchemata();
        }
        return schemata;
    }

    protected Column getMetadataFor( SelectorName selectorName,
                                     String propertyName ) {
        return schemata().getTable(selectorName).getColumn(propertyName);
    }
}
