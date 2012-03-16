/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.internal;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.context.FlagContainer;
import org.infinispan.schematic.SchematicEntry;

/**
 * A helper that locates or safely constructs and registers schematic values with a given cache. This should be the <b>only</b>
 * way {@link SchematicEntry}s are created/retrieved, to prevent concurrent creation, registration and possibly overwriting of
 * such a value within the cache.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @see SchematicEntry
 * @since 5.1
 */
public class SchematicEntryLookup {

    /**
     * Retrieves a schematic value from the given cache, stored under the given key. If a schematic value did not exist, one is
     * created and registered in an atomic fashion.
     * 
     * @param cache underlying cache
     * @param key key under which the schematic value exists
     * @return an AtomicMap
     */
    public static SchematicEntry getSchematicValue( Cache<String, SchematicEntry> cache,
                                                    String key ) {
        return getSchematicValue(cache, key, true, null);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cache underlying cache
     * @param key key under which the atomic map exists
     * @param createIfAbsent if true, a new atomic map is created if one doesn't exist; otherwise null is returned if the map
     *        didn't exist.
     * @return an AtomicMap, or null if one did not exist.
     */
    public static SchematicEntry getSchematicValue( Cache<String, SchematicEntry> cache,
                                                    String key,
                                                    boolean createIfAbsent ) {
        return getSchematicValue(cache, key, createIfAbsent, null);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cache underlying cache
     * @param key key under which the atomic map exists
     * @param flagContainer a container to pass in per-invocation flags to the underlying cache. May be null if no flags are used.
     * @return an AtomicMap, or null if one did not exist.
     */
    public static SchematicEntry getSchematicValue( Cache<String, SchematicEntry> cache,
                                                    String key,
                                                    FlagContainer flagContainer ) {
        return getSchematicValue(cache, key, true, flagContainer);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cache underlying cache
     * @param key key under which the atomic map exists
     * @param createIfAbsent if true, a new atomic map is created if one doesn't exist; otherwise null is returned if the map
     *        didn't exist.
     * @param flagContainer a container to pass in per-invocation flags to the underlying cache. May be null if no flags are used.
     * @return an AtomicMap, or null if one did not exist.
     */
    private static final SchematicEntry getSchematicValue( Cache<String, SchematicEntry> cache,
                                                           String key,
                                                           boolean createIfAbsent,
                                                           FlagContainer flagContainer ) {
        SchematicEntry value = cache.get(key);
        if (value == null) {
            if (createIfAbsent) value = SchematicEntryLiteral.newInstance(cache, key);
            else return null;
        }
        SchematicEntryLiteral castValue = (SchematicEntryLiteral)value;
        AdvancedCache<String, SchematicEntry> advCache = cache.getAdvancedCache();
        return castValue.getProxy(advCache, key, flagContainer);
    }

    /**
     * Retrieves an atomic map from a given cache, stored under a given key, for reading only. The atomic map returned will not
     * support updates, and if the map did not in fact exist, an empty map is returned.
     * 
     * @param cache underlying cache
     * @param key key under which the atomic map exists
     * @return an immutable, read-only map
     */
    public static SchematicEntry getReadOnlySchematicValue( Cache<String, SchematicEntry> cache,
                                                            String key ) {
        SchematicEntry existingValue = getSchematicValue(cache, key, false, null);
        if (existingValue == null) existingValue = new SchematicEntryLiteral(key);
        return new ImmutableSchematicValue(existingValue);
    }

    /**
     * Removes the atomic map associated with the given key from the underlying cache.
     * 
     * @param cache underlying cache
     * @param key key under which the atomic map exists
     */
    public static void removeSchematicValue( Cache<String, SchematicEntry> cache,
                                             String key ) {
        cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD).remove(key);
    }
}
