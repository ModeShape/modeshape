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
package org.jboss.dna.connector.store.jpa.model.basic;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCode;

/**
 * An identifier for a reference, comprised of a single {@link NodeId} of the node containing the reference and a single
 * {@link NodeId} of the node being referenced.
 */
@Embeddable
@Immutable
@org.hibernate.annotations.Immutable
public class ReferenceId implements Serializable {

    /**
     * Version {@value}
     */
    private static final long serialVersionUID = 1L;

    @Column( name = "WORKSPACE_ID", nullable = false )
    private Long workspaceId;

    @Column( name = "FROM_UUID", nullable = false, updatable = false, length = 36 )
    private String fromUuidString;

    @Column( name = "TO_UUID", nullable = false, updatable = false, length = 36 )
    private String toUuidString;

    public ReferenceId() {
    }

    public ReferenceId( Long workspaceId,
                        String fromUuid,
                        String toUuid ) {
        this.workspaceId = workspaceId;
        this.fromUuidString = fromUuid;
        this.toUuidString = toUuid;
    }

    /**
     * @return fromUuidString
     */
    public String getFromUuidString() {
        return fromUuidString;
    }

    /**
     * @return toUuidString
     */
    public String getToUuidString() {
        return toUuidString;
    }

    /**
     * @return workspaceId
     */
    public Long getWorkspaceId() {
        return workspaceId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(fromUuidString, toUuidString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ReferenceId) {
            ReferenceId that = (ReferenceId)obj;
            if (this.workspaceId == null) {
                if (that.workspaceId != null) return false;
            } else {
                if (!this.workspaceId.equals(that.workspaceId)) return false;
            }
            if (this.fromUuidString == null) {
                if (that.fromUuidString != null) return false;
            } else {
                if (!this.fromUuidString.equals(that.fromUuidString)) return false;
            }
            if (this.toUuidString == null) {
                if (that.toUuidString != null) return false;
            } else {
                if (!this.toUuidString.equals(that.toUuidString)) return false;
            }
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
        return "Reference from " + fromUuidString + " to " + toUuidString + " in workspace " + workspaceId;
    }

}
