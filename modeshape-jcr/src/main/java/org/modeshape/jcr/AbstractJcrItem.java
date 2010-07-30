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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * An abstract {@link Item} implementation.
 */
@Immutable
abstract class AbstractJcrItem implements Item {

    protected final SessionCache cache;

    protected AbstractJcrItem( SessionCache cache ) {
        assert cache != null;
        this.cache = cache;
    }

    /**
     * Check that the session is still valid and {@link Session#isLive() live}.
     * 
     * @throws RepositoryException if the session is not valid or live
     */
    protected final void checkSession() throws RepositoryException {
        cache.session().checkLive();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getSession()
     */
    public Session getSession() {
        // Do not check whether the session is valid, because we need to be able to get the session to check isLive()
        // checkSession();
        return cache.session();
    }

    final JcrSession session() {
        return cache.session();
    }

    final ExecutionContext context() {
        return cache.context();
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

    abstract Path path() throws RepositoryException;

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame( Item otherItem ) throws RepositoryException {
        assert getSession() != null;
        assert otherItem.getSession() != null;
        assert getSession().getRepository() != null;
        assert otherItem.getSession().getRepository() != null;

        if (getSession().getRepository() != otherItem.getSession().getRepository()) {
            return false;
        }

        assert getSession().getWorkspace() != null;
        assert otherItem.getSession().getWorkspace() != null;

        String workspaceName = getSession().getWorkspace().getName();
        String otherWorkspaceName = otherItem.getSession().getWorkspace().getName();

        if (workspaceName == null) {
            return otherWorkspaceName == null;
        }

        return workspaceName.equals(otherWorkspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getAncestor(int)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        checkSession();
        return path().size();
    }

}
