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
package org.modeshape.jcr.index.lucene.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.Cast;
import org.modeshape.jcr.api.query.qom.NodeDepth;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.index.lucene.FieldUtil;
import org.modeshape.jcr.index.lucene.LuceneConfig;
import org.modeshape.jcr.index.lucene.LuceneIndexException;
import org.modeshape.jcr.index.lucene.LuceneIndexProviderI18n;
import org.modeshape.jcr.query.engine.QueryUtil;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.Relike;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.Subquery;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * The factory that creates a Lucene {@link Query} object from a Query Object Model {@link Constraint} object.
 *
 * @since 4.5
 */
@ThreadSafe
@Immutable
@SuppressWarnings("deprecation")
public class LuceneQueryFactory {

    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final StringFactory stringFactory;
    protected final Map<String, Object> variables;
    protected final ValueFactories factories;
    protected final Map<String, PropertyType> propertyTypesByName;

    private LuceneQueryFactory( ValueFactories factories,
                                Map<String, Object> variables,
                                Map<String, PropertyType> propertyTypesByName ) {
        assert factories != null;
        this.factories = factories;
        this.pathFactory = factories.getPathFactory();
        this.nameFactory = factories.getNameFactory();
        this.stringFactory = factories.getStringFactory();

        this.variables = variables != null ? variables : Collections.emptyMap();
        assert propertyTypesByName != null;
        this.propertyTypesByName = propertyTypesByName;
    }

    /**
     * Creates a new query factory which can be used to produce Lucene queries for {@link org.modeshape.jcr.index.lucene.MultiColumnIndex}
     * indexes.
     *
     * @param factories a {@link ValueFactories} instance; may not be null
     * @param variables a {@link Map} instance which contains the query variables for a particular query; may be {@code null}
     * @param propertyTypesByName a {@link Map} representing the columns and their types for the index definition
     * for which the query should be created; may not be null.
     * @return a {@link LuceneQueryFactory} instance, never {@code null}
     */
    public static LuceneQueryFactory forMultiColumnIndex( ValueFactories factories,
                                                          Map<String, Object> variables,
                                                          Map<String, PropertyType> propertyTypesByName ) {
        return new LuceneQueryFactory(factories, variables, propertyTypesByName);
    }

    /**
     * Creates a new query factory which can be used to produce Lucene queries for {@link org.modeshape.jcr.index.lucene.SingleColumnIndex}
     * indexes.
     *
     * @param factories a {@link ValueFactories} instance; may not be null
     * @param variables a {@link Map} instance which contains the query variables for a particular query; may be {@code null}
     * @param propertyTypesByName a {@link Map} representing the columns and their types for the index definition
     * for which the query should be created; may not be null.
     * @return a {@link LuceneQueryFactory} instance, never {@code null}
     */
    public static LuceneQueryFactory forSingleColumnIndex( ValueFactories factories,
                                                           Map<String, Object> variables,
                                                           Map<String, PropertyType> propertyTypesByName ) {
        return new SingleColumnQueryFactory(factories, variables, propertyTypesByName);
    }

    /**
     * Creates a new query factory which can be used to produce Lucene queries for {@link org.modeshape.jcr.index.lucene.TextIndex}
     * indexes.
     *
     * @param factories a {@link ValueFactories} instance; may not be null
     * @param variables a {@link Map} instance which contains the query variables for a particular query; may be {@code null}
     * @param propertyTypesByName a {@link Map} representing the columns and their types for the index definition
     * for which the query should be created; may not be null.
     * @param config a {@link LuceneConfig} instance required to get various information for FTS (e.g. configured analyzer); may
     * not be null
     * @return a {@link LuceneQueryFactory} instance, never {@code null}
     */
    public static LuceneQueryFactory forTextIndex( ValueFactories factories,
                                                   Map<String, Object> variables,
                                                   Map<String, PropertyType> propertyTypesByName,
                                                   LuceneConfig config ) {
        return new TextQueryFactory(factories, variables, propertyTypesByName, config);
    }

    /**
     * Create a Lucene {@link Query} that represents the supplied Query Object Model {@link Constraint}.
     *
     * @param constraint the QOM constraint; never null
     * @return the corresponding Query object; never null
     */
    public Query createQuery( Constraint constraint ) {
        if (constraint instanceof And) {
            return createQuery((And) constraint);
        }
        if (constraint instanceof Or) {
            return createQuery((Or) constraint);
        }
        if (constraint instanceof Not) {
            return createQuery((Not) constraint);
        }
        if (constraint instanceof SetCriteria) {
            return createQuery((SetCriteria) constraint);
        }
        if (constraint instanceof PropertyExistence) {
            return createQuery((PropertyExistence) constraint);
        }
        if (constraint instanceof Between) {
            return createQuery((Between) constraint);
        }
        if (constraint instanceof Relike) {
            return createQuery((Relike) constraint);
        }
        if (constraint instanceof Comparison) {
            return createQuery((Comparison) constraint);
        }
        if (constraint instanceof FullTextSearch) {
            return createQuery((FullTextSearch) constraint);
        }
        // Should not get here ...
        throw new LuceneIndexException(
                "Unexpected Constraint instance: class=" + (constraint != null ? constraint.getClass() : "null")
                + " and instance=" + constraint);
    }

