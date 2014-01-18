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
package org.modeshape.jcr.query.lucene;

import java.io.IOException;
import javax.jcr.query.qom.Comparison;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.modeshape.jcr.query.lucene.CaseOperations.CaseOperation;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueComparators;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the name of nodes. This
 * query implementation works by using the weight and {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped
 * query to score (and return) only those documents that correspond to nodes with Names that satisfy the constraint.
 */
public class CompareNameQuery extends CompareQuery<Path.Segment> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<Path.Segment> EQUAL_TO = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) == 0;
        }

        @Override
        public String toString() {
            return " = ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_LESS_THAN = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) < 0;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) <= 0;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_GREATER_THAN = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) > 0;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };

    protected static final Evaluator<Path.Segment> EQUAL_TO_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_NAME_COMPARATOR.compare(nodeValue, constraintValue) == 0;
        }

        @Override
        public String toString() {
            return " = ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_LESS_THAN_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_NAME_COMPARATOR.compare(nodeValue, constraintValue) < 0;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_LESS_THAN_OR_EQUAL_TO_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_NAME_COMPARATOR.compare(nodeValue, constraintValue) <= 0;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_GREATER_THAN_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_NAME_COMPARATOR.compare(nodeValue, constraintValue) > 0;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Path.Segment> IS_GREATER_THAN_OR_EQUAL_TO_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_NAME_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
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
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static Query createQueryForNodesWithNameEqualTo( Path.Segment constraintValue,
                                                            String localNameField,
                                                            String snsIndexFieldName,
                                                            ValueFactories factories,
                                                            CaseOperation caseOperation,
                                                            boolean includeSns ) {
        if (caseOperation == CaseOperations.AS_IS) {
            String nameStr = factories.getStringFactory().create(constraintValue.getName());
            // We can use a normal TermQuery for the name ...
            Query nameQuery = new TermQuery(new Term(localNameField, nameStr));
            if (includeSns) {
                // We can just do a normal BooleanQuery that uses a TermQuery and NumericQuery ...
                int snsIndex = constraintValue.getIndex();
                BooleanQuery booleanQuery = new BooleanQuery();
                booleanQuery.add(nameQuery, Occur.MUST);
                booleanQuery.add(NumericRangeQuery.newIntRange(snsIndexFieldName, snsIndex, snsIndex, true, true), Occur.MUST);
                return booleanQuery;
            }
            return nameQuery;
        }
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? EQUAL_TO : EQUAL_TO_NO_SNS, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThan( Path.Segment constraintValue,
                                                                           String localNameField,
                                                                           String snsIndexFieldName,
                                                                           ValueFactories factories,
                                                                           CaseOperation caseOperation,
                                                                           boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_GREATER_THAN : IS_GREATER_THAN_NO_SNS, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than or equal to the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThanOrEqualTo( Path.Segment constraintValue,
                                                                                    String localNameField,
                                                                                    String snsIndexFieldName,
                                                                                    ValueFactories factories,
                                                                                    CaseOperation caseOperation,
                                                                                    boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_GREATER_THAN_OR_EQUAL_TO : IS_GREATER_THAN_OR_EQUAL_TO_NO_SNS, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThan( Path.Segment constraintValue,
                                                                        String localNameField,
                                                                        String snsIndexFieldName,
                                                                        ValueFactories factories,
                                                                        CaseOperation caseOperation,
                                                                        boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_LESS_THAN : IS_LESS_THAN_NO_SNS, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than or equal to the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThanOrEqualTo( Path.Segment constraintValue,
                                                                                 String localNameField,
                                                                                 String snsIndexFieldName,
                                                                                 ValueFactories factories,
                                                                                 CaseOperation caseOperation,
                                                                                 boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_LESS_THAN_OR_EQUAL_TO : IS_LESS_THAN_OR_EQUAL_TO_NO_SNS, caseOperation);
    }

    private final String snsIndexFieldName;
    private final ValueFactory<Long> longFactory;
    private final PathFactory pathFactory;
    private final CaseOperation caseOperation;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param constraintValue the constraint path; may not be null
     * @param pathFactory the path factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param longFactory the long factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     */
    protected CompareNameQuery( final String localNameField,
                                final String snsIndexFieldName,
                                Path.Segment constraintValue,
                                PathFactory pathFactory,
                                ValueFactory<String> stringFactory,
                                ValueFactory<Long> longFactory,
                                Evaluator<Path.Segment> evaluator,
                                CaseOperation caseOperation ) {
        super(localNameField, constraintValue, null, stringFactory, evaluator, new FieldSelector() {
            private static final long serialVersionUID = 1L;

            @Override
            public FieldSelectorResult accept( String fieldName ) {
                if (fieldName.equals(localNameField)) return FieldSelectorResult.LOAD;
                if (fieldName.equals(snsIndexFieldName)) return FieldSelectorResult.LOAD;
                return FieldSelectorResult.NO_LOAD;
            }
        });
        this.snsIndexFieldName = snsIndexFieldName;
        this.longFactory = longFactory;
        this.pathFactory = pathFactory;
        this.caseOperation = caseOperation;
        assert this.snsIndexFieldName != null;
        assert this.longFactory != null;
    }

    @Override
    protected Path.Segment readFromDocument( IndexReader reader,
                                             int docId ) throws IOException {
        Document doc = reader.document(docId, fieldSelector);
        String localName = doc.get(fieldName);
        if (localName == null) return null;
        localName = caseOperation.execute(localName);
        int sns = longFactory.create(doc.get(snsIndexFieldName)).intValue();
        return pathFactory.createSegment(localName, sns);
    }

    @Override
    public Object clone() {
        return new CompareNameQuery(fieldName, snsIndexFieldName, constraintValue, pathFactory, stringFactory, longFactory,
                                    evaluator, caseOperation);
    }
}
