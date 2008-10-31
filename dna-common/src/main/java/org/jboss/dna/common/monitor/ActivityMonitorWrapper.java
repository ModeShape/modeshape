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

package org.jboss.dna.common.monitor;

import java.util.Locale;
import org.jboss.dna.common.i18n.I18n;
import org.slf4j.Marker;

/**
 * The thread safety of this class is determined by the delegate.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ActivityMonitorWrapper implements ActivityMonitor {

    private final ActivityMonitor delegate;

    public ActivityMonitorWrapper( ActivityMonitor delegate ) {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void beginTask( double totalWork,
                           I18n name,
                           Object... params ) {
        this.delegate.beginTask(totalWork, name, params);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#capture(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void capture( I18n message,
                         Object... parameters ) {
        delegate.capture(message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#capture(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void capture( Marker marker,
                         I18n message,
                         Object... parameters ) {
        delegate.capture(marker, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(java.lang.Throwable)
     */
    public void captureError( Throwable throwable ) {
        delegate.captureError(throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureError( I18n message,
                              Object... parameters ) {
        delegate.captureError(message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureError( Marker marker,
                              I18n message,
                              Object... parameters ) {
        delegate.captureError(marker, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureError( Throwable throwable,
                              I18n message,
                              Object... parameters ) {
        delegate.captureError(throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.slf4j.Marker, java.lang.Throwable,
     *      org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureError( Marker marker,
                              Throwable throwable,
                              I18n message,
                              Object... parameters ) {
        delegate.captureError(marker, throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(java.lang.Throwable)
     */
    public void captureWarning( Throwable throwable ) {
        delegate.captureWarning(throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureWarning( I18n message,
                                Object... parameters ) {
        delegate.captureWarning(message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureWarning( Marker marker,
                                I18n message,
                                Object... parameters ) {
        delegate.captureWarning(marker, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureWarning( Throwable throwable,
                                I18n message,
                                Object... parameters ) {
        delegate.captureWarning(throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.slf4j.Marker, java.lang.Throwable,
     *      org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureWarning( Marker marker,
                                Throwable throwable,
                                I18n message,
                                Object... parameters ) {
        delegate.captureWarning(marker, throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#createSubtask(double)
     */
    public ActivityMonitor createSubtask( double subtaskWork ) {
        return this.delegate.createSubtask(subtaskWork);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#done()
     */
    public void done() {
        this.delegate.done();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName()
     */
    public String getActivityName() {
        return delegate.getActivityName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName(java.util.Locale)
     */
    public String getActivityName( Locale locale ) {
        return delegate.getActivityName(locale);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getStatus()
     */
    public ActivityStatus getStatus() {
        return delegate.getStatus();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getStatus(java.util.Locale)
     */
    public ActivityStatus getStatus( Locale locale ) {
        return this.delegate.getStatus(locale);
    }

    public ActivityMonitor getWrappedMonitor() {
        return this.delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#isCancelled()
     */
    public boolean isCancelled() {
        return this.delegate.isCancelled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#isDone()
     */
    public boolean isDone() {
        return delegate.isDone();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#setCancelled(boolean)
     */
    public void setCancelled( boolean value ) {
        this.delegate.setCancelled(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#worked(double)
     */
    public void worked( double work ) {
        this.delegate.worked(work);
    }
}
