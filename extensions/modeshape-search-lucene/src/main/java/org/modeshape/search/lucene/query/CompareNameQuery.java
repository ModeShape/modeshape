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

import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueComparators;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.model.Comparison;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the name of nodes. This
 * query implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * that correspond to nodes with Names that satisfy the constraint.
 */
public class CompareNameQuery extends CompareQuery<Path.Segment> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<Path.Segment> IS_LESS_THAN = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

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

        public boolean satisfiesConstraint( Path.Segment nodeValue,
                                            Path.Segment constraintValue ) {
            return ValueComparators.PATH_SEGMENT_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };

    protected static final Evaluator<Path.Segment> IS_LESS_THAN_NO_SNS = new Evaluator<Path.Segment>() {
        private static final long serialVersionUID = 1L;

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
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThan( Path.Segment constraintValue,
                                                                           String localNameField,
                                                                           String snsIndexFieldName,
                                                                           ValueFactories factories,
                                                                           boolean caseSensitive,
                                                                           boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_GREATER_THAN : IS_GREATER_THAN_NO_SNS, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is greater than or equal to the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameGreaterThanOrEqualTo( Path.Segment constraintValue,
                                                                                    String localNameField,
                                                                                    String snsIndexFieldName,
                                                                                    ValueFactories factories,
                                                                                    boolean caseSensitive,
                                                                                    boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_GREATER_THAN_OR_EQUAL_TO : IS_GREATER_THAN_OR_EQUAL_TO_NO_SNS, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThan( Path.Segment constraintValue,
                                                                        String localNameField,
                                                                        String snsIndexFieldName,
                                                                        ValueFactories factories,
                                                                        boolean caseSensitive,
                                                                        boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_LESS_THAN : IS_LESS_THAN_NO_SNS, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a name
     * that is less than or equal to the supplied constraint name.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param localNameField the name of the document field containing the local name value; may not be null
     * @param snsIndexFieldName the name of the document field containing the same-name-sibling index; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @param includeSns true if the SNS index should be considered, or false if the SNS value should be ignored
     * @return the query; never null
     */
    public static CompareNameQuery createQueryForNodesWithNameLessThanOrEqualTo( Path.Segment constraintValue,
                                                                                 String localNameField,
                                                                                 String snsIndexFieldName,
                                                                                 ValueFactories factories,
                                                                                 boolean caseSensitive,
                                                                                 boolean includeSns ) {
        return new CompareNameQuery(localNameField, snsIndexFieldName, constraintValue, factories.getPathFactory(),
                                    factories.getStringFactory(), factories.getLongFactory(),
                                    includeSns ? IS_LESS_THAN_OR_EQUAL_TO : IS_LESS_THAN_OR_EQUAL_TO_NO_SNS, caseSensitive);
    }

    private final String snsIndexFieldName;
    private final ValueFactory<Long> longFactory;
    private final PathFactory pathFactory;
    private final boolean caseSensitive;

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
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     */
    protected CompareNameQuery( final String localNameField,
                                final String snsIndexFieldName,
                                Path.Segment constraintValue,
                                PathFactory pathFactory,
                                ValueFactory<String> stringFactory,
                                ValueFactory<Long> longFactory,
                                Evaluator<Path.Segment> evaluator,
                                boolean caseSensitive ) {
        super(localNameField, constraintValue, null, stringFactory, evaluator, new FieldSelector() {
            private static final long serialVersionUID = 1L;

            public FieldSelectorResult accept( String fieldName ) {
                if (fieldName.equals(localNameField)) return FieldSelectorResult.LOAD;
                if (fieldName.equals(snsIndexFieldName)) return FieldSelectorResult.LOAD;
                return FieldSelectorResult.NO_LOAD;
            }
        });
        this.snsIndexFieldName = snsIndexFieldName;
        this.longFactory = longFactory;
        this.pathFactory = pathFactory;
        this.caseSensitive = caseSensitive;
        assert this.snsIndexFieldName != null;
        assert this.longFactory != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.query.CompareQuery#readFromDocument(org.apache.lucene.index.IndexReader, int)
     */
    @Override
    protected Path.Segment readFromDocument( IndexReader reader,
                                             int docId ) throws IOException {
        Document doc = reader.document(docId, fieldSelector);
        String localName = doc.get(fieldName);
        if (!caseSensitive) localName = localName.toLowerCase();
        int sns = longFactory.create(doc.get(snsIndexFieldName)).intValue();
        return pathFactory.createSegment(localName, sns);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new CompareNameQuery(fieldName, snsIndexFieldName, constraintValue, pathFactory, stringFactory, longFactory,
                                    evaluator, caseSensitive);
    }
}
