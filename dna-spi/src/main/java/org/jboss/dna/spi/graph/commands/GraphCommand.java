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
package org.jboss.dna.spi.graph.commands;

/**
 * The base interface for all repository commands
 * 
 * @author Randall Hauch
 */
public interface GraphCommand {

    /**
     * Return whether this command has been cancelled.
     * 
     * @return true if this command has been cancelled, or false otherwise.
     */
    boolean isCancelled();

    /**
     * Set the error for this command.
     * 
     * @param error the exception
     * @see #getError()
     * @see #hasError()
     * @see #hasNoError()
     */
    void setError( Throwable error );

    /**
     * Get the error for this command.
     * 
     * @return the error, or null if there is no error
     * @see #setError(Throwable)
     * @see #hasError()
     * @see #hasNoError()
     */
    Throwable getError();

    /**
     * Return true if this command has an {@link #getError() error}.
     * 
     * @return true if the command has an error, or false otherwise
     * @see #getError()
     * @see #setError(Throwable)
     * @see #hasNoError()
     */
    boolean hasError();

    /**
     * Convenience method that is equivalent to <code>!hasError()</code>.
     * 
     * @return true if the command has no error, or false otherwise
     * @see #getError()
     * @see #setError(Throwable)
     * @see #hasError()
     */
    boolean hasNoError();

}
