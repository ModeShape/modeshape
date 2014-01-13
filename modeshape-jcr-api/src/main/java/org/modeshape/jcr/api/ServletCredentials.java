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
package org.modeshape.jcr.api;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link Credentials} implementation that assumes servlet-based authentication and provides authorization through the
 * {@link HttpServletRequest#isUserInRole(String) servlet role-checking mechanism}.
 * <p>
 * These credentials are really only valid for the life of the {@link HttpServletRequest servlet request}, and thus should be used
 * to obtain a Session for each request.
 * </p>
 * <p>
 * Note that this class can only be used if the {@link HttpServletRequest} class is on the classpath.
 * </p>
 */
public class ServletCredentials implements Credentials {

    private static final long serialVersionUID = 1L;

    private transient HttpServletRequest request;

    /**
     * Create a {@link Credentials} using the supplied {@link HttpServletRequest servlet information}.
     * 
     * @param request the servlet request
     */
    public ServletCredentials( HttpServletRequest request ) {
        this.request = request;
    }

    /**
     * Get the Servlet request that this credentials applies to.
     * 
     * @return the request, or null if this credentials is no longer valid
     */
    public HttpServletRequest getRequest() {
        return request;
    }
}
