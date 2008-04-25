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

/**
 * A basic progress monitor that facilitates the monitoring of an activity.
 * <p>
 * The progress of each activity is started when {@link #beginTask(double, I18n, Object...)} is called, continues with a mixture
 * of work ({@link #worked(double)}) and subtasks ({@link #createSubtask(double)}), and finishes when the activity is
 * completed ({@link #done()}) or cancelled ({@link #setCancelled(boolean)}).
 * </p>
 * @author Randall Hauch
 */
public interface ProgressMonitor {

    /**
     * Get the name of the activity. This should never change for a progress monitor, and all
     * {@link #createSubtask(double) subtasks} should have the same name.
     * @return the activity's name
     */
    String getActivityName();

    /**
     * Start work on the task, specifying the total amount of work that this task constitutes.
     * @param totalWork the total number of work units for the task
     * @param name the name of the task
     * @param params the parameters for localization
     */
    void beginTask( double totalWork, I18n name, Object... params );

    /**
     * Report work completed for this task.
     * @param work the number of work units that have been worked
     */
    void worked( double work );

    /**
     * Create a subtask with the given about of work. The resulting monitor must be started ({@link #beginTask(double, I18n, Object...)})
     * and finished ({@link #done()}).
     * @param subtaskWork the number of work units for this subtask
     * @return the progress monitor for the subtask
     */
    ProgressMonitor createSubtask( double subtaskWork );

    /**
     * Mark this task as being completed. This method must be called for the task to be properly completed.
     */
    void done();

    /**
     * Set the cancelled state of this activity. Cancelling the activity must be considered a request that can be denied by
     * setting the cancelled state to <code>false</code>.
     * @param value true if requesting the activity be cancelled.
     */
    void setCancelled( boolean value );

    /**
     * Returned whether this activity has been {@link #setCancelled(boolean) cancelled}.
     * @return true if this activity has been requested to be cancelled, or false otherwise.
     */
    boolean isCancelled();

    /**
     * Return the current status of this activity, localized to the specified locale. This method returns an immutable but
     * consistent snapshot of the status for this activity. Note that if this instance is a {@link #createSubtask(double) subtask},
     * this method returns the status of the subtask.
     * @param locale the locale in which the status is to be represented; if null, the {@link Locale#getDefault() default locale}
     * will be used
     * @return the status of this activity
     */
    ProgressStatus getStatus( Locale locale );
}
