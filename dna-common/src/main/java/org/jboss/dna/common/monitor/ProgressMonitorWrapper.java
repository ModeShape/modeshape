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
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.i18n.I18n;

/**
 * The thread safety of this class is determined by the delegate.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ProgressMonitorWrapper implements ProgressMonitor {

    private final ProgressMonitor delegate;

    public ProgressMonitorWrapper( ProgressMonitor delegate ) {
        this.delegate = delegate;
    }

    public void beginTask( double totalWork,
                           I18n name,
                           Object... params ) {
        this.delegate.beginTask(totalWork, name, params);
    }

    public ProgressMonitor createSubtask( double subtaskWork ) {
        return this.delegate.createSubtask(subtaskWork);
    }

    public void done() {
        this.delegate.done();
    }

    public String getActivityName() {
        return this.delegate.getActivityName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getParentActivityName()
     */
    public String getParentActivityName() {
        return this.delegate.getParentActivityName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#getProblems()
     */
    public Problems getProblems() {
        return delegate.getProblems();
    }

    public ProgressStatus getStatus( Locale locale ) {
        return this.delegate.getStatus(locale);
    }

    public ProgressMonitor getWrappedMonitor() {
        return this.delegate;
    }

    public boolean isCancelled() {
        return this.delegate.isCancelled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.monitor.ProgressMonitor#isDone()
     */
    public boolean isDone() {
        return delegate.isDone();
    }

    public void setCancelled( boolean value ) {
        this.delegate.setCancelled(value);
    }

    public void worked( double work ) {
        this.delegate.worked(work);
    }
}
