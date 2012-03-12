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
