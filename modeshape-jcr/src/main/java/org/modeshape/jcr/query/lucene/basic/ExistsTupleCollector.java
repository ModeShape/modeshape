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
package org.modeshape.jcr.query.lucene.basic;

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;

/**
 * A special TupleCollector implementation that will terminate when it comes across a second document with a non-zero score.
 */
public class ExistsTupleCollector extends TupleCollector {

    private final TupleCollector collector;
    private boolean found = false;

    /**
     * Create a ExistsTupleCollector wrapper over another {@link Collector}, where the wrapper stops after the first non-zero
     * scored document.
     * 
     * @param collector the wrapped {@link Collector}
     */
    public ExistsTupleCollector( final TupleCollector collector ) {
        this.collector = collector;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    public float doCollect( int doc ) throws IOException {
        if (found) {
            throw new CompletedException();
        }
        float score = this.collector.doCollect(doc);
        if (score >= 0.0f) {
            found = true;
        }
        return score;
    }

    /** Thrown when elapsed search time exceeds allowed search time. */
    public static class CompletedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private CompletedException() {
            super("Exists collector completed with 1 found tuple");
        }
    }

    @Override
    public List<Object[]> getTuples() {
        return collector.getTuples();
    }

    @Override
    public void setScorer( Scorer scorer ) throws IOException {
        collector.setScorer(scorer);
    }

    @Override
    public void setNextReader( IndexReader reader,
                               int docBase ) throws IOException {
        collector.setNextReader(reader, docBase);
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return collector.acceptsDocsOutOfOrder();
    }
}
