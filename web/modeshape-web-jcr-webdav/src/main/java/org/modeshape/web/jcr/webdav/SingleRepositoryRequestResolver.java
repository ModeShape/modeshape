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
package org.modeshape.web.jcr.webdav;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.i18n.I18n;

/**
 * Default {@link RequestResolver} that performs a direct mapping from all incoming URIs to the same path within a single
 * repository and workspace.
 * 
 * @see MultiRepositoryRequestResolver
 */
public class SingleRepositoryRequestResolver implements RequestResolver {
    public static final String INIT_REPOSITORY_NAME = "org.modeshape.web.jcr.webdav.SINGLE_REPOSITORY_RESOLVER_REPOSITORY_NAME";
    public static final String INIT_WORKSPACE_NAME = "org.modeshape.web.jcr.webdav.SINGLE_REPOSITORY_RESOLVER_WORKSPACE_NAME";

    private String repositoryName;
    private String workspaceName;

    @Override
    public void initialize( ServletContext context ) {
        repositoryName = context.getInitParameter(INIT_REPOSITORY_NAME);
        if (repositoryName == null) {
            I18n msg = WebdavI18n.requiredParameterMissing;
            throw new IllegalStateException(msg.text(INIT_REPOSITORY_NAME));
        }

        workspaceName = context.getInitParameter(INIT_WORKSPACE_NAME);
        if (workspaceName == null) {
            I18n msg = WebdavI18n.requiredParameterMissing;
            throw new IllegalStateException(msg.text(INIT_WORKSPACE_NAME));
        }
    }

    @Override
    public ResolvedRequest resolve( HttpServletRequest request,
                                    String relativePath ) {
        return new ResolvedRequest(request, repositoryName, workspaceName, relativePath);
    }
}
