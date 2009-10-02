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
package org.jboss.dna.web.jcr.rest.client;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;
import org.jboss.dna.web.jcr.rest.client.json.JsonRestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>ServerManager</code> class managers the creation, deletion, and editing of servers hosting DNA repositories.
 */
@ThreadSafe
public final class ServerManager implements IRestClient {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    /**
     * The tag used to persist a server's login password.
     */
    private static final String PASSWORD_TAG = "password"; //$NON-NLS-1$

    /**
     * The file name used when persisting the server registry.
     */
    private static final String REGISTRY_FILE = "serverRegistry.xml"; //$NON-NLS-1$

    /**
     * The tag used when persisting a server.
     */
    private static final String SERVER_TAG = "server"; //$NON-NLS-1$

    /**
     * The server collection tag used when persisting the server registry.
     */
    private static final String SERVERS_TAG = "servers"; //$NON-NLS-1$

    /**
     * The tag used to persist a server's URL.
     */
    private static final String URL_TAG = "url"; //$NON-NLS-1$

    /**
     * The tag used to persist a server's login user.
     */
    private static final String USER_TAG = "user"; //$NON-NLS-1$

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The listeners registered to receive {@link ServerRegistryEvent server registry events}.
     */
    private final CopyOnWriteArrayList<IServerRegistryListener> listeners;

    /**
     * Executes the commands run on the DNA REST server.
     */
    private final IRestClient delegate;

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(ServerManager.class);

    /**
     * The path where the server registry is persisted or <code>null</code> if not persisted.
     */
    private final String stateLocationPath;

    /**
     * The server registry.
     */
    @GuardedBy( "serverLock" )
    private final List<Server> servers;

