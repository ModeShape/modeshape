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

package org.modeshape.jcr.index.local;

import java.util.Comparator;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.NodeTypes.Supplier;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.index.local.MapDB.Serializers;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueComparators;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A builder for {@link LocalIndex local indexes}.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @param <T> the type of value that is indexed
 */
public abstract class LocalIndexBuilder<T> extends ManagedIndexBuilder {

    /**
     * Create a builder for the supplied index definition.
     *
     * @param context the execution context in which the index should operate; may not be null
     * @param defn the index definition; may not be null
     * @param nodeTypesSupplier the supplier of the {@link NodeTypes} instance; may not be null
     * @param workspaceName the name of the workspace for which to build the index; may not be null
     * @param matcher the node type matcher used to determine which nodes should be included in the index; may not be null
     * @param db the MapDB DB instance; may not be null
     * @return the index builder; never null
     */
    public static <T> LocalIndexBuilder<T> create( ExecutionContext context,
                                                   IndexDefinition defn,
                                                   Supplier nodeTypesSupplier,
                                                   String workspaceName,
                                                   NodeTypePredicate matcher,
                                                   DB db) {
        SimpleProblems problems = new SimpleProblems();
        validate(defn, problems);
        if (problems.hasErrors()) {
            throw new LocalIndexException(problems.toString());
        }
        PropertyType actualPropertyType = determineActualPropertyType(defn.getColumnDefinition(0));
        return new SingleColumnIndexBuilder<>(context, defn, nodeTypesSupplier,workspaceName, matcher, actualPropertyType, db);
    }

    protected final Serializers serializers;
    
    protected LocalIndexBuilder( ExecutionContext context,
                                 IndexDefinition defn,
                                 Supplier nodeTypesSupplier,
                                 String workspaceName, 
                                 NodeTypePredicate matcher ) {
        super(context, defn,  workspaceName, nodeTypesSupplier, matcher);
        this.serializers = MapDB.serializers(this.context.getValueFactories());
    }
    
    /**
     * Validate whether the index definition is acceptable for this provider.
     *
     * @param defn the definition to validate; may not be {@code null}
     * @param problems the component to record any problems, errors, or warnings; may not be null
     */
    protected static void validate( IndexDefinition defn, Problems problems ) {
        if (!defn.hasSingleColumn()) {
            problems.addError(JcrI18n.localIndexProviderDoesNotSupportMultiColumnIndexes, defn.getName(), defn.getProviderName());    
        }
        switch (defn.getKind()) {
            case TEXT:
                // This is not valid ...
                problems.addError(JcrI18n.localIndexProviderDoesNotSupportTextIndexes, defn.getName(),defn.getProviderName());        
        }
    }

    protected final String indexName() {
        return defn.getName();
    }

    protected abstract Serializer<T> getSerializer();

    protected abstract Comparator<T> getComparator();

    protected static class SingleColumnIndexBuilder<T> extends LocalIndexBuilder<T> {
        private final PropertyType type;
        private final Serializer<T> serializer;
        private final BTreeKeySerializer<T> btreeKeySerializer;
        private final Comparator<T> comparator;
        private final BTreeKeySerializer<String> stringBtreeSerializer;
        private final Comparator<String> stringComparator;
        private final Class<T> clazz;
        private final Converter<T> converter;
        private final Converter<String> stringConverter;
        private final ValueFactory<T> factory;
        private final ValueFactory<String> stringFactory;
        private final DB db;

        @SuppressWarnings( "unchecked" )
        protected SingleColumnIndexBuilder( ExecutionContext context,
                                            IndexDefinition defn,
                                            Supplier nodeTypesSupplier,
                                            String workspaceName,
                                            NodeTypePredicate matcher,
                                            PropertyType actualPropertyType, 
                                            DB db ) {
            super(context, defn, nodeTypesSupplier, workspaceName, matcher);
            assert defn.hasSingleColumn();
            type = actualPropertyType;
            clazz = (Class<T>)type.getValueClass();
            serializer = (Serializer<T>)serializers.serializerFor(clazz);
            comparator = (Comparator<T>)type.getComparator();
            btreeKeySerializer = (BTreeKeySerializer<T>)serializers.bTreeKeySerializerFor(clazz, comparator, false);
            factory = (ValueFactory<T>)this.context.getValueFactories().getValueFactory(type);
            converter = IndexValues.converter(factory);
            stringComparator = ValueComparators.STRING_COMPARATOR;
            stringFactory = this.context.getValueFactories().getStringFactory();
            stringBtreeSerializer = (BTreeKeySerializer<String>)serializers.bTreeKeySerializerFor(String.class, stringComparator,
                                                                                                  false);
            stringConverter = IndexValues.converter(stringFactory);
            this.db = db;
        }

        @Override
        protected Serializer<T> getSerializer() {
            return serializer;
        }
        
        @Override
        protected Comparator<T> getComparator() {
            return comparator;
        }

        protected PropertyType getColumnType() {
            return type;
        }

        protected Converter<T> getConverter() {
            return converter;
        }

        protected BTreeKeySerializer<T> getBTreeKeySerializer() {
            return btreeKeySerializer;
        }

        @Override
        protected ProvidedIndex<?> buildMultiValueIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                         Supplier nodeTypesSupplier,
                                                         NodeTypePredicate matcher ) {
            return LocalDuplicateIndex.create(indexName(), workspaceName, db, getConverter(), getSerializer(), getComparator());
        }

        @Override
        protected ProvidedIndex<?> buildUniqueValueIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                          Supplier nodeTypesSupplier,
                                                          NodeTypePredicate matcher ) {
            return LocalUniqueIndex.create(indexName(), workspaceName, db, getConverter(), getBTreeKeySerializer(), getSerializer());
        }

        @Override
        protected ProvidedIndex<?> buildEnumeratedIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                         Supplier nodeTypesSupplier,
                                                         NodeTypePredicate matcher ) {
            return LocalEnumeratedIndex.create(defn.getName(), workspaceName, db, stringConverter, stringBtreeSerializer);
        }

        @Override
        protected ProvidedIndex<?> buildTextIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                   Supplier nodeTypesSupplier,
                                                   NodeTypePredicate matcher ) {
            throw new UnsupportedOperationException("should not ever see this because validation should prevent such indexes from being used");
        }

        @Override
        protected ProvidedIndex<?> buildNodeTypeIndex( ExecutionContext context, IndexDefinition defn, String workspaceName,
                                                       Supplier nodeTypesSupplier,
                                                       NodeTypePredicate matcher ) {
            return LocalEnumeratedIndex.create(defn.getName(), workspaceName, db, stringConverter, stringBtreeSerializer);
        }
    }
}
