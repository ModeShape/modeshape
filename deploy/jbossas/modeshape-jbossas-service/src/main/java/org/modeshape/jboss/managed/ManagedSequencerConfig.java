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

import net.jcip.annotations.Immutable;

import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.modeshape.common.util.CheckArg;
import org.modeshape.repository.sequencer.SequencerConfig;

/**
 * A <code>ManagedSequncerConfig</code> instance is a JBoss managed object for a
 * {@link SequencerConfig}.
 */
@Immutable
@ManagementObject
public final class ManagedSequencerConfig implements ModeShapeManagedObject {

	private String name;
	private String description;

	public ManagedSequencerConfig() {
		this.setName(name);
		this.setDescription(description);
	}

	/**
	 * Creates a JBoss managed object from the specified engine.
	 *
	 * @param name 
	 * @param description 
	 * 
	 */
	public ManagedSequencerConfig(String name, String description) {
		CheckArg.isNotNull(name, "name");
		CheckArg.isNotNull(description, "description");
		this.setName(name);
		this.setDescription(description);
	}

	/**
     * Name of the sequencer. This is a JBoss managed readonly property.
     * 
     * @return name The name of this sequencer
     */
    @ManagementProperty( name = "Sequencer name", description = "The name of this sequencer", readOnly = true, use = ViewUse.CONFIGURATION )
    public String getName() {
        return this.name;
    }
    
    /**
     * Description of the sequencer. This is a JBoss managed readonly property.
     * 
     * @return name The description of this sequencer
     */
    @ManagementProperty( name = "Sequencer description", description = "The description of this sequencer", readOnly = true, use = ViewUse.CONFIGURATION )
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Set the name of the sequencer.
     * @param name 
     * 
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Set the description of the sequencer. 
     * @param description 
     * 
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
