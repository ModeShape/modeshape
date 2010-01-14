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

import java.util.UUID;
import javax.persistence.EntityManager;
import org.modeshape.common.util.CheckArg;
import org.modeshape.connector.store.jpa.Model;

/**
 * A utility class that provides easy access to the options stored within a database.
 */
public class StoreOptions {

    public static final class Dna {
        public static final String ROOT_NODE_UUID = "org.jboss.dna.store.rootNodeUuid";
        public static final String VERSION = "org.jboss.dna.store.version";
        public static final String MODEL = "org.jboss.dna.store.model";
    }

    public static final String ROOT_NODE_UUID = "org.modeshape.store.rootNodeUuid";
    public static final String VERSION = "org.modeshape.store.version";
    public static final String MODEL = "org.modeshape.store.model";

    private final EntityManager entityManager;

    public StoreOptions( EntityManager manager ) {
        CheckArg.isNotNull(manager, "manager");
        this.entityManager = manager;
    }

    public UUID getRootNodeUuid() {
        String value = getOption(ROOT_NODE_UUID);
        if (value == null) {
            // See if there is an option for an existing DNA uuid ...
            value = getOption(Dna.ROOT_NODE_UUID);
            if (value != null) {
                // There was, so set the ModeShape uuid ...
                setOption(ROOT_NODE_UUID, value);
            }
        }
        return value != null ? UUID.fromString(value) : null;
    }

    public void setRootNodeUuid( UUID uuid ) {
        CheckArg.isNotNull(uuid, "uuid");
        setOption(ROOT_NODE_UUID, uuid.toString());
    }

    public String getVersion() {
        String value = getOption(VERSION);
        if (value == null) {
            // See if there is an option for an existing DNA version ...
            value = getOption(Dna.VERSION);
            if (value != null) {
                // There was, so set the ModeShape version ...
                setOption(VERSION, value);
            }
        }
        return value;
    }

    public void setVersion( String version ) {
        setOption(VERSION, version);
    }

    public String getModelName() {
        String value = getOption(MODEL);
        if (value == null) {
            // See if there is an option for an existing DNA version ...
            value = getOption(Dna.MODEL);
            if (value != null) {
                // There was, so set the ModeShape version ...
                setOption(MODEL, value);
            }
        }
        return value;
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
