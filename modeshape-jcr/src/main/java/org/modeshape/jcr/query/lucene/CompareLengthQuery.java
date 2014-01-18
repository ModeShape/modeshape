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
package org.modeshape.jcr.query.lucene;

import java.io.IOException;
import javax.jcr.query.qom.Length;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Length} constraint against a string field. This query
 * implementation works by using the weight and {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query
 * to score (and return) only those documents with string fields that satisfy the constraint.
 */
public class CompareLengthQuery extends CompareQuery<Long> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<Long> EQUAL_TO = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
            return nodeValue == length;
        }

        @Override
        public String toString() {
            return " = ";
        }
    };
    protected static final Evaluator<Long> NOT_EQUAL_TO = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
            return nodeValue == length;
        }

        @Override
        public String toString() {
            return " != ";
        }
    };
    protected static final Evaluator<Long> IS_LESS_THAN = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Long> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Long> IS_GREATER_THAN = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
            return nodeValue < length;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Long> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Long nodeValue,
                                            Long length ) {
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
    public static CompareLengthQuery createQueryForNodesWithFieldEqualTo( Long constraintValue,
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
    public static CompareLengthQuery createQueryForNodesWithFieldNotEqualTo( Long constraintValue,
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
    public static CompareLengthQuery createQueryForNodesWithFieldGreaterThan( Long constraintValue,
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
    public static CompareLengthQuery createQueryForNodesWithFieldGreaterThanOrEqualTo( Long constraintValue,
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
    public static CompareLengthQuery createQueryForNodesWithFieldLessThan( Long constraintValue,
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
    public static CompareLengthQuery createQueryForNodesWithFieldLessThanOrEqualTo( Long constraintValue,
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
                                  Long constraintValue,
                                  ValueFactory<String> stringFactory,
                                  Evaluator<Long> evaluator ) {
        super(fieldName, constraintValue, null, stringFactory, evaluator);
    }

    @Override
    protected Long readFromDocument( IndexReader reader,
                                     int docId ) throws IOException {
        // This implementation reads the length of the field ...
        Document doc = reader.document(docId, fieldSelector);
        String valueString = doc.get(fieldName);
        String value = stringFactory.create(valueString);
        return value != null ? (long)value.length() : 0L;
    }

    @Override
    public Object clone() {
        return new CompareLengthQuery(fieldName, constraintValue, stringFactory, evaluator);
    }
}
