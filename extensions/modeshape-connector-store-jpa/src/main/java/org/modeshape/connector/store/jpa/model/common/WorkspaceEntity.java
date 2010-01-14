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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.modeshape.common.util.CheckArg;

/**
 * A WorkspaceEntity represents a workspace that has been create in the store. WorkspaceEntity records are immutable and shared by
 * one or more enities.
 */
@Entity
@Table( name = "DNA_WORKSPACES" )
@org.hibernate.annotations.Table( appliesTo = "DNA_WORKSPACES", indexes = @Index( name = "WS_NAME_INX", columnNames = {"NAME"} ) )
@NamedQueries( {@NamedQuery( name = "WorkspaceEntity.findAll", query = "select ws from WorkspaceEntity as ws" ),
    @NamedQuery( name = "WorkspaceEntity.findByName", query = "select ws from WorkspaceEntity as ws where ws.name = :name" ),
    @NamedQuery( name = "WorkspaceEntity.findAllNames", query = "select ws.name from WorkspaceEntity as ws" )} )
public class WorkspaceEntity {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    private Long id;

    @Column( name = "NAME", nullable = false, unique = false, length = 128, updatable = false )
    private String name;

    /**
     * 
     */
    public WorkspaceEntity() {
    }

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id Sets id to the specified value.
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
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
        if (obj instanceof WorkspaceEntity) {
            WorkspaceEntity that = (WorkspaceEntity)obj;
            if (!this.id.equals(that.id)) return false;
            if (!this.name.equals(that.name)) return false;
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
        return name;
    }

    /**
     * Find an existing workspace by its name, or create and return one if it does not already exist.
     * 
     * @param manager the entity manager
     * @param name the name of the workspace
     * @return the existing workspace
     * @throws IllegalArgumentException if the manager or name are null
     */
    public static WorkspaceEntity findByName( EntityManager manager,
                                              String name ) {
        return findByName(manager, name, true);
    }

    /**
     * Find an existing workspace by its name.
     * 
     * @param manager the entity manager
     * @param name the name of the workspace
     * @param createIfRequired if the workspace should be persisted if it does not yet exist
     * @return the existing workspace, or null if one does not exist
     * @throws IllegalArgumentException if the manager or name are null
     */
    public static WorkspaceEntity findByName( EntityManager manager,
                                              String name,
                                              boolean createIfRequired ) {
        CheckArg.isNotNull(manager, "manager");
        CheckArg.isNotNull(name, "name");
        Query query = manager.createNamedQuery("WorkspaceEntity.findByName");
        query.setParameter("name", name);
        try {
            return (WorkspaceEntity)query.getSingleResult();
        } catch (NoResultException e) {
            if (!createIfRequired) return null;
            WorkspaceEntity workspace = new WorkspaceEntity();
            workspace.setName(name);
            manager.persist(workspace);
            return workspace;
        }
    }

    /**
     * Find the set of names for the existing workspaces.
     * 
     * @param manager the entity manager
     * @return the names of the existing workspaces; never null
     * @throws IllegalArgumentException if the manager or name are null
     */
    @SuppressWarnings( "unchecked" )
    public static Set<String> findAllNames( EntityManager manager ) {
        CheckArg.isNotNull(manager, "manager");
        Query query = manager.createNamedQuery("WorkspaceEntity.findAllNames");
        List<String> names = query.getResultList();
        return new HashSet<String>(names);
    }
}
