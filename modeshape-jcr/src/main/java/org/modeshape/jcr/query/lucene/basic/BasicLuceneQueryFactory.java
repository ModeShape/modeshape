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
package org.modeshape.jcr.query.lucene.basic;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.PropertyValue;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.regex.JavaUtilRegexCapabilities;
import org.apache.lucene.search.regex.RegexQuery;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.query.qom.NodeDepth;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.lucene.CompareLengthQuery;
import org.modeshape.jcr.query.lucene.CompareNameQuery;
import org.modeshape.jcr.query.lucene.ComparePathQuery;
import org.modeshape.jcr.query.lucene.CompareStringQuery;
import org.modeshape.jcr.query.lucene.FieldUtil;
import org.modeshape.jcr.query.lucene.LuceneQueryFactory;
import org.modeshape.jcr.query.lucene.MatchNoneQuery;
import org.modeshape.jcr.query.lucene.NotQuery;
import org.modeshape.jcr.query.lucene.basic.NodeInfoIndex.FieldName;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * The {@link LuceneQueryFactory} customization that produces {@link Query} objects based upon the {@link BasicLuceneSchema}.
 */
public class BasicLuceneQueryFactory extends LuceneQueryFactory {

    protected static final int MIN_DEPTH = 0;
    protected static final int MAX_DEPTH = 1000;
    protected static final int MIN_SNS_INDEX = 1;
    protected static final int MAX_SNS_INDEX = 10000000; // assume there won't be more than 10M same-name-siblings

    /**
     * @param context
     * @param searchFactory
     * @param version the Lucene version
     */
    public BasicLuceneQueryFactory( QueryContext context,
                                    SearchFactory searchFactory,
                                    Version version ) {
        super(context, searchFactory, version);
    }

