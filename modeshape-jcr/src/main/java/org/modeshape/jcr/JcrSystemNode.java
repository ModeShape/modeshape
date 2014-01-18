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

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import org.modeshape.jcr.cache.NodeKey;

/**
 * A Node implementation that is used to represent all nodes within the "/jcr:system" subgraph.
 */
public class JcrSystemNode extends JcrNode {

    JcrSystemNode( JcrSession session,
                   NodeKey nodeKey ) {
        super(session, nodeKey);
    }

    @Override
    boolean isSystem() {
        return true;
    }

    @Override
    protected void doRemove() throws ConstraintViolationException, RepositoryException {
        String msg = JcrI18n.unableToRemoveSystemNodes.text(location(), workspaceName());
        throw new ConstraintViolationException(msg);
    }

    @Override
    protected void checkNodeTypeCanBeModified() throws RepositoryException {
        String msg = JcrI18n.unableToModifySystemNodes.text(location(), workspaceName());
        throw new ConstraintViolationException(msg);
    }

}
