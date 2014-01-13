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
package org.modeshape.common.logging;

import org.modeshape.common.CommonI18n;
import org.modeshape.common.logging.jdk.JdkLoggerFactory;
import org.modeshape.common.logging.log4j.Log4jLoggerFactory;
import org.modeshape.common.logging.slf4j.SLF4JLoggerFactory;

/**
 * The abstract class for the LogFactory, which is called to create a specific implementation of the {@link Logger}.
 * <p>
 * ModeShape provides out of the box several LogFactory implementations that work with common log frameworks:
 * <ol>
 * <li>SLF4J (which sits atop several logging frameworks)</li>
 * <li>Log4J</li>
 * <li>JDK Util Logging</li>
 * </ol>
 * The static initializer for this class checks the classpath for the availability of these frameworks, and as soon as one is
 * found the LogFactory implementation for that framework is instantiated and used for all ModeShape logging.
 * </p>
 * <p>
 * However, since ModeShape can be embedded into any application, it is possible that applications use a logging framework other
 * than those listed above. So before falling back to the JDK logging, ModeShape looks for the
 * <code>org.modeshape.common.logging.CustomLoggerFactory</code> class, and if found attempts to instantiate and use it. But
 * ModeShape does not provide this class out of the box; rather an application that is embedding ModeShape can provide its own
 * version of that class that should extend {@link LogFactory} and create an appropriate implementation of {@link Logger} that
 * forwards ModeShape log messages to the application's logging framework.
 * </p>
 */
public abstract class LogFactory {

    /**
     * The name of the {@link LogFactory} implementation that is not provided out of the box but can be created, implemented, and
     * placed on the classpath to have ModeShape send log messages to a custom framework.
     */
    public static final String CUSTOM_LOG_FACTORY_CLASSNAME = "org.modeshape.common.logging.CustomLoggerFactory";

    private static LogFactory LOGFACTORY;

    static {
        Throwable customLoggingError = null;
        boolean customLogging = false;
        boolean slf4jLogging = false;
        boolean log4jLogging = false;

        if (isCustomLoggerAvailable()) {
            try {
                @SuppressWarnings( "unchecked" )
                Class<LogFactory> customClass = (Class<LogFactory>)Class.forName(CUSTOM_LOG_FACTORY_CLASSNAME);
                LOGFACTORY = customClass.newInstance();
                customLogging = true;
            } catch (Throwable throwable) {
                customLoggingError = throwable;
            }
        }

        if (LOGFACTORY == null) {
            if (isSLF4JAvailable()) {
                LOGFACTORY = new SLF4JLoggerFactory();
                slf4jLogging = true;
            } else if (isLog4jAvailable()) {
                LOGFACTORY = new Log4jLoggerFactory();
                log4jLogging = true;
            } else {
                LOGFACTORY = new JdkLoggerFactory();
            }
        }

        Logger logger = LOGFACTORY.getLogger(LogFactory.class.getName());

        if (customLogging) {
            logger.info(CommonI18n.customLoggingAvailable, CUSTOM_LOG_FACTORY_CLASSNAME);
        } else if (slf4jLogging) {
            logger.info(CommonI18n.slf4jAvailable);
        } else if (log4jLogging) {
            logger.info(CommonI18n.log4jAvailable);
        } else {
            logger.info(CommonI18n.jdkFallback);
        }

        if (customLoggingError != null) {
            // log the problem related to the custom logger
            logger.warn(customLoggingError,
                        CommonI18n.errorInitializingCustomLoggerFactory,
                        CUSTOM_LOG_FACTORY_CLASSNAME);
        }
    }

    private static boolean isSLF4JAvailable() {
        try {
            // check if the api is in the classpath and initialize the classes
            Class.forName("org.slf4j.Logger");
            Class.forName("org.slf4j.LoggerFactory");

            // check if there's at least one implementation and initialize the classes
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isLog4jAvailable() {
        try {
            // Check if the Log4J main interface is in the classpath and initialize the class
            Class.forName("org.apache.log4j.Logger");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isCustomLoggerAvailable() {
        try {
            // Check if a custom log factory implementation is in the classpath and initialize the class
            Class.forName(CUSTOM_LOG_FACTORY_CLASSNAME);
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
