/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * A utility class that maintains a quick-lookup cache of all the child node definitions and property definitions for a series of
 * node types. This class is used with {@link JcrNodeType} to represent these definitions for defined on the node type as well as
 * those inherited from supertypes. However, it also can be used as a quick-lookup cache for a node's primary type and mixin
 * types.
 */
@Immutable
final class DefinitionCache {

    /**
     * A local cache of all defined and inherited child node definitions, keyed by their name, that allow same-name-sibilings.
     * This includes residual child node definitions, which are keyed by the {@link JcrNodeType#RESIDUAL_NAME}. The content of
     * this map is used frequently to find the most appropriate node definition for children, and so it is computed once at
     * construction time (recall that all definitions, including the supertypes, are immutable).
     * <p>
     * Note that a node type may have multiple child node definitions with the same name,
     * "as long as they are distinguishable by the required primary types attribute." (Section 4.7.15 of the JSR-283 draft
     * specification). The order of the node definitions stored for each name is such that this node's definitions are stored
     * first, followed by those of the immediate supertypes, followed by those from those supertypes' supertypes, etc.
     * </p>
     */
    private final Multimap<Name, JcrNodeDefinition> childNodeDefinitionsThatAllowSns = LinkedListMultimap.create();
    /**
     * A local cache of all defined and inherited child node definitions, keyed by their name, that do <i>not</i> allow
     * same-name-sibilings. This includes residual child node definitions, which are keyed by the
     * {@link JcrNodeType#RESIDUAL_NAME}. The content of this map is used requently to find the most appropriate node definition
     * for children, and so it is computed once at construction time (recall that all definitions, including the supertypes, are
     * immutable).
     * <p>
     * Note that a node type may have multiple child node definitions with the same name,
     * "as long as they are distinguishable by the required primary types attribute." (Section 4.7.15 of the JSR-283 draft
     * specification). The order of the node definitions stored for each name is such that this node's definitions are stored
     * first, followed by those of the immediate supertypes, followed by those from those supertypes' supertypes, etc.
     * </p>
     */
    private final Multimap<Name, JcrNodeDefinition> childNodeDefinitionsThatAllowNoSns = LinkedListMultimap.create();

    /**
     * A local cache of all defined and inherited property definitions, keyed by their name, that allow multiple values. This
     * includes residual property definitions, which are keyed by the {@link JcrNodeType#RESIDUAL_NAME}. The content of this map
     * is used frequently to find the most appropriate property definition for properties of nodes that use this node type, and so
     * it is computed once at construction time (recall that all definitions, including supertypes, are immutable).
     * <p>
     * Note that a node type may have multiple property node definitions with the same name, "as long as the definitions are
     * otherwise distinguishable by either the required type attribute (the value returned by PropertyDefinition.getRequiredType)
     * or the multiple attribute" (Section 4.7.15 of the JSR-283 draft specification).
     * </p>
     */
    private final Multimap<Name, JcrPropertyDefinition> multiValuedPropertyDefinitions = LinkedListMultimap.create();
    /**
     * A local cache of all defined and inherited property definitions, keyed by their name, that allow single values. This
     * includes residual property definitions, which are keyed by the {@link JcrNodeType#RESIDUAL_NAME}. The content of this map
     * is used frequently to find the most appropriate property definition for properties of nodes that use this node type, and so
     * it is computed once at construction time (recall that all definitions, including supertypes, are immutable).
     * <p>
     * Note that a node type may have multiple property node definitions with the same name, "as long as the definitions are
     * otherwise distinguishable by either the required type attribute (the value returned by PropertyDefinition.getRequiredType)
     * or the multiple attribute" (Section 4.7.15 of the JSR-283 draft specification).
     * </p>
     */
    private final Multimap<Name, JcrPropertyDefinition> singleValuedPropertyDefinitions = LinkedListMultimap.create();

    private final Multimap<Name, JcrNodeDefinition> allChildNodeDefinitions = LinkedListMultimap.create();
    private final Multimap<Name, JcrPropertyDefinition> allPropertyDefinitions = LinkedListMultimap.create();

    DefinitionCache( JcrNodeType nodeType ) {
        addDefinitionsForTypeAndAllSupertypes(nodeType);
    }

    DefinitionCache( JcrNodeType primaryType,
                     Iterable<JcrNodeType> mixinTypes ) {
        addDefinitionsForTypeAndAllSupertypes(primaryType);
        // Assumption is that addMixin won't allow mixin types that conflict with primary types
        if (mixinTypes != null) {
            for (JcrNodeType mixinType : mixinTypes) {
                addDefinitionsForTypeAndAllSupertypes(mixinType);
            }
        }
    }

