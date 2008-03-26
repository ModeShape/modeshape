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
import org.jboss.dna.common.i18n.I18n;
import net.jcip.annotations.ThreadSafe;

/**
 * Progress monitor that records nothing.
 * @author Randall Hauch
 */
@ThreadSafe
public class NullProgressMonitor implements ProgressMonitor {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final String activityName;

    public NullProgressMonitor( String activityName ) {
        this.activityName = activityName;
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
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public ProgressMonitor createSubtask( double subtaskWork ) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void done() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setCancelled( boolean value ) {
        cancelled.set(value);
    }

    /**
     * {@inheritDoc}
     */
    public void worked( double work ) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public ProgressStatus getStatus() {
        return new ProgressStatus(this.activityName, "Not available", 0.0d, cancelled.get());
    }
}
