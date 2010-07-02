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
package org.modeshape.jboss.managed;

import java.util.concurrent.TimeUnit;
import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.connector.RepositoryConnectionPool;

/**
 * A <code>ManagedConnectionPool</code> instance is a JBoss managed object for a {@link RepositoryConnectionPool repository
 * connection pool}.
 */
@ManagementObject( name = "ModeShapeConnectionPool", description = "A ModeShape repository connection pool", componentType = @ManagementComponent( type = "ModeShape", subtype = "ConnectionPool" ), properties = ManagementProperties.EXPLICIT )
public final class ManagedConnectionPool implements ModeShapeManagedObject {

    /**
     * The ModeShape object being managed and delegated to (never <code>null</code>).
     */
    private final RepositoryConnectionPool connectionPool;

    /**
     * Creates a JBoss managed object for the specified connection pool.
     * 
     * @param connectionPool the connection pool being managed (never <code>null</code>)
     */
    public ManagedConnectionPool( RepositoryConnectionPool connectionPool ) {
        CheckArg.isNotNull(connectionPool, "connectionPool");
        this.connectionPool = connectionPool;
    }

    /**
     * Removes all connections. This is a JBoss managed operation.
     * 
     * @return <code>true</code> if successful
     */
    @ManagementOperation( description = "Removes all in-use connections from the pool", impact = Impact.WriteOnly )
    public boolean flush() {
        // TODO implement flush()
        return false;
    }

    /**
     * Obtains the number of connections available for use. This is a JBoss managed readonly property.
     * 
     * @return the number of available connections
     */
    @ManagementProperty( name = "Available Connections", description = "The number of available connections", readOnly = true, use = ViewUse.STATISTIC )
    public int getAvailableCount() {
        return getMaxSize() - getSize();
    }

    /**
     * Obtains the number of connections that have been created. This is a JBoss managed readonly property.
     * 
     * @return the number of connections created
     */
    @ManagementProperty( name = "Connections Created", description = "The number of connections that have been created", readOnly = true, use = ViewUse.STATISTIC )
    public long getCreatedCount() {
        return this.connectionPool.getTotalConnectionsCreated();
    }

    /**
     * Obtains the number of connections that have been destroyed. This is a JBoss managed readonly property.
     * 
     * @return the number of connections destroyed
     */
    @ManagementProperty( name = "Connections Destroyed", description = "The number of connections that have been destroyed", readOnly = true, use = ViewUse.STATISTIC )
    public long getDestroyedCount() {
        // TODO how do you calculate connection destroyed count
        return this.connectionPool.getTotalConnectionsCreated() - getSize();
    }

    /**
     * Obtains the total number of times connections have been used. This is a JBoss managed readonly property.
     * 
     * @return the number of times
     */
    @ManagementProperty( name = "Connections In-use", description = "The number of connections currently being used", readOnly = true, use = ViewUse.STATISTIC )
    public int getInUseCount() {
        return this.connectionPool.getInUseCount();
    }

    @ManagementProperty( name = "In-use High Water Mark", description = "The in-use high water mark", readOnly = true, use = ViewUse.STATISTIC )
    public int getInUseHighWaterMark() {
        // TODO implement getInUseHighWaterMark()
        return 0;
    }

    /**
     * Obtains the time a connection can remain idle before being closed. This is a JBoss managed writable property.
     * 
     * @return the new connection max idle time
     */
    @ManagementProperty( name = "Keep Alive Time", description = "The time in nanonseconds to keep an idle connection alive", readOnly = false, use = ViewUse.RUNTIME )
    public long getKeepAliveTime() {
        return this.connectionPool.getKeepAliveTime(TimeUnit.NANOSECONDS);
    }

    /**
     * Obtains the number of failed attempts at trying to a connection before throwing an error. This is a JBoss managed writable
     * property.
     * 
     * @return the number of failed attempts to connect
     */
    @ManagementProperty( name = "Max Failed Attempts Before Error", description = "The number of failed attempts to establish a connection before throwing an error", readOnly = false, use = ViewUse.RUNTIME )
    public int getMaxFailedAttemptsBeforeError() {
        return this.connectionPool.getMaxFailedAttemptsBeforeError();
    }

    /**
     * Obtains the maximum size of the pool. This is a JBoss managed writable property.
     * 
     * @return the maximum pool size
     */
    @ManagementProperty( name = "Max size", description = "The maximum number of connections allowed", readOnly = false, use = ViewUse.RUNTIME )
    public int getMaxSize() {
        return this.connectionPool.getMaximumPoolSize();
    }

    @ManagementProperty( name = "Min size", description = "The minimum number of connections allowed", readOnly = true, use = ViewUse.CONFIGURATION )
    public int getMinSize() {
        // TODO implement getMinSize()
        return 0;
    }

    /**
     * Obtains the time to wait for a ping to complete. This is a JBoss managed writable property.
     * 
     * @return the time in nanoseconds
     */
    @ManagementProperty( name = "Ping Timeout", description = "The time in nanoseconds that ping should wait before timing out and failing", readOnly = false, use = ViewUse.RUNTIME )
    public long getPingTimeout() {
        return this.connectionPool.getPingTimeoutInNanos();
    }

    /**
     * Obtains the current number of connections. This is a JBoss managed readonly property.
     * 
     * @return the number of connections
     */
    @ManagementProperty( name = "Total Connections", description = "The total number of connections (in use and available)", readOnly = true, use = ViewUse.STATISTIC )
    public int getSize() {
        return this.connectionPool.getCorePoolSize();
    }

    /**
     * Indicates if connections should be validated before being used. This is a JBoss managed writable property.
     * 
     * @return <code>true</code> if connections are validated before being used
     */
    @ManagementProperty( name = "Validate Before Use", description = "Indicates if connections should be validated before being used", readOnly = false, use = ViewUse.RUNTIME )
    public boolean getValidateConnectionBeforeUse() {
        return this.connectionPool.getValidateConnectionBeforeUse();
    }

    /**
     * Sets the time a connection can remain idle before being closed. This is a JBoss managed property.
     * 
     * @param nanos the new connection max idle time (must be non-negative)
     */
    public void setKeepAliveTime( long nanos ) {
        CheckArg.isNonNegative(nanos, "nanos");
        this.connectionPool.setKeepAliveTime(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Sets the maximum number of attempts to try and establish a connection. This is a JBoss managed property.
     * 
     * @param attempts the new maximum number of attempts (must be non-negative)
     */
    public void setMaxFailedAttemptsBeforeError( int attempts ) {
        CheckArg.isNonNegative(attempts, "attempts");
        this.connectionPool.setMaxFailedAttemptsBeforeError(attempts);
    }

    /**
     * Sets the maximum size of the number of connections allowed. This is a JBoss managed property.
     * 
     * @param size the new maximum pool size (must be positive)
     */
    public void setSize( int size ) {
        CheckArg.isPositive(size, "size");
        this.connectionPool.setMaximumPoolSize(size);
    }

    /**
     * Sets the time a ping will wait to complete. This is a JBoss managed property.
     * 
     * @param nanos the new time to wait in nanoseconds (must be non-negative)
     */
    public void setPingTimout( long nanos ) {
        CheckArg.isNonNegative(nanos, "nanos");
        this.connectionPool.setPingTimeout(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Sets the flag indicating if the connection should be validated before being used. This is a JBoss managed property.
     * 
     * @param validate the new value validate flag value
     */
    public void setValidateConnectionBeforeUse( boolean validate ) {
        this.connectionPool.setValidateConnectionBeforeUse(validate);
    }

}
