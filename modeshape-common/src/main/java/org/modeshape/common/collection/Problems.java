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
package org.modeshape.common.collection;

import java.io.Serializable;
import java.util.Iterator;
import org.modeshape.common.i18n.I18n;

/**
 * An interface for a collection of {@link Problem} objects, with multiple overloaded methods for adding errors, warnings, and
 * informational messages.
 */
public interface Problems extends Iterable<Problem>, Serializable {

    /**
     * Add an error message with the parameters that should be used when localizing the message.
     * 
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( I18n message,
                   Object... params );

    /**
     * Add an error exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the error; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( Throwable throwable,
                   I18n message,
                   Object... params );

    /**
     * Add an error message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( String resource,
                   String location,
                   I18n message,
                   Object... params );

    /**
     * Add an error exception and message with a description of the resource, its location, and the parameters that should be used
     * when localizing the message
     * 
     * @param throwable the exception that represents the error; may be null
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( Throwable throwable,
                   String resource,
                   String location,
                   I18n message,
                   Object... params );

    /**
     * Add an error message with the parameters that should be used when localizing the message.
     * 
     * @param code the error code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( int code,
                   I18n message,
                   Object... params );

    /**
     * Add an error exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the error; may be null
     * @param code the error code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( Throwable throwable,
                   int code,
                   I18n message,
                   Object... params );

    /**
     * Add an error message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param code the error code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( int code,
                   String resource,
                   String location,
                   I18n message,
                   Object... params );

    /**
     * Add an error exception and message with a description of the resource, its location, and the parameters that should be used
     * when localizing the message
     * 
     * @param throwable the exception that represents the error; may be null
     * @param code the error code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addError( Throwable throwable,
                   int code,
                   String resource,
                   String location,
                   I18n message,
                   Object... params );

    /**
     * Add a warning message with the parameters that should be used when localizing the message.
     * 
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( I18n message,
                     Object... params );

    /**
     * Add a warning exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the error; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( Throwable throwable,
                     I18n message,
                     Object... params );

    /**
     * Add a warning message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( String resource,
                     String location,
                     I18n message,
                     Object... params );

    /**
     * Add a warning exception and message with a description of the resource, its location, and the parameters that should be
     * used when localizing the message
     * 
     * @param throwable the exception that represents the warning; may be null
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( Throwable throwable,
                     String resource,
                     String location,
                     I18n message,
                     Object... params );

    /**
     * Add a warning message with the parameters that should be used when localizing the message.
     * 
     * @param code the problem code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( int code,
                     I18n message,
                     Object... params );

    /**
     * Add a warning exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the warning; may be null
     * @param code the problem code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( Throwable throwable,
                     int code,
                     I18n message,
                     Object... params );

    /**
     * Add a warning message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param code the problem code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( int code,
                     String resource,
                     String location,
                     I18n message,
                     Object... params );

    /**
     * Add a warning exception and message with a description of the resource, its location, and the parameters that should be
     * used when localizing the message
     * 
     * @param throwable the exception that represents the warning; may be null
     * @param code the problem code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addWarning( Throwable throwable,
                     int code,
                     String resource,
                     String location,
                     I18n message,
                     Object... params );

    /**
     * Add a informational message with the parameters that should be used when localizing the message.
     * 
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( I18n message,
                  Object... params );

    /**
     * Add an informational exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the warning; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( Throwable throwable,
                  I18n message,
                  Object... params );

    /**
     * Add an informational message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( String resource,
                  String location,
                  I18n message,
                  Object... params );

    /**
     * Add an informational exception and message with a description of the resource, its location, and the parameters that should
     * be used when localizing the message
     * 
     * @param throwable the exception that represents the problem; may be null
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( Throwable throwable,
                  String resource,
                  String location,
                  I18n message,
                  Object... params );

    /**
     * Add a informational message with the parameters that should be used when localizing the message.
     * 
     * @param code the problem code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( int code,
                  I18n message,
                  Object... params );

    /**
     * Add a informational exception and message with the parameters that should be used when localizing the message.
     * 
     * @param throwable the exception that represents the warning; may be null
     * @param code the problem code
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( Throwable throwable,
                  int code,
                  I18n message,
                  Object... params );

    /**
     * Add an informational message with a description of the resource, its location, and the parameters that should be used when
     * localizing the message
     * 
     * @param code the problem code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( int code,
                  String resource,
                  String location,
                  I18n message,
                  Object... params );

    /**
     * Add an informational exception and message with a description of the resource, its location, and the parameters that should
     * be used when localizing the message
     * 
     * @param throwable the exception that represents the problem; may be null
     * @param code the problem code
     * @param resource the description of the resource; may be null
     * @param location the location of the resource; may be null
     * @param message the internationalized message describing the problem
     * @param params the values for the parameters in the message
     */
    void addInfo( Throwable throwable,
                  int code,
                  String resource,
                  String location,
                  I18n message,
                  Object... params );

    /**
     * Add all of the problems in the supplied list.
     * 
     * @param problems the problems to add to this list; this method does nothing if null or empty
     */
    void addAll( Iterable<Problem> problems );

    /**
     * Get the number of problems that are in this collection
     * 
     * @return the number of problems; never negative
     * @see #hasProblems()
     * @see #isEmpty()
     */
    int size();

    /**
     * Determine if this collection is empty.
     * 
     * @return true if the there are no problems, or false if there is at least one
     * @see #hasProblems()
     * @see #size()
     */
    boolean isEmpty();

    /**
     * Determine if there are problems in this collection.
     * 
     * @return true if there is at least one problem, or false if it is empty
     * @see #isEmpty()
     * @see #size()
     */
    boolean hasProblems();

    /**
     * Determine if there is at least one error in this collection.
     * 
     * @return true if there is at least one error in this collection, or false if there are no errors
     */
    boolean hasErrors();

    /**
     * Determine if there is at least one warning in this collection.
     * 
     * @return true if there is at least one warning in this collection, or false if there are no warnings
     */
    boolean hasWarnings();

    /**
     * Determine if there is at least one informational problem in this collection.
     * 
     * @return true if there is at least one informational problem in this collection, or false if there are no informational
     *         problems
     */
    boolean hasInfo();

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see java.lang.Iterable#iterator()
     */
    Iterator<Problem> iterator();
}
