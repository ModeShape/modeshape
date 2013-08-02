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
package org.modeshape.web.jcr;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.Logger;
import org.modeshape.jcr.api.RepositoriesContainer;
import org.modeshape.jcr.api.ServletCredentials;

/**
 * Manager for accessing JCR Repository instances. This manager uses the idiomatic way to find JCR Repository (and ModeShape
 * Repositories) instances via the {@link ServiceLoader} and {@link org.modeshape.jcr.api.RepositoriesContainer} mechanism.
 */
@ThreadSafe
public class RepositoryManager {

    private static final Logger LOGGER = WebLogger.getLogger(RepositoryManager.class);
    private static final Map<String, Object> factoryParams = new HashMap<String, Object>();

    private static RepositoriesContainer repositoriesContainer;

    private RepositoryManager() {
    }

    /**
     * Initializes the repository factory. For more details, please see the {@link RepositoryManager class-level documentation}.
     * 
     * @param context the servlet context; may not be null
     * @see RepositoryManager
     */
    static synchronized void initialize( ServletContext context ) {
        CheckArg.isNotNull(context, "context");
        loadFactoryParameters(context);
        loadRepositoriesContainer();
    }

    private static void loadRepositoriesContainer() {
        Iterator<RepositoriesContainer> containersIterator = ServiceLoader.load(RepositoriesContainer.class).iterator();
        if (!containersIterator.hasNext()) {
            throw new IllegalStateException(
                    WebJcrI18n.repositoriesContainerNotFoundInClasspath.text(RepositoriesContainer.class.getName()));
        }
        //there shouldn't be more than 1 container
        repositoriesContainer = containersIterator.next();
    }

    private static void loadFactoryParameters( ServletContext context ) {
        factoryParams.clear();
        Enumeration<?> names = context.getInitParameterNames();
        if (names == null) {
            addParameter(RepositoriesContainer.URL, context);
            addParameter(RepositoriesContainer.REPOSITORY_NAME, context);
        } else {
            while (names.hasMoreElements()) {
                Object next = names.nextElement();
                if (next == null) continue;
                String name = next.toString();
                addParameter(name, context);
            }
        }
    }

    private static void addParameter( String name,
                                      ServletContext context ) {
        String value = context.getInitParameter(name);
        if (value != null) factoryParams.put(name, value);
    }

    /**
     * Get a JCR Session for the named workspace in the named repository, using the supplied HTTP servlet request for
     * authentication information.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param repositoryName the name of the repository in which the session is created
     * @param workspaceName the name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if the named repository does not exist or there was a problem obtaining the named repository
     */
    public static Session getSession( HttpServletRequest request,
                                      String repositoryName,
                                      String workspaceName ) throws RepositoryException {
        // Go through all the RepositoryFactory instances and try to create one ...
        Repository repository = getRepository(repositoryName);

        // If there's no authenticated user, try an anonymous login
        if (request == null || request.getUserPrincipal() == null) {
            return repository.login(workspaceName);
        }

        return repository.login(new ServletCredentials(request), workspaceName);
    }

    /**
     * Returns the {@link Repository} instance with the given name.
     * @param repositoryName a {@code non-null} string
     * @return a {@link Repository} instance, never {@code null}
     * @throws NoSuchRepositoryException if no repository with the given name exists.
     */
    public static Repository getRepository( String repositoryName ) throws NoSuchRepositoryException {
        Repository repository = null;
        try {
            repository = repositoriesContainer.getRepository(repositoryName, Collections.unmodifiableMap(factoryParams));
        } catch (RepositoryException e) {
            throw new NoSuchRepositoryException(WebJcrI18n.cannotInitializeRepository.text(repositoryName), e);
        }

        if (repository == null) {
            throw new NoSuchRepositoryException(WebJcrI18n.repositoryNotFound.text(repositoryName));
        }
        return repository;
    }

    /**
     * Returns a set with all the names of the available repositories.
     * @return a set with the names, never {@code null}
     */
    public static Set<String> getJcrRepositoryNames() {
        try {
            return repositoriesContainer.getRepositoryNames(Collections.unmodifiableMap(factoryParams));
        } catch (RepositoryException e) {
            LOGGER.error(e, WebJcrI18n.cannotLoadRepositoryNames.text());
            return Collections.emptySet();
        }
    }

    static void shutdown() {
        repositoriesContainer.shutdown();
    }
}
