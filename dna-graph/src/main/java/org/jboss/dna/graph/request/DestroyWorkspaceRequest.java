/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.graph.request;

import org.jboss.dna.common.util.CheckArg;

/**
 * Request that an existing workspace with the supplied name be destroyed.
 */
public final class DestroyWorkspaceRequest extends Request {

    private static final long serialVersionUID = 1L;

    private final String workspaceName;

    /**
     * Create a request to destroy an existing workspace.
     * 
     * @param workspaceName the name of the workspace that is to be destroyed
     */
    public DestroyWorkspaceRequest( String workspaceName ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
    }

    /**
     * Get the name for the workspace that is to be destroyed.
     * 
     * @return the name for the workspace; never null
     */
    public String workspaceName() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return workspaceName.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            DestroyWorkspaceRequest that = (DestroyWorkspaceRequest)obj;
            if (!this.workspaceName.equals(that.workspaceName())) return false;
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
        return "destroy workspace \"" + workspaceName() + "\"";
    }
}
