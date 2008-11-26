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
package org.jboss.dna.connector.store.jpa.models.common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import org.hibernate.annotations.Index;
import org.jboss.dna.common.util.CheckArg;

/**
 * A NamespaceEntity represents a namespace that has been used in the store. NamespaceEntity records are immutable and shared by
 * one or more enities.
 * 
 * @author Randall Hauch
 */
@Entity
@Table( name = "DNA_NAMESPACES" )
@org.hibernate.annotations.Table( appliesTo = "DNA_NAMESPACES", indexes = @Index( name = "NS_URI_INX", columnNames = {"URI"} ) )
@NamedQueries( {@NamedQuery( name = "NamespaceEntity.findAll", query = "select ns from NamespaceEntity as ns" ),
    @NamedQuery( name = "NamespaceEntity.findByUri", query = "select ns from NamespaceEntity as ns where ns.uri = ?1" )} )
public class NamespaceEntity {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    private Integer id;

    @Column( name = "URI", nullable = false, unique = false, length = 512, updatable = false )
    private String uri;

    /**
     * 
     */
    public NamespaceEntity() {
    }

    /**
     * @param uri the namespace URI
     */
    public NamespaceEntity( String uri ) {
        setUri(uri);
    }

    /**
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id Sets id to the specified value.
     */
    public void setId( Integer id ) {
        this.id = id;
    }

    /**
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri Sets uri to the specified value.
     */
    public void setUri( String uri ) {
        this.uri = uri;
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
        if (obj instanceof NamespaceEntity) {
            NamespaceEntity that = (NamespaceEntity)obj;
            if (!this.id.equals(that.id)) return false;
            if (!this.uri.equals(that.uri)) return false;
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
        return uri;
    }

    /**
     * Find an existing namespace by its URI, or create and return one if it does not already exist.
     * 
     * @param manager the entity manager
     * @param uri the URI
     * @return the existing namespace, or null if one does not exist
     * @throws IllegalArgumentException if the manager or URI are null
     */
    public static NamespaceEntity findByUri( EntityManager manager,
                                             String uri ) {
        return findByUri(manager, uri, true);
    }

    /**
     * Find an existing namespace by its URI.
     * 
     * @param manager the entity manager
     * @param uri the URI
     * @param createIfRequired if the namespace should be persisted if it does not yet exist
     * @return the existing namespace, or null if one does not exist
     * @throws IllegalArgumentException if the manager or URI are null
     */
    public static NamespaceEntity findByUri( EntityManager manager,
                                             String uri,
                                             boolean createIfRequired ) {
        CheckArg.isNotNull(manager, "manager");
        CheckArg.isNotNull(uri, "uri");
        Query query = manager.createNamedQuery("NamespaceEntity.findByUri");
        query.setParameter(1, uri);
        try {
            return (NamespaceEntity)query.getSingleResult();
        } catch (NoResultException e) {
            if (!createIfRequired) return null;
            NamespaceEntity namespace = new NamespaceEntity(uri);
            manager.persist(namespace);
            return namespace;
        }
    }
}
