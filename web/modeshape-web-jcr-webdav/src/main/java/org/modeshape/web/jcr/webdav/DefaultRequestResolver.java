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
package org.modeshape.web.jcr.webdav;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.i18n.I18n;

/**
 * {@link RequestResolver} implementation that exists for backward compatibility.
 * 
 * @deprecated use {@link SingleRepositoryRequestResolver} instead for the same functionality
 */
@Deprecated
public class DefaultRequestResolver implements RequestResolver {
    public static final String INIT_REPOSITORY_NAME = "org.modeshape.web.jcr.webdav.DEFAULT_RESOLVER_REPOSITORY_NAME";
    public static final String INIT_WORKSPACE_NAME = "org.modeshape.web.jcr.webdav.DEFAULT_RESOLVER_WORKSPACE_NAME";

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
