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

import org.modeshape.common.util.ClassUtil;
import org.modeshape.common.logging.jdk.JdkLoggerFactory;
import org.modeshape.common.logging.log4j.Log4jLoggerFactory;
import org.modeshape.common.logging.slf4j.SLF4JLoggerFactory;

/**
 * The abstract class for the LogFactory, which is called to create a specific implementation of the {@link Logger}.
 * <p>
 * ModeShape provides out of the box several LogFactory implementations that work with common log frameworks:
 * <ol>
 *  <li>SLF4J (which sits atop several logging frameworks)</li>
 *  <li>Log4J</li>
 *  <li>JDK Util Logging</li>
 * </ol>
 * The static initializer for this class checks the classpath for the availability of these frameworks, and as soon as one 
 * is found the LogFactory implementation for that framework is instantiated and used for all ModeShape logging.
 * </p>
 * <p>
 * However, since ModeShape can be embedded into any application, it is possible that applications use a logging
 * framework other than those listed above. So before falling back to the JDK logging, ModeShape looks for the 
 * <code>org.modeshape.common.logging.CustomLoggerFactory</code> class, and if found attempts to instantiate and use it. 
 * But ModeShape does not provide this class out of the box; rather an application that is embedding ModeShape can provide 
 * its own version of that class that should extend {@link LogFactory} and create an appropriate implementation of 
 * {@link Logger} that forwards ModeShape log messages to the application's logging framework.
 * </p>
 */
public abstract class LogFactory {
    
    /**
     * The name of the {@link LogFactory} implementation that is not provided out of the box but can be created,
     * implemented, and placed on the classpath to have ModeShape send log messages to a custom framework.
     */
    public static final String CUSTOM_LOG_FACTORY_CLASSNAME = "org.modeshape.common.logging.CustomLoggerFactory";

    private static LogFactory LOGFACTORY;

    static {
        if (isSLF4JAvailable()) {
            LOGFACTORY = new SLF4JLoggerFactory();
        }
        else if (isLog4jAvailable()) {
            LOGFACTORY = new Log4jLoggerFactory();
        }
        else if (isCustomLoggerAvailable()) {
            try {
                @SuppressWarnings( "unchecked" )
                Class<LogFactory> customClass = (Class<LogFactory>)Class.forName(CUSTOM_LOG_FACTORY_CLASSNAME);
                LOGFACTORY = customClass.newInstance();
            } catch ( Throwable e ) {
                // We're going to fallback to the JDK logger anyway, so use it and log this problem ...
                LOGFACTORY = new JdkLoggerFactory();
                java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(LogFactory.class.getName());
                String msg = org.modeshape.common.CommonI18n.errorInitializingCustomLoggerFactory.text(CUSTOM_LOG_FACTORY_CLASSNAME);
                jdkLogger.log(java.util.logging.Level.WARNING, msg,e);
            }
        }
        else {
            LOGFACTORY = new JdkLoggerFactory();
        }
    }

    private static boolean isSLF4JAvailable() {
        try {
            //check if the api is in the classpath
            ClassUtil.loadClassStrict("org.slf4j.LoggerFactory");
            ClassUtil.loadClassStrict("org.slf4j.Logger");

            //check if there's at least one implementation
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

    private static boolean isCustomLoggerAvailable() {
        try {
            ClassUtil.loadClassStrict(CUSTOM_LOG_FACTORY_CLASSNAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static LogFactory getLogFactory() {
        return LOGFACTORY;
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