    /**
     * Checks if for the query produced by this factory scores are expected or not for matching documents.
     *
     * @return {@code true} if scores are expected to matching documents, {@code false} otherwise
     */
    public boolean scoreDocuments() {
        return false;
    }

    protected Query createQuery( FullTextSearch constraint ) {
        throw new UnsupportedOperationException("Only text indexes support FTS constraints...");
    }

    protected Query createQuery( Comparison comparison ) {
        return createQuery(comparison.getOperand1(), comparison.operator(), comparison.getOperand2(), null);
    }

    protected Query createQuery( Not not ) {
        return not(createQuery(not.getConstraint()));
    }

    protected Query createQuery( Relike relike ) {
        StaticOperand op1 = relike.getOperand1();
        PropertyValue op2 = relike.getOperand2();

        Object relikeValue = getSingleValueFromStaticOperand(op1);
        assert relikeValue != null;
        String fieldName = op2.getPropertyName();
        return new RelikeQuery(fieldName, relikeValue.toString());
    }

    protected Query createQuery( Between between ) {
        DynamicOperand operand = between.getOperand();

        StaticOperand lower = between.getLowerBound();
        StaticOperand upper = between.getUpperBound();

        boolean upperBoundIncluded = between.isUpperBoundIncluded();
        boolean lowerBoundIncluded = between.isLowerBoundIncluded();

        // Handle the static operands ...
        Object lowerValue = getSingleValueFromStaticOperand(lower);
        Object upperValue = getSingleValueFromStaticOperand(upper);
        assert lowerValue != null;
        assert upperValue != null;

        // Only in the case of a PropertyValue and Depth will we need to do something special ...
        if (operand instanceof NodeDepth) {
            return createRangeQuery(depthField(), lowerValue, upperValue, lowerBoundIncluded, upperBoundIncluded);
        } else if (operand instanceof PropertyValue) {
            String field = ((PropertyValue) operand).getPropertyName();
            PropertyType lowerType = PropertyType.discoverType(lowerValue);
            PropertyType upperType = PropertyType.discoverType(upperValue);
            if (upperType == lowerType) {
                switch (upperType) {
                    case DATE:
                    case LONG:
                    case DOUBLE:
                    case DECIMAL:
                        return createRangeQuery(field, lowerValue, upperValue, lowerBoundIncluded, upperBoundIncluded);

                    default:
                        // continue on and handle as boolean query ...
                }
            }
        }

        // Otherwise, just create a boolean query ...
        Operator lowerOp = lowerBoundIncluded ? Operator.GREATER_THAN_OR_EQUAL_TO : Operator.GREATER_THAN;
        Operator upperOp = upperBoundIncluded ? Operator.LESS_THAN_OR_EQUAL_TO : Operator.LESS_THAN;
        Query lowerQuery = createQuery(operand, lowerOp, lower, null);
        Query upperQuery = createQuery(operand, upperOp, upper, null);
        return booleanQuery(lowerQuery, Occur.MUST, upperQuery, Occur.MUST);
    }

    protected Query createRangeQuery( String field, Object lowerValue, Object upperValue, boolean includesLower,
                                      boolean includesUpper ) {
        PropertyType type = null;
        PropertyType lowerType = PropertyType.discoverType(lowerValue);
        PropertyType upperType = PropertyType.discoverType(upperValue);
        if (lowerType != upperType) {
            // the types of the bounds don't match, so nothing can be done
            return new MatchNoDocsQuery();
        } else {
            type = lowerType;
        }

        switch (type) {
            case DATE:
                long lowerDate = factories.getLongFactory().create(lowerValue);
                long upperDate = factories.getLongFactory().create(upperValue);
                return LegacyNumericRangeQuery.newLongRange(field, lowerDate, upperDate, includesLower, includesUpper);
            case LONG:
                long lowerLong = factories.getLongFactory().create(lowerValue);
                long upperLong = factories.getLongFactory().create(upperValue);
                return LegacyNumericRangeQuery.newLongRange(field, lowerLong, upperLong, includesLower, includesUpper);
            case DOUBLE:
                double lowerDouble = factories.getDoubleFactory().create(lowerValue);
                double upperDouble = factories.getDoubleFactory().create(upperValue);
                return LegacyNumericRangeQuery.newDoubleRange(field, lowerDouble, upperDouble, includesLower, includesUpper);
            case BOOLEAN:
                int lowerInt = factories.getBooleanFactory().create(lowerValue) ? 1 : 0;
                int upperInt = factories.getBooleanFactory().create(upperValue) ? 1 : 0;
                return LegacyNumericRangeQuery.newIntRange(field, lowerInt, upperInt, includesLower, includesUpper);
            case DECIMAL:
                BigDecimal lowerDecimal = factories.getDecimalFactory().create(lowerValue);
                BigDecimal upperDecimal = factories.getDecimalFactory().create(upperValue);
                String lsv = FieldUtil.decimalToString(lowerDecimal);
                String usv = FieldUtil.decimalToString(upperDecimal);
                Query lower = null;
                if (includesLower) {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(lsv, field, null);
                } else {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(lsv, field, null);
                }
                Query upper = null;
                if (includesUpper) {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(usv, field, null);
                } else {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThan(usv, field, null);
                }
                return booleanQuery(lower, Occur.MUST, upper, Occur.MUST);
            case OBJECT:
            case URI:
            case PATH:
            case NAME:
            case STRING:
            case REFERENCE:
            case WEAKREFERENCE:
            case SIMPLEREFERENCE:
            case BINARY:
                throw new LuceneIndexException("Unsupported type for range query:" + type);
        }
        return new MatchNoDocsQuery();
    }