    /**
     * Lock used for when accessing the server registry.
     */
    private final ReadWriteLock serverLock = new ReentrantReadWriteLock();

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param stateLocationPath the directory where the {@link Server} registry} is persisted (may be <code>null</code> if
     *        persistence is not desired)
     * @param restClient the client that will communicate with the DNA REST server (never <code>null</code>)
     */
    public ServerManager( String stateLocationPath,
                          IRestClient restClient ) {
        CheckArg.isNotNull(restClient, "restClient"); //$NON-NLS-1$

        this.servers = new ArrayList<Server>();
        this.stateLocationPath = stateLocationPath;
        this.delegate = restClient;
        this.listeners = new CopyOnWriteArrayList<IServerRegistryListener>();
    }

    /**
     * This server manager uses the default REST Client.
     * 
     * @param stateLocationPath the directory where the {@link Server} registry} is persisted (may be <code>null</code> if
     *        persistence is not desired)
     */
    public ServerManager( String stateLocationPath ) {
        this(stateLocationPath, new JsonRestClient());
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * Listeners already registered will not be added again. The new listener will receive events for all existing servers.
     * 
     * @param listener the listener being register to receive events (never <code>null</code>)
     * @return <code>true</code> if listener was added
     */
    public boolean addRegistryListener( IServerRegistryListener listener ) {
        CheckArg.isNotNull(listener, "listener"); //$NON-NLS-1$
        boolean result = this.listeners.addIfAbsent(listener);

        // inform new listener of registered servers
        for (Server server : getServers()) {
            listener.serverRegistryChanged(ServerRegistryEvent.createNewEvent(this, server));
        }

        return result;
    }

    /**
     * Registers the specified <code>Server</code>.
     * 
     * @param server the server being added (never <code>null</code>)
     * @return a status indicating if the server was added to the registry
     */
    public Status addServer( Server server ) {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$
        return internalAddServer(server, true);
    }

    /**
     * @param url the URL of the server being requested (never <code>null</code>)
     * @param user the user ID of the server being requested (never <code>null</code>)
     * @return the requested server or <code>null</code> if not found in the registry
     */
    public Server findServer( String url,
                              String user ) {
        CheckArg.isNotNull(url, "url"); //$NON-NLS-1$
        CheckArg.isNotNull(user, "user"); //$NON-NLS-1$

        for (Server server : getServers()) {
            if (url.equals(server.getUrl()) && user.equals(server.getUser())) {
                return server;
            }
        }

        return null;
    }

    /**
     * @return an unmodifiable collection of registered servers (never <code>null</code>)
     */
    public Collection<Server> getServers() {
        try {
            this.serverLock.readLock().lock();
            return Collections.unmodifiableCollection(new ArrayList<Server>(this.servers));
        } finally {
            this.serverLock.readLock().unlock();
        }
    }

    /**
     * @return the name of the state file that the server registry is persisted to or <code>null</code>
     */
    private String getStateFileName() {
        String name = this.stateLocationPath;

        if (this.stateLocationPath != null) {
            name += File.separatorChar + REGISTRY_FILE;
        }

        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getRepositories(org.jboss.dna.web.jcr.rest.client.domain.Server)
     * @throws RuntimeException if the server is not registered
     * @see #isRegistered(Server)
     */
    public Collection<Repository> getRepositories( Server server ) throws Exception {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$

        try {
            this.serverLock.readLock().lock();

            if (isRegistered(server)) {
                Collection<Repository> repositories = this.delegate.getRepositories(server);
                return Collections.unmodifiableCollection(new ArrayList<Repository>(repositories));
            }

            // server must be registered in order to obtain it's repositories
            throw new RuntimeException(RestClientI18n.serverManagerUnregisteredServer.text(server.getShortDescription()));
        } finally {
            this.serverLock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getUrl(java.io.File, java.lang.String,
     *      org.jboss.dna.web.jcr.rest.client.domain.Workspace)
     */
    public URL getUrl( File file,
                       String path,
                       Workspace workspace ) throws Exception {
        return this.delegate.getUrl(file, path, workspace);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#getWorkspaces(org.jboss.dna.web.jcr.rest.client.domain.Repository)
     * @throws RuntimeException if the server is not registered
     * @see #isRegistered(Server)
     */
    public Collection<Workspace> getWorkspaces( Repository repository ) throws Exception {
        CheckArg.isNotNull(repository, "repository"); //$NON-NLS-1$

        try {
            this.serverLock.readLock().lock();

            if (isRegistered(repository.getServer())) {
                Collection<Workspace> workspaces = this.delegate.getWorkspaces(repository);
                return Collections.unmodifiableCollection(new ArrayList<Workspace>(workspaces));
            }

            // a repository's server must be registered in order to obtain it's workspaces
            String msg = RestClientI18n.serverManagerUnregisteredServer.text(repository.getServer().getShortDescription());
            throw new RuntimeException(msg);
        } finally {
            this.serverLock.readLock().unlock();
        }
    }

    /**
     * Registers the specified <code>Server</code>.
     * 
     * @param server the server being added
     * @param notifyListeners indicates if registry listeners should be notified
     * @return a status indicating if the server was added to the registry
     */
    private Status internalAddServer( Server server,
                                      boolean notifyListeners ) {
        boolean added = false;

        try {
            this.serverLock.writeLock().lock();

            if (!isRegistered(server)) {
                added = this.servers.add(server);
            }
        } finally {
            this.serverLock.writeLock().unlock();
        }

        if (added) {
            if (notifyListeners) {
                Exception[] errors = notifyRegistryListeners(ServerRegistryEvent.createNewEvent(this, server));
                return processRegistryListenerErrors(errors);
            }

            return Status.OK_STATUS;
        }

        // server already exists
        return new Status(Severity.ERROR, RestClientI18n.serverExistsMsg.text(server.getShortDescription()), null);
    }

    /**
     * @param server the server being removed
     * @param notifyListeners indicates if registry listeners should be notified
     * @return a status indicating if the specified server was removed from the registry
     */
    private Status internalRemoveServer( Server server,
                                         boolean notifyListeners ) {
        boolean removed = false;

        try {
            this.serverLock.writeLock().lock();

            // see if registered server has the same key
            for (Server registeredServer : this.servers) {
                if (registeredServer.hasSameKey(server)) {
                    removed = this.servers.remove(registeredServer);
                    break;
                }
            }
        } finally {
            this.serverLock.writeLock().unlock();
        }

        if (removed) {
            if (notifyListeners) {
                Exception[] errors = notifyRegistryListeners(ServerRegistryEvent.createRemoveEvent(this, server));
                return processRegistryListenerErrors(errors);
            }

            return Status.OK_STATUS;
        }

        // server could not be removed
        return new Status(Severity.ERROR,
                          RestClientI18n.serverManagerRegistryRemoveUnexpectedError.text(server.getShortDescription()), null);
    }

    /**
     * @param server the server being tested (never <code>null</code>)
     * @return <code>true</code> if the server has been registered
     * @see #addServer(Server)
     */
    public boolean isRegistered( Server server ) {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$

        try {
            this.serverLock.readLock().lock();

            // check to make sure no other registered server has the same key
            for (Server registeredServer : this.servers) {
                if (registeredServer.hasSameKey(server)) {
                    return true;
                }
            }

            return false;
        } finally {
            this.serverLock.readLock().unlock();
        }
    }

    /**
     * @param event the event the registry listeners are to process
     * @return any errors thrown by or found by the listeners or <code>null</code> (never empty)
     */
    private Exception[] notifyRegistryListeners( ServerRegistryEvent event ) {
        Collection<Exception> errors = null;

        for (IServerRegistryListener l : this.listeners) {
            try {
                Exception[] problems = l.serverRegistryChanged(event);

                if ((problems != null) && (problems.length != 0)) {
                    if (errors == null) {
                        errors = new ArrayList<Exception>();
                    }

                    errors.addAll(Arrays.asList(problems));
                }
            } catch (Exception e) {
                if (errors == null) {
                    errors = new ArrayList<Exception>();
                }

                errors.add(e);
            }
        }

        if ((errors != null) && !errors.isEmpty()) {
            return errors.toArray(new Exception[errors.size()]);
        }

        return null;
    }

    /**
     * @param errors the errors reported by the registry listeners
     * @return a status indicating if registry listeners reported any errors
     */
    private Status processRegistryListenerErrors( Exception[] errors ) {
        if (errors == null) {
            return Status.OK_STATUS;
        }

        for (Exception error : errors) {
            this.logger.error(error, RestClientI18n.serverManagerRegistryListenerError);
        }

        return new Status(Severity.WARNING, RestClientI18n.serverManagerRegistryListenerErrorsOccurred.text(), null);
    }

    /**
     * Attempts to connect to the server. The server does <strong>NOT</strong> need to be registered.
     * 
     * @param server the server being pinged (never <code>null</code>)
     * @return a status indicating if the server can be connected to
     * @see #isRegistered(Server)
     */
    public Status ping( Server server ) {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$

        try {
            this.delegate.getRepositories(server);
            return new Status(Severity.OK, RestClientI18n.serverManagerConnectionEstablishedMsg.text(), null);
        } catch (Exception e) {
            return new Status(Severity.ERROR, RestClientI18n.serverManagerConnectionFailedMsg.text(e), null);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only tries to unpublish if the workspace's {@link Server server} is registered.
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#publish(org.jboss.dna.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     * @see #isRegistered(Server)
     */
    public Status publish( Workspace workspace,
                           String path,
                           File file ) {
        CheckArg.isNotNull(workspace, "workspace"); //$NON-NLS-1$
        CheckArg.isNotNull(path, "path"); //$NON-NLS-1$
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$

        Server server = workspace.getServer();

        if (isRegistered(server)) {
            return this.delegate.publish(workspace, path, file);
        }

        // server must be registered in order to publish
        throw new RuntimeException(RestClientI18n.serverManagerUnregisteredServer.text(server.getShortDescription()));
    }

    /**
     * @param listener the listener being unregistered and will no longer receive events (never <code>null</code>)
     * @return <code>true</code> if listener was removed
     */
    public boolean removeRegistryListener( IServerRegistryListener listener ) {
        CheckArg.isNotNull(listener, "listener"); //$NON-NLS-1$
        return this.listeners.remove(listener);
    }

    /**
     * @param server the server being removed (never <code>null</code>)
     * @return a status indicating if the specified server was removed from the registry (never <code>null</code>)
     */
    public Status removeServer( Server server ) {
        CheckArg.isNotNull(server, "server"); //$NON-NLS-1$
        return internalRemoveServer(server, true);
    }

    /**
     * @return a status indicating if the previous session state was restored successfully
     */
    public Status restoreState() {
        if (this.stateLocationPath != null) {
            if (stateFileExists()) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = factory.newDocumentBuilder();
                    Document doc = docBuilder.parse(new File(getStateFileName()));
                    Node root = doc.getDocumentElement();
                    NodeList servers = root.getChildNodes();

                    for (int size = servers.getLength(), i = 0; i < size; ++i) {
                        Node server = servers.item(i);

                        if (server.getNodeType() != Node.TEXT_NODE) {
                            NamedNodeMap attributeMap = server.getAttributes();

                            if (attributeMap == null) continue;

                            Node urlNode = attributeMap.getNamedItem(URL_TAG);
                            Node userNode = attributeMap.getNamedItem(USER_TAG);
                            Node passwordNode = attributeMap.getNamedItem(PASSWORD_TAG);
                            String pswd = ((passwordNode == null) ? null : new String(Base64.decode(passwordNode.getNodeValue()),
                                                                                      "UTF-8")); //$NON-NLS-1$

                            // add server to registry
                            addServer(new Server(urlNode.getNodeValue(), userNode.getNodeValue(), pswd, (pswd != null)));
                        }
                    }
                } catch (Exception e) {
                    return new Status(Severity.ERROR, RestClientI18n.errorRestoringServerRegistry.text(getStateFileName()), e);
                }
            }
        }

        // do nothing of there is no save location or state file does not exist
        return Status.OK_STATUS;
    }

    /**
     * Saves the {@link Server} registry to the file system.
     * 
     * @return a status indicating if the registry was successfully saved
     */
    public Status saveState() {
        if ((this.stateLocationPath != null) && !getServers().isEmpty()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = factory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();

                // create root element
                Element root = doc.createElement(SERVERS_TAG);
                doc.appendChild(root);

                for (Server server : getServers()) {
                    Element serverElement = doc.createElement(SERVER_TAG);
                    root.appendChild(serverElement);

                    serverElement.setAttribute(URL_TAG, server.getUrl());
                    serverElement.setAttribute(USER_TAG, server.getUser());

                    if (server.isPasswordBeingPersisted()) {
                        serverElement.setAttribute(PASSWORD_TAG, Base64.encodeBytes(server.getPassword().getBytes()));
                    }
                }

                DOMSource source = new DOMSource(doc);
                StreamResult resultXML = new StreamResult(new FileOutputStream(getStateFileName()));
                TransformerFactory transFactory = TransformerFactory.newInstance();
                Transformer transformer = transFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
                transformer.transform(source, resultXML);
            } catch (Exception e) {
                return new Status(Severity.ERROR, RestClientI18n.errorSavingServerRegistry.text(getStateFileName()), e);
            }
        } else if ((this.stateLocationPath != null) && stateFileExists()) {
            // delete current registry file since all servers have been deleted
            try {
                new File(getStateFileName()).delete();
            } catch (Exception e) {
                return new Status(Severity.ERROR, RestClientI18n.errorDeletingServerRegistryFile.text(getStateFileName()), e);
            }
        }

        return Status.OK_STATUS;
    }

    /**
     * @return <code>true</code> if the state file already exists
     */
    private boolean stateFileExists() {
        return new File(getStateFileName()).exists();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only tries to unpublish if the workspace's {@link Server server} is registered.
     * 
     * @see org.jboss.dna.web.jcr.rest.client.IRestClient#unpublish(org.jboss.dna.web.jcr.rest.client.domain.Workspace,
     *      java.lang.String, java.io.File)
     * @see #isRegistered(Server)
     */
    public Status unpublish( Workspace workspace,
                             String path,
                             File file ) {
        CheckArg.isNotNull(workspace, "workspace"); //$NON-NLS-1$
        CheckArg.isNotNull(path, "path"); //$NON-NLS-1$
        CheckArg.isNotNull(file, "file"); //$NON-NLS-1$

        Server server = workspace.getServer();

        if (isRegistered(server)) {
            return this.delegate.unpublish(workspace, path, file);
        }

        // server must be registered in order to unpublish
        throw new RuntimeException(RestClientI18n.serverManagerUnregisteredServer.text(server.getShortDescription()));
    }

    /**
     * Updates the server registry with a new version of a server.
     * 
     * @param previousServerVersion the version of the server being replaced (never <code>null</code>)
     * @param newServerVersion the new version of the server being put in the server registry (never <code>null</code>)
     * @return a status indicating if the server was updated in the registry (never <code>null</code>)
     */
    public Status updateServer( Server previousServerVersion,
                                Server newServerVersion ) {
        CheckArg.isNotNull(previousServerVersion, "previousServerVersion"); //$NON-NLS-1$
        CheckArg.isNotNull(newServerVersion, "newServerVersion"); //$NON-NLS-1$

        Status status = null;

        try {
            this.serverLock.writeLock().lock();
            status = internalRemoveServer(previousServerVersion, false);

            if (status.isOk()) {
                status = internalAddServer(newServerVersion, false);

                if (status.isOk()) {
                    // all good so notify listeners
                    Exception[] errors = notifyRegistryListeners(ServerRegistryEvent.createUpdateEvent(this,
                                                                                                       previousServerVersion,
                                                                                                       newServerVersion));
                    return processRegistryListenerErrors(errors);
                }

                // unexpected problem adding new version of server to registry
                return new Status(Severity.ERROR, RestClientI18n.serverManagerRegistryUpdateAddError.text(status.getMessage()),
                                  status.getException());
            }
        } finally {
            this.serverLock.writeLock().unlock();
        }

        // unexpected problem removing server from registry
        return new Status(Severity.ERROR, RestClientI18n.serverManagerRegistryUpdateRemoveError.text(status.getMessage()),
                          status.getException());
    }

}
