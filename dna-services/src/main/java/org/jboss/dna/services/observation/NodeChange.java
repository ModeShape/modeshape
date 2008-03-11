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
package org.jboss.dna.services.observation;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCodeUtil;

/**
 * An enqueued notification that a node has changed.
 * @author Randall Hauch
 */
@Immutable
public class NodeChange {

    private final String repositoryWorkspaceName;
    private final String absolutePath;
    private int eventTypes;
    private final int hc;

    public NodeChange( String repositoryWorkspaceName, String absolutePath, int eventTypes ) {
        assert repositoryWorkspaceName != null;
        assert absolutePath != null;
        this.repositoryWorkspaceName = repositoryWorkspaceName;
        this.absolutePath = absolutePath.trim();
        this.hc = HashCodeUtil.computeHash(this.repositoryWorkspaceName, this.absolutePath);
        this.eventTypes = eventTypes;
    }

    /**
     * @return absolutePath
     */
    public String getAbsolutePath() {
        return this.absolutePath;
    }

    /**
     * @return repositoryWorkspaceName
     */
    public String getRepositoryWorkspaceName() {
        return this.repositoryWorkspaceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.hc;
    }

    public boolean includesAllEventTypes( int... jcrEventTypes ) {
        for (int jcrEventType : jcrEventTypes) {
            if ((this.eventTypes & jcrEventType) == 0) return false;
        }
        return true;
    }

    public boolean includesEventTypes( int... jcrEventTypes ) {
        for (int jcrEventType : jcrEventTypes) {
            if ((this.eventTypes & jcrEventType) != 0) return true;
        }
        return false;
    }

    /* package */void setEventType( int jcrEventType ) {
        this.eventTypes |= jcrEventType;
    }

    public boolean isSameNode( NodeChange that ) {
        if (that == this) return true;
        if (this.hc != that.hc) return false;
        if (!this.repositoryWorkspaceName.equals(that.repositoryWorkspaceName)) return false;
        if (!this.absolutePath.equals(that.absolutePath)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeChange) {
            NodeChange that = (NodeChange)obj;
            if (!this.isSameNode(that)) return false;
            if (this.eventTypes != that.eventTypes) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.repositoryWorkspaceName + "=>" + this.absolutePath;
    }
}