    protected Query createQuery( SetCriteria setCriteria ) {
        DynamicOperand left = setCriteria.leftOperand();
        int numRightOperands = setCriteria.rightOperands().size();
        assert numRightOperands > 0;
        if (numRightOperands == 1) {
            StaticOperand rightOperand = setCriteria.rightOperands().iterator().next();
            if (rightOperand instanceof Literal) {
                return createQuery(left, Operator.EQUAL_TO, setCriteria.rightOperands().iterator().next(),null);
            }
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.setDisableCoord(true);
        for (StaticOperand right : setCriteria.rightOperands()) {
            if (right instanceof BindVariableName) {
                // This single value is a variable name, which may evaluate to a single value or multiple values ...
                BindVariableName var = (BindVariableName) right;
                String bindVariableName = var.getBindVariableName();
                Object value = variables.get(bindVariableName);
                if (value == null) {
                    if (bindVariableName.startsWith(Subquery.VARIABLE_PREFIX)) {
                        // when subqueries are involved during the planning phase, they will not be resolved yet and therefore
                        // a 'null' value present in the variables map. However, since the index was called in the first place,
                        // we know it must apply to the constraint. Therefore, return all the docs in the index to make sure
                        // that during planning, this index is taken into account and can be compared with other "competing" indexes.
                        // Note that the real cardinality wil really be "at most" all documents (possibly less) depending on 
                        // the actual subquery results. These will be resolved during the actual query-run time.
                        return new MatchAllDocsQuery();
                    }
                    throw new LuceneIndexException(JcrI18n.missingVariableValue.text(bindVariableName));
                }
                if (value instanceof Iterable<?>) {
                    for (Object resolvedValue : (Iterable<?>) value) {
                        if (resolvedValue == null) {
                            continue;
                        }
                        if (resolvedValue instanceof Object[]) {
                            // The row has multiple values (e.g., a multi-valued property) ...
                            for (Object val : (Object[]) resolvedValue) {
                                addQueryForSetConstraint(builder, left, val);
                            }
                        } else {
                            addQueryForSetConstraint(builder, left, resolvedValue);
                        }
                    }
                } else {
                    addQueryForSetConstraint(builder, left, value);
                }
            } else {
                Query rightQuery = createQuery(left, Operator.EQUAL_TO, right, null);
                builder.add(rightQuery, Occur.SHOULD);
            }
        }
        return builder.build();
    }

    private Query createQuery( Or or ) {
        Query leftQuery = createQuery(or.left());
        Query rightQuery = createQuery(or.right());
        if (leftQuery == null) {
            return rightQuery != null ? rightQuery : null;
        } else if (rightQuery == null) {
            return leftQuery;
        }
        return booleanQuery(leftQuery, Occur.SHOULD, rightQuery, Occur.SHOULD);
    }

    protected Query createQuery( And and ) {
        Query leftQuery = createQuery(and.left());
        Query rightQuery = createQuery(and.right());
        if (leftQuery == null || rightQuery == null) {
            return null;
        }
        return booleanQuery(leftQuery, Occur.MUST, rightQuery, Occur.MUST);
    }

    protected Query createQuery( PropertyExistence propertyExistence ) {
        String field = propertyExistence.getPropertyName();
        assert propertyTypesByName.containsKey(field); //or this index should not have been planned in the first place...
        return new FieldExistsQuery(field);
    }

    private void addQueryForSetConstraint( BooleanQuery.Builder setQueryBuilder, DynamicOperand left, Object resolvedValue ) {
        StaticOperand elementInRight = resolvedValue instanceof Literal ? (Literal) resolvedValue : new Literal(resolvedValue);
        Query rightQuery = createQuery(left, Operator.EQUAL_TO, elementInRight, null);
        setQueryBuilder.add(rightQuery, Occur.SHOULD);
    }

    protected Query createQuery( DynamicOperand left,
                                 Operator operator,
                                 StaticOperand right,
                                 Function<String, String> caseOperation) {
        // Handle the static operand ...
        Object value = getSingleValueFromStaticOperand(right);
        assert value != null;

        // Address the dynamic operand ...
        if (left instanceof PropertyValue) {
            return createPropertyValueQuery((PropertyValue) left, operator, value, caseOperation);
        } else if (left instanceof ReferenceValue) {
            return createReferenceValueQuery((ReferenceValue) left, operator, value);
        } else if (left instanceof Length) {
            return createLengthQuery((Length) left, operator, value);
        } else if (left instanceof LowerCase) {
            LowerCase lowercase = (LowerCase) left;
            return createQuery(lowercase.getOperand(), operator, right, String::toLowerCase);
        } else if (left instanceof UpperCase) {
            UpperCase uppercase = (UpperCase) left;
            return createQuery(uppercase.getOperand(), operator, right, String::toUpperCase);
        } else if (left instanceof NodeDepth) {
            // this only applies to mode:depth
            return longFieldQuery(depthField(), operator, value);
        } else if (left instanceof NodePath) {
            // this only applies to jcr:path
            String field = stringFactory.create(JcrLexicon.PATH);
            return pathFieldQuery(field, operator, value, caseOperation);
        } else if (left instanceof NodeName) {
            // this only applies to jcr:name
            String field = stringFactory.create(JcrLexicon.NAME);
            return nameFieldQuery(field, operator, value, caseOperation);
        } else if (left instanceof NodeLocalName) {
            // this only applies to mode:localName
            String field = stringFactory.create(ModeShapeLexicon.LOCALNAME);
            return stringFieldQuery(field, operator, value, caseOperation);
        } else if (left instanceof Cast) {
            Cast cast = (Cast) left;
            return createQuery(cast.getOperand(), operator, right, caseOperation);
        }
        throw new LuceneIndexException("Unexpected DynamicOperand instance: class=" + (left != null ? left.getClass() : "null")
                                       + " and instance=" + left);
    }

    private String depthField() {
        return stringFactory.create(ModeShapeLexicon.DEPTH);
    }

    protected Object getSingleValueFromStaticOperand( StaticOperand operand ) {
        Object value = null;
        if (operand instanceof Literal) {
            Literal literal = (Literal) operand;
            value = literal.value();
        } else if (operand instanceof BindVariableName) {
            BindVariableName variable = (BindVariableName) operand;
            String variableName = variable.getBindVariableName();
            value = variables.get(variableName);
            if (value instanceof Iterable<?>) {
                // We can only return one value ...
                Iterator<?> iter = ((Iterable<?>) value).iterator();
                if (iter.hasNext()) {
                    return iter.next();
                }
                value = null;
            }
            if (value == null) {
                throw new LuceneIndexException(JcrI18n.missingVariableValue.text(variableName));
            }
        } else {
            throw new IllegalArgumentException("Unknown operand type:" + operand);
        }
        return value;
    }

    protected BooleanQuery not( Query notted ) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.setDisableCoord(true);
        // We need at least some positive match, so get all docs ...
        builder.add(new MatchAllDocsQuery(), Occur.SHOULD);
        // Now apply the original query being 'NOT-ed' as a MUST_NOT occurrence ...
        builder.add(notted, Occur.MUST_NOT);
        return builder.build();
    }

