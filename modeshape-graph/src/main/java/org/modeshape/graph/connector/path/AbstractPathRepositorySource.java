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
package org.modeshape.graph.connector.path;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.path.cache.NoCachePolicy;
import org.modeshape.graph.connector.path.cache.PathCachePolicy;
import org.modeshape.graph.connector.path.cache.PathRepositoryCache;

/**
 * Basic implementation of the trivial {@link PathRepositorySource} methods and the {@link org.modeshape.graph.connector.path path
 * repository cache life cycle}.
 */
@ThreadSafe
public abstract class AbstractPathRepositorySource implements PathRepositorySource {

    private static final long serialVersionUID = 1L;

    /**
     * The default UUID that is used for root nodes in a store.
     */
    public static final String DEFAULT_ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    /**
     * The default number of times that a request that failed due to system error should be retried
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default cache policy for this repository source (no caching)
     */
    public static final PathCachePolicy DEFAULT_CACHE_POLICY = new NoCachePolicy();

    protected int retryLimit = DEFAULT_RETRY_LIMIT;
    protected String name;

    protected transient RepositoryContext repositoryContext;
    protected transient UUID rootNodeUuid = UUID.fromString(DEFAULT_ROOT_NODE_UUID);
    protected transient PathCachePolicy cachePolicy = DEFAULT_CACHE_POLICY;

    private transient PathRepositoryCache repositoryCache = new PathRepositoryCache(cachePolicy);

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.path.PathRepositorySource#areUpdatesAllowed()
     */
    public boolean areUpdatesAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.path.PathRepositorySource#getRepositoryContext()
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        CheckArg.isNotNull(context, "context");
        this.repositoryContext = context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.path.PathRepositorySource#getCachePolicy()
     */
    public PathCachePolicy getCachePolicy() {
        return this.cachePolicy;
    }

    /**
     * Sets the cache policy for the repository and replaces the path repository cache with a new path repository cache tied to
     * the new cache policy
     * 
     * @param cachePolicy the new cache policy; may not be null
     */
    public void setCachePolicy( PathCachePolicy cachePolicy ) {
        CheckArg.isNotNull(cachePolicy, "cachePolicy");

        PathRepositoryCache oldCache = repositoryCache;
        this.cachePolicy = cachePolicy;
        this.repositoryCache = new PathRepositoryCache(cachePolicy);

        oldCache.close();
    }

    /**
     * @return rootNodeUuid
     */
    public UUID getRootNodeUuid() {
        return rootNodeUuid;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     * @throws IllegalArgumentException if the string value cannot be converted to UUID
     */
    public void setRootNodeUuid( String rootNodeUuid ) {
        if (rootNodeUuid != null && rootNodeUuid.trim().length() == 0) rootNodeUuid = DEFAULT_ROOT_NODE_UUID;
        this.rootNodeUuid = UUID.fromString(rootNodeUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the repository source. The name should be unique among loaded repository sources.
     * 
     * @param name the new name for the repository source; may not be empty
     */
    public void setName( String name ) {
        if (name != null) {
            name = name.trim();
            if (name.length() == 0) name = null;
        }

        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int limit)
     */
    public void setRetryLimit( int limit ) {
        this.retryLimit = limit < 0 ? 0 : limit;
    }

    /**
     * Extracts the values from the given reference, automatically translating {@link BinaryRefAddr} instances into the
     * deserialized classes that they represent.
     * 
     * @param ref the reference from which the values should be extracted
     * @return a map of value names to values from the reference
     * @throws IOException if there is an error deserializing a {@code BinaryRefAddr}
     * @throws ClassNotFoundException if a serialized class cannot be deserialized because its class is not in the classpath
     */
    protected Map<String, Object> valuesFrom( Reference ref ) throws IOException, ClassNotFoundException {
        Map<String, Object> values = new HashMap<String, Object>();

        Enumeration<?> en = ref.getAll();
        while (en.hasMoreElements()) {
            RefAddr subref = (RefAddr)en.nextElement();
            if (subref instanceof StringRefAddr) {
                String key = subref.getType();
                Object value = subref.getContent();
                if (value != null) values.put(key, value.toString());
            } else if (subref instanceof BinaryRefAddr) {
                String key = subref.getType();
                Object value = subref.getContent();
                if (value instanceof byte[]) {
                    // Deserialize ...
                    ByteArrayInputStream bais = new ByteArrayInputStream((byte[])value);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    value = ois.readObject();
                    values.put(key, value);
                }
            }
        }

        return values;
    }

    /**
     * @return the active path repository cache; never null
     */
    public PathRepositoryCache getPathRepositoryCache() {
        return repositoryCache;
    }
}
