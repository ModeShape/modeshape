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

package org.modeshape.common.util.log;

import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.StringUtil;
import org.slf4j.LoggerFactory;

/**
 * Logger that delivers messages to a Log4J logger
 * 
 * @since 2.5
 */
public class SLF4JLoggerImpl extends org.modeshape.common.util.Logger {
    private final org.slf4j.Logger logger;

    public SLF4JLoggerImpl( String category ) {
        logger = LoggerFactory.getLogger(category);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void warn( Throwable t,
                      I18n message,
                      Object... params ) {
        if (!isWarnEnabled()) return;
        if (t == null) {
            warn(message, params);
            return;
        }
        if (message == null) {
            logger.warn(null, t);
            return;
        }
        logger.warn(message.text(LOGGING_LOCALE.get(), params), t);
    }

    @Override
    public void warn( I18n message,
                      Object... params ) {
        if (!isWarnEnabled()) return;
        if (message == null) return;
        logger.warn(message.text(LOGGING_LOCALE.get(), params));
    }

    /**
     * Log a message at the DEBUG level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the DEBUG level.
     * 
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void debug( String message,
                       Object... params ) {
        if (!isDebugEnabled()) return;
        if (message == null) return;
        logger.debug(StringUtil.createString(message, params));
    }

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying message. If the exception is null, then this method
     * calls {@link #debug(String, Object...)}.
     * 
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void debug( Throwable t,
                       String message,
                       Object... params ) {
        if (!isDebugEnabled()) return;
        if (t == null) {
            debug(message, params);
            return;
        }
        if (message == null) {
            logger.debug(null, t);
            return;
        }
        logger.debug(StringUtil.createString(message, params), t);
    }

    /**
     * Log a message at the ERROR level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the ERROR level.
     * 
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void error( I18n message,
                       Object... params ) {
        if (!isErrorEnabled()) return;
        if (message == null) return;
        logger.error(message.text(LOGGING_LOCALE.get(), params));
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message. If the exception is null, then this method
     * calls {@link #error(I18n, Object...)}.
     * 
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void error( Throwable t,
                       I18n message,
                       Object... params ) {
        if (!isErrorEnabled()) return;
        if (t == null) {
            error(message, params);
            return;
        }
        if (message == null) {
            logger.error(null, t);
            return;
        }
        logger.error(message.text(LOGGING_LOCALE.get(), params), t);
    }

    /**
     * Log a message at the INFO level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the INFO level.
     * 
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void info( I18n message,
                      Object... params ) {
        if (!isInfoEnabled()) return;
        if (message == null) return;
        logger.info(message.text(LOGGING_LOCALE.get(), params));
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message. If the exception is null, then this method
     * calls {@link #info(I18n, Object...)}.
     * 
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void info( Throwable t,
                      I18n message,
                      Object... params ) {
        if (!isInfoEnabled()) return;
        if (t == null) {
            info(message, params);
            return;
        }
        if (message == null) {
            logger.info(null, t);
            return;
        }
        logger.info(message.text(LOGGING_LOCALE.get(), params), t);
    }

    /**
     * Log a message at the TRACE level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the TRACE level.
     * 
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void trace( String message,
                       Object... params ) {
        if (!isTraceEnabled()) return;
        if (message == null) return;
        logger.trace(StringUtil.createString(message, params));
    }

    /**
     * Log an exception (throwable) at the TRACE level with an accompanying message. If the exception is null, then this method
     * calls {@link #trace(String, Object...)}.
     * 
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void trace( Throwable t,
                       String message,
                       Object... params ) {
        if (!isTraceEnabled()) return;
        if (t == null) {
            this.trace(message, params);
            return;
        }
        if (message == null) {
            logger.trace(null, t);
            return;
        }
        logger.trace(StringUtil.createString(message, params), t);
    }

}
