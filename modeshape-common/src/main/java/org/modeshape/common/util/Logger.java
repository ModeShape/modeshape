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
package org.modeshape.common.util;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.log.LogFactory;

/**
 * A simple logging interface that is fully compatible with multiple logging
 * implementations. If no log4j implementation is found, then its defaulted to
 * the JDK Logger implementation. This interface does take advantage of the
 * variable arguments and autoboxing features in Java 5, reducing the number of
 * methods that are necessary and allowing callers to supply primitive values as
 * parameters.
 */
@ThreadSafe
public abstract class Logger {

    static final LogFactory LOG_FACTORY;

    public enum Level {
	OFF, ERROR, WARNING, INFO, DEBUG, TRACE;
    }

    static {
	LOG_FACTORY = LogFactory.getLogFactory();

    }

    protected static final AtomicReference<Locale> LOGGING_LOCALE = new AtomicReference<Locale>(
	    null);

    /**
     * Get the locale used for the logs. If null, the
     * {@link Locale#getDefault() default locale} is used.
     * 
     * @return the current locale used for logging, or null if the system locale
     *         is used
     * @see #setLoggingLocale(Locale)
     */
    public static Locale getLoggingLocale() {
	return LOGGING_LOCALE.get();
    }

    /**
     * Set the locale used for the logs. This should be used when the logs are
     * to be written is a specific locale, independent of the
     * {@link Locale#getDefault() default locale}. To use the default locale,
     * call this method with a null value.
     * 
     * @param locale
     *            the desired locale to use for the logs, or null if the system
     *            locale should be used
     * @return the previous locale
     * @see #getLoggingLocale()
     */
    public static Locale setLoggingLocale(Locale locale) {
	return LOGGING_LOCALE.getAndSet(locale != null ? locale : Locale
		.getDefault());
    }

    /**
     * Return a logger named corresponding to the class passed as parameter,
     * using the statically bound {@link ILoggerFactory} instance.
     * 
     * @param clazz
     *            the returned logger will be named after clazz
     * @return logger
     */
    public static Logger getLogger(Class<?> clazz) {
	return LOG_FACTORY.getLogger(clazz);
    }

    /**
     * Return a logger named according to the name parameter using the
     * statically bound {@link ILoggerFactory} instance.
     * 
     * @param name
     *            The name of the logger.
     * @return logger
     */
    public static Logger getLogger(String name) {
	return LOG_FACTORY.getLogger(name);
    }

    /**
     * Return the name of this logger instance.
     * 
     * @return the logger's name
     */
    public abstract String getName();

    /**
     * Log a message at the suplied level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the desired level.
     * 
     * @param level
     *            the level at which to log
     * @param message
     *            the (localized) message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public void log(Level level, I18n message, Object... params) {
	if (message == null)
	    return;
	switch (level) {
	case DEBUG:
	    debug(message.text(LOGGING_LOCALE.get(), params));
	    break;
	case ERROR:
	    error(message, params);
	    break;
	case INFO:
	    info(message, params);
	    break;
	case TRACE:
	    trace(message.text(LOGGING_LOCALE.get(), params));
	    break;
	case WARNING:
	    warn(message, params);
	    break;
	case OFF:
	    break;
	}
    }

    /**
     * Log an exception (throwable) at the supplied level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #debug(String, Object...)}.
     * 
     * @param level
     *            the level at which to log
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public void log(Level level, Throwable t, I18n message, Object... params) {
	if (message == null)
	    return;
	switch (level) {
	case DEBUG:
	    debug(t, message.text(LOGGING_LOCALE.get(), params));
	    break;
	case ERROR:
	    error(t, message, params);
	    break;
	case INFO:
	    info(t, message, params);
	    break;
	case TRACE:
	    trace(t, message.text(LOGGING_LOCALE.get(), params));
	    break;
	case WARNING:
	    warn(t, message, params);
	    break;
	case OFF:
	    break;
	}
    }

    /**
     * Log a message at the DEBUG level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the DEBUG level.
     * 
     * @param message
     *            the message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void debug(String message, Object... params);

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #debug(String, Object...)}.
     * 
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void debug(Throwable t, String message, Object... params);

    /**
     * Log a message at the ERROR level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the ERROR level.
     * 
     * @param message
     *            the message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void error(I18n message, Object... params);

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #error(I18n, Object...)}.
     * 
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void error(Throwable t, I18n message, Object... params);

    /**
     * Log a message at the INFO level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the INFO level.
     * 
     * @param message
     *            the message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void info(I18n message, Object... params);

    /**
     * Log an exception (throwable) at the INFO level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #info(I18n, Object...)}.
     * 
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void info(Throwable t, I18n message, Object... params);

    /**
     * Log a message at the TRACE level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the TRACE level.
     * 
     * @param message
     *            the message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void trace(String message, Object... params);

    /**
     * Log an exception (throwable) at the TRACE level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #trace(String, Object...)}.
     * 
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void trace(Throwable t, String message, Object... params);

    /**
     * Log a message at the WARNING level according to the specified format and
     * (optional) parameters. The message should contain a pair of empty curly
     * braces for each of the parameter, which should be passed in the correct
     * order. This method is efficient and avoids superfluous object creation
     * when the logger is disabled for the WARNING level.
     * 
     * @param message
     *            the message string
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void warn(I18n message, Object... params);

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying
     * message. If the exception is null, then this method calls
     * {@link #warn(I18n, Object...)}.
     * 
     * @param t
     *            the exception (throwable) to log
     * @param message
     *            the message accompanying the exception
     * @param params
     *            the parameter values that are to replace the variables in the
     *            format string
     */
    public abstract void warn(Throwable t, I18n message, Object... params);

    /**
     * Return whether messages at the INFORMATION level are being logged.
     * 
     * @return true if INFORMATION log messages are currently being logged, or
     *         false otherwise.
     */
    public abstract boolean isInfoEnabled();

    /**
     * Return whether messages at the WARNING level are being logged.
     * 
     * @return true if WARNING log messages are currently being logged, or false
     *         otherwise.
     */
    public abstract boolean isWarnEnabled();

    /**
     * Return whether messages at the ERROR level are being logged.
     * 
     * @return true if ERROR log messages are currently being logged, or false
     *         otherwise.
     */
    public abstract boolean isErrorEnabled();

    /**
     * Return whether messages at the DEBUG level are being logged.
     * 
     * @return true if DEBUG log messages are currently being logged, or false
     *         otherwise.
     */
    public abstract boolean isDebugEnabled();

    /**
     * Return whether messages at the TRACE level are being logged.
     * 
     * @return true if TRACE log messages are currently being logged, or false
     *         otherwise.
     */
    public abstract boolean isTraceEnabled();

    /**
     * Get the logging level at which this logger is current set.
     * 
     * @return the current logging level
     */
    public Level getLevel() {
	if (this.isTraceEnabled())
	    return Level.TRACE;
	if (this.isDebugEnabled())
	    return Level.DEBUG;
	if (this.isInfoEnabled())
	    return Level.INFO;
	if (this.isWarnEnabled())
	    return Level.WARNING;
	if (this.isErrorEnabled())
	    return Level.ERROR;
	return Level.OFF;
    }

}
