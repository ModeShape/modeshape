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
package org.jboss.dna.connector.store.jpa.util;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.connector.store.jpa.Model;

/**
 * @author Randall Hauch
 */
public class StoreOptions {

    public static final String ROOT_NODE_UUID = "org.jboss.dna.store.rootNodeUuid";
    public static final String VERSION = "org.jboss.dna.store.version";
    public static final String MODEL = "org.jboss.dna.store.model";

    private final EntityManager entityManager;

    public StoreOptions( EntityManager manager ) {
        CheckArg.isNotNull(manager, "manager");
        this.entityManager = manager;
    }

    public UUID getRootNodeUuid() {
        String value = getOption(ROOT_NODE_UUID);
        return value != null ? UUID.fromString(value) : null;
    }

    public void setRootNodeUuid( UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        setOption(ROOT_NODE_UUID, uuid.toString());
    }

    public String getVersion() {
        return getOption(VERSION);
    }

    public void setVersion( String version ) {
        setOption(VERSION, version);
    }

    public String getModelName() {
        return getOption(MODEL);
    }

    public void setModelName( Model model ) {
        String modelName = model != null ? model.getName() : null;
        setOption(MODEL, modelName);
    }

    public String getOption( String name ) {
        StoreOptionEntity entity = entityManager.find(StoreOptionEntity.class, name);
        return entity != null ? entity.getValue() : null;
    }

    public void setOption( String name,
                           String value ) {
        CheckArg.isNotEmpty(name, "name");
        if (value != null) value = value.trim();
        StoreOptionEntity entity = entityManager.find(StoreOptionEntity.class, name);
        if (entity == null) {
            if (value != null) {
                // There is no existing entity, but there is a valid value ...
                entity = new StoreOptionEntity(name, value);
                entityManager.persist(entity);
            }
        } else {
            if (value != null) {
                // Set value on the entity ...
                entity.setValue(value);
            } else {
                // The existing entity is to be removed ...
                entityManager.remove(entity);
            }
        }
    }

    public void removeOption( String name ) {
        StoreOptionEntity entity = new StoreOptionEntity(name);
        entityManager.remove(entity);
    }
}
