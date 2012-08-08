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

package org.modeshape.web.jcr;

import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;

/**
 * Implementation of the {@link org.modeshape.jcr.api.Logger} interface which delegates the logging operations to an I18n based
 * {@link org.modeshape.common.logging.Logger} implementation, using pass-through {@link TextI18n} objects.
 *
 * This should be used in ModeShape's server-side web modules.
 *
 * @author Horia Chiorean
 */
public final class WebLogger implements org.modeshape.jcr.api.Logger {

    private final Logger logger;

    private WebLogger( Logger logger ) {
        this.logger = logger;
    }

    public static org.modeshape.jcr.api.Logger getLogger(Class<?> clazz) {
        return new WebLogger(Logger.getLogger(clazz));
    }

    @Override
    public void debug( String message,
                       Object... params ) {
        logger.debug(message, params);
    }

    @Override
    public void debug( Throwable t,
                       String message,
                       Object... params ) {
        logger.debug(t, message, params);
    }

    @Override
    public void error( String message,
                       Object... params ) {
        if (logger.isErrorEnabled()) {
            logger.error(new TextI18n(message), params);
        }
    }

    @Override
    public void error( Throwable t,
                       String message,
                       Object... params ) {
        if (logger.isErrorEnabled()) {
            logger.error(t, new TextI18n(message), params);
        }
    }

    @Override
    public void info( String message,
                      Object... params ) {
        if (logger.isInfoEnabled()) {
            logger.info(new TextI18n(message), params);
        }
    }

    @Override
    public void info( Throwable t,
                      String message,
                      Object... params ) {
        if (logger.isInfoEnabled()) {
            logger.info(t, new TextI18n(message), params);
        }
    }

    @Override
    public void trace( String message,
                       Object... params ) {
        logger.trace(message, params);
    }

    @Override
    public void trace( Throwable t,
                       String message,
                       Object... params ) {
        logger.trace(t, message, params);
    }

    @Override
    public void warn( String message,
                      Object... params ) {
        if (logger.isWarnEnabled()) {
            logger.warn(new TextI18n(message), params);
        }
    }

    @Override
    public void warn( Throwable t,
                      String message,
                      Object... params ) {
        if (logger.isWarnEnabled()) {
            logger.warn(t, new TextI18n(message), params);
        }
    }
}
