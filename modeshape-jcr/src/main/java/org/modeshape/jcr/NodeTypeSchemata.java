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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.query.PseudoColumns;
import org.modeshape.jcr.query.model.AllNodes;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.validate.ImmutableSchemata;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.NamespaceRegistry.Namespace;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;

/**
 * A {@link Schemata} implementation that is constructed from the {@link NodeType}s and {@link PropertyDefinition}s contained
 * within a {@link RepositoryNodeTypeManager}. The resulting {@link org.modeshape.jcr.query.validate.Schemata.Table}s will never
 * change, so the {@link RepositoryNodeTypeManager} must replace it's cached instance whenever the node types change.
 */
@Immutable
public class NodeTypeSchemata implements Schemata {

    protected static final boolean DEFAULT_CAN_CONTAIN_REFERENCES = true;
    protected static final boolean DEFAULT_FULL_TEXT_SEARCHABLE = true;

    private final Schemata schemata;
    private final Map<Integer, String> types;
    private final Map<String, String> prefixesByUris = new HashMap<String, String>();
    private final boolean includeColumnsForInheritedProperties;
    private final boolean includePseudoColumnsInSelectStar;
    private final NodeTypes nodeTypes;
    private final Map<JcrNodeType, Collection<JcrNodeType>> subtypesByName = new HashMap<JcrNodeType, Collection<JcrNodeType>>();
    private final List<JcrPropertyDefinition> pseudoProperties = new ArrayList<JcrPropertyDefinition>();
    private final Name[] keyPropertyNames;

    NodeTypeSchemata( ExecutionContext context,
                      NodeTypes nodeTypes,
                      boolean includeColumnsForInheritedProperties,
                      boolean includePseudoColumnsInSelectStar ) {
        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.includePseudoColumnsInSelectStar = includePseudoColumnsInSelectStar;
        this.nodeTypes = nodeTypes;

        // Register all the namespace prefixes by URIs ...
        for (Namespace namespace : context.getNamespaceRegistry().getNamespaces()) {
            this.prefixesByUris.put(namespace.getNamespaceUri(), namespace.getPrefix());
        }

        // Identify the subtypes for each node type, and do this before we build any views ...
        for (JcrNodeType nodeType : nodeTypes.getAllNodeTypes()) {
            // For each of the supertypes ...
            for (JcrNodeType supertype : nodeType.getTypeAndSupertypes()) {
                Collection<JcrNodeType> types = subtypesByName.get(supertype);
                if (types == null) {
                    types = new LinkedList<JcrNodeType>();
                    subtypesByName.put(supertype, types);
                }
                types.add(nodeType);
            }
        }

        // Build the schemata for the current node types ...
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        ImmutableSchemata.Builder builder = ImmutableSchemata.createBuilder(context, nodeTypes);

        // Build the fast-search for type names based upon PropertyType values ...
        types = new HashMap<Integer, String>();
        types.put(PropertyType.BINARY, typeSystem.getBinaryFactory().getTypeName());
        types.put(PropertyType.BOOLEAN, typeSystem.getBooleanFactory().getTypeName());
        types.put(PropertyType.DATE, typeSystem.getDateTimeFactory().getTypeName());
        types.put(PropertyType.DECIMAL, typeSystem.getDecimalFactory().getTypeName());
        types.put(PropertyType.DOUBLE, typeSystem.getDoubleFactory().getTypeName());
        types.put(PropertyType.LONG, typeSystem.getLongFactory().getTypeName());
        types.put(PropertyType.PATH, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.REFERENCE, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.WEAKREFERENCE, typeSystem.getStringFactory().getTypeName());
        types.put(org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.STRING, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.NAME, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.URI, typeSystem.getStringFactory().getTypeName());

        // Don't include 'jcr:uuid' in all pseudocolumns, since it should only appear in 'mix:referencable' nodes ...
        for (PseudoColumns.Info pseudoColumn : PseudoColumns.allColumnsExceptJcrUuid()) {
            pseudoProperties.add(pseudoProperty(context, pseudoColumn.getQualifiedName(), pseudoColumn.getType()));
        }

        keyPropertyNames = new Name[] {JcrLexicon.UUID, ModeShapeLexicon.ID};

        // Create the "ALLNODES" table, which will contain all possible properties ...
        addAllNodesTable(builder, context, pseudoProperties, keyPropertyNames);

        // Define a view for each node type ...
        for (JcrNodeType nodeType : nodeTypes.getAllNodeTypes()) {
            addView(builder, context, nodeType);
        }

        schemata = builder.build();
    }

