/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Path.Segment;

/**
 * @author jverhaeg
 */
@NotThreadSafe
final class JcrNode extends AbstractJcrNode {

    private final UUID parentUuid;
    private final Segment segment;

    JcrNode( Session session,
             UUID parentUuid,
             Segment segment ) {
        super(session);
        assert parentUuid != null;
        assert segment != null;
        this.parentUuid = parentUuid;
        this.segment = segment;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        return getParent().getDepth() + 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() {
        return segment.getIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public String getName() {
        return segment.getName().getString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    public Node getParent() throws RepositoryException {
        return getSession().getNodeByUUID(parentUuid.toString());
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
