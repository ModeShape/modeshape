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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.api.Namespaced;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;

/**
 * The abstract base class for all {@link Item} implementations.
 */
@ThreadSafe
abstract class AbstractJcrItem implements Item, Namespaced {

    protected final JcrSession session;

    protected AbstractJcrItem( JcrSession session ) {
        this.session = session;
    }

    final JcrSession session() {
        return session;
    }

    final String workspaceName() {
        return session.workspaceName();
    }

    /**
     * Check that the session is still valid and {@link Session#isLive() live}.
     * 
     * @throws RepositoryException if the session is not valid or live
     */
    protected final void checkSession() throws RepositoryException {
        session.checkLive();
    }

    final ExecutionContext context() {
        return session.context();
    }

    final NodeCache cache() {
        return session().cache();
    }

    final Name nameFrom( String name ) {
        return context().getValueFactories().getNameFactory().create(name);
    }

    final Path pathFrom( String path ) {
        return context().getValueFactories().getPathFactory().create(path);
    }

    final Path.Segment segmentFrom( String segment ) {
        return context().getValueFactories().getPathFactory().createSegment(segment);
    }

    final Path.Segment segmentFrom( Name segment ) {
        return context().getValueFactories().getPathFactory().createSegment(segment);
    }

    final NamespaceRegistry namespaces() {
        return context().getNamespaceRegistry();
    }

    final JcrValueFactory valueFactory() {
        return session().valueFactory();
    }

    abstract Path path() throws RepositoryException;

    @Override
    public Item getAncestor( int depth ) throws RepositoryException {
        checkSession();

        if (depth < 0) {
            throw new ItemNotFoundException(JcrI18n.noNegativeDepth.text(depth));
        }

        /*
         * depth argument is absolute depth from root of content graph, not relative depth from current node.
         * That is, if current node is at path /foo/bar/baz, getAncestor(1) returns node at /foo, getAncestor(2)
         * returns node at /foo/bar, getAncestor(3) returns node at /foo/bar/baz, getAncestor(0) returns root node,
         * and any other argument results in ItemNotFoundException.
         * Next statement converts depth parameter from a relative depth to an absolute depth.
         */
        depth = getDepth() - depth;

        if (depth < 0) {
            throw new ItemNotFoundException(JcrI18n.tooDeep.text(depth));
        }

        Item ancestor = this;
        while (--depth >= 0) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    @Override
    public int getDepth() throws RepositoryException {
        checkSession();
        return path().size();
    }

    @Override
    public abstract AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException;

    protected boolean isSameRepository( Item otherItem ) throws RepositoryException {
        assert getSession() != null;
        assert otherItem.getSession() != null;
        assert getSession().getRepository() != null;
        assert otherItem.getSession().getRepository() != null;

        if (getSession().getRepository() != otherItem.getSession().getRepository()) {
            return false;
        }
        return true;
    }

}