    protected Query createPropertyValueQuery( PropertyValue propertyValue,
                                              Operator operator,
                                              Object value,
                                              Function<String, String> caseOperation ) {
        if (operator == Operator.LIKE) {
            String stringValue = stringFactory.create(value);
            if (!stringValue.contains("%") && !stringValue.contains("_") && !stringValue.contains("\\")) {
                // The value is not a LIKE literal, so we can treat it as an '=' operator ...
                operator = Operator.EQUAL_TO;
            }
        }

        String propertyName = propertyValue.getPropertyName();
        PropertyType valueType = propertyTypesByName.get(propertyName);

        switch (valueType) {
            case REFERENCE:
            case WEAKREFERENCE:
            case SIMPLEREFERENCE:
                return stringFieldQuery(propertyName, operator, value, null);
            case URI:
            case STRING:
                return stringFieldQuery(propertyName, operator, value, caseOperation);
            case PATH:
                return pathFieldQuery(propertyName, operator, value, caseOperation);
            case NAME:
                return nameFieldQuery(propertyName, operator, value, caseOperation);
            case DECIMAL:
                return decimalFieldQuery(propertyName, operator, value, caseOperation);
            case DATE:
                return dateFieldQuery(propertyName, operator, value);
            case LONG:
                return longFieldQuery(propertyName, operator, value);
            case BOOLEAN:
                return booleanFieldQuery(propertyName, operator, value);
            case DOUBLE:
                return doubleFieldQuery(propertyName, operator, value);
            case BINARY:
            case OBJECT:
            default:
                throw new IllegalArgumentException("Unsupported value type:" + valueType);
        }
    }

