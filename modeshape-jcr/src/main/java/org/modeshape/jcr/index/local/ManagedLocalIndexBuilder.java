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
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.NodeTypes.Supplier;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.index.local.MapDB.Serializers;
import org.modeshape.jcr.spi.index.provider.IndexChangeAdapter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueComparators;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A builder for {@link LocalIndex local indexes}.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the type of value that is indexed
 */
public abstract class ManagedLocalIndexBuilder<T> {

    /**
     * Create a builder for the supplied index definition.
     *
     * @param context the execution context in which the index should operate; may not be null
     * @param defn the index definition; may not be null
     * @param nodeTypesSupplier the supplier of the {@link NodeTypes} instance; may not be null
     * @return the index builder; never null
     */
    public static <T> ManagedLocalIndexBuilder<T> create( ExecutionContext context,
                                                          IndexDefinition defn,
                                                          Supplier nodeTypesSupplier ) {
        if (defn.hasSingleColumn()) {
            PropertyType actualPropertyType = determineActualPropertyType(defn.getColumnDefinition(0));
            return new SingleColumnIndexBuilder<T>(context, defn, nodeTypesSupplier, actualPropertyType);
        }
        throw new LocalIndexException("The local provider does not support multi-column indexes");
    }

    protected static PropertyType determineActualPropertyType( IndexColumnDefinition columnDefn ) {
        PropertyType type = PropertyType.valueFor(columnDefn.getColumnType());
        switch (type) {
            case BOOLEAN:
            case DATE:
            case DECIMAL:
            case DOUBLE:
            case LONG:
            case STRING:
            case NAME:
            case PATH:
                // These types are all usable as-is
                return type; // no conversion
            case BINARY:
            case OBJECT:
            case REFERENCE:
            case SIMPLEREFERENCE:
            case WEAKREFERENCE:
            case URI:
                // These types are all represented in the indexes as STRING
                return PropertyType.STRING;
        }
        assert false : "should never get here";
        return type;
    }

    protected final ExecutionContext context;
    protected final Serializers serializers;
    protected final Supplier nodeTypesSupplier;
    protected final IndexDefinition defn;

    protected ManagedLocalIndexBuilder( ExecutionContext context,
                                        IndexDefinition defn,
                                        Supplier nodeTypesSupplier ) {
        this.context = context;
        this.serializers = MapDB.serializers(this.context.getValueFactories());
        this.nodeTypesSupplier = nodeTypesSupplier;
        this.defn = defn;
    }

    /**
     * Build the managed index.
     *
     * @param workspaceName the name of the workspace; may not be null
     * @param localDatabase the local database; may not be null
     * @return the managed local index
     * @throws LocalIndexException if there is a problem creating the index
     */
    public abstract ManagedLocalIndex build( String workspaceName,
                                             DB localDatabase ) throws LocalIndexException;

    protected final Supplier getNodeTypesSupplier() {
        return nodeTypesSupplier;
    }

    protected final String indexName() {
        return defn.getName();
    }

    protected abstract Serializer<T> getSerializer();

    protected abstract BTreeKeySerializer<T> getBTreeKeySerializer();

    protected abstract Comparator<T> getComparator();

    protected abstract Converter<T> getConverter();

    protected abstract Class<T> getValueClass();

    protected boolean isNodeTypesIndex() {
        return false;
    }

    protected boolean isPrimaryTypeIndex() {
        return false;
    }

    protected boolean isMixinTypesIndex() {
        return false;
    }

    protected boolean isNodeNameIndex() {
        return false;
    }

    protected boolean isNodeLocalNameIndex() {
        return false;
    }

    protected boolean isNodeDepthIndex() {
        return false;
    }

    protected boolean isNodePathIndex() {
        return false;
    }

    protected boolean hasSingleColumn() {
        return defn.hasSingleColumn();
    }

    protected IndexColumnDefinition firstColumn() {
        return defn.getColumnDefinition(0);
    }

    protected final Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected final boolean matches( IndexColumnDefinition defn,
                                     Name name ) {
        return defn.getPropertyName().equals(name.getString(context.getNamespaceRegistry()));
    }

    protected final boolean matches( String actual,
                                     Name name ) {
        return actual.equals(name.getString(context.getNamespaceRegistry()));
    }

    protected final boolean isType( PropertyType propType,
                                    PropertyType expected ) {
        return propType == expected;
    }

    protected final boolean isType( PropertyType propType,
                                    PropertyType expected1,
                                    PropertyType expected2 ) {
        return propType == expected1 || propType == expected2;
    }

