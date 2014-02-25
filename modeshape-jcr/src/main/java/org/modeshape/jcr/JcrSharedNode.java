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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrSharedNodeCache.SharedSet;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

/**
 * A concrete {@link javax.jcr.Node JCR Node} implementation that is used for all nodes that are part of a shared set but not the
 * original node that was shared. In essence, all instances of JcrShareableNode are proxies that have their own name and location,
 * but that delegate nearly all other operations to the referenced node.
 * <p>
 * Instances of this class are created for each node in a workspace that have a primary type of "{@link ModeShapeLexicon#SHARE
 * mode:share}". These nodes each have a single "{@link ModeShapeLexicon#SHARED_UUID mode:sharedUuid}" REFERENCE property that
 * points to the original shareable node. Thus, with the help of this class, JCR clients do not ever see this "mode:share" node,
 * but instead see the original sharable node.
 * </p>
 * 
 * @see JcrRootNode
 * @see JcrNode
 */
@ThreadSafe
final class JcrSharedNode extends JcrNode {

    private final NodeKey parentKey;
    private final SharedSet sharedSet;

    protected JcrSharedNode( SharedSet sharedSet,
                             NodeKey parentKey ) {
        super(sharedSet.session(), sharedSet.key());
        this.parentKey = parentKey;
        this.sharedSet = sharedSet;
        assert this.parentKey != null;
        assert this.sharedSet != null;
    }

    @Override
    boolean isShared() {
        return true;
    }

    @Override
    protected NodeKey parentKey() {
        return parentKey;
    }

    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        checkSession();
        return parent();
    }

    protected AbstractJcrNode parent() throws ItemNotFoundException {
        return session().node(parentKey, null);
    }

    @Override
    SharedSet sharedSet() {
        return sharedSet;
    }

    @Override
    Path path() throws ItemNotFoundException, InvalidItemStateException {
        AbstractJcrNode parent = parent();
        CachedNode node = parent.node();
        SessionCache cache = session.cache();
        ChildReference childRef = node.getChildReferences(cache).getChild(sharedSet.key());
        Path parentPath = parent.path();
        return session().pathFactory().create(parentPath, childRef.getSegment());
    }

    @Override
    protected Name name() throws RepositoryException {
        return segment().getName();
    }

    @Override
    protected Segment segment() throws RepositoryException {
        AbstractJcrNode parent = parent();
        CachedNode node = parent.node();
        SessionCache cache = session.cache();
        ChildReference childRef = node.getChildReferences(cache).getChild(sharedSet.key());
        return childRef.getSegment();
    }
}
