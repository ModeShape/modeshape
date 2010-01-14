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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.modeshape.common.i18n.I18n;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link Logger} class, ensuring that it uses Log4J appropriately. The {@link Logger} class uses the SLF4J generalized
 * logging framework, which can sit on top of multiple logging frameworks, including Log4J. Therefore, this test assumes that
 * SLF4J works correctly for all logging frameworks, then the {@link Logger} class can be tested by using it and checking the
 * resulting Log4J output.
 * <p>
 * To ensure that the Log4J configuration used in the remaining tests (in <code>src/test/resources/log4j.properties</code>)
 * does not interfere with this test, the underlying Log4J logger is obtained before each test and programmatically reconfigured
 * and, after each test, is then restored to it's previous state. This reconfiguration involves identifying and removing all of
 * the {@link Appender Log4J Appender} on the tree of Log4J loggers, and substituting a special {@link LogRecorder Appender} that
 * records the log messages in memory. During the test, this in-memory list of log messages is checked by the test case using
 * standard assertions to verify the proper order and content of the log messages. After each of the tests, all of the original
 * Adapters are restored to the appropriate Log4J loggers.
 * </p>
 */
public class LoggerTest {

    public static I18n errorMessageWithNoParameters;
    public static I18n warningMessageWithNoParameters;
    public static I18n infoMessageWithNoParameters;
    public static I18n errorMessageWithTwoParameters;
    public static I18n warningMessageWithTwoParameters;
    public static I18n infoMessageWithTwoParameters;
    public static I18n errorMessageWithException;
    public static I18n warningMessageWithException;
    public static I18n infoMessageWithException;
    public static I18n errorMessageWithNullException;
    public static I18n warningMessageWithNullException;
    public static I18n infoMessageWithNullException;
    public static I18n someMessage;

    private LogRecorder log;
    private Logger logger;
    private org.apache.log4j.Logger log4jLogger;
    private Map<String, List<Appender>> existingAppendersByLoggerName = new HashMap<String, List<Appender>>();

    @BeforeClass
    public static void beforeAll() {
        // Initialize the I18n static fields ...
        I18n.initialize(LoggerTest.class);
    }

    @Before
    public void beforeEach() {
        logger = Logger.getLogger(LoggerTest.class);

        // Find all of the existing appenders on all of the loggers, and
        // remove them all (keeping track of which appender they're on)
        log4jLogger = org.apache.log4j.Logger.getLogger(logger.getName());
        org.apache.log4j.Logger theLogger = log4jLogger;
        while (theLogger != null) {
            List<Appender> appenders = new ArrayList<Appender>();
            Enumeration<?> previousAppenders = theLogger.getAllAppenders();
            while (previousAppenders.hasMoreElements()) {
                appenders.add((Appender)previousAppenders.nextElement());
            }
            existingAppendersByLoggerName.put(theLogger.getName(), appenders);
            theLogger.removeAllAppenders();
            theLogger = (org.apache.log4j.Logger)theLogger.getParent();
        }

        // Set up the appender from which we can easily grab the content of the log during the tests.
        // This assumes we're using Log4J. Also, the Log4J properties should specify that the
        // logger for this particular class.
        log = new LogRecorder();
        log4jLogger = org.apache.log4j.Logger.getLogger(logger.getName());
        log4jLogger.addAppender(this.log);
        log4jLogger.setLevel(Level.ALL);
    }

    @After
    public void afterEach() {
        // Put all of the existing appenders onto the correct logger, and remove the testing appender ...
        for (Map.Entry<String, List<Appender>> entry : this.existingAppendersByLoggerName.entrySet()) {
            String loggerName = entry.getKey();
            List<Appender> appenders = entry.getValue();
            org.apache.log4j.Logger theLogger = org.apache.log4j.Logger.getLogger(loggerName);
            theLogger.removeAllAppenders(); // removes the testing appender, if on this logger
            for (Appender appender : appenders) {
                theLogger.addAppender(appender);
            }
        }
    }

