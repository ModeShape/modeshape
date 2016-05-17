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
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RandomAccessWeight;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
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

    protected ConstantScoreWeightQuery(String field) {
        CheckArg.isNotNull(field, "field");
        this.field = field;
    }

    protected String field() {
        return field;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        Set<String> fieldSet = Collections.singleton(field);
        // return a weight which uses a constant (1.0f) scorer...
        return new RandomAccessWeight(this) {
            @Override
            protected Bits getMatchingDocs(LeafReaderContext context) throws IOException {
                LeafReader leafReader = context.reader();
                Bits liveDocs = leafReader.getLiveDocs();
                // if liveDocs is null it means there are no deleted documents...
                int docsCount = liveDocs != null ? liveDocs.length() : leafReader.numDocs();
                FixedBitSet result = new FixedBitSet(leafReader.maxDoc());
                for (int i = 0; i < docsCount; i++) {
                    if (liveDocs != null && !liveDocs.get(i)) {
                        continue;
                    }
                    Document document = leafReader.document(i, fieldSet);
                    IndexableField[] fields = document.getFields(field);
                    if (fields.length == 0) {
                        // the document doesn't have the field...
                        continue;
                    }
                    if (areValid(fields)) {
                        result.set(i);
                    }
                }
                return result.cardinality() > 0 ? result : null;
            }
        };
    }

    /**
     * Checks if the given fields are valid for the document matched by a particular query or not.
     *
     * @param fields a {@link IndexableField} array instance which was loaded by the scorer for the current field; never {@code null}
     * and never empty.
     * @return {@code true} if the array of fields is valid and document should be returned by the query or {@code false} if not.
     */
    protected abstract boolean areValid(IndexableField... fields);
}
