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

import javax.jcr.Session;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.WebLogger;
import org.modeshape.web.jcr.rest.handler.AbstractHandler;

/**
 * {@link PostProcessInterceptor} implementation which will always close an active {@link Session} instance, if such an instance
 * has been opened during a request.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
@ServerInterceptor
public class CleanupInterceptor implements PostProcessInterceptor {

    private static final Logger LOGGER = WebLogger.getLogger(CleanupInterceptor.class);

    @Override
    public void postProcess( ServerResponse response ) {
        LOGGER.trace("Executing CleanupInterceptor...");
        AbstractHandler.cleanupActiveSession();
    }
}
