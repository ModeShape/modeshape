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

package org.modeshape.web.jcr.rest.interceptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.WebLogger;

/**
 * REST Easy {@link PreProcessInterceptor} which will print out various logging information in DEBUG mode.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
@ServerInterceptor
public class LoggingInterceptor implements PreProcessInterceptor {

    private static final Logger LOGGER = WebLogger.getLogger(LoggingInterceptor.class);

    @Override
    public ServerResponse preProcess( HttpRequest request,
                                      ResourceMethod method ) throws Failure, WebApplicationException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received request: {0}", request.getUri().getRequestUri().toString());
            LOGGER.debug("Executing method: {0}", method.getMethod().toString());
        }
        return null;
    }
}
