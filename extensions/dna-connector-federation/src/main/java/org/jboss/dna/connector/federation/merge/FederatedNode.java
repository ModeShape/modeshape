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
package org.jboss.dna.connector.federation.merge;

import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.request.ReadNodeRequest;

/**
 * An in-memory (and temporary) representation of a federated node and it's merged properties and children.
 * 
 * @author Randall Hauch
 */
public class FederatedNode extends ReadNodeRequest {

    private static final long serialVersionUID = 1L;

    private MergePlan mergePlan;

    /**
     * Create a federated node given the path and UUID.
     * 
     * @param location the location of the federated node; may not be null
     * @param workspaceName the name of the (federated) workspace in which this node exists
     */
    public FederatedNode( Location location,
                          String workspaceName ) {
        super(location, workspaceName);
        super.setActualLocationOfNode(location);
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
            if (!this.at().equals(that.at())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            return true;
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
