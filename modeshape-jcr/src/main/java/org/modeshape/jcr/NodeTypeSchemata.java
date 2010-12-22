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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.modeshape.graph.query.model.AllNodes;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.search.lucene.IndexRules;
import org.modeshape.search.lucene.LuceneSearchEngine;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link Schemata} implementation that is constructed from the {@link NodeType}s and {@link PropertyDefinition}s contained
 * within a {@link RepositoryNodeTypeManager}. The resulting {@link Schemata.Table}s will never change, so the
 * {@link RepositoryNodeTypeManager} must replace it's cached instance whenever the node types change.
 */
@Immutable
class NodeTypeSchemata implements Schemata {

    protected static final boolean DEFAULT_CAN_CONTAIN_REFERENCES = true;
    protected static final boolean DEFAULT_FULL_TEXT_SEARCHABLE = true;

    private final Schemata schemata;
    private final Map<Integer, String> types;
    private final Map<String, String> prefixesByUris = new HashMap<String, String>();
    private final boolean includeColumnsForInheritedProperties;
    private final boolean includePseudoColumnsInSelectStar;
    private final Collection<JcrPropertyDefinition> propertyDefinitions;
    private final Map<Name, JcrNodeType> nodeTypesByName;
    private final Multimap<JcrNodeType, JcrNodeType> subtypesByName = LinkedHashMultimap.create();
    private final IndexRules indexRules;
    private final List<JcrPropertyDefinition> pseudoProperties = new ArrayList<JcrPropertyDefinition>();

    NodeTypeSchemata( ExecutionContext context,
                      Map<Name, JcrNodeType> nodeTypes,
                      Collection<JcrPropertyDefinition> propertyDefinitions,
                      boolean includeColumnsForInheritedProperties,
                      boolean includePseudoColumnsInSelectStar ) {
        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.includePseudoColumnsInSelectStar = includePseudoColumnsInSelectStar;
        this.propertyDefinitions = propertyDefinitions;
        this.nodeTypesByName = nodeTypes;

        // Register all the namespace prefixes by URIs ...
        for (Namespace namespace : context.getNamespaceRegistry().getNamespaces()) {
            this.prefixesByUris.put(namespace.getNamespaceUri(), namespace.getPrefix());
        }

        // Identify the subtypes for each node type, and do this before we build any views ...
        for (JcrNodeType nodeType : nodeTypesByName.values()) {
            // For each of the supertypes ...
            for (JcrNodeType supertype : nodeType.getTypeAndSupertypes()) {
                subtypesByName.put(supertype, nodeType);
            }
        }

        // Build the schemata for the current node types ...
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        ImmutableSchemata.Builder builder = ImmutableSchemata.createBuilder(typeSystem);

        // Build the fast-search for type names based upon PropertyType values ...
        types = new HashMap<Integer, String>();
        types.put(PropertyType.BINARY, typeSystem.getBinaryFactory().getTypeName());
        types.put(PropertyType.BOOLEAN, typeSystem.getBooleanFactory().getTypeName());
        types.put(PropertyType.DATE, typeSystem.getDateTimeFactory().getTypeName());
        types.put(PropertyType.DECIMAL, typeSystem.getDecimalFactory().getTypeName());
        types.put(PropertyType.DOUBLE, typeSystem.getDoubleFactory().getTypeName());
        types.put(PropertyType.LONG, typeSystem.getLongFactory().getTypeName());
        types.put(PropertyType.PATH, typeSystem.getPathFactory().getTypeName());
        types.put(PropertyType.REFERENCE, typeSystem.getReferenceFactory().getTypeName());
        types.put(PropertyType.WEAKREFERENCE, typeSystem.getReferenceFactory().getTypeName());
        types.put(PropertyType.STRING, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.NAME, typeSystem.getStringFactory().getTypeName());
        types.put(PropertyType.URI, typeSystem.getStringFactory().getTypeName());

        pseudoProperties.add(pseudoProperty(context, JcrLexicon.PATH, PropertyType.PATH));
        pseudoProperties.add(pseudoProperty(context, JcrLexicon.NAME, PropertyType.NAME));
        pseudoProperties.add(pseudoProperty(context, JcrLexicon.SCORE, PropertyType.DOUBLE));
        pseudoProperties.add(pseudoProperty(context, ModeShapeLexicon.LOCALNAME, PropertyType.STRING));
        pseudoProperties.add(pseudoProperty(context, ModeShapeLexicon.DEPTH, PropertyType.LONG));

        // Create the "ALLNODES" table, which will contain all possible properties ...
        IndexRules.Builder indexRulesBuilder = IndexRules.createBuilder(LuceneSearchEngine.DEFAULT_RULES);
        indexRulesBuilder.defaultTo(Field.Store.YES,
                                    Field.Index.ANALYZED,
                                    DEFAULT_CAN_CONTAIN_REFERENCES,
                                    DEFAULT_FULL_TEXT_SEARCHABLE);
        addAllNodesTable(builder, indexRulesBuilder, context, pseudoProperties);

        // Define a view for each node type ...
        for (JcrNodeType nodeType : nodeTypesByName.values()) {
            addView(builder, context, nodeType);
        }

        schemata = builder.build();
        indexRules = indexRulesBuilder.build();
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
        Value[] defaultValues = null;
        String[] valueConstraints = new String[] {};
        String[] queryOperators = null;
        return new JcrPropertyDefinition(context, null, name, opv, autoCreated, mandatory, isProtected, defaultValues,
                                         propertyType, valueConstraints, multiple, fullTextSearchable, queryOrderable,
                                         queryOperators);
    }

    /**
     * Get the index rules ...
     * 
     * @return indexRules
     */
    public IndexRules getIndexRules() {
        return indexRules;
    }

