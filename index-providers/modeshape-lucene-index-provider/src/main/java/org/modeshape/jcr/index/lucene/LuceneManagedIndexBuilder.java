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

import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;
import org.modeshape.jcr.value.PropertyType;

/**
 * {@link ManagedIndexBuilder} extension which builds different types of Lucene indexes.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
public class LuceneManagedIndexBuilder extends ManagedIndexBuilder {
    
    private final LuceneConfig luceneConfig;

    protected LuceneManagedIndexBuilder( ExecutionContext context,
                                         IndexDefinition defn,
                                         String workspaceName,
                                         NodeTypes.Supplier nodeTypesSupplier,
                                         ChangeSetAdapter.NodeTypePredicate matcher,
                                         LuceneConfig luceneConfig ) {
        super(context, defn, workspaceName, nodeTypesSupplier, matcher);
        assert luceneConfig != null;
        this.luceneConfig = luceneConfig;
    }
    
    private Map<String, PropertyType> propertyTypesByName(IndexDefinition defn) {
        int size = defn.size();
        Map<String, PropertyType> result = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            IndexColumnDefinition columnDef = defn.getColumnDefinition(i);
            PropertyType propertyType = PropertyType.valueFor(columnDef.getColumnType());
            String propertyName = columnDef.getPropertyName();
            if (isPrimaryTypeIndex(columnDef, propertyType) || 
                isMixinTypesIndex(columnDef, propertyType) ||
                isNodeNameIndex(columnDef, propertyType)) {
                // we know all these will be send by the adapters as names...
                result.put(propertyName, PropertyType.NAME);
            } else if (isNodeLocalNameIndex(columnDef, propertyType)) {
                // we know this will be a string...
                result.put(propertyName, PropertyType.STRING);
            } else if (isNodeDepthIndex(columnDef, propertyType)) {
                // we know this will be a long
                result.put(propertyName, PropertyType.LONG);
            } else if (isNodePathIndex(columnDef, propertyType)) {
                // we know this will be a path
                result.put(propertyName, PropertyType.PATH);
            } else {
                // just add as is
                result.put(propertyName, propertyType);
            }             
        }    
        return result;
    }
    
    protected static void validate(IndexDefinition definition, Problems problems) {
        IndexDefinition.IndexKind kind = definition.getKind();
        boolean isTextIndex = kind == IndexDefinition.IndexKind.TEXT;
        if (isTextIndex && definition.size() > 1){
            problems.addError(LuceneIndexProviderI18n.multiColumnTextIndexesNotSupported, definition.getName());    
        }
        for (int i = 0; i < definition.size(); i++) {
            IndexColumnDefinition columnDef = definition.getColumnDefinition(i);
            PropertyType propertyType = PropertyType.valueFor(columnDef.getColumnType());
            switch (propertyType) {
                case OBJECT: {
                    problems.addError(LuceneIndexProviderI18n.invalidColumnType, propertyType, columnDef.getPropertyName(),
                                      definition.getName());
                    continue;                    
                }
                case BINARY: {
                    if (!isTextIndex) {
                        problems.addError(LuceneIndexProviderI18n.invalidColumnType, propertyType, columnDef.getPropertyName(),
                                          definition.getName());

                    }
                }
            }
        }
    }

    @Override
    protected ProvidedIndex<?> buildMultiValueIndex( ExecutionContext context, 
                                                     IndexDefinition defn, 
                                                     String workspaceName,
                                                     NodeTypes.Supplier nodeTypesSupplier,
                                                     ChangeSetAdapter.NodeTypePredicate matcher ) {
        validate(defn);
        return defn.size() > 1 ? 
               new MultiColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context) :
               new SingleColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context);
    }
    
    @Override
    protected ProvidedIndex<?> buildUniqueValueIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                      NodeTypes.Supplier nodeTypesSupplier,
                                                      ChangeSetAdapter.NodeTypePredicate matcher ) {
        validate(defn);
        return defn.size() > 1 ?
               new MultiColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context) :
               new SingleColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context);
    }

    @Override
    protected ProvidedIndex<?> buildEnumeratedIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                     NodeTypes.Supplier nodeTypesSupplier,
                                                     ChangeSetAdapter.NodeTypePredicate matcher ) {
        validate(defn);
        return defn.size() > 1 ?
               new MultiColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context) :
               new SingleColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context);
    }

    @Override
    protected ProvidedIndex<?> buildTextIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                               NodeTypes.Supplier nodeTypesSupplier,
                                               ChangeSetAdapter.NodeTypePredicate matcher ) {
        validate(defn);
        return new TextIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context);
    }

    @Override
    protected ProvidedIndex<?> buildNodeTypeIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                   NodeTypes.Supplier nodeTypesSupplier,
                                                   ChangeSetAdapter.NodeTypePredicate matcher ) {
        assert defn.size() == 1; //should've been validated by the super class
        return new SingleColumnIndex(defn.getName(), workspaceName, luceneConfig, propertyTypesByName(defn), context);
    }

    private void validate( IndexDefinition defn ) {
        SimpleProblems problems = new SimpleProblems();
        validate(defn, problems);
        if (problems.hasErrors()) {
            throw new LuceneIndexException(problems.toString());
        }
    }
}
