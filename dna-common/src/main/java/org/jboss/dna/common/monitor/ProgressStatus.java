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

import java.io.Serializable;
import java.text.DecimalFormat;
import org.jboss.dna.common.util.StringUtil;
import net.jcip.annotations.Immutable;

/**
 * A snapshot of the progress on an activity.
 * @author Randall Hauch
 */
@Immutable
public class ProgressStatus implements Serializable, Comparable<ProgressStatus> {

    protected static final String PERCENTAGE_PATTERN = "##0.0"; // percentage should always fit
    protected static final double PERCENT_PRECISION = 0.001d;

    /**
     * Compute the percentage worked, accounting for imprecision in
     * @param workedSoFar the amount of work so far percentage worked
     * @param totalWork the total amount of work for this activity
     * @return
     */
    protected static double computePercentage( double workedSoFar, double totalWork ) {
        if (isSamePercentage(workedSoFar, 0.0d)) return 0.0d;
        assert totalWork > 0.0d;
        double percentage = workedSoFar / totalWork * 100.0d;
        if (isSamePercentage(percentage, 100.0d)) percentage = 100.0d;
        return percentage;
    }

    protected static boolean isSamePercentage( double percentage1, double percentage2 ) {
        return Math.abs(percentage1 - percentage2) <= PERCENT_PRECISION;
    }

    private final String activityName;
    private final double percentWorked;
    private final boolean done;
    private final boolean cancelled;
    private final String message;

    /**
     * Create the progress status.
     * @param activityName the name of the activity, which may not be null
     * @param message the message for the progress, which may not be null
     * @param percentWorked the percentage worked, ranging from 0.0 for not started to 100.0 for complete; a negative value are
     * treated as 0.0, while a value greater than 100.0 is treated as 100.0
     * @param cancelled true if the activity has been requested to be cancelled, or false otherwise
     */
    public ProgressStatus( String activityName, String message, double percentWorked, boolean cancelled ) {
        assert activityName != null;
        assert message != null;
        this.activityName = activityName;
        this.done = percentWorked >= 100.0d;
        this.percentWorked = this.done ? 100.0d : (percentWorked <= 0.0d ? 0.0d : percentWorked);
        this.message = message;
        this.cancelled = cancelled;
    }

    /**
     * Create the progress status and compute the percentage worked.
     * @param activityName the name of the activity, which may not be null
     * @param message the message for the progress, which may not be null
     * @param workedSoFar the amount of work so far percentage worked
     * @param totalWork the total amount of work for this activity
     * @param cancelled true if the activity has been requested to be cancelled, or false otherwise
     */
    public ProgressStatus( String activityName, String message, double workedSoFar, double totalWork, boolean cancelled ) {
        this(activityName, message, computePercentage(workedSoFar, totalWork), cancelled);
    }

    /**
     * Get the name of the activity.
     * @return the activity's name
     */
    public String getActivityName() {
        return this.activityName;
    }

    /**
     * Get the progress as a percentage of the total work that's been completed.
     * @return the percentage worked, ranging from 0.0 to 100.0
     */
    public double getPercentWorked() {
        return this.percentWorked;
    }

    /**
     * Get the progress monitor's text message.
     * @return the text message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Return whether work on this activity has completed.
     * @return true if work has completed, or false if work on the activity is still progressing
     * @see #isCancelled()
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Return whether the activity was requested to be cancelled.
     * @return cancelled
     * @see #isDone()
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String percentage = new DecimalFormat(PERCENTAGE_PATTERN).format(getPercentWorked());
        percentage = StringUtil.justifyRight(percentage, PERCENTAGE_PATTERN.length(), ' ');
        String cancelled = this.isCancelled() ? " (cancelled)" : "";
        return this.activityName + " (" + this.message + ") " + percentage + " %" + cancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getActivityName().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof ProgressStatus) {
            final ProgressStatus that = (ProgressStatus)obj;
            // First check that the name is the same ...
            if (!this.getActivityName().equals(that.getActivityName())) return false;
            // Then check doneness and percent complete ...
            if (this.isDone() != that.isDone()) return false;
            if (!isSamePercentage(this.getPercentWorked(), that.getPercentWorked())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( ProgressStatus that ) {
        if (this == that) return 0;

        // First check the name ...
        int diff = this.getActivityName().compareTo(that.getActivityName());
        if (diff != 0) return diff;

        // Then check the percentage ...
        return Double.compare(this.getPercentWorked(), that.getPercentWorked());
    }
}
