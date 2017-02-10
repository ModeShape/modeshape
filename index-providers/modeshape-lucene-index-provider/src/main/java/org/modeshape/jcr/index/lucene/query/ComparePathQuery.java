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

import static org.modeshape.jcr.value.ValueComparators.PATH_COMPARATOR;

import java.util.function.BiPredicate;
import java.util.function.Function;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link javax.jcr.query.qom.Comparison} constraint against the
 * Path of nodes. This query implementation works by using the weight and {@link Weight#scorer(LeafReaderContext)} 
 * scorer} of the wrapped query to score (and return) only those documents that correspond to nodes with Paths that satisfy the
 * constraint.
 */
@Immutable
public class ComparePathQuery extends CompareQuery<Path> {
    
    private final ValueFactory<Path> pathFactory;
    
    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     *
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param constraintPath the constraint path; may not be null
     * @param pathFactory the value factory that can be used during the scoring; may not be null
     * @param evaluator the {@link BiPredicate} implementation that returns whether the node path satisfies the
     * constraint; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being 
     * evaluated
     */
    protected ComparePathQuery(String fieldName,
                               Path constraintPath,
                               ValueFactory<Path> pathFactory,
                               BiPredicate<Path, Path> evaluator,
                               Function<String, String> caseOperation) {
        super(fieldName, constraintPath, evaluator, caseOperation);
        this.pathFactory = pathFactory;
    }
    
    @Override
    protected Path convertValue(String casedValue) {
        return pathFactory.create(casedValue);
    }
    
    @Override
    public Query clone() {
        return new ComparePathQuery(field(), constraintValue, pathFactory, evaluator, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is greater than the supplied constraint path.
     *
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathGreaterThan( Path constraintPath,
                                                                           String fieldName,
                                                                           ValueFactories factories,
                                                                           Function<String, String> caseOperation) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(),
                                    (path1, path2) -> PATH_COMPARATOR.compare(path1, path2) > 0, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is greater than or equal to the supplied constraint path.
     *
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathGreaterThanOrEqualTo( Path constraintPath,
                                                                                    String fieldName,
                                                                                    ValueFactories factories,
                                                                                    Function<String, String> caseOperation ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(),
                                    (path1, path2) -> PATH_COMPARATOR.compare(path1, path2) >= 0, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is less than the supplied constraint path.
     *
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathLessThan( Path constraintPath,
                                                                        String fieldName,
                                                                        ValueFactories factories,
                                                                        Function<String, String> caseOperation ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(),
                                    (path1, path2) -> PATH_COMPARATOR.compare(path1, path2) < 0, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents such that the node represented by the document has a path
     * that is less than or equal to the supplied constraint path.
     *
     * @param constraintPath the constraint path; may not be null
     * @param fieldName the name of the document field containing the path value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the path query; never null
     */
    public static ComparePathQuery createQueryForNodesWithPathLessThanOrEqualTo( Path constraintPath,
                                                                                 String fieldName,
                                                                                 ValueFactories factories,
                                                                                 Function<String, String> caseOperation ) {
        return new ComparePathQuery(fieldName, constraintPath, factories.getPathFactory(),
                                    (path1, path2) -> PATH_COMPARATOR.compare(path1, path2) <= 0, caseOperation);
    }
}
