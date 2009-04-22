/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.jbosscache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.naming.Context;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.config.ConfigurationException;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;

/**
 * This class represents a set of workspaces used by the {@link JBossCacheSource JBoss Cache connector}.
 */
@ThreadSafe
public class JBossCacheWorkspaces {

    private final String sourceName;
    private final ConcurrentHashMap<String, Cache<Name, Object>> caches = new ConcurrentHashMap<String, Cache<Name, Object>>();
    private final Set<String> initialNames;
    private final CacheFactory<Name, Object> cacheFactory;
    private final String defaultCacheFactoryConfigurationName;
    private final Context jndi;
    private final Set<String> workspaceNamesForJndiClassCastProblems = new HashSet<String>();
    private final Set<String> workspaceNamesForConfigurationNameProblems = new HashSet<String>();
    private final Lock writeLock = new ReentrantLock();

    /**
     * Create a new instance of the workspace and cache manager for the JBoss Cache connector.
     * 
     * @param sourceName the name of the source that uses this object; may not be null
     * @param cacheFactory the factory that should be used to create new caches; may not be null
     * @param defaultCacheFactoryConfigurationName the name of the configuration that is supplied to the {@link CacheFactory cache
     *        factory} to {@link CacheFactory#createCache(String) create the new cache} if the workspace name does not correspond
     *        to a configuration; may be null
     * @param initialNames the initial names for the workspaces; may be null or empty
     * @param jndiContext the JNDI context that should be used, or null if JNDI should not be used at all
     */
    public JBossCacheWorkspaces( String sourceName,
                                 CacheFactory<Name, Object> cacheFactory,
                                 String defaultCacheFactoryConfigurationName,
                                 Set<String> initialNames,
                                 Context jndiContext ) {
        assert sourceName != null;
        this.sourceName = sourceName;
        if (initialNames == null) initialNames = Collections.emptySet();
        this.initialNames = initialNames;
        this.cacheFactory = cacheFactory;
        this.defaultCacheFactoryConfigurationName = defaultCacheFactoryConfigurationName;
        this.jndi = jndiContext;
    }

