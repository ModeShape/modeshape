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

package org.modeshape.jcr;

import org.modeshape.common.i18n.TextI18n;
import org.modeshape.common.logging.Logger;

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

    /**
     * Creates a new logger instance for the underlying class.
     *
     * @param clazz a {@link Class} instance; never null
     * @return a {@link org.modeshape.jcr.api.Logger} implementation
     */
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
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
