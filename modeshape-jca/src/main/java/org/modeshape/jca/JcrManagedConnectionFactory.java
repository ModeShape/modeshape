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
package org.modeshape.jca;

import java.io.PrintWriter;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.Session;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

import javax.security.auth.Subject;

/**
 * Provides implementation for Managed Connection Factory.
 *
 * @author kulikov
 */
@ConnectionDefinition(connectionFactory = Repository.class,
connectionFactoryImpl = JcrRepositoryHandle.class,
connection = Session.class,
connectionImpl = JcrSessionHandle.class)
public class JcrManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * The resource adapter
     */
    private JcrResourceAdapter ra;
    /**
     * The logwriter
     */
    private PrintWriter logwriter;

    /**
     * Creates new factory instance.
     */
    public JcrManagedConnectionFactory() {
    }

    /**
     * Provides access to the configured repository.
     *
     * @return repository specified by resource adapter configuration.
     */
    public synchronized Repository getRepository() throws ResourceException {
        return ra.getRepository();
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @param cxManager ConnectionManager to be associated with created EIS
     * connection factory instance
     * @return EIS-specific Connection Factory instance or
     * javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        JcrRepositoryHandle handle = new JcrRepositoryHandle(ra, this, cxManager);
        return handle;
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @return EIS-specific Connection Factory instance or
     * javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new JcrConnectionManager());
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     *
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection
     * request information
     * @throws ResourceException generic exception
     * @return ManagedConnection instance
     */
    public ManagedConnection createManagedConnection(Subject subject,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return new JcrManagedConnection(this, (JcrConnectionRequestInfo)cxRequestInfo);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     *
     * @param connectionSet Candidate connection set
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection
     * request information
     * @throws ResourceException generic exception
     * @return ManagedConnection if resource adapter finds an acceptable match
     * otherwise null
     */
    public ManagedConnection matchManagedConnections(Set connectionSet,
            Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        for (Object connection : connectionSet) {
            if (connection instanceof JcrManagedConnection) {
                JcrManagedConnection mc = (JcrManagedConnection) connection;
                if (equals(mc.getManagedConnectionFactory())) {
                    JcrConnectionRequestInfo otherCri = mc.getConnectionRequestInfo();
                    if (cxRequestInfo == otherCri || (cxRequestInfo != null && cxRequestInfo.equals(otherCri))) {
                        return mc;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the log writer for this ManagedConnectionFactory instance.
     *
     * @return PrintWriter
     * @throws ResourceException generic exception
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return logwriter;
    }

    /**
     * Set the log writer for this ManagedConnectionFactory instance.
     *
     * @param out PrintWriter - an out stream for error logging and tracing
     * @throws ResourceException generic exception
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logwriter = out;
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        this.ra = (JcrResourceAdapter) ra;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 17;
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false
     * otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof JcrManagedConnectionFactory) {
            return this == other;
        }
        return false;
    }
}
