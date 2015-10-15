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
package org.modeshape.jcr.index.lucene;

import java.io.File;
import java.nio.file.Paths;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.DynamicOperand;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.query.qom.ChildCount;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexUsage;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;

/**
 * {@link org.modeshape.jcr.spi.index.provider.IndexProvider} implementation which uses the Apache Lucene library.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
public class LuceneIndexProvider extends IndexProvider {

    /**
     * The cost estimate for this index; this is the same as the local index atm 
     */
    private static final int COST_ESTIMATE = IndexCostCalculator.Costs.LOCAL;
    
    /**
     * The directory in which the indexes are to be stored. This can to be set, or the {@link #path} and {@link #relativeTo}
     * need to be set. If neither of those properties are present, the provider will store the indexes in RAM.
     */
    private String directory;
    
    /**
     * The path in which the indexes are to be stored, relative to {@link #relativeTo}. These can be set, or the
     * {@link #directory} can to be set.
     */
    private String path;
    
    /**
     * The directory relative to which the {@link #path} specifies where the indexes are to be stored. These can be
     * set, or {@link #directory} can be set.
     */
    private String relativeTo;

    /**
     * A number of properties which allow advanced lucene configuration. Each of them is optional and will default to Lucene
     * defaults
     */
    private String lockFactoryClass;
    private String directoryClass;
    private String analyzerClass;
    private String codecName;
    
    private LuceneConfig luceneConfig;
    
    @Override
    protected void doInitialize() throws RepositoryException {
        String baseDir = baseDir(); 
        this.luceneConfig = new LuceneConfig(baseDir, lockFactoryClass, directoryClass, analyzerClass, codecName, environment());
    }

    private String baseDir() throws RepositoryException {
        if (directory == null && relativeTo != null && path != null) {
            // Try to set the directory using relativeTo and path ...
            try {
                File rel = new File(relativeTo);
                File dir = Paths.get(rel.toURI()).resolve(path).toFile();
                directory = dir.getAbsolutePath();
            } catch (RuntimeException e) {
                throw new RepositoryException(e);
            }
        }
        if (directory == null) {
            logger().debug("The lucene index provider '{0}' for repository '{1}' will be held in memory", getName(), getRepositoryName());
            return null;
        } else {
            logger().debug("Initializing the lucene index provider '{0}' in repository '{1}' at: {2}", getName(), getRepositoryName(),
                           directory);
            return directory;
        }
    }

    @Override
    public void validateProposedIndex( ExecutionContext context, 
                                       IndexDefinition defn, 
                                       NodeTypes.Supplier nodeTypesSupplier,
                                       Problems problems ) {
        LuceneManagedIndexBuilder.validate(defn, problems);
    }

    @Override
    protected int getCostEstimate() {
        return COST_ESTIMATE;
    }

    @Override
    public Long getLatestIndexUpdateTime() {
        return luceneConfig.lastSuccessfulCommitTime();
    }

    @Override
    protected ManagedIndexBuilder getIndexBuilder( IndexDefinition defn, String workspaceName,
                                                   NodeTypes.Supplier nodeTypesSupplier,
                                                   ChangeSetAdapter.NodeTypePredicate matcher ) {
        return new LuceneManagedIndexBuilder(context(), defn, workspaceName, nodeTypesSupplier, matcher, luceneConfig);
    }

    @Override
    protected IndexUsage evaluateUsage( QueryContext context, IndexCostCalculator calculator, final IndexDefinition defn ) {
        return new IndexUsage(context, calculator, defn) {
            @Override
            protected boolean applies( ChildCount operand ) {
                // nothing to do about this...
                return false;
            }
            
            @Override
            protected boolean applies( DynamicOperand operand ) {
                if (IndexDefinition.IndexKind.TEXT == defn.getKind() && !(operand instanceof FullTextSearch)) {
                    // text indexes can ONLY apply to FTS operands...
                    return false;
                }
                return super.applies(operand);
            }
        };
    }
}
