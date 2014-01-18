/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jca;

import java.io.Serializable;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 * CCI connection factory interface
 * 
 * @author kulikov
 */
public class JcrRepositoryHandle implements Referenceable, Serializable, Repository {
    private static final long serialVersionUID = 1L;
    private Reference reference;
    // Managed connection factory.
    private final JcrManagedConnectionFactory mcf;
    // Connection manager.
    private final ConnectionManager cm;
    private Repository repository;

    /**
     * Construct the repository.
     * 
     * @param mcf Managed connection factory interface
     * @param cm Connection manager.
     * @throws ResourceException if there is an error getting the repository
     */
    public JcrRepositoryHandle( JcrManagedConnectionFactory mcf,
                                ConnectionManager cm ) throws ResourceException {
        this.mcf = mcf;
        this.cm = cm;
        try {
            this.repository = mcf.getRepository();
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public Reference getReference() {
        return reference;
    }

    @Override
    public void setReference( Reference reference ) {
        this.reference = reference;
    }

    @Override
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor( String key ) {
        return repository.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor( String key ) {
        return repository.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue( String key ) {
        return repository.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues( String key ) {
        return repository.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor( String key ) {
        return repository.getDescriptor(key);
    }

    @Override
    public Session login( Credentials c ) throws LoginException, RepositoryException {
        return login(c, null);
    }

    @Override
    public Session login( String ws ) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, ws);
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public Session login( Credentials c,
                          String workspace ) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        try {
            return (Session)cm.allocateConnection(mcf, new JcrConnectionRequestInfo(c, workspace));
        } catch (ResourceException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e.getLinkedException();
            }
            if (cause instanceof LoginException) {
                throw (LoginException)cause;
            } else if (cause instanceof NoSuchWorkspaceException) {
                throw (NoSuchWorkspaceException)cause;
            } else if (cause instanceof RepositoryException) {
                throw (RepositoryException)cause;
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
