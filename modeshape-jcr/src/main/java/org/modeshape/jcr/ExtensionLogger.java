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

package org.modeshape.jcr;

import org.modeshape.common.i18n.I18nResource;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import java.util.Locale;

/**
 * Implementation of the {@link org.modeshape.jcr.api.Logger} interface which delegates the logging operations to an I18n based
 * {@link org.modeshape.common.logging.Logger} implementation, using pass-through I18n objects. This is implementation should be
 * normally used by ModeShape extensions, to avoid coupling with the I18n objects and the -common module.
 *
 * @author Horia Chiorean
 */
public final class ExtensionLogger implements org.modeshape.jcr.api.Logger {

    private final Logger logger;

    private ExtensionLogger( Logger logger ) {
        this.logger = logger;
    }

    public static org.modeshape.jcr.api.Logger getLogger(Class<?> clazz) {
        return new ExtensionLogger(Logger.getLogger(clazz));
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
        logger.error(new I18n(message), params);
    }

    @Override
    public void error( Throwable t,
                       String message,
                       Object... params ) {
        logger.error(t, new I18n(message), params);
    }

    @Override
    public void info( String message,
                      Object... params ) {
        logger.info(new I18n(message), params);

    }

    @Override
    public void info( Throwable t,
                      String message,
                      Object... params ) {
        logger.info(t, new I18n(message), params);

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
        logger.warn(new I18n(message), params);

    }

    @Override
    public void warn( Throwable t,
                      String message,
                      Object... params ) {
        logger.warn(t, new I18n(message), params);
    }

    /**
     * A pass-through implementation of {@link I18nResource} which uses an underlying text as the real value, ignoring any
     * kind of internationalization.
     */
    private static class I18n implements I18nResource {
        private final static String BLANK = "";

        private final String text;

        I18n( String text ) {
            this.text = StringUtil.isBlank(text) ? BLANK : text;
        }

        @Override
        public String text( Object... arguments ) {
            return StringUtil.createString(text, arguments);
        }

        @Override
        public String text( Locale locale,
                            Object... arguments ) {
            return StringUtil.createString(text, arguments);
        }
    }
}
