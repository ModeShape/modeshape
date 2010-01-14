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
package org.modeshape.graph.connector;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.ReferentialIntegrityException;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * A connection to a repository source.
 * <p>
 * These connections need not support concurrent operations by multiple threads.
 * </p>
 * <h3>Implementing a connector</h3>
 * <p>
 * While most of these methods are straightforward, a few warrant additional information. The {@link #ping(long, TimeUnit)} method
 * allows ModeShape to check the connection to see if it is alive. This method can be used in a variety of situations, ranging from
 * verifying that a {@link RepositorySource}'s JavaBean properties are correct to ensuring that a connection is still alive before
 * returning the connection from a connection pool.
 * </p>
 * <p>
 * The most important method on this interface, though, is the {@link #execute(ExecutionContext, Request)} method, which serves as
 * the mechanism by which the component using the connector access and manipulates the content exposed by the connector. The first
 * parameter to this method is the {@link ExecutionContext}, which contains the information about environment as well as the
 * subject performing the request.
 * </p>
 * <p>
 * The second parameter, however, represents a request that is to be processed by the connector. {@link Request} objects can take
 * many different forms, as there are different classes for each kind of request (see the {org.modeshape.graph.request} package
 * for more detail). Each request contains the information a connector needs to do the processing, and it also is the place where
 * the connector places the results (or the error, if one occurs).
 * </p>
 * <p>
 * Although there are over a dozen different kinds of requests, we do anticipate adding more in future releases. For example, ModeShape
 * will likely support searching repository content in sources through an additional subclass of {@link Request}. Getting the
 * version history for a node will likely be another kind of request added in an upcoming release.
 * </p>
 * <p>
 * A connector is technically free to implement the {@link #execute(ExecutionContext, Request)} method in any way, as long as the
 * semantics are maintained. But ModeShape provides a {@link RequestProcessor} class that can simplify writing your own connector and at
 * the same time help insulate your connector from new kinds of requests that may be added in the future. The
 * {@link RequestProcessor} is an abstract class that defines a <code>process(...)</code> method for each concrete {@link Request}
 * subclass. In other words, there is a {@link RequestProcessor#process(org.modeshape.graph.request.CompositeRequest)} method, a
 * {@link RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)} method, and so on.
 * </p>
 * <p>
 * To use a request processor in your connector, simply subclass {@link RequestProcessor} and override all of the abstract methods
 * and optionally override any of the other methods that have a default implementation. In many cases, the default implementations
 * of the <code>process(...)</code> methods are <i>sufficient</i> but probably not <i>efficient or optimum.</i> If that is the
 * case, simply provide your own methods that perform the request in a manner that is efficient for your source. However, if
 * performance is not a big issue, all of the concrete methods will provide the correct behavior. And remember, you can always
 * provide better implementations later, so it's often best to keep things simple at first.
 * </p>
 * <p>
 * Then, in your connector's {@link #execute(ExecutionContext, Request)} method, instantiate your {@link RequestProcessor}
 * subclass and pass the {@link #execute(ExecutionContext, Request) execute(...)} method's Request parameter directly into the the
 * request processor's {@link RequestProcessor#process(Request)} method, which will determine the appropriate method given the
 * actual Request object and will then invoke that method. For example:
 * 
 * <pre>
 * public void execute( ExecutionContext context,
 *                      Request request ) throws RepositorySourceException {
 *     RequestProcessor processor = new RequestProcessor(context);
 *     try {
 *         proc.process(request);
 *     } finally {
 *         proc.close();
 *     }
 * }
 * </pre>
 * 
 * If you do this, the bulk of your connector implementation will be in the RequestProcessor implementation methods. This not only
 * is more maintainable, it also lends itself to easier testing. And should any new request types be added in the future, your
 * connector may work just fine without any changes. In fact, if the {@link RequestProcessor} class can implement meaningful
 * methods for those new request types, your connector may "just work". Or, at least your connector will still be binary
 * compatible, even if your connector won't support any of the new features.
 * </p>
 * <p>
 * Finally, how should the connector handle exceptions? As mentioned above, each {@link Request} object has a
 * {@link Request#setError(Throwable) slot} where the connector can set any exception encountered during processing. This not only
 * handles the exception, but in the case of a {@link CompositeRequest} it also correctly associates the problem with the request.
 * However, it is perfectly acceptable to throw an exception if the connection becomes invalid (e.g., there is a communication
 * failure) or if a fatal error would prevent subsequent requests from being processed.
 * </p>
 */
@NotThreadSafe
public interface RepositoryConnection {

    /**
     * Get the name for this repository source. This value should be the same as that {@link RepositorySource#getName() returned}
     * by the same {@link RepositorySource} that created this connection.
     * 
     * @return the identifier; never null or empty
     */
    String getSourceName();

    /**
     * Return the transactional resource associated with this connection. The transaction manager will use this resource to manage
     * the participation of this connection in a distributed transaction.
     * 
     * @return the XA resource, or null if this connection is not aware of distributed transactions
     */
    XAResource getXAResource();

    /**
     * Ping the underlying system to determine if the connection is still valid and alive.
     * 
     * @param time the length of time to wait before timing out
     * @param unit the time unit to use; may not be null
     * @return true if this connection is still valid and can still be used, or false otherwise
     * @throws InterruptedException if the thread has been interrupted during the operation
     */
    boolean ping( long time,
                  TimeUnit unit ) throws InterruptedException;

    /**
     * Get the default cache policy for this repository. If none is provided, a global cache policy will be used.
     * 
     * @return the default cache policy
     */
    CachePolicy getDefaultCachePolicy();

    /**
     * Execute the supplied commands against this repository source.
     * 
     * @param context the environment in which the commands are being executed; never null
     * @param request the request to be executed; never null
     * @throws PathNotFoundException if the request(s) contain paths to nodes that do not exist
     * @throws ReferentialIntegrityException if the request is or contains a delete operation, where the delete could not be
     *         performed because some references to deleted nodes would have remained after the delete operation completed
     * @throws RepositorySourceException if there is a problem loading the node data
     */
    void execute( ExecutionContext context,
                  Request request ) throws RepositorySourceException;

    /**
     * Close this connection to signal that it is no longer needed and that any accumulated resources are to be released.
     */
    void close();
}
