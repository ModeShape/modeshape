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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 *
 * @author kulikov
 */
public class JcrManagedConnection implements ManagedConnection, ManagedConnectionMetaData {

    // Managed connection factory.
    private final JcrManagedConnectionFactory mcf;
    // Connection request info.
    private final JcrConnectionRequestInfo cri;
    // Session instance.
    private Session session;
    // XAResource instance.
    private XAResource xaResource;
    // Listeners.
    private final List<ConnectionEventListener> listeners = new CopyOnWriteArrayList();
    // Handles.
    private final List<JcrSessionHandle> handles = new CopyOnWriteArrayList();;
    // Log writer.
    private PrintWriter logWriter;

    /**
     * Construct the managed connection.
     */
    public JcrManagedConnection(JcrManagedConnectionFactory mcf, JcrConnectionRequestInfo cri)
            throws ResourceException {
        this.mcf = mcf;
        this.cri = cri;
        this.session = openSession();
        if (this.mcf.bindSessionToTransaction) {
            this.xaResource = new TransactionalXAResource(this, (XAResource) session);
        } else {
            this.xaResource = (XAResource) session;
        }
    }

    /**
     * Create a new session.
     */
    private Session openSession() throws ResourceException {
        try {
            Repository repo = mcf.getRepository();
            System.out.println("Repository=" + repo);
            System.out.println("Connection request info=" + cri);

            Session session = repo.login(
                    cri.getCredentials(), cri.getWorkspace());
            log("Created session (" + session + ")");
            return session;
        } catch (RepositoryException e) {
            log("Failed to create session", e);
            ResourceException exception = new ResourceException(
                    "Failed to create session: " + e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Return the managed connection factory.
     */
    public JcrManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Return the connection request info.
     */
    public JcrConnectionRequestInfo getConnectionRequestInfo() {
        return cri;
    }

    @Override
    public Object getConnection(Subject sbjct, ConnectionRequestInfo cri) throws ResourceException {
        JcrSessionHandle handle = new JcrSessionHandle(this);
        addHandle(handle);
        return handle;
    }

    @Override
    public void destroy() throws ResourceException {
        this.session.logout();
        this.handles.clear();
    }

    @Override
    public void cleanup() throws ResourceException {
        synchronized (handles) {
            this.session.logout();
            this.session = openSession();
            this.handles.clear();
            if (this.mcf.bindSessionToTransaction && (this.xaResource instanceof TransactionalXAResource)) {
            	((TransactionalXAResource) this.xaResource).rebind((XAResource) session);
            } else {
            	this.xaResource = (XAResource) session;
            }
        }
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        JcrSessionHandle handle = (JcrSessionHandle) connection;
        if (handle.getManagedConnection() != this) {
            handle.getManagedConnection().removeHandle(handle);
            handle.setManagedConnection(this);
            addHandle(handle);
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return xaResource;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return this;
    }

    @Override
    public void setLogWriter(PrintWriter writer) throws ResourceException {
        this.logWriter = writer;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public String getEISProductName() throws ResourceException {
        try {
            return mcf.getRepository().getDescriptor("");
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public String getEISProductVersion() throws ResourceException {
        try {
            return mcf.getRepository().getDescriptor("");
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public int getMaxConnections() throws ResourceException {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getUserName() throws ResourceException {
        return session.getUserID();
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

    /**
     * Add a session handle.
     */
    private void addHandle(JcrSessionHandle handle) {
        synchronized (handles) {
            handles.add(handle);
        }
    }

    /**
     * Remove a session handle.
     */
    private void removeHandle(JcrSessionHandle handle) {
        synchronized (handles) {
            handles.remove(handle);
        }
    }

    /**
     * Release handles.
     */
    void closeHandles() {
        synchronized (handles) {
            JcrSessionHandle[] handlesArray = new JcrSessionHandle[handles.size()];
            handles.toArray(handlesArray);
            for (int i = 0; i < handlesArray.length; i++) {
                this.closeHandle(handlesArray[i]);
            }
        }
    }

    /**
     * Send event.
     */
    private void sendEvent(ConnectionEvent event) {
        synchronized (listeners) {
            for (Iterator<ConnectionEventListener> i = listeners.iterator(); i.hasNext();) {
                ConnectionEventListener listener = i.next();

                switch (event.getId()) {
                    case ConnectionEvent.CONNECTION_CLOSED:
                        listener.connectionClosed(event);
                        break;
                    case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                        listener.connectionErrorOccurred(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                        listener.localTransactionCommitted(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                        listener.localTransactionRolledback(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                        listener.localTransactionStarted(event);
                        break;
                    default:
                        // Unknown event, skip
                }
            }
        }
    }

    /**
     * Send event.
     */
    public void sendEvent(int type, Object handle, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (handle != null) {
            event.setConnectionHandle(handle);
        }

        sendEvent(event);
    }

    /**
     * Close the handle.
     */
    public void closeHandle(JcrSessionHandle handle) {
        if (handle != null) {
            removeHandle(handle);
            sendEvent(ConnectionEvent.CONNECTION_CLOSED, handle, null);
        }
    }

    /**
     * Return the session.
     */
    public Session getSession(JcrSessionHandle handle) {
        synchronized (handles) {
            if ((handles.size() > 0) && (handles.get(0) == handle)) {
                return session;
            } else {
                throw new java.lang.IllegalStateException("Inactive logical session handle called");
            }
        }
    }
}
