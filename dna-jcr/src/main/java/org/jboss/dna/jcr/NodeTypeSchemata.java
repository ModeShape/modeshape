/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.NamespaceRegistry.Namespace;
import org.jboss.dna.graph.property.basic.LocalNamespaceRegistry;
import org.jboss.dna.graph.query.model.AllNodes;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.validate.ImmutableSchemata;
import org.jboss.dna.graph.query.validate.Schemata;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link Schemata} implementation that is constructed from the {@link NodeType}s and {@link PropertyDefinition}s contained
 * within a {@link RepositoryNodeTypeManager}. The resulting {@link Schemata.Table}s will never change, so the
 * {@link RepositoryNodeTypeManager} must replace it's cached instance whenever the node types change.
 */
@Immutable
class NodeTypeSchemata implements Schemata {

    private final Schemata schemata;
    private final Map<Integer, String> types;
    private final Map<String, String> prefixesByUris = new HashMap<String, String>();
    private final boolean includeColumnsForInheritedProperties;
    private final Iterable<JcrPropertyDefinition> propertyDefinitions;
    private final Map<Name, JcrNodeType> nodeTypesByName;
    private final Multimap<JcrNodeType, JcrNodeType> subtypesByName = LinkedHashMultimap.create();

    NodeTypeSchemata( ExecutionContext context,
                      Map<Name, JcrNodeType> nodeTypes,
                      Iterable<JcrPropertyDefinition> propertyDefinitions,
                      boolean includeColumnsForInheritedProperties ) {
        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.propertyDefinitions = propertyDefinitions;
        this.nodeTypesByName = nodeTypes;

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
        for (String typeName : typeSystem.getTypeNames()) {
            org.jboss.dna.graph.property.PropertyType dnaType = org.jboss.dna.graph.property.PropertyType.valueOf(typeName);
            int jcrType = PropertyTypeUtil.jcrPropertyTypeFor(dnaType);
            types.put(jcrType, typeName);
        }

        // Create the "ALLNODES" table, which will contain all possible properties ...
        addAllNodesTable(builder, context);

        // Define a view for each node type ...
        for (JcrNodeType nodeType : nodeTypesByName.values()) {
            addView(builder, context, nodeType);
        }

        schemata = builder.build();
    }

    protected JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypesByName.get(nodeTypeName);
    }

    private void recordName( NamespaceRegistry registry,
                             Name name ) {
        String uri = name.getNamespaceUri();
        prefixesByUris.put(uri, registry.getPrefixForNamespaceUri(uri, false));
    }

    protected final void addAllNodesTable( ImmutableSchemata.Builder builder,
                                           ExecutionContext context ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();

        String tableName = AllNodes.ALL_NODES_NAME.getName();
        boolean first = true;
        Map<String, String> typesForNames = new HashMap<String, String>();
        Set<String> fullTextSearchableNames = new HashSet<String>();
        for (JcrPropertyDefinition defn : propertyDefinitions) {
            if (defn.isResidual()) continue;
            if (defn.isMultiple()) continue;
            Name name = defn.getInternalName();
            recordName(registry, name);
            String columnName = name.getString(registry);
            if (first) {
                builder.addTable(tableName, columnName);
                first = false;
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
            builder.addColumn(tableName, columnName, type, fullTextSearchable);
        }
    }

    protected final void addView( ImmutableSchemata.Builder builder,
                                  ExecutionContext context,
                                  JcrNodeType nodeType ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        String tableName = nodeType.getName();
        JcrPropertyDefinition[] defns = null;
        if (includeColumnsForInheritedProperties) {
            defns = nodeType.getPropertyDefinitions();
        } else {
            defns = nodeType.getDeclaredPropertyDefinitions();
        }
        if (defns.length == 0) {
            // There are no properties, so there's no reason to have the view ...
            return;
        }
        // Create the SQL statement ...
        StringBuilder viewDefinition = new StringBuilder("SELECT ");
        boolean first = true;
        for (JcrPropertyDefinition defn : defns) {
            if (defn.isResidual()) continue;
            if (defn.isMultiple()) continue;
            Name name = defn.getInternalName();
            recordName(registry, name);
            String columnName = name.getString(registry);
            if (first) first = false;
            else viewDefinition.append(',');
            viewDefinition.append('[').append(columnName).append(']');
        }
        viewDefinition.append(" FROM ").append(AllNodes.ALL_NODES_NAME).append(" WHERE ");

        Collection<JcrNodeType> typeAndSubtypes = subtypesByName.get(nodeType);
        if (nodeType.isMixin()) {
            // Build the list of mixin types ...
            StringBuilder mixinTypes = null;
            for (JcrNodeType thisOrSupertype : typeAndSubtypes) {
                if (!thisOrSupertype.isMixin()) continue;
                if (mixinTypes == null) {
                    mixinTypes = new StringBuilder();
                    mixinTypes.append('[').append(JcrLexicon.MIXIN_TYPES.getString(registry)).append("] IN (");
                } else {
                    mixinTypes.append(',');
                }
                assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                String name = thisOrSupertype.getInternalName().getString(registry);
                mixinTypes.append(name);
            }
            assert mixinTypes != null; // should at least include itself
            viewDefinition.append(mixinTypes);
        } else {
            // Build the list of node type names ...
            StringBuilder primaryTypes = null;
            for (JcrNodeType thisOrSupertype : typeAndSubtypes) {
                if (thisOrSupertype.isMixin()) continue;
                if (primaryTypes == null) {
                    primaryTypes = new StringBuilder();
                    primaryTypes.append('[').append(JcrLexicon.PRIMARY_TYPE.getString(registry)).append("] IN (");
                } else {
                    primaryTypes.append(',');
                }
                assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                String name = thisOrSupertype.getInternalName().getString(registry);
                primaryTypes.append(name);
            }
            assert primaryTypes != null; // should at least include itself
            viewDefinition.append(primaryTypes);
        }

        // Define the view ...
        builder.addView(tableName, viewDefinition.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata#getTable(org.jboss.dna.graph.query.model.SelectorName)
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
            addAllNodesTable(builder, context);
            this.schemata = builder.build();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.validate.Schemata#getTable(org.jboss.dna.graph.query.model.SelectorName)
         */
        public Table getTable( SelectorName name ) {
            Table table = schemata.getTable(name);
            if (table == null) {
                // Try getting it ...
                Name nodeTypeName = nameFactory.create(name.getName());
                JcrNodeType nodeType = getNodeType(nodeTypeName);
                if (nodeType == null) return null;
                addView(builder, context, nodeType);
                schemata = builder.build();
            }
            return schemata.getTable(name);
        }
    }

}
