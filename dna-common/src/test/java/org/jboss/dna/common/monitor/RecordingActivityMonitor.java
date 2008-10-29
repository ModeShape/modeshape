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

import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.i18n.MockI18n;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class RecordingActivityMonitor extends ActivityMonitorWrapper {

    private int beginTaskCount;
    private int doneCount;
    private int createSubtaskCount;
    private int setCancelledCount;

    public RecordingActivityMonitor( ActivityMonitor delegate ) {
        super(delegate);
    }

    public RecordingActivityMonitor( String name ) {
        this(new SimpleActivityMonitor(MockI18n.passthrough, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginTask( double totalWork,
                           I18n name,
                           Object... params ) {
        ++beginTaskCount;
        super.beginTask(totalWork, name, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void done() {
        ++doneCount;
        super.done();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityMonitor createSubtask( double subtaskWork ) {
        ++createSubtaskCount;
        return super.createSubtask(subtaskWork);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled( boolean value ) {
        ++setCancelledCount;
        super.setCancelled(value);
    }

    /**
     * @return beginTaskCount
     */
    public int getBeginTaskCount() {
        return this.beginTaskCount;
    }

    /**
     * @return doneCount
     */
    public int getDoneCount() {
        return this.doneCount;
    }

    /**
     * @return createSubtaskCount
     */
    public int getCreateSubtaskCount() {
        return this.createSubtaskCount;
    }

    /**
     * @return setCancelledCount
     */
    public int getSetCancelledCount() {
        return this.setCancelledCount;
    }
}
