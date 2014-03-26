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

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.NamedRepository;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jcr.api.RepositoriesContainer;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * Service provider implementation of the {@link RepositoriesContainer} interface.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public final class JcrRepositoriesContainer implements RepositoriesContainer {

    protected static final Logger LOG = Logger.getLogger(JcrRepositoriesContainer.class);
    protected static final String REPOSITORY_NAME_URL_PARAM = "repositoryName";
    protected static final String FILE_PROTOCOL = "file:";
    protected static final String JNDI_PROTOCOL = "jndi:";

    /**
     * The engine that hosts the deployed repository instances.
     */
    protected static final ModeShapeEngine ENGINE = new ModeShapeEngine();

    @Override
    @SuppressWarnings( {"unchecked", "rawtypes"} )
    public JcrRepository getRepository( String repositoryName,
                                        Map parameters ) throws RepositoryException {
        if (!StringUtil.isBlank(repositoryName)) {
            try {
                JcrRepository repository = engine().getRepository(repositoryName);
                if (repository.getState() == ModeShapeEngine.State.STARTING
                    || repository.getState() == ModeShapeEngine.State.RUNNING) {
                    return repository;
                }
            } catch (NoSuchRepositoryException e) {
                // there is no such repository, so try to create & initialize it
            }
        }

        String repositoryURL = repositoryURLFromParams(parameters);
        if (StringUtil.isBlank(repositoryURL)) {
            LOG.debug("No repository URL parameter found");
            return null;
        }

        Map configParams = parameters;
        if (!StringUtil.isBlank(repositoryName)) {
            configParams = new HashMap(parameters);
            configParams.put(RepositoryFactory.REPOSITORY_NAME, repositoryName);
        }

        if (repositoryURL.toLowerCase().startsWith(JNDI_PROTOCOL)) {
            return new JNDIRepositoryLookup().repository(configParams, repositoryURL);
        }
        return new FileRepositoryLookup().repository(configParams, repositoryURL);
    }

    @Override
    public Set<String> getRepositoryNames( Map<?, ?> parameters ) throws RepositoryException {
        Set<String> repositoryNames = engine().getRepositoryNames();

        String repositoryURL = repositoryURLFromParams(parameters);
        if (repositoryURL == null) {
            LOG.debug("No repository URL parameter found");
            return repositoryNames;
        }

        if (repositoryURL.toLowerCase().startsWith(JNDI_PROTOCOL)) {
            Set<String> jndiRepositories = new JNDIRepositoryLookup().repositoryNames(parameters, repositoryURL);
            repositoryNames.addAll(jndiRepositories);
        } else {
            // based on the parameters try to lookup a repository (note that this may actually start it)
            JcrRepository repository = new FileRepositoryLookup().repository(parameters, repositoryURL);
            if (repository != null) {
                repositoryNames.add(repository.getName());
            }
        }
        return repositoryNames;
    }

    @Override
    public Future<Boolean> shutdown() {
        return ENGINE.shutdown();
    }

    @Override
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException {
        try {
            return ENGINE.shutdown().get(timeout, unit);
        } catch (ExecutionException e) {
            LOG.error(e, JcrI18n.errorShuttingDownJcrRepositoryFactory);
            return false;
        } catch (TimeoutException e) {
            LOG.warn(e, JcrI18n.timeoutWhileShuttingRepositoryDown);
            return false;
        }
    }

    protected ModeShapeEngine engine() {
        // Make sure the engine is started ...
        switch (ENGINE.getState()) {
            case NOT_RUNNING:
                ENGINE.start();
                break;
            case STOPPING:
                // Wait until it's shutdown ...
                try {
                    ENGINE.shutdown().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // ignore and let if fail ...
                }
                break;
            case RESTORING:
            case RUNNING:
            case STARTING:
                // do nothing ...
        }
        return ENGINE;
    }

    private String repositoryURLFromParams( Map<?, ?> parameters ) {
        if (parameters == null) {
            LOG.debug("The supplied parameters are null");
            return null;
        }

        Object rawUrl = parameters.get(RepositoryFactory.URL);
        if (rawUrl == null) {
            LOG.debug("No parameter found with key: " + RepositoryFactory.URL);
            return null;
        }
        return rawUrl.toString();
    }

    protected String repositoryNameFrom( String url,
                                         Map<?, ?> parameters ) {
        // First look in the parameters ...
        Object repoName = parameters.get(RepositoryFactory.REPOSITORY_NAME);
        if (repoName != null) {
            return repoName.toString();
        }
        // Then look for a query parameter in the URL ...
        int queryBeginIndex = url.indexOf("?") + 1;
        if (queryBeginIndex > 0 && queryBeginIndex < url.length()) {
            String query = url.substring(queryBeginIndex);
            for (String keyValuePair : query.split("&")) {
                String[] splitPair = keyValuePair.split("=");

                if (splitPair.length == 2 && REPOSITORY_NAME_URL_PARAM.equals(splitPair[0])) {
                    return splitPair[1];
                }
            }
        }
        return null;
    }

    protected class FileRepositoryLookup {
        protected JcrRepository repository( Map<?, ?> parameters,
                                            String repositoryURL ) throws RepositoryException {
            String repositoryName = repositoryNameFrom(repositoryURL, parameters);

            JcrRepository repository = lookForAlreadyDeployedRepositoryKey(repositoryURL);
            if (repository != null) {
                return repository;
            }

            RepositoryConfiguration configuration = loadRepositoryConfigurationFrom(repositoryURL);
            repository = lookForAlreadyDeployedRepositoryName(configuration.getName());
            if (repository != null) {
                return StringUtil.isBlank(repositoryName) || configuration.getName().equalsIgnoreCase(repositoryName) ? repository : null;
            }

            // Now deploy the new repository ...
            try {
                repository = ENGINE.deploy(configuration);
                repository.start();

                // The name might not match the configuration's name ...
                if (StringUtil.isBlank(repositoryName) || repository.getName().equals(repositoryName)) {
                    return repository;
                }
                LOG.warn(JcrI18n.repositoryNotFound, repositoryName, repositoryURL, parameters);
                return null;
            } catch (RepositoryException re) {
                throw re;
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }

        private JcrRepository lookForAlreadyDeployedRepositoryKey( String key ) {
            if (engine().getRepositoryKeys().contains(key)) {
                try {
                    return engine().getRepository(key);
                } catch (NoSuchRepositoryException e) {
                    // Must have been removed since we checked, so just continue on to redeploy it ...
                }
            }
            return null;
        }

        private JcrRepository lookForAlreadyDeployedRepositoryName( String name ) {
            if (engine().getRepositoryNames().contains(name)) {
                try {
                    return engine().getRepository(name);
                } catch (NoSuchRepositoryException e) {
                    // Must have been removed since we checked, so just continue on to redeploy it ...
                }
            }
            return null;
        }

        private RepositoryConfiguration loadRepositoryConfigurationFrom( String repositoryURL ) throws RepositoryException {
            try {
                if (repositoryURL.toLowerCase().startsWith(FILE_PROTOCOL)) {
                    String configLocation = removeForwardSlashes(repositoryURL.substring(FILE_PROTOCOL.length()));
                    if (configLocation.indexOf("?") > 0) {
                        configLocation = configLocation.substring(0, configLocation.indexOf("?"));
                    }
                    return RepositoryConfiguration.read(configLocation);
                }
                // Just try resolving the URL ...
                return RepositoryConfiguration.read(repositoryURL);
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }

        private String removeForwardSlashes( String path ) {
            return path.replaceFirst("//+", "");
        }
    }

    protected class JNDIRepositoryLookup {

        private Object doJNDILookup( String jndiName,
                                     Map<?, ?> parameters ) throws RepositoryException {
            if (jndiName == null) {
                return null;
            }
            try {
                InitialContext ic = new InitialContext(hashtable(parameters));
                return ic.lookup(jndiName);
            } catch (NamingException e) {
                throw new RepositoryException(e);
            }
        }

        protected JcrRepository repository( Map<?, ?> parameters,
                                            String repositoryURL ) throws RepositoryException {
            String jndiName = repositoryURL.substring(JNDI_PROTOCOL.length());
            if (jndiName.indexOf("?") > 0) {
                jndiName = jndiName.substring(0, jndiName.indexOf("?"));
            }
            String repositoryName = repositoryNameFrom(repositoryURL, parameters);
            Object jndiObject = doJNDILookup(jndiName, parameters);

            if (jndiObject instanceof ModeShapeEngine) {
                return repositoryFromEngine((ModeShapeEngine)jndiObject, jndiName, repositoryName);
            } else if (jndiObject instanceof JcrRepository) {
                // Just return the repository instance ...
                return (JcrRepository)jndiObject;
            } else {
                return null;
            }
        }

        protected Set<String> repositoryNames( Map<?, ?> parameters,
                                               String repositoryURL ) throws RepositoryException {
            String jndiName = repositoryURL.substring(JNDI_PROTOCOL.length());
            Object jndiObject = doJNDILookup(jndiName, parameters);

            if (jndiObject instanceof NamedRepository) {
                return Collections.singleton(((NamedRepository)jndiObject).getName());
            } else if (jndiObject instanceof Repositories) {
                return ((Repositories)jndiObject).getRepositoryNames();
            } else {
                return Collections.emptySet();
            }
        }

        private JcrRepository repositoryFromEngine( ModeShapeEngine engine,
                                                    String jndiName,
                                                    String repositoryName ) {
            switch (engine.getState()) {
                case NOT_RUNNING:
                case STOPPING:
                    LOG.error(JcrI18n.engineAtJndiLocationIsNotRunning, jndiName);
                    return null;
                case RUNNING:
                case RESTORING:
                case STARTING:
                    break; // continue
            }

            if (repositoryName == null && engine.getRepositories().size() == 1) {
                repositoryName = engine.getRepositories().keySet().iterator().next();
            }

            if (StringUtil.isBlank(repositoryName)) {
                return null;
            }

            if (repositoryName != null) {
                repositoryName = repositoryName.trim();
            }
            // Look for a repository with the supplied name ...
            try {
                JcrRepository repository = engine.getRepository(repositoryName);
                switch (repository.getState()) {
                    case STARTING:
                    case RUNNING:
                        return repository;
                    default:
                        LOG.debug("The '{0}' repository in JNDI at '{1}' is not (yet) running, but may be (re)started when needed.",
                                  repositoryName, jndiName);
                        return repository;
                }
            } catch (NoSuchRepositoryException e) {
                LOG.warn(JcrI18n.repositoryNotFoundInEngineAtJndiLocation, repositoryName, jndiName);
                return null;
            }
        }

        private Hashtable<String, String> hashtable( Map<?, ?> map ) {
            if (map == null) {
                return new Hashtable<String, String>();
            }
            Hashtable<String, String> hash = new Hashtable<String, String>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                hash.put(entry.getKey().toString(), value != null ? value.toString() : null);
            }

            return hash;
        }
    }
}
