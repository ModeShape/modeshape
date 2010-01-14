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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueComparators;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.model.Comparison;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against the Path of nodes. This
 * query implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * that correspond to nodes with Paths that satisfy the constraint.
 */
public class ComparePathQuery extends CompareQuery<Path> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<Path> PATH_IS_LESS_THAN = new Evaluator<Path>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Path nodePath,
                                            Path constraintPath ) {
            return ValueComparators.PATH_COMPARATOR.compare(nodePath, constraintPath) < 0;
        }

        @Override
        public String toString() {
            return " < ";
        }
    };
    protected static final Evaluator<Path> PATH_IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<Path>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Path nodePath,
                                            Path constraintPath ) {
            return ValueComparators.PATH_COMPARATOR.compare(nodePath, constraintPath) <= 0;
        }

        @Override
        public String toString() {
            return " <= ";
        }
    };
    protected static final Evaluator<Path> PATH_IS_GREATER_THAN = new Evaluator<Path>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Path nodePath,
                                            Path constraintPath ) {
            return ValueComparators.PATH_COMPARATOR.compare(nodePath, constraintPath) > 0;
        }

        @Override
        public String toString() {
            return " > ";
        }
    };
    protected static final Evaluator<Path> PATH_IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<Path>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( Path nodePath,
                                            Path constraintPath ) {
            return ValueComparators.PATH_COMPARATOR.compare(nodePath, constraintPath) >= 0;
        }

        @Override
        public String toString() {
            return " >= ";
        }
    };

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is greater than the supplied constraint path.
     * 
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathGreaterThan( Path constraintPath,
                                                                           String fieldName,
                                                                           ValueFactories factories,
                                                                           boolean caseSensitive ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(), factories.getStringFactory(),
                                    PATH_IS_GREATER_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is greater than or equal to the supplied constraint path.
     * 
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathGreaterThanOrEqualTo( Path constraintPath,
                                                                                    String fieldName,
                                                                                    ValueFactories factories,
                                                                                    boolean caseSensitive ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(), factories.getStringFactory(),
                                    PATH_IS_GREATER_THAN_OR_EQUAL_TO, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is less than the supplied constraint path.
     * 
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathLessThan( Path constraintPath,
                                                                        String fieldName,
                                                                        ValueFactories factories,
                                                                        boolean caseSensitive ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(), factories.getStringFactory(),
                                    PATH_IS_LESS_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is less than or equal to the supplied constraint path.
     * 
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathLessThanOrEqualTo( Path constraintPath,
                                                                                 String fieldName,
                                                                                 ValueFactories factories,
                                                                                 boolean caseSensitive ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(), factories.getStringFactory(),
                                    PATH_IS_LESS_THAN_OR_EQUAL_TO, caseSensitive);
    }

    private final boolean caseSensitive;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param constraintPath the constraint path; may not be null
     * @param pathFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     */
    protected ComparePathQuery( String fieldName,
                                Path constraintPath,
                                ValueFactory<Path> pathFactory,
                                ValueFactory<String> stringFactory,
                                Evaluator<Path> evaluator,
                                boolean caseSensitive ) {
        super(fieldName, constraintPath, pathFactory, stringFactory, evaluator);
        this.caseSensitive = caseSensitive;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.query.CompareQuery#readFromDocument(org.apache.lucene.index.IndexReader, int)
     */
    @Override
    protected Path readFromDocument( IndexReader reader,
                                     int docId ) throws IOException {
        Document doc = reader.document(docId, fieldSelector);
        String valueString = doc.get(fieldName);
        if (!caseSensitive) valueString = valueString.toLowerCase();
        return valueTypeFactory.create(valueString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new ComparePathQuery(fieldName, constraintValue, valueTypeFactory, stringFactory, evaluator, caseSensitive);
    }
}
