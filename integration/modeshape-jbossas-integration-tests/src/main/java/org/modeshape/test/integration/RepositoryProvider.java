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
