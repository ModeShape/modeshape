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
package org.modeshape.jboss.managed;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import net.jcip.annotations.Immutable;

import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementParameter;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.joda.time.DateTime;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.common.util.Logger.Level;
import org.modeshape.common.util.Reflection.Property;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.repository.sequencer.SequencerConfig;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * A <code>ManagedEngine</code> instance is a JBoss managed object for a
 * {@link JcrEngine}.
 */
@Immutable
@ManagementObject(isRuntime = true, name = "ModeShapeEngine", description = "A ModeShape engine", componentType = @ManagementComponent(type = "ModeShape", subtype = "Engine"), properties = ManagementProperties.EXPLICIT)
public final class ManagedEngine implements ModeShapeManagedObject {

	/**
	 * The ModeShape object being managed and delegated to (never
	 * <code>null</code>).
	 */
	private JcrEngine engine;

	public static enum Component {
		CONNECTOR, SEQUENCER
	}

	public ManagedEngine() {
		this.engine = null;
	}

	/**
	 * Creates a JBoss managed object from the specified engine.
	 * 
	 * @param engine
	 *            the engine being managed (never <code>null</code>)
	 */
	public ManagedEngine(JcrEngine engine) {
		CheckArg.isNotNull(engine, "engine");
		this.engine = engine;
	}

	/**
	 * Obtains the managed connectors of this engine. This is a JBoss managed
	 * operation.
	 * 
	 * @return an unmodifiable collection of managed connectors (never
	 *         <code>null</code>)
	 */
	@ManagementOperation(description = "Obtains the managed connectors of this engine", impact = Impact.ReadOnly)
	public Collection<ManagedConnector> getConnectors() {

		Collection<ManagedConnector> connectors = new ArrayList<ManagedConnector>();

		if (isRunning()) {
			for (String repositoryName : this.engine.getRepositoryNames()) {
				RepositorySource repositorySource = this.engine
						.getRepositorySource(repositoryName);
				assert (repositorySource != null) : "Repository '"
						+ repositoryName + "' does not exist";
				ManagedConnector connector = new ManagedConnector(
						repositorySource);

				connectors.add(connector);
			}
		}

		return Collections.unmodifiableCollection(connectors);
	}

