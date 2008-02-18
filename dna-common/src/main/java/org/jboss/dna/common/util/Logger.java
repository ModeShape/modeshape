/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.util;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * A simple logging interface that is fully compatible with multiple logging implementations. This interface does take advantage
 * of the variable arguments and autoboxing features in Java 5, reducing the number of methods that are necessary and allowing
 * callers to supply primitive values as parameters.
 */
public class Logger {

    public enum Level {
        OFF,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        TRACE;
    }

    /**
     * Return a logger named corresponding to the class passed as parameter, using the statically bound {@link ILoggerFactory}
     * instance.
     * @param clazz the returned logger will be named after clazz
     * @return logger
     */
    public static Logger getLogger( Class clazz ) {
        return new Logger(LoggerFactory.getLogger(clazz));
    }

    /**
     * Return a logger named according to the name parameter using the statically bound {@link ILoggerFactory} instance.
     * @param name The name of the logger.
     * @return logger
     */
    public static Logger getLogger( String name ) {
        return new Logger(LoggerFactory.getLogger(name));
    }

    private org.slf4j.Logger delegate;

    private Logger( org.slf4j.Logger delegate ) {
        this.delegate = delegate;
    }

    /**
     * Return the name of this logger instance.
     * @return the logger's name
     */
    public String getName() {
        return this.delegate.getName();
    }

    /**
     * Log a message at the DEBUG level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the DEBUG level.
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void debug( String message, Object... params ) {
        if (message == null) return;
        if (params == null || params.length == 0) {
            this.delegate.debug(message);
        } else {
            this.delegate.debug(message, params);
        }
    }

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying message. If the exception is null, then this method
     * calls {@link #debug(String, Object...)}.
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void debug( Throwable t, String message, Object... params ) {
        if (t == null) {
            this.debug(message, params);
            return;
        }
        if (message == null) {
            this.delegate.debug(null, t);
            return;
        }
        this.delegate.debug(StringUtil.createString(message, params), t);
    }

    /**
     * Log a message at the ERROR level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the ERROR level.
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void error( String message, Object... params ) {
        if (message == null) return;
        if (params == null || params.length == 0) {
            this.delegate.error(message);
        } else {
            this.delegate.error(message, params);
        }
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message. If the exception is null, then this method
     * calls {@link #error(String, Object...)}.
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void error( Throwable t, String message, Object... params ) {
        if (t == null) {
            this.error(message, params);
            return;
        }
        if (message == null) {
            this.delegate.error(null, t);
            return;
        }
        this.delegate.error(StringUtil.createString(message, params), t);
    }

    /**
     * Log a message at the INFO level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the INFO level.
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void info( String message, Object... params ) {
        if (message == null) return;
        if (params == null || params.length == 0) {
            this.delegate.info(message);
        } else {
            this.delegate.info(message, params);
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message. If the exception is null, then this method
     * calls {@link #info(String, Object...)}.
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void info( Throwable t, String message, Object... params ) {
        if (t == null) {
            this.info(message, params);
            return;
        }
        if (message == null) {
            this.delegate.info(null, t);
            return;
        }
        this.delegate.info(StringUtil.createString(message, params), t);
    }

    /**
     * Log a message at the TRACE level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is efficient
     * and avoids superfluous object creation when the logger is disabled for the TRACE level.
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void trace( String message, Object... params ) {
        if (message == null) return;
        if (params == null || params.length == 0) {
            this.delegate.trace(message);
        } else {
            this.delegate.trace(message, params);
        }
    }

    /**
     * Log an exception (throwable) at the TRACE level with an accompanying message. If the exception is null, then this method
     * calls {@link #trace(String, Object...)}.
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void trace( Throwable t, String message, Object... params ) {
        if (t == null) {
            this.trace(message, params);
            return;
        }
        if (message == null) {
            this.delegate.trace(null, t);
            return;
        }
        this.delegate.trace(StringUtil.createString(message, params), t);
    }

    /**
     * Log a message at the WARNING level according to the specified format and (optional) parameters. The message should contain
     * a pair of empty curly braces for each of the parameter, which should be passed in the correct order. This method is
     * efficient and avoids superfluous object creation when the logger is disabled for the WARNING level.
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void warn( String message, Object... params ) {
        if (message == null) return;
        if (params == null || params.length == 0) {
            this.delegate.warn(message);
        } else {
            this.delegate.warn(message, params);
        }
    }

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying message. If the exception is null, then this method
     * calls {@link #warn(String, Object...)}.
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void warn( Throwable t, String message, Object... params ) {
        if (t == null) {
            this.warn(message, params);
            return;
        }
        if (message == null) {
            this.delegate.warn(null, t);
            return;
        }
        this.delegate.warn(StringUtil.createString(message, params), t);
    }

    /**
     * Return whether messages at the INFORMATION level are being logged.
     * @return true if INFORMATION log messages are currently being logged, or false otherwise.
     */
    public boolean isInfoEnabled() {
        return this.delegate.isInfoEnabled();
    }

    /**
     * Return whether messages at the WARNING level are being logged.
     * @return true if WARNING log messages are currently being logged, or false otherwise.
     */
    public boolean isWarnEnabled() {
        return this.delegate.isWarnEnabled();
    }

    /**
     * Return whether messages at the ERROR level are being logged.
     * @return true if ERROR log messages are currently being logged, or false otherwise.
     */
    public boolean isErrorEnabled() {
        return this.delegate.isErrorEnabled();
    }

    /**
     * Return whether messages at the DEBUG level are being logged.
     * @return true if DEBUG log messages are currently being logged, or false otherwise.
     */
    public boolean isDebugEnabled() {
        return this.delegate.isDebugEnabled();
    }

    /**
     * Return whether messages at the TRACE level are being logged.
     * @return true if TRACE log messages are currently being logged, or false otherwise.
     */
    public boolean isTraceEnabled() {
        return this.delegate.isTraceEnabled();
    }

    /**
     * Get the logging level at which this logger is current set.
     * @return the current logging level
     */
    public Level getLevel() {
        if (this.isTraceEnabled()) return Level.TRACE;
        if (this.isDebugEnabled()) return Level.DEBUG;
        if (this.isInfoEnabled()) return Level.INFO;
        if (this.isWarnEnabled()) return Level.WARNING;
        if (this.isErrorEnabled()) return Level.ERROR;
        return Level.OFF;
    }

}
