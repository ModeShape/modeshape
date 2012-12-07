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

import java.io.Serializable;
import javax.jcr.*;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 *
 * @author kulikov
 */
public class JcrRepositoryHandle implements Referenceable, Serializable, Repository {

    private Reference reference;
    // Managed connection factory.
    private final JcrManagedConnectionFactory mcf;
    // Connection manager.
    private final ConnectionManager cm;
    private Repository repository;

    /**
     * Construct the repository.
     */
    public JcrRepositoryHandle(JcrResourceAdapter ra, JcrManagedConnectionFactory mcf, ConnectionManager cm) throws ResourceException {
        System.out.println("Resource adaptor: " + ra);
        this.mcf = mcf;
        this.cm = cm;
        try {
            this.repository = ra.getRepository();
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }

    /**
     * Set the reference.
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return repository.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return repository.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(String key) {
        return repository.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return repository.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor(String key) {
        return repository.getDescriptor(key);
    }

    @Override
    public Session login(Credentials c) throws LoginException, RepositoryException {
        return login(c, null);
    }

    @Override
    public Session login(String ws) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, ws);
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }

    @Override
    public Session login(Credentials c, String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        try {
            return (Session) cm.allocateConnection(
                    mcf, new JcrConnectionRequestInfo(c, workspace));
        } catch (ResourceException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e.getLinkedException();
            }
            if (cause instanceof LoginException) {
                throw (LoginException) cause;
            } else if (cause instanceof NoSuchWorkspaceException) {
                throw (NoSuchWorkspaceException) cause;
            } else if (cause instanceof RepositoryException) {
                throw (RepositoryException) cause;
            } else if (cause != null) {
                throw new RepositoryException(cause);
            } else {
                throw new RepositoryException(e);
            }
        }
    }

    public Session getConnection() throws ResourceException {
        try {
            return login();
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }
}
