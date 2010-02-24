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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.junit.Test;

public class NotQueryTest {

    @Test
    public void scorerShouldSkipAdjacentDocsIfScoredByOperandScorer() throws IOException {
        IndexReader reader = mock(IndexReader.class);
        when(reader.isDeleted(anyInt())).thenReturn(false);
        when(reader.maxDoc()).thenReturn(10);
        Scorer operandScorer = new MockScorer(0, 1, 2, 3, 4);
        Scorer notScorer = new NotQuery.NotScorer(operandScorer, reader);
        assertScores(notScorer, 5, 6, 7, 8, 9);
    }

    @Test
    public void scorerShouldSkipDocsAtEndIfScoredByOperandScorer() throws IOException {
        IndexReader reader = mock(IndexReader.class);
        when(reader.isDeleted(anyInt())).thenReturn(false);
        when(reader.maxDoc()).thenReturn(10);
        Scorer operandScorer = new MockScorer(8, 9);
        Scorer notScorer = new NotQuery.NotScorer(operandScorer, reader);
        assertScores(notScorer, 0, 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void scorerShouldScoreFirstDocsIfNotScoredByOperandScorer() throws IOException {
        IndexReader reader = mock(IndexReader.class);
        when(reader.isDeleted(anyInt())).thenReturn(false);
        when(reader.maxDoc()).thenReturn(10);
        Scorer operandScorer = new MockScorer(2, 3, 4);
        Scorer notScorer = new NotQuery.NotScorer(operandScorer, reader);
        assertScores(notScorer, 0, 1, 5, 6, 7, 8, 9);
    }

    @Test
    public void scorerShouldScoreNonAdjacentDocsNotScoredByOperandScorer() throws IOException {
        IndexReader reader = mock(IndexReader.class);
        when(reader.isDeleted(anyInt())).thenReturn(false);
        when(reader.maxDoc()).thenReturn(10);
        Scorer operandScorer = new MockScorer(2, 4, 8);
        Scorer notScorer = new NotQuery.NotScorer(operandScorer, reader);
        assertScores(notScorer, 0, 1, 3, 5, 6, 7, 9);
    }

    protected void assertScores( Scorer scorer,
                                 int... docIds ) throws IOException {
        for (int docId : docIds) {
            assertThat(scorer.nextDoc(), is(docId));
            assertThat(scorer.score(), is(1.0f));
        }
        assertThat(scorer.nextDoc(), is(Scorer.NO_MORE_DOCS));
    }

    protected static class MockScorer extends Scorer {
        private final Iterator<Integer> docIds;

        protected MockScorer( int... docIds ) {
            super(Similarity.getDefault());
            List<Integer> ids = new ArrayList<Integer>();
            for (int docId : docIds) {
                ids.add(new Integer(docId));
            }
            this.docIds = ids.iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
         */
        @Override
        public int advance( int target ) {
            int doc;
            while ((doc = nextDoc()) < target) {
            }
            return doc;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#docID()
         */
        @Override
        public int docID() {
            return nextDoc();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
         */
        @Override
        public int nextDoc() {
            if (docIds.hasNext()) return docIds.next();
            return Scorer.NO_MORE_DOCS;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.lucene.search.Scorer#score()
         */
        @Override
        public float score() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }

}
