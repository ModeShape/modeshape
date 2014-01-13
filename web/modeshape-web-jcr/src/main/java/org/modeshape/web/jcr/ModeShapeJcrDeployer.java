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
package org.modeshape.web.jcr;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener that is responsible for {@link RepositoryManager#initialize(javax.servlet.ServletContext)
 * initializing} the {@link RepositoryManager repository factory}.
 * <p>
 * This class is not thread safe, but in practice this does not matter as the servlet container must ensure that only a single
 * instance of this exists per web context and that it is only called in a single-threaded manner.
 * </p>
 * <p>
 * This class is not thread-safe.
 * </p>
 * 
 * @see RepositoryManager
 */
public class ModeShapeJcrDeployer implements ServletContextListener {

    /**
     * Alerts the repository factory that the web application is shutting down
     * 
     * @param event the servlet context event
     * @see RepositoryManager#shutdown()
     */
    @Override
    public void contextDestroyed( ServletContextEvent event ) {
        RepositoryManager.shutdown();
    }

    /**
     * Initializes the repository factory
     * 
     * @param event the servlet context event
     * @see RepositoryManager#initialize(javax.servlet.ServletContext)
     */
    @Override
    public void contextInitialized( ServletContextEvent event ) {
        RepositoryManager.initialize(event.getServletContext());
    }
}