    protected static class SingleColumnIndexBuilder<T> extends ManagedLocalIndexBuilder<T> {
        private final IndexColumnDefinition columnDefn;
        private final PropertyType type;
        private final Serializer<T> serializer;
        private final BTreeKeySerializer<T> btreeSerializer;
        private final Comparator<T> comparator;
        private final BTreeKeySerializer<String> stringBtreeSerializer;
        private final Comparator<String> stringComparator;
        private final Class<T> clazz;
        private final Converter<T> converter;
        private final Converter<String> stringConverter;
        private final ValueFactory<T> factory;
        private final ValueFactory<String> stringFactory;

        @SuppressWarnings( "unchecked" )
        protected SingleColumnIndexBuilder( ExecutionContext context,
                                            IndexDefinition defn,
                                            Supplier nodeTypesSupplier,
                                            PropertyType actualPropertyType ) {
            super(context, defn, nodeTypesSupplier);
            assert defn.hasSingleColumn();
            columnDefn = defn.getColumnDefinition(0);
            type = actualPropertyType;
            clazz = (Class<T>)type.getValueClass();
            serializer = (Serializer<T>)serializers.serializerFor(clazz);
            comparator = (Comparator<T>)type.getComparator();
            btreeSerializer = (BTreeKeySerializer<T>)serializers.bTreeKeySerializerFor(clazz, comparator, false);
            factory = (ValueFactory<T>)this.context.getValueFactories().getValueFactory(type);
            converter = IndexValues.converter(factory);
            stringComparator = ValueComparators.STRING_COMPARATOR;
            stringFactory = this.context.getValueFactories().getStringFactory();
            stringBtreeSerializer = (BTreeKeySerializer<String>)serializers.bTreeKeySerializerFor(String.class, stringComparator,
                                                                                                  false);
            stringConverter = IndexValues.converter(stringFactory);
        }

        @Override
        protected Serializer<T> getSerializer() {
            return serializer;
        }

        @Override
        protected BTreeKeySerializer<T> getBTreeKeySerializer() {
            return btreeSerializer;
        }

        @Override
        protected Comparator<T> getComparator() {
            return comparator;
        }

        @Override
        protected Class<T> getValueClass() {
            return clazz;
        }

        protected PropertyType getColumnType() {
            return type;
        }

        @Override
        protected Converter<T> getConverter() {
            return converter;
        }

        @Override
        protected boolean isNodeTypesIndex() {
            return defn.getKind() == IndexKind.NODE_TYPE;
        }

