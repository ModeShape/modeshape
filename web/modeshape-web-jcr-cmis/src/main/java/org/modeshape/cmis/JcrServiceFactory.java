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
package org.modeshape.cmis;

import java.math.BigInteger;
import java.util.*;
import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.servlet.ServletContext;
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
    public void init(Map<String, String> parameters) {
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
    public CmisService getService(CallContext context) {
        CmisServiceWrapper<JcrService> serviceWrapper = new CmisServiceWrapper<JcrService>(
                createJcrService(jcrRepositories, context), DEFAULT_MAX_ITEMS_TYPES, DEFAULT_DEPTH_TYPES,
                DEFAULT_MAX_ITEMS_OBJECTS, DEFAULT_DEPTH_OBJECTS);

        serviceWrapper.getWrappedService().setCallContext(context);
        return serviceWrapper;
    }

    //------------------------------------------< factories >---

    private Map<String, JcrRepository> loadRepositories(PathManager pathManger,
            JcrTypeHandlerManager typeHandlerManager) {
        Map<String, JcrRepository> list = new HashMap();
        Set<String> names = RepositoryManager.getJcrRepositoryNames();

        for (String repositoryId : names) {
//            Map params = jcrConfig.get(repositoryId);
//            Repository repository = acquireJcrRepository(params);
            try {
                Repository repository = RepositoryManager.getRepository(repositoryId);
                list.put(repositoryId, new JcrRepository(repository, pathManger,
                        typeManager, typeHandlerManager));
                System.out.println("--- loaded repository " + repositoryId);
            } catch (NoSuchRepositoryException e) {
                //should never happen;
                e.printStackTrace();
            }
        }

        return list;
    }

    /**
     * Acquire the JCR repository given a configuration. This implementation used
     * {@link javax.imageio.spi.ServiceRegistry#lookupProviders(Class)} for
     * locating <code>RepositoryFactory</code> instances. The first instance
     * which can handle the <code>jcrConfig</code> parameters is used to
     * acquire the repository.
     *
     * @param jcrConfig  configuration determining the JCR repository to be returned
     * @return
     * @throws RepositoryException
     */
    protected Repository acquireJcrRepository(Map<String, String> jcrConfig) {
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
                else {
                    log.debug("Could not acquire JCR repository from factory " + factory);
                }
            }
            throw new CmisConnectionException("No JCR repository factory for configured parameters");
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisConnectionException(e.getMessage(), e);
        }
    }

    /**
     * Create a <code>JcrService</code> from a <code>JcrRepository</code>JcrRepository> and
     * <code>CallContext</code>.
     *
     * @param jcrRepository
     * @param context
     * @return
     */
    protected JcrService createJcrService(Map<String,JcrRepository> jcrRepositories, CallContext context) {
        return new JcrService(jcrRepositories);
    }

    protected JcrTypeManager createTypeManager() {
        return new JcrTypeManager();
    }

    protected JcrTypeHandlerManager createTypeHandlerManager(PathManager pathManager, JcrTypeManager typeManager) {
        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
        typeHandlerManager.addHandler(new DefaultDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
        return typeHandlerManager;
    }

    //------------------------------------------< private >---

    private void readConfiguration(Map<String, String> parameters) {
        mountPath = parameters.get(MOUNT_PATH_CONFIG);
        jcrConfig = RepositoryConfig.load(parameters);
    }

    

    public JcrTypeManager getTypeManager() {
        return typeManager;
    }


//    public JcrRepository getJcrRepository() {
//        return jcrRepository;
//    }
}
