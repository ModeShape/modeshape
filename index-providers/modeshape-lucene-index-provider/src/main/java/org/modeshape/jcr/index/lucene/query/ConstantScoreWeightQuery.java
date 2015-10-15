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

import java.io.IOException;
import java.util.Collections;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * Base class for ModeShape-specific Lucene queries, which always use a constant weight and score of 1.0f for the documents.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
public abstract class ConstantScoreWeightQuery extends Query {
    
    private final String field;

    protected ConstantScoreWeightQuery( String field ) {
        CheckArg.isNotNull(field, "field");
        this.field = field;
    }
    
    protected String field() {
        return field;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        return new ConstantWeight(this);
    }
    
    protected class ConstantWeight extends ConstantScoreWeight {
        private ConstantWeight( Query query ) {
            super(query);
        }

        @Override
        public Scorer scorer( LeafReaderContext context ) throws IOException {
            return new ConstantWeightScorer(context, this);
        }
    }

    protected class ConstantWeightScorer extends Scorer {
        private int docId = -1;
        private final int maxDocId;
        private final LeafReader leafReader;

        public ConstantWeightScorer( LeafReaderContext readerContext, Weight weight ) {
            // We don't care which Similarity we have, because we don't use it. So get the default.
            super(weight);
            this.leafReader = readerContext.reader();
            assert this.leafReader != null;
            this.maxDocId = this.leafReader.maxDoc();
        }

        @Override
        public int freq() throws IOException {
            return 1;
        }

        @Override
        public long cost() {
            return maxDocId;
        }


        @Override
        public int docID() {
            return docId;
        }

        @Override
        public int nextDoc() throws IOException {
            do {
                ++docId;
                if (docId == maxDocId) {
                    docId = Scorer.NO_MORE_DOCS;
                    return Scorer.NO_MORE_DOCS;
                }
                Document document = leafReader.document(docId, Collections.singleton(field));
                if (document == null || document.getField(field) == null) {
                    // the document doesn't have the field...
                    continue;
                }
                if (isValid(document)) {
                    return docId;
                }
            } while (true);
        }

        @Override
        public int advance( int target ) throws IOException {
            if (target == Scorer.NO_MORE_DOCS) {
                return target;
            }
            while (true) {
                int doc = nextDoc();
                if (doc >= target) return doc;
            }
        }

        /**
         * This method always returns a score of 1.0 for the current document, since all the documents which satisfy the constraint
         * are considered equal
         *
         * @see org.apache.lucene.search.Scorer#score()
         */
        @Override
        public float score() {
            return 1.0f;
        }
    }

    /**
     * Checks if the given document is a valid document for a particular query or not. When this method is called can safely 
     * assume that {@link #field} is present on the document.
     *
     * @param document a {@link Document} instance which was loaded by the scorer for the current field. Subclasses can rely on
     * the fact that this document has a value for {@link #field}
     * @return {@code true} if the document instance is valid and should be returned by the query or {@code false} meaning it 
     * should be rejected.
     */
    protected abstract boolean isValid( Document document );
}
