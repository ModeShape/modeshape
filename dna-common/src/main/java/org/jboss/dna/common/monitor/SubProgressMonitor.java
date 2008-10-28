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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.i18n.I18n;

/**
 * This class is thread-safe except when accessing or adding {@link #getProblems() problems}. Problems must only be added by the
 * {@link ProgressMonitor <strong>Updater</strong>}, and accessed by {@link ProgressMonitor Observers} only after the activity has
 * been {@link #done() completed}.
 * 
 * @author Randall Hauch
 */
public class SubProgressMonitor implements ProgressMonitor {

    @GuardedBy( "lock" )
    private I18n taskName;
    @GuardedBy( "lock" )
    private Object[] params;
    @GuardedBy( "lock" )
    private double totalWork;
    @GuardedBy( "lock" )
    private double parentWorkScaleFactor;
    @GuardedBy( "lock" )
    private double submittedToParent;

    private final double subtaskTotalInParent;
    private final ProgressMonitor parent;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SubProgressMonitor( final ProgressMonitor parent,
                               final double subtaskTotalInParent ) {
        assert subtaskTotalInParent > 0;
        assert parent != null;
        this.parent = parent;
        this.subtaskTotalInParent = subtaskTotalInParent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, java.lang.Object[])
     */
    public void beginTask( double totalWork,
                           I18n name,
                           Object... params ) {
        assert totalWork > 0;
        try {
            this.lock.writeLock().lock();
            this.taskName = name;
            this.params = params;
            this.totalWork = totalWork;
            this.parentWorkScaleFactor = ((float)subtaskTotalInParent) / ((float)totalWork);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#createSubtask(double)
     */
    public ProgressMonitor createSubtask( double subtaskWork ) {
        return new SubProgressMonitor(this, subtaskWork);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#done()
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
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getActivityName()
     */
    public String getActivityName() {
        return this.parent.getActivityName();
    }

    /**
     * @return parent
     */
    public ProgressMonitor getParent() {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getParentActivityName()
     */
    public String getParentActivityName() {
        return parent.getParentActivityName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getProblems()
     */
    public Problems getProblems() {
        return parent.getProblems();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getStatus(java.util.Locale)
     */
    public ProgressStatus getStatus( Locale locale ) {
        try {
            this.lock.readLock().lock();
            return new ProgressStatus(this.getActivityName(), this.taskName.text(locale, this.params), this.submittedToParent,
                                      this.subtaskTotalInParent, this.isCancelled());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#isCancelled()
     */
    public boolean isCancelled() {
        return this.parent.isCancelled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#isDone()
     */
    public boolean isDone() {
        return parent.isDone();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#setCancelled(boolean)
     */
    public void setCancelled( boolean value ) {
        this.parent.setCancelled(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#worked(double)
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
