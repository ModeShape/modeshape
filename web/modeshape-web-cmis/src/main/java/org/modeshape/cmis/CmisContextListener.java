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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.modeshape.web.jcr.ModeShapeJcrDeployer;

/**
 *
 * @author kulikov
 */
public class CmisContextListener extends ModeShapeJcrDeployer {

    public static final String SERVICES_FACTORY = "org.apache.chemistry.opencmis.servicesfactory";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);

        // create services factory
        JcrServiceFactory factory = new JcrServiceFactory();
        factory.init();
        
        // set the services factory into the servlet context
        sce.getServletContext().setAttribute(SERVICES_FACTORY, factory);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // destroy services factory
        CmisServiceFactory factory = (CmisServiceFactory) sce.getServletContext().getAttribute(SERVICES_FACTORY);
        if (factory != null) {
            factory.destroy();
        }

        super.contextDestroyed(sce);
    }

}
