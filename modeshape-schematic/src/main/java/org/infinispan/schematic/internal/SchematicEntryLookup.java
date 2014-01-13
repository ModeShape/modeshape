/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infinispan.schematic.internal;

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
     * @param cacheContext cache context
     * @param key key under which the schematic value exists
     * @return an AtomicMap
     */
    public static SchematicEntry getSchematicValue( CacheContext cacheContext,
                                                    String key ) {
        return getSchematicValue(cacheContext, key, true, null);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cacheContext cache context
     * @param key key under which the atomic map exists
     * @param createIfAbsent if true, a new atomic map is created if one doesn't exist; otherwise null is returned if the map
     *        didn't exist.
     * @return an AtomicMap, or null if one did not exist.
     */
    public static SchematicEntry getSchematicValue( CacheContext cacheContext,
                                                    String key,
                                                    boolean createIfAbsent ) {
        return getSchematicValue(cacheContext, key, createIfAbsent, null);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cacheContext cache context
     * @param key key under which the atomic map exists
     * @param flagContainer a container to pass in per-invocation flags to the underlying cache. May be null if no flags are used.
     * @return an AtomicMap, or null if one did not exist.
     */
    public static SchematicEntry getSchematicValue( CacheContext cacheContext,
                                                    String key,
                                                    FlagContainer flagContainer ) {
        return getSchematicValue(cacheContext, key, true, flagContainer);
    }

    /**
     * Retrieves a schematic value from the given cache, stored under the given key.
     * 
     * @param cacheContext cache context
     * @param key key under which the atomic map exists
     * @param createIfAbsent if true, a new atomic map is created if one doesn't exist; otherwise null is returned if the map
     *        didn't exist.
     * @param flagContainer a container to pass in per-invocation flags to the underlying cache. May be null if no flags are used.
     * @return an AtomicMap, or null if one did not exist.
     */
    private static final SchematicEntry getSchematicValue( CacheContext cacheContext,
                                                           String key,
                                                           boolean createIfAbsent,
                                                           FlagContainer flagContainer ) {
        Cache<String, SchematicEntry> cache = cacheContext.getCache();
        SchematicEntry value = cache.get(key);
        if (value == null) {
            if (createIfAbsent) value = SchematicEntryLiteral.newInstance(cache, key);
            else return null;
        }
        SchematicEntryLiteral castValue = (SchematicEntryLiteral)value;
        return castValue.getProxy(cacheContext, key, flagContainer);
    }

    /**
     * Retrieves an atomic map from a given cache, stored under a given key, for reading only. The atomic map returned will not
     * support updates, and if the map did not in fact exist, an empty map is returned.
     * 
     * @param cacheContext cache context
     * @param key key under which the atomic map exists
     * @return an immutable, read-only map
     */
    public static SchematicEntry getReadOnlySchematicValue( CacheContext cacheContext,
                                                            String key ) {
        SchematicEntry existingValue = getSchematicValue(cacheContext, key, false, null);
        if (existingValue == null) existingValue = new SchematicEntryLiteral(key);
        return new ImmutableSchematicValue(existingValue);
    }

    /**
     * Removes the atomic map associated with the given key from the underlying cache.
     * 
     * @param cacheContext cache context
     * @param key key under which the atomic map exists
     */
    public static void removeSchematicValue( CacheContext cacheContext,
                                             String key ) {
        cacheContext.getCache().withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD).remove(key);
    }
}
