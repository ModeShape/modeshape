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
package org.modeshape.common.logging.jdk;

import java.util.logging.Logger;
import org.modeshape.common.i18n.I18nResource;
import org.modeshape.common.util.StringUtil;

/**
 * Logger that delivers messages to a JDK logger
 * 
 * @since 2.5
 */
final class JdkLoggerImpl extends org.modeshape.common.logging.Logger {

    private final java.util.logging.Logger logger;

    public JdkLoggerImpl( String category ) {
        logger = Logger.getLogger(category);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private void log( java.util.logging.Level level,
                      String message,
                      Throwable ex ) {
        if (logger.isLoggable(level)) {
            Throwable dummyException = new Throwable();
            StackTraceElement locations[] = dummyException.getStackTrace();
            String className = "unknown";
            String methodName = "unknown";
            int depth = 2;
            if (locations != null && locations.length > depth) {
                StackTraceElement caller = locations[depth];
                className = caller.getClassName();
                methodName = caller.getMethodName();
            }
            if (ex == null) {
                logger.logp(level, className, methodName, message);
            } else {
                logger.logp(level, className, methodName, message, ex);
            }
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(java.util.logging.Level.FINER);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(java.util.logging.Level.FINE);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(java.util.logging.Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(java.util.logging.Level.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(java.util.logging.Level.SEVERE);
    }

    @Override
    public void debug( String message,
                       Object... params ) {
        log(java.util.logging.Level.FINE, StringUtil.createString(message, params), null);
    }

    @Override
    public void debug( Throwable t,
                       String message,
                       Object... params ) {
        log(java.util.logging.Level.FINE, StringUtil.createString(message, params), t);
    }

    @Override
    public void error( I18nResource message,
                       Object... params ) {
        log(java.util.logging.Level.SEVERE, message.text(getLoggingLocale(), params), null);
    }

    @Override
    public void error( Throwable t,
                       I18nResource message,
                       Object... params ) {
        log(java.util.logging.Level.SEVERE, message.text(getLoggingLocale(), params), t);
    }

    @Override
    public void info( I18nResource message,
                      Object... params ) {
        log(java.util.logging.Level.INFO, message.text(getLoggingLocale(), params), null);
    }

    @Override
    public void info( Throwable t,
                      I18nResource message,
                      Object... params ) {
        log(java.util.logging.Level.INFO, message.text(getLoggingLocale(), params), t);
    }

    @Override
    public void trace( String message,
                       Object... params ) {
        log(java.util.logging.Level.FINER, StringUtil.createString(message, params), null);
    }

    @Override
    public void trace( Throwable t,
                       String message,
                       Object... params ) {
        log(java.util.logging.Level.FINER, StringUtil.createString(message, params), t);

    }

    @Override
    public void warn( I18nResource message,
                      Object... params ) {
        log(java.util.logging.Level.WARNING, message.text(getLoggingLocale(), params), null);
    }

    @Override
    public void warn( Throwable t,
                      I18nResource message,
                      Object... params ) {
        log(java.util.logging.Level.WARNING, message.text(getLoggingLocale(), params), t);

    }
}
