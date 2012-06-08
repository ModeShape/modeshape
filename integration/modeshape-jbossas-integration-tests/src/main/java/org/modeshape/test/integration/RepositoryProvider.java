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
package org.modeshape.test.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.modeshape.jcr.api.Repositories;

/**
 * A utility class that can obtain a repository a variety of different ways. Typically, an application will use only one approach.
 */
public class RepositoryProvider {

    public Repository getRepositoryFromJndi( String jndiName ) throws NamingException {
        InitialContext context = new InitialContext();
        return (Repository)context.lookup(jndiName);
    }

    public Repository getRepositoryFromRepositoriesInJndi( String jndiNameOfRepositories,
                                                           String repositoryName ) throws NamingException, RepositoryException {
        InitialContext context = new InitialContext();
        Repositories repositories = (Repositories)context.lookup(jndiNameOfRepositories);
        if (repositories == null) {
            throw new RepositoryException("Repository container could not be found in JNDI at '" + jndiNameOfRepositories + "'");
        }
        return repositories.getRepository(repositoryName);
    }

    public Repository getRepositoryUsingRepositoryFactory( String url ) throws RepositoryException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(org.modeshape.jcr.api.RepositoryFactory.URL, url);
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            Repository repository = factory.getRepository(params);
            if (repository != null) {
                return repository;
            }
        }
        return null;
    }

    public Repository getRepositoryUsingRepositoryFactory( String url,
                                                           String repositoryName ) throws RepositoryException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(org.modeshape.jcr.api.RepositoryFactory.URL, url);
        params.put(org.modeshape.jcr.api.RepositoryFactory.REPOSITORY_NAME, repositoryName);
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            Repository repository = factory.getRepository(params);
            if (repository != null) {
                return repository;
            }
        }
        return null;
    }

    public void loginAndLogout( Repository repository ) throws RepositoryException {
        Session session = repository.login();
        try {
            // so the following code is unreachable
            Node node = session.getRootNode();
            String path = node.getPath();
            assert path != null : "Path of the root node was null; what's up with that?";
        } finally {
            session.logout();
        }
    }

    public void loginAndLogout( Repository repository,
                                String workspaceName ) throws RepositoryException {
        Session session = repository.login(workspaceName);
        try {
            // so the following code is unreachable
            Node node = session.getRootNode();
            String path = node.getPath();
            assert path != null : "Path of the root node was null; what's up with that?";
        } finally {
            session.logout();
        }
    }

    public void loginAndLogout( Repository repository,
                                Credentials credentials ) throws RepositoryException {
        Session session = repository.login(credentials);
        try {
            // so the following code is unreachable
            Node node = session.getRootNode();
            String path = node.getPath();
            assert path != null : "Path of the root node was null; what's up with that?";
        } finally {
            session.logout();
        }
    }

    public void loginAndLogout( Repository repository,
                                Credentials credentials,
                                String workspaceName ) throws RepositoryException {
        Session session = repository.login(credentials, workspaceName);
        try {
            // so the following code is unreachable
            Node node = session.getRootNode();
            String path = node.getPath();
            assert path != null : "Path of the root node was null; what's up with that?";
        } finally {
            session.logout();
        }
    }
}
