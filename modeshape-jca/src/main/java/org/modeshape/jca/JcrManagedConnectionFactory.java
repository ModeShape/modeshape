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

import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Provides implementation for Managed Connection Factory.
 * 
 * @author kulikov
 */
@ConnectionDefinition( connectionFactory = Repository.class, connectionFactoryImpl = JcrRepositoryHandle.class, connection = Session.class, connectionImpl = JcrSessionHandle.class )
public class JcrManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

    private static final Logger LOGGER = Logger.getLogger(JcrManagedConnectionFactory.class);
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
     * repositoryURL
     */
    @ConfigProperty
    private String repositoryURL;

    /**
     * Repository instance
     */
    private Repository repository;

    private ModeShapeEngine engine;

    /**
     * Creates new factory instance.
     */
    public JcrManagedConnectionFactory() {
    }

    private boolean isAbsolutePath( String uri ) {
        return !(uri.startsWith("jndi") || uri.startsWith("file"));
    }

    private Repository deployRepository( String uri ) throws ResourceException {
        if (engine == null) {
            engine = ra.getEngine();
            if (engine == null) {
                throw new ResourceException("Engine not started by resource adapter!");
            }
        }

        // load configuration
        RepositoryConfiguration config = null;
        try {
            URL url = isAbsolutePath(uri) ? getClass().getClassLoader().getResource(uri) : new URL(uri);
            config = RepositoryConfiguration.read(url);
        } catch (Exception e) {
            throw new ResourceException(e);
        }

        // check configuration
        Problems problems = config.validate();
        if (problems.hasErrors()) {
            throw new ResourceException(problems.toString());
        }

        try {
            return engine.deploy(config);
        } catch (RepositoryException e) {
            throw new ResourceException(e);
        }
    }

    /**
     * Set repositoryURL
     * 
     * @param repositoryURL The value
     */
    public void setRepositoryURL( String repositoryURL ) {
        LOGGER.debug("Set repository URL=[{0}]", repositoryURL);
        this.repositoryURL = repositoryURL;
    }

    /**
     * Get repositoryURL
     * 
     * @return The value
     */
    public String getRepositoryURL() {
        return repositoryURL;
    }

    /**
     * Provides access to the configured repository.
     * 
     * @return repository specified by resource adapter configuration.
     * @throws ResourceException if there is an error getting the repository
     */
    public synchronized Repository getRepository() throws ResourceException {
        if (this.repository == null) {
            LOGGER.debug("Deploying repository URL [{0}]", repositoryURL);
            this.repository = deployRepository(repositoryURL);
        }
        return this.repository;
    }

    /**
     * Creates a Connection Factory instance.
     * 
     * @param cxManager ConnectionManager to be associated with created EIS connection factory instance
     * @return EIS-specific Connection Factory instance or javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    @Override
    public Object createConnectionFactory( ConnectionManager cxManager ) throws ResourceException {
        JcrRepositoryHandle handle = new JcrRepositoryHandle(this, cxManager);
        return handle;
    }

    /**
     * Creates a Connection Factory instance.
     * 
     * @return EIS-specific Connection Factory instance or javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new JcrConnectionManager());
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     * 
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @throws ResourceException generic exception
     * @return ManagedConnection instance
     */
    @Override
    public ManagedConnection createManagedConnection( Subject subject,
                                                      ConnectionRequestInfo cxRequestInfo ) throws ResourceException {
        return new JcrManagedConnection(this, (JcrConnectionRequestInfo)cxRequestInfo);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     * 
     * @param connectionSet Candidate connection set
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @throws ResourceException generic exception
     * @return ManagedConnection if resource adapter finds an acceptable match otherwise null
     */
    @SuppressWarnings( "rawtypes" )
    @Override
    public ManagedConnection matchManagedConnections( Set connectionSet,
                                                      Subject subject,
                                                      ConnectionRequestInfo cxRequestInfo ) throws ResourceException {
        for (Object connection : connectionSet) {
            if (connection instanceof JcrManagedConnection) {
                JcrManagedConnection mc = (JcrManagedConnection)connection;
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
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logwriter;
    }

    /**
     * Set the log writer for this ManagedConnectionFactory instance.
     * 
     * @param out PrintWriter - an out stream for error logging and tracing
     * @throws ResourceException generic exception
     */
    @Override
    public void setLogWriter( PrintWriter out ) throws ResourceException {
        logwriter = out;
    }

    /**
     * Get the resource adapter
     * 
     * @return The handle
     */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    /**
     * Set the resource adapter
     * 
     * @param ra The handle
     */
    @Override
    public void setResourceAdapter( ResourceAdapter ra ) {
        this.ra = (JcrResourceAdapter)ra;
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryURL == null) ? 0 : repositoryURL.hashCode());
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     * 
     * @param obj The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        JcrManagedConnectionFactory other = (JcrManagedConnectionFactory)obj;
        if (repositoryURL == null) {
            if (other.repositoryURL != null) return false;
        } else if (!repositoryURL.equals(other.repositoryURL)) return false;
        return true;
    }
}
