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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.RepositoryException;

import net.jcip.annotations.Immutable;

import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Logger.Level;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;

/**
 * A <code>ManagedEngine</code> instance is a JBoss managed object for a {@link JcrEngine}.
 */
@Immutable
@ManagementObject( name = "ModeShapeEngine", description = "A ModeShape engine", componentType = @ManagementComponent( type = "Modeshape", subtype = "Engine" ), properties = ManagementProperties.EXPLICIT )
public final class ManagedEngine implements ModeShapeManagedObject {
    
    private String configFile = null;

    public ManagedEngine() {
        this.engine = null;
    }

    /**
     * The ModeShape object being managed and delegated to (never <code>null</code>).
     */
    private JcrEngine engine;

    /**
     * The managed object of the sequencing service of the engine.
     */
    private ManagedSequencingService sequencingService;
    
    /**
     * Creates a JBoss managed object from the specified engine.
     * 
     * @param engine the engine being managed (never <code>null</code>)
     */
    public ManagedEngine( JcrEngine engine ) {
        CheckArg.isNotNull(engine, "engine");
        this.engine = engine;
    }

    /**
     * Obtains the managed connectors of this engine. This is a JBoss managed operation.
     * 
     * @return an unmodifiable collection of managed connectors (never <code>null</code>)
     */
    @ManagementOperation( description = "Obtains the managed connectors of this engine", impact = Impact.ReadOnly )
    public Collection<ManagedConnector> getConnectors() {

        Collection<ManagedConnector> connectors = new ArrayList<ManagedConnector>();

        if (isRunning()) {
            for (String repositoryName : this.engine.getRepositoryNames()) {
                RepositorySource repositorySource = this.engine.getRepositorySource(repositoryName);
                assert (repositorySource != null) : "Repository '" + repositoryName + "' does not exist";
                connectors.add(new ManagedConnector(repositorySource));
            }
        }

        return Collections.unmodifiableCollection(connectors);
    }

    /**
     * Obtains the managed repositories of this engine. This is a JBoss managed operation.
     * 
     * @return an unmodifiable collection of repositories (never <code>null</code>)
     */
    @ManagementOperation( description = "Obtains the managed repositories of this engine", impact = Impact.ReadOnly )
    public Collection<ManagedRepository> getRepositories() {

        Collection<ManagedRepository> repositories = new ArrayList<ManagedRepository>();

        if (isRunning()) {
            for (String repositoryName : this.engine.getRepositoryNames()) {
                try {
                    JcrRepository repository = this.engine.getRepository(repositoryName);
                    repositories.add(new ManagedRepository(repository));
                } catch (RepositoryException e) {
                    Logger.getLogger(getClass()).log(Level.ERROR,
                                                     e,
                                                     JBossManagedI18n.errorGettingRepositoryFromEngine,
                                                     repositoryName);
                }
            }
        }

        return Collections.unmodifiableCollection(repositories);
    }
    
    /**
     * Obtains the specified managed repository of this engine. This is called by the JNDIManagedRepositories when a
     * JNID lookup is performed to find a repository.
     * @param repositoryName for the repository to be returned
     * 
     * @return a repository or <code>null</code> if repository doesn't exist
     */
     public ManagedRepository getRepository(String repositoryName) {
        if (isRunning()) {
                try {
                   JcrRepository repository = this.engine.getRepository(repositoryName);
                   if (repository != null) {
                       return (new ManagedRepository(repository));
                   }
                   
                } catch (RepositoryException e) {
                    Logger.getLogger(getClass()).log(Level.ERROR,
                                                     e,
                                                     JBossManagedI18n.errorGettingRepositoryFromEngine,
                                                     repositoryName);
                }
        }

        return null;
    }

    /**
     * Obtains the managed sequencing service. This is a JBoss managed operation.
     * 
     * @return the sequencing service or <code>null</code> if never started
     */
    @ManagementOperation( description = "Obtains the managed sequencing service of this engine", impact = Impact.ReadOnly )
    public ManagedSequencingService getSequencingService() {
        if (isRunning()) {
            if (this.sequencingService == null) {
                this.sequencingService = new ManagedSequencingService(this.engine.getSequencingService());
            }
        } else {
            this.sequencingService = null;
        }

        return this.sequencingService;
    }

    /**
     * Indicates if the managed engine is running. This is a JBoss managed operation.
     * 
     * @return <code>true</code> if the engine is running
     */
    @ManagementOperation( description = "Indicates if this engine is running", impact = Impact.ReadOnly )
    public boolean isRunning() {
        try {
            this.engine.getRepositoryService();
            return true;
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
     */
    @ManagementOperation( description = "Restarts this engine", impact = Impact.Lifecycle )
    public void restart() {
        shutdown();
        start();
    }

    /**
     * Performs an engine shutdown. This is a JBoss managed operation.
     * 
     * @see JcrEngine#shutdown()
     */
    @ManagementOperation( description = "Shutdowns this engine", impact = Impact.Lifecycle )
    public void shutdown() {
        this.engine.shutdown();
    }

    /**
     * Starts the engine. This is a JBoss managed operation.
     * 
     * @see JcrEngine#start()
     */
    @ManagementOperation( description = "Starts this engine", impact = Impact.Lifecycle )
    public void start() {
        this.engine.start();
    }

    protected JcrEngine getEngine() {
	return this.engine;
    }
    
    public void setEngine(JcrEngine jcrEngine) {
	this.engine = jcrEngine;
    }
    
    public void setConfigFile(String configurationFile) throws Exception {
	this.configFile = configurationFile;
	JcrConfiguration jcrConfig = new JcrConfiguration().loadFrom(this.configFile);
	this.engine = jcrConfig.build();
	this.start();
    }
}
