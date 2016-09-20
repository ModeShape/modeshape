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
import java.util.Map;
import java.util.Set;
import javax.jcr.Repository;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.apache.chemistry.opencmis.jcr.JcrTypeManager;
import org.apache.chemistry.opencmis.jcr.PathManager;
import org.apache.chemistry.opencmis.jcr.impl.DefaultFolderTypeHandler;
import org.apache.chemistry.opencmis.jcr.impl.DefaultUnversionedDocumentTypeHandler;
import org.apache.chemistry.opencmis.jcr.type.JcrTypeHandlerManager;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
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
    public static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);
    public static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);
    public static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);
    public static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);
    
    private static final String DEFAULT_MOUNT_PATH = "/";

    protected JcrTypeManager typeManager;
    protected Map<String, Map<String, String>> jcrConfig;
    protected String mountPath;
    
    private PathManager pathManager;
    private JcrTypeHandlerManager typeHandlerManager;

    @Override
    public void init( Map<String, String> parameters ) {
        readConfiguration(parameters);
        this.typeManager = new JcrTypeManager();
        this.pathManager = new PathManager(mountPath);
        this.typeHandlerManager = createTypeHandlerManager(this.pathManager, typeManager);
    }

    protected void init() {
        this.typeManager = new JcrTypeManager();
        this.pathManager = new PathManager(DEFAULT_MOUNT_PATH);
        this.typeHandlerManager = createTypeHandlerManager(this.pathManager, typeManager);
    }

    @Override
    public void destroy() {
        this.typeManager = null;
        this.pathManager = null;
        this.typeHandlerManager = null;
        if (this.jcrConfig != null) {
            this.jcrConfig.clear();
            this.jcrConfig = null;
        }
    }

    @Override
    public CmisService getService( CallContext context ) {
        JcrService jcrService = createJcrService(loadRepositories(), context);
        ConformanceCmisServiceWrapper serviceWrapper = new ConformanceCmisServiceWrapper(jcrService,
                                                                                         DEFAULT_MAX_ITEMS_TYPES,
                                                                                         DEFAULT_DEPTH_TYPES,
                                                                                         DEFAULT_MAX_ITEMS_OBJECTS,
                                                                                         DEFAULT_DEPTH_OBJECTS);

        serviceWrapper.setCallContext(context);
        return serviceWrapper;
    }

    // ------------------------------------------< factories >---

    private Map<String, JcrRepository> loadRepositories() {
        Map<String, JcrRepository> list = new HashMap<>();
        Set<String> names = RepositoryManager.getJcrRepositoryNames();

        for (String repositoryId : names) {
            try {
                Repository repository = RepositoryManager.getRepository(repositoryId);
                list.put(repositoryId, new JcrMsRepository(repository, pathManager, typeManager, typeHandlerManager));
                log.debug("--- loaded repository " + repositoryId);
            } catch (NoSuchRepositoryException e) {
                // should never happen;
            }
        }

        return list;
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

    protected JcrTypeHandlerManager createTypeHandlerManager( PathManager pathManager,
                                                              JcrTypeManager typeManager ) {
        JcrTypeHandlerManager typeHandlerManager = new JcrTypeHandlerManager(pathManager, typeManager);
        typeHandlerManager.addHandler(new DefaultFolderTypeHandler());
        typeHandlerManager.addHandler(new MsDocumentTypeHandler());
        typeHandlerManager.addHandler(new DefaultUnversionedDocumentTypeHandler());
        return typeHandlerManager;
    }

    // ------------------------------------------< private >---

    private void readConfiguration( Map<String, String> parameters ) {
        String mountPath = parameters.get(MOUNT_PATH_CONFIG);
        this.mountPath = mountPath != null ? mountPath : DEFAULT_MOUNT_PATH;
        this.jcrConfig = RepositoryConfig.load(parameters);
    }
}
