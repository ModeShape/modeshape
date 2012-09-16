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


import org.modeshape.common.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebdavContextListener implements ServletContextListener {

    private static final String INIT_CONTENT_MAPPER_CLASS_NAME = "org.modeshape.web.jcr.webdav.CONTENT_MAPPER_CLASS_NAME";
    private static final String INIT_REQUEST_RESOLVER_CLASS_NAME = "org.modeshape.web.jcr.webdav.REQUEST_RESOLVER_CLASS_NAME";

    private RequestResolver requestResolver;
    private ContentMapper contentMapper;

    public static ModeShapeWebdavStore webdavStore;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();

        constructRequestResolver(servletContext.getInitParameter(INIT_REQUEST_RESOLVER_CLASS_NAME), servletContext);
        constructContentMapper(servletContext.getInitParameter(INIT_CONTENT_MAPPER_CLASS_NAME), servletContext);

        webdavStore = new ModeShapeWebdavStore(requestResolver, contentMapper);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if(webdavStore != null){
            webdavStore.destroy();
        }
    }

    /**
     * Loads and initializes the {@link #requestResolver}
     */
    private void constructRequestResolver(String requestResolverClassName, ServletContext servletContext) {
        // Initialize the request resolver
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
        this.requestResolver.initialize(servletContext);
    }

    /**
     * Loads and initializes the {@link #contentMapper}
     */
    private void constructContentMapper(String contentMapperClassName, ServletContext servletContext) {
        // Initialize the request resolver
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
        this.contentMapper.initialize(servletContext);
    }
}
