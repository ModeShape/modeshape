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
import java.util.Set;
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

/**
 * A Lucene {@link Query} implementation that is used to score positively those documents that have a ID in the supplied set. This
 * works for large sets of IDs; in smaller numbers, it may be more efficient to create a boolean query that checks for each of the
 * IDs.
 */
public class IdsQuery extends Query {

    private static final long serialVersionUID = 1L;

    /**
     * The operand that is being negated by this query.
     */
    protected final Set<String> uuids;
    protected final FieldSelector fieldSelector;
    protected final String fieldName;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param ids the set of ID values; may not be null
     */
    public IdsQuery( String fieldName,
                     Set<String> ids ) {
        this.fieldName = fieldName;
        this.uuids = ids;
        assert this.fieldName != null;
        assert this.uuids != null;
        this.fieldSelector = new FieldSelector() {
            private static final long serialVersionUID = 1L;

            public FieldSelectorResult accept( String fieldName ) {
                return fieldName.equals(fieldName) ? FieldSelectorResult.LOAD_AND_BREAK : FieldSelectorResult.NO_LOAD;
            }
        };
    }

    protected boolean includeDocument( IndexReader reader,
                                       int docId ) throws IOException {
        Document doc = reader.document(docId, fieldSelector);
        String valueString = doc.get(fieldName);
        return valueString != null && uuids.contains(valueString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    @Override
    public Weight createWeight( Searcher searcher ) {
        return new IdSetWeight(searcher);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString( String field ) {
        return fieldName + " IN UUIDs";
    }

    /**
     * Calculates query weights and builds query scores for our NOT queries.
     */
    protected class IdSetWeight extends Weight {
        private static final long serialVersionUID = 1L;
        private final Searcher searcher;

        protected IdSetWeight( Searcher searcher ) {
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
            return IdsQuery.this;
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
            return new IdScorer(reader);
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
    protected class IdScorer extends Scorer {
        private int docId = -1;
        private final int pastMaxDocId;
        private final IndexReader reader;

        protected IdScorer( IndexReader reader ) {
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
                if (includeDocument(reader, docId)) return docId;
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
