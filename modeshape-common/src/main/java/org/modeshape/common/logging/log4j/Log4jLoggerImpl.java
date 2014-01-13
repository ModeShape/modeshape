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
        if (!isDebugEnabled()) return;
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
        if (!isDebugEnabled()) return;
        logger.debug(StringUtil.createString(message, params), t);
    }

    @Override
    public void error( I18nResource message,
                       Object... params ) {
        if (message == null) {
            return;
        }
        if (!isErrorEnabled()) return;
        logger.error(message.text(getLoggingLocale(), params));
    }

    @Override
    public void error( Throwable t,
                       I18nResource message,
                       Object... params ) {
        if (message == null) {
            return;
        }
        if (!isErrorEnabled()) return;
        logger.error(message.text(getLoggingLocale(), params), t);

    }

    @Override
    public void info( I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        if (!isInfoEnabled()) return;
        logger.info(message.text(getLoggingLocale(), params));
    }

    @Override
    public void info( Throwable t,
                      I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        if (!isInfoEnabled()) return;
        logger.info(message.text(getLoggingLocale(), params), t);
    }

    @Override
    public void trace( String message,
                       Object... params ) {
        if (StringUtil.isBlank(message)) {
            return;
        }
        if (!isTraceEnabled()) return;
        logger.trace(StringUtil.createString(message, params));
    }

    @Override
    public void trace( Throwable t,
                       String message,
                       Object... params ) {
        if (StringUtil.isBlank(message)) {
            return;
        }
        if (!isTraceEnabled()) return;
        logger.trace(StringUtil.createString(message, params), t);
    }

    @Override
    public void warn( I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        if (!isWarnEnabled()) return;
        logger.warn(message.text(getLoggingLocale(), params));
    }

    @Override
    public void warn( Throwable t,
                      I18nResource message,
                      Object... params ) {
        if (message == null) {
            return;
        }
        if (!isWarnEnabled()) return;
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
