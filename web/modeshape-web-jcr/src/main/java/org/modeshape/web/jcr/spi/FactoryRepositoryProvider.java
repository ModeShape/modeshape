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
package org.modeshape.web.jcr.spi;

import java.util.ServiceLoader;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.api.RepositoryFactory;
import org.modeshape.jcr.api.SecurityContextCredentials;
import org.modeshape.web.jcr.ServletSecurityContext;

/**
 * Repository provider backed by the ModeShape {@link RepositoryFactory} implementation.
 * <p>
 * The provider instantiates a {code JcrEngine} that is configured from the file in the location specified by the servlet context
 * parameter {@code org.modeshape.web.jcr.rest.CONFIG_FILE}. This location must be accessible by the classloader for this class.
 * </p>
 * *
 * <p>
 * This class is thread-safe.
 * </p>
 * 
 * @see RepositoryProvider
 * @see Class#getResourceAsStream(String)
 */
public class FactoryRepositoryProvider implements RepositoryProvider {

    public static final String JCR_URL = "org.modeshape.web.jcr.JCR_URL";

    private String jcrUrl;

    public FactoryRepositoryProvider() {
    }

    public Set<String> getJcrRepositoryNames() {
        RepositoryFactory factory = factory();
        if (factory == null) return null;

        return factory.getRepositories(jcrUrl).getRepositoryNames();
    }

    private Repository getRepository( String repositoryName ) throws RepositoryException {
        RepositoryFactory factory = factory();
        if (factory == null) return null;

        return factory.getRepositories(jcrUrl).getRepository(repositoryName);
    }

    public void startup( ServletContext context ) {
        this.jcrUrl = context.getInitParameter(JCR_URL);
    }

    public void shutdown() {
        factory().shutdown();
    }

    private final RepositoryFactory factory() {
        
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            return factory;
        }
        
        throw new IllegalStateException("No RepositoryFactory implementation on the classpath");
    }

    /**
     * Returns an active session for the given workspace name in the named repository.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param repositoryName the name of the repository in which the session is created
     * @param workspaceName the name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if any other error occurs
     */
    public Session getSession( HttpServletRequest request,
                               String repositoryName,
                               String workspaceName ) throws RepositoryException {
        assert request != null;

        Repository repository;

        try {
            repository = getRepository(repositoryName);

        } catch (RepositoryException re) {
            throw new NoSuchRepositoryException(re.getMessage(), re);
        }

        // If there's no authenticated user, try an anonymous login
        if (request.getUserPrincipal() == null) {
            return repository.login(workspaceName);
        }

        return repository.login(new SecurityContextCredentials(new ServletSecurityContext(request)), workspaceName);

    }
}
