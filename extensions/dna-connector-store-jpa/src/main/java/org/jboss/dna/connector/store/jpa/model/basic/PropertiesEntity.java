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

import java.util.Collection;
import java.util.HashSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.jboss.dna.connector.store.jpa.util.Serializer;

/**
 * Represents the packed properties of a single node. Node that the object has the node's identifier and the packed properties,
 * but nothing else. The PropertiesEntity doesn't even have the name. This is because this class is used to read, modify, and save
 * the properties of a node. Finding a node by its name or working with the children, however, requires working with the
 * {@link ChildEntity node children}.
 * 
 * @author Randall Hauch
 */
@Entity
@Table( name = "DNA_BASIC_NODEPROPS" )
@NamedQueries( {
    @NamedQuery( name = "PropertiesEntity.findByUuid", query = "select prop from PropertiesEntity as prop where prop.id.uuidString = :uuid" ),
    @NamedQuery( name = "PropertiesEntity.deleteByUuid", query = "delete PropertiesEntity prop where prop.id.uuidString = :uuid" )} )
public class PropertiesEntity {
    @Id
    private NodeId id;

    @Lob
    @Column( name = "DATA", nullable = true, unique = false )
    private byte[] data;

    @Column( name = "NUM_PROPS", nullable = false )
    private int propertyCount;

    /**
     * Flag specifying whether the binary data is stored in a compressed format.
     */
    @Column( name = "COMPRESSED", nullable = true )
    private Boolean compressed;

    /**
     * Flag specifying whether the binary data is stored in a compressed format.
     */
    @Column( name = "ENFORCEREFINTEG", nullable = false )
    private boolean referentialIntegrityEnforced = true;

    @org.hibernate.annotations.CollectionOfElements( fetch = FetchType.LAZY )
    @JoinTable( name = "DNA_LARGEVALUE_USAGES", joinColumns = @JoinColumn( name = "NODE_UUID" ) )
    private Collection<LargeValueId> largeValues = new HashSet<LargeValueId>();

    public PropertiesEntity() {
    }

    public PropertiesEntity( NodeId id ) {
        setId(id);
    }

    /**
     * Get the node's identifier.
     * 
     * @return the node's identifier
     */
    public NodeId getId() {
        return id;
    }

    /**
     * Set the node's identifier.
     * 
     * @param id the new identifier for the node
     */
    public void setId( NodeId id ) {
        this.id = id;
    }

    /**
     * Get the data that represents the {@link Serializer packed} properties.
     * 
     * @return the raw data representing the properties
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Set the data that represents the {@link Serializer packed} properties.
     * 
     * @param data the raw data representing the properties
     */
    public void setData( byte[] data ) {
        this.data = data;
    }

    /**
     * @return propertyCount
     */
    public int getPropertyCount() {
        return propertyCount;
    }

    /**
     * @param propertyCount Sets propertyCount to the specified value.
     */
    public void setPropertyCount( int propertyCount ) {
        this.propertyCount = propertyCount;
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
     * @return largeValues
     */
    public Collection<LargeValueId> getLargeValues() {
        return largeValues;
    }

    /**
     * @return referentialIntegrityEnforced
     */
    public boolean isReferentialIntegrityEnforced() {
        return referentialIntegrityEnforced;
    }

    /**
     * @param referentialIntegrityEnforced Sets referentialIntegrityEnforced to the specified value.
     */
    public void setReferentialIntegrityEnforced( boolean referentialIntegrityEnforced ) {
        this.referentialIntegrityEnforced = referentialIntegrityEnforced;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertiesEntity) {
            PropertiesEntity that = (PropertiesEntity)obj;
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
        return "Properties for " + this.id;
    }
}
