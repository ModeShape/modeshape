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

import java.util.logging.Logger;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.StringUtil;

/**
 * Logger that delivers messages to a JDK logger
 * 
 * @since 2.5
 */
public class JdkLoggerImpl extends org.modeshape.common.util.Logger {

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
    public void error( I18n message,
                       Object... params ) {
        log(java.util.logging.Level.SEVERE, message.text(LOGGING_LOCALE.get(), params), null);
    }

    @Override
    public void error( Throwable t,
                       I18n message,
                       Object... params ) {
        log(java.util.logging.Level.SEVERE, message.text(LOGGING_LOCALE.get(), params), t);
    }

    @Override
    public void info( I18n message,
                      Object... params ) {
        log(java.util.logging.Level.INFO, message.text(LOGGING_LOCALE.get(), params), null);
    }

    @Override
    public void info( Throwable t,
                      I18n message,
                      Object... params ) {
        log(java.util.logging.Level.INFO, message.text(LOGGING_LOCALE.get(), params), t);
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
        // TODO Auto-generated method stub
        log(java.util.logging.Level.FINER, StringUtil.createString(message, params), t);

    }

    @Override
    public void warn( I18n message,
                      Object... params ) {
        log(java.util.logging.Level.WARNING, message.text(LOGGING_LOCALE.get(), params), null);
    }

    @Override
    public void warn( Throwable t,
                      I18n message,
                      Object... params ) {
        log(java.util.logging.Level.WARNING, message.text(LOGGING_LOCALE.get(), params), t);

    }
}
