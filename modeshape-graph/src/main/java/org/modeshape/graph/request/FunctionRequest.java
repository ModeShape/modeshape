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
package org.modeshape.graph.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.base.Processor;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.function.Function;

/**
 * 
 */
public final class FunctionRequest extends Request implements Cloneable {

    private static final long serialVersionUID = 1L;

    private final Location at;
    private final String workspaceName;
    private final Function function;
    private final Map<String, Serializable> inputs;
    private final Map<String, Serializable> outputs;
    private List<Request> requests;
    private Location actualLocation;

    /**
     * Create a request to execute the function the properties and number of children of a node at the supplied location.
     * 
     * @param function the function to be performed
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param inputs the immutable map of input names to values for the function invocation, or null if there are no inputs
     * @throws IllegalArgumentException if the function, location or workspace name is null
     */
    public FunctionRequest( Function function,
                            Location at,
                            String workspaceName,
                            Map<String, Serializable> inputs ) {
        CheckArg.isNotNull(function, "function");
        CheckArg.isNotNull(at, "at");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.at = at;
        this.function = function;
        this.inputs = inputs != null ? inputs : Collections.<String, Serializable>emptyMap();
        this.outputs = new HashMap<String, Serializable>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return function.isReadOnly();
    }

    /**
     * Get the location defining the node that is to be read.
     * 
     * @return the location of the node; never null
     */
    public Location at() {
        return at;
    }

    /**
     * Get the name of the workspace in which the function is being applied.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * The unmodifiable map of input parameter name to value.
     * 
     * @return the map of input names to values; never null but possibly empty
     */
    public Map<String, Serializable> inputs() {
        return inputs;
    }

    /**
     * Get the value for the named input to this function. If the actual value corresponds to a {@link PropertyType}, then the
     * value will be converted to the specified type using the {@link ExecutionContext}'s {@link ValueFactories}. Otherwise, the
     * value will simply be cast to the supplied type.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the input parameter
     * @param type the expected type of the value
     * @param defaultValue the default value to use, if the input doesn't have the named parameter
     * @param context the execution context to be used for converting the value; may not be null
     * @return the (possibly null) value for the named input, or the specified default value if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    public <T> T input( String name,
                        Class<T> type,
                        T defaultValue,
                        ExecutionContext context ) {
        return convert(name, type, defaultValue, this.inputs, context);
    }

    /**
     * Get the value for the named input to this function. The actual value will be converted to the specified type using the
     * {@link ExecutionContext}'s {@link ValueFactories}.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the input parameter
     * @param type the expected type of the value
     * @param defaultValue the default value to use, if the input doesn't have the named parameter
     * @param context the execution context to be used for converting the value; may not be null
     * @return the (possibly null) value for the named input, or the specified default value if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    public <T> T input( String name,
                        PropertyType type,
                        T defaultValue,
                        ExecutionContext context ) {
        return convert(name, type, defaultValue, this.inputs, context);
    }

    /**
     * Get the function implementation
     * 
     * @return the function implementation; never null
     */
    public Function function() {
        return function;
    }

    /**
     * The unmodifiable map of input parameter name to value.
     * 
     * @return the map of input names to values; never null but possibly empty
     */
    public Map<String, Serializable> outputs() {
        return outputs;
    }

    /**
     * Get the value for the named output to this function.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the output parameter
     * @param type the expected type of the value used to convert the actual value using the
     * @param context the execution context to be used for converting the value; may not be null
     * @return the (possibly null) value for the named output, or null if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    public <T> T output( String name,
                         Class<T> type,
                         ExecutionContext context ) {
        return convert(name, type, null, this.outputs, context);
    }

    /**
     * Get the value for the named output to this function.
     * 
     * @param <T> the desired type of the value
     * @param name the name of the output parameter
     * @param type the expected type of the value used to convert the actual value using the
     * @param defaultValue the default value for the output parameter, if there is no such named parameter
     * @param context the execution context to be used for converting the value; may not be null
     * @return the (possibly null) value for the named output, or the specified default if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    public <T> T output( String name,
                         Class<T> type,
                         T defaultValue,
                         ExecutionContext context ) {
        return convert(name, type, defaultValue, this.outputs, context);
    }

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
     * @param defaultValue the default value for the output parameter, if there is no such named parameter
     * @param context the execution context to be used for converting the value; may not be null
     * @return the (possibly null) value for the named output, or null if there is no such named parameter
     * @throws ValueFormatException if the conversion from to the expected value could not be performed
     */
    public <T> T output( String name,
                         PropertyType type,
                         T defaultValue,
                         ExecutionContext context ) {
        return convert(name, type, defaultValue, this.outputs, context);
    }

