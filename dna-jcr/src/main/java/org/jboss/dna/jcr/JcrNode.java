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

import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrNode extends AbstractJcrNode {

    private final UUID parentUuid;

    JcrNode( JcrSession session,
             UUID parentUuid,
             Location location,
             NodeDefinition nodeDefinition ) {
        super(session, location, nodeDefinition);
        assert parentUuid != null;
        this.parentUuid = parentUuid;
    }

    final Path.Segment segment() {
        return location.getPath().getLastSegment();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() {
        return segment().getIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public String getName() {
        return segment().getName().getString(((JcrSession)getSession()).getExecutionContext().getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    public Node getParent() throws ItemNotFoundException {
        Node node = session().getNode(parentUuid);
        if (node == null) {
            throw new ItemNotFoundException();
        }
        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() throws RepositoryException {
        Node parent = getParent();
        StringBuilder builder = new StringBuilder(parent.getPath());
        assert builder.length() > 0;
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append('/');
        }
        String name = getName();
        builder.append(name);
        int ndx = getIndex();
        if (ndx > 1) {
            builder.append('[');
            builder.append(ndx);
            builder.append(']');
        }
        return builder.toString();
    }
}
