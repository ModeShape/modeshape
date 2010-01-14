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
package org.modeshape.search.lucene.query;

import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.model.Length;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Length} constraint against a string field. This query
 * implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * with string fields that satisfy the constraint.
 */
public class CompareLengthQuery extends CompareQuery<Integer> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<Integer> EQUAL_TO = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue == length;
        }

        @Override
        public String toString() {
            return " = ";
        }
    };
    protected static final Evaluator<Integer> NOT_EQUAL_TO = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue == length;
        }

        @Override
        public String toString() {
            return " != ";
        }
    };
    protected static final Evaluator<Integer> IS_LESS_THAN = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Integer> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Integer> IS_GREATER_THAN = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Integer> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<Integer>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Integer nodeValue,
                                            Integer length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is equal to the supplied constraint
     * value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldEqualTo( Integer constraintValue,
                                                                          String fieldName,
                                                                          ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_GREATER_THAN);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is not equal to the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldNotEqualTo( Integer constraintValue,
                                                                             String fieldName,
                                                                             ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_GREATER_THAN);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is greater than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldGreaterThan( Integer constraintValue,
                                                                              String fieldName,
                                                                              ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_GREATER_THAN);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is greater than or equal to the
     * supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldGreaterThanOrEqualTo( Integer constraintValue,
                                                                                       String fieldName,
                                                                                       ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_GREATER_THAN_OR_EQUAL_TO);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is less than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldLessThan( Integer constraintValue,
                                                                           String fieldName,
                                                                           ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_LESS_THAN);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a field length that is less than or equal to the
     * supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @return the query; never null
     */
    public static CompareLengthQuery createQueryForNodesWithFieldLessThanOrEqualTo( Integer constraintValue,
                                                                                    String fieldName,
                                                                                    ValueFactories factories ) {
        return new CompareLengthQuery(fieldName, constraintValue, factories.getStringFactory(), IS_LESS_THAN_OR_EQUAL_TO);
    }

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     */
    protected CompareLengthQuery( String fieldName,
                                  Integer constraintValue,
                                  ValueFactory<String> stringFactory,
                                  Evaluator<Integer> evaluator ) {
        super(fieldName, constraintValue, null, stringFactory, evaluator);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.query.CompareQuery#readFromDocument(org.apache.lucene.index.IndexReader, int)
     */
    @Override
    protected Integer readFromDocument( IndexReader reader,
                                        int docId ) throws IOException {
        // This implementation reads the length of the field ...
        Document doc = reader.document(docId, fieldSelector);
        String valueString = doc.get(fieldName);
        String value = stringFactory.create(valueString);
        return value != null ? value.length() : 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new CompareLengthQuery(fieldName, constraintValue, stringFactory, evaluator);
    }
}