    /**
     * Set the value for the named output parameter for the function. If the value is nul, the parameter will be removed.
     * 
     * @param name the name of the output parameter; may not be null
     * @param value the value for the named parameter; may be null
     * @return the prior value for this named parameter
     * @throws IllegalArgumentException if the name is null or empty
     * @throws IllegalStateException if the request is frozen
     */
    public Serializable setOutput( String name,
                                   Serializable value ) {
        checkNotFrozen();
        CheckArg.isNotEmpty(name, "name");
        return value != null ? this.outputs.put(name, value) : this.outputs.remove(name);
    }

    /**
     * Add an actual request created and executed by the invocation of this function. This method will be called by the
     * {@link Processor} and should not be called elsewhere.
     * 
     * @param request the request
     * @throws IllegalArgumentException if the request reference is null
     */
    public void addActualRequest( Request request ) {
        checkNotFrozen();
        CheckArg.isNotNull(request, "request");
        if (requests == null) requests = new ArrayList<Request>();
        requests.add(request);
    }

    /**
     * Add the actual requests created and executed by the invocation of this function. This method will be called by the
     * {@link Processor} and should not be called elsewhere.
     * 
     * @param requests the requests
     * @throws IllegalArgumentException if the requests reference is null
     */
    public void addActualRequests( Iterable<Request> requests ) {
        checkNotFrozen();
        CheckArg.isNotNull(requests, "requests");
        if (this.requests == null) requests = new ArrayList<Request>();
        for (Request request : requests) {
            if (request != null) this.requests.add(request);
        }
    }

    /**
     * Get the number of actual requests created and executed by the invocation of this function. This will always be 0 before the
     * request is processed.
     * 
     * @return the number of actual requests; never negative
     */
    public int getActualRequestCount() {
        return requests != null ? requests.size() : 0;
    }

    /**
     * Get the actual requests created and executed by the invocation of this function. These may be read requests or
     * {@link ChangeRequest change requests}.
     * 
     * @return the actual requests that the function created and executed; never null
     */
    public Iterator<Request> getActualRequests() {
        if (requests != null) return requests.iterator();
        return Collections.<Request>emptyList().iterator();
    }

    /**
     * Sets the actual and complete location of the node whose children and properties have been read. This method must be called
     * when processing the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being read, or null if the {@link #at() current location} should be used
     * @throws IllegalArgumentException if the actual location is null or does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
    }

    /**
     * Get the actual location of the node whose children and properties were read.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocation = null;
        this.outputs.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(at, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            FunctionRequest that = (FunctionRequest)obj;
            if (!this.at().isSame(that.at())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            if (!this.function.getClass().equals(that.function.getClass())) return false;
            if (!this.inputs().equals(that.inputs())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String workspaceName = this.workspaceName != null ? "'" + this.workspaceName + "'" : "default";
        String functionName = function.getClass().getSimpleName();
        return "applyfn at " + printable(at()) + " (in " + workspaceName + " workspace) the " + functionName;
    }

    @Override
    public RequestType getType() {
        return RequestType.FUNCTION;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not clone the results.
     * </p>
     */
    @Override
    public FunctionRequest clone() {
        return new FunctionRequest(function, at, workspaceName, inputs);
    }

    @SuppressWarnings( "unchecked" )
    protected final <T> T convert( String name,
                                   Class<T> type,
                                   T defaultValue,
                                   Map<String, Serializable> values,
                                   ExecutionContext context ) {
        Object value = values.get(name);
        if (value == null) return defaultValue;
        PropertyType propertyType = PropertyType.discoverType(type);
        if (propertyType != null) {
            ValueFactory<?> factory = context.getValueFactories().getValueFactory(propertyType);
            return (T)factory.create(value);
        }
        if (type.isInstance(value)) {
            return (T)value;
        }
        String msg = GraphI18n.errorConvertingType.text(value.getClass().getSimpleName(), type.getSimpleName(), value);
        throw new ValueFormatException(value, PropertyType.OBJECT, msg);
    }

    @SuppressWarnings( "unchecked" )
    protected final <T> T convert( String name,
                                   PropertyType type,
                                   T defaultValue,
                                   Map<String, Serializable> values,
                                   ExecutionContext context ) {
        Object value = values.get(name);
        if (value == null) return defaultValue;
        ValueFactory<?> factory = context.getValueFactories().getValueFactory(type);
        return (T)factory.create(value);
    }

}
