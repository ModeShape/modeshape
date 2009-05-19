/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * JDBC connection wrapper
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class JdbcConnection implements RepositoryConnection {
    /**
     * Logging for this instance
     */
    protected Logger log = Logger.getLogger(getClass());

    private final String name;
    private final CachePolicy cachePolicy;
    private final Connection connection;
    private final UUID rootNodeUuid;

    /*package*/JdbcConnection( String sourceName,
                                CachePolicy cachePolicy,
                                Connection connection,
                                UUID rootNodeUuid ) {
        assert sourceName != null;
        assert connection != null;
        assert rootNodeUuid != null;
        this.name = sourceName;
        this.cachePolicy = cachePolicy; // may be null
        this.connection = connection;
        this.rootNodeUuid = rootNodeUuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return cachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getXAResource()
     */
    public XAResource getXAResource() {
        // if implemented by JDBC driver
        if (connection instanceof XAConnection) {
            try {
                return ((XAConnection)connection).getXAResource();
            } catch (SQLException e) {
                // handle an exception silently so far and write it to the log
                log.error(e, JdbcMetadataI18n.unableToGetXAResource, getSourceName());
                return null;
            }
        }
        // default
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        try {
            // JDBC 4 has a method to check validity of a connection (connection.isValid(timeout))
            // but many drivers didn't get updated with latest spec
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            // debug
            if (log.isDebugEnabled()) {
                log.debug(e, "{0}: Unable to check database connection due to error.", getSourceName());
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        // create processor and delegate handling
        RequestProcessor proc = new JdbcRequestProcesor(getSourceName(), context, connection, rootNodeUuid);
        try {
            proc.process(request);
        } finally {
            proc.close();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
        try {
            // release the JDBC connection resource
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            // handle exception silently so far
            if (log.isDebugEnabled()) {
                log.debug(e, "{0}: Unable to close database connection due to error.", getSourceName());
            }
        }
    }

}
