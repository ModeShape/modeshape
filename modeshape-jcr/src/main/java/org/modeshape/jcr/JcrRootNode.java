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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import org.modeshape.jcr.cache.NodeKey;

/**
 * The {@link Node} implementation for the root node.
 */
final class JcrRootNode extends AbstractJcrNode {

    private final String NAME_STR = "";
    private final String PATH = "/";

    private NodeDefinitionId rootNodeDefnId;

    protected JcrRootNode( JcrSession session,
                           NodeKey rootNodeKey ) {
        super(session, rootNodeKey);
    }

    @Override
    NodeDefinitionId nodeDefinitionId() throws RepositoryException {
        if (rootNodeDefnId == null) {
            // Idempotent so we can do this without a lock ...
            rootNodeDefnId = session.workspace().nodeTypeManager().getRootNodeDefinition().getId();
        }
        return rootNodeDefnId;
    }

    @Override
    public JcrNodeDefinition getDefinition() throws RepositoryException {
        return session.workspace().nodeTypeManager().getRootNodeDefinition();
    }

    @Override
    final boolean isRoot() {
        return true;
    }

    @Override
    public boolean isNew() {
        return false; // the root node is never false
    }

    @Override
    Type type() {
        return Type.ROOT;
    }

    @Override
    public String getName() {
        return NAME_STR;
    }

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        throw new ItemNotFoundException();
    }

    @Override
    protected void doRemove() throws ConstraintViolationException, RepositoryException {
        String msg = JcrI18n.unableToRemoveRootNode.text(workspaceName());
        throw new ConstraintViolationException(msg);
    }

    @Override
    public String getIdentifier() {
        //only return the identifier path of the key for root nodes, to account for the case when it is referenceable and
        //so all root nodes should have the same id.
        return key.getIdentifier();
    }
}