    protected JcrPropertyDefinition pseudoProperty( ExecutionContext context,
                                                    Name name,
                                                    int propertyType ) {
        int opv = OnParentVersionAction.IGNORE;
        boolean autoCreated = true;
        boolean mandatory = true;
        boolean isProtected = true;
        boolean multiple = false;
        boolean fullTextSearchable = false;
        boolean queryOrderable = true;
        JcrValue[] defaultValues = null;
        String[] valueConstraints = new String[] {};
        String[] queryOperators = null;
        return new JcrPropertyDefinition(context, null, null, name, opv, autoCreated, mandatory, isProtected, defaultValues,
                                         propertyType, valueConstraints, multiple, fullTextSearchable, queryOrderable,
                                         queryOperators);
    }

    protected JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypes.getNodeType(nodeTypeName);
    }

    protected final void addAllNodesTable( ImmutableSchemata.Builder builder,
                                           ExecutionContext context,
                                           List<JcrPropertyDefinition> additionalProperties,
                                           Name[] keyPropertyNames ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();

        String tableName = AllNodes.ALL_NODES_NAME.name();
        boolean first = true;
        Map<String, String> typesForNames = new HashMap<String, String>();
        Set<String> fullTextSearchableNames = new HashSet<String>();
        for (JcrPropertyDefinition defn : nodeTypes.getAllPropertyDefinitions()) {
            if (defn.isResidual()) continue;
            Name name = defn.getInternalName();

            String columnName = name.getString(registry);
            if (first) {
                builder.addTable(tableName, columnName);
                first = false;
            }
            org.modeshape.jcr.value.PropertyType requiredType = PropertyTypeUtil.modePropertyTypeFor(defn.getRequiredType());
            switch (defn.getRequiredType()) {
                case PropertyType.REFERENCE:
                    break;
                case PropertyType.WEAKREFERENCE:
                case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                case PropertyType.UNDEFINED:
                    requiredType = org.modeshape.jcr.value.PropertyType.STRING;
                    break;
            }
            String type = typeSystem.getDefaultType();
            if (defn.getRequiredType() != PropertyType.UNDEFINED) {
                type = types.get(defn.getRequiredType());
            }
            assert type != null;
            String previousType = typesForNames.put(columnName, type);
            if (previousType != null && !previousType.equals(type)) {
                // There are two property definitions with the same name but different types, so we need to find a common type ...
                type = typeSystem.getCompatibleType(previousType, type);
            }
            boolean fullTextSearchable = fullTextSearchableNames.contains(columnName) || defn.isFullTextSearchable();
            if (fullTextSearchable) fullTextSearchableNames.add(columnName);
            // Add (or overwrite) the column ...
            boolean orderable = defn.isQueryOrderable();
            Set<Operator> operators = operatorsFor(defn);
            Object minimum = defn.getMinimumValue();
            Object maximum = defn.getMaximumValue();
            builder.addColumn(tableName, columnName, type, requiredType, fullTextSearchable, orderable, minimum, maximum,
                              operators);
        }
        if (additionalProperties != null) {
            boolean fullTextSearchable = false;
            for (JcrPropertyDefinition defn : additionalProperties) {
                Name name = defn.getInternalName();
                String columnName = name.getString(registry);
                assert defn.getRequiredType() != PropertyType.UNDEFINED;
                String type = types.get(defn.getRequiredType());
                assert type != null;
                String previousType = typesForNames.put(columnName, type);
                if (previousType != null && !previousType.equals(type)) {
                    // There are two property definitions with the same name but different types, so we need to find a common type
                    // ...
                    type = typeSystem.getCompatibleType(previousType, type);
                }
                // Add (or overwrite) the column ...
                boolean orderable = defn.isQueryOrderable();
                Set<Operator> operators = operatorsFor(defn);
                Object minimum = defn.getMinimumValue();
                Object maximum = defn.getMaximumValue();
                org.modeshape.jcr.value.PropertyType requiredType = PropertyTypeUtil.modePropertyTypeFor(defn.getRequiredType());
                builder.addColumn(tableName, columnName, type, requiredType, fullTextSearchable, orderable, minimum, maximum,
                                  operators);
                if (!includePseudoColumnsInSelectStar) {
                    builder.excludeFromSelectStar(tableName, columnName);
                }
            }
        }
        if (keyPropertyNames != null) {
            StringFactory strings = context.getValueFactories().getStringFactory();
            for (Name name : keyPropertyNames) {
                // Add a key for each key property ...
                builder.addKey(tableName, strings.create(name));
            }
        }
    }

    protected Set<Operator> operatorsFor( JcrPropertyDefinition defn ) {
        String[] ops = defn.getAvailableQueryOperators();
        if (ops == null || ops.length == 0) return EnumSet.allOf(Operator.class);
        Set<Operator> result = new HashSet<Operator>();
        for (String symbol : ops) {
            Operator op = JcrPropertyDefinition.operatorFromSymbol(symbol);
            assert op != null;
            result.add(op);
        }
        return result;
    }

    protected final void addView( ImmutableSchemata.Builder builder,
                                  ExecutionContext context,
                                  JcrNodeType nodeType ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        if (!nodeType.isQueryable()) {
            // The node type is defined as not queryable, so skip it ...
            return;
        }

        String tableName = nodeType.getName();
        JcrPropertyDefinition[] defns = null;
        if (includeColumnsForInheritedProperties) {
            defns = nodeType.getPropertyDefinitions();
        } else {
            defns = nodeType.getDeclaredPropertyDefinitions();
        }
        // Create the SQL statement ...
        StringBuilder viewDefinition = new StringBuilder("SELECT ");
        boolean hasResidualProperties = false;
        boolean first = true;
        for (JcrPropertyDefinition defn : defns) {
            if (defn.isResidual()) {
                hasResidualProperties = true;
                continue;
            }
            // if (defn.isMultiple()) continue;
            Name name = defn.getInternalName();

            String columnName = name.getString(registry);
            if (first) first = false;
            else viewDefinition.append(',');
            viewDefinition.append('[').append(columnName).append(']');
            if (!defn.isQueryOrderable()) {
                builder.markOrderable(tableName, columnName, false);
            }
            builder.markOperators(tableName, columnName, operatorsFor(defn));
        }
        // Add the pseudo-properties ...
        for (JcrPropertyDefinition defn : pseudoProperties) {
            Name name = defn.getInternalName();
            String columnName = name.getString(registry);
            if (first) first = false;
            else viewDefinition.append(',');
            viewDefinition.append('[').append(columnName).append(']');
            builder.markOperators(tableName, columnName, operatorsFor(defn));
        }
        if (first) {
            // All the properties were skipped ...
            return;
        }
        viewDefinition.append(" FROM ").append(AllNodes.ALL_NODES_NAME).append(" AS [").append(tableName).append(']');

        // The 'nt:base' node type will have every single object in it, so we don't need to add the type criteria ...
        if (!JcrNtLexicon.BASE.equals(nodeType.getInternalName())) {
            // The node type is not 'nt:base', which
            viewDefinition.append(" WHERE ");

            int mixinTypeCount = 0;
            int primaryTypeCount = 0;
            StringBuilder mixinTypes = new StringBuilder();
            StringBuilder primaryTypes = new StringBuilder();
            Collection<JcrNodeType> typeAndSubtypes = subtypesByName.get(nodeType);
            for (JcrNodeType thisOrSupertype : typeAndSubtypes) {
                if (thisOrSupertype.isMixin()) {
                    if (mixinTypeCount > 0) mixinTypes.append(',');
                    assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                    String name = thisOrSupertype.getInternalName().getString(registry);
                    mixinTypes.append('[').append(name).append(']');
                    ++mixinTypeCount;
                } else {
                    if (primaryTypeCount > 0) primaryTypes.append(',');
                    assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                    String name = thisOrSupertype.getInternalName().getString(registry);
                    primaryTypes.append('[').append(name).append(']');
                    ++primaryTypeCount;
                }
            }
            if (primaryTypeCount > 0) {
                viewDefinition.append('[').append(JcrLexicon.PRIMARY_TYPE.getString(registry)).append(']');
                if (primaryTypeCount == 1) {
                    viewDefinition.append('=').append(primaryTypes);
                } else {
                    viewDefinition.append(" IN (").append(primaryTypes).append(')');
                }
            }
            if (mixinTypeCount > 0) {
                if (primaryTypeCount > 0) viewDefinition.append(" OR ");
                viewDefinition.append('[').append(JcrLexicon.MIXIN_TYPES.getString(registry)).append(']');
                if (mixinTypeCount == 1) {
                    viewDefinition.append('=').append(mixinTypes);
                } else {
                    viewDefinition.append(" IN (").append(mixinTypes).append(')');
                }
            }
        }

        // Define the view ...
        builder.addView(tableName, viewDefinition.toString());

        if (hasResidualProperties) {
            // Record that there are residual properties ...
            builder.markExtraColumns(tableName);
        }
    }

    @Override
    public Table getTable( SelectorName name ) {
        return schemata.getTable(name);
    }

    /**
     * Get a schemata instance that works with the supplied session and that uses the session-specific namespace mappings. Note
     * that the resulting instance does not change as the session's namespace mappings are changed, so when that happens the
     * JcrSession must call this method again to obtain a new schemata.
     * 
     * @param session the session; may not be null
     * @return the schemata that can be used for the session; never null
     */
    public Schemata getSchemataForSession( JcrSession session ) {
        assert session != null;
        // If the session does not override any namespace mappings used in this schemata ...
        if (!overridesNamespaceMappings(session)) {
            // Then we can just use this schemata instance ...
            return this;
        }

        // Otherwise, the session has some custom namespace mappings, so we need to return a session-specific instance...
        return new SessionSchemata(session);
    }

    /**
     * Determine if the session overrides any namespace mappings used by this schemata.
     * 
     * @param session the session; may not be null
     * @return true if the session overrides one or more namespace mappings used in this schemata, or false otherwise
     */
    private boolean overridesNamespaceMappings( JcrSession session ) {
        NamespaceRegistry registry = session.context().getNamespaceRegistry();
        if (registry instanceof LocalNamespaceRegistry) {
            Set<Namespace> localNamespaces = ((LocalNamespaceRegistry)registry).getLocalNamespaces();
            if (localNamespaces.isEmpty()) {
                // There are no local mappings ...
                return false;
            }
            for (Namespace namespace : localNamespaces) {
                if (prefixesByUris.containsKey(namespace.getNamespaceUri())) return true;
            }
            // None of the local namespace mappings overrode any namespaces used by this schemata ...
            return false;
        }
        // We can't find the local mappings, so brute-force it ...
        for (Namespace namespace : registry.getNamespaces()) {
            String expectedPrefix = prefixesByUris.get(namespace.getNamespaceUri());
            if (expectedPrefix == null) {
                // This namespace is not used by this schemata ...
                continue;
            }
            if (!namespace.getPrefix().equals(expectedPrefix)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return schemata.toString();
    }

    /**
     * Implementation class that builds the tables lazily.
     */
    @NotThreadSafe
    protected class SessionSchemata implements Schemata {
        private final JcrSession session;
        private final ExecutionContext context;
        private final ImmutableSchemata.Builder builder;
        private final NameFactory nameFactory;
        private Schemata schemata;

        protected SessionSchemata( JcrSession session ) {
            this.session = session;
            this.context = this.session.context();
            this.nameFactory = context.getValueFactories().getNameFactory();
            this.builder = ImmutableSchemata.createBuilder(context, session.nodeTypes());
            // Add the "AllNodes" table ...
            addAllNodesTable(builder, context, null, null);
            this.schemata = builder.build();
        }

        @Override
        public Table getTable( SelectorName name ) {
            Table table = schemata.getTable(name);
            if (table == null) {
                // Try getting it ...
                Name nodeTypeName = nameFactory.create(name.name());
                JcrNodeType nodeType = getNodeType(nodeTypeName);
                if (nodeType == null) return null;
                addView(builder, context, nodeType);
                schemata = builder.build();
            }
            return schemata.getTable(name);
        }
    }
}
