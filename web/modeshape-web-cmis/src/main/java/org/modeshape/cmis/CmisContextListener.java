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

import javax.servlet.ServletContextEvent;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.modeshape.web.jcr.ModeShapeJcrDeployer;

/**
 * @author kulikov
 */
public class CmisContextListener extends ModeShapeJcrDeployer {

    public static final String SERVICES_FACTORY = "org.apache.chemistry.opencmis.servicesfactory";

    @Override
    public void contextInitialized( ServletContextEvent sce ) {
        super.contextInitialized(sce);

        // create services factory
        JcrServiceFactory factory = new JcrServiceFactory();
        factory.init();

        // set the services factory into the servlet context
        sce.getServletContext().setAttribute(SERVICES_FACTORY, factory);
    }

    @Override
    public void contextDestroyed( ServletContextEvent sce ) {
        // destroy services factory
        CmisServiceFactory factory = (CmisServiceFactory)sce.getServletContext().getAttribute(SERVICES_FACTORY);
        if (factory != null) {
            factory.destroy();
        }

        super.contextDestroyed(sce);
    }

}
