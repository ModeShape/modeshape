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
package org.modeshape.jcr;

import java.io.IOException;
import javax.naming.NamingException;
import org.infinispan.manager.CacheContainer;
import org.jgroups.Channel;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A basic environment in which a repository operates. The logical names supplied to these methods are typically obtained directly
 * from the {@link RepositoryConfiguration}.
 */
@ThreadSafe
public interface Environment {

    /**
     * Get the cache container with the given name. Note that the name might be a logical name or it might refer to the location
     * of an Infinispan configuration; the exact semantics is dependent upon the implementation.
     * 
     * @param name the name of the cache container; may be null
     * @return the cache container; never null
     * @throws IOException if there is an error accessing any resources required to start the container
     * @throws NamingException if there is an error accessing JNDI (if that is used in the implementation)
     */
    CacheContainer getCacheContainer( String name ) throws IOException, NamingException;

    /**
     * Get the JGroups channel with the given logical name.
     * 
     * @param name the name of the channel; may not be null
     * @return the channel, or null if there is no such channel and the environment does not support clustering
     * @throws Exception if there is a problem obtaining the named channel
     */
    Channel getChannel( String name ) throws Exception;

    /**
     * Get a classloader given the supplied set of logical classpath entries, which the implementation can interpret however it
     * needs.
     * 
     * @param fallbackLoader the classloader that should be used is the fallback class loader
     * @param classpathEntries the logical classpath entries
     * @return the classloader
     */
    ClassLoader getClassLoader( ClassLoader fallbackLoader,
                                String... classpathEntries );

    /**
     * Shutdown this environment, allowing it to reclaim any resources.
     */
    void shutdown();

}
