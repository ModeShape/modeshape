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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * A basic progress monitor
 * @author Randall Hauch
 */
@ThreadSafe
public class SimpleProgressMonitor implements ProgressMonitor {

    @GuardedBy( "lock" )
    private I18n taskName;
    @GuardedBy( "lock" )
    private Object[] params;
    @GuardedBy( "lock" )
    private double totalWork;
    @GuardedBy( "lock" )
    private double worked;
    @GuardedBy( "lock" )
    private boolean done;

    private final String activityName;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public SimpleProgressMonitor( String activityName ) {
        this.activityName = activityName != null ? activityName.trim() : "";
        this.taskName = CommonI18n.initialProgressMonitorTaskName;
    }

    /**
     * {@inheritDoc}
     */
    public String getActivityName() {
        return this.activityName;
    }

    /**
     * {@inheritDoc}
     */
    public void beginTask( double totalWork, I18n name, Object... params ) {
        assert totalWork > 0;
        try {
            this.lock.writeLock().lock();
            this.taskName = name;
            this.params = params;
            this.totalWork = totalWork;
            this.worked = 0.0d;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ProgressMonitor createSubtask( double subtaskWork ) {
        return new SubProgressMonitor(this, subtaskWork);
    }

    /**
     * {@inheritDoc}
     */
    public void done() {
        boolean alreadyDone = false;
        try {
            this.lock.writeLock().lock();
            if (!this.done) {
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
     */
    public boolean isCancelled() {
        return this.cancelled.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setCancelled( boolean value ) {
        this.cancelled.set(value);
    }

    /**
     * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
    public ProgressStatus getStatus() {
        try {
            this.lock.readLock().lock();
            return new ProgressStatus(this.getActivityName(), this.taskName.text(this.params), this.worked, this.totalWork, this.isCancelled());
        } finally {
            this.lock.readLock().unlock();
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
}
