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
import java.util.HashSet;
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
import org.modeshape.jcr.api.JndiRepositoryFactory;
import org.modeshape.jcr.api.NamedRepository;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jcr.api.RepositoryFactory;
import org.modeshape.jcr.api.ServletCredentials;

/**
 * Manager for accessing JCR Repository instances. This manager uses the idiomatic way to find JCR Repository (and ModeShape
 * Repositories) instances via the ServiceLoader and JCR RepositoryFactory mechanism.
 */
@ThreadSafe
public class RepositoryManager {

    private static final Map<String, Object> factoryParams = new HashMap<String, Object>();
    private static final Map<String, Object> immutableFactoryParams = Collections.unmodifiableMap(factoryParams);

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
        factoryParams.clear();
        Enumeration<?> names = context.getInitParameterNames();
        if (names == null) {
            addParameter(factoryParams, RepositoryFactory.URL, context);
            addParameter(factoryParams, RepositoryFactory.REPOSITORY_NAME, context);
        } else {
            while (names.hasMoreElements()) {
                Object next = names.nextElement();
                if (next == null) continue;
                String name = next.toString();
                addParameter(factoryParams, name, context);
            }
        }
    }

    private static final void addParameter( Map<String, Object> params,
                                            String name,
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
        Repository repository = null;
        boolean found = false;
        for (javax.jcr.RepositoryFactory factory : ServiceLoader.load(javax.jcr.RepositoryFactory.class)) {
            found = true;
            if (factory instanceof Repositories) {
                Repositories repositories = (Repositories)factory;
                try {
                    repository = repositories.getRepository(repositoryName);
                    break; // found it, so break out of the loop ...
                } catch (RepositoryException e) {
                    // do nothing ...
                }
            }
            // Try loading the repository via the parameters ...
            try {
                Map<String, Object> params = new HashMap<String, Object>(immutableFactoryParams);
                params.put(org.modeshape.jcr.api.RepositoryFactory.REPOSITORY_NAME, repositoryName);
                repository = factory.getRepository(params);
                if (repository != null) break;
            } catch (RepositoryException e) {
                // do nothing ...
            }
        }

        if (!found) {
            throw new IllegalStateException("No javax.jcr.RepositoryFactory implementation on the classpath");
        }

        if (repository == null) {
            throw new NoSuchRepositoryException("No repository named '" + repositoryName + "' was found");
        }

        // If there's no authenticated user, try an anonymous login
        if (request == null || request.getUserPrincipal() == null) {
            return repository.login(workspaceName);
        }

        return repository.login(new ServletCredentials(request), workspaceName);
    }

    public static Set<String> getJcrRepositoryNames() {
        Set<String> names = new HashSet<String>();

        // Go through all the RepositoryFactory instances and see if any can be shutdown ...
        boolean found = false;
        for (javax.jcr.RepositoryFactory factory : ServiceLoader.load(javax.jcr.RepositoryFactory.class)) {
            found = true;
            if (factory instanceof Repositories) {
                Repositories repositories = (Repositories)factory;
                Set<String> someNames = repositories.getRepositoryNames();
                if (someNames.isEmpty()) {
                    // It might not be initialized, so try creating the repository ...
                    try {
                        Repository repository = factory.getRepository(immutableFactoryParams);
                        if (repository instanceof NamedRepository) {
                            String name = ((NamedRepository)repository).getName();
                            names.add(name);
                        }
                    } catch (RepositoryException e) {
                        // do nothing ...
                    }
                }
                names.addAll(repositories.getRepositoryNames());
            } else if (factory instanceof JndiRepositoryFactory) {
                JndiRepositoryFactory jndiFactory = (JndiRepositoryFactory)factory;
                try {
                    names.addAll(jndiFactory.getRepositoryNames(immutableFactoryParams));
                } catch (RepositoryException e) {
                    // do nothing ...
                }
            } else {
                try {
                    Repository repository = factory.getRepository(immutableFactoryParams);
                    if (repository instanceof NamedRepository) {
                        String name = ((NamedRepository)repository).getName();
                        names.add(name);
                    }
                } catch (RepositoryException e) {
                    // do nothing ...
                }
            }
        }
        if (!found) {
            throw new IllegalStateException("No RepositoryFactory implementation on the classpath");
        }

        return names;
    }

    static void shutdown() {
        // Go through all the RepositoryFactory instances and see if any can be shutdown ...
        for (javax.jcr.RepositoryFactory factory : ServiceLoader.load(javax.jcr.RepositoryFactory.class)) {
            if (factory instanceof org.modeshape.jcr.api.RepositoryFactory) {
                org.modeshape.jcr.api.RepositoryFactory modeShapeFactory = (org.modeshape.jcr.api.RepositoryFactory)factory;
                modeShapeFactory.shutdown();
            }
        }
    }
}
