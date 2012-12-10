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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 * JcrManagedConnection
 *
 * @author kulikov
 */
public class JcrManagedConnection implements ManagedConnection {

    /**
     * The logger
     */
    private static final Logger log = Logger.getLogger("JcrManagedConnection");
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
    private final List<ConnectionEventListener> listeners = new CopyOnWriteArrayList();

    private final JcrConnectionRequestInfo cri;
    private Session session;

    // Handles.
    private final List<JcrSessionHandle> handles = new CopyOnWriteArrayList();;

    /**
     * Creates new instance of the managed connection.
     *
     * @param mcf Managed Connection Factory instance
     * @param cri Connection request info
     */
    public JcrManagedConnection(JcrManagedConnectionFactory mcf, JcrConnectionRequestInfo cri) throws ResourceException {
        this.mcf = mcf;
        this.cri = cri;
        this.logwriter = null;
        
        //init repository and open session
        this.session = openSession();
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
    private void addHandle(JcrSessionHandle handle) {
        handles.add(handle);
    }

    /**
     * Remove a session handle.
     *
     * @param handle session handle to remove.
     */
    private void removeHandle(JcrSessionHandle handle) {
        handles.remove(handle);
    }

    /**
     * Create a new session.
     *
     * @return new JCR session handle object.
     */
    private Session openSession() throws ResourceException {
        try {
            Repository repo = mcf.getRepository();
            Session s = repo.login(cri.getCredentials(), cri.getWorkspace());
            log.log(Level.FINEST, "Created session ({0})", session);
            return s;
        } catch (RepositoryException e) {
            throw new ResourceException("Failed to create session: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new connection handle for the underlying physical connection
     * represented by the ManagedConnection instance.
     *
     * @param subject Security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @return generic Object instance representing the connection handle.
     * @throws ResourceException generic exception if operation fails
     */
    public Object getConnection(Subject subject,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        JcrSessionHandle handle = new JcrSessionHandle(this);
        addHandle(handle);
        return handle;
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a ManagedConneciton instance.
     *
     * @param connection Application-level connection handle
     * @throws ResourceException generic exception if operation fails
     */
    public void associateConnection(Object connection) throws ResourceException {
        JcrSessionHandle handle = (JcrSessionHandle) connection;
        if (handle.getManagedConnection() != this) {
            handle.getManagedConnection().removeHandle(handle);
            handle.setManagedConnection(this);
            addHandle(handle);
        }
    }

    /**
     * Application server calls this method to force any cleanup on the
     * ManagedConnection instance.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void cleanup() throws ResourceException {
        this.session.logout();
        this.session = openSession();
        this.handles.clear();
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void destroy() throws ResourceException {
        this.session.logout();
        this.handles.clear();
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener A new ConnectionEventListener to be registered
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null");
        }
        listeners.add(listener);
    }

    /**
     * Removes an already registered connection event listener from the
     * ManagedConnection instance.
     *
     * @param listener already registered connection event listener to be
     * removed
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
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
    protected void closeHandle(JcrSessionHandle handle) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }
    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     *
     * @return Character ourput stream associated with this Managed-Connection
     * instance
     * @throws ResourceException generic exception if operation fails
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return logwriter;
    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     *
     * @param out Character Output stream to be associated
     * @throws ResourceException generic exception if operation fails
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        logwriter = out;
    }

    /**
     * Returns an
     * <code>javax.resource.spi.LocalTransaction</code> instance.
     *
     * @return LocalTransaction instance
     * @throws ResourceException generic exception if operation fails
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    /**
     * Returns an
     * <code>javax.transaction.xa.XAresource</code> instance.
     *
     * @return XAResource instance
     * @throws ResourceException generic exception if operation fails
     */
    public XAResource getXAResource() throws ResourceException {
        return (XAResource)session;
    }

    /**
     * Gets the metadata information for this connection's underlying EIS
     * resource manager instance.
     *
     * @return ManagedConnectionMetaData instance
     * @throws ResourceException generic exception if operation fails
     */
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
     * @return session related to specified handle.
     */
    public Session getSession(JcrSessionHandle handle) {
        if ((handles.size() > 0) && (handles.get(0) == handle)) {
            return session;
        } else {
            throw new java.lang.IllegalStateException("Inactive logical session handle called");
        }
    }

}
