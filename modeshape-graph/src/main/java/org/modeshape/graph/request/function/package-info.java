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
/**
 * The {@link org.modeshape.graph.request.function.Function} interface provides a way to inject custom logic 
 * into a connector, where it can be executed close to the actual data and where it can make decisions about 
 * what changes should be made to the content. The {@link org.modeshape.graph.request.function.FunctionContext}
 * interface encapsulates the context in which a function is {@link org.modeshape.graph.request.function.Function#run(FunctionContext) run}, including
 * the input parameters, output parameters, location in the graph, and ways of building and executing other
 * requests.
 * <p>
 * To use a {@link org.modeshape.graph.request.function.Function}, simply subclass it, implement the 
 * {@link org.modeshape.graph.request.function.Function#run(FunctionContext)} method, and then used it
 * via the {@link org.modeshape.graph.Graph Graph API}, either through direct (immediately) interaction:
 * <pre>
 *     Graph graph = ...
 *     Function myFunction = ...
 *     Map<String,Serializable> output = graph.applyFunction(myFunction)
 *                                            .with("a","val1")
 *                                            .and("b",3)
 *                                            .to("/");
 *     Object count = output.get("count");
 * </pre>
 * or via batch invocation:
 * <pre>
 *     Graph graph = ...
 *     Function myFunction = ...
 *     Graph.Batch batch = graph.batch();
 *     batch.applyFunction(myFunction)
 *          .with("a","val1")
 *          .and("b",3)
 *          .to("/")
 *          .and()
 *          .read("/myNode");
 *     Results results = batch.execute();
 *     List<Request> requests = results.getRequests();
 *     FunctionRequest functRequest = requests.get(0);
 *     ReadNodeRequest readRequest = requests.get(1);
 *     
 *     // use the results in the requests ...
 *     long count = functRequest.output("success",PropertyType.LONG,new Long(0)).longValue();
 *     
 *     for ( Property prop : readRequest.getProperties() ) {
 *        // do something ...
 *     }
 * </pre>
 * </p>
 */

package org.modeshape.graph.request.function;