    /**
     * Attempt to create a new workspace with the supplied name.
     * 
     * @param workspaceName the name of the new workspace, which may be a valid URI if the cache is to be found in JNDI
     * @return the new workspace, or null if there is already a workspace with the name
     */
    public Cache<Name, Object> createWorkspace( String workspaceName ) {
        try {
            writeLock.lock();
            // First, see if there is already an existing cache ...
            Cache<Name, Object> cache = caches.get(workspaceName);
            if (cache != null) {
                // There is already a workspace, so we can't create ...
                return null;
            }

            // There isn't already a cache, but next check the list of initial names ...
            if (initialNames.contains(workspaceName)) {
                // The workspace already exists, but we just haven't accessed it yet
                return null;
            }

            // Time to create a new cache. First see if we're supposed to use a cache already in JNDI ...
            cache = findCacheInJndi(workspaceName);
            if (cache == null) {
                // Try to create one ...
                cache = createNewCache(workspaceName);
            }

            if (cache != null) {
                // Manage this cache ...
                Cache<Name, Object> existing = caches.putIfAbsent(workspaceName, cache);
                if (existing != null) cache = existing;
            }
            return cache; // may still be null if we couldn't create a new cache

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Get the cache that corresponds to the supplied workspace name, and optionally create a new cache if no such cache already
     * exists. This method first checks for {@link Cache} instances previously found for the same workspace name. If no cache is
     * found, this method then checks whether the supplied workspace name is a valid URI, and if so the method looks for a
     * {@link Cache} instance in JNDI at that URI. If none is found (or the name is not a valid URI), this method then creates a
     * new {@link Cache} instance using the {@link CacheFactory} supplied in the constructor.
     * 
     * @param workspaceName the name of the workspace, which may be a valid URI if the cache is to be found in JNDI
     * @param createIfMissing true if the cache should be created if no such cache already exists
     * @return the cache that corresponds to the workspace with the supplied name, or null if there is no cache for that workspace
     *         (and one could not be or was not created)
     */
    public Cache<Name, Object> getWorkspace( String workspaceName,
                                             boolean createIfMissing ) {
        // First, see if there is already an existing cache ...
        Cache<Name, Object> cache = caches.get(workspaceName);
        if (cache != null) return cache;

        try {
            writeLock.lock();
            // Ensure one didn't get created while we waited for the lock ...
            cache = caches.get(workspaceName);
            if (cache != null) return cache;

            // We've not yet come across the cache for the workspace.

            // Check whether the workspace name was one of the initial set of names...
            if (this.initialNames.contains(workspaceName)) {
                // This workspace/cache was one of those defined at startup to be available,
                // so we really don't consider this to be "creating a new cache"; it's just the first time we've used it
                // and we're lazily finding the instances. So, just mark 'createIfMissing' to true and continue ...
                createIfMissing = true;
            }

            if (!createIfMissing) return null;

            // First see if we can find a cache in JNDI ...
            cache = findCacheInJndi(workspaceName);

            if (cache == null) {
                // Try to create one ...
                cache = createNewCache(workspaceName);
            }

            if (cache != null) {
                Cache<Name, Object> existing = caches.putIfAbsent(workspaceName, cache);
                if (existing != null) cache = existing;
            }
            return cache; // may still be null if we couldn't create a new cache
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Attempt to find an existing {@link Cache} object in JNDI, using the supplied workspace name as the JNDI name.
     * 
     * @param workspaceName the name of the workspace
     * @return the cache found in JNDI that corresponds to the workspace name
     */
    @SuppressWarnings( "unchecked" )
    @GuardedBy( "writeLock" )
    protected Cache<Name, Object> findCacheInJndi( String workspaceName ) {
        assert workspaceName != null;
        if (jndi == null) return null;

        // Try to look up the cache instance in JDNI ...
        workspaceName = workspaceName.trim();
        if (workspaceName.length() != 0) {
            try {
                new URI(workspaceName.trim());
                Object object = null;
                try {
                    object = jndi.lookup(workspaceName);
                    if (object != null && object instanceof Cache) {
                        return (Cache<Name, Object>)object;
                    }
                } catch (ClassCastException err) {
                    // The object found in JNDI was not a JBoss Cache instance ...
                    if (this.workspaceNamesForJndiClassCastProblems.add(workspaceName)) {
                        // Log this problem only the first time ...
                        String className = object != null ? object.getClass().getName() : "null";
                        I18n msg = JBossCacheConnectorI18n.objectFoundInJndiWasNotCache;
                        Logger.getLogger(getClass()).warn(msg, workspaceName, sourceName, className);
                    }
                } catch (Throwable error) {
                    // try loading
                    if (error instanceof RuntimeException) throw (RuntimeException)error;
                    throw new RepositorySourceException(sourceName, error);
                }

            } catch (URISyntaxException err) {
                // Not a valid URI, so just continue ...
            }
        }
        return null;
    }

    /**
     * Method that is responsible for attempting to create a new cache given the supplied workspace name. Note that this is
     * probably called at most once for each workspace name (except if this method fails to create a cache for a given workspace
     * name).
     * 
     * @param workspaceName the name of the workspace
     * @return the new cache that corresponds to the workspace name
     */
    @GuardedBy( "writeLock" )
    protected Cache<Name, Object> createNewCache( String workspaceName ) {
        assert workspaceName != null;
        if (this.cacheFactory == null) return null;

        // Try to create the cache using the workspace name as the configuration ...
        try {
            return this.cacheFactory.createCache(workspaceName);
        } catch (ConfigurationException error) {
            // The workspace name is probably not the name of a configuration ...
            I18n msg = JBossCacheConnectorI18n.workspaceNameWasNotValidConfiguration;
            Logger.getLogger(getClass()).debug(msg.text(workspaceName, error.getMessage()));
        }

        if (this.defaultCacheFactoryConfigurationName != null) {
            // Try to create the cache using the default configuration name ...
            try {
                return this.cacheFactory.createCache(this.defaultCacheFactoryConfigurationName);
            } catch (ConfigurationException error) {
                // The default configuration name is not valid ...
                if (this.workspaceNamesForConfigurationNameProblems.add(workspaceName)) {
                    // Log this problem only the first time ...
                    I18n msg = JBossCacheConnectorI18n.defaultCacheFactoryConfigurationNameWasNotValidConfiguration;
                    Logger.getLogger(getClass()).debug(msg.text(workspaceName));
                }
            }
        }

        // Just create a new cache with the default configuration ...
        return this.cacheFactory.createCache();
    }

    /**
     * Return an immutable set of names for the currently available workspaces.
     * 
     * @return the immutable set of workspace names; never null
     */
    public Set<String> getWorkspaceNames() {
        Set<String> names = new HashSet<String>();
        if (!initialNames.isEmpty()) names.addAll(initialNames);
        names.addAll(caches.keySet());
        return Collections.unmodifiableSet(names);
    }

    /**
     * Remove the cache that corresponds to the supplied workspace name as no longer being available. This will remove the cache
     * even if the workspace name is one of the "initial names" provided to this object's constructor.
     * 
     * @param workspaceName the name of the existing workspace that is to be removed
     * @return true if there was an existing workspace that was removed by this call, or false if there was no workspace with the
     *         supplied name
     */
    public boolean removeWorkspace( String workspaceName ) {
        try {
            writeLock.lock();

            // Remove this from both the cache and initialNames ...
            boolean removed = initialNames.remove(workspaceName);
            if (caches.remove(workspaceName) != null) removed = true;
            return removed;
        } finally {
            writeLock.unlock();
        }
    }
}
