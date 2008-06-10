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
 * A basic progress monitor that facilitates the updating and monitoring of progress towards the completion of an activity.
 * Documentation hereafter will refer to the code in an application that updates progress as the <strong>Updater</strong>, and
 * code that monitors progress as an <strong>Observer</strong>.
 * <p>
 * The progress of each {@link #getActivityName() activity} is started when the <strong>Updater</strong> calls
 * {@link #beginTask(double, I18n, Object...)}, continues with a mixture of work ({@link #worked(double)}) and subtasks ({@link #createSubtask(double)}),
 * and finishes when the activity is completed ({@link #done()}) or cancelled ({@link #setCancelled(boolean)}).
 * </p>
 * <p>
 * If an activity is interrupted before its normal completion due to a cancellation request by an <strong>Observer</strong>, it
 * is still the responsibility of the <strong>Updater</strong> to mark the activity as completed. Similarly, if an activity
 * cannot be cancelled before its normal completion, the <strong>Updater</strong> must deny any previous cancellation request by
 * calling {@link #setCancelled(boolean) setCancelled(false)}.
 * </p>
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public interface ProgressMonitor {

    /**
     * Get the name of the activity. This should never change for a progress monitor, and all
     * {@link #createSubtask(double) subtasks} should have the same name.
     * 
     * @return the activity's name
     */
    String getActivityName();

    /**
     * Called by the <strong>Updater</strong> to indicate work has started on the task, specifying the total amount of work that
     * this task constitutes.
     * 
     * @param totalWork the total number of work units for the task
     * @param name the name of the task
     * @param params the parameters for localization
     */
    void beginTask( double totalWork,
                    I18n name,
                    Object... params );

    /**
     * Called by the <strong>Updater</strong> to report work completed for this task.
     * 
     * @param work the number of work units that have been worked
     */
    void worked( double work );

    /**
     * Called by the <strong>Updater</strong> to create a subtask with the given about of work. The resulting progress monitor
     * must be started ({@link #beginTask(double, I18n, Object...)}) and finished ({@link #done()}).
     * 
     * @param subtaskWork the number of work units for this subtask
     * @return the progress monitor for the subtask
     */
    ProgressMonitor createSubtask( double subtaskWork );

    /**
     * Called by the <strong>Updater</strong> to mark this activity as complete. This method must be called, even if the activity
     * has been cancelled.
     */
    void done();

    /**
     * Return whether this activity has completed.
     * 
     * @return <code>true</code> if this activity has completed.
     */
    boolean isDone();

    /**
     * Called by an <strong>Observer</strong> to request the cancellation of this activity, or by the <strong>Updater</strong>
     * to deny a prior cancellation request (i.e., when the activity {@link #done() completes} before the <strong>Updater</strong>
     * recognizes a cancellation request by an <strong>Observer</strong>).
     * 
     * @param value <code>true</code> if requesting the activity be cancelled.
     */
    void setCancelled( boolean value );

    /**
     * Return whether a request was made by an <strong>Observer</strong> to {@link #setCancelled(boolean) cancel} this activity.
     * 
     * @return <code>true</code> if this activity has been requested to be cancelled.
     */
    boolean isCancelled();

    /**
     * Return the current status of this activity, localized to the specified locale. This method returns an immutable but
     * consistent snapshot of the status for this activity. Note that if this instance is a {@link #createSubtask(double) subtask},
     * this method returns the status of the subtask.
     * 
     * @param locale the locale in which the status is to be represented; if null, the {@link Locale#getDefault() default locale}
     *        will be used
     * @return the status of this activity
     */
    ProgressStatus getStatus( Locale locale );

    /**
     * Return the problems encountered during the {@link #getStatus(Locale) progress} made towards completing the associated
     * {@link #getActivityName() activity}.
     * 
     * @return the list of problems
     */
    Problems getProblems();
}
