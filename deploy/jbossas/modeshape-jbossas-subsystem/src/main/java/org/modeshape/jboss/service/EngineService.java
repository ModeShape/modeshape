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
package org.modeshape.jboss.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.jcr.RepositoryException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection.Property;
import org.modeshape.jboss.subsystem.JBossSubsystemI18n;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrEngine.State;
import org.modeshape.jcr.JcrRepository;

/**
 * A <code>ManagedEngine</code> instance is a JBoss managed object for a {@link JcrEngine}.
 */
public final class EngineService implements Service<JcrEngine>, Serializable {

    private static final long serialVersionUID = 1L;

    private JcrEngine engine = new JcrEngine();

    public static enum Component {
        CONNECTOR,
        SEQUENCER,
        CONNECTIONPOOL,
        SEQUENCINGSERVICE,
        REPOSITORY
    }

    public EngineService() {
        this.engine = null;
    }

    /**
     * Set the engine instance for this service
     * 
     * @param engine the engine (never <code>null</code>)
     */
    public EngineService( JcrEngine engine ) {
        CheckArg.isNotNull(engine, "engine");
        this.engine = engine;
    }

    @Override
    public JcrEngine getValue() throws IllegalStateException, IllegalArgumentException {
        return this.getEngine();
    }

    @Override
    public void start( final StartContext context ) {

        this.engine.start();

    }

    @Override
    public void stop( StopContext arg0 ) {

        engine.shutdown();

    }

    /**
     * A utility method that must be used by all non-synchronized methods to access the engine. Subsequent calls to this method
     * may return different JcrEngine instances, so all non-synchronized methods should call this method once and use the returned
     * reference for all operations against the engine.
     * 
     * @return the engine at the moment this method is called; may be null if an engine could not be created
     */
    protected synchronized JcrEngine getEngine() {
        return this.engine;
    }

