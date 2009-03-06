/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Name;

/**
 * Local implementation of @{link NodeTypeManager}. Initialized with {@link NodeType} source data when it is created (in the
 * {@link JcrWorkspace} constructor.
 */
@NotThreadSafe
class JcrNodeTypeManager implements NodeTypeManager {

    private final Map<Name, JcrNodeType> primaryNodeTypes;
    private final Map<Name, JcrNodeType> mixinNodeTypes;
    private final JcrSession session;

    JcrNodeTypeManager( JcrSession session,
                        JcrNodeTypeSource source ) {
        this.session = session;
        Collection<JcrNodeType> primary = source.getPrimaryNodeTypes();
        Collection<JcrNodeType> mixins = source.getMixinNodeTypes();

        primaryNodeTypes = new HashMap<Name, JcrNodeType>(primary.size());
        for (JcrNodeType nodeType : primary) {
            primaryNodeTypes.put(nodeType.getInternalName(), nodeType);
        }

        mixinNodeTypes = new HashMap<Name, JcrNodeType>(mixins.size());
        for (JcrNodeType nodeType : mixins) {
            mixinNodeTypes.put(nodeType.getInternalName(), nodeType);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     */
    public NodeTypeIterator getAllNodeTypes() {

        // TODO: Can revisit this approach later if it becomes a performance issue
        /*
         * Note also that this creates a subtle difference in behavior for concurrent modification
         * between this method and the specific get*NodeTypes methods.  That is, if a type is added
         * while an iterator from the corresponding specific get*NodeType method is being traversed,
         * a ConcurrentModificationException will be thrown.  Because this iterator is based on a copy
         * of the underlying maps, no exception would be thrown in the same case.
         */

        List<NodeType> allTypes = new ArrayList<NodeType>(primaryNodeTypes.size() + mixinNodeTypes.size());
        allTypes.addAll(primaryNodeTypes.values());
        allTypes.addAll(mixinNodeTypes.values());
        return new JcrNodeTypeIterator(allTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     */
    public NodeTypeIterator getMixinNodeTypes() {
        return new JcrNodeTypeIterator(mixinNodeTypes.values());
    }

    final JcrNodeType getNodeType( Name nodeTypeName ) {

        JcrNodeType nodeType = primaryNodeTypes.get(nodeTypeName);
        if (nodeType == null) {
            nodeType = mixinNodeTypes.get(nodeTypeName);
        }
        return nodeType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(java.lang.String)
     */
    public NodeType getNodeType( String nodeTypeName ) throws NoSuchNodeTypeException, RepositoryException {
        Name ntName = session.getExecutionContext().getValueFactories().getNameFactory().create(nodeTypeName);
        NodeType type = getNodeType(ntName);
        if (type != null) return type;
        throw new NoSuchNodeTypeException(JcrI18n.typeNotFound.text(nodeTypeName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     */
    public NodeTypeIterator getPrimaryNodeTypes() {
        return new JcrNodeTypeIterator(primaryNodeTypes.values());
    }

    /**
     * Get the {@link NodeDefinition} for the root node.
     * 
     * @return the definition; never null
     * @throws RepositoryException
     * @throws NoSuchNodeTypeException
     */
    JcrNodeDefinition getRootNodeDefinition() throws NoSuchNodeTypeException, RepositoryException {
        // TODO: Fix this
        for (NodeDefinition definition : getNodeType("nt:unstructured").getChildNodeDefinitions()) {
            if (definition.getName().equals(JcrNodeType.RESIDUAL_ITEM_NAME)) return (JcrNodeDefinition)definition;
        }
        assert false; // should not get here
        return null;
    }

}
