/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.repository.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InitialContext;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * A SessionFactory implementation that creates {@link Session} instances using {@link Repository} instances registered in JNDI.
 * <p>
 * This factory using a naming convention where the name supplied to the {@link #createSession(String)} contains both the name of
 * the repository and the name of the workspace. Typically, this is <i><code>repositoryName/workspaceName</code></i>, where
 * <code>repositoryName</code> is the JNDI name under which the Repository instance was bound, and <code>workspaceName</code>
 * is the name of the workspace. Note that this method looks for the last delimiter in the whole name to distinguish between the
 * repository and workspace names.
 * </p>
 * <p>
 * For example, if "<code>java:comp/env/repository/dataRepository/myWorkspace</code>" is passed to the
 * {@link #createSession(String)} method, this factory will look for a {@link Repository} instance registered in JDNI with the
 * name "<code>java:comp/env/repository/dataRepository</code>" and use it to {@link Repository#login(String) create a session}
 * to the workspace named "<code>myWorkspace</code>".
 * </p>
 * <p>
 * By default, this factory creates an anonymous JCR session. To use sessions with specific {@link Credentials}, simply
 * {@link #registerCredentials(String, Credentials) register} credentials for the appropriate repository/workspace name. For
 * security reasons, it is not possible to retrieve the Credentials once registered with this factory.
 * </p>
 * @author Randall Hauch
 */
public class SimpleSessionFactory extends AbstractSessionFactory {

    private final Map<String, Repository> repositories = new ConcurrentHashMap<String, Repository>();

    /**
     * Create an instance of the factory by creating a new {@link InitialContext}.
     */
    public SimpleSessionFactory() {
        super();
    }

    /**
     * Create an instance of the factory by supplying the characters that may be used to delimit the workspace name from the
     * repository name.
     * @param workspaceDelimiters the delimiters, or null/empty if the default delimiter of '/' should be used.
     */
    public SimpleSessionFactory( char... workspaceDelimiters ) {
        super(workspaceDelimiters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRegisterRepository( String name, Repository repository ) throws SystemFailureException {
        this.repositories.put(name, repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doUnregisterRepository( String name ) throws SystemFailureException {
        if (this.repositories.remove(name) == null) {
            throw new SystemFailureException(RepositoryI18n.unableToRemoveRepository.text(name));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Repository findRegisteredRepository( String name ) throws SystemFailureException {
        Repository repository = this.repositories.get(name);
        if (repository == null) {
            throw new SystemFailureException(RepositoryI18n.unableToFindRepositoryWithName.text(name));
        }
        return repository;
    }

}
