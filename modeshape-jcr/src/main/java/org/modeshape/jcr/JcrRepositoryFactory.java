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

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * Service provider for the JCR2 {@code RepositoryFactory} interface. This class provides a single public method,
 * {@link #getRepository(Map)}, that allows for a runtime link to a ModeShape JCR repository.
 * <p>
 * The canonical way to get a reference to this class is to use the {@link ServiceLoader}:
 * 
 * <pre>
 * String configUrl = ... ; // URL that points to your configuration file
 * Map parameters = Collections.singletonMap(JcrRepositoryFactory.URL, configUrl);
 * Repository repository;
 * 
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * It is also possible to instantiate this class directly.
 * 
 * <pre>
 * RepositoryFactory repoFactory = new JcrRepositoryFactory();    
 * String url = ... ; // URL that points to your configuration file
 * Map params = Collections.singletonMap(JcrRepositoryFactory.URL, url);
 * 
 * Repository repository = repoFactory.getRepository(params);]]></programlisting>
 * </pre>
 * 
 * </p>
 * <p>
 * Several URL formats are supported:
 * <ul>
 * <li><strong>JNDI location of repository</strong> - The URL contains the location in JNDI of an existing
 * <code>javax.jcr.Repository</code> instance. For example, "<code>jndi:jcr/local/my_repository</code>" is a URL that identifies
 * the JCR repository located in JNDI at the name "jcr/local/my_repository". Note that the use of such URLs requires that the
 * repository already be registered in JNDI at that location.</li>
 * <li><strong>JNDI location of engine and repository name</strong> - The URL contains the location in JNDI of an existing
 * ModeShape {@link org.modeshape.jcr.ModeShapeEngine engine} instance and the name of the <code>javax.jcr.Repository</code>
 * repository as a URL query parameter. For example, "<code>jndi:jcr/local?repositoryName=my_repository</code>" identifies a
 * ModeShape engine registered in JNDI at "jcr/local", and looks in that engine for a JCR repository named "
 * <code>my_repository</code>".</li>
 * <li><strong>Location of a repository configuration</strong> - The URL contains a location that is resolvable to a configuration
 * file for the repository. If the configuration file has not already been loaded by the factory, then the configuration file is
 * read and used to deploy a new repository; subsequent uses of the same URL will return the previously deployed repository
 * instance. Several URL schemes are supported, including <code>classpath:</code>, "<code>file:</code>", <code>http:</code> and
 * any other URL scheme that can be {@link URL#openConnection() resolved and opened}. For example, "
 * <code>file://path/to/myRepoConfig.json</code>" identifies the file on the file system at the absolute path "
 * <code>/path/to/myRepoConfig.json</code>"; "<code>classpath://path/to/myRepoConfig.json</code>" identifies the file at "
 * <code>/path/to/myRepoConfig.json</code>" on the classpath, and "<code>http://www.example.com/path/to/myRepoConfig.json</code>
 * " identifies the file "<code>myRepoConfig.json</code>" at the given URL.</li>
 * </ul>
 * </p>
 * 
 * @see #getRepository(Map)
 * @see RepositoryFactory#getRepository(Map)
 */
@ThreadSafe
public class JcrRepositoryFactory implements RepositoryFactory {

    private static final Logger LOG = Logger.getLogger(JcrRepositoryFactory.class);

    /**
     * The container which hold the engine and which is responsible for initializing & returning the repository.
     */
    private static final JcrRepositoriesContainer CONTAINER = new JcrRepositoriesContainer();

    /**
     * Returns a reference to the appropriate repository for the given parameter map, if one exists. Although the
     * {@code parameters} map can have any number of entries, this method only considers the entry with the key
     * JcrRepositoryFactory#URL.
     * <p>
     * The value of this key is treated as a URL with the format {@code PROTOCOL://PATH[?repositoryName=REPOSITORY_NAME]} where
     * PROTOCOL is "jndi" or "file", PATH is the JNDI name of the {@link ModeShapeEngine} or the path to the configuration file,
     * and REPOSITORY_NAME is the name of the repository to return if there is more than one JCR repository in the given
     * {@link ModeShapeEngine} or configuration file.
     * </p>
     * 
     * @param parameters a map of parameters to use to look up the repository; may be null
     * @return the repository specified by the value of the entry with key {@link #URL}, or null if any of the following are true:
     *         <ul>
     *         <li>the parameters map is empty; or,</li>
     *         <li>there is no parameter with the {@link #URL}; or,</li>
     *         <li>the value for the {@link #URL} key is null or cannot be parsed into a ModeShape JCR URL; or,</li>
     *         <li>the ModeShape JCR URL is parseable but does not point to a {@link ModeShapeEngine} (in the JNDI tree) or a
     *         configuration file (in the classpath or file system); or,</li>
     *         <li>or there is an error starting up the {@link ModeShapeEngine} with the given configuration information.</li>
     *         <ul>
     * @see RepositoryFactory#getRepository(Map)
     */
    @Override
    @SuppressWarnings( "rawtypes" )
    public Repository getRepository( Map parameters ) throws RepositoryException {
        LOG.debug("Trying to load ModeShape JCR Repository with parameters: " + parameters);
        return CONTAINER.getRepository(null, parameters);
    }

    @Override
    @Deprecated
    public Future<Boolean> shutdown() {
        return CONTAINER.shutdown();
    }

    @Override
    @Deprecated
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException {
        return CONTAINER.shutdown(timeout, unit);
    }

    @Override
    @Deprecated
    public Repository getRepository( String repositoryName ) throws RepositoryException {
        return CONTAINER.getRepository(repositoryName, null);
    }

    @Override
    @Deprecated
    public Set<String> getRepositoryNames() {
        try {
            return CONTAINER.getRepositoryNames(null);
        } catch (RepositoryException e) {
            LOG.debug(e, "Cannot retrieve the names of all the repositories");
            return Collections.emptySet();
        }
    }
}
