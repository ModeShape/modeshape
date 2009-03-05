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
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;

/**
 * {@link JcrNodeTypeSource} that provides built-in node types provided by DNA.
 */
@Immutable
class DnaBuiltinNodeTypeSource extends AbstractJcrNodeTypeSource {

    /** The list of primary node types. */
    private final List<JcrNodeType> primaryNodeTypes;
    /** The list of mixin node types. */
    private final List<JcrNodeType> mixinNodeTypes;

    DnaBuiltinNodeTypeSource( JcrSession session,
                              JcrNodeTypeSource predecessor ) {
        super(predecessor);

        primaryNodeTypes = new ArrayList<JcrNodeType>();
        mixinNodeTypes = new ArrayList<JcrNodeType>();

        JcrNodeType base = findType(JcrNtLexicon.BASE);

        if (base == null) {
            String baseTypeName = JcrNtLexicon.BASE.getString(session.getExecutionContext().getNamespaceRegistry());
            String namespaceTypeName = DnaLexicon.NAMESPACE.getString(session.getExecutionContext().getNamespaceRegistry());
            throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
        }

        // Stubbing in child node and property definitions for now
        JcrNodeType namespace = new JcrNodeType(session, DnaLexicon.NAMESPACE, Arrays.asList(new NodeType[] {base}),
                                                DnaLexicon.URI, NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                                    new JcrPropertyDefinition(session, null, DnaLexicon.URI,
                                                                              OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                              true, true, true, NO_DEFAULT_VALUES,
                                                                              PropertyType.STRING, NO_CONSTRAINTS, false)}),
                                                NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        primaryNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {namespace}));

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
