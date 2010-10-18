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

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.WebdavServlet;
import org.modeshape.common.util.Logger;

/**
 * Custom servlet implementation that provides WebDAV access to a JCR repository. Nodes in the repository with a specified primary
 * type (nt:file, by default) are treated as WebDAV resources (files) while nodes with any other primary type are treated as
 * WebDAV folders.
 */
public class ModeShapeWebdavServlet extends WebdavServlet {

    private static final long serialVersionUID = 1L;

    public static final String INIT_REQUEST_RESOLVER_CLASS_NAME = "org.modeshape.web.jcr.webdav.REQUEST_RESOLVER_CLASS_NAME";

    public static final String INIT_CONTENT_PRIMARY_TYPE_NAMES = "org.modeshape.web.jcr.webdav.CONTENT_PRIMARY_TYPE_NAMES";
    public static final String INIT_RESOURCE_PRIMARY_TYPES_NAMES = "org.modeshape.web.jcr.webdav.RESOURCE_PRIMARY_TYPE_NAMES";
    public static final String INIT_NEW_FOLDER_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_FOLDER_PRIMARY_TYPE_NAME";
    public static final String INIT_NEW_RESOURCE_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_RESOURCE_PRIMARY_TYPE_NAME";
    public static final String INIT_NEW_CONTENT_PRIMARY_TYPE_NAME = "org.modeshape.web.jcr.webdav.NEW_CONTENT_PRIMARY_TYPE_NAME";

    private RequestResolver requestResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    protected IWebdavStore constructStore( String clazzName,
                                           File root ) {
        return new ModeShapeWebdavStore(getParam(INIT_CONTENT_PRIMARY_TYPE_NAMES), getParam(INIT_RESOURCE_PRIMARY_TYPES_NAMES),
                                        getParam(INIT_NEW_FOLDER_PRIMARY_TYPE_NAME),
                                        getParam(INIT_NEW_RESOURCE_PRIMARY_TYPE_NAME),
                                        getParam(INIT_NEW_CONTENT_PRIMARY_TYPE_NAME), requestResolver);
    }

    protected String getParam( String name ) {
        return getServletContext().getInitParameter(name);
    }

    /**
     * Loads and initializes the {@link #requestResolver}
     */
    private void constructRequestResolver() {
        // Initialize the request resolver
        String requestResolverClassName = getParam(INIT_REQUEST_RESOLVER_CLASS_NAME);
        Logger.getLogger(getClass()).debug("WebDAV Servlet resolver class name = " + requestResolverClassName);
        if (requestResolverClassName == null) {
            this.requestResolver = new MultiRepositoryRequestResolver();
        } else {
            try {
                Class<? extends RequestResolver> clazz = Class.forName(requestResolverClassName)
                                                              .asSubclass(RequestResolver.class);
                this.requestResolver = clazz.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        Logger.getLogger(getClass()).debug("WebDAV Servlet using resolver class = " + requestResolver.getClass().getName());
        this.requestResolver.initialize(getServletContext());
    }

    @Override
    public void init() throws ServletException {
        constructRequestResolver();

        super.init();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method also sets and clears a thread-local reference to the incoming {@link HttpServletRequest request}.
     * </p>
     */
    @Override
    protected void service( HttpServletRequest req,
                            HttpServletResponse resp ) throws ServletException, IOException {
        ModeShapeWebdavStore.setRequest(req);
        try {
            super.service(req, resp);
        } finally {
            ModeShapeWebdavStore.setRequest(null);
        }
    }
}
