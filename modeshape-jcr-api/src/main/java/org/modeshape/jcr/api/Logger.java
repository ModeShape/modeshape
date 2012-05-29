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

package org.modeshape.jcr.api;

/**
 * A generic logger interface.
 *
 * @author Horia Chiorean
 */
public interface Logger {

    /**
     * Log a message at the DEBUG level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order.
     *
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public void debug( String message,
                       Object... params );

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying message. If the exception is null, then this method
     * calls {@link #debug(String, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void debug( Throwable t,
                                String message,
                                Object... params );

    /**
     * Log a message at the ERROR level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order.
     *
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void error( String message,
                                Object... params );

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message. If the exception is null, then this method
     * calls {@link #error(String, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void error( Throwable t,
                                String message,
                                Object... params );

    /**
     * Log a message at the INFO level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order.
     *
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void info( String message,
                               Object... params );

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message. If the exception is null, then this method
     * calls {@link #info(String, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void info( Throwable t,
                               String message,
                               Object... params );

    /**
     * Log a message at the TRACE level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order.
     *
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void trace( String message,
                                Object... params );

    /**
     * Log an exception (throwable) at the TRACE level with an accompanying message. If the exception is null, then this method
     * calls {@link #trace(String, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void trace( Throwable t,
                                String message,
                                Object... params );

    /**
     * Log a message at the WARNING level according to the specified format and (optional) parameters. The message should contain
     * a pair of empty curly braces for each of the parameter, which should be passed in the correct order.
     *
     * @param message the message string
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void warn( String message,
                               Object... params );

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying message. If the exception is null, then this method
     * calls {@link #warn(String, Object...)}.
     *
     * @param t the exception (throwable) to log
     * @param message the message accompanying the exception
     * @param params the parameter values that are to replace the variables in the format string
     */
    public abstract void warn( Throwable t,
                               String message,
                               Object... params );
}
