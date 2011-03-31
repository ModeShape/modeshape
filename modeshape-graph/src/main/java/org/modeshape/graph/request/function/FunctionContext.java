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
package org.modeshape.graph.request.function;

import java.io.Serializable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.RequestBuilder;

/**
 * The context in which a {@link Function} is executed.
 */
public interface FunctionContext {

    /**
     * The context in which this function is executing.
     * 
     * @return the execution context; never null
     */
    ExecutionContext getExecutionContext();

    /**
     * Get the location of the node at which the function is to be applied.
     * 
     * @return the location where the function is to be applied; never null
     */
    Location appliedAt();

    /**
     * Get the name of the workspace in which this function is being applied.
     * 
     * @return the workspace name
     */
    String workspace();

    /**
     * Get the value for the named input to this function. If the actual value corresponds to a {@link PropertyType}, then the
     * value will be converted to the specified type using the {@link ExecutionContext}'s {@link ValueFactories}. Otherwise, the
     * value will simply be cast to the supplied type.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the input parameter
     * @param type the expected type of the value
     * @return the (possibly null) value for the named input, or null if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    <T> T input( String name,
                 Class<T> type );

    /**
     * Get the value for the named input to this function. If the actual value corresponds to a {@link PropertyType}, then the
     * value will be converted to the specified type using the {@link ExecutionContext}'s {@link ValueFactories}. Otherwise, the
     * value will simply be cast to the supplied type.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the input parameter
     * @param type the expected type of the value
     * @param defaultValue the default value for the parameter
     * @return the (possibly null) value for the named input, or the default value if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    <T> T input( String name,
                 Class<T> type,
                 T defaultValue );

    /**
     * Get the value for the named input to this function. The actual value will be converted to the specified type using the
     * {@link ExecutionContext}'s {@link ValueFactories}.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the input parameter
     * @param type the expected type of the value
     * @param defaultValue the default value for the parameter
     * @return the (possibly null) value for the named input, or the default value if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    <T> T input( String name,
                 PropertyType type,
                 T defaultValue );

    /**
     * Get the value for the named output to this function. If the actual value corresponds to a {@link PropertyType}, then the
     * value will be converted to the specified type using the {@link ExecutionContext}'s {@link ValueFactories}. Otherwise, the
     * value will simply be cast to the supplied type.
     * <p>
     * This is a convenience method, as the value would have already been set by the same function invocation using
     * {@link #setOutput(String, Serializable)}.
     * </p>
     * 
     * @param <T> the desired type of the value
     * @param name the name of the output parameter
     * @param type the expected type of the value
     * @param defaultValue the default value for the parameter
     * @return the (possibly null) value for the named output, or the default value if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    <T> T output( String name,
                  Class<T> type,
                  T defaultValue );

    /**
     * Get the value for the named output to this function. The actual value will be converted to the specified type using the
     * {@link ExecutionContext}'s {@link ValueFactories}.
     * <p>
     * This is a convenience method, as the value would have already been set by the same function invocation using
     * {@link #setOutput(String, Serializable)}.
     * </p>
     * 
     * @param <T> the desired type of the value
     * @param name the name of the output parameter
     * @param type the expected type of the value
     * @param defaultValue the default value for the parameter
     * @return the (possibly null) value for the named output, or null if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    <T> T output( String name,
                  PropertyType type,
                  T defaultValue );

    /**
     * Set the value for the named output parameter for the function.
     * 
     * @param name the name of the output parameter; may not be null
     * @param value the value for the named parameter; must be {@link Serializable}
     */
    void setOutput( String name,
                    Serializable value );

    /**
     * Immediately execute the supplied request on this connector. Note that the caller is responsible for checking whether the
     * supplied request {@link Request#hasError() has an error}, and if that error results in an error condition of the caller,
     * then the caller is responsible for {@link #setError(Throwable) setting the error on this context}.
     * 
     * @param request the request to be performed; may not be null
     * @see #builder()
     */
    void execute( Request request );

    /**
     * Return a build that can be used to build, immediately execute, and return the executed requests. When this is used, there
     * is no need to {@link #execute(Request)} them manually.
     * 
     * @return the builder; never null
     * @see #execute(Request)
     */
    RequestBuilder builder();

    /**
     * Get the 'current time' for this processor, which is usually a constant during its lifetime.
     * 
     * @return the current time in UTC; never null
     */
    DateTime getNowInUtc();

    /**
     * Record that an error occurred.
     * 
     * @param t the exception
     */
    void setError( Throwable t );

    /**
     * Determine whether this execution has been cancelled. This should be checked periodically in longer-running functions.
     * 
     * @return true if the execution has been cancelled, or false otherwise
     */
    boolean isCancelled();

}
