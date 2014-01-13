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
package org.modeshape.jcr.api;

/**
 * A generic logger interface.
 *
 * @author Horia Chiorean
 */
public interface Logger {

    /**
     * Log a message at the DEBUG level according to the specified format and (optional) parameters. The message should contain a
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 0. Therefore, the first
     * parameter replaces all occurrences of "{0}", the second parameter replaces all occurrences of "{1}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
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
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 0. Therefore, the first
     * parameter replaces all occurrences of "{0}", the second parameter replaces all occurrences of "{1}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
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
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 0. Therefore, the first
     * parameter replaces all occurrences of "{0}", the second parameter replaces all occurrences of "{1}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
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
     * pair of empty curly braces for each of the parameter, which should be passed in the correct order. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 0. Therefore, the first
     * parameter replaces all occurrences of "{0}", the second parameter replaces all occurrences of "{1}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
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
     * a pair of empty curly braces for each of the parameter, which should be passed in the correct order. The pattern consists of
     * zero or more keys of the form <code>{n}</code>, where <code>n</code> is an integer starting at 0. Therefore, the first
     * parameter replaces all occurrences of "{0}", the second parameter replaces all occurrences of "{1}", etc.
     * <p>
     * If any parameter is null, the corresponding key is replaced with the string "null". Therefore, consider using an empty
     * string when keys are to be removed altogether.
     * </p>
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

    /**
     * Return whether messages at the INFORMATION level are being logged.
     *
     * @return true if INFORMATION log messages are currently being logged, or false otherwise.
     */
    public abstract boolean isInfoEnabled();

    /**
     * Return whether messages at the WARNING level are being logged.
     *
     * @return true if WARNING log messages are currently being logged, or false otherwise.
     */
    public abstract boolean isWarnEnabled();

    /**
     * Return whether messages at the ERROR level are being logged.
     *
     * @return true if ERROR log messages are currently being logged, or false otherwise.
     */
    public abstract boolean isErrorEnabled();

    /**
     * Return whether messages at the DEBUG level are being logged.
     *
     * @return true if DEBUG log messages are currently being logged, or false otherwise.
     */
    public abstract boolean isDebugEnabled();

    /**
     * Return whether messages at the TRACE level are being logged.
     *
     * @return true if TRACE log messages are currently being logged, or false otherwise.
     */
    public abstract boolean isTraceEnabled();
}
