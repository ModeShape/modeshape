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
package org.jboss.dna.connector.store.jpa.model.basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import org.jboss.dna.graph.property.PropertyType;

/**
 * A single property value that is too large to be stored on the individual node, and which will be shared among all properties
 * that have the same value.
 * 
 * @author Randall Hauch
 */
@Entity
@Table( name = "DNA_BASIC_LARGE_VALUES" )
@NamedQuery( name = "LargeValueEntity.deleteUnused", query = "delete LargeValueEntity value where value.id not in (select values.hash from PropertiesEntity prop join prop.largeValues values)" )
public class LargeValueEntity {

    @Id
    private LargeValueId id;

    /**
     * The property type for this value. Typically, this is {@link PropertyType#STRING} or {@link PropertyType#BINARY}, although
     * technically it could be any type.
     */
    @Enumerated( value = EnumType.STRING )
    @Column( name = "TYPE", nullable = false )
    private PropertyType type;

    /**
     * The number of bytes in this value.
     */
    @Column( name = "LENGTH", nullable = false )
    private long length;

    /**
     * Flag specifying whether the binary data is stored in a compressed format.
     */
    @Column( name = "COMPRESSED", nullable = true )
    private Boolean compressed;

    /**
     * Lazily-fetched value
     */
    @Lob
    @Column( name = "DATA", nullable = false )
    private byte[] data;

    /**
     * @return id
     */
    public LargeValueId getId() {
        return id;
    }

    /**
     * @param id Sets id to the specified value.
     */
    public void setId( LargeValueId id ) {
        this.id = id;
    }

    /**
     * @return length
     */
    public long getLength() {
        return length;
    }

    /**
     * @param length Sets length to the specified value.
     */
    public void setLength( long length ) {
        this.length = length;
    }

    /**
     * @return type
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * @param type Sets type to the specified value.
     */
    public void setType( PropertyType type ) {
        this.type = type;
    }

    /**
     * @return data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data Sets data to the specified value.
     */
    public void setData( byte[] data ) {
        this.data = data;
    }

    /**
     * @return compressed
     */
    public boolean isCompressed() {
        return compressed != null && compressed.booleanValue();
    }

    /**
     * @param compressed Sets compressed to the specified value.
     */
    public void setCompressed( boolean compressed ) {
        this.compressed = Boolean.valueOf(compressed);
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
        if (obj instanceof LargeValueEntity) {
            LargeValueEntity that = (LargeValueEntity)obj;
            if (this.getId().equals(that.getId())) return true;
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
        return "Large " + this.type + " value (hash=" + this.getId().getHash() + ",compressed=" + isCompressed() + ")";
    }

    /**
     * Delete all unused large value entities.
     * 
     * @param manager the manager; never null
     * @return the number of deleted large values
     */
    public static int deleteUnused( EntityManager manager ) {
        assert manager != null;
        Query delete = manager.createNamedQuery("LargeValueEntity.deleteUnused");
        int result = delete.executeUpdate();
        manager.flush();
        return result;
    }
}
