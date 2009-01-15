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

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import org.hibernate.annotations.Index;

/**
 * A record of a reference from one node to another.
 * 
 * @author Randall Hauch
 */
@Entity
@Table( name = "DNA_BASIC_REFERENCES" )
@org.hibernate.annotations.Table( appliesTo = "DNA_BASIC_REFERENCES", indexes = {
    @Index( name = "REFINDEX_INX", columnNames = {"FROM_UUID", "TO_UUID"} ),
    @Index( name = "REFTOUUID_INX", columnNames = {"TO_UUID"} )} )
@NamedQueries( {
    @NamedQuery( name = "ReferenceEntity.removeReferencesFrom", query = "delete ReferenceEntity where id.fromUuidString = :fromUuid" ),
    @NamedQuery( name = "ReferenceEntity.removeNonEnforcedReferences", query = "delete ReferenceEntity as ref where ref.id.fromUuidString not in ( select props.id.uuidString from PropertiesEntity props where props.referentialIntegrityEnforced = true )" ),
    @NamedQuery( name = "ReferenceEntity.countUnresolveReferences", query = "select count(*) from ReferenceEntity as ref where ref.id.toUuidString not in ( select props.id.uuidString from PropertiesEntity props where props.referentialIntegrityEnforced = true )" ),
    @NamedQuery( name = "ReferenceEntity.getUnresolveReferences", query = "select ref from ReferenceEntity as ref where ref.id.toUuidString not in ( select props.id.uuidString from PropertiesEntity props where props.referentialIntegrityEnforced = true )" )} )
public class ReferenceEntity {

    @Id
    private ReferenceId id;

    /**
     * 
     */
    public ReferenceEntity() {
    }

    /**
     * @param id the id
     */
    public ReferenceEntity( ReferenceId id ) {
        this.id = id;
    }

    /**
     * @return id
     */
    public ReferenceId getId() {
        return id;
    }

    /**
     * @param id Sets id to the specified value.
     */
    public void setId( ReferenceId id ) {
        this.id = id;
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
        if (obj instanceof ReferenceEntity) {
            ReferenceEntity that = (ReferenceEntity)obj;
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
        return this.id.toString();
    }

    /**
     * Delete all references that start from the node with the supplied UUID.
     * 
     * @param uuid the UUID of the node from which the references start
     * @param manager the manager; may not be null
     * @return the number of deleted references
     */
    public static int deleteReferencesFrom( String uuid,
                                            EntityManager manager ) {
        assert manager != null;
        Query delete = manager.createNamedQuery("ReferenceEntity.removeReferencesFrom");
        delete.setParameter("fromUuid", uuid);
        int result = delete.executeUpdate();
        manager.flush();
        return result;
    }

    /**
     * Delete all references that start from nodes that do not support enforced referential integrity.
     * 
     * @param manager the manager; may not be null
     * @return the number of deleted references
     */
    public static int deleteUnenforcedReferences( EntityManager manager ) {
        assert manager != null;
        Query delete = manager.createNamedQuery("ReferenceEntity.removeNonEnforcedReferences");
        int result = delete.executeUpdate();
        manager.flush();
        return result;
    }

    /**
     * Delete all references that start from nodes that do not support enforced referential integrity.
     * 
     * @param manager the manager; may not be null
     * @return the number of deleted references
     */
    public static int countAllReferencesResolved( EntityManager manager ) {
        assert manager != null;
        Query query = manager.createNamedQuery("ReferenceEntity.getUnresolveReferences");
        try {
            return (Integer)query.getSingleResult();
        } catch (NoResultException e) {
            return 0;
        }
    }

    /**
     * Delete all references that start from nodes that do not support enforced referential integrity.
     * 
     * @param manager the manager; may not be null
     * @return the number of deleted references
     */
    @SuppressWarnings( "unchecked" )
    public static List<ReferenceEntity> verifyAllReferencesResolved( EntityManager manager ) {
        assert manager != null;
        Query query = manager.createNamedQuery("ReferenceEntity.getUnresolveReferences");
        return query.getResultList();
    }
}