    protected Query stringFieldQuery( String field, Operator operator, Object value, Function<String, String> caseOperation ) {
        String stringValue = stringFactory.create(value);
        switch (operator) {
            case EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field, caseOperation);
            case NOT_EQUAL_TO:
                Query query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field,
                                                                                     caseOperation);
                return not(query);
            case GREATER_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThan(stringValue, field, caseOperation);
            case GREATER_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(stringValue, field,
                                                                                           caseOperation);
            case LESS_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldLessThan(stringValue, field, caseOperation);
            case LESS_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(stringValue, field,
                                                                                        caseOperation);
            case LIKE:
                return CompareStringQuery.createQueryForNodesWithFieldLike(stringValue, field, caseOperation);
            default:
                throw new IllegalArgumentException("Unknown operator:" + operator);
        }
    }

    protected Query decimalFieldQuery( String field, Operator operator, Object value, Function<String, String> caseOperation ) {
        String decimalString = null;
        if (operator != Operator.LIKE) {
            // Decimal values are stored in a special lexicographically sortable form, so we have to
            // convert the value to this ...
            BigDecimal decimalValue = factories.getDecimalFactory().create(value);
            decimalString = FieldUtil.decimalToString(decimalValue);
        } else {
            // search for LIKE as a regular string expression
            decimalString = stringFactory.create(value);
        }
        return stringFieldQuery(field, operator, decimalString, caseOperation);
    }

    protected Query booleanFieldQuery( String field, Operator operator, Object value ) {
        Boolean booleanValue = factories.getBooleanFactory().create(value);

        if (booleanValue) {
            switch (operator) {
                case EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, false, true);
                case NOT_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, true, false);
                case GREATER_THAN_OR_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 1, 1, true, true);
                case LESS_THAN_OR_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, true, true);
                case GREATER_THAN:
                    // Can't be greater than 'true', per JCR spec
                    return new MatchNoDocsQuery();
                case LESS_THAN:
                    // 'false' is less than 'true' ...
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 0, true, true);
                case LIKE:
                    // This is not supported
                    throw new LuceneIndexException(LuceneIndexProviderI18n.invalidOperatorForPropertyType.text(Operator.LIKE,
                                                                                                               PropertyType.BOOLEAN));
                default:
                    throw new IllegalArgumentException("Unknown operator:" + operator);

            }
        } else {
            switch (operator) {
                case EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, true, false);
                case NOT_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, false, true);
                case GREATER_THAN_OR_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 1, true, true);
                case LESS_THAN_OR_EQUAL_TO:
                    return LegacyNumericRangeQuery.newIntRange(field, 0, 0, true, true);
                case GREATER_THAN:
                    // 'true' is greater than 'false' ...
                    return LegacyNumericRangeQuery.newIntRange(field, 1, 1, true, true);
                case LESS_THAN:
                    // Can't be less than 'false', per JCR spec
                    return new MatchNoDocsQuery();
                case LIKE:
                    // This is not supported
                    throw new LuceneIndexException(LuceneIndexProviderI18n.invalidOperatorForPropertyType.text(Operator.LIKE,
                                                                                                               PropertyType.BOOLEAN));
                default:
                    throw new IllegalArgumentException("Unknown operator:" + operator);
            }
        }
    }

    protected Query longFieldQuery( String field, Operator operator, Object value ) {
        Long longMinimum = Long.MIN_VALUE;
        Long longMaximum = Long.MAX_VALUE;
        long longValue = factories.getLongFactory().create(value);
        switch (operator) {
            case EQUAL_TO:
                if (longValue < longMinimum || longValue > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
            case NOT_EQUAL_TO:
                if (longValue < longMinimum || longValue > longMaximum) {
                    return new MatchAllDocsQuery();
                }
                Query lowerRange = LegacyNumericRangeQuery.newLongRange(field, longMinimum, longValue, true, false);
                Query upperRange = LegacyNumericRangeQuery.newLongRange(field, longValue, longMaximum, false, true);
                return booleanQuery(lowerRange, Occur.SHOULD, upperRange, Occur.SHOULD);
            case GREATER_THAN:
                if (longValue > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longValue, longMaximum, false, true);
            case GREATER_THAN_OR_EQUAL_TO:
                if (longValue > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longValue, longMaximum, true, true);
            case LESS_THAN:
                if (longValue < longMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longMinimum, longValue, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                if (longValue < longMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longMinimum, longValue, true, true);
            case LIKE:
                throw new LuceneIndexException(LuceneIndexProviderI18n.invalidOperatorForPropertyType.text(operator,
                                                                                                           PropertyType.LONG));
            default:
                throw new IllegalArgumentException("Unknown operator:" + operator);
        }
    }

    protected Query doubleFieldQuery( String field, Operator operator, Object value ) {
        double doubleValue = factories.getDoubleFactory().create(value);
        Double doubleMinimum = Double.MIN_VALUE;
        Double doubleMaximum = Double.MAX_VALUE;
        switch (operator) {
            case EQUAL_TO:
                if (doubleValue < doubleMinimum || doubleValue > doubleMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
            case NOT_EQUAL_TO:
                if (doubleValue < doubleMinimum || doubleValue > doubleMaximum) {
                    return new MatchAllDocsQuery();
                }
                Query lowerRange = LegacyNumericRangeQuery.newDoubleRange(field, doubleMinimum, doubleValue, true,
                                                                    false);
                Query upperRange = LegacyNumericRangeQuery.newDoubleRange(field, doubleValue, doubleMaximum, false,
                                                                    true);
                return booleanQuery(lowerRange, Occur.SHOULD, upperRange, Occur.SHOULD);
            case GREATER_THAN:
                if (doubleValue > doubleMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newDoubleRange(field, doubleValue, doubleMaximum, false, true);
            case GREATER_THAN_OR_EQUAL_TO:
                if (doubleValue > doubleMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newDoubleRange(field, doubleValue, doubleMaximum, true, true);
            case LESS_THAN:
                if (doubleValue < doubleMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newDoubleRange(field, doubleMinimum, doubleValue, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                if (doubleValue < doubleMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newDoubleRange(field, doubleMinimum, doubleValue, true, true);
            case LIKE:
                // should never happen (the double conversion should've failed)
                assert false;
            default:
                throw new IllegalArgumentException("Unknown operator:" + operator);
        }
    }

    protected Query dateFieldQuery( String field, Operator operator, Object value ) {
        Long longMinimum = Long.MIN_VALUE;
        Long longMaximum = Long.MAX_VALUE;
        long millis = factories.getDateFactory().create(value).getMilliseconds();
        switch (operator) {
            case EQUAL_TO:
                if (millis < longMinimum || millis > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, millis, millis, true, true);
            case NOT_EQUAL_TO:
                if (millis < longMinimum || millis > longMaximum) {
                    return new MatchAllDocsQuery();
                }
                Query lowerRange = LegacyNumericRangeQuery.newLongRange(field, longMinimum, millis, true, false);
                Query upperRange = LegacyNumericRangeQuery.newLongRange(field, millis, longMaximum, false, true);
                return booleanQuery(lowerRange, Occur.SHOULD, upperRange, Occur.SHOULD);
            case GREATER_THAN:
                if (millis > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, millis, longMaximum, false, true);
            case GREATER_THAN_OR_EQUAL_TO:
                if (millis > longMaximum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, millis, longMaximum, true, true);
            case LESS_THAN:
                if (millis < longMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longMinimum, millis, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                if (millis < longMinimum) {
                    return new MatchNoDocsQuery();
                }
                return LegacyNumericRangeQuery.newLongRange(field, longMinimum, millis, true, true);
            case LIKE:
                // should never happen (the millis conversion should've failed)
                assert false;
            default:
                throw new IllegalArgumentException("Unknown operator:" + operator);
        }
    }

    protected Query createReferenceValueQuery( ReferenceValue referenceValue, Operator operator, Object value ) {
        String field = referenceValue.getPropertyName();
        if (field != null) {
            return stringFieldQuery(field, operator, value, null);
        }

        // we are being asked to query for all the references fields that apply to this index, so we need to collect them first
        List<String> referenceFields = collectReferenceFieldNames(referenceValue);
        assert !referenceFields.isEmpty(); // this can't be empty because this index was called in the first place....
        if (referenceFields.size() == 1) {
            return stringFieldQuery(referenceFields.get(0), operator, value, null);
        } else {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setDisableCoord(true);
            for (String fieldName : referenceFields) {
                Query fieldQuery = stringFieldQuery(fieldName, operator, value, null);
                builder.add(fieldQuery, Occur.SHOULD);
            }
            return builder.build();
        }
    }

    protected List<String> collectReferenceFieldNames( ReferenceValue referenceValue ) {
        List<String> result = new ArrayList<>();
        boolean includeWeakReferences = referenceValue.includesWeakReferences();
        boolean includeSimpleReferences = referenceValue.includeSimpleReferences();
        for (Map.Entry<String, PropertyType> propertyEntry : propertyTypesByName.entrySet()) {
            PropertyType propertyType = propertyEntry.getValue();
            switch (propertyType) {
                case WEAKREFERENCE: {
                    if (includeWeakReferences) {
                        result.add(propertyEntry.getKey());
                    }
                    break;
                }
                case SIMPLEREFERENCE: {
                    if (includeSimpleReferences) {
                        result.add(propertyEntry.getKey());
                    }
                    break;
                }
                case REFERENCE: {
                    result.add(propertyEntry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    protected Query createLengthQuery( Length propertyLength, Operator operator, Object value ) {
        assert propertyLength != null;
        assert value != null;
        long length = factories.getLongFactory().create(value);
        if (length <= 0L) {
            return new MatchNoDocsQuery();
        }
        String field = FieldUtil.lengthField(propertyLength.getPropertyValue().getPropertyName());
        switch (operator) {
            case EQUAL_TO:
                return LegacyNumericRangeQuery.newLongRange(field, length, length, true, true);
            case NOT_EQUAL_TO:
                Query upper = LegacyNumericRangeQuery.newLongRange(field, length, Long.MAX_VALUE, false, false);
                Query lower = LegacyNumericRangeQuery.newLongRange(field, 0L, length, true, false);
                return booleanQuery(upper, Occur.SHOULD, lower, Occur.SHOULD);
            case GREATER_THAN:
                return LegacyNumericRangeQuery.newLongRange(field, length, Long.MAX_VALUE, false, false);
            case GREATER_THAN_OR_EQUAL_TO:
                return LegacyNumericRangeQuery.newLongRange(field, length, Long.MAX_VALUE, true, false);
            case LESS_THAN:
                return LegacyNumericRangeQuery.newLongRange(field, 0L, length, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                return LegacyNumericRangeQuery.newLongRange(field, 0L, length, true, true);
            case LIKE:
                // This is not allowed ...
                throw new LuceneIndexException(LuceneIndexProviderI18n.invalidOperatorForOperand.text(operator,
                                                                                                      propertyLength));
            default: {
                throw new IllegalArgumentException("Unknown operator:" + operator);
            }
        }
    }

    protected Query pathFieldQuery( String field, Operator operator, Object value, Function<String, String> caseOperation ) {
        Path path = null;
        if (operator != Operator.LIKE) {
            path = !(value instanceof Path) ? pathFactory.create(value) : (Path) value;
        }
        switch (operator) {
            case EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringFactory.create(path), field,
                                                                              caseOperation);
            case NOT_EQUAL_TO:
                return not(CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringFactory.create(path), field,
                                                                                  caseOperation));
            case LIKE:
                String likeExpression = stringFactory.create(value);
                // the paths are stored in the index via stringFactory.create, which doesn't have the "1" index for SNS...
                likeExpression = likeExpression.replaceAll("\\[1\\]", "");
                if (likeExpression.contains("[%]")) {
                    // We can't use '[%]' because we only want to match digits,
                    // so handle this using a regex ...
                    // !!! LUCENE Regexp is not the same as Java's. See the javadoc RegExp
                    String regex = likeExpression;
                    regex = regex.replace("[%]", "(\\[[0-9]+\\])?");
                    regex = regex.replaceAll("\\[\\d+\\]", "\\[[0-9]+\\]");
                    //regex = regex.replace("]", "\\]");
                    regex = regex.replace("*", ".*");
                    regex = regex.replace("%", ".*").replace("_", ".");
                    // Now create a regex query ...
                    int flags = caseOperation == null ? 0 : Pattern.CASE_INSENSITIVE;
                    return new RegexpQuery(new Term(field, regex), flags);
                } else {
                    return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, field, caseOperation);
                }
            case GREATER_THAN:
                return ComparePathQuery.createQueryForNodesWithPathGreaterThan(path, field, factories, caseOperation);
            case GREATER_THAN_OR_EQUAL_TO:
                return ComparePathQuery.createQueryForNodesWithPathGreaterThanOrEqualTo(path, field, factories, caseOperation);
            case LESS_THAN:
                return ComparePathQuery.createQueryForNodesWithPathLessThan(path, field, factories, caseOperation);
            case LESS_THAN_OR_EQUAL_TO:
                return ComparePathQuery.createQueryForNodesWithPathLessThanOrEqualTo(path, field, factories, caseOperation);
            default: {
                throw new IllegalArgumentException("Unknown operator:" + operator);
            }
        }
    }

    protected Query nameFieldQuery( String field, Operator operator, Object value, Function<String, String> caseOperation ) {
        Name name = null;
        if (operator != Operator.LIKE) {
            name = !(value instanceof Name) ? factories.getNameFactory().create(value) : (Name) value;
        }
        switch (operator) {
            case EQUAL_TO:
                return CompareNameQuery.createQueryForNodesWithNameEqualTo(name, field, factories, caseOperation);
            case NOT_EQUAL_TO:
                Query equalToQuery = CompareNameQuery.createQueryForNodesWithNameEqualTo(name, field, factories, caseOperation);
                return not(equalToQuery);
            case GREATER_THAN:
                return CompareNameQuery.createQueryForNodesWithNameGreaterThan(name, field, factories, caseOperation);
            case GREATER_THAN_OR_EQUAL_TO:
                return CompareNameQuery.createQueryForNodesWithNameGreaterThanOrEqualTo(name, field, factories, caseOperation);
            case LESS_THAN:
                return CompareNameQuery.createQueryForNodesWithNameLessThan(name, field, factories, caseOperation);
            case LESS_THAN_OR_EQUAL_TO:
                return CompareNameQuery.createQueryForNodesWithNameLessThanOrEqualTo(name, field, factories, caseOperation);
            case LIKE:
                // we can only process the value as a string...
                String likeExpression = stringFactory.create(value);
                return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, field, caseOperation);
            default:
                throw new IllegalArgumentException("Unknown operator:" + operator);
        }
    }

    protected BooleanQuery booleanQuery( Query leftQuery, Occur leftOccur, Query rightQuery, Occur rightOccur ) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.setDisableCoord(true);
        builder.add(leftQuery, leftOccur);
        builder.add(rightQuery, rightOccur);
        return builder.build();
    }

    protected static class SingleColumnQueryFactory extends LuceneQueryFactory {
        private SingleColumnQueryFactory( ValueFactories factories, Map<String, Object> variables,
                                          Map<String, PropertyType> propertyTypesByName ) {
            super(factories, variables, propertyTypesByName);
        }

        @Override
        protected Query createQuery( PropertyExistence propertyExistence ) {
            // since there can be only one property per indexed document, the fact that this was called means it applies to all the
            // documents of the stored index
            return new MatchAllDocsQuery();
        }

        @Override
        protected List<String> collectReferenceFieldNames( ReferenceValue referenceValue ) {
            // there should be just one column for these types of indexes and that column should already have the reference value
            assert propertyTypesByName.size() == 1;
            return Collections.singletonList(propertyName());
        }

        protected String propertyName() {
            // these indexes can only apply to 1 single property
            return propertyTypesByName.keySet().iterator().next();
        }
    }

    protected static class TextQueryFactory extends SingleColumnQueryFactory {
        private static final PhraseQuery EMPTY_PHRASE_QUERY = new PhraseQuery.Builder().build();

        private final Analyzer analyzer;

        private TextQueryFactory( ValueFactories factories,
                                  Map<String, Object> variables,
                                  Map<String, PropertyType> propertyTypesByName,
                                  LuceneConfig config ) {
            super(factories, variables, propertyTypesByName);
            this.analyzer = config.getAnalyzer();
        }

        @Override
        public boolean scoreDocuments() {
            return true;
        }

        @Override
        protected Query createQuery( FullTextSearch search ) {
            String propertyName = search.getPropertyName();
            if (propertyName == null) {
                // the search if for * (all properties) so this query should be done for the current index's property
                propertyName = propertyName();
            }
            StaticOperand expression = search.getFullTextSearchExpression();
            Object value = getSingleValueFromStaticOperand(expression);
            try {
                String valueString = value instanceof Value ? ((Value) value).getString() : stringFactory.create(value);
                search = search.withFullTextExpression(valueString);
                return createQuery(propertyName, search.getTerm());
            } catch (RepositoryException e) {
                throw new LuceneIndexException(e);
            }
        }

        protected Query createQuery( String fieldName, FullTextSearch.Term term ) {
            assert fieldName != null;
            if (term instanceof FullTextSearch.Conjunction) {
                return createConjunctionQuery(fieldName, (FullTextSearch.Conjunction) term);
            }
            if (term instanceof FullTextSearch.Disjunction) {
                return createDisjunctionQuery(fieldName, (FullTextSearch.Disjunction) term);
            }
            if (term instanceof FullTextSearch.SimpleTerm) {
                return createSimpleTermQuery(fieldName, (FullTextSearch.SimpleTerm) term);
            }
            if (term instanceof FullTextSearch.NegationTerm) {
                return createNegationTermQuery(fieldName, (FullTextSearch.NegationTerm) term);
            }
            throw new IllegalArgumentException("Unknown term instance:" + term);
        }

        private Query createNegationTermQuery( String fieldName, FullTextSearch.NegationTerm negation ) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setDisableCoord(true);
            Query subQuery = createQuery(fieldName, negation.getNegatedTerm());
            if (!EMPTY_PHRASE_QUERY.equals(subQuery)) {
                builder.add(subQuery, Occur.MUST_NOT);
                // need to add at least a positive match
                builder.add(new MatchAllDocsQuery(), Occur.FILTER);
                return builder.build();
            } else {
                return new MatchAllDocsQuery();
            }
        }

        private Query createSimpleTermQuery( String fieldName, FullTextSearch.SimpleTerm simple ) {
            try {
                if (QueryUtil.hasWildcardCharacters(simple.getValue())) {
                    return createWildcardQuery(fieldName, simple);
                }
                PhraseQuery.Builder builder = new PhraseQuery.Builder();
                builder.setSlop(0); // terms must be adjacent
                String expression = simple.getValue();
                // Run the expression through the Lucene analyzer to extract the terms ...
                try (TokenStream stream = analyzer.tokenStream(fieldName, expression)) {
                    stream.reset();
                    CharTermAttribute termAttribute = stream.addAttribute(CharTermAttribute.class);
                    while (stream.incrementToken()) {
                        // The term attribute object has been modified to contain the next term ...
                        String analyzedTerm = termAttribute.toString();
                        builder.add(new Term(fieldName, analyzedTerm));
                    }
                    stream.end();
                }
                return builder.build();
            } catch (Exception e) {
                throw new LuceneIndexException(e);
            }
        }

        private Query createDisjunctionQuery( String fieldName, FullTextSearch.Disjunction disjunction ) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setDisableCoord(true);
            boolean atLeastOnePositiveClause = false;
            for (FullTextSearch.Term nested : disjunction) {
                if (nested instanceof FullTextSearch.NegationTerm) {
                    //Lucene does not have a SHOULD_NOT and MUST_NOT is too strong for disjunctions...
                } else {
                    Query subQuery = createQuery(fieldName, nested);
                    if (!EMPTY_PHRASE_QUERY.equals(subQuery)) {
                        atLeastOnePositiveClause = true;
                        builder.add(subQuery, Occur.SHOULD);
                    }
                }
            }
            if (!atLeastOnePositiveClause) {
                // there are only MUST_NOT terms so we should add one positive for this to work
                builder.add(new MatchAllDocsQuery(), Occur.FILTER);
            }
            return builder.build();
        }

        private Query createConjunctionQuery( String fieldName, FullTextSearch.Conjunction conjunction ) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.setDisableCoord(true);
            boolean atLeastOnePositiveClause = false;
            for (FullTextSearch.Term nested : conjunction) {
                if (nested instanceof FullTextSearch.NegationTerm) {
                    Query subQuery = createQuery(fieldName, ((FullTextSearch.NegationTerm) nested).getNegatedTerm());
                    if (!EMPTY_PHRASE_QUERY.equals(subQuery)) {
                        builder.add(subQuery, Occur.MUST_NOT);
                    }
                } else {
                    Query subQuery = createQuery(fieldName, nested);
                    if (!EMPTY_PHRASE_QUERY.equals(subQuery)) {
                        atLeastOnePositiveClause = true;
                        builder.add(subQuery, Occur.MUST);
                    }
                }
            }
            if (!atLeastOnePositiveClause) {
                // there are only MUST_NOT terms so we should add one positive for this to work
                builder.add(new MatchAllDocsQuery(), Occur.FILTER);
            }
            return builder.build();
        }

        private Query createWildcardQuery( final String fieldName, FullTextSearch.SimpleTerm simple ) throws ParseException {
            // Use the standard parser, but instead of wildcard queries (which don't work with leading
            // wildcards) we should use our like queries (which often use RegexQuery where applicable) ...

            //as an alternative, for leading wildcards one could call parser.setAllowLeadingWildcard(true);
            //and use the default Lucene query parser
            QueryParser parser = new QueryParser(fieldName, analyzer) {
                @Override
                protected Query getWildcardQuery( String field, String termStr ) {
                    return CompareStringQuery.createQueryForNodesWithFieldLike(termStr.toLowerCase(), fieldName,
                                                                               null);
                }
            };

            String expression = simple.getValue();
            // The ComplexPhraseQueryParser only understands the '?' and '*' as being wildcards ...
            expression = expression.replaceAll("(?<![\\\\])_", "?");
            expression = expression.replaceAll("(?<![\\\\])%", "*");
            // // Replace any '-' between tokens, except when preceded or followed by a digit, '*', or '?' ...
            expression = expression.replaceAll("((?<![\\d*?]))[-]((?![\\d*?]))", "$1 $2");
            // Then use the parser ...
            return parser.parse(expression);
        }
    }
}
