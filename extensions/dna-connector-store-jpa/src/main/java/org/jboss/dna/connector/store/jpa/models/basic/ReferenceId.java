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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCode;

/**
 * An identifier for a reference, comprised of a single {@link NodeId} of the node containing the reference and a single
 * {@link NodeId} of the node being referenced.
 * 
 * @author Randall Hauch
 */
@Embeddable
@Immutable
@org.hibernate.annotations.Immutable
public class ReferenceId implements Serializable {

    /**
     * Version {@value}
     */
    private static final long serialVersionUID = 1L;

    @Column( name = "FROM_UUID", nullable = false, updatable = false, length = 36 )
    private String fromUuidString;

    @Column( name = "TO_UUID", nullable = false, updatable = false, length = 36 )
    private String toUuidString;

    public ReferenceId() {
    }

    public ReferenceId( String fromUuid,
                        String toUuid ) {
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
        return "Reference from " + fromUuidString + " to " + toUuidString;
    }

}
