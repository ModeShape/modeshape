/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.federation.merge;

import java.util.UUID;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.requests.ReadNodeRequest;

/**
 * An in-memory (and temporary) representation of a federated node and it's merged properties and children.
 * 
 * @author Randall Hauch
 */
public class FederatedNode extends ReadNodeRequest {

    private static final long serialVersionUID = 1L;

    private UUID uuid;
    private MergePlan mergePlan;

    /**
     * Create a federated node given the path and UUID.
     * 
     * @param location the location of the federated node; may not be null
     * @param uuid the UUID of the federated node; may not be null
     */
    public FederatedNode( Location location,
                          UUID uuid ) {
        super(location);
        assert uuid != null;
        this.uuid = uuid;
        super.setActualLocationOfNode(location);
    }

    /**
     * Get the UUID for this federated node.
     * 
     * @return the UUID; never null
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @param uuid Sets uuid to the specified value.
     */
    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     * Get the merge plan for this federated node
     * 
     * @return the merge plan, or null if there is no merge plan
     */
    public MergePlan getMergePlan() {
        return mergePlan;
    }

    /**
     * Set the merge plan for this federated node
     * 
     * @param mergePlan the new merge plan for this federated node; may be null
     */
    public void setMergePlan( MergePlan mergePlan ) {
        this.mergePlan = mergePlan;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.at().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedNode) {
            FederatedNode that = (FederatedNode)obj;
            if (this.at().equals(that.at())) return true;
            if (this.getUuid().equals(that.getUuid())) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return at().toString();
    }
}
