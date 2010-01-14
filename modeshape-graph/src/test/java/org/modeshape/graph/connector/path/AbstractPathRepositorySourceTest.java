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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.naming.Reference;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.path.cache.CacheStatistics;
import org.modeshape.graph.connector.path.cache.PathCachePolicy;
import org.modeshape.graph.connector.path.cache.PathRepositoryCache;
import org.modeshape.graph.connector.path.cache.WorkspaceCache;
import org.modeshape.graph.property.Path;
import org.junit.Before;
import org.junit.Test;

public class AbstractPathRepositorySourceTest {

    private AbstractPathRepositorySource source;

    @SuppressWarnings( "serial" )
    @Before
    public void beforeEach() throws Exception {
        source = new AbstractPathRepositorySource() {

            public String getDefaultWorkspaceName() {
                return null;
            }

            public void setUpdatesAllowed( boolean updatesAllowed ) {
            }

            public RepositorySourceCapabilities getCapabilities() {
                return null;
            }

            public RepositoryConnection getConnection() throws RepositorySourceException {
                return null;
            }

            public Reference getReference() {
                return null;
            }
        };
    }

    @Test
    public void shouldProvideDefaultCachePolicy() throws Exception {
        assertThat(source.getCachePolicy(), is(notNullValue()));
    }

    @Test
    public void shouldProvideDefaultPathRepositoryCache() throws Exception {
        assertThat(source.getPathRepositoryCache(), is(notNullValue()));
    }

    @Test
    public void shouldMakeNewRepositoryCacheWhenCachePolicyIsChanged() throws Exception {
        PathCachePolicy policy1 = new MockCachePolicy();
        source.setCachePolicy(policy1);

        PathRepositoryCache cache1 = source.getPathRepositoryCache();
        assertThat(cache1, is(notNullValue()));

        PathCachePolicy policy2 = new MockCachePolicy();
        source.setCachePolicy(policy2);

        assertThat(source.getCachePolicy(), is(policy2));
        assertThat(source.getCachePolicy(), is(not(policy1)));

        PathRepositoryCache cache2 = source.getPathRepositoryCache();
        assertThat(cache2, is(notNullValue()));
        assertThat(cache2, is(not(cache1)));
    }

    @Test
    public void shouldCloseOldWorkspaceCacheWhenCachePolicyIsChanged() throws Exception {
        PathCachePolicy policy1 = new MockCachePolicy();
        source.setCachePolicy(policy1);

        PathRepositoryCache repositoryCache = source.getPathRepositoryCache();
        MockWorkspaceCache cache = (MockWorkspaceCache)repositoryCache.getCache("foo");
        assertThat(cache, is(notNullValue()));

        source.setCachePolicy(new MockCachePolicy());

        assertThat(cache.closed, is(true));
    }

    @SuppressWarnings( "serial" )
    class MockCachePolicy implements PathCachePolicy {

        public Class<? extends WorkspaceCache> getCacheClass() {
            return MockWorkspaceCache.class;
        }

        public boolean shouldCache( PathNode node ) {
            return false;
        }

        public long getTimeToLive() {
            return 0;
        }

    }

    public static class MockWorkspaceCache implements WorkspaceCache {

        boolean closed = false;
        PathCachePolicy policy;

        public void initialize( PathCachePolicy policy,
                                String workspaceName ) {
            this.policy = policy;
        }

        public void clearStatistics() {
        }

        public void close() {
            closed = true;
        }

        public PathNode get( Path path ) {
            return null;
        }

        public CacheStatistics getStatistics() {
            return null;
        }

        public void invalidate( Path path ) {
        }

        public void set( PathNode node ) {
        }

    }
}
