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
package org.jboss.dna.connector.store.jpa.models.basic;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.connector.store.jpa.models.common.NodeId;

/**
 * A unique identifer for a parent-child relationship.
 * 
 * @author Randall Hauch
 */
@Embeddable
public class ChildId implements Serializable {

    /**
     * Version {@value}
     */
    private static final long serialVersionUID = 1L;

    @Column( name = "PARENT_UUID", nullable = false )
    private String parentUuidString;

    @Column( name = "CHILD_UUID", nullable = false )
    private String childUuidString;

    private transient UUID parentUuid;
    private transient UUID childUuid;

    public ChildId() {
    }

    public ChildId( UUID parentUuid,
                    UUID childUuid ) {
        setParentUuid(parentUuid);
        setChildUuid(childUuid);
    }

    public ChildId( NodeId parentId,
                    NodeId childId ) {
        if (parentId != null) setParentUuid(parentId.getUuid());
        if (childId != null) setChildUuid(childId.getUuid());
    }

    public ChildId( String parentUuid,
                    String childUuid ) {
        setParentUuidString(parentUuid);
        setChildUuidString(childUuid);
    }

    public UUID getParentUuid() {
        if (parentUuidString == null) return null;
        if (parentUuid == null) {
            parentUuid = UUID.fromString(parentUuidString);
        }
        return parentUuid;
    }

    public void setParentUuid( UUID uuid ) {
        this.parentUuid = uuid;
        this.parentUuidString = uuid != null ? uuid.toString() : null;
    }

    public UUID getChildUuid() {
        if (childUuidString == null) return null;
        if (childUuid == null) {
            childUuid = UUID.fromString(childUuidString);
        }
        return childUuid;
    }

    public void setChildUuid( UUID uuid ) {
        this.childUuid = uuid;
        this.childUuidString = uuid != null ? uuid.toString() : null;
    }

    /**
     * @return parentUuidString
     */
    public String getParentUuidString() {
        return parentUuidString;
    }

    /**
     * @param parentUuidString Sets parentUuidString to the specified value.
     */
    public void setParentUuidString( String parentUuidString ) {
        this.parentUuid = null;
        this.parentUuidString = parentUuidString;
    }

    /**
     * @return childUuidString
     */
    public String getChildUuidString() {
        return childUuidString;
    }

    /**
     * @param childUuidString Sets childUuidString to the specified value.
     */
    public void setChildUuidString( String childUuidString ) {
        this.childUuid = null;
        this.childUuidString = childUuidString;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(parentUuidString, childUuidString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildId) {
            ChildId that = (ChildId)obj;
            if (this.parentUuidString == null) {
                if (that.parentUuidString != null) return false;
            } else {
                if (!this.parentUuidString.equals(that.parentUuidString)) return false;
            }
            if (this.childUuidString == null) {
                if (that.childUuidString != null) return false;
            } else {
                if (!this.childUuidString.equals(that.childUuidString)) return false;
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
        return "Child " + childUuidString + " of " + parentUuidString;
    }

}
