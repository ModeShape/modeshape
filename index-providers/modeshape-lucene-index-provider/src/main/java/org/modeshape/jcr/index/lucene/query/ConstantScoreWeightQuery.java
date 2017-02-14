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
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.BytesRef;
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
    
    protected static final float SCORE = 1.0f;
   
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
        return new ConstantScoreWeight(this) {
            @Override
            public Scorer scorer(LeafReaderContext context) throws IOException {
                LeafReader leafReader = context.reader();
                Terms terms = leafReader.terms(field);
                if (terms == null) {
                    return new ConstantScoreScorer(this, SCORE, DocIdSetIterator.empty()); 
                }
                TermsEnum termsEnum = terms.iterator();
                FixedBitSet result = new FixedBitSet(leafReader.maxDoc());
                BytesRef bytesRef;
                PostingsEnum postingsEnum = null;
                while ((bytesRef = termsEnum.next()) != null) {
                    String value = bytesRef.utf8ToString();
                    if (accepts(value)) {
                        postingsEnum = termsEnum.postings(postingsEnum);
                        int docId;
                        while ((docId = postingsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                            result.set(docId);    
                        }
                    }
                }
                return new ConstantScoreScorer(this, SCORE, new BitSetIterator(result, result.cardinality()));
            }
        };
    }

    /**
     * Checks if the given fields are valid for the document matched by a particular query or not.
     *
     * @param value a {@code String} representing a stored index value; never {@code null}
     * @return {@code true} if the value is valid, {@code false} if not.
     */
    protected abstract boolean accepts(String value);
    
    @Override
    public boolean equals(Object obj) {
        return sameClassAs(obj) && field.equals(((ConstantScoreWeightQuery)obj).field);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = classHash();
        result = prime * result + field.hashCode();
        return result;
    }
}
