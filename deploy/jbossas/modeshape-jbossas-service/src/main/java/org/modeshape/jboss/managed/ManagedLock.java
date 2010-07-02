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

import java.util.Comparator;
import net.jcip.annotations.Immutable;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.joda.time.DateTime;
import org.modeshape.common.util.CheckArg;

/**
 * The <code>ManagedSession</code> class is a JBoss managed object of a ModeShape lock.
 */
@Immutable
@ManagementObject( description = "A ModeShape node lock", componentType = @ManagementComponent( type = "ModeShape", subtype = "Lock" ), properties = ManagementProperties.EXPLICIT )
public final class ManagedLock implements ModeShapeManagedObject {

    /**
     * A sorter for when locks need to be sorted by owner.
     */
    public static final Comparator<ManagedLock> SORT_BY_OWNER = new Comparator<ManagedLock>() {
        @Override
        public int compare( ManagedLock thisLock,
                            ManagedLock thatLock ) {
            return thisLock.getOwner().compareTo(thatLock.getOwner());
        }
    };

    /**
     * The JBoss managed readonly property indicating if this lock is a deep lock.
     */
    private final boolean deep;

    /**
     * The JBoss managed readonly property for the lock's expiration time. The expiration time is never <code>null</code>.
     */
    private final DateTime expiration;

    /**
     * The JBoss managed readonly property for the lock's identifier. The ID is never <code>null</code> and never empty.
     */
    private final String id;

    /**
     * The JBoss managed readonly property for the lock's session identifier. The session ID is never <code>null</code> and never
     * empty.
     */
    private final String sessionId;

    /**
     * The JBoss managed readonly property for the lock's owner. The owner is never <code>null</code> and never empty.
     */
    private final String owner;

    /**
     * The JBoss managed readonly property indicating if this lock is a session-based lock.
     */
    private final boolean sessionBased;

    /**
     * The JBoss managed readonly property for the lock's workspace name. The workspace name is never <code>null</code> and never
     * empty.
     */
    private final String workspaceName;

    /**
     * Constructs a managed object for a ModeShape lock.
     * 
     * @param workspaceName the name of the lock's workspace (never <code>null</code> and never empty)
     * @param sessionBased the flag indicating if this lock is session-based
     * @param sessionId the session ID of this lock (never <code>null</code> and never empty)
     * @param expiration the lock expiration time (never <code>null</code>)
     * @param id the lock ID (never <code>null</code> and never empty)
     * @param owner the owner of the lock (never <code>null</code> or empty)
     * @param deep the flag indicating if this is a deep lock
     */
    public ManagedLock( String workspaceName,
                        boolean sessionBased,
                        String sessionId,
                        DateTime expiration,
                        String id,
                        String owner,
                        boolean deep ) {
        CheckArg.isNotEmpty(workspaceName, "workspaceName");
        CheckArg.isNotEmpty(sessionId, "sessionId");
        CheckArg.isNotNull(expiration, "expiration");
        CheckArg.isNotEmpty(id, "id");
        CheckArg.isNotEmpty(owner, "owner");

        this.deep = deep;
        this.expiration = expiration;
        this.id = id;
        this.sessionId = sessionId;
        this.owner = owner;
        this.sessionBased = sessionBased;
        this.workspaceName = workspaceName;
    }

    /**
     * Obtains the lock's expiration time. This is a JBoss managed readonly property.
     * 
     * @return the time this lock will expire (never <code>null</code>)
     */
    @ManagementProperty( name = "Expiration Time", description = "The time this lock will expire", readOnly = true, use = ViewUse.RUNTIME )
    public DateTime getExpiration() {
        return this.expiration;
    }

    /**
     * Obtains the lock's identifier. This is a JBoss managed readonly property.
     * 
     * @return the identifier of this lock (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Lock ID", description = "The lock's unique identifier", readOnly = true, use = ViewUse.RUNTIME )
    @ManagementObjectID( prefix = "ModeShapeLock-" )
    public String getId() {
        return this.id;
    }

    /**
     * Obtains the lock's owner. This is a JBoss managed readonly property.
     * 
     * @return the owner of this lock (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Owner", description = "The owner of the lock", readOnly = true, use = ViewUse.RUNTIME )
    public String getOwner() {
        return this.owner;
    }

    /**
     * Obtains the session's unique identifier. This is a JBoss managed readonly property.
     * 
     * @return the session identifier of this lock (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Session ID", description = "The identifier of the session this lock belongs to", readOnly = true, use = ViewUse.RUNTIME )
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Obtains the lock's workspace name. This is a JBoss managed readonly property.
     * 
     * @return the workspace name of this lock (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Workspace", description = "The name of the workspace lock belongs to", readOnly = true, use = ViewUse.RUNTIME )
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * Indicates if this lock is a deep lock. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if a deep lock
     */
    @ManagementProperty( name = "Deep", description = "Indicates if this lock is a deep lock", readOnly = true, use = ViewUse.RUNTIME )
    public boolean isDeep() {
        return this.deep;
    }

    /**
     * Indicates if this lock is a session-based lock. This is a JBoss managed readonly property.
     * 
     * @return <code>true</code> if a session-based lock
     */
    @ManagementProperty( name = "Session-Based", description = "Indicates if this lock is a session-based lock", readOnly = true, use = ViewUse.RUNTIME )
    public boolean isSessionBased() {
        return this.sessionBased;
    }

}
