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
package org.modeshape.web.jcr;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.modeshape.web.jcr.spi.RepositoryProvider;

/**
 * Servlet context listener that is responsible for {@link RepositoryFactory#initialize(javax.servlet.ServletContext)
 * initializing} the {@link RepositoryFactory repository factory}.
 * <p>
 * This class is not thread safe, but in practice this does not matter as the servlet container must ensure that only a single
 * instance of this exists per web context and that it is only called in a single-threaded manner.
 * </p>
 * <p>
 * This class is not thread-safe.
 * </p>
 * 
 * @see RepositoryFactory
 */
public class ModeShapeJcrDeployer implements ServletContextListener {

    /**
     * Alerts the repository factory that the web application is shutting down
     * 
     * @param event the servlet context event
     * @see RepositoryFactory#shutdown()
     * @see RepositoryProvider#shutdown()
     */
    public void contextDestroyed( ServletContextEvent event ) {
        RepositoryFactory.shutdown();
    }

    /**
     * Initializes the repository factory
     * 
     * @param event the servlet context event
     * @see RepositoryFactory#initialize(javax.servlet.ServletContext)
     */
    public void contextInitialized( ServletContextEvent event ) {
        RepositoryFactory.initialize(event.getServletContext());
    }
}