    /**
     * Indicates if the managed engine is running. This is a JBoss managed operation.
     * 
     * @return <code>true</code> if the engine is running
     */
    public synchronized boolean isRunning() {
        if (this.engine == null) return false;
        try {
            return this.engine.getState().equals(State.RUNNING);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * First {@link #shutdown() shutdowns} the engine and then {@link #start() starts} it back up again. This is a JBoss managed
     * operation.
     * 
     * @see #shutdown()
     * @see #start()
     * @throws Exception if there is a problem restarting the engine (usually reading the configuration file)
     */
    // @ManagementOperation(description = "Restarts this engine", impact = Impact.Lifecycle)
    // public synchronized void restart() throws Exception {
    // // Grab a reference to the existing engine first ...
    // JcrEngine oldEngine = this.engine;
    // try {
    // // Before we shutdown the existing engine, start up a new one ...
    // // loadConfigurationAndCreateEngine();
    // start();
    // } catch (Throwable e) {
    // // There was a problem starting the new engine, so keep the old
    // // engine (which is still running) ...
    // this.engine = oldEngine;
    // if (e instanceof RuntimeException)
    // throw (RuntimeException) e;
    // throw (Exception) e;
    // }
    //
    // // At this point, we know that the new engine was started correctly and
    // // is ready to be used.
    // // So now we can shutdown the old engine (preventing new sessions from
    // // being created), waiting up to 10 seconds
    // // for any previously-created sessions to be closed gracefully, and then
    // // forcing termination of all
    // // remaining, longer-running sessions.
    // oldEngine.shutdownAndAwaitTermination(10, TimeUnit.SECONDS);
    // }

    // /**
    // * Performs an engine shutdown. This is a JBoss managed operation.
    // *
    // * @see JcrEngine#shutdown()
    // */
    // // @ManagementOperation(description = "Shutdowns this engine", impact = Impact.Lifecycle)
    // public synchronized void shutdown() {
    // if (isRunning()) {
    // try {
    // this.engine.shutdown();
    // } finally {
    // this.engine = null;
    // }
    // }
    // }

    /**
     * Obtains the managed connectors of this engine. This is a JBoss managed operation.
     * 
     * @return an unmodifiable collection of managed connectors (never <code>null</code>)
     */
    // @ManagementOperation(description = "Obtains the managed connectors of this engine", impact = Impact.ReadOnly)
    // public Collection<ManagedConnector> getConnectors() {
    // if (!isRunning())
    // return Collections.emptyList();
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // Collection<ManagedConnector> connectors = new ArrayList<ManagedConnector>();
    // for (RepositorySource repositorySource : engine.getRepositoryService().getRepositoryLibrary().getSources()) {
    // assert repositorySource != null;
    // connectors.add(new ManagedConnector(repositorySource));
    // }
    //
    // return Collections.unmodifiableCollection(connectors);
    // }

    /**
     * Get the number of connections currently in use
     * 
     * @param connectorName
     * @return boolean
     */
    // public long getInUseConnections(String connectorName) {
    // if (!isRunning())
    // return 0;
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // long totalConnectionsInUse = 0;
    // try {
    // totalConnectionsInUse = engine.getRepository(repositoryName)
    // } catch (Exception e) {
    // Logger.getLogger(getClass()).error(e,
    // JBossManagedI18n.errorDeterminingTotalInUseConnections,
    // connectorName);
    // }
    //
    // return totalConnectionsInUse;
    // }

    /**
     * Get the number of sessions currently active
     * 
     * @param repositoryName
     * @return boolean
     */
    // public long getActiveSessions(String repositoryName) {
    // if (!isRunning())
    // return 0;
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // int totalActiveSessions = 0;
    // try {
    // totalActiveSessions = getRepository(repositoryName).getRepositoryStatistics().statisticsFor(value)
    // } catch (Exception e) {
    // Logger.getLogger(getClass()).error(e,
    // JBossManagedI18n.errorDeterminingTotalInUseConnections,
    // repositoryName);
    // }
    //
    // return totalActiveSessions;
    // }

    /**
     * Obtains the properties for the passed in object. This is a JBoss managed operation.
     * 
     * @param objectName
     * @param objectType
     * @return an collection of managed properties (may be <code>null</code>)
     */
    // public List<ManagedProperty> getProperties(String objectName,
    // Component objectType) {
    // if (!isRunning())
    // return Collections.emptyList();
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // List<ManagedProperty> managedProps = new ArrayList<ManagedProperty>();
    // if (objectType.equals(Component.CONNECTOR)) {
    // repositorySource repositorySource = engine
    // .getRepositorySource(objectName);
    // assert repositorySource != null : "Connection '" + objectName
    // + "' does not exist";
    // managedProps = ManagedUtils.getProperties(objectType,
    // repositorySource);
    // } else if (objectType.equals(Component.CONNECTIONPOOL)) {
    // RepositoryConnectionPool connectionPool = engine
    // .getRepositoryService().getRepositoryLibrary()
    // .getConnectionPool(objectName);
    // assert connectionPool != null : "Repository Connection Pool for repository '"
    // + objectName + "' does not exist";
    // managedProps = ManagedUtils.getProperties(objectType,
    // connectionPool);
    // }
    //
    // return managedProps;
    // }

    /*
     * ManagedRepository operations
     */

    /**
     * Obtains the repositories of this engine.
     * 
     * @return an unmodifiable collection of repositories (never <code>null</code>)
     */
    public Collection<String> getRepositories() {
        if (!isRunning()) return Collections.emptyList();

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        Collection<String> repositories = new ArrayList<String>();
        for (String repositoryName : engine.getRepositoryNames()) {
            repositories.add(repositoryName);
        }

        return repositories;
    }

    /**
     * Obtains the specified managed repository of this engine. This is called by the JNDIManagedRepositories when a JNDI lookup
     * is performed to find a repository.
     * 
     * @param repositoryName for the repository to be returned
     * @return a repository or <code>null</code> if repository doesn't exist
     */
    public JcrRepository getRepository( String repositoryName ) {
        if (!isRunning()) return null;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        try {
            return engine.getRepository(repositoryName);
        } catch (RepositoryException e) {
            Logger.getLogger(getClass()).error(e, JBossSubsystemI18n.errorGettingRepositoryFromEngine, repositoryName);
            return null;
        }
    }

    /**
     * Obtains the JCR version supported by this repository. This is a JBoss managed readonly property.
     * 
     * @param repositoryName (never <code>null</code>)
     * @return String version (never <code>null</code>)
     */
    public String getRepositoryVersion( String repositoryName ) {
        String version = null;
        JcrRepository repository = getRepository(repositoryName);
        if (repository != null) {
            version = repository.getDescriptor(JcrRepository.SPEC_NAME_DESC) + " "
                      + repository.getDescriptor(JcrRepository.SPEC_VERSION_DESC);
        }
        return version;
    }

    public int getQueryActivity() {
        if (!isRunning()) return 0;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        // TODO implement getQueryActivity() and define in doc what the return
        // value actually represents
        return 0;
    }

    public int getSaveActivity() {
        if (!isRunning()) return 0;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        // TODO implement getSaveActivity() and define in doc what the return
        // value actually represents
        return 0;
    }

    public Object getSessionActivity() {
        if (!isRunning()) return null;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        // TODO implement getSaveActivity() and define in doc what the return
        // value actually represents
        return 0;
    }

    /**
     * Obtains all the repository locks sorted by owner. This is a JBoss managed operation.
     * 
     * @return a list of sessions sorted by owner (never <code>null</code>)
     */
    // public List<ModeShapeLock> listLocks() {
    // return listLocks(ModeShapeLock.SORT_BY_OWNER);
    // }
    //
    // /**
    // * Obtains all the repository locks sorted by the specified sorter.
    // *
    // * @param lockSorter
    // * the lock sorter (never <code>null</code>)
    // * @return a list of locks sorted by the specified lock sorter (never
    // * <code>null</code>)
    // */
    // public List<ManagedLock> listLocks(Comparator<ManagedLock> lockSorter) {
    // CheckArg.isNotNull(lockSorter, "lockSorter");
    // if (!isRunning())
    // return Collections.emptyList();
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // // TODO implement listLocks(Comparator)
    // List<ManagedLock> locks = new ArrayList<ManagedLock>();
    //
    // // create temporary date
    // for (int i = 0; i < 5; ++i) {
    // locks.add(new ManagedLock("workspace-" + i, true, "sessionId-1",
    // new DateTime(), "id-" + i, "owner-" + i, true));
    // }
    //
    // // sort
    // Collections.sort(locks, lockSorter);
    //
    // return locks;
    // }

    /**
     * Obtains all the repository sessions sorted by user name. This is a JBoss managed operation.
     * 
     * @return a list of sessions sorted by user name (never <code>null</code>)
     */
    // public List<ManagedSession> listSessions() {
    // return listSessions(ManagedSession.SORT_BY_USER);
    // }
    //
    // /**
    // * Obtains all the repository sessions sorted by the specified sorter.
    // *
    // * @param sessionSorter
    // * the session sorter (never <code>null</code>)
    // * @return a list of locks sorted by the specified session sorter (never
    // * <code>null</code>)
    // */
    // public List<ManagedSession> listSessions(
    // Comparator<ManagedSession> sessionSorter) {
    // CheckArg.isNotNull(sessionSorter, "sessionSorter");
    // if (!isRunning())
    // return Collections.emptyList();
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // // TODO implement listSessions(Comparator)
    // List<ManagedSession> sessions = new ArrayList<ManagedSession>();
    //
    // // create temporary date
    // for (int i = 0; i < 5; ++i) {
    // sessions.add(new ManagedSession("workspace-" + i, "userName-" + i,
    // "sessionId-1", new DateTime()));
    // }
    //
    // // sort
    // Collections.sort(sessions, sessionSorter);
    //
    // return sessions;
    // }

    /**
     * Removes the lock with the specified identifier. This is a JBoss managed operation.
     * 
     * @param lockId the lock's identifier
     * @return <code>true</code> if the lock was removed
     */
    public boolean removeLock( String lockId ) {
        if (!isRunning()) return false;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        // TODO implement removeLockWithLockToken()
        return false;
    }

    /**
     * Terminates the session with the specified identifier. This is a JBoss managed operation.
     * 
     * @param sessionId the session's identifier
     * @return <code>true</code> if the session was terminated
     */
    public boolean terminateSession( String sessionId ) {
        if (!isRunning()) return false;

        // Get engine to use for the rest of the method (this is synchronized)
        // ...
        final JcrEngine engine = getEngine();
        assert engine != null;

        // TODO implement terminateSessionBySessionId()
        return false;
    }

    /*
     * Connector operations
     */

    // /**
    // * Obtains a connector by name.
    // *
    // * @param connectorName
    // * @return RepositorySource - may be <code>null</code>)
    // */
    // public RepositorySource getConnector(String connectorName) {
    // if (!isRunning())
    // return null;
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // RepositorySource repositorySource = engine
    // .getRepositorySource(connectorName);
    // assert (repositorySource != null) : "Connector '" + connectorName
    // + "' does not exist";
    // return repositorySource;
    // }
    //
    // /**
    // * Pings a connector by name.
    // *
    // * @param connectorName
    // * @return RepositorySource - may be <code>null</code>)
    // */
    // public boolean pingConnector(String connectorName) {
    // if (!isRunning())
    // return false;
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // final JcrEngine engine = getEngine();
    // assert engine != null;
    //
    // boolean success = false;
    // String pingDuration = null;
    // try {
    // RepositoryConnectionPool pool = engine.getRepositoryService()
    // .getRepositoryLibrary().getConnectionPool(connectorName);
    // if (pool != null) {
    // Stopwatch sw = new Stopwatch();
    // sw.start();
    // success = pool.ping();
    // sw.stop();
    // pingDuration = sw.getTotalDuration().toString();
    // }
    // } catch (Exception e) {
    // Logger.getLogger(getClass()).error(e,
    // JBossManagedI18n.errorDeterminingIfConnectionIsAlive,
    // connectorName);
    // }
    // if (pingDuration == null)
    // pingDuration = new Duration(0L).toString();
    // return success;
    // }

    /*
     * SequencingService operations
     */

    // /**
    // * Obtains the managed sequencing service. This is a JBoss managed operation.
    // *
    // * @return the sequencing service or <code>null</code> if never started
    // */
    // public SequencingService getSequencingService() {
    // if (!isRunning())
    // return null;
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // return getEngine().getSequencingService();
    // }
    //
    // /**
    // * Obtains the managed sequencing service. This is a JBoss managed
    // * operation.
    // *
    // * @param repositoryName
    // *
    // * @return the sequencing service or <code>null</code> if never started
    // */
    // public List<ManagedProperty> getRepositoryProperties(String repositoryName) {
    // if (!isRunning()) return null;
    //
    // List<ManagedProperty> propertyList = new ArrayList<ManagedProperty>();
    //
    // JcrRepository repository = getRepository(repositoryName);
    //
    // String[] descriptorKeys = repository.getDescriptorKeys();
    //
    // for (String key: descriptorKeys){
    // String value = repository.getDescriptor(key);
    // propertyList.add(new ManagedProperty(ManagedUtils.createLabel(key),value));
    // }
    //
    // return propertyList;
    // }
    //
    // /**
    // * Obtains the version (build number) of this ModeShape instance. This is a
    // * JBoss managed operation.
    // *
    // * @return the ModeShape version
    // */
    // public synchronized String getVersion() {
    // if (!isRunning())
    // return "";
    //
    // // Get engine to use for the rest of the method (this is synchronized)
    // // ...
    // return getEngine().getEngineVersion();
    // }

    public static class ManagedProperty {
        private static final long serialVersionUID = 1L;

        private String name;
        private String label;
        private String description;
        private String value;

        public ManagedProperty() {

        }

        public ManagedProperty( String label,
                                String currentValue ) {
            this.setLabel(label);
            this.value = currentValue;
        }

        public ManagedProperty( Property property,
                                String currentValue ) {
            this.setName(property.getName());
            this.setLabel(property.getLabel());
            this.setDescription(property.getDescription());
            this.value = currentValue;
        }

        /**
         * @param description
         */
        public void setDescription( String description ) {
            this.description = description;
        }

        /**
         * @param name Sets name to the specified value.
         */
        public void setName( String name ) {
            this.name = name;
        }

        /**
         * @return name
         */
        public String getName() {
            return name;
        }

        /**
         * @param label Sets label to the specified value.
         */
        public void setLabel( String label ) {
            this.label = label;
        }

        /**
         * @return label
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return value
         */
        public String getValue() {
            return this.value;
        }

        /**
         * @return description
         */
        public String getDescription() {
            return description;
        }

    }

}
