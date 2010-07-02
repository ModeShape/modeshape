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
 * The <code>ManagedSession</code> class is a JBoss managed object of a ModeShape session.
 */
@Immutable
@ManagementObject( description = "A ModeShape session", componentType = @ManagementComponent( type = "ModeShape", subtype = "Session" ), properties = ManagementProperties.EXPLICIT )
public final class ManagedSession implements ModeShapeManagedObject {

    /**
     * A sorter for when sessions need to be sorted by user name.
     */
    public static final Comparator<ManagedSession> SORT_BY_USER = new Comparator<ManagedSession>() {
        @Override
        public int compare( ManagedSession thisSession,
                            ManagedSession thatSession ) {
            return thisSession.getUserName().compareTo(thatSession.getUserName());
        }
    };

    /**
     * The JBoss managed readonly property for the session's identifier. The ID is never <code>null</code> and never empty.
     */
    private final String id;

    /**
     * The JBoss managed readonly property for the session's creation time. The time is never <code>null</code>.
     */
    private final DateTime timeCreated;

    /**
     * The JBoss managed readonly property for the session's user name. The user name is never <code>null</code> and never empty.
     */
    private final String userName;

    /**
     * The JBoss managed readonly property for the session's workspace name. The workspace name is never <code>null</code> and
     * never empty.
     */
    private final String workspaceName;

    /**
     * Constructs a managed object for a ModeShape session.
     * 
     * @param workspaceName the session's workspace name (never <code>null</code> and never empty)
     * @param userName the user name of the session (never <code>null</code> and never empty)
     * @param id the session's identifier (never <code>null</code> and never empty)
     * @param timeCreated the session's time of creation (never <code>null</code>)
     */
    public ManagedSession( String workspaceName,
                           String userName,
                           String id,
                           DateTime timeCreated ) {
        CheckArg.isNotEmpty(workspaceName, "workspaceName");
        CheckArg.isNotEmpty(userName, "userName");
        CheckArg.isNotEmpty(id, "id");
        CheckArg.isNotNull(timeCreated, "timeCreated");

        this.id = id;
        this.timeCreated = timeCreated;
        this.userName = userName;
        this.workspaceName = workspaceName;
    }

    /**
     * Obtains the session's unique identifier. This is a JBoss managed readonly property.
     * 
     * @return the identifier of this session (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Session ID", description = "The session's unique identifier", readOnly = true, use = ViewUse.RUNTIME )
    @ManagementObjectID( prefix = "ModeShapeSession-" )
    public String getId() {
        return this.id;
    }

    /**
     * Obtains the session's creation time. This is a JBoss managed readonly property.
     * 
     * @return the time this session was created (never <code>null</code>)
     */
    @ManagementProperty( name = "Time Created", description = "The time this session was created", readOnly = true, use = ViewUse.RUNTIME )
    public DateTime getTimeCreated() {
        return this.timeCreated;
    }

    /**
     * Obtains the session's user name. This is a JBoss managed readonly property.
     * 
     * @return the user name of this session (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "User Name", description = "The name of the session's user", readOnly = true, use = ViewUse.RUNTIME )
    public String getUserName() {
        return this.userName;
    }

    /**
     * Obtains the session's workspace name. This is a JBoss managed readonly property.
     * 
     * @return the workspace name of this session (never <code>null</code> and never empty)
     */
    @ManagementProperty( name = "Workspace Name", description = "The name of the workspace this session belongs to", readOnly = true, use = ViewUse.RUNTIME )
    public String getWorkspaceName() {
        return this.workspaceName;
    }

}
