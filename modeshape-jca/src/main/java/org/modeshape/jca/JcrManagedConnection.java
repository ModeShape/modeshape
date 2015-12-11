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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.txn.Transactions;


/**
 * JcrManagedConnection
 * 
 * @author kulikov
 */
public class JcrManagedConnection implements ManagedConnection {

    private static final Logger LOGGER = Logger.getLogger(JcrManagedConnection.class);
    /**
     * The logwriter
     */
    private PrintWriter logwriter;
    /**
     * ManagedConnectionFactory
     */
    private final JcrManagedConnectionFactory mcf;
    /**
     * Listeners
     */
    private final List<ConnectionEventListener> listeners = new CopyOnWriteArrayList<ConnectionEventListener>();

    private final JcrConnectionRequestInfo cri;
    private JcrSession session;
    private Transactions transactions;

    // Handles.
    private final List<JcrSessionHandle> handles = new CopyOnWriteArrayList<JcrSessionHandle>();

    /**
     * Creates new instance of the managed connection.
     * 
     * @param mcf Managed Connection Factory instance
     * @param cri Connection request info
     * @throws ResourceException if there is an error opening the session
     */
    public JcrManagedConnection( JcrManagedConnectionFactory mcf,
                                 JcrConnectionRequestInfo cri ) throws ResourceException {
        this.mcf = mcf;
        this.cri = cri;
        this.logwriter = null;

        // init repository and open session
        this.session = openSession();
        this.transactions = session.getRepository().transactions();
    }

    /**
     * Gets the managed connection factory.
     * 
     * @return Managed connection factory object.
     */
    public JcrManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Gets the connection request info.
     * 
     * @return Connection request info object.
     */
    public JcrConnectionRequestInfo getConnectionRequestInfo() {
        return cri;
    }

    /**
     * Add a session handle.
     * 
     * @param handle the session handle object to add
     */
    private void addHandle( JcrSessionHandle handle ) {
        handles.add(handle);
    }

    /**
     * Remove a session handle.
     * 
     * @param handle session handle to remove.
     */
    private void removeHandle( JcrSessionHandle handle ) {
        handles.remove(handle);
    }

    /**
     * Create a new session.
     * 
     * @return new JCR session handle object.
     * @throws ResourceException if there is an error opening the session
     */
    private JcrSession openSession() throws ResourceException {
        try {
            Repository repo = mcf.getRepository();
            Session s = repo.login(cri.getCredentials(), cri.getWorkspace());
            return (JcrSession) s;
        } catch (RepositoryException e) {
            throw new ResourceException("Failed to create session: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new connection handle for the underlying physical connection represented by the ManagedConnection instance.
     * 
     * @param subject Security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @return generic Object instance representing the connection handle.
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public Object getConnection( Subject subject,
                                 ConnectionRequestInfo cxRequestInfo ) throws ResourceException {
        JcrSessionHandle handle = new JcrSessionHandle(this);
        addHandle(handle);
        return handle;
    }

    /**
     * Used by the container to change the association of an application-level connection handle with a ManagedConneciton
     * instance.
     * 
     * @param connection Application-level connection handle
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public void associateConnection( Object connection ) throws ResourceException {
        JcrSessionHandle handle = (JcrSessionHandle)connection;
        if (handle.getManagedConnection() != this) {
            handle.getManagedConnection().removeHandle(handle);
            handle.setManagedConnection(this);
            addHandle(handle);
        }
    }

    /**
     * Application server calls this method to force any cleanup on the ManagedConnection instance.
     * 
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public void cleanup() throws ResourceException {
        this.session.logout();
        this.session = openSession();
        this.transactions = session.getRepository().transactions();
        this.handles.clear();
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     * 
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public void destroy() throws ResourceException {
        LOGGER.debug("Shutting down connection to repo '{0}'", mcf.getRepositoryURL());
        this.session.logout();
        this.handles.clear();
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     * 
     * @param listener A new ConnectionEventListener to be registered
     */
    @Override
    public void addConnectionEventListener( ConnectionEventListener listener ) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null");
        }
        listeners.add(listener);
    }

    /**
     * Removes an already registered connection event listener from the ManagedConnection instance.
     * 
     * @param listener already registered connection event listener to be removed
     */
    @Override
    public void removeConnectionEventListener( ConnectionEventListener listener ) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null");
        }
        listeners.remove(listener);
    }

    /**
     * Close handle
     * 
     * @param handle The handle
     */
    protected void closeHandle( JcrSessionHandle handle ) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }
    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     * 
     * @return Character output stream associated with this Managed-Connection instance
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logwriter;
    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     * 
     * @param out Character Output stream to be associated
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public void setLogWriter( PrintWriter out ) throws ResourceException {
        logwriter = out;
    }

    /**
     * Returns an <code>javax.resource.spi.LocalTransaction</code> instance.
     * 
     * @return LocalTransaction instance
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return new JcrLocalTransaction(transactions); 
    }

    /**
     * Returns an <code>javax.transaction.xa.XAresource</code> instance.
     * 
     * @return XAResource instance
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new UnsupportedOperationException("ModeShape 5 does not support XA");
    }

    /**
     * Gets the metadata information for this connection's underlying EIS resource manager instance.
     * 
     * @return ManagedConnectionMetaData instance
     * @throws ResourceException generic exception if operation fails
     */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        try {
            return new JcrManagedConnectionMetaData(mcf.getRepository(), session);
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    /**
     * Searches session object using handle.
     * 
     * @param handle the session handle
     * @return session related to specified handle.
     */
    public Session getSession( JcrSessionHandle handle ) {
        if ((handles.size() > 0) && (handles.get(0) == handle)) {
            return session;
        }
        throw new java.lang.IllegalStateException("Inactive logical session handle called");
    }

}
