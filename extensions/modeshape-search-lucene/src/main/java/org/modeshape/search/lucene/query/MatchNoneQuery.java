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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

/**
 * A Lucene {@link Query} implementation that always matches no documents.
 */
public class MatchNoneQuery extends Query {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a query that always returns no documents.
     */
    public MatchNoneQuery() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    @Override
    public Weight createWeight( Searcher searcher ) {
        return new NoneWeight();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new MatchNoneQuery();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString( String field ) {
        return "NO DOCS";
    }

    /**
     * Calculates query weights and builds query scores for our NOT queries.
     */
    protected class NoneWeight extends Weight {
        private static final long serialVersionUID = 1L;

        protected NoneWeight() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#getQuery()
         */
        @Override
        public Query getQuery() {
            return MatchNoneQuery.this;
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
            return new NoneScorer();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public Explanation explain( IndexReader reader,
                                    int doc ) {
            return new Explanation(getValue(), "NO VALUES");
        }
    }

    /**
     * A scorer for the NOT query that iterates over documents (in increasing docId order), using the given scorer implementation
     * for the operand of the NOT.
     */
    protected static class NoneScorer extends Scorer {
        private int docId = -1;

        protected NoneScorer() {
            // We don't care which Similarity we have, because we don't use it. So get the default.
            super(Similarity.getDefault());
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
        public int nextDoc() {
            docId = Scorer.NO_MORE_DOCS;
            return docId;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
         */
        @Override
        public int advance( int target ) {
            return Scorer.NO_MORE_DOCS;
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
