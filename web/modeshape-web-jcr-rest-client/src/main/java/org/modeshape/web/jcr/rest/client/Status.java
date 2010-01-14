/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.jcr.rest.client;

import net.jcip.annotations.Immutable;

/**
 * The <code>Status</code> class is an outcome that provides an outcome or result of an operation.
 */
@Immutable
public final class Status {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The status severity levels.
     */
    public enum Severity {
        /**
         * Indicates an error status.
         */
        ERROR,

        /**
         * Indicates an informational status.
         */
        INFO,

        /**
         * Indicates an OK status.
         */
        OK,

        /**
         * Indicates an unknown status. This is automatically assigned if status is constructed with a <code>null</code> severity.
         */
        UNKNOWN,

        /**
         * Indicates a warning status.
         */
        WARNING
    }

    /**
     * A status with an OK severity and no message and no exception.
     * 
     * @see Severity#OK
     */
    public static final Status OK_STATUS = new Status(Severity.OK, null, null);

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The exception of this status or <code>null</code>.
     */
    private final Throwable exception;

    /**
     * The localized message of this status (can be <code>null</code>).
     */
    private final String message;

    /**
     * The severity level of this status (never <code>null</code>).
     */
    private final Severity severity;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param severity the status severity (if <code>null</code> it will be converted to {@link Severity#UNKNOWN}.
     * @param message the status message (if <code>null</code> it will be returned as an empty string)
     * @param exception the status exception or <code>null</code>
     */
    public Status( Severity severity,
                   String message,
                   Throwable exception ) {
        this.severity = ((severity == null) ? Severity.UNKNOWN : severity);
        this.message = message;
        this.exception = exception;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the status exception (may be <code>null</code>)
     */
    public Throwable getException() {
        return this.exception;
    }

    /**
     * @return the status message (never <code>null</code> but can be empty)
     */
    public String getMessage() {
        return ((this.message == null) ? "" : this.message);
    }

    /**
     * @return severity the status severity (never <code>null</code>)
     */
    public Severity getSeverity() {
        return this.severity;
    }

    /**
     * @return <code>true</code> if the status has a severity of {@link Severity#ERROR error}.
     */
    public boolean isError() {
        return (this.severity == Severity.ERROR);
    }

    /**
     * @return <code>true</code> if the status has a severity of {@link Severity#INFO info}.
     */
    public boolean isInfo() {
        return (this.severity == Severity.INFO);
    }

    /**
     * @return <code>true</code> if the status has a severity of {@link Severity#OK OK}.
     */
    public boolean isOk() {
        return (this.severity == Severity.OK);
    }

    /**
     * @return <code>true</code> if the status has a severity of {@link Severity#UNKNOWN unknown}.
     */
    public boolean isUnknown() {
        return (this.severity == Severity.UNKNOWN);
    }

    /**
     * @return <code>true</code> if the status has a severity of {@link Severity#WARNING warning}.
     */
    public boolean isWarning() {
        return (this.severity == Severity.WARNING);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder("Status ");
        txt.append(this.severity.toString()).append(": ");
        txt.append((getMessage().length() == 0) ? "<no message>" : getMessage());
        txt.append(" : ");
        txt.append((getException() == null) ? "<no error>" : getException());
        return txt.toString();
    }

}
