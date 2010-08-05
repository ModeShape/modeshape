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

import java.util.List;

import net.jcip.annotations.Immutable;

import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.modeshape.common.util.CheckArg;
import org.modeshape.repository.sequencer.Sequencer;
import org.modeshape.repository.sequencer.SequencerConfig;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * A <code>ManagedSequencingService</code> instance is a JBoss managed object for a {@link SequencingService sequencing service}.
 */
@Immutable
@ManagementObject( isRuntime=true, name = "ModeShapeSequencingService", description = "A ModeShape sequencing service", componentType = @ManagementComponent( type = "ModeShape", subtype = "SequencingService" ), properties = ManagementProperties.EXPLICIT )
public class ManagedSequencingService implements ModeShapeManagedObject {

 	/**
     * The ModeShape ManagedEngine (never <code>null</code>).
     */
    private ManagedEngine engine;
    
    public ManagedSequencingService() {
        this.sequencingService = null;
    }

    /****************************** above for temporary testing only ***********************************************************/

    /**
     * The ModeShape object being managed and delegated to (never <code>null</code>).
     */
    private final SequencingService sequencingService;

    /**
     * Creates a JBoss managed object for the specified sequencing service.
     * @param managedEngine 
     */
    public ManagedSequencingService( ManagedEngine managedEngine ) {
        CheckArg.isNotNull(managedEngine, "managedEngine");
        this.sequencingService = managedEngine.getSequencingService();
    }

    @ManagementProperty( name = "Job Activity", description = "The number of sequencing jobs executed", readOnly = true, use = ViewUse.STATISTIC )
    public int getJobActivity() {
        // TODO implement getJobActivity()
        return 0;
    }

    /**
     * The number of nodes that have been sequenced. This is a JBoss managed readonly property.
     * 
     * @return the count of sequenced nodes
     */
    @ManagementProperty( name = "Nodes Sequenced", description = "The number of nodes that have been sequenced", readOnly = true, use = ViewUse.STATISTIC )
    public long getNodesSequencedCount() {
        return this.sequencingService.getStatistics().getNumberOfNodesSequenced();
    }

    /**
     * The number of nodes that have been skipped. This is a JBoss managed readonly property.
     * 
     * @return the count of skipped nodes
     */
    @ManagementProperty( name = "Nodes Skipped", description = "The number of nodes skipped (not sequenced)", readOnly = true, use = ViewUse.STATISTIC )
    public long getNodesSkippedCount() {
        return this.sequencingService.getStatistics().getNumberOfNodesSkipped();
    }

    /**
     * he sequencers currently deployed. This is a JBoss managed readonly property.
     * @return List of <code>Sequencers</code>
     * 
     */
    @ManagementOperation( name = "Sequencers", description = "The sequencers currently deployed", impact=Impact.ReadOnly )
    public List<SequencerConfig> getSequencers() {
        return this.sequencingService.getSequencers();
    }
    
    @ManagementProperty( name = "Queued Jobs", description = "The number of queued jobs", readOnly = true, use = ViewUse.STATISTIC )
    public int getQueuedJobCount() {
        return 0;
    }

    /**
     * Obtains the time the counting of nodes being sequenced and skipped began. This is a JBoss managed readonly property.
     * 
     * @return the time the sequencing statistics began
     */
    @ManagementProperty( name = "Start Time", description = "The time the sequencing statistics (sequenced, skipped) began", readOnly = true, use = ViewUse.RUNTIME )
    public long getStartTime() {
        // TODO this should return localized stringified version of time
        return this.sequencingService.getStatistics().getStartTime();
    }

    /**
     * Obtains a current list of queued sequencing jobs. This is a JBoss managed operation.
     * 
     * @return the jobs (never <code>null</code>)
     */
    @ManagementOperation( description = "Obtains the list of queued managed jobs", impact = Impact.ReadOnly )
    public Object listQueuedJobs() {
        // TODO implement listQueuedJobs()
        return null;
    }
    
    public void setManagedEngine(ManagedEngine managedEngine) throws Exception {   	
    	this.setEngine(managedEngine);
    }

	/**
	 * @param engine Sets engine to the specified value.
	 */
	public void setEngine(ManagedEngine engine) {
		this.engine = engine;
	}

	/**
	 * @return engine
	 */
	public ManagedEngine getEngine() {
		return engine;
	}

}
