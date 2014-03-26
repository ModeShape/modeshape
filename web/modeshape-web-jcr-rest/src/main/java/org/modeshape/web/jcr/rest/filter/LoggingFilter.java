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

package org.modeshape.web.jcr.rest.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.WebLogger;

/**
 * REST Easy {@link ContainerRequestFilter} which will print out various logging information in DEBUG mode.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = WebLogger.getLogger(LoggingFilter.class);

    @Override
    public void filter( ContainerRequestContext requestContext ) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received request: {0}", requestContext.getUriInfo().getRequestUri().toString());
            LOGGER.debug("Executing method: {0}", requestContext.getMethod());
        }
    }
}
