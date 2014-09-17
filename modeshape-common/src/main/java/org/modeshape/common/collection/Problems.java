/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.collection;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Iterator;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.function.Consumer;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;

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
     * Determine the number of errors within these results.
     * 
     * @return the number of errors; always 0 or a positive number
     */
    int errorCount();

    /**
     * Determine the number of warnings within these results.
     * 
     * @return the number of warnings; always 0 or a positive number
     */
    int warningCount();

    /**
     * Determine the number of information messages within these results.
     * 
     * @return the number of information messages; always 0 or a positive number
     */
    int infoCount();

    /**
     * Determine the number of problems (that is, errors and warnings) within these results.
     * 
     * @return the number of errors and warnings; always 0 or a positive number
     */
    int problemCount();

    @Override
    Iterator<Problem> iterator();

    /**
     * Apply the consumer to each of the problems. This method does nothing if the consumer is null.
     *
     * @param consumer the consumer, which operates with side effects
     */
    void apply( Consumer<Problem> consumer );

    /**
     * Apply the consumer to each of the problems with the supplied status. This method does nothing if the status or consumer are
     * null.
     *
     * @param status the status of the problems to be consumed
     * @param consumer the consumer, which operates with side effects
     */
    void apply( Status status,
                Consumer<Problem> consumer );

    /**
     * Apply the consumer to each of the problems with the supplied status. This method does nothing if the status or consumer are
     * null.
     *
     * @param statuses the statuses of the problems to be consumed
     * @param consumer the consumer, which operates with side effects
     */
    void apply( EnumSet<Status> statuses,
                Consumer<Problem> consumer );

    /**
     * Write the problems to the supplied logger.
     *
     * @param logger the logger
     */
    void writeTo( Logger logger );

    /**
     * Write the problems to the supplied logger.
     *
     * @param logger the logger
     * @param firstStatus the first status to be logged
     * @param additionalStatuses the additional statuses to be logged
     */
    void writeTo( Logger logger,
                  Status firstStatus,
                  Status... additionalStatuses );
}
