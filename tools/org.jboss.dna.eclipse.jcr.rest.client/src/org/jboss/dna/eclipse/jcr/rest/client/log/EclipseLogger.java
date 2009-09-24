/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client.log;

import java.text.MessageFormat;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jboss.dna.eclipse.jcr.rest.client.IUiConstants;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * The <code>EclipseLogger</code> class provides an <code>org.slf4j.Logger</code> implementation that uses the Eclipse logger.
 */
public final class EclipseLogger implements Logger {

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    private static ILog LOGGER = Platform.getLog(Platform.getBundle(IUiConstants.PLUGIN_ID));

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private String name;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    EclipseLogger( String name ) {
        this.name = name;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(java.lang.String)
     */
    @Override
    public void debug( String message ) {
        if (isDebugEnabled()) info(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object)
     */
    @Override
    public void debug( String pattern,
                       Object arg ) {
        if (isDebugEnabled()) info(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object[])
     */
    @Override
    public void debug( String pattern,
                       Object[] arguments ) {
        if (isDebugEnabled()) info(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void debug( String message,
                       Throwable e ) {
        if (isDebugEnabled()) info(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String)
     */
    @Override
    public void debug( Marker marker,
                       String message ) {
        debug(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void debug( String pattern,
                       Object arg1,
                       Object arg2 ) {
        if (isDebugEnabled()) info(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String, java.lang.Object)
     */
    @Override
    public void debug( Marker marker,
                       String pattern,
                       Object arg ) {
        debug(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void debug( Marker marker,
                       String pattern,
                       Object[] arguments ) {
        debug(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void debug( Marker marker,
                       String message,
                       Throwable e ) {
        debug(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#debug(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void debug( Marker marker,
                       String pattern,
                       Object arg1,
                       Object arg2 ) {
        debug(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(java.lang.String)
     */
    @Override
    public void error( String message ) {
        if (isErrorEnabled()) LOGGER.log(new Status(IStatus.ERROR, IUiConstants.PLUGIN_ID, message, null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object)
     */
    @Override
    public void error( String pattern,
                       Object arg ) {
        if (isErrorEnabled()) LOGGER.log(new Status(IStatus.ERROR, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern, arg),
                                                    null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[])
     */
    @Override
    public void error( String pattern,
                       Object[] arguments ) {
        if (isErrorEnabled()) LOGGER.log(new Status(IStatus.ERROR, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern,
                                                                                                                arguments), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error( String message,
                       Throwable e ) {
        if (isErrorEnabled()) LOGGER.log(new Status(IStatus.ERROR, IUiConstants.PLUGIN_ID, message, e));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String)
     */
    @Override
    public void error( Marker marker,
                       String message ) {
        error(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void error( String pattern,
                       Object arg1,
                       Object arg2 ) {
        if (isErrorEnabled()) LOGGER.log(new Status(IStatus.ERROR, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern,
                                                                                                                arg1,
                                                                                                                arg2), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String, java.lang.Object)
     */
    @Override
    public void error( Marker marker,
                       String pattern,
                       Object arg ) {
        error(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void error( Marker marker,
                       String pattern,
                       Object[] arguments ) {
        error(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error( Marker marker,
                       String message,
                       Throwable e ) {
        error(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#error(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void error( Marker marker,
                       String pattern,
                       Object arg1,
                       Object arg2 ) {
        error(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(java.lang.String)
     */
    @Override
    public void info( String message ) {
        if (isInfoEnabled()) LOGGER.log(new Status(IStatus.INFO, IUiConstants.PLUGIN_ID, message, null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object)
     */
    @Override
    public void info( String pattern,
                      Object arg ) {
        if (isInfoEnabled()) LOGGER.log(new Status(IStatus.INFO, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern, arg), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[])
     */
    @Override
    public void info( String pattern,
                      Object[] arguments ) {
        if (isInfoEnabled()) LOGGER.log(new Status(IStatus.INFO, IUiConstants.PLUGIN_ID,
                                                   MessageFormat.format(pattern, arguments), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void info( String message,
                      Throwable e ) {
        if (isInfoEnabled()) LOGGER.log(new Status(IStatus.INFO, IUiConstants.PLUGIN_ID, message, e));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String)
     */
    @Override
    public void info( Marker marker,
                      String message ) {
        info(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void info( String pattern,
                      Object arg1,
                      Object arg2 ) {
        if (isInfoEnabled()) LOGGER.log(new Status(IStatus.INFO, IUiConstants.PLUGIN_ID,
                                                   MessageFormat.format(pattern, arg1, arg2), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String, java.lang.Object)
     */
    @Override
    public void info( Marker marker,
                      String pattern,
                      Object arg ) {
        info(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void info( Marker marker,
                      String pattern,
                      Object[] arguments ) {
        info(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void info( Marker marker,
                      String message,
                      Throwable e ) {
        info(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#info(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void info( Marker marker,
                      String pattern,
                      Object arg1,
                      Object arg2 ) {
        info(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isDebugEnabled()
     */
    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isDebugEnabled(org.slf4j.Marker)
     */
    @Override
    public boolean isDebugEnabled( Marker marker ) {
        return isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isErrorEnabled()
     */
    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isErrorEnabled(org.slf4j.Marker)
     */
    @Override
    public boolean isErrorEnabled( Marker marker ) {
        return isErrorEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isInfoEnabled()
     */
    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isInfoEnabled(org.slf4j.Marker)
     */
    @Override
    public boolean isInfoEnabled( Marker marker ) {
        return isInfoEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isTraceEnabled()
     */
    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isTraceEnabled(org.slf4j.Marker)
     */
    @Override
    public boolean isTraceEnabled( Marker marker ) {
        return isTraceEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isWarnEnabled()
     */
    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#isWarnEnabled(org.slf4j.Marker)
     */
    @Override
    public boolean isWarnEnabled( Marker marker ) {
        return isWarnEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(java.lang.String)
     */
    @Override
    public void trace( String message ) {
        if (isTraceEnabled()) info(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object)
     */
    @Override
    public void trace( String pattern,
                       Object arg ) {
        if (isTraceEnabled()) info(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object[])
     */
    @Override
    public void trace( String pattern,
                       Object[] arguments ) {
        if (isTraceEnabled()) info(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void trace( String message,
                       Throwable e ) {
        if (isTraceEnabled()) info(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String)
     */
    @Override
    public void trace( Marker marker,
                       String message ) {
        trace(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void trace( String pattern,
                       Object arg1,
                       Object arg2 ) {
        if (isTraceEnabled()) info(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String, java.lang.Object)
     */
    @Override
    public void trace( Marker marker,
                       String pattern,
                       Object arg ) {
        trace(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void trace( Marker marker,
                       String pattern,
                       Object[] arguments ) {
        trace(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void trace( Marker marker,
                       String message,
                       Throwable e ) {
        trace(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#trace(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void trace( Marker marker,
                       String pattern,
                       Object arg1,
                       Object arg2 ) {
        trace(pattern, arg1, arg2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(java.lang.String)
     */
    @Override
    public void warn( String message ) {
        if (isWarnEnabled()) LOGGER.log(new Status(IStatus.WARNING, IUiConstants.PLUGIN_ID, message, null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object)
     */
    @Override
    public void warn( String pattern,
                      Object arg ) {
        if (isWarnEnabled()) LOGGER.log(new Status(IStatus.WARNING, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern, arg),
                                                   null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object[])
     */
    @Override
    public void warn( String pattern,
                      Object[] arguments ) {
        if (isWarnEnabled()) LOGGER.log(new Status(IStatus.WARNING, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern,
                                                                                                                 arguments), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void warn( String message,
                      Throwable e ) {
        if (isWarnEnabled()) LOGGER.log(new Status(IStatus.WARNING, IUiConstants.PLUGIN_ID, message, e));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String)
     */
    @Override
    public void warn( Marker marker,
                      String message ) {
        warn(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void warn( String pattern,
                      Object arg1,
                      Object arg2 ) {
        if (isWarnEnabled()) LOGGER.log(new Status(IStatus.WARNING, IUiConstants.PLUGIN_ID, MessageFormat.format(pattern,
                                                                                                                 arg1,
                                                                                                                 arg2), null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String, java.lang.Object)
     */
    @Override
    public void warn( Marker marker,
                      String pattern,
                      Object arg ) {
        warn(pattern, arg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void warn( Marker marker,
                      String pattern,
                      Object[] arguments ) {
        warn(pattern, arguments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void warn( Marker marker,
                      String message,
                      Throwable e ) {
        warn(message, e);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.slf4j.Logger#warn(org.slf4j.Marker, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void warn( Marker marker,
                      String pattern,
                      Object arg1,
                      Object arg2 ) {
        warn(pattern, arg1, arg2);
    }

}
