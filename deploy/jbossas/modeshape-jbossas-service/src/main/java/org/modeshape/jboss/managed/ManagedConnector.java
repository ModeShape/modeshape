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

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.connector.RepositorySource;

/**
 * A <code>ManagedConnector</code> instance is a JBoss managed object for a {@link RepositorySource repository source}.
 */
@ManagementObject( description = "A ModeShape connector (repository source)", componentType = @ManagementComponent( type = "ModeShape", subtype = "Connector" ), properties = ManagementProperties.EXPLICIT )
public class ManagedConnector implements ModeShapeManagedObject {

    // TODO get rid of this constructor
    protected ManagedConnector() {
        this.connector = null;
    }

    /****************************** above for temporary testing only ***********************************************************/

    // TODO find other properties using reflection and make them readonly properties

    /**
     * The ModeShape object being managed and delegated to (never <code>null</code>).
     */
    private final RepositorySource connector;
    
    /**
     * Creates a JBoss managed object for the specified repository source.
     * 
     * @param connector the repository source being managed (never <code>null</code>)
     */
    public ManagedConnector( RepositorySource connector ) {
        CheckArg.isNotNull(connector, "connector");
        this.connector = connector;
    }

    /**
     * Obtains the maximum number of retries to perform on an operation. This is a JBoss managed writable property.
     * 
     * @return the number of retries
     */
    @ManagementProperty( name = "Retry Limit", description = "The maximum number of retries to perform on an operation", readOnly = false, use = ViewUse.RUNTIME )
    public int getRetryLimit() {
        return this.connector.getRetryLimit();
    }

    /**
     * Obtains the name of the repository source. This is a JBoss managed readonly property.
     * 
     * @return the name of the connector
     */
    @ManagementProperty( name = "Connector Name", description = "The name of the repository source", readOnly = true, use = ViewUse.CONFIGURATION )
    @ManagementObjectID(prefix="ModeShape-")
    public String getName() {
        return this.connector.getName();
    }

    /**
     * Indicates if the repository source supports creating workspaces. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if created workspaces is supported
     */
    @ManagementProperty( name = "Supports Creating Workspaces", description = "Indicates if creating workspaces is allowed", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingCreatingWorkspaces() {
        return this.connector.getCapabilities().supportsCreatingWorkspaces();
    }

    /**
     * Indicates if the repository source supports publishing change events. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if events are supported
     */
    @ManagementProperty( name = "Supports Events", description = "Indicates if the publishing of changes events is supported", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingEvents() {
        return this.connector.getCapabilities().supportsEvents();
    }

    /**
     * Indicates if the repository source supports creating locks. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if locking is supported
     */
    @ManagementProperty( name = "Supports Locks", description = "Indicates if locks can be created", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingLocks() {
        return this.connector.getCapabilities().supportsLocks();
    }

    /**
     * Indicates if the repository source supports queries. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if queries are supported
     */
    @ManagementProperty( name = "Supports Queries", description = "Indicates if queries are supported", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingQueries() {
        return this.connector.getCapabilities().supportsQueries();
    }

    /**
     * Indicates if the repository source supports references by identifiers. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if references are supported
     */
    @ManagementProperty( name = "Supports References", description = "Indicates if references by identifiers are supported", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingReferences() {
        return this.connector.getCapabilities().supportsReferences();
    }

    /**
     * Indicates if the repository source supports siblings being able to have the same name. This is a JBoss managed readonly
     * property.
     * 
     * @return <code>true</code> if same name siblings are supported
     */
    @ManagementProperty( name = "Supports Same Name Siblings", description = "Indicates if siblings can have the same name", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingSameNameSiblings() {
        return this.connector.getCapabilities().supportsSameNameSiblings();
    }

    /**
     * Indicates if the repository source supports full-text searches. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if searches are supported
     */
    @ManagementProperty( name = "Supports Searches", description = "Indicates if full-text searches are supported", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingSearches() {
        return this.connector.getCapabilities().supportsSearches();
    }

    /**
     * Indicates if the repository source supports updates. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if updates are supported
     */
    @ManagementProperty( name = "Supports Updates", description = "Indicates if updates can be made to the repository source", readOnly = true, use = ViewUse.CONFIGURATION )
    public boolean isSupportingUpdates() {
        return this.connector.getCapabilities().supportsUpdates();
    }

    /**
     * Sets the maximum number of retries to perform on an operation. This is a JBoss managed property.
     * 
     * @param limit the new retry limit (must be non-negative)
     */
    public void setRetryLimit( int limit ) {
        CheckArg.isNonNegative(limit, "limit");
        this.connector.setRetryLimit(limit);
    }
    
}