    protected JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypesByName.get(nodeTypeName);
    }

    protected final void addAllNodesTable( ImmutableSchemata.Builder builder,
                                           IndexRules.Builder indexRuleBuilder,
                                           ExecutionContext context,
                                           List<JcrPropertyDefinition> additionalProperties ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();

        String tableName = AllNodes.ALL_NODES_NAME.name();
        boolean first = true;
        Map<String, String> typesForNames = new HashMap<String, String>();
        Set<String> fullTextSearchableNames = new HashSet<String>();
        for (JcrPropertyDefinition defn : propertyDefinitions) {
            if (defn.isResidual()) continue;
            if (defn.isPrivate()) continue;
            // if (defn.isMultiple()) continue;
            Name name = defn.getInternalName();

            String columnName = name.getString(registry);
            if (first) {
                builder.addTable(tableName, columnName);
                first = false;
            }
            boolean canBeReference = false;
            boolean isStrongReference = false;
            switch (defn.getRequiredType()) {
                case PropertyType.REFERENCE:
                    isStrongReference = true;
                    canBeReference = true;
                    break;
                case PropertyType.WEAKREFERENCE:
                case PropertyType.UNDEFINED:
                    canBeReference = true;
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
            builder.addColumn(tableName, columnName, type, fullTextSearchable, orderable, operators);

            // And build an indexing rule for this type ...
            if (indexRuleBuilder != null) addIndexRule(indexRuleBuilder,
                                                       defn,
                                                       type,
                                                       typeSystem,
                                                       canBeReference,
                                                       isStrongReference);
        }
        if (additionalProperties != null) {
            boolean canBeReference = false;
            boolean isStrongReference = false;
            boolean fullTextSearchable = false;
            assert !first;
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
                builder.addColumn(tableName, columnName, type, fullTextSearchable, orderable, operators);
                if (!includePseudoColumnsInSelectStar) {
                    builder.excludeFromSelectStar(tableName, columnName);
                }

                // And build an indexing rule for this type ...
                if (indexRuleBuilder != null) addIndexRule(indexRuleBuilder,
                                                           defn,
                                                           type,
                                                           typeSystem,
                                                           canBeReference,
                                                           isStrongReference);
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

    /**
     * Add an index rule for the given property definition and the type in the {@link TypeSystem}.
     * 
     * @param builder the index rule builder; never null
     * @param defn the property definition; never null
     * @param type the TypeSystem type, which may be a more general type than dictated by the definition, since multiple
     *        definitions with the same name require the index rule to use the common base type; never null
     * @param typeSystem the type system; never null
     * @param canBeReference true if the property described the rule can hold reference values, or false otherwise
     * @param isStrongReference true if the index rule can be a reference and it should be included in referential integrity
     *        checks
     */
    protected final void addIndexRule( IndexRules.Builder builder,
                                       JcrPropertyDefinition defn,
                                       String type,
                                       TypeSystem typeSystem,
                                       boolean canBeReference,
                                       boolean isStrongReference ) {
        Store store = Store.YES;
        Index index = defn.isFullTextSearchable() ? Index.ANALYZED : Index.NO;
        if (typeSystem.getStringFactory().getTypeName().equals(type)) {
            builder.stringField(defn.getInternalName(), store, index, canBeReference, defn.isFullTextSearchable());
        } else if (typeSystem.getDateTimeFactory().getTypeName().equals(type)) {
            Long minimum = typeSystem.getLongFactory().create(defn.getMinimumValue());
            Long maximum = typeSystem.getLongFactory().create(defn.getMaximumValue());
            builder.dateField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getLongFactory().getTypeName().equals(type)) {
            Long minimum = typeSystem.getLongFactory().create(defn.getMinimumValue());
            Long maximum = typeSystem.getLongFactory().create(defn.getMaximumValue());
            builder.longField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getDoubleFactory().getTypeName().equals(type)) {
            Double minimum = typeSystem.getDoubleFactory().create(defn.getMinimumValue());
            Double maximum = typeSystem.getDoubleFactory().create(defn.getMaximumValue());
            builder.doubleField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getBooleanFactory().getTypeName().equals(type)) {
            builder.booleanField(defn.getInternalName(), store, index);
        } else if (typeSystem.getBinaryFactory().getTypeName().equals(type)) {
            store = Store.NO;
            builder.binaryField(defn.getInternalName(), store, index, defn.isFullTextSearchable());
        } else if (typeSystem.getReferenceFactory().getTypeName().equals(type)) {
            store = Store.NO;
            builder.referenceField(defn.getInternalName(), store, index);
        } else if (typeSystem.getPathFactory().getTypeName().equals(type)) {
            builder.pathField(defn.getInternalName(), store, index);
        } else {
            // Everything else gets stored as a string ...
            builder.stringField(defn.getInternalName(), store, index, canBeReference, defn.isFullTextSearchable());
        }

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
            if (defn.isMultiple()) continue;
            if (defn.isPrivate()) continue;
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.validate.Schemata#getTable(org.modeshape.graph.query.model.SelectorName)
     */
    public Table getTable( SelectorName name ) {
        return schemata.getTable(name);
    }

    /**
     * Get a schemata instance that works with the suppplied session and that uses the session-specific namespace mappings. Note
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
        NamespaceRegistry registry = session.getExecutionContext().getNamespaceRegistry();
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
            this.context = this.session.getExecutionContext();
            this.nameFactory = context.getValueFactories().getNameFactory();
            this.builder = ImmutableSchemata.createBuilder(context.getValueFactories().getTypeSystem());
            // Add the "AllNodes" table ...
            addAllNodesTable(builder, null, context, null);
            this.schemata = builder.build();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.validate.Schemata#getTable(org.modeshape.graph.query.model.SelectorName)
         */
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
