/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
 * A SessionFactory implementation that creates {@link Session} instances from a map of named {@link Repository} references
 * managed by this factory.
 * <p>
 * By default, this factory creates an anonymous JCR session. To use sessions with specific {@link Credentials}, simply
 * {@link #registerCredentials(String, Credentials) register} credentials for the appropriate repository/workspace name. For
 * security reasons, it is not possible to retrieve the Credentials once registered with this factory.
 * </p>
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
     * 
     * @param workspaceDelimiters the delimiters, or null/empty if the default delimiter of '/' should be used.
     */
    public SimpleSessionFactory( char... workspaceDelimiters ) {
        super(workspaceDelimiters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRegisterRepository( String name,
                                         Repository repository ) {
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
