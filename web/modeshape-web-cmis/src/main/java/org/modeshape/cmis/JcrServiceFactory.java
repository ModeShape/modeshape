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
package org.modeshape.cmis;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;
import org.modeshape.common.logging.Logger;
import org.modeshape.web.jcr.NoSuchRepositoryException;
import org.modeshape.web.jcr.RepositoryManager;

/**
 * Implementation overwrites original service factory.
 * 
 * @author kulikov
 */
@SuppressWarnings( "deprecation" )
public class JcrServiceFactory extends AbstractServiceFactory {

    private static final Logger log = Logger.getLogger(JcrServiceFactory.class);

    public static final String MOUNT_PATH_CONFIG = "mount-path";
    public static final String PREFIX_JCR_CONFIG = "jcr.";

    public static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);
    public static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);
    public static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);
    public static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);

    protected JcrTypeManager typeManager;
    protected Map<String, Map<String, String>> jcrConfig;
    protected String mountPath;
    protected Map<String, JcrRepository> jcrRepositories;

    @Override
    public void init( Map<String, String> parameters ) {
        typeManager = createTypeManager();
        readConfiguration(parameters);
        PathManager pathManager = new PathManager(mountPath);
        JcrTypeHandlerManager typeHandlerManager = createTypeHandlerManager(pathManager, typeManager);
        jcrRepositories = loadRepositories(pathManager, typeHandlerManager);
    }

    public void init() {
        typeManager = createTypeManager();
        PathManager pathManager = new PathManager("/");
        JcrTypeHandlerManager typeHandlerManager = createTypeHandlerManager(pathManager, typeManager);
        jcrRepositories = loadRepositories(pathManager, typeHandlerManager);
    }

    @Override
    public void destroy() {
        jcrRepositories.clear();
        typeManager = null;
    }

    @Override
    public CmisService getService( CallContext context ) {
        CmisServiceWrapper<JcrService> serviceWrapper = new CmisServiceWrapper<JcrService>(createJcrService(jcrRepositories,
                                                                                                            context),
                                                                                           DEFAULT_MAX_ITEMS_TYPES,
                                                                                           DEFAULT_DEPTH_TYPES,
                                                                                           DEFAULT_MAX_ITEMS_OBJECTS,
                                                                                           DEFAULT_DEPTH_OBJECTS);

        serviceWrapper.getWrappedService().setCallContext(context);
        return serviceWrapper;
    }

    // ------------------------------------------< factories >---

    private Map<String, JcrRepository> loadRepositories( PathManager pathManger,
                                                         JcrTypeHandlerManager typeHandlerManager ) {
        Map<String, JcrRepository> list = new HashMap<String, JcrRepository>();
        Set<String> names = RepositoryManager.getJcrRepositoryNames();

        for (String repositoryId : names) {
            // Map params = jcrConfig.get(repositoryId);
            // Repository repository = acquireJcrRepository(params);
            try {
                Repository repository = RepositoryManager.getRepository(repositoryId);
                list.put(repositoryId, new JcrRepository(repository, pathManger, typeManager, typeHandlerManager));
                log.debug("--- loaded repository " + repositoryId);
            } catch (NoSuchRepositoryException e) {
                // should never happen;
                e.printStackTrace();
            }
        }

        return list;
    }

    /**
     * Acquire the JCR repository given a configuration. This implementation used
     * {@link javax.imageio.spi.ServiceRegistry#lookupProviders(Class)} for locating <code>RepositoryFactory</code> instances. The
     * first instance which can handle the <code>jcrConfig</code> parameters is used to acquire the repository.
     * 
     * @param jcrConfig configuration determining the JCR repository to be returned
     * @return the repository
     */
    protected Repository acquireJcrRepository( Map<String, String> jcrConfig ) {
        try {
            Iterator<RepositoryFactory> factories = ServiceRegistry.lookupProviders(RepositoryFactory.class);
            while (factories.hasNext()) {
                RepositoryFactory factory = factories.next();
                log.debug("Trying to acquire JCR repository from factory " + factory);
                Repository repository = factory.getRepository(jcrConfig);
                if (repository != null) {
                    log.debug("Successfully acquired JCR repository from factory " + factory);
                    return repository;
                }
                log.debug("Could not acquire JCR repository from factory " + factory);
            }
            throw new CmisConnectionException("No JCR repository factory for configured parameters");
        } catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisConnectionException(e.getMessage(), e);
        }
    }

    /**
     * Create a <code>JcrService</code> from a <code>JcrRepository</code>JcrRepository> and <code>CallContext</code>.
     * 
     * @param jcrRepositories the repositories
     * @param context the context
     * @return the new JCR service
     */
    protected JcrService createJcrService( Map<String, JcrRepository> jcrRepositories,
                                           CallContext context ) {
        return new JcrService(jcrRepositories);
    }

    protected JcrTypeManager createTypeManager() {
        return new JcrTypeManager();
    }

    protected JcrTypeHandlerManager createTypeHandlerManager( PathManager pathManager,
                                                              JcrTypeManager typeManager ) {
        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
        return typeHandlerManager;
    }

    // ------------------------------------------< private >---

    private void readConfiguration( Map<String, String> parameters ) {
        mountPath = parameters.get(MOUNT_PATH_CONFIG);
        jcrConfig = RepositoryConfig.load(parameters);
    }

    public JcrTypeManager getTypeManager() {
        return typeManager;
    }

    // public JcrRepository getJcrRepository() {
    // return jcrRepository;
    // }
}
