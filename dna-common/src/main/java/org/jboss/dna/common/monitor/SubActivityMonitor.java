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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.slf4j.Marker;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
@ThreadSafe
class SubActivityMonitor implements ActivityMonitor {

    @GuardedBy( "lock" )
    private I18n taskName;
    @GuardedBy( "lock" )
    private Object[] taskNameParameters;
    @GuardedBy( "lock" )
    private double totalWork;
    @GuardedBy( "lock" )
    private double parentWorkScaleFactor;
    @GuardedBy( "lock" )
    private double submittedToParent;

    private final double subtaskTotalInParent;
    private final ActivityMonitor parent;
    private final I18n activityName;
    private final Object[] activityNameParameters;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<UnlocalizedActivityInfo> capturedInformation;

    SubActivityMonitor( final ActivityMonitor parent,
                        final I18n activityName,
                        final Object[] activityNameParameters,
                        final double subtaskTotalInParent,
                        List<UnlocalizedActivityInfo> capturedInformation ) {
        assert subtaskTotalInParent > 0;
        assert parent != null;
        assert activityName != null;
        this.activityName = activityName;
        this.activityNameParameters = activityNameParameters;
        assert capturedInformation != null;
        this.parent = parent;
        this.subtaskTotalInParent = subtaskTotalInParent;
        this.capturedInformation = capturedInformation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void beginTask( double totalWork,
                           I18n name,
                           Object... params ) {
        assert totalWork > 0;
        try {
            this.lock.writeLock().lock();
            this.taskName = name;
            this.taskNameParameters = params;
            this.totalWork = totalWork;
            this.parentWorkScaleFactor = ((float)subtaskTotalInParent) / ((float)totalWork);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#capture(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void capture( I18n message,
                         Object... parameters ) {
        parent.capture(message, parameters);
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
        parent.capture(marker, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(java.lang.Throwable)
     */
    public void captureError( Throwable throwable ) {
        parent.captureError(throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureError( I18n message,
                              Object... parameters ) {
        parent.captureError(message, parameters);
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
        parent.captureError(marker, message, parameters);
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
        parent.captureError(throwable, message, parameters);
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
        parent.captureError(marker, throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(java.lang.Throwable)
     */
    public void captureWarning( Throwable throwable ) {
        parent.captureWarning(throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureWarning( I18n message,
                                Object... parameters ) {
        parent.captureWarning(message, parameters);
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
        parent.captureWarning(marker, message, parameters);
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
        parent.captureWarning(throwable, message, parameters);
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
        parent.captureWarning(marker, throwable, message, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#createSubtask(double)
     */
    public ActivityMonitor createSubtask( double subtaskWork ) {
        return new SubActivityMonitor(this, activityName, activityNameParameters, subtaskWork, capturedInformation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#done()
     */
    public void done() {
        // Compute the total work for this task in terms of the parent ...
        double workInParentRemaining = 0.0d;
        try {
            this.lock.writeLock().lock();
            double totalWorkInParent = this.totalWork * this.parentWorkScaleFactor;
            workInParentRemaining = totalWorkInParent - this.submittedToParent;
        } finally {
            this.lock.writeLock().unlock();
        }
        // Don't do this in the lock: it's incremental, but doing so might cause deadlock with future changes
        this.parent.worked(workInParentRemaining);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName()
     */
    public String getActivityName() {
        return parent.getActivityName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName(java.util.Locale)
     */
    public String getActivityName( Locale locale ) {
        return parent.getActivityName(locale);
    }

    /**
     * @return parent
     */
    public ActivityMonitor getParent() {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getStatus()
     */
    public ActivityStatus getStatus() {
        return getStatus(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getStatus(java.util.Locale)
     */
    public ActivityStatus getStatus( Locale locale ) {
        try {
            lock.readLock().lock();
            return new ActivityStatus(activityName, activityNameParameters, taskName, taskNameParameters, submittedToParent,
                                      subtaskTotalInParent, isCancelled(), capturedInformation, locale);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#isCancelled()
     */
    public boolean isCancelled() {
        return this.parent.isCancelled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#isDone()
     */
    public boolean isDone() {
        return parent.isDone();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#setCancelled(boolean)
     */
    public void setCancelled( boolean value ) {
        this.parent.setCancelled(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#worked(double)
     */
    public void worked( double work ) {
        if (this.isCancelled()) return;
        if (work > 0) {
            double workInParent = 0.0d;
            try {
                this.lock.writeLock().lock();
                workInParent = work * parentWorkScaleFactor;
                this.submittedToParent += workInParent;
            } finally {
                this.lock.writeLock().unlock();
            }
            // Don't do this in the lock: it's incremental, but doing so might cause deadlock with future changes
            this.parent.worked(workInParent);
        }
    }
}
