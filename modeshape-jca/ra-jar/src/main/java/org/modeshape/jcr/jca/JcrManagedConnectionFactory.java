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
package org.modeshape.jcr.jca;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 *
 * @author kulikov
 */
public class JcrManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

    //repository
    private Repository repository;

    //log writer
    private PrintWriter logWriter;

    // Repository parameters.
    private final Map<String, String> parameters = new HashMap<String, String>();

    //RA
    private JcrResourceAdapter ra;

    protected boolean bindSessionToTransaction = true;

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        JcrRepositoryHandle handle = new JcrRepositoryHandle(ra, this, cm);
        log("Created repository handle (" + handle + ")");
        return handle;
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new JcrConnectionManager());
    }

    @Override
    public ManagedConnection createManagedConnection(Subject sbjct, ConnectionRequestInfo cri) throws ResourceException {
        return new JcrManagedConnection(this, (JcrConnectionRequestInfo)cri);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set set, Subject sbjct,
    ConnectionRequestInfo cri) throws ResourceException {
        for (Object connection : set) {
            if (connection instanceof JcrManagedConnection) {
                JcrManagedConnection mc = (JcrManagedConnection) connection;
                if (equals(mc.getManagedConnectionFactory())) {
                    JcrConnectionRequestInfo otherCri = mc.getConnectionRequestInfo();
                    if (cri == otherCri || (cri != null && cri.equals(otherCri))) {
                        return mc;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        this.logWriter = writer;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    /**
     * Return the repository URI.
     */
    public String getRepositoryURI() {
        return parameters.get(RepositoryFactory.URL);
    }

    /**
     * Set the repository URI.
     */
    public void setRepositoryURI(String uri) {
        parameters.put(RepositoryFactory.URL, uri);
    }
    /**
     * Return the repository.
     */
    public synchronized Repository getRepository() throws RepositoryException {
        return ra.getRepository();
    }

    /**
     * Log a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Log a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof JcrManagedConnectionFactory)) {
            return false;
        }

        return ((JcrManagedConnectionFactory)other).repository == repository;
    }

    @Override
    public int hashCode() {
        return repository.hashCode();
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return this.ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        System.out.println("Set resource adaptor");
        this.ra = (JcrResourceAdapter) ra;
    }
}
