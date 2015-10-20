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
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.modeshape.jcr.index.lucene.query.CaseOperations.CaseOperation;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueComparators;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the name of nodes. This
 * query implementation works by using the weight and {@link Weight#scorer(LeafReaderContext)}  scorer} of the wrapped
 * query to score (and return) only those documents that correspond to nodes with Names that satisfy the constraint.
 */
public class CompareNameQuery extends CompareQuery<Name> {

    protected static final Evaluator<Name> EQUAL_TO = new Evaluator<Name>() {
        @Override
        public boolean satisfiesConstraint( Name nodeValue,
                                            Name constraintValue ) {
            return ValueComparators.NAME_COMPARATOR.compare(nodeValue, constraintValue) == 0;
        }

        @Override
        public String toString() {
            return " = ";
        }
    };
    protected static final Evaluator<Name> IS_LESS_THAN = new Evaluator<Name>() {
        @Override
        public boolean satisfiesConstraint( Name nodeValue,
                                            Name constraintValue ) {
            return ValueComparators.NAME_COMPARATOR.compare(nodeValue, constraintValue) < 0;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Name> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<Name>() {
        @Override
        public boolean satisfiesConstraint( Name nodeValue,
                                            Name constraintValue ) {
            return ValueComparators.NAME_COMPARATOR.compare(nodeValue, constraintValue) <= 0;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Name> IS_GREATER_THAN = new Evaluator<Name>() {
        @Override
        public boolean satisfiesConstraint( Name nodeValue,
                                            Name constraintValue ) {
            return ValueComparators.NAME_COMPARATOR.compare(nodeValue, constraintValue) > 0;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Name> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<Name>() {
        @Override
        public boolean satisfiesConstraint( Name nodeValue,
                                            Name constraintValue ) {
            return ValueComparators.NAME_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };
    
    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than the supplied constraint name.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the name value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static Query createQueryForNodesWithNameEqualTo( Name constraintValue,
                                                            String fieldName,
                                                            ValueFactories factories,
                                                            CaseOperation caseOperation ) {
        return new CompareNameQuery(fieldName, constraintValue, factories.getNameFactory(), factories.getStringFactory(),
                                    EQUAL_TO, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than the supplied constraint name.
     *
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThan( Name constraintValue,
                                                                           String localNameField,
                                                                           ValueFactories factories,
                                                                           CaseOperation caseOperation ) {
        return new CompareNameQuery(localNameField, constraintValue, factories.getNameFactory(),
                                    factories.getStringFactory(), IS_GREATER_THAN, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than or equal to the supplied constraint name.
     *
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThanOrEqualTo( Name constraintValue,
                                                                                    String localNameField,
                                                                                    ValueFactories factories,
                                                                                    CaseOperation caseOperation ) {
        return new CompareNameQuery(localNameField, constraintValue, factories.getNameFactory(),
                                    factories.getStringFactory(), IS_GREATER_THAN_OR_EQUAL_TO, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than the supplied constraint name.
     *
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThan( Name constraintValue,
                                                                        String localNameField,
                                                                        ValueFactories factories,
                                                                        CaseOperation caseOperation ) {
        return new CompareNameQuery(localNameField, constraintValue, factories.getNameFactory(),
                                    factories.getStringFactory(), IS_LESS_THAN, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than or equal to the supplied constraint name.
     *
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThanOrEqualTo( Name constraintValue,
                                                                                 String localNameField,
                                                                                 ValueFactories factories,
                                                                                 CaseOperation caseOperation ) {
        return new CompareNameQuery(localNameField, constraintValue, factories.getNameFactory(),
                                    factories.getStringFactory(), IS_LESS_THAN_OR_EQUAL_TO, caseOperation);
    }


    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     *
     * @param field the name of the document field containing the local name value; may not be null
     * @param constraintValue the constraint path; may not be null
     * @param nameFactory the value factory used during scoring; may not be null
     * @param evaluator the {@link Evaluator} implementation that returns whether the node path satisfies the
     * constraint; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     */
    protected CompareNameQuery( final String field,
                                Name constraintValue,
                                ValueFactory<Name> nameFactory,
                                ValueFactory<String> stringFactory,
                                Evaluator<Name> evaluator,
                                CaseOperation caseOperation ) {
        super(field, constraintValue, nameFactory, stringFactory, evaluator, caseOperation);
    }

    @Override
    public Query clone() {
        return new CompareNameQuery(field(), constraintValue, valueTypeFactory, stringFactory, evaluator, caseOperation);
    }
}
