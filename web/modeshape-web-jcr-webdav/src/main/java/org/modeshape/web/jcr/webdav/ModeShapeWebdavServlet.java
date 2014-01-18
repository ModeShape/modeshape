/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.web.jcr.webdav;

import java.io.File;
import java.io.IOException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.webdav.IWebdavStore;
import org.modeshape.webdav.WebdavServlet;
import org.modeshape.webdav.exceptions.ObjectAlreadyExistsException;
import org.modeshape.webdav.exceptions.ObjectNotFoundException;
import org.modeshape.webdav.exceptions.WebdavException;

/**
 * Custom servlet implementation that provides WebDAV access to a JCR repository. Nodes in the repository with a specified primary
 * type (nt:file, by default) are treated as WebDAV resources (files) while nodes with any other primary type are treated as
 * WebDAV folders.
 */
public class ModeShapeWebdavServlet extends WebdavServlet {

    private static final long serialVersionUID = 1L;

    public static final String INIT_CONTENT_MAPPER_CLASS_NAME = "org.modeshape.web.jcr.webdav.CONTENT_MAPPER_CLASS_NAME";
    public static final String INIT_REQUEST_RESOLVER_CLASS_NAME = "org.modeshape.web.jcr.webdav.REQUEST_RESOLVER_CLASS_NAME";

    private RequestResolver requestResolver;
    private ContentMapper contentMapper;

    @Override
    protected IWebdavStore constructStore( String clazzName,
                                           File root ) {
        return new ModeShapeWebdavStore(requestResolver, contentMapper);
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
                Class<? extends RequestResolver> clazz = Class.forName(requestResolverClassName).asSubclass(RequestResolver.class);
                this.requestResolver = clazz.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        Logger.getLogger(getClass()).debug("WebDAV Servlet using resolver class = " + requestResolver.getClass().getName());
        this.requestResolver.initialize(getServletContext());
    }

    /**
     * Loads and initializes the {@link #contentMapper}
     */
    private void constructContentMapper() {
        // Initialize the request resolver
        String contentMapperClassName = getParam(INIT_CONTENT_MAPPER_CLASS_NAME);
        Logger.getLogger(getClass()).debug("WebDAV Servlet content mapper class name = " + contentMapperClassName);
        if (contentMapperClassName == null) {
            this.contentMapper = new DefaultContentMapper();
        } else {
            try {
                Class<? extends ContentMapper> clazz = Class.forName(contentMapperClassName).asSubclass(ContentMapper.class);
                this.contentMapper = clazz.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        Logger.getLogger(getClass()).debug("WebDAV Servlet using content mapper class = " + contentMapper.getClass().getName());
        this.contentMapper.initialize(getServletContext());
    }

    @Override
    public void init() throws ServletException {
        constructRequestResolver();
        constructContentMapper();

        super.init();
    }

    /**
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

    @Override
    protected Throwable translate( Throwable t ) {
        return translateError(t);
    }

    protected static WebdavException translateError( Throwable t ) {
        if (t instanceof AccessDeniedException) {
            return new org.modeshape.webdav.exceptions.AccessDeniedException(t.getMessage(), t);
        } else if (t instanceof LoginException) {
            return new org.modeshape.webdav.exceptions.AccessDeniedException(t.getMessage(), t);
        } else if (t instanceof ItemExistsException) {
            return new ObjectAlreadyExistsException(t.getMessage(), t);
        } else if (t instanceof PathNotFoundException) {
            return new ObjectNotFoundException(t.getMessage(), t);
        } else if (t instanceof ItemNotFoundException) {
            return new ObjectNotFoundException(t.getMessage(), t);
        } else if (t instanceof NoSuchWorkspaceException) {
            return new ObjectNotFoundException(t.getMessage(), t);
        } else {
            return new WebdavException(t.getMessage(), t);
        }
    }
}