	/**
	 * Obtains the properties for the passed in object. This is a JBoss managed
	 * operation.
	 * 
	 * @param objectName
	 * @param objectType
	 * 
	 * @return an collection of managed properties (may be
	 *         <code>null</code>)
	 */
	@ManagementOperation(description = "Obtains the properties for an object", impact = Impact.ReadOnly)
	public List<ManagedProperty> getProperties(String objectName,
			Component objectType) {

		Reflection reflection = null;

		List<Property> props = null;
		List<ManagedProperty> managedProps = new ArrayList<ManagedProperty>();
		try {

			if (objectType.equals(Component.CONNECTOR)) {
				reflection = new Reflection(RepositorySource.class);
				RepositorySource repositorySource = getConnector(objectName);
				props = reflection.getAllPropertiesOn(repositorySource);
			}

		} catch (SecurityException e) {
			Logger.getLogger(getClass()).log(Level.ERROR, e,
					JBossManagedI18n.errorGettingPropertiesFromManagedObject,
					objectName);
		} catch (IllegalArgumentException e) {
			{
				Logger
						.getLogger(getClass())
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectName);
			}
		} catch (NoSuchMethodException e) {
			{
				Logger
						.getLogger(getClass())
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectName);
			}
		} catch (IllegalAccessException e) {
			{
				Logger
						.getLogger(getClass())
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectName);
			}
		} catch (InvocationTargetException e) {
			{
				Logger
						.getLogger(getClass())
						.log(
								Level.ERROR,
								e,
								JBossManagedI18n.errorGettingPropertiesFromManagedObject,
								objectName);
			}
		}

		if (props != null) {
			for (Property prop : props) {
				if (prop.getType().isPrimitive()
						|| prop.getType().toString().contains("java.lang.String")) {
					if (prop.getValue().getClass().isArray()){
						StringBuffer sb = new StringBuffer();
						String[] stringArray = (String[])prop.getValue();
						for (String cell:stringArray){
							sb.append(cell).append(" ");
						}
					prop.setValue(sb.toString());
					}
					managedProps.add(new ManagedProperty(prop));
				}
			}
		}

		return managedProps;
	}

	/**
	 * Obtains a connector by name.
	 * 
	 * @param connectorName
	 * 
	 * @return RepositorySource - may be <code>null</code>)
	 */
	public RepositorySource getConnector(String connectorName) {

		RepositorySource repositorySource = null;
		if (isRunning()) {
			repositorySource = this.engine.getRepositorySource(connectorName);
			assert (repositorySource != null) : "Connector '" + connectorName
					+ "' does not exist";
		}

		return repositorySource;
	}

	/*
	 * ManagedRepository operations
	 */

	/**
	 * Obtains the managed repositories of this engine. This is a JBoss managed
	 * operation.
	 * 
	 * @return an unmodifiable collection of repositories (never
	 *         <code>null</code>)
	 */
	@ManagementOperation(description = "Obtains the managed repositories of this engine", impact = Impact.ReadOnly)
	public Collection<ManagedRepository> getRepositories() {

		Collection<ManagedRepository> repositories = new ArrayList<ManagedRepository>();

		if (isRunning()) {
			for (String repositoryName : this.engine.getRepositoryNames()) {
				repositories.add(new ManagedRepository(repositoryName));
			}
		}

		return repositories;
	}

	/**
	 * Obtains the specified managed repository of this engine. This is called
	 * by the JNDIManagedRepositories when a JNDI lookup is performed to find a
	 * repository.
	 * 
	 * @param repositoryName
	 *            for the repository to be returned
	 * 
	 * @return a repository or <code>null</code> if repository doesn't exist
	 */
	public JcrRepository getRepository(String repositoryName) {
		JcrRepository repository = null;

		if (isRunning()) {
			try {
				repository = this.engine.getRepository(repositoryName);
			} catch (RepositoryException e) {
				Logger.getLogger(getClass()).log(Level.ERROR, e,
						JBossManagedI18n.errorGettingRepositoryFromEngine,
						repositoryName);
			}
		}

		return repository;
	}

	/**
	 * Obtains the JCR version supported by this repository. This is a JBoss
	 * managed readonly property.
	 * 
	 * @param repositoryName
	 *            (never <code>null</code>)
	 * 
	 * @return String version (never <code>null</code>)
	 */
	@ManagementOperation(description = "The JCR version supported by this repository", impact = Impact.ReadOnly)
	public String getRepositoryVersion(String repositoryName) {
		String version = null;
		JcrRepository repository = getRepository(repositoryName);
		if (repository != null) {
			version = repository.getDescriptor(Repository.SPEC_VERSION_DESC)
					+ " " + repository.getDescriptor(Repository.SPEC_NAME_DESC);
		}
		return version;
	}

	@ManagementProperty(name = "Query Activity", description = "The number of queries executed", use = ViewUse.STATISTIC)
	public int getQueryActivity() {
		// TODO implement getQueryActivity() and define in doc what the return
		// value actually represents
		return 0;
	}

	@ManagementProperty(name = "Save Activity", description = "The number of nodes saved", use = ViewUse.STATISTIC)
	public int getSaveActivity() {
		// TODO implement getSaveActivity() and define in doc what the return
		// value actually represents
		return 0;
	}

	@ManagementProperty(name = "Session Activity", description = "The number of sessions created", use = ViewUse.STATISTIC)
	public Object getSessionActivity() {
		// TODO implement getSaveActivity() and define in doc what the return
		// value actually represents
		return 0;
	}

	/**
	 * Obtains all the repository locks sorted by owner. This is a JBoss managed
	 * operation.
	 * 
	 * @return a list of sessions sorted by owner (never <code>null</code>)
	 */
	@ManagementOperation(description = "Obtains all the managed locks sorted by the owner", impact = Impact.ReadOnly)
	public List<ManagedLock> listLocks() {
		return listLocks(ManagedLock.SORT_BY_OWNER);
	}

	/**
	 * Obtains all the repository locks sorted by the specified sorter.
	 * 
	 * @param lockSorter
	 *            the lock sorter (never <code>null</code>)
	 * @return a list of locks sorted by the specified lock sorter (never
	 *         <code>null</code>)
	 */
	public List<ManagedLock> listLocks(Comparator<ManagedLock> lockSorter) {
		CheckArg.isNotNull(lockSorter, "lockSorter");

		// TODO implement listLocks(Comparator)
		List<ManagedLock> locks = new ArrayList<ManagedLock>();

		// create temporary date
		for (int i = 0; i < 5; ++i) {
			locks.add(new ManagedLock("workspace-" + i, true, "sessionId-1",
					new DateTime(), "id-" + i, "owner-" + i, true));
		}

		// sort
		Collections.sort(locks, lockSorter);

		return locks;
	}

	/**
	 * Obtains all the repository sessions sorted by user name. This is a JBoss
	 * managed operation.
	 * 
	 * @return a list of sessions sorted by user name (never <code>null</code>)
	 */
	@ManagementOperation(description = "Obtains the managed sessions sorted by user name", impact = Impact.ReadOnly)
	public List<ManagedSession> listSessions() {
		return listSessions(ManagedSession.SORT_BY_USER);
	}

	/**
	 * Obtains all the repository sessions sorted by the specified sorter.
	 * 
	 * @param sessionSorter
	 *            the session sorter (never <code>null</code>)
	 * @return a list of locks sorted by the specified session sorter (never
	 *         <code>null</code>)
	 */
	public List<ManagedSession> listSessions(
			Comparator<ManagedSession> sessionSorter) {
		CheckArg.isNotNull(sessionSorter, "sessionSorter");

		// TODO implement listSessions(Comparator)
		List<ManagedSession> sessions = new ArrayList<ManagedSession>();

		// create temporary date
		for (int i = 0; i < 5; ++i) {
			sessions.add(new ManagedSession("workspace-" + i, "userName-" + i,
					"sessionId-1", new DateTime()));
		}

		// sort
		Collections.sort(sessions, sessionSorter);

		return sessions;
	}

	/**
	 * Removes the lock with the specified identifier. This is a JBoss managed
	 * operation.
	 * 
	 * @param lockId
	 *            the lock's identifier
	 * @return <code>true</code> if the lock was removed
	 */
	@ManagementOperation(description = "Removes the lock with the specified ID", impact = Impact.WriteOnly, params = { @ManagementParameter(name = "lockId", description = "The lock identifier") })
	public boolean removeLock(String lockId) {
		// TODO implement removeLockWithLockToken()
		return false;
	}

	/**
	 * Terminates the session with the specified identifier. This is a JBoss
	 * managed operation.
	 * 
	 * @param sessionId
	 *            the session's identifier
	 * @return <code>true</code> if the session was terminated
	 */
	@ManagementOperation(description = "Terminates the session with the specified ID", impact = Impact.WriteOnly, params = { @ManagementParameter(name = "sessionId", description = "The session identifier") })
	public boolean terminateSession(String sessionId) {
		// TODO implement terminateSessionBySessionId()
		return false;
	}

	/*
	 * SequencingService operations
	 */

	/**
	 * Obtains the managed sequencing service. This is a JBoss managed
	 * operation.
	 * 
	 * @return the sequencing service or <code>null</code> if never started
	 */
	@ManagementOperation(description = "Obtains the managed sequencing service of this engine", impact = Impact.ReadOnly)
	public SequencingService getSequencingService() {
		if (isRunning()) {
			return this.engine.getSequencingService();
		}

		return null;
	}

	/**
	 * Obtains the version (build number) of this ModeShape instance. This is a
	 * JBoss managed operation.
	 * 
	 * @return the ModeShape version
	 */
	@ManagementOperation(description = "Obtains the version of this ModeShape instance", impact = Impact.ReadOnly)
	public String getVersion() {

		return this.engine.getEngineVersion();
	}

	/**
	 * Indicates if the managed engine is running. This is a JBoss managed
	 * operation.
	 * 
	 * @return <code>true</code> if the engine is running
	 */
	@ManagementOperation(description = "Indicates if this engine is running", impact = Impact.ReadOnly)
	public boolean isRunning() {
		try {
			this.engine.getRepositoryService();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	/**
	 * First {@link #shutdown() shutdowns} the engine and then {@link #start()
	 * starts} it back up again. This is a JBoss managed operation.
	 * 
	 * @see #shutdown()
	 * @see #start()
	 */
	@ManagementOperation(description = "Restarts this engine", impact = Impact.Lifecycle)
	public void restart() {
		shutdown();
		start();
	}

	/**
	 * Performs an engine shutdown. This is a JBoss managed operation.
	 * 
	 * @see JcrEngine#shutdown()
	 */
	@ManagementOperation(description = "Shutdowns this engine", impact = Impact.Lifecycle)
	public void shutdown() {
		this.engine.shutdown();
	}

	/**
	 * Starts the engine. This is a JBoss managed operation.
	 * 
	 * @see JcrEngine#start()
	 */
	@ManagementOperation(description = "Starts this engine", impact = Impact.Lifecycle)
	public void start() {
		this.engine.start();
		// force initialization and loading
		this.getRepositories();
	}

	protected JcrEngine getEngine() {
		return this.engine;
	}

	public void setEngine(JcrEngine jcrEngine) {
		this.engine = jcrEngine;
	}

	public void setConfigURL(java.net.URL configurationUrl) throws Exception {
		JcrConfiguration jcrConfig = new JcrConfiguration()
				.loadFrom(configurationUrl);
		this.engine = jcrConfig.build();
	}

	public static class ManagedProperty {
		private static final long serialVersionUID = 1L;

		private String name;
		private String label;
		private String description;
		private String value;

		public ManagedProperty() {

		}

		public ManagedProperty(Property property) {
			this.setName(property.getName());
			this.setLabel(property.getLabel());
			this.setDescription(property.getDescription());
			this.setValue(property.getValue().toString());
		}

		/**
		 * @param description
		 */
		public void setDescription(String description) {
			this.description = description;
		}

		/**
		 * @param name
		 *            Sets name to the specified value.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param label
		 *            Sets label to the specified value.
		 */
		public void setLabel(String label) {
			this.label = label;
		}

		/**
		 * @return label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * @param value
		 *            Sets value to the specified value.
		 */
		public void setValue(String value) {
			this.value = value;
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
