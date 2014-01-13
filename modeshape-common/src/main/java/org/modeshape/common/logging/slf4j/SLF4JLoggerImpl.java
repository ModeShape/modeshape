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
package org.modeshape.common.logging.slf4j;

import org.modeshape.common.i18n.I18nResource;
import org.modeshape.common.util.StringUtil;
import org.modeshape.common.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger that delivers messages to a Log4J logger
 * 
 * @since 2.5
 */
final class SLF4JLoggerImpl extends Logger {
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
                      I18nResource message,
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
        logger.warn(message.text(getLoggingLocale(), params), t);
    }

    @Override
    public void warn( I18nResource message,
                      Object... params ) {
        if (!isWarnEnabled()) return;
        if (message == null) return;
        logger.warn(message.text(getLoggingLocale(), params));
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
    public void error( I18nResource message,
                       Object... params ) {
        if (!isErrorEnabled()) return;
        if (message == null) return;
        logger.error(message.text(getLoggingLocale(), params));
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message. If the exception is null, then this method
     * calls {@link org.modeshape.common.logging.Logger#error(org.modeshape.common.i18n.I18nResource, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void error( Throwable t,
                       I18nResource message,
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
        logger.error(message.text(getLoggingLocale(), params), t);
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
    public void info( I18nResource message,
                      Object... params ) {
        if (!isInfoEnabled()) return;
        if (message == null) return;
        logger.info(message.text(getLoggingLocale(), params));
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message. If the exception is null, then this method
     * calls {@link org.modeshape.common.logging.Logger#info(org.modeshape.common.i18n.I18nResource, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    @Override
    public void info( Throwable t,
                      I18nResource message,
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
        logger.info(message.text(getLoggingLocale(), params), t);
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
