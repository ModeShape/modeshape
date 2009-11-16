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
package org.jboss.dna.search.query;

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.jboss.dna.graph.property.ValueComparators;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.model.Comparison;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against a string field. This query
 * implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * with string fields that satisfy the constraint.
 */
public class CompareStringQuery extends CompareQuery<String> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<String> IS_LESS_THAN = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) < 0;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<String> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) <= 0;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<String> IS_GREATER_THAN = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) > 0;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<String> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThan( String constraintValue,
                                                                              String fieldName,
                                                                              ValueFactories factories,
                                                                              boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than or equal to
     * the supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThanOrEqualTo( String constraintValue,
                                                                                       String fieldName,
                                                                                       ValueFactories factories,
                                                                                       boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN_OR_EQUAL_TO, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThan( String constraintValue,
                                                                           String fieldName,
                                                                           ValueFactories factories,
                                                                           boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than or equal to the
     * supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThanOrEqualTo( String constraintValue,
                                                                                    String fieldName,
                                                                                    ValueFactories factories,
                                                                                    boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN_OR_EQUAL_TO, caseSensitive);
    }

    private final boolean caseSensitive;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     */
    protected CompareStringQuery( String fieldName,
                                  String constraintValue,
                                  ValueFactory<String> valueFactory,
                                  ValueFactory<String> stringFactory,
                                  Evaluator<String> evaluator,
                                  boolean caseSensitive ) {
        super(fieldName, caseSensitive ? constraintValue : constraintValue.toLowerCase(), valueFactory, stringFactory, evaluator);
        this.caseSensitive = caseSensitive;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.query.CompareQuery#readFromDocument(org.apache.lucene.index.IndexReader, int)
     */
    @Override
    protected String readFromDocument( IndexReader reader,
                                       int docId ) throws IOException {
        String result = super.readFromDocument(reader, docId);
        return caseSensitive ? result : result.toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new CompareStringQuery(fieldName, constraintValue, valueTypeFactory, stringFactory, evaluator, caseSensitive);
    }
}
