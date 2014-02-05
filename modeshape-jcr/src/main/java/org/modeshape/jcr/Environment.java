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
package org.modeshape.jcr;

import java.io.IOException;
import javax.naming.NamingException;
import org.infinispan.manager.CacheContainer;
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