    private final void addDefinitionsForTypeAndAllSupertypes( JcrNodeType nodeType ) {
        // And the definitions from the type and all supertypes ...
        for (NodeType superSuperType : nodeType.getTypeAndSupertypes()) {
            addDefinitions((JcrNodeType)superSuperType);
        }
    }

    private final void addDefinitions( JcrNodeType nodeType ) {
        Set<Name> namesFromThisType = new HashSet<Name>();

        for (JcrNodeDefinition definition : nodeType.childNodeDefinitions()) {
            Name name = definition.getInternalName();

            /*
             * If the child node was already defined in the type hierarchy at some other level, ignore the definition 
             * - it was overridden by the previous definition.  This relies on the fact that TypeA.getTypeAndSupertypes()
             * always returns TypeX before TypeY if TypeX is closer to TypeA on the inheritance graph than TypeY is...
             * 
             * ...UNLESS this is a residual definition, in which case side-by-side definitions must be allowed per 6.7.8 
             * of the 1.0.1 specification.
             */
            if (allChildNodeDefinitions.containsKey(name) && !namesFromThisType.contains(name)
                && !JcrNodeType.RESIDUAL_NAME.equals(name)) {
                continue;
            }

            if (definition.allowsSameNameSiblings()) {
                childNodeDefinitionsThatAllowSns.put(name, definition);
            } else {
                childNodeDefinitionsThatAllowNoSns.put(name, definition);
            }
            allChildNodeDefinitions.put(name, definition);
            namesFromThisType.add(name);
        }

        namesFromThisType.clear();
        for (JcrPropertyDefinition definition : nodeType.propertyDefinitions()) {
            Name name = definition.getInternalName();

            /*
             * If the property was already defined in the type hierarchy at some other level, ignore the definition 
             * - it was overridden by the previous definition.  This relies on the fact that TypeA.getTypeAndSupertypes()
             * always returns TypeX before TypeY if TypeX is closer to TypeA on the inheritance graph than TypeY is...
             * 
             * ...UNLESS this is a residual definition, in which case side-by-side definitions must be allowed per 6.7.8 
             * of the 1.0.1 specification.
             */
            if (allPropertyDefinitions.containsKey(name) && !namesFromThisType.contains(name)
                && !JcrNodeType.RESIDUAL_NAME.equals(name)) {
                continue;
            }
            if (definition.isMultiple()) {
                multiValuedPropertyDefinitions.put(name, definition);
            } else {
                singleValuedPropertyDefinitions.put(name, definition);
            }
            namesFromThisType.add(name);
            allPropertyDefinitions.put(name, definition);
        }
    }

    public Collection<JcrPropertyDefinition> allSingleValuePropertyDefinitions( Name propertyName ) {
        return this.singleValuedPropertyDefinitions.get(propertyName);
    }

    public Collection<JcrPropertyDefinition> allMultiValuePropertyDefinitions( Name propertyName ) {
        return this.multiValuedPropertyDefinitions.get(propertyName);
    }

    public Collection<JcrPropertyDefinition> allPropertyDefinitions( Name propertyName ) {
        return this.allPropertyDefinitions.get(propertyName);
    }

    public Collection<JcrPropertyDefinition> allPropertyDefinitions() {
        return this.allPropertyDefinitions.values();
    }

    public Collection<JcrNodeDefinition> allChildNodeDefinitionsWithNoSns( Name childName ) {
        return this.childNodeDefinitionsThatAllowNoSns.get(childName);
    }

    public Collection<JcrNodeDefinition> allChildNodeDefinitionsWithSns( Name childName ) {
        return this.childNodeDefinitionsThatAllowSns.get(childName);
    }

    public Collection<JcrNodeDefinition> allChildNodeDefinitions( Name childName ) {
        return this.allChildNodeDefinitions.get(childName);
    }

    public Collection<JcrNodeDefinition> allChildNodeDefinitions( Name childName,
                                                                  boolean requireSns ) {
        if (requireSns) {
            // Only return definitions that allow SNS since we require SNS support
            return childNodeDefinitionsThatAllowSns.get(childName);
        }
        return allChildNodeDefinitions.get(childName);
    }

    public Collection<JcrNodeDefinition> allChildNodeDefinitions() {
        return this.allChildNodeDefinitions.values();
    }
}
