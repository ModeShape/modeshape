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

import javax.jcr.query.qom.Comparison;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.index.lucene.query.CaseOperations.CaseOperation;
import org.modeshape.jcr.value.ValueFactory;

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

    protected static interface Evaluator<ValueType> {
        boolean satisfiesConstraint( ValueType nodeValue,
                                     ValueType constraintValue );
    }

    /**
     * The operand that is being negated by this query.
     */
    protected final ValueType constraintValue;
    protected final Evaluator<ValueType> evaluator;
    protected final ValueFactory<ValueType> valueTypeFactory;
    protected final ValueFactory<String> stringFactory;
    protected final CaseOperation caseOperation;
    
    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueTypeFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link Evaluator} implementation that returns whether the node value satisfies the constraint; may not
     *        be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     */
    protected CompareQuery( final String fieldName,
                            ValueType constraintValue,
                            ValueFactory<ValueType> valueTypeFactory,
                            ValueFactory<String> stringFactory,
                            Evaluator<ValueType> evaluator,
                            CaseOperation caseOperation) {
        super(fieldName);
        this.constraintValue = constraintValue;
        this.valueTypeFactory = valueTypeFactory;
        this.stringFactory = stringFactory;
        this.caseOperation = caseOperation;
        this.evaluator = evaluator;
        assert this.constraintValue != null;
        assert this.evaluator != null;
        assert this.caseOperation != null;
        
    }

    @Override
    protected boolean isValid( Document document ) {
        String valueAsString = document.get(field());
        ValueType value = valueAsString != null ? valueTypeFactory.create(caseOperation.execute(valueAsString)) : null;
        return evaluator.satisfiesConstraint(value, constraintValue);
    }
  
    @Override
    public String toString( String field ) {
        return "(" + field() + evaluator.toString()
               + (stringFactory != null ? stringFactory.create(constraintValue) : constraintValue.toString()) + ")";
    }
}
