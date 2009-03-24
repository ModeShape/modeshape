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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.jcr.PropertyType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrMixLexicon;

/**
 * {@link JcrNodeTypeSource} that provides built-in node types provided by DNA.
 */
@Immutable
class DnaBuiltinNodeTypeSource extends AbstractJcrNodeTypeSource {

    /** The list of primary node types. */
    private final List<JcrNodeType> primaryNodeTypes;
    /** The list of mixin node types. */
    private final List<JcrNodeType> mixinNodeTypes;

    DnaBuiltinNodeTypeSource( ExecutionContext context,
                              JcrNodeTypeSource predecessor ) {
        super(predecessor);

        primaryNodeTypes = new ArrayList<JcrNodeType>();
        mixinNodeTypes = new ArrayList<JcrNodeType>();

        JcrNodeType base = findType(JcrNtLexicon.BASE);

        if (base == null) {
            String baseTypeName = JcrNtLexicon.BASE.getString(context.getNamespaceRegistry());
            String namespaceTypeName = DnaLexicon.NAMESPACE.getString(context.getNamespaceRegistry());
            throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
        }

        JcrNodeType referenceable = findType(JcrMixLexicon.REFERENCEABLE);

        if (referenceable == null) {
            String baseTypeName = JcrMixLexicon.REFERENCEABLE.getString(context.getNamespaceRegistry());
            String namespaceTypeName = DnaLexicon.SYSTEM.getString(context.getNamespaceRegistry());
            throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
        }

        // Stubbing in child node and property definitions for now
        JcrNodeType namespace = new JcrNodeType(
                                                context,
                                                NO_NODE_TYPE_MANAGER,
                                                DnaLexicon.NAMESPACE,
                                                Arrays.asList(new JcrNodeType[] {base}),
                                                DnaLexicon.URI,
                                                NO_CHILD_NODES,
                                                Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                     context,
                                                                                                                     null,
                                                                                                                     DnaLexicon.URI,
                                                                                                                     OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                     true,
                                                                                                                     true,
                                                                                                                     true,
                                                                                                                     NO_DEFAULT_VALUES,
                                                                                                                     PropertyType.STRING,
                                                                                                                     NO_CONSTRAINTS,
                                                                                                                     false)}),
                                                NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType namespaces = new JcrNodeType(
                                                 context,
                                                 NO_NODE_TYPE_MANAGER,
                                                 DnaLexicon.NAMESPACES,
                                                 Arrays.asList(new JcrNodeType[] {base}),
                                                 NO_PRIMARY_ITEM_NAME,
                                                 Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                              context,
                                                                                                              null,
                                                                                                              null,
                                                                                                              OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                              false,
                                                                                                              false,
                                                                                                              true,
                                                                                                              false,
                                                                                                              DnaLexicon.NAMESPACE,
                                                                                                              new JcrNodeType[] {namespace})}),
                                                 NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType system = new JcrNodeType(
                                             context,
                                             NO_NODE_TYPE_MANAGER,
                                             DnaLexicon.SYSTEM,
                                             Arrays.asList(new JcrNodeType[] {base}),
                                             NO_PRIMARY_ITEM_NAME,
                                             Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          DnaLexicon.NAMESPACES,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          true,
                                                                                                          true,
                                                                                                          true,
                                                                                                          false,
                                                                                                          DnaLexicon.NAMESPACES,
                                                                                                          new JcrNodeType[] {namespaces})}),
                                             NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType root = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, DnaLexicon.ROOT, Arrays.asList(new JcrNodeType[] {base,
            referenceable}), NO_PRIMARY_ITEM_NAME, Arrays.asList(new JcrNodeDefinition[] {
            new JcrNodeDefinition(context, null, JcrLexicon.SYSTEM, OnParentVersionBehavior.IGNORE.getJcrValue(), true, true,
                                  true, false, DnaLexicon.SYSTEM, new JcrNodeType[] {system}),
            new JcrNodeDefinition(context, null, ALL_NODES, OnParentVersionBehavior.VERSION.getJcrValue(), false, false, false,
                                  true, JcrNtLexicon.UNSTRUCTURED, new JcrNodeType[] {base}),

        }), Arrays.asList(new JcrPropertyDefinition[] {
            new JcrPropertyDefinition(context, null, ALL_NODES, OnParentVersionBehavior.COPY.getJcrValue(), false, false, false,
                                      NO_DEFAULT_VALUES, PropertyType.UNDEFINED, NO_CONSTRAINTS, false),
            new JcrPropertyDefinition(context, null, ALL_NODES, OnParentVersionBehavior.COPY.getJcrValue(), false, false, false,
                                      NO_DEFAULT_VALUES, PropertyType.UNDEFINED, NO_CONSTRAINTS, true),}), NOT_MIXIN,
                                           ORDERABLE_CHILD_NODES);

        primaryNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {root, system, namespaces, namespace,}));
        mixinNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {}));

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getMixinNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredMixinNodeTypes() {
        return mixinNodeTypes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getPrimaryNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredPrimaryNodeTypes() {
        return primaryNodeTypes;
    }

}
