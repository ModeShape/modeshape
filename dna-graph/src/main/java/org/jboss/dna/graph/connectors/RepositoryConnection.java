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
package org.jboss.dna.graph.connectors;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.GraphCommand;

/**
 * A connection to a repository source.
 * <p>
 * These connections need not support concurrent operations by multiple threads.
 * </p>
 * 
 * @author Randall Hauch
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
     * Set the listener that is to receive notifications to changes to content within this source.
     * 
     * @param listener the new listener, or null if no component is interested in the change notifications
     */
    void setListener( RepositorySourceListener listener );

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
     * @param commands the commands to be executed; never null
     * @throws RepositorySourceException if there is a problem loading the node data
     */
    void execute( ExecutionContext context,
                  GraphCommand... commands ) throws RepositorySourceException;

    /**
     * Close this connection to signal that it is no longer needed and that any accumulated resources are to be released.
     */
    void close();
}