    protected final String pathAsString( Path path ) {
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

    @Override
    protected Analyzer getFullTextSearchAnalyzer() {
        return searchFactory.getAnalyzer(NodeInfo.class);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If a property name is provided, then the resulting field name is generated from the
     * {@link NodeInfoIndex.FieldName#FULL_TEXT_PREFIX full-text prefix} and the property name. Otherwise, the result is the field
     * name where the full-text terms for the entire node are indexed.
     * </p>
     */
    @Override
    protected String fullTextFieldName( String propertyName ) {
        return propertyName == null ? FieldName.FULL_TEXT : FieldName.FULL_TEXT_PREFIX + propertyName;
    }

    @Override
    protected Query findAllNodesBelow( Path ancestorPath ) {
        // Find the path of the parent ...
        String stringifiedPath = pathAsString(ancestorPath);
        // Append a '/' to the parent path, and we'll only get decendants ...
        stringifiedPath = stringifiedPath + '/';

        // Create a prefix query ...
        return new PrefixQuery(new Term(FieldName.PATH, stringifiedPath));
    }

    @Override
    protected Query findAllNodesAtOrBelow( Path ancestorPath ) {
        if (ancestorPath.isRoot()) {
            return new MatchAllDocsQuery();
        }
        // Find the path of the parent ...
        String stringifiedPath = pathAsString(ancestorPath);
        // Do not append a '/' to the parent path ... otherwise we won't get the parent node

        // Create a prefix query ...
        return new PrefixQuery(new Term(FieldName.PATH, stringifiedPath));
    }

    @Override
    protected Query findChildNodes( Path parentPath ) {
        // Create a query to find all descendants ...
        Query descendants = findAllNodesBelow(parentPath);
        // And another to find all nodes at the depth of the children ...
        int childrenDepth = parentPath.size() + 1;
        Query depthQuery = NumericRangeQuery.newIntRange(FieldName.DEPTH, childrenDepth, childrenDepth, true, true);
        // Now combine ...
        BooleanQuery combinedQuery = new BooleanQuery();
        combinedQuery.add(descendants, Occur.MUST);
        combinedQuery.add(depthQuery, Occur.MUST);
        return combinedQuery;
    }

    @Override
    protected Query findNodeAt( Path path ) {
        if (path.isRoot()) {
            // Look for the root node using the depth (which is hopefully the fastest) ...
            return NumericRangeQuery.newIntRange(FieldName.DEPTH, 0, 0, true, true);
        }
        String stringifiedPath = pathAsString(path);
        return new TermQuery(new Term(FieldName.PATH, stringifiedPath));
    }

    @Override
    protected Query findNodesLike( SelectorName selectorName,
                                   String fieldName,
                                   String likeExpression,
                                   boolean caseSensitive ) {
        return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, fieldName, factories, caseSensitive);
    }

    protected Query findNodesLike( String fieldName,
                                   String likeExpression,
                                   boolean caseSensitive ) {
        return CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression, fieldName, factories, caseSensitive);
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   Length propertyLength,
                                   Operator operator,
                                   Object value ) {
        assert propertyLength != null;
        assert value != null;
        PropertyValue propertyValue = propertyLength.getPropertyValue();
        String field = stringFactory.create(propertyValue.getPropertyName());
        int length = factories.getLongFactory().create(value).intValue();
        if (JcrConstants.JCR_NAME.equals(field) || JcrConstants.JCR_PATH.equals(field)
            || JcrConstants.MODE_LOCAL_NAME.equals(field)) {
            // We can actually use the stored field ...
            switch (operator) {
                case EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldEqualTo(length, field, factories);
                case NOT_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldNotEqualTo(length, field, factories);
                case GREATER_THAN:
                    return CompareLengthQuery.createQueryForNodesWithFieldGreaterThan(length, field, factories);
                case GREATER_THAN_OR_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(length, field, factories);
                case LESS_THAN:
                    return CompareLengthQuery.createQueryForNodesWithFieldLessThan(length, field, factories);
                case LESS_THAN_OR_EQUAL_TO:
                    return CompareLengthQuery.createQueryForNodesWithFieldLessThanOrEqualTo(length, field, factories);
                case LIKE:
                    // This is not allowed ...
                    assert false;
                    break;
            }
        } else {
            // We should use the field that begins with ':len:' ...
            field = FieldName.LENGTH_PREFIX + field;
            switch (operator) {
                case EQUAL_TO:
                    return NumericRangeQuery.newIntRange(field, length, length, true, true);
                case NOT_EQUAL_TO:
                    return not(NumericRangeQuery.newIntRange(field, length, length, true, true));
                case GREATER_THAN:
                    return NumericRangeQuery.newIntRange(field, length, Integer.MAX_VALUE, false, false);
                case GREATER_THAN_OR_EQUAL_TO:
                    return NumericRangeQuery.newIntRange(field, length, Integer.MAX_VALUE, true, false);
                case LESS_THAN:
                    return NumericRangeQuery.newIntRange(field, 0, length, true, false);
                case LESS_THAN_OR_EQUAL_TO:
                    return NumericRangeQuery.newIntRange(field, 0, length, true, true);
                case LIKE:
                    // This is not allowed ...
                    assert false;
                    break;
            }
        }
        return null;
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   PropertyValue propertyValue,
                                   Operator operator,
                                   Object value,
                                   boolean caseSensitive ) {
        String field = propertyValue.getPropertyName();
        Schemata.Column metadata = getMetadataFor(selectorName, field);
        if (metadata != null) {
            PropertyType type = metadata.getRequiredType();
            return findNodesWith(selectorName, propertyValue, operator, value, caseSensitive, type, metadata);
        }
        assert metadata == null;
        if (!(value instanceof String)) {
            // This is due to an explicit cast, so treat it as the actual value ...
            PropertyType type = PropertyType.discoverType(value);
            return findNodesWith(selectorName, propertyValue, operator, value, caseSensitive, type, metadata);
        }
        // Otherwise, the metadata is null and the value is a string. We can't find metadata if the property is residual,
        // and since the value is a string, we may be able to represent the value using different types. So rather than
        // determining the type from the string value, we can try converting the value to the different types and see
        // which ones work. If there are multiple conversions (including string), then we can OR them together.
        BooleanQuery orOfValues = new BooleanQuery();
        boolean checkBoolean = false;
        boolean checkDate = true;
        try {
            Long lValue = factories.getLongFactory().create(value);
            Query query = findNodesWith(selectorName, propertyValue, operator, lValue, caseSensitive, PropertyType.LONG, null);
            if (query != null) {
                orOfValues.add(query, Occur.SHOULD);
            }
            checkBoolean = lValue.longValue() == 1L || lValue.longValue() == 0L;
            checkDate = false; // no need to check the date, as we'd just convert it to a long and we've already added that
        } catch (ValueFormatException e) {
            // Not a long value ...
        }

        try {
            Double dValue = factories.getDoubleFactory().create(value);
            Query query = findNodesWith(selectorName, propertyValue, operator, dValue, caseSensitive, PropertyType.DOUBLE, null);
            if (query != null) {
                orOfValues.add(query, Occur.SHOULD);
            }
        } catch (ValueFormatException e) {
            // Not a long value ...
        }

        if (checkBoolean) {
            try {
                Boolean b = factories.getBooleanFactory().create(value);
                Query query = findNodesWith(selectorName, propertyValue, operator, b, caseSensitive, PropertyType.BOOLEAN, null);
                if (query != null) {
                    orOfValues.add(query, Occur.SHOULD);
                }
            } catch (ValueFormatException e) {
                // Not a long value ...
            }
        }

        if (checkDate) {
            try {
                DateTime date = factories.getDateFactory().create(value);
                Query query = findNodesWith(selectorName, propertyValue, operator, date, caseSensitive, PropertyType.DATE, null);
                if (query != null) {
                    orOfValues.add(query, Occur.SHOULD);
                }
            } catch (ValueFormatException e) {
                // Not a long value ...
            }
        }

        // Finally treat it as a string ...
        String strValue = stringFactory.create(value);
        Query strQuery = findNodesWith(selectorName, propertyValue, operator, strValue, caseSensitive, PropertyType.STRING, null);

        if (orOfValues.clauses().isEmpty()) {
            return strQuery;
        }
        orOfValues.add(strQuery, Occur.SHOULD);
        return orOfValues;
    }

    protected Query findNodesWith( SelectorName selectorName,
                                   PropertyValue propertyValue,
                                   Operator operator,
                                   Object value,
                                   boolean caseSensitive,
                                   PropertyType valueType,
                                   Schemata.Column metadata ) {

        String field = propertyValue.getPropertyName();
        if (valueType == PropertyType.OBJECT) {
            // There is no known/prescribed property type, so match our criteria based upon the type of value we have ...
            valueType = PropertyType.discoverType(value);
        }
        if (operator == Operator.LIKE) {
            String stringValue = stringFactory.create(value);
            if (stringValue.indexOf('%') != -1 || stringValue.indexOf('_') != -1 || stringValue.indexOf('\\') != -1) {
                // This value is not a literal value ...
                valueType = PropertyType.STRING;
            } else {
                // The value is not a LIKE literal, so we can treat it as an '=' operator ...
                operator = Operator.EQUAL_TO;
            }
        }
        switch (valueType) {
            case REFERENCE:
            case WEAKREFERENCE:
            case UUID:
            case PATH:
            case NAME:
            case URI:
            case DECIMAL: // stored in lexicographically-ordered form
            case STRING:
                String stringValue = stringFactory.create(value);
                if (value instanceof Path) {
                    stringValue = pathAsString((Path)value);
                }
                if (!caseSensitive) stringValue = stringValue.toLowerCase();
                switch (operator) {
                    case EQUAL_TO:
                        return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue,
                                                                                      field,
                                                                                      factories,
                                                                                      caseSensitive);
                    case NOT_EQUAL_TO:
                        Query query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue,
                                                                                             field,
                                                                                             factories,
                                                                                             caseSensitive);
                        return not(query);
                    case GREATER_THAN:
                        return CompareStringQuery.createQueryForNodesWithFieldGreaterThan(stringValue,
                                                                                          field,
                                                                                          factories,
                                                                                          caseSensitive);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(stringValue,
                                                                                                   field,
                                                                                                   factories,
                                                                                                   caseSensitive);
                    case LESS_THAN:
                        return CompareStringQuery.createQueryForNodesWithFieldLessThan(stringValue,
                                                                                       field,
                                                                                       factories,
                                                                                       caseSensitive);
                    case LESS_THAN_OR_EQUAL_TO:
                        return CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(stringValue,
                                                                                                field,
                                                                                                factories,
                                                                                                caseSensitive);
                    case LIKE:
                        return findNodesLike(selectorName, field, stringValue, caseSensitive);
                }
                break;
            case DATE:
                Long longMinimum = Long.MIN_VALUE;
                Long longMaximum = Long.MAX_VALUE;
                if (metadata != null) {
                    longMinimum = (Long)metadata.getMinimum();
                    longMaximum = (Long)metadata.getMaximum();
                }
                long date = factories.getLongFactory().create(value);
                switch (operator) {
                    case EQUAL_TO:
                        if (date < longMinimum || date > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, date, true, true);
                    case NOT_EQUAL_TO:
                        if (date < longMinimum || date > longMaximum) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newLongRange(field, date, date, true, true);
                        return not(query);
                    case GREATER_THAN:
                        if (date > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, longMaximum, false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (date > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, date, longMaximum, true, true);
                    case LESS_THAN:
                        if (date < longMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longMinimum, date, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (date < longMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longMinimum, date, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case LONG:
                if (metadata != null) {
                    longMinimum = (Long)metadata.getMinimum();
                    longMaximum = (Long)metadata.getMaximum();
                } else {
                    longMinimum = Long.MIN_VALUE;
                    longMaximum = Long.MAX_VALUE;
                }
                long longValue = factories.getLongFactory().create(value);
                switch (operator) {
                    case EQUAL_TO:
                        if (longValue < longMinimum || longValue > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                    case NOT_EQUAL_TO:
                        if (longValue < longMinimum || longValue > longMaximum) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newLongRange(field, longValue, longValue, true, true);
                        return not(query);
                    case GREATER_THAN:
                        if (longValue > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longMaximum, false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (longValue > longMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longValue, longMaximum, true, true);
                    case LESS_THAN:
                        if (longValue < longMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longMinimum, longValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (longValue < longMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newLongRange(field, longMinimum, longValue, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case BOOLEAN:
                boolean booleanValue = factories.getBooleanFactory().create(value);
                int intValue = booleanValue ? 1 : 0;
                switch (operator) {
                    case EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                    case NOT_EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intValue, intValue, true, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, intValue, 1, true, true);
                    case LESS_THAN_OR_EQUAL_TO:
                        return NumericRangeQuery.newIntRange(field, 0, intValue, true, true);
                    case GREATER_THAN:
                        if (!booleanValue) {
                            // 'true' is greater than 'false' ...
                            return NumericRangeQuery.newIntRange(field, 1, 1, true, true);
                        }
                        // Can't be greater than 'true', per JCR spec
                        return new MatchNoneQuery();
                    case LESS_THAN:
                        if (booleanValue) {
                            // 'false' is less than 'true' ...
                            return NumericRangeQuery.newIntRange(field, 0, 0, true, true);
                        }
                        // Can't be less than 'false', per JCR spec
                        return new MatchNoneQuery();
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case DOUBLE:
                double doubleValue = factories.getDoubleFactory().create(value);
                Double doubleMinimum = Double.MIN_VALUE;
                Double doubleMaximum = Double.MAX_VALUE;
                if (metadata != null) {
                    doubleMinimum = (Double)metadata.getMinimum();
                    doubleMaximum = (Double)metadata.getMaximum();
                }
                switch (operator) {
                    case EQUAL_TO:
                        if (doubleValue < doubleMinimum || doubleValue > doubleMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                    case NOT_EQUAL_TO:
                        if (doubleValue < doubleMinimum || doubleValue > doubleMaximum) return new MatchAllDocsQuery();
                        Query query = NumericRangeQuery.newDoubleRange(field, doubleValue, doubleValue, true, true);
                        return new NotQuery(query);
                    case GREATER_THAN:
                        if (doubleValue > doubleMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleMaximum, false, true);
                    case GREATER_THAN_OR_EQUAL_TO:
                        if (doubleValue > doubleMaximum) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleValue, doubleMaximum, true, true);
                    case LESS_THAN:
                        if (doubleValue < doubleMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleMinimum, doubleValue, true, false);
                    case LESS_THAN_OR_EQUAL_TO:
                        if (doubleValue < doubleMinimum) return new MatchNoneQuery();
                        return NumericRangeQuery.newDoubleRange(field, doubleMinimum, doubleValue, true, true);
                    case LIKE:
                        // This is not allowed ...
                        assert false;
                        return null;
                }
                break;
            case BINARY:
            case OBJECT:
                // This is not allowed ...
                assert false;
                return null;
        }
        return null;
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   ReferenceValue referenceValue,
                                   Operator operator,
                                   Object value ) {
        String field = referenceValue.getPropertyName();
        if (field == null) {
            if (referenceValue.includesWeakReferences()) {
                field = FieldName.ALL_REFERENCES;
            } else {
                field = FieldName.STRONG_REFERENCES;
            }
        }
        String stringValue = stringFactory.create(value);
        switch (operator) {
            case EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field, factories, true);
            case NOT_EQUAL_TO:
                return not(CompareStringQuery.createQueryForNodesWithFieldEqualTo(stringValue, field, factories, true));
            case GREATER_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThan(stringValue, field, factories, true);
            case GREATER_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(stringValue, field, factories, true);
            case LESS_THAN:
                return CompareStringQuery.createQueryForNodesWithFieldLessThan(stringValue, field, factories, true);
            case LESS_THAN_OR_EQUAL_TO:
                return CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(stringValue, field, factories, true);
            case LIKE:
                return findNodesLike(selectorName, field, stringValue, false);
        }
        return null;
    }

    @Override
    protected Query findNodesWithNumericRange( SelectorName selectorName,
                                               PropertyValue propertyValue,
                                               Object lowerValue,
                                               Object upperValue,
                                               boolean includesLower,
                                               boolean includesUpper ) {
        String field = stringFactory.create(propertyValue.getPropertyName());
        return findNodesWithNumericRange(selectorName, field, lowerValue, upperValue, includesLower, includesUpper);
    }

    @Override
    protected Query findNodesWithNumericRange( SelectorName selectorName,
                                               NodeDepth depth,
                                               Object lowerValue,
                                               Object upperValue,
                                               boolean includesLower,
                                               boolean includesUpper ) {
        return findNodesWithNumericRange(selectorName, FieldName.DEPTH, lowerValue, upperValue, includesLower, includesUpper);
    }

    protected Query findNodesWithNumericRange( SelectorName selectorName,
                                               String field,
                                               Object lowerValue,
                                               Object upperValue,
                                               boolean includesLower,
                                               boolean includesUpper ) {
        Schemata.Column metadata = getMetadataFor(selectorName, field);
        PropertyType type = metadata.getRequiredType();
        switch (type) {
            case DATE:
                long lowerDate = factories.getLongFactory().create(lowerValue);
                long upperDate = factories.getLongFactory().create(upperValue);
                return NumericRangeQuery.newLongRange(field, lowerDate, upperDate, includesLower, includesUpper);
            case LONG:
                long lowerLong = factories.getLongFactory().create(lowerValue);
                long upperLong = factories.getLongFactory().create(upperValue);
                return NumericRangeQuery.newLongRange(field, lowerLong, upperLong, includesLower, includesUpper);
            case DOUBLE:
                double lowerDouble = factories.getDoubleFactory().create(lowerValue);
                double upperDouble = factories.getDoubleFactory().create(upperValue);
                return NumericRangeQuery.newDoubleRange(field, lowerDouble, upperDouble, includesLower, includesUpper);
            case BOOLEAN:
                int lowerInt = factories.getBooleanFactory().create(lowerValue).booleanValue() ? 1 : 0;
                int upperInt = factories.getBooleanFactory().create(upperValue).booleanValue() ? 1 : 0;
                return NumericRangeQuery.newIntRange(field, lowerInt, upperInt, includesLower, includesUpper);
            case DECIMAL:
                BigDecimal lowerDecimal = factories.getDecimalFactory().create(lowerValue);
                BigDecimal upperDecimal = factories.getDecimalFactory().create(upperValue);
                String lsv = FieldUtil.decimalToString(lowerDecimal);
                String usv = FieldUtil.decimalToString(upperDecimal);
                Query lower = null;
                if (includesLower) {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(lsv, field, factories, false);
                } else {
                    lower = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(lsv, field, factories, false);
                }
                Query upper = null;
                if (includesUpper) {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(usv, field, factories, false);
                } else {
                    upper = CompareStringQuery.createQueryForNodesWithFieldLessThan(usv, field, factories, false);
                }
                BooleanQuery query = new BooleanQuery();
                query.add(lower, Occur.MUST);
                query.add(upper, Occur.MUST);
                return query;
            case OBJECT:
            case URI:
            case UUID:
            case PATH:
            case NAME:
            case STRING:
            case REFERENCE:
            case WEAKREFERENCE:
            case BINARY:
                assert false;
        }
        return new MatchNoneQuery();
    }

    protected String likeExpresionForWildcardPath( String path ) {
        if (path.equals("/") || path.equals("%")) return path;
        StringBuilder sb = new StringBuilder();
        path = path.replaceAll("%+", "%");
        if (path.startsWith("%/")) {
            sb.append("%");
            if (path.length() == 2) return sb.toString();
            path = path.substring(2);
        }
        for (String segment : path.split("/")) {
            if (segment.length() == 0) continue;
            sb.append("/");
            sb.append(segment);
            if (segment.equals("%") || segment.equals("_")) continue;
            if (!segment.endsWith("]") && !segment.endsWith("]%") && !segment.endsWith("]_")) {
                sb.append("[1]");
            }
        }
        if (path.endsWith("/")) sb.append("/");
        return sb.toString();
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   NodePath nodePath,
                                   Operator operator,
                                   Object value,
                                   boolean caseSensitive ) {
        if (!caseSensitive) value = stringFactory.create(value).toLowerCase();
        Path pathValue = operator != Operator.LIKE ? pathFactory.create(value) : null;
        Query query = null;
        switch (operator) {
            case EQUAL_TO:
                return findNodeAt(pathValue);
            case NOT_EQUAL_TO:
                return not(findNodeAt(pathValue));
            case LIKE:
                String likeExpression = stringFactory.create(value);
                likeExpression = likeExpresionForWildcardPath(likeExpression);
                if (likeExpression.indexOf("[%]") != -1) {
                    // We can't use '[%]' because we only want to match digits,
                    // so handle this using a regex ...
                    String regex = likeExpression;
                    regex = regex.replace("[%]", "[\\d+]");
                    regex = regex.replace("[", "\\[");
                    regex = regex.replace("*", ".*").replace("?", ".");
                    regex = regex.replace("%", ".*").replace("_", ".");
                    // Now create a regex query ...
                    RegexQuery regexQuery = new RegexQuery(new Term(FieldName.PATH, regex));
                    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                    regexQuery.setRegexImplementation(new JavaUtilRegexCapabilities(flags));
                    query = regexQuery;
                } else {
                    query = findNodesLike(selectorName, FieldName.PATH, likeExpression, caseSensitive);
                }
                break;
            case GREATER_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThan(pathValue,
                                                                                FieldName.PATH,
                                                                                factories,
                                                                                caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathGreaterThanOrEqualTo(pathValue,
                                                                                         FieldName.PATH,
                                                                                         factories,
                                                                                         caseSensitive);
                break;
            case LESS_THAN:
                query = ComparePathQuery.createQueryForNodesWithPathLessThan(pathValue, FieldName.PATH, factories, caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = ComparePathQuery.createQueryForNodesWithPathLessThanOrEqualTo(pathValue,
                                                                                      FieldName.PATH,
                                                                                      factories,
                                                                                      caseSensitive);
                break;
        }
        return query;
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   NodeName nodeName,
                                   Operator operator,
                                   Object value,
                                   boolean caseSensitive ) {
        String stringValue = stringFactory.create(value);
        if (!caseSensitive) stringValue = stringValue.toLowerCase();
        Path.Segment segment = operator != Operator.LIKE ? pathFactory.createSegment(stringValue) : null;
        // Determine if the string value contained a SNS index ...
        boolean includeSns = stringValue.indexOf('[') != -1;
        int snsIndex = operator != Operator.LIKE ? segment.getIndex() : 0;
        Query query = null;
        switch (operator) {
            case EQUAL_TO:
                if (!includeSns) {
                    return new TermQuery(new Term(FieldName.NODE_NAME, stringValue));
                }
                BooleanQuery booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(FieldName.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, snsIndex, snsIndex, true, true), Occur.MUST);
                return booleanQuery;
            case NOT_EQUAL_TO:
                if (!includeSns) {
                    return not(new TermQuery(new Term(FieldName.NODE_NAME, stringValue)));
                }
                booleanQuery = new BooleanQuery();
                booleanQuery.add(new TermQuery(new Term(FieldName.NODE_NAME, stringValue)), Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, snsIndex, snsIndex, true, true), Occur.MUST);
                return not(booleanQuery);
            case GREATER_THAN:
                query = CompareNameQuery.createQueryForNodesWithNameGreaterThan(segment,
                                                                                FieldName.NODE_NAME,
                                                                                FieldName.SNS_INDEX,
                                                                                factories,
                                                                                caseSensitive,
                                                                                includeSns);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = CompareNameQuery.createQueryForNodesWithNameGreaterThanOrEqualTo(segment,
                                                                                         FieldName.NODE_NAME,
                                                                                         FieldName.SNS_INDEX,
                                                                                         factories,
                                                                                         caseSensitive,
                                                                                         includeSns);
                break;
            case LESS_THAN:
                query = CompareNameQuery.createQueryForNodesWithNameLessThan(segment,
                                                                             FieldName.NODE_NAME,
                                                                             FieldName.SNS_INDEX,
                                                                             factories,
                                                                             caseSensitive,
                                                                             includeSns);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = CompareNameQuery.createQueryForNodesWithNameLessThanOrEqualTo(segment,
                                                                                      FieldName.NODE_NAME,
                                                                                      FieldName.SNS_INDEX,
                                                                                      factories,
                                                                                      caseSensitive,
                                                                                      includeSns);
                break;
            case LIKE:
                // See whether the like expression has brackets ...
                String likeExpression = stringValue;
                int openBracketIndex = likeExpression.indexOf('[');
                if (openBracketIndex != -1) {
                    String localNameExpression = likeExpression.substring(0, openBracketIndex);
                    String snsIndexExpression = likeExpression.substring(openBracketIndex);
                    Query localNameQuery = CompareStringQuery.createQueryForNodesWithFieldLike(localNameExpression,
                                                                                               FieldName.NODE_NAME,
                                                                                               factories,
                                                                                               caseSensitive);
                    Query snsQuery = createSnsIndexQuery(snsIndexExpression);
                    if (localNameQuery == null) {
                        if (snsQuery == null) {
                            query = new MatchNoneQuery();
                        } else {
                            // There is just an SNS part ...
                            query = snsQuery;
                        }
                    } else {
                        // There is a local name part ...
                        if (snsQuery == null) {
                            query = localNameQuery;
                        } else {
                            // There is both a local name part and a SNS part ...
                            booleanQuery = new BooleanQuery();
                            booleanQuery.add(localNameQuery, Occur.MUST);
                            booleanQuery.add(snsQuery, Occur.MUST);
                            query = booleanQuery;
                        }
                    }
                } else {
                    // There is no SNS expression ...
                    query = CompareStringQuery.createQueryForNodesWithFieldLike(likeExpression,
                                                                                FieldName.NODE_NAME,
                                                                                factories,
                                                                                caseSensitive);
                }
                assert query != null;
                break;
        }
        return query;
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   NodeLocalName nodeName,
                                   Operator operator,
                                   Object value,
                                   boolean caseSensitive ) {
        String nameValue = stringFactory.create(value);
        Query query = null;
        switch (operator) {
            case LIKE:
                String likeExpression = nameValue;
                query = findNodesLike(FieldName.LOCAL_NAME, likeExpression, caseSensitive);
                break;
            case EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               FieldName.LOCAL_NAME,
                                                                               factories,
                                                                               caseSensitive);
                break;
            case NOT_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldEqualTo(nameValue,
                                                                               FieldName.LOCAL_NAME,
                                                                               factories,
                                                                               caseSensitive);
                query = not(query);
                break;
            case GREATER_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThan(nameValue,
                                                                                   FieldName.LOCAL_NAME,
                                                                                   factories,
                                                                                   caseSensitive);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldGreaterThanOrEqualTo(nameValue,
                                                                                            FieldName.LOCAL_NAME,
                                                                                            factories,
                                                                                            caseSensitive);
                break;
            case LESS_THAN:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThan(nameValue,
                                                                                FieldName.LOCAL_NAME,
                                                                                factories,
                                                                                caseSensitive);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                query = CompareStringQuery.createQueryForNodesWithFieldLessThanOrEqualTo(nameValue,
                                                                                         FieldName.LOCAL_NAME,
                                                                                         factories,
                                                                                         caseSensitive);
                break;
        }
        return query;
    }

    @Override
    protected Query findNodesWith( SelectorName selectorName,
                                   NodeDepth depthConstraint,
                                   Operator operator,
                                   Object value ) {
        int depth = factories.getLongFactory().create(value).intValue();
        switch (operator) {
            case EQUAL_TO:
                return NumericRangeQuery.newIntRange(FieldName.DEPTH, depth, depth, true, true);
            case NOT_EQUAL_TO:
                Query query = NumericRangeQuery.newIntRange(FieldName.DEPTH, depth, depth, true, true);
                return not(query);
            case GREATER_THAN:
                return NumericRangeQuery.newIntRange(FieldName.DEPTH, depth, MAX_DEPTH, false, true);
            case GREATER_THAN_OR_EQUAL_TO:
                return NumericRangeQuery.newIntRange(FieldName.DEPTH, depth, MAX_DEPTH, true, true);
            case LESS_THAN:
                return NumericRangeQuery.newIntRange(FieldName.DEPTH, MIN_DEPTH, depth, true, false);
            case LESS_THAN_OR_EQUAL_TO:
                return NumericRangeQuery.newIntRange(FieldName.DEPTH, MIN_DEPTH, depth, true, true);
            case LIKE:
                // This is not allowed ...
                return null;
        }
        return null;
    }

    /**
     * Utility method to generate a query against the SNS indexes. This method attempts to generate a query that works most
     * efficiently, depending upon the supplied expression. For example, if the supplied expression is just "[3]", then a range
     * query is used to find all values matching '3'. However, if "[3_]" is used (where '_' matches any single-character, or digit
     * in this case), then a range query is used to find all values between '30' and '39'. Similarly, if "[3%]" is used, then a
     * regular expression query is used.
     * 
     * @param likeExpression the expression that uses the JCR 2.0 LIKE representation, and which includes the leading '[' and
     *        trailing ']' characters
     * @return the query, or null if the expression cannot be represented as a query
     */
    protected Query createSnsIndexQuery( String likeExpression ) {
        if (likeExpression == null) return null;
        likeExpression = likeExpression.trim();
        if (likeExpression.length() == 0) return null;

        // Remove the leading '[' ...
        assert likeExpression.charAt(0) == '[';
        likeExpression = likeExpression.substring(1);

        // Remove the trailing ']' if it exists ...
        int closeBracketIndex = likeExpression.indexOf(']');
        if (closeBracketIndex != -1) {
            likeExpression = likeExpression.substring(0, closeBracketIndex);
        }
        if (likeExpression.equals("_")) {
            // The SNS expression can only be one digit ...
            return NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, MIN_SNS_INDEX, 9, true, true);
        }
        if (likeExpression.equals("%")) {
            // The SNS expression can be any digits ...
            return NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, MIN_SNS_INDEX, MAX_SNS_INDEX, true, true);
        }
        if (likeExpression.indexOf('_') != -1) {
            if (likeExpression.indexOf('%') != -1) {
                // Contains both ...
                return findNodesLike(FieldName.SNS_INDEX, likeExpression, true);
            }
            // It presumably contains some numbers and at least one '_' character ...
            int firstWildcardChar = likeExpression.indexOf('_');
            if (firstWildcardChar + 1 < likeExpression.length()) {
                // There's at least some characters after the first '_' ...
                int secondWildcardChar = likeExpression.indexOf('_', firstWildcardChar + 1);
                if (secondWildcardChar != -1) {
                    // There are multiple '_' characters ...
                    return findNodesLike(FieldName.SNS_INDEX, likeExpression, true);
                }
            }
            // There's only one '_', so parse the lowermost value and uppermost value ...
            String lowerExpression = likeExpression.replace('_', '0');
            String upperExpression = likeExpression.replace('_', '9');
            try {
                // This SNS is just a number ...
                int lowerSns = Integer.parseInt(lowerExpression);
                int upperSns = Integer.parseInt(upperExpression);
                return NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, lowerSns, upperSns, true, true);
            } catch (NumberFormatException e) {
                // It's not a number but it's in the SNS field, so there will be no results ...
                return new MatchNoneQuery();
            }
        }
        if (likeExpression.indexOf('%') != -1) {
            // It presumably contains some numbers and at least one '%' character ...
            return findNodesLike(FieldName.SNS_INDEX, likeExpression, true);
        }
        // This is not a LIKE expression but an exact value specification and should be a number ...
        try {
            // This SNS is just a number ...
            int sns = Integer.parseInt(likeExpression);
            return NumericRangeQuery.newIntRange(FieldName.SNS_INDEX, sns, sns, true, true);
        } catch (NumberFormatException e) {
            // It's not a number but it's in the SNS field, so there will be no results ...
            return new MatchNoneQuery();
        }
    }

}
