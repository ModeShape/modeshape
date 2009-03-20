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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;

/**
 * The {@link RepositoryNodeTypeManager} is the maintainer of node type information for the entire repository at run-time. The
 * repository manager maintains a list of all node types and the ability to retrieve node types by {@link Name}. </p> The JCR 1.0
 * and 2.0 specifications both require that node type information be shared across all sessions within a repository and that the
 * {@link javax.jcr.nodetype.NodeTypeManager} perform operations based on the string versions of {@link Name}s based on the permanent
 * (workspace-scoped) and transient (session-scoped) namespace mappings. DNA achieves this by maintaining a single master
 * repository of all node type information (the {@link RepositoryNodeTypeManager}) and per-session wrappers (
 * {@link JcrNodeTypeManager}) for this master repository that perform {@link String} to {@link Name} translation based on the
 * {@link javax.jcr.Session}'s transient mappings and then delegating node type lookups to the repository manager.
 */
@Immutable
class RepositoryNodeTypeManager {

    private final Map<Name, JcrNodeType> primaryNodeTypes;
    private final Map<Name, JcrNodeType> mixinNodeTypes;

    RepositoryNodeTypeManager( ExecutionContext context,
                               JcrNodeTypeSource source ) {
        Collection<JcrNodeType> primary = source.getPrimaryNodeTypes();
        Collection<JcrNodeType> mixins = source.getMixinNodeTypes();

        primaryNodeTypes = new HashMap<Name, JcrNodeType>(primary.size());
        for (JcrNodeType nodeType : primary) {
            primaryNodeTypes.put(nodeType.getInternalName(), nodeType.with(this));
        }

        mixinNodeTypes = new HashMap<Name, JcrNodeType>(mixins.size());
        for (JcrNodeType nodeType : mixins) {
            mixinNodeTypes.put(nodeType.getInternalName(), nodeType.with(this));
        }
    }

    public Collection<JcrNodeType> getMixinNodeTypes() {
        return mixinNodeTypes.values();
    }

    public Collection<JcrNodeType> getPrimaryNodeTypes() {
        return primaryNodeTypes.values();
    }

    JcrNodeType getNodeType( Name nodeTypeName ) {

        JcrNodeType nodeType = primaryNodeTypes.get(nodeTypeName);
        if (nodeType == null) {
            nodeType = mixinNodeTypes.get(nodeTypeName);
        }
        return nodeType;
    }
}
