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

package org.modeshape.common.logging.log4j;

import org.apache.log4j.Logger;
import org.modeshape.common.i18n.I18nResource;
import org.modeshape.common.util.StringUtil;

/**
 * {@link org.modeshape.common.logging.Logger} implementation which uses a Log4j logger to perform the logging operation
 *
 * @author Horia Chiorean
 */
final class Log4jLoggerImpl extends org.modeshape.common.logging.Logger {

    private final org.apache.log4j.Logger logger;

    public Log4jLoggerImpl( String name ) {
        logger = Logger.getLogger(name);
    }

    @Override
    public void debug( String message,
                       Object... params ) {
        logger.debug(StringUtil.createString(message, params));
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void debug( Throwable t,
                       String message,
                       Object... params ) {
        if (StringUtil.isBlank(message)) {
            return;
        }
        logger.debug(StringUtil.createString(message, params), t);
    }

    @Override
    public void error( I18nResource message,
                       Object... params ) {
        if (message == null) {
            return;
        }
        logger.error(message.text(getLoggingLocale(), params));
    }

    @Override
    public void error( Throwable t,
                       I18nResource message,
                       Object... params ) {
        if (message == null) {
            return;
        }
        logger.error(message.text(getLoggingLocale(), params), t);

    }

    @Override
    public void info( I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        logger.info(message.text(getLoggingLocale(), params));
    }

    @Override
    public void info( Throwable t,
                      I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        logger.info(message.text(getLoggingLocale(), params), t);
    }

    @Override
    public void trace( String message,
                       Object... params ) {
        if (StringUtil.isBlank(message)) {
            return;
        }
        logger.trace(StringUtil.createString(message, params));
    }

    @Override
    public void trace( Throwable t,
                       String message,
                       Object... params ) {
        if (StringUtil.isBlank(message)) {
            return;
        }
        logger.trace(StringUtil.createString(message, params), t);
    }

    @Override
    public void warn( I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        logger.warn(message.text(getLoggingLocale(), params));
    }

    @Override
    public void warn( Throwable t,
                      I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        logger.warn(message.text(getLoggingLocale(), params), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isEnabledFor(org.apache.log4j.Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isEnabledFor(org.apache.log4j.Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabledFor(org.apache.log4j.Level.ERROR);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isEnabledFor(org.apache.log4j.Level.DEBUG);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isEnabledFor(org.apache.log4j.Level.TRACE);
    }
}
