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

package org.modeshape.common.logging;

import org.modeshape.common.logging.jdk.JdkLoggerFactory;
import org.modeshape.common.logging.log4j.Log4jLoggerFactory;
import org.modeshape.common.logging.slf4j.SLF4JLoggerFactory;
import org.modeshape.common.util.ClassUtil;

/**
 * The abstract class for the LogFactory, which is called to create a specific implementation of the {@link Logger}.
 */
public abstract class LogFactory {

    private static volatile LogFactory LOGFACTORY;

    static {
        if (isSLF4JAvailable()) {
            LOGFACTORY = new SLF4JLoggerFactory();
        } else if (isLog4jAvailable()) {
            LOGFACTORY = new Log4jLoggerFactory();
        } else {
            LOGFACTORY = new JdkLoggerFactory();
        }
    }

    private static boolean isSLF4JAvailable() {
        try {
            // check if the api is in the classpath
            ClassUtil.loadClassStrict("org.slf4j.LoggerFactory");
            ClassUtil.loadClassStrict("org.slf4j.Logger");

            // check if there's at least one implementation
            ClassUtil.loadClassStrict("org.slf4j.impl.StaticLoggerBinder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isLog4jAvailable() {
        try {
            ClassUtil.loadClassStrict("org.apache.log4j.Logger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static LogFactory getLogFactory() {
        return LOGFACTORY;
    }

    /**
     * Manually set the log factory that should be used. This is package-protected so that it cannot be directly called by
     * subclasses.
     * 
     * @param logFactory the log factory that should be used from now forward
     * @see Logger#useCustomLogging(LogFactory)
     */
    static void setLogFactory( LogFactory logFactory ) {
        if (logFactory != null && LOGFACTORY != logFactory && !LOGFACTORY.getClass().equals(logFactory.getClass())) {
            // Start using the new log factory ...
            LOGFACTORY = logFactory;
        }
    }

    /**
     * Return a logger named corresponding to the class passed as parameter.
     * 
     * @param clazz the returned logger will be named after clazz
     * @return logger
     */
    Logger getLogger( Class<?> clazz ) {
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Return a logger named according to the name parameter.
     * 
     * @param name The name of the logger.
     * @return logger
     */
    protected abstract Logger getLogger( String name );

}
