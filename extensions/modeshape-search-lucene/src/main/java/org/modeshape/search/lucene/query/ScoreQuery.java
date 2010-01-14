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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.modeshape.graph.query.model.FullTextSearchScore;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link FullTextSearchScore} criteria a NOT expression of another
 * wrapped Query object. This query implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * that were <i>not</i> scored by the wrapped query. In other words, if the wrapped query ended up scoring any document, that
 * document is <i>not</i> scored (i.e., skipped) by this query.
 */
public class ScoreQuery extends Query {

    private static final long serialVersionUID = 1L;

    /**
     * The operand that is being negated by this query.
     */
    protected final Query operand;

    /**
     * Construct a NOT(x) constraint where the 'x' operand is supplied.
     * 
     * @param operand the operand being negated
     */
    public ScoreQuery( Query operand ) {
        this.operand = operand;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    @Override
    public Weight createWeight( Searcher searcher ) {
        return new NotWeight(searcher);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new ScoreQuery(operand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString( String field ) {
        return "NOT(" + operand.toString(field) + ")";
    }

    /**
     * Calculates query weights and builds query scores for our NOT queries.
     */
    protected class NotWeight extends Weight {
        private static final long serialVersionUID = 1L;
        private final Searcher searcher;

        protected NotWeight( Searcher searcher ) {
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
            return ScoreQuery.this;
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
                              boolean topScorer ) throws IOException {
            // Get the operand's score, and set this on the NOT query
            Scorer operandScorer = operand.weight(searcher).scorer(reader, scoreDocsInOrder, topScorer);
            // Return a custom scorer ...
            return new NotScorer(operandScorer, reader);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int)
         */
        @Override
        public Explanation explain( IndexReader reader,
                                    int doc ) throws IOException {
            Explanation operandExplanation = operand.weight(searcher).explain(reader, doc);
            String desc = operandExplanation.getDescription();
            return new Explanation(getValue(), "NOT(" + desc + ")");
        }
    }

    /**
     * A scorer for the NOT query that iterates over documents (in increasing docId order), using the given scorer implementation
     * for the operand of the NOT.
     */
    protected static class NotScorer extends Scorer {
        private int docId = -1;
        private int nextScoredDocId = -1;
        private final Scorer operandScorer;
        private final IndexReader reader;
        private final int pastMaxDocId;

        /**
         * @param operandScorer the scorer that is used to score the documents based upon the operand of the NOT; may not be null
         * @param reader the reader that has access to all the docs ...
         */
        protected NotScorer( Scorer operandScorer,
                             IndexReader reader ) {
            // We don't care which Similarity we have, because we don't use it. So get the default.
            super(Similarity.getDefault());
            this.operandScorer = operandScorer;
            this.reader = reader;
            assert this.operandScorer != null;
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
            if (nextScoredDocId == -1) {
                // Find the first document that is scored by the operand's scorer ...
                nextScoredDocId = operandScorer.nextDoc();
            }
            do {
                ++docId;
                if (docId == pastMaxDocId) {
                    // We're aleady to the end of the documents in the index, so return no more docs
                    return Scorer.NO_MORE_DOCS;
                }
                if (docId == nextScoredDocId) {
                    // Find the next document that is scored by the operand's scorer ...
                    nextScoredDocId = operandScorer.nextDoc();
                    continue;
                }
                if (reader.isDeleted(docId)) {
                    // We should skip this document ...
                    continue;
                }
                return docId;
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