    @Test
    public void shouldLogAppropriateMessagesIfSetToAllLevel() {
        log4jLogger.setLevel(Level.ALL);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        log.removeFirst(Logger.Level.INFO, "This is an info message with no parameters");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with no parameters");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogAppropriateMessagesIfLog4jSetToTraceLevel() {
        log4jLogger.setLevel(Level.TRACE);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        log.removeFirst(Logger.Level.INFO, "This is an info message with no parameters");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with no parameters");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogAppropriateMessagesIfLog4jSetToDebugLevel() {
        log4jLogger.setLevel(Level.DEBUG);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        log.removeFirst(Logger.Level.INFO, "This is an info message with no parameters");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogAppropriateMessagesIfLog4jSetToInfoLevel() {
        log4jLogger.setLevel(Level.INFO);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        log.removeFirst(Logger.Level.INFO, "This is an info message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogAppropriateMessagesIfLog4jSetToWarningLevel() {
        log4jLogger.setLevel(Level.WARN);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogAppropriateMessagesIfLog4jSetToErrorLevel() {
        log4jLogger.setLevel(Level.ERROR);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldLogNoMessagesIfLog4jSetToOffLevel() {
        log4jLogger.setLevel(Level.OFF);
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldNotAcceptMessageWithNonNullAndNullParameters() {
        logger.error(errorMessageWithTwoParameters, "first", null);
        logger.warn(warningMessageWithTwoParameters, "first", null);
        logger.info(infoMessageWithTwoParameters, "first", null);
        logger.debug("This is a debug message with a {0} parameter and the {1} parameter", "first", null);
        logger.trace("This is a trace message with a {0} parameter and the {1} parameter", "first", null);

        log.removeFirst(Logger.Level.ERROR, "This is an error message with a first parameter and the null parameter");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with a first parameter and the null parameter");
        log.removeFirst(Logger.Level.INFO, "This is an info message with a first parameter and the null parameter");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with a first parameter and the null parameter");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with a first parameter and the null parameter");
        assertEquals(false, log.hasEvents());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAcceptErrorMessageWithTooFewParameters() {
        logger.error(errorMessageWithTwoParameters, (Object[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAcceptWarningMessageWithTooFewParameters() {
        logger.warn(warningMessageWithTwoParameters, (Object[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAcceptInfoMessageWithTooFewParameters() {
        logger.info(infoMessageWithTwoParameters, (Object[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAcceptDebugMessageWithTooFewParameters() {
        logger.debug("This is a debug message with a {0} parameter and the {1} parameter", (Object[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAcceptTraceMessageWithTooFewParameters() {
        logger.trace("This is a trace message with a {0} parameter and the {1} parameter", (Object[])null);
    }

    @Test
    public void shouldAcceptMessageWithNoParameters() {
        logger.error(errorMessageWithNoParameters);
        logger.warn(warningMessageWithNoParameters);
        logger.info(infoMessageWithNoParameters);
        logger.debug("This is a debug message with no parameters");
        logger.trace("This is a trace message with no parameters");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with no parameters");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with no parameters");
        log.removeFirst(Logger.Level.INFO, "This is an info message with no parameters");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with no parameters");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with no parameters");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldAcceptMessageWithObjectAndPrimitiveParameters() {
        logger.error(errorMessageWithTwoParameters, "first", 2);
        logger.warn(warningMessageWithTwoParameters, "first", 2);
        logger.info(infoMessageWithTwoParameters, "first", 2);
        logger.debug("This is a debug message with a {0} parameter and the {1} parameter", "first", 2);
        logger.trace("This is a trace message with a {0} parameter and the {1} parameter", "first", 2);

        log.removeFirst(Logger.Level.ERROR, "This is an error message with a first parameter and the 2 parameter");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with a first parameter and the 2 parameter");
        log.removeFirst(Logger.Level.INFO, "This is an info message with a first parameter and the 2 parameter");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with a first parameter and the 2 parameter");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with a first parameter and the 2 parameter");
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldAcceptMessageAndThrowable() {
        Throwable t = new RuntimeException("This is the runtime exception message");
        logger.error(t, errorMessageWithException);
        logger.warn(t, warningMessageWithException);
        logger.info(t, infoMessageWithException);
        logger.debug(t, "This is a debug message with an exception");
        logger.trace(t, "This is a trace message with an exception");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with an exception", RuntimeException.class);
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with an exception", RuntimeException.class);
        log.removeFirst(Logger.Level.INFO, "This is an info message with an exception", RuntimeException.class);
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with an exception", RuntimeException.class);
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with an exception", RuntimeException.class);
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldAcceptMessageAndNullThrowable() {
        Throwable t = null;
        logger.error(t, errorMessageWithNullException);
        logger.warn(t, warningMessageWithNullException);
        logger.info(t, infoMessageWithNullException);
        logger.debug(t, "This is a debug message with a null exception");
        logger.trace(t, "This is a trace message with a null exception");

        log.removeFirst(Logger.Level.ERROR, "This is an error message with a null exception");
        log.removeFirst(Logger.Level.WARNING, "This is a warning message with a null exception");
        log.removeFirst(Logger.Level.INFO, "This is an info message with a null exception");
        log.removeFirst(Logger.Level.DEBUG, "This is a debug message with a null exception");
        log.removeFirst(Logger.Level.TRACE, "This is a trace message with a null exception");
        assertEquals(false, log.hasEvents());
    }

    public void shouldQuietlyAcceptNullMessage() {
        logger.error(null);
        logger.warn(null);
        logger.info(null);
        logger.debug(null);
        logger.trace(null);

        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldAcceptNullMessageAndThrowable() {
        Throwable t = new RuntimeException("This is the runtime exception message in LoggerTest");
        logger.error(t, null);
        logger.warn(t, null);
        logger.info(t, null);
        logger.debug(t, null);
        logger.trace(t, null);

        log.removeFirst(Logger.Level.ERROR, null, RuntimeException.class);
        log.removeFirst(Logger.Level.WARNING, null, RuntimeException.class);
        log.removeFirst(Logger.Level.INFO, null, RuntimeException.class);
        log.removeFirst(Logger.Level.DEBUG, null, RuntimeException.class);
        log.removeFirst(Logger.Level.TRACE, null, RuntimeException.class);
        assertEquals(false, log.hasEvents());
    }

    @Test
    public void shouldAcceptNullThrowableInError() {
        logger.error((Throwable)null, someMessage);
        logger.warn((Throwable)null, someMessage);
        logger.info((Throwable)null, someMessage);
        logger.debug((Throwable)null, "some message");
        logger.trace((Throwable)null, "some message");

        log.removeFirst(Logger.Level.ERROR, "some message");
        log.removeFirst(Logger.Level.WARNING, "some message");
        log.removeFirst(Logger.Level.INFO, "some message");
        log.removeFirst(Logger.Level.DEBUG, "some message");
        log.removeFirst(Logger.Level.TRACE, "some message");
    }

    @Test
    public void shouldSupportAskingWhetherLoggingLevelsAreEnabled() {
        logger.isErrorEnabled();
        logger.isWarnEnabled();
        logger.isInfoEnabled();
        logger.isDebugEnabled();
        logger.isTraceEnabled();
    }

    /**
     * A special Log4J Appender that records log messages and whose content can be
     * {@link #removeFirst(org.modeshape.common.util.Logger.Level, String, Class) validated} to ensure that the log contains
     * messages in the proper order and with the proper content.
     */
    public class LogRecorder extends WriterAppender {

        private final LinkedList<LoggingEvent> events = new LinkedList<LoggingEvent>();
        private int lineNumber;

        public LogRecorder( StringWriter writer ) {
            super(new SimpleLayout(), writer);
        }

        public LogRecorder() {
            this(new StringWriter());
        }

        @Override
        protected void subAppend( LoggingEvent event ) {
            super.subAppend(event);
            this.events.add(event);
        }

        public LoggingEvent removeFirst() {
            if (hasEvents()) {
                ++lineNumber;
                return this.events.removeFirst();
            }
            return null;
        }

        public boolean hasEvents() {
            return this.events.size() != 0;
        }

        /**
         * Remove the message that is currently at the front of the log, and verify that it contains the supplied information.
         * 
         * @param expectedLevel the level that the next log message should have
         * @param expectedMessageExpression the message that the next log message should have, or a regular expression that would
         *        match the log message
         * @param expectedExceptionClass the exception class that was expected, or null if there should not be an exception
         */
        public void removeFirst( Logger.Level expectedLevel,
                                 String expectedMessageExpression,
                                 Class<? extends Throwable> expectedExceptionClass ) {
            if (!hasEvents()) {
                fail("Expected log message but found none: " + expectedLevel + " - " + expectedMessageExpression);
            }
            LoggingEvent event = removeFirst();

            // Check the log message ...
            if (expectedMessageExpression != null && event.getMessage() == null) {
                fail("Log line " + lineNumber + " was missing expected message: " + expectedMessageExpression);
            } else if (expectedMessageExpression == null && event.getMessage() != null) {
                fail("Log line " + lineNumber + " had unexpected message: " + event.getMessage());
            } else if (expectedMessageExpression != null) {
                String actual = event.getMessage().toString();
                // Treat as a regular expression, which works for both regular expressions and strings ...
                if (!actual.matches(expectedMessageExpression)) {
                    fail("Log line " + lineNumber + " differed: \nwas     :\t" + actual + "\nexpected:\t"
                         + expectedMessageExpression);
                }
            } // else they are both null

            // Check the exception ...
            ThrowableInformation throwableInfo = event.getThrowableInformation();
            if (expectedExceptionClass == null && throwableInfo != null) {
                fail("Log line " + lineNumber + " had unexpected exception: "
                     + event.getThrowableInformation().getThrowableStrRep());
            } else if (expectedExceptionClass != null && throwableInfo == null) {
                fail("Log line " + lineNumber + " was missing expected exception of type "
                     + expectedExceptionClass.getCanonicalName());
            } else if (expectedExceptionClass != null && throwableInfo != null) {
                Throwable actualException = throwableInfo.getThrowable();
                assertSame(expectedExceptionClass, actualException.getClass());
            } // else they are both null
        }

        public void removeFirst( Logger.Level expectedLevel,
                                 String expectedMessageExpression ) {
            removeFirst(expectedLevel, expectedMessageExpression, null);
        }
    }

}
