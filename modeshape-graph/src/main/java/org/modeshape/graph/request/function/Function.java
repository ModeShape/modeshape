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

/**
 * A serializable function that is to be called within a connector during a single atomic operation. Implementations subclass and
 * implement the {@link #run(FunctionContext)} method, where the supplied {@link FunctionContext} has all the information
 * necessary for the function to run: the supplied input parameters, a way to invoke read and change requests on the content, a
 * place to write the output, etc.
 * <p>
 * Here is a sample implementation of the Function interface that merely counts the number of nodes in a subgraph.
 * 
 * <pre>
 * protected static class CountNodesFunction extends Function {
 *     private static final long serialVersionUID = 1L;
 * 
 *     &#064;Override
 *     public void run( FunctionContext context ) {
 *         // Read the input parameter(s) ...
 *         int maxDepth = context.input(&quot;maxDepth&quot;, PropertyType.LONG, new Long(Integer.MAX_VALUE)).intValue();
 * 
 *         // Read the subgraph under the location ...
 *         ReadBranchRequest readSubgraph = context.builder().readBranch(context.appliedAt(), context.workspace(), maxDepth);
 *         // Process that request ...
 *         if (readSubgraph.hasError()) {
 *             context.setError(readSubgraph.getError());
 *         } else {
 * 
 *             // And count the number of nodes within the subgraph ...
 *             int counter = 0;
 *             for (Location location : readSubgraph) {
 *                 if (location != null) ++counter;
 *             }
 * 
 *             // And write the count as an output parameter ...
 *             context.setOutput(&quot;nodeCount&quot;, counter);
 *         }
 *     }
 * }
 * </pre>
 * 
 * </p>
 */
public abstract class Function implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The method called to invoke the function. The implementation can obtain from the supplied {@link FunctionContext} the
     * inputs to the function, a {@link FunctionContext#builder()} that can be used to create and immediately execute other
     * requests, the {@link FunctionContext#getExecutionContext() context of execution}, the {@link FunctionContext#appliedAt()
     * location in the graph} where the function is being applied, and other information needed during execution. The
     * implementation even uses the supplied FunctionContext to write {@link FunctionContext#output(String, Class, Object) output}
     * {@link FunctionContext#output(String, org.modeshape.graph.property.PropertyType, Object) parameters}.
     * 
     * @param context the context in which the function is being invoked, and which contains the inputs, the outputs, and methods
     *        to create and invoke other requests on the connector
     */
    public abstract void run( FunctionContext context );

    /**
     * Return whether this function only reads information.
     * <p>
     * This method always returns 'false'.
     * </p>
     * 
     * @return true if this function reads information, or false if it requests that the repository content be changed in some way
     */
    public boolean isReadOnly() {
        return false;
    }
}
