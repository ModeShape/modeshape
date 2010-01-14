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
package org.modeshape.connector.store.jpa.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.connector.store.jpa.model.common.WorkspaceEntity;

/**
 * A manager and cache for {@link WorkspaceEntity} objects.
 */
@NotThreadSafe
public class Workspaces {

    private final EntityManager entityManager;
    private final Map<String, WorkspaceEntity> cache = new HashMap<String, WorkspaceEntity>();

    public Workspaces( EntityManager manager ) {
        this.entityManager = manager;
    }

    /**
     * Create a workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace; may not be null
     * @return the workspace entity, or null if there already was a workspace with the supplied name
     */
    public WorkspaceEntity create( String workspaceName ) {
        assert workspaceName != null;
        WorkspaceEntity entity = cache.get(workspaceName);
        if (entity != null) return null;
        entity = WorkspaceEntity.findByName(entityManager, workspaceName, false);
        if (entity != null) return null;
        // Create one ...
        entity = WorkspaceEntity.findByName(entityManager, workspaceName, true);
        cache.put(workspaceName, entity);
        return entity;
    }

    /**
     * Get the workspace with the supplied name, and optionally create a new one if missing.
     * 
     * @param workspaceName the name of the workspace; never null
     * @param createIfRequired true if the workspace should be created if there is no existing workspace with the supplied name
     * @return the workspace entity, or null if no workspace existed with the supplied name and <code>createIfRequired</code> was
     *         false
     */
    public WorkspaceEntity get( String workspaceName,
                                boolean createIfRequired ) {
        WorkspaceEntity entity = cache.get(workspaceName);
        if (entity == null) {
            entity = WorkspaceEntity.findByName(entityManager, workspaceName, createIfRequired);
            if (entity != null) {
                cache.put(workspaceName, entity);
            }
        }
        return entity;
    }

    /**
     * Find the set of names for the existing workspaces.
     * 
     * @return the set of names; never null
     */
    public Set<String> getWorkspaceNames() {
        return WorkspaceEntity.findAllNames(entityManager);
    }

    /**
     * Remove the entity representation of the workspace. This does not remove any other data associated with the workspace other
     * than the {@link WorkspaceEntity} record.
     * 
     * @param workspaceName the name of the workspace; may not be null
     * @return true if the workspace record was found and removed, or false if there was no workspace with the supplied name
     */
    public boolean destroy( String workspaceName ) {
        assert workspaceName != null;
        WorkspaceEntity entity = cache.remove(workspaceName);
        if (entity == null) {
            entity = WorkspaceEntity.findByName(entityManager, workspaceName, false);
        }
        if (entity != null) {
            entityManager.remove(entity);
        }
        return entity != null;
    }
}
