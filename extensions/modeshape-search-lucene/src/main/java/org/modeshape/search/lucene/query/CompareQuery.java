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
import java.io.Serializable;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.model.Comparison;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the Path of nodes. This
 * query implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * that correspond to nodes with Paths that satisfy the constraint.
 * 
 * @param <ValueType>
 */
public abstract class CompareQuery<ValueType> extends Query {

    private static final long serialVersionUID = 1L;

    protected static interface Evaluator<ValueType> extends Serializable {
        boolean satisfiesConstraint( ValueType nodeValue,
                                     ValueType constraintValue );
    }

    /**
     * The operand that is being negated by this query.
     */
    protected final String fieldName;
    protected final FieldSelector fieldSelector;
    protected final ValueType constraintValue;
    protected final Evaluator<ValueType> evaluator;
    protected final ValueFactory<ValueType> valueTypeFactory;
    protected final ValueFactory<String> stringFactory;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueTypeFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link Evaluator} implementation that returns whether the node value satisfies the constraint; may not
     *        be null
     */
    protected CompareQuery( String fieldName,
                            ValueType constraintValue,
                            ValueFactory<ValueType> valueTypeFactory,
                            ValueFactory<String> stringFactory,
                            Evaluator<ValueType> evaluator ) {
        this(fieldName, constraintValue, valueTypeFactory, stringFactory, evaluator, null);
    }

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueTypeFactory the value factory that can be used during the scoring; may not be null unless
     *        {@link #readFromDocument(IndexReader, int)} is overloaded to not use it
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link Evaluator} implementation that returns whether the node value satisfies the constraint; may not
     *        be null
     * @param fieldSelector the field selector that should load the fields needed to recover the value; may be null if the field
     *        selector should be generated automatically
     */
    protected CompareQuery( final String fieldName,
                            ValueType constraintValue,
                            ValueFactory<ValueType> valueTypeFactory,
                            ValueFactory<String> stringFactory,
                            Evaluator<ValueType> evaluator,
                            FieldSelector fieldSelector ) {
        this.fieldName = fieldName;
        this.constraintValue = constraintValue;
        this.valueTypeFactory = valueTypeFactory;
        this.stringFactory = stringFactory;
        this.evaluator = evaluator;
        assert this.fieldName != null;
        assert this.constraintValue != null;
        assert this.evaluator != null;
        this.fieldSelector = fieldSelector != null ? fieldSelector : new FieldSelector() {
            private static final long serialVersionUID = 1L;

            public FieldSelectorResult accept( String fieldName ) {
                return CompareQuery.this.fieldName.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
            }
        };
    }

    protected ValueType readFromDocument( IndexReader reader,
                                          int docId ) throws IOException {
        Document doc = reader.document(docId, fieldSelector);
        String valueString = doc.get(fieldName);
        return valueTypeFactory.create(valueString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    @Override
    public Weight createWeight( Searcher searcher ) {
        return new CompareWeight(searcher);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString( String field ) {
        return "(" + fieldName + evaluator.toString()
               + (stringFactory != null ? stringFactory.create(constraintValue) : constraintValue.toString()) + ")";
    }

    /**
     * Calculates query weights and builds query scores for our NOT queries.
     */
    protected class CompareWeight extends Weight {
        private static final long serialVersionUID = 1L;
        private final Searcher searcher;

        protected CompareWeight( Searcher searcher ) {
            this.searcher = searcher;
            assert this.searcher != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#getQuery()
         */
        @Override
        public Query getQuery() {
            return CompareQuery.this;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This implementation always returns a weight factor of 1.0.
         * </p>
         * 
         * @see org.apache.lucene.search.Weight#getValue()
         */
        @Override
        public float getValue() {
            return 1.0f; // weight factor of 1.0
        }

        /**
         * {@inheritDoc}
         * <p>
         * This implementation always returns a normalization factor of 1.0.
         * </p>
         * 
         * @see org.apache.lucene.search.Weight#sumOfSquaredWeights()
         */
        @Override
        public float sumOfSquaredWeights() {
            return 1.0f; // normalization factor of 1.0
        }

        /**
         * {@inheritDoc}
         * <p>
         * This implementation always does nothing, as there is nothing to normalize.
         * </p>
         * 
         * @see org.apache.lucene.search.Weight#normalize(float)
         */
        @Override
        public void normalize( float norm ) {
            // No need to do anything here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.IndexReader, boolean, boolean)
         */
        @Override
        public Scorer scorer( IndexReader reader,
                              boolean scoreDocsInOrder,
                              boolean topScorer ) {
            // Return a custom scorer ...
            return new CompareScorer(reader);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public Explanation explain( IndexReader reader,
                                    int doc ) {
            return new Explanation(getValue(), getQuery().toString());
        }
    }

    /**
     * A scorer for the Path query.
     */
    protected class CompareScorer extends Scorer {
        private int docId = -1;
        private final int pastMaxDocId;
        private final IndexReader reader;

        protected CompareScorer( IndexReader reader ) {
            // We don't care which Similarity we have, because we don't use it. So get the default.
            super(Similarity.getDefault());
            this.reader = reader;
            assert this.reader != null;
            this.pastMaxDocId = this.reader.maxDoc();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#docID()
         */
        @Override
        public int docID() {
            return docId;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
         */
        @Override
        public int nextDoc() throws IOException {
            do {
                ++docId;
                if (docId == pastMaxDocId) return Scorer.NO_MORE_DOCS;
                if (reader.isDeleted(docId)) {
                    // We should skip this document ...
                    continue;
                }
                ValueType value = readFromDocument(reader, docId);
                if (value == null) {
                    continue; // skip null values
                }
                if (evaluator.satisfiesConstraint(value, constraintValue)) return docId;
            } while (true);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
         */
        @Override
        public int advance( int target ) throws IOException {
            if (target == Scorer.NO_MORE_DOCS) return target;
            while (true) {
                int doc = nextDoc();
                if (doc >= target) return doc;
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * This method always returns a score of 1.0 for the current document, since only those documents that satisfy the NOT are
         * scored by this scorer.
         * </p>
         * 
         * @see org.apache.lucene.search.Scorer#score()
         */
        @Override
        public float score() {
            return 1.0f;
        }
    }
}