        @Override
        protected boolean isPrimaryTypeIndex() {
            if (!matches(columnDefn, JcrLexicon.PRIMARY_TYPE)) return false;
            if (!isType(getColumnType(), PropertyType.NAME)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.NAME);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @Override
        protected boolean isMixinTypesIndex() {
            if (!matches(columnDefn, JcrLexicon.MIXIN_TYPES)) return false;
            if (!isType(getColumnType(), PropertyType.NAME)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.NAME);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @Override
        protected boolean isNodeNameIndex() {
            if (!matches(columnDefn, JcrLexicon.NAME)) return false;
            if (!isType(getColumnType(), PropertyType.NAME)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.NAME);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @Override
        protected boolean isNodeLocalNameIndex() {
            if (!matches(columnDefn, ModeShapeLexicon.LOCALNAME)) return false;
            if (!isType(getColumnType(), PropertyType.STRING)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.STRING);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @Override
        protected boolean isNodeDepthIndex() {
            if (!matches(columnDefn, ModeShapeLexicon.DEPTH)) return false;
            if (!isType(getColumnType(), PropertyType.LONG)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.LONG);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @Override
        protected boolean isNodePathIndex() {
            if (!matches(columnDefn, JcrLexicon.PATH)) return false;
            if (!isType(getColumnType(), PropertyType.PATH)) {
                String msg = JcrI18n.localIndexMustHaveOneColumnOfSpecificType.text(defn.getProviderName(), defn.getName(),
                                                                                    columnDefn.getPropertyName(), type,
                                                                                    PropertyType.PATH);
                throw new LocalIndexException(msg);
            }
            return true;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public ManagedLocalIndex build( String workspaceName,
                                        DB db ) throws LocalIndexException {
            IndexChangeAdapter changeAdapter = null;
            switch (defn.getKind()) {
                case VALUE:
                    assert !isNodeTypesIndex();
                    LocalDuplicateIndex<T> dupIndex = LocalDuplicateIndex.create(indexName(), workspaceName, db, getConverter(),
                                                                                 getSerializer(), getComparator());
                    if (isPrimaryTypeIndex()) {
                        // We know that the value type must be a name ...
                        LocalDuplicateIndex<Name> strIndex = (LocalDuplicateIndex<Name>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forPrimaryType(context, workspaceName, strIndex);
                    } else if (isMixinTypesIndex()) {
                        // We know that the value type must be a name ...
                        LocalDuplicateIndex<Name> strIndex = (LocalDuplicateIndex<Name>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forMixinTypes(context, workspaceName, strIndex);
                    } else if (isNodeNameIndex()) {
                        // We know that the value type must be a name ...
                        LocalDuplicateIndex<Name> strIndex = (LocalDuplicateIndex<Name>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forNodeName(context, workspaceName, strIndex);
                    } else if (isNodeLocalNameIndex()) {
                        // We know that the value type must be a string ...
                        LocalDuplicateIndex<String> strIndex = (LocalDuplicateIndex<String>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forNodeLocalName(context, workspaceName, strIndex);
                    } else if (isNodePathIndex()) {
                        // We know that the value type must be a path ...
                        LocalDuplicateIndex<Path> strIndex = (LocalDuplicateIndex<Path>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forNodePath(context, workspaceName, strIndex);
                    } else if (isNodeDepthIndex()) {
                        // We know that the value type must be a long ...
                        LocalDuplicateIndex<Long> strIndex = (LocalDuplicateIndex<Long>)dupIndex;
                        changeAdapter = IndexChangeAdapters.forNodeDepth(context, workspaceName, strIndex);
                    } else {
                        // This is a single type ...
                        Name propertyName = name(firstColumn().getPropertyName());
                        changeAdapter = IndexChangeAdapters.forSingleValuedProperty(context, workspaceName, propertyName,
                                                                                    factory, dupIndex);
                    }
                    return new ManagedLocalIndex(dupIndex, changeAdapter);
                case UNIQUE_VALUE:
                    assert !isNodeTypesIndex();
                    if (isPrimaryTypeIndex()) {
                        throw new LocalIndexException("Unable to create UNIQUE index '{0}'with 'jcr:primaryType' column");
                    } else if (isMixinTypesIndex()) {
                        throw new LocalIndexException("Unable to create UNIQUE index '{0}' with 'jcr:mixinTypes' column");
                    }
                    LocalUniqueIndex<T> uidx = LocalUniqueIndex.create(indexName(), workspaceName, db, getConverter(),
                                                                       getBTreeKeySerializer(), getSerializer());
                    // This is a single type ...
                    Name propertyName = name(firstColumn().getPropertyName());
                    changeAdapter = IndexChangeAdapters.forUniqueValuedProperty(context, workspaceName, propertyName, factory,
                                                                                uidx);
                    return new ManagedLocalIndex(uidx, changeAdapter);
                case ENUMERATED_VALUE:
                    assert !isNodeTypesIndex();
                    if (isPrimaryTypeIndex()) {
                        throw new LocalIndexException("Unable to create ENUMERATED index '{0}' with 'jcr:primaryType' column");
                    } else if (isMixinTypesIndex()) {
                        throw new LocalIndexException("Unable to create ENUMERATED index '{0}' with 'jcr:mixinTypes' column");
                    }

                    // We know that the value type must be a string if this is an enumerated ...
                    propertyName = name(firstColumn().getPropertyName());
                    LocalEnumeratedIndex idx = LocalEnumeratedIndex.create(defn.getName(), workspaceName, db, stringConverter,
                                                                           stringBtreeSerializer);
                    changeAdapter = IndexChangeAdapters.forSingleValuedEnumeratedProperty(context, workspaceName, propertyName,
                                                                                          idx);
                    return new ManagedLocalIndex(idx, changeAdapter);
                case NODE_TYPE:
                    // We know that the value type must be a string ...
                    idx = LocalEnumeratedIndex.create(defn.getName(), workspaceName, db, stringConverter, stringBtreeSerializer);
                    changeAdapter = IndexChangeAdapters.forNodeTypes(context, workspaceName, idx);
                    return new ManagedLocalIndex(idx, changeAdapter);
                case TEXT:
                    // This is not valid ...
                    throw new LocalIndexException("Unable to create TEXT index '{0}'");
            }
            assert false : "Should never get here";
            return null;
        }
    }
}
