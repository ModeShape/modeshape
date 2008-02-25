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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class SubProgressMonitor implements IProgressMonitor {

    @GuardedBy( "lock" )
    private String taskName;
    @GuardedBy( "lock" )
    private double totalWork;
    @GuardedBy( "lock" )
    private double parentWorkScaleFactor;
    @GuardedBy( "lock" )
    private double submittedToParent;

    private final double subtaskTotalInParent;
    private final IProgressMonitor parent;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SubProgressMonitor( final IProgressMonitor parent, final double subtaskTotalInParent ) {
        assert subtaskTotalInParent > 0;
        assert parent != null;
        this.parent = parent;
        this.subtaskTotalInParent = subtaskTotalInParent;
    }

    /**
     * {@inheritDoc}
     */
    public String getActivityName() {
        return this.parent.getActivityName();
    }

    /**
     * @return parent
     */
    public IProgressMonitor getParent() {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     */
    public void beginTask( String name, double totalWork ) {
        assert totalWork > 0;
        try {
            this.lock.writeLock().lock();
            this.taskName = name;
            this.totalWork = totalWork;
            this.parentWorkScaleFactor = ((float)subtaskTotalInParent) / ((float)totalWork);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public IProgressMonitor createSubtask( double subtaskWork ) {
        return new SubProgressMonitor(this, subtaskWork);
    }

    /**
     * {@inheritDoc}
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
     */
    public boolean isCancelled() {
        return this.parent.isCancelled();
    }

    /**
     * {@inheritDoc}
     */
    public void setCancelled( boolean value ) {
        this.parent.setCancelled(value);
    }

    /**
     * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
    public ProgressStatus getStatus() {
        try {
            this.lock.readLock().lock();
            return new ProgressStatus(this.getActivityName(), this.taskName, this.submittedToParent, this.subtaskTotalInParent, this.isCancelled());
        } finally {
            this.lock.readLock().unlock();
        }
    }
}
