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
}
