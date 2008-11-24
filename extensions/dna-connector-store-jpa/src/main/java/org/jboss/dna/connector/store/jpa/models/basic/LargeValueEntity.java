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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import org.jboss.dna.graph.properties.PropertyType;

/**
 * A single property value that is too large to be stored on the individual node.
 * 
 * @author Randall Hauch
 */
@Entity( name = "DNA_BASIC_LARGE_VALUES" )
public class LargeValueEntity {

    /**
     * The 160-bit SHA-1 hash of this value, in hex form (40-bytes). The SHA-1 algorithm is fast and has not yet proven to have
     * any duplicates. Even if SHA-2 and SHA-3 are better for cryptographically secure purposes, it is doubtful whether a
     * repository needs more than SHA-1.
     */
    @Id
    @Column( name = "SHA1", nullable = false, unique = true, length = 40 )
    private String hash;

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
     * The number of times this value is used. If this value drops below 1, the value could be removed from the store.
     */
    @Column( name = "USAGE_COUNT", nullable = false )
    private int usageCount = 1;

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
     * @return hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash Sets hash to the specified value.
     */
    public void setHash( String hash ) {
        this.hash = hash;
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
     * @return usageCount
     */
    public int getUsageCount() {
        return usageCount;
    }

    /**
     * @param usageCount Sets usageCount to the specified value.
     */
    public void setUsageCount( int usageCount ) {
        this.usageCount = usageCount;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public int decrementUsageCount() {
        if (this.usageCount == 0) return 0;
        return --this.usageCount;
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
        return getHash().hashCode();
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
            if (this.getHash().equals(that.getHash())) return true;
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
        return "Large " + this.type + " value (hash=" + this.hash + ",compressed=" + isCompressed() + ")";
    }
}
