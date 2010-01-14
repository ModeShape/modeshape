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
 * Sometimes its useful to work with a graph using objects that represent individual 
 * <a href="http://en.wikipedia.org/wiki/Command_pattern">commands</a> on the graph.  For example, "read node A"
 * or "create a node named C under node /A/B" or "create a copy of the subgraph at /E/F/G and place it under /E/H/I".
 * The command pattern has a number of benefits.  Since commands represent atomic activities, they map well to events
 * and can be easily serialized (making them very useful for {@link org.modeshape.graph.connector.RepositoryConnection connectors}). 
 * They provide an easy way to inherit or override functionality.  
 * New kinds of commands can be added with minimal (sometimes no) impact.
 * And existing commands can be changed to include new fields, making it possible to evolve a command
 * while minimizing the changes.
 * <p>
 * This package defines standard commands, called "requests", that correspond to different kinds of actions against a ModeShape graph.
 * Each kind of request is represented by a single concrete class, and all request classes extend from the
 * {@link Request} abstract class.  Because a lot of inheritance among commands can cause interference and inheritance cross-talk,
 * inheritance other than from {@link Request} is avoided as much possible.  (One exception to this is
 * {@link CacheableRequest}, which extends {@link Request} and serves as the base class for the "read" requests
 * that return results.)
 * </p>
 * <h3>Processing Requests</h3>
 * <p>
 * Request objects are sent to a {@link org.modeshape.graph.request.processor.RequestProcessor}, which is responsible
 * for performing the request.  {@link org.modeshape.graph.connector.RepositoryConnection Repository connectors} usually
 * implement their own <code>RequestProcessor</code>, which processes each submitted request by performing the
 * requested work.
 * </p>
 * <p>
 * Processor generally do not throw exceptions when processing requests (other than errors that signal problems
 * with the processor itself, such as connectivity problems, that signals the interruption of not only this request
 * but subsequent requests, too).  Instead, each request has an
 * {@link Request#setError(Throwable) error} field that can be used to store the exception that was encountered 
 * while processing the request.  This makes it possible to submit {@link CompositeRequest multiple requests}
 * at once, and have any errors directly associated with the request.
 * </p>
 * <h3>What's in a Request?</h3>
 * <p>
 * In general, a single request contains two kinds of information: the information that makes up the request (called the "input"),
 * and the information that is the result of the request (called the "result").  The input information contains everything
 * a processor needs to know to successfully perform the request.  For example, if the properties of a node are to be
 * read, then the input information must include the identifier or location of the  node.  If a node is to be moved,
 * then the input information must include the original location of the node as well as the new location.
 * Oh, the request's input information is immutable, ensuring that this part of the request doesn't change as it is passed
 * around the system.
 * </p>
 * <p>
 * A processor then fulfills the request by performing the requested work, and setting on the request any 
 * requested "results".  For example, if the properties of a node are to be read, then the results include
 * the set of {@link org.modeshape.graph.property.Property} objects.  If the children are to be read, then the
 * results consist of the list of {@link org.modeshape.graph.Location} object for each child.
 * </p>
 * <h3>Locations</h3>
 * <p>
 * All requests operate on some portion of the graph, so it's imperative that there be an easy but flexible
 * way to identify the location of that area, whether it's a node, subgraph, child reference, or node reference.
 * Like other parts of the ModeShape Graph API, requests use {@link org.modeshape.graph.Location} (or multiple
 * Location objects) as request inputs, and one Location object for each "reference" to a node that's in the output.
 * </p>
 * <p>
 * A {@link org.modeshape.graph.Location} can be specified as a {@link org.modeshape.graph.property.Path} and/or
 * as a set of identification properties.  Usually, Locations are created using just the path, since that's how
 * nodes are identified most of the time.  However, identification properties usually consist of information that 
 * uniquely (and quickly) identifies a node, so including identification properties in a Location may allow the 
 * processor to easily or more efficiently find the node given by the location.
 * </p>
 * <p>
 * Fortunately, requests often return Location objects that are <i>fully-defined</i>, meaning they have a Path 
 * <i>and</i> identification properties.  For example, the children of a node will be returned as a list of (fully-defined)
 * Location objects.  In fact, all requests have as part of their results an "actual" Location for each Location in the input,
 * so even when you don't have a fully-defined Location as an input, the request (after processing) should contain
 * a fully-defined Location for the input.
 * </p>
 * <p>
 * Because of this, and because working with a graph usually consists of making one request, using the results of that
 * request to create additional requests, and so on, you'll find that it's very easy to include fully-defined Location
 * objects in your requests.  Well, except for the first request.
 * </p>
 * <h3>Kinds of Requests</h3>
 * <p>
 * There are really two very broad categories of {@link Request}s: requests that don't modify content and those that do.
 * The requests that don't modify content are generally "read" requests that are requests to return information about
 * some part of the graph, and these requests should really have not side-effects on the graph.  Since these requests
 * contain results that are essentially snapshots in time of a portion of the graph content, these request types
 * extend {@link CacheableRequest} and contain fields for referencing a {@link org.modeshape.graph.cache.CachePolicy}
 * and a time that the results were loaded.
 * </p>
 * <p>
 * Requests that do modify graph content are used to create, update, move and delete content in the graph.  These
 * kinds of requests often have little or no results (the changes will be made to the graph unless an exception
 * is set on the request during processing), and as such the requests do not have any fields related to caching.
 * </p>
 * <p>
 * The supported requests currently include:
 * <ul>
 *   <li>{@link ReadNodeRequest} - A request to read the specified node and return all of that node's properties and the 
 *   location of every child node.</li>
 *   <li>{@link ReadAllChildrenRequest} - A request to read and return all of the children of the specified node.
 *   Each child node is represented by a {@link org.modeshape.graph.Location} object.  This request is useful for
 *   determining the structure of the graph without having to load all of the properties of each node.</li>
 *   <li>{@link ReadAllPropertiesRequest} - A request to read and return all of the properties of the specified node.
 *   This request is useful when all of the properties for a node are to be returned, but without any children
 *   information (usually because the children will be read later or were already read).</li>
 *   <li>{@link ReadBranchRequest} - A request to read a branch of the graph, returning the node (with its properties
 *   and children) at the specified location as well as nodes (with their properties and children) below the specified node,
 *   up to an optional maximum depth.  This request is useful for efficiently obtaining the nodes in a subgraph when
 *   the structure is known ahead of time.  This request can sometimes be significantly more efficient that walking
 *   the subgraph and submitting individual {@link ReadNodeRequest}s for each node in the subgraph.</li>
 *   <li>{@link ReadPropertyRequest} - A request to read and return a single named property on a specified node.</li>
 *   <li>{@link ReadBlockOfChildrenRequest} - A request to read the children of the specified node, returning only
 *   the locations for a subset of the children. The subset of children is defined by the index of the first child
 *   to be returned and the number of children to be returned.  This is often useful when the repository may have large numbers
 *   of children and it is unlikely that client applications will need all of them. </li>
 *   <li>{@link ReadNextBlockOfChildrenRequest} - A request to read the children of the specified node, returning
 *   only the locations for a subset of the children.  Unlike {@link ReadBlockOfChildrenRequest}, this request
 *   specifies the Location of the child that appears immediately before the first child to be returned.  This
 *   request is useful in concurrent applications when one client is reading the children while another client
 *   may be adding or removing children (thereby changing the indexes).  Since this request includes the Location of the 
 *   last child previously read, the request is unaffected by changing indexes.</li>
 *   <li>{@link CreateNodeRequest} - A request to create a new node under an existing parent node.  This request
 *   may optionally include properties for the new node.</li>
 *   <li>{@link CopyBranchRequest} - A request to copy an entire branch located under a specified node, and place
 *   the copy of that branch under another (existing) node.</li>
 *   <li>{@link MoveBranchRequest} - A request to move the specified node and all nodes below it into a new location
 *   specified by the Location of the new parent node.</li>
 *   <li>{@link RenameNodeRequest} - A request to rename an existing node.  This may adjust the indexes of sibling nodes
 *   with the same names (as same-name-sibling nodes are identified with SNS indexes).</li>
 *   <li>{@link UpdatePropertiesRequest} - A request to update one or more properties on a node.  Any property
 *   with no values will be removed, while properties with one or more values will be set (replace any existing property
 *   with the same name, if they exist). </li>
 *   <li>{@link RemovePropertyRequest} - A request to remove one property from a node.  No error is reported
 *   if the node does not contain a property that is to be removed.</li>
 *   <li>{@link SetPropertyRequest} - A request to set one property on a node.  No error is reported
 *   if the node does not already have the property, since the property is just created.</li>
 *   <li>{@link DeleteBranchRequest} - A request to delete a node and all nodes located below it.</li>
 *   <li>{@link CompositeRequest} - A request that acts as a container for multiple other requests (of various kinds),
 *   allowing you to batch together multiple for processing.  Use the one of the {@link CompositeRequest#with(Request...) CompositeRequest.with(...)}
 *   methods to create a CompositeRequest from a series of other requests.</li>
 * </ul>
 * </p>
 */

package org.modeshape.graph.request;

