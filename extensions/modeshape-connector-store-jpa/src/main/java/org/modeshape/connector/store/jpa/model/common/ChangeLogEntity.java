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
package org.modeshape.connector.store.jpa.model.common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.hibernate.annotations.Index;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.basic.JodaDateTime;

/**
 * Represents a record of the changes that have been made to the repository. The actual change events are serialized and stored in
 * a binary (and compressed) format.
 */
@Entity
@Table( name = "DNA_CHANGELOG" )
@org.hibernate.annotations.Table( appliesTo = "DNA_CHANGELOG", indexes = @Index( name = "NS_CHANGE_TS_INX", columnNames = {"UTC_TIMESTAMP"} ) )
@NamedQueries( {
    @NamedQuery( name = "ChangeLogEntity.findBetween", query = "select entry from ChangeLogEntity as entry where entry.timestampInUtc >= :start and entry.timestampInUtc <= :end" ),
    @NamedQuery( name = "ChangeLogEntity.deleteBefore", query = "delete ChangeLogEntity entry where entry.timestampInUtc < :timestamp" )} )
public class ChangeLogEntity {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    @Column( name = "ID", updatable = false )
    private Long id;

    @Column( name = "USERNAME", updatable = false, nullable = false, length = 64, unique = false )
    private String username;

    @Column( name = "UTC_TIMESTAMP", updatable = false, nullable = false, unique = false )
    private long timestampInUtc;

    @Column( name = "CHANGE_COUNT", updatable = false, nullable = false, unique = false )
    private int numChanges;

    @Lob
    @Column( name = "CHANGES", updatable = false, nullable = false, unique = false )
    private byte[] changes;

    public ChangeLogEntity( String username,
                            DateTime timestamp,
                            int numChanges,
                            byte[] changes ) {
        this.username = username;
        this.timestampInUtc = timestamp.toUtcTimeZone().getMilliseconds();
        this.numChanges = numChanges;
        this.changes = changes;
    }

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return timestampInUtc
     */
    public long getTimestampInUtc() {
        return timestampInUtc;
    }

    /**
     * @return changes
     */
    public byte[] getChanges() {
        return changes;
    }

    /**
     * @return numChanges
     */
    public int getNumChanges() {
        return numChanges;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChangeLogEntity) {
            ChangeLogEntity that = (ChangeLogEntity)obj;
            return id.equals(that.id);
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
        return "" + numChanges + " changes by " + username + " at " + new JodaDateTime(timestampInUtc);
    }
}
