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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.Logger.Level;
import org.slf4j.Marker;

/**
 * A basic activity monitor.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@ThreadSafe
public final class SimpleActivityMonitor implements ActivityMonitor {

    @GuardedBy( "lock" )
    private I18n taskName;
    @GuardedBy( "lock" )
    private Object[] taskNameParameters;
    @GuardedBy( "lock" )
    private double totalWork;
    @GuardedBy( "lock" )
    private double worked;

    private final I18n activityName;
    private final Object[] activityNameParameters;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final List<UnlocalizedActivityInfo> capturedInformation = new CopyOnWriteArrayList<UnlocalizedActivityInfo>();

    public SimpleActivityMonitor( I18n activityName,
                                  Object... params ) {
        CheckArg.isNotNull(activityName, "activityName");
        this.activityName = activityName;
        this.activityNameParameters = params;
        this.taskName = null;
        this.taskNameParameters = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void beginTask( double totalWork,
                           I18n taskName,
                           Object... taskNameParameters ) {
        CheckArg.isGreaterThan(totalWork, 0.0, "totalWork");
        if (taskName == null && taskNameParameters != null) {
            CheckArg.isEmpty(taskNameParameters, "taskNameParameters");
        }
        try {
            this.lock.writeLock().lock();
            this.taskName = taskName;
            this.taskNameParameters = taskNameParameters;
            this.totalWork = totalWork;
            this.worked = 0.0d;
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
                         Object... messageParameters ) {
        capture(null, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#capture(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void capture( Marker marker,
                         I18n message,
                         Object... messageParameters ) {
        capture(Level.INFO, marker, null, message, messageParameters);
    }

    private void capture( Logger.Level type,
                          Marker marker,
                          Throwable throwable,
                          I18n message,
                          Object... messageParameters ) {
        assert type != null;
        if (message == null && messageParameters != null) {
            CheckArg.isEmpty(messageParameters, "messageParameters");
        }
        capturedInformation.add(new UnlocalizedActivityInfo(type, taskName, taskNameParameters, marker, null, message,
                                                            messageParameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(java.lang.Throwable)
     */
    public void captureError( Throwable throwable ) {
        captureError(throwable, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureError( I18n message,
                              Object... messageParameters ) {
        captureError(null, null, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureError( Marker marker,
                              I18n message,
                              Object... messageParameters ) {
        captureError(marker, null, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureError(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureError( Throwable throwable,
                              I18n message,
                              Object... messageParameters ) {
        captureError(null, throwable, message, messageParameters);
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
                              Object... messageParameters ) {
        capture(Level.ERROR, marker, throwable, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(java.lang.Throwable)
     */
    public void captureWarning( Throwable throwable ) {
        captureWarning(throwable, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void captureWarning( I18n message,
                                Object... messageParameters ) {
        captureWarning(null, null, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(org.slf4j.Marker, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureWarning( Marker marker,
                                I18n message,
                                Object... messageParameters ) {
        captureWarning(marker, null, message, messageParameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#captureWarning(java.lang.Throwable, org.jboss.dna.common.i18n.I18n,
     *      java.lang.Object[])
     */
    public void captureWarning( Throwable throwable,
                                I18n message,
                                Object... messageParameters ) {
        captureWarning(null, throwable, message, messageParameters);
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
                                Object... messageParameters ) {
        capture(Level.WARNING, marker, throwable, message, messageParameters);
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
        boolean alreadyDone = false;
        try {
            this.lock.writeLock().lock();
            if (this.worked < this.totalWork) {
                this.worked = this.totalWork;
            } else {
                alreadyDone = true;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        if (!alreadyDone) notifyProgress();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName()
     */
    public String getActivityName() {
        return getActivityName(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#getActivityName(java.util.Locale)
     */
    public String getActivityName( Locale locale ) {
        return activityName.text(locale, activityNameParameters);
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
            return new ActivityStatus(activityName, activityNameParameters, taskName, taskNameParameters, worked, totalWork,
                                      isCancelled(), capturedInformation, locale);
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
        return this.cancelled.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#isDone()
     */
    public boolean isDone() {
        lock.readLock().lock();
        try {
            return worked >= totalWork;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Method that is called in {@link #worked(double)} (which is called by {@link #createSubtask(double) subtasks}) when there
     * has been some positive work, or when the monitor is first marked as {@link #done()}.
     * <p>
     * This method implementation does nothing, but subclasses can easily override this method if they want to be updated with the
     * latest progress.
     * </p>
     */
    protected void notifyProgress() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#setCancelled(boolean)
     */
    public void setCancelled( boolean value ) {
        this.cancelled.set(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ActivityMonitor#worked(double)
     */
    public void worked( double work ) {
        if (work > 0) {
            try {
                this.lock.writeLock().lock();
                if (this.worked < this.totalWork) {
                    this.worked += work;
                    if (this.worked > this.totalWork) this.worked = this.totalWork;
                }
            } finally {
                this.lock.writeLock().unlock();
            }
            notifyProgress();
        }
    }
}
