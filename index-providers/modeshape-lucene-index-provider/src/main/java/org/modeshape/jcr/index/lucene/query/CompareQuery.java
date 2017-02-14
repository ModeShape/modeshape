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

import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.jcr.query.qom.Comparison;
import org.apache.lucene.search.Query;
import org.modeshape.common.annotation.Immutable;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the indexed nodes. 
 * <p> 
 * This should only be used when the data stored in the indexes for {@code ValueType} is a {@link String}.
 * </p> 
 * 
 * @param <ValueType> the actual value type used by the query
 */
@SuppressWarnings( "deprecation" )
@Immutable
public abstract class CompareQuery<ValueType> extends ConstantScoreWeightQuery {

    /**
     * The operand that is being negated by this query.
     */
    protected final ValueType constraintValue;
    protected final BiPredicate<ValueType, ValueType> evaluator;
    protected final Function<String, String> caseOperation;
    
    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     *
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param evaluator the {@link BiPredicate} implementation that returns whether the node value satisfies the constraint; may not
     * be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     */
    protected CompareQuery(final String fieldName,
                           ValueType constraintValue,
                           BiPredicate<ValueType, ValueType> evaluator,
                           Function<String, String> caseOperation) {
        super(fieldName);
        this.constraintValue = constraintValue;
        this.caseOperation = caseOperation;
        this.evaluator = evaluator;
        assert this.constraintValue != null;
        assert this.evaluator != null;
    }

    @Override
    protected boolean accepts(String value) {
        if (value == null) {
            return false;
        }
        String casedValue = caseOperation != null ? caseOperation.apply(value) : value;
        ValueType convertedValue = convertValue(casedValue);
        return evaluator.test(convertedValue, constraintValue);
    }
    
    protected abstract ValueType convertValue(String casedValue);
    
    @Override
    public String toString( String field ) {
        return "compare['" + field + "' against '" + constraintValue + "']";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!sameClassAs(obj)) {
            return false;
        }
        CompareQuery<?> otherQuery = (CompareQuery<?>) obj;
        return sameClassAs(obj) && field().equals(otherQuery.field()) && constraintValue.equals(otherQuery.constraintValue);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = classHash();
        result = prime * result + field().hashCode();
        result = prime * result + constraintValue.hashCode();
        return result;
    }
}
