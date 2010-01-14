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

import net.jcip.annotations.Immutable;
import org.slf4j.MDC;

/**
 * Provides a "mapped diagnostic context" (MDC) for use in capturing extra context information to be included in logs of
 * multithreaded applications. Not all logging implementations support MDC, although a few do (including <a
 * href="http://logging.apache.org/log4j/1.3/index.html">Log4J</a> and <a href="http://logback.qos.ch/">Logback</a>). Note that if
 * the logging implementation does not support MDC, this information is ignored.
 * <p>
 * It can be difficult to understand what is going on within a multithreaded application. When multiple threads are working
 * simultaneously, their log messages are mixed together. Thus, it's difficult to follow the log messages of a single thread. Log
 * contexts provide a way to associate additional information with "the current context", and log messages can include that
 * additional information in the messages.
 * </p>
 * <p>
 * Log contexts are managed for you, and so using them is very straightforward. Typically, log contexts are used within
 * well-defined activities, and additional information is recorded in the context at the beginning of the activity and cleared at
 * the end of the activity.
 * </p>
 * <p>
 * The following example shows how to set and clear this additional information:
 * 
 * <pre>
 *   LogContext.set(&quot;username&quot;,&quot;jsmith&quot;);
 *   LogContext.set(&quot;operation&quot;,&quot;process&quot;);
 *   ...
 *   // do work here
 *   ...
 *   LogContext.clear();
 * </pre>
 * 
 * Note that the actually values would not be hardcoded but would be retrieved from other objects available at the time.
 * </p>
 * <p>
 * If the logging system doesn't support MDC, then the additional information provided via LogContext is ignored. However, if the
 * logging system is able to use MDC and it is set up with patterns that reference the keys, then those log messages will contain
 * the values for those keys.
 * </p>
 */
@Immutable
public class LogContext {

    /**
     * Put a context value (the <code>val</code> parameter) as identified with the <code>key</code> parameter into the current
     * thread's context map. The <code>key</code> parameter cannot be null. The code>val</code> parameter can be null only if the
     * underlying implementation supports it.
     * <p>
     * This method delegates all work to the MDC of the underlying logging system.
     * 
     * @param key the key
     * @param value the value
     * @throws IllegalArgumentException in case the "key" parameter is null
     */
    public static void set( String key,
                            String value ) {
        MDC.put(key, value);
    }

    /**
     * Get the context identified by the <code>key</code> parameter. The <code>key</code> parameter cannot be null.
     * <p>
     * This method delegates all work to the MDC of the underlying logging system.
     * 
     * @param key the key
     * @return the string value identified by the <code>key</code> parameter.
     * @throws IllegalArgumentException in case the "key" parameter is null
     */
    public static String get( String key ) {
        return MDC.get(key);
    }

    /**
     * Remove the the context identified by the <code>key</code> parameter using the underlying system's MDC implementation. The
     * <code>key</code> parameter cannot be null. This method does nothing if there is no previous value associated with
     * <code>key</code>.
     * 
     * @param key the key
     * @throws IllegalArgumentException in case the "key" parameter is null
     */
    public static void remove( String key ) {
        MDC.remove(key);
    }

    /**
     * Clear all entries in the MDC of the underlying implementation.
     */
    public static void clear() {
        MDC.clear();
    }

}
