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
package org.modeshape.jcr.spi.index.provider;

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Class responsible for building {@link ManagedIndex} instances and providing them to the repository. Index providers may either 
 * choose to ignore class and use their own logic for creating/updating managed indexes or may extend this and simply provide
 * the mechanism for building the 5 different index types:
 * <ul>
 *     <li>{@link #buildMultiValueIndex(ExecutionContext, IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate)}</li>
 *     <li>{@link #buildUniqueValueIndex(ExecutionContext, IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate)}</li>
 *     <li>{@link #buildEnumeratedIndex(ExecutionContext, IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate)}</li>
 *     <li>{@link #buildNodeTypeIndex(ExecutionContext, IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate)}</li>
 *     <li>{@link #buildTextIndex(ExecutionContext, IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate)}</li>
 * </ul>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @see IndexProvider#createIndex(IndexDefinition, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate, IndexFeedback) 
 * @see IndexProvider#updateIndex(IndexDefinition, IndexDefinition, ManagedIndex, String, NodeTypes.Supplier, ChangeSetAdapter.NodeTypePredicate, IndexFeedback)
 * 
 * @since 4.5
 */
@Immutable
public abstract class ManagedIndexBuilder {

    protected final ExecutionContext context;
    protected final IndexDefinition defn;
    protected final NodeTypes.Supplier nodeTypesSupplier;
    protected final ChangeSetAdapter.NodeTypePredicate matcher;
    protected final String workspaceName;

    protected ManagedIndexBuilder( ExecutionContext context,
                                   IndexDefinition defn,
                                   String workspaceName,
                                   NodeTypes.Supplier nodeTypesSupplier,
                                   ChangeSetAdapter.NodeTypePredicate matcher ) {
        this.context = context;
        this.workspaceName = workspaceName;
        this.defn = defn;
        this.nodeTypesSupplier = nodeTypesSupplier;
        this.matcher = matcher;
    }

     /**
     * Creates a new managed index which wraps a provider-specific index plus a change adapter.
     *
     * @return a {@link ManagedIndex} instance, never {@code null}
      * *@see ProvidedIndexDelegate#wrap(ProvidedIndex, IndexChangeAdapter) 
     */
    @SuppressWarnings("unchecked")
    public ManagedIndex build() {
        List<IndexChangeAdapter> changeAdapters = new ArrayList<>();
        ProvidedIndex<?> index = null;
        switch (defn.getKind()) {
            case VALUE:
                index = buildMultiValueIndex(context, defn, workspaceName, nodeTypesSupplier, matcher);
                for (int i = 0; i < defn.size(); i++) {
                    IndexColumnDefinition columnDef = defn.getColumnDefinition(i);
                    PropertyType type = determineActualPropertyType(columnDef);
                    if (isPrimaryTypeIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forPrimaryType(context, matcher, workspaceName, index));
                    } else if (isMixinTypesIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forMixinTypes(context, matcher, workspaceName, index));
                    } else if (isNodeNameIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forNodeName(context, matcher, workspaceName, index));
                    } else if (isNodeLocalNameIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forNodeLocalName(context, matcher, workspaceName, index));
                    } else if (isNodePathIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forNodePath(context, matcher, workspaceName, index));
                    } else if (isNodeDepthIndex(columnDef, type)) {
                        changeAdapters.add(IndexChangeAdapters.forNodeDepth(context, matcher, workspaceName, index));
                    } else {
                        // This is a generic index for a property ...
                        Name propertyName = name(columnDef.getPropertyName());
                        assert propertyName != null;

                        ValueFactory<?> valueFactory = context.getValueFactories().getValueFactory(type);
                        changeAdapters.add(IndexChangeAdapters.forProperty(context, matcher, workspaceName, 
                                                                           propertyName, valueFactory, index));
                    }
                } 
                break;
            case UNIQUE_VALUE: 
                index = buildUniqueValueIndex(context, defn, workspaceName, nodeTypesSupplier, matcher);
                for (int i = 0; i < defn.size(); i++) {
                    IndexColumnDefinition columnDef = defn.getColumnDefinition(i);
                    PropertyType type = determineActualPropertyType(columnDef);
                    Name propertyName = name(columnDef.getPropertyName());
                    assert propertyName != null;
                    ValueFactory<?> valueFactory = context.getValueFactories().getValueFactory(type);
                    changeAdapters.add(IndexChangeAdapters.forUniqueValuedProperty(context, matcher, workspaceName,
                                                                                   propertyName,
                                                                                   valueFactory, index));
                }
                break;
            case ENUMERATED_VALUE:
                index = buildEnumeratedIndex(context, defn, workspaceName, nodeTypesSupplier, matcher);
                for (int i = 0; i < defn.size(); i++) {
                    IndexColumnDefinition columnDef = defn.getColumnDefinition(i);
                    Name propertyName = name(columnDef.getPropertyName());
                    assert propertyName != null;
                    changeAdapters.add(IndexChangeAdapters.forEnumeratedProperty(context, matcher, workspaceName,
                                                                                 propertyName, index));
                }
                break;
            case NODE_TYPE: 
                index = buildNodeTypeIndex(context, defn, workspaceName, nodeTypesSupplier, matcher);
                if (defn.size() > 1) {
                    throw new IllegalArgumentException("Cannot have a multi column node-type index");
                }
                String property = defn.getColumnDefinition(0).getPropertyName();
                if (property == null) {
                    property = defn.getName();
                }
                changeAdapters.add(IndexChangeAdapters.forNodeTypes(property, context, matcher, workspaceName, index));
                break;
            case TEXT: 
                index = buildTextIndex(context, defn, workspaceName, nodeTypesSupplier, matcher);
                ValueFactory<String> valueFactory = (ValueFactory<String>)context.getValueFactories().getValueFactory(PropertyType.STRING);
                for (int i = 0; i < defn.size(); i++) {
                    IndexColumnDefinition columnDef = defn.getColumnDefinition(i);
                    PropertyType type = determineActualPropertyType(columnDef);
                    Name propertyName = name(columnDef.getPropertyName());
                    assert propertyName != null;
                    if (isNodeNameIndex(columnDef, type)) {
                        // FTS on jcr:name
                        changeAdapters.add(IndexChangeAdapters.forNodeName(context, matcher, workspaceName, index));
                    } else if (isNodeLocalNameIndex(columnDef, type)) {
                        // FTS on mode:localName
                        changeAdapters.add(IndexChangeAdapters.forNodeLocalName(context, matcher, workspaceName, index));
                    } else if (isNodePathIndex(columnDef, type)) {
                        // FTS on jcr:path
                        changeAdapters.add(IndexChangeAdapters.forNodePath(context, matcher, workspaceName, index));
                    } else {
                        // default to a property....
                        changeAdapters.add(IndexChangeAdapters.forTextProperty(context, matcher, workspaceName,
                                                                               propertyName, valueFactory, index));
                    }
                }
                break;
            default: {
                throw new IllegalArgumentException("Unexpected index kind on: " + defn);
            }
        }
        
        assert !changeAdapters.isEmpty();
        IndexChangeAdapter adapter = changeAdapters.size() == 1 ? 
                                     changeAdapters.get(0) :
                                     IndexChangeAdapters.forMultipleColumns(context, matcher, workspaceName, index,
                                                                            changeAdapters);
        return new DefaultManagedIndex(index, adapter);
    }

    protected boolean isPrimaryTypeIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, JcrLexicon.PRIMARY_TYPE) && isType(type, PropertyType.NAME);
    }

    protected boolean isMixinTypesIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, JcrLexicon.MIXIN_TYPES) && isType(type, PropertyType.NAME);
    }

    protected boolean isNodeNameIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, JcrLexicon.NAME) && isType(type, PropertyType.NAME);
    }

    protected boolean isNodeLocalNameIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, ModeShapeLexicon.LOCALNAME) && isType(type, PropertyType.STRING);
    }

    protected boolean isNodeDepthIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, ModeShapeLexicon.DEPTH) && isType(type, PropertyType.LONG);
    }

    protected boolean isNodePathIndex( IndexColumnDefinition columnDefn, PropertyType type ) {
        return matches(columnDefn, JcrLexicon.PATH) && isType(type, PropertyType.PATH);
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

    protected abstract ProvidedIndex<?> buildMultiValueIndex( ExecutionContext context,
                                                              IndexDefinition defn,
                                                              String workspaceName,
                                                              NodeTypes.Supplier nodeTypesSupplier,
                                                              ChangeSetAdapter.NodeTypePredicate matcher );

    protected abstract ProvidedIndex<?> buildUniqueValueIndex( ExecutionContext context,
                                                               IndexDefinition defn,
                                                               String workspaceName,
                                                               NodeTypes.Supplier nodeTypesSupplier,
                                                               ChangeSetAdapter.NodeTypePredicate matcher );

    protected abstract ProvidedIndex<?> buildEnumeratedIndex( ExecutionContext context,
                                                              IndexDefinition defn,
                                                              String workspaceName,
                                                              NodeTypes.Supplier nodeTypesSupplier,
                                                              ChangeSetAdapter.NodeTypePredicate matcher );

    protected abstract ProvidedIndex<?> buildTextIndex( ExecutionContext context,
                                                        IndexDefinition defn,
                                                        String workspaceName,
                                                        NodeTypes.Supplier nodeTypesSupplier,
                                                        ChangeSetAdapter.NodeTypePredicate matcher );

    protected abstract ProvidedIndex<?> buildNodeTypeIndex( ExecutionContext context,
                                                            IndexDefinition defn,
                                                            String workspaceName,
                                                            NodeTypes.Supplier nodeTypesSupplier,
                                                            ChangeSetAdapter.NodeTypePredicate matcher );
}
