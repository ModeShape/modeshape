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
package org.jboss.dna.repository.federation;

import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionPool;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * A component that represents a {@link RepositorySource repository source} (with its state) being federated in a
 * {@link FederatedRepository}.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedSource implements RepositoryConnectionFactory {

    private final RepositorySource source;
    private final RepositoryConnectionPool connectionPool;

    public FederatedSource( RepositorySource source ) {
        ArgCheck.isNotNull(source, "source");
        this.source = source;
        this.connectionPool = new RepositoryConnectionPool(source);
    }

    protected FederatedSource( RepositorySource source, RepositoryConnectionPool connectionPool ) {
        ArgCheck.isNotNull(source, "source");
        this.source = source;
        this.connectionPool = connectionPool;
    }

    /**
     * Get the name for this federated repository source.
     * 
     * @return the name; never null or empty
     */
    public String getName() {
        return this.source.getName();
    }

    /**
     * Get the RepositorySource repository source information for this federated source.
     * 
     * @return the repository source; never null
     */
    public RepositorySource getRepositorySource() {
        return this.source;
    }

    /**
     * Get the connection pool used by this federated source.
     * 
     * @return the connection pool; never null
     */
    protected RepositoryConnectionPool getConnectionPool() {
        return this.connectionPool;
    }

    /**
     * {@inheritDoc}
     */
    public RepositoryConnection getConnection() throws RepositorySourceException, InterruptedException {
        return this.connectionPool.getConnection();
    }

    /**
     * Determine whether the federated source is available by attempting to connect to the source and
     * {@link RepositoryConnection#ping(long, TimeUnit) pinging} the source.
     * 
     * @param timeToWait the time the caller is willing to wait to establish a connection
     * @param unit the time unit for the <code>timeToWait</code> parameter; may not be null
     * @return true if the federated source is available, or false if the source is not available in the allotted time period
     */
    public boolean isAvailable( long timeToWait, TimeUnit unit ) {
        RepositoryConnection connection = null;
        try {
            connection = this.connectionPool.getConnection();
            return connection.ping(timeToWait, unit);
        } catch (IllegalStateException e) {
            // The connection pool is not running, so return false ..
            return false;
        } catch (InterruptedException e) {
            // Consider an attempt to get a connection and being interrupted as NOT being connected
            return false;
        } catch (Throwable e) {
            // Consider any other failure, including RepositorySourceException, as meaning not available
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (InterruptedException e) {
                    // Consider an attempt to get a connection and being interrupted as NOT being connected
                    return false;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return source.getName().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof FederatedSource) {
            FederatedSource that = (FederatedSource)obj;
            if (!this.source.getName().equals(that.source.getName())) return false;
            return true;
        }
        return false;
    }

}
