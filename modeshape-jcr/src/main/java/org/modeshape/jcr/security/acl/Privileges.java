/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr.security.acl;

import java.util.HashMap;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.ModeShapePermissions;

/**
 * Collection of supported privileges.
 * 
 * Internal helper class that groups privileges objects declared by JCR 2.0 spec and provides 
 * several utility methods.
 * 
 * @author kulikov
 */
public class Privileges {
    /**
     * Privilege objects defined by JCR spec.
     */ 
    public PrivilegeImpl ADD_CHILD_NODES;
    public PrivilegeImpl LIFECYCLE_MANAGEMENT;
    public PrivilegeImpl LOCK_MANAGEMENT;
    public PrivilegeImpl MODIFY_ACCESS_CONTROL;
    public PrivilegeImpl MODIFY_PROPERTIES;
    public PrivilegeImpl NODE_TYPE_MANAGEMENT;
    public PrivilegeImpl READ;
    public PrivilegeImpl READ_ACCESS_CONTROL;
    public PrivilegeImpl REMOVE_CHILD_NODES;
    public PrivilegeImpl REMOVE_NODE;
    public PrivilegeImpl RETENTION_MANAGEMENT;
    public PrivilegeImpl VERSION_MANAGEMENT;
    public PrivilegeImpl WRITE;
    public PrivilegeImpl ALL;

    /**
     * Map between privilege's JCR names and privilege objects.
     * 
     * Following to the specification, each Privilege is identified by JCR name.
     * This map allows to find privilege object using its name.
     */
    private final HashMap<String, PrivilegeImpl> privileges = new HashMap();

    /**
     * List of applicable privileges.
     */
    private final PrivilegeImpl[] applicablePrivileges;

    /**
     * Map between actions names used by Modeshape internally and 
     * privilege object.     * 
     */
    private final HashMap<String, PrivilegeImpl> actions = new HashMap();
    
    /**
     * Creates privileges related to the given session.
     * 
     * @param session the jcr session.
     */
    public Privileges(JcrSession session) {
        ADD_CHILD_NODES = new PrivilegeImpl(session, Privilege.JCR_ADD_CHILD_NODES, new Privilege[]{});
        LIFECYCLE_MANAGEMENT = new PrivilegeImpl(session, Privilege.JCR_LIFECYCLE_MANAGEMENT, new Privilege[]{});
        LOCK_MANAGEMENT = new PrivilegeImpl(session, Privilege.JCR_LOCK_MANAGEMENT, new Privilege[]{});
        MODIFY_ACCESS_CONTROL = new PrivilegeImpl(session, Privilege.JCR_MODIFY_ACCESS_CONTROL, new Privilege[]{});
        MODIFY_PROPERTIES = new PrivilegeImpl(session, Privilege.JCR_MODIFY_PROPERTIES, new Privilege[]{});
        NODE_TYPE_MANAGEMENT = new PrivilegeImpl(session, Privilege.JCR_NODE_TYPE_MANAGEMENT, new Privilege[]{});
        READ = new PrivilegeImpl(session, Privilege.JCR_READ, new Privilege[]{});
        READ_ACCESS_CONTROL = new PrivilegeImpl(session, Privilege.JCR_READ_ACCESS_CONTROL, new Privilege[]{});
        REMOVE_CHILD_NODES = new PrivilegeImpl(session, Privilege.JCR_REMOVE_CHILD_NODES, new Privilege[]{});
        REMOVE_NODE = new PrivilegeImpl(session, Privilege.JCR_REMOVE_NODE, new Privilege[]{});
        RETENTION_MANAGEMENT = new PrivilegeImpl(session, Privilege.JCR_RETENTION_MANAGEMENT, new Privilege[]{});
        VERSION_MANAGEMENT = new PrivilegeImpl(session, Privilege.JCR_VERSION_MANAGEMENT, new Privilege[]{});
        WRITE = new PrivilegeImpl(session, Privilege.JCR_WRITE,
                new Privilege[]{
            MODIFY_PROPERTIES,
            ADD_CHILD_NODES,
            REMOVE_NODE,
            REMOVE_CHILD_NODES
        });
        ALL = new PrivilegeImpl(session, Privilege.JCR_ALL,
                new Privilege[]{
            READ,
            WRITE,
            READ_ACCESS_CONTROL,
            MODIFY_ACCESS_CONTROL,
            LOCK_MANAGEMENT,
            LIFECYCLE_MANAGEMENT,
            VERSION_MANAGEMENT,
            NODE_TYPE_MANAGEMENT,
            RETENTION_MANAGEMENT
        });


        privileges.clear();
        
        if (session.isReadOnly()) {
            applicablePrivileges = new PrivilegeImpl[] {
                READ, READ_ACCESS_CONTROL
            };
        } else {
            applicablePrivileges = new PrivilegeImpl[] {
                ALL, 
                ADD_CHILD_NODES, 
                LIFECYCLE_MANAGEMENT,
                LOCK_MANAGEMENT,
                MODIFY_ACCESS_CONTROL,
                MODIFY_PROPERTIES, 
                NODE_TYPE_MANAGEMENT, 
                REMOVE_CHILD_NODES,
                REMOVE_NODE, 
                RETENTION_MANAGEMENT, 
                VERSION_MANAGEMENT, 
                WRITE,
                READ, 
                READ_ACCESS_CONTROL
            };
        }
        
        privileges.put(ADD_CHILD_NODES.localName(), ADD_CHILD_NODES);
        privileges.put(ALL.localName(), ALL);
        privileges.put(LIFECYCLE_MANAGEMENT.localName(), LIFECYCLE_MANAGEMENT);
        privileges.put(LOCK_MANAGEMENT.localName(), LOCK_MANAGEMENT);
        privileges.put(MODIFY_ACCESS_CONTROL.localName(), MODIFY_ACCESS_CONTROL);
        privileges.put(MODIFY_PROPERTIES.localName(), MODIFY_PROPERTIES);
        privileges.put(NODE_TYPE_MANAGEMENT.localName(), NODE_TYPE_MANAGEMENT);
        privileges.put(REMOVE_CHILD_NODES.localName(), REMOVE_CHILD_NODES);
        privileges.put(REMOVE_NODE.localName(), REMOVE_NODE);
        privileges.put(RETENTION_MANAGEMENT.localName(), RETENTION_MANAGEMENT);
        privileges.put(VERSION_MANAGEMENT.localName(), VERSION_MANAGEMENT);
        privileges.put(WRITE.localName(), WRITE);
        privileges.put(READ.localName(), READ);
        privileges.put(READ_ACCESS_CONTROL.localName(), READ_ACCESS_CONTROL);
        
        actions.clear();
        actions.put(ModeShapePermissions.ADD_NODE, ADD_CHILD_NODES);
        actions.put(ModeShapePermissions.MODIFY_ACCESS_CONTROL, MODIFY_ACCESS_CONTROL);
        actions.put(ModeShapePermissions.SET_PROPERTY, MODIFY_PROPERTIES);
        actions.put(ModeShapePermissions.REGISTER_TYPE, NODE_TYPE_MANAGEMENT);
        actions.put(ModeShapePermissions.READ, READ);
        actions.put(ModeShapePermissions.READ_ACCESS_CONTROL, READ_ACCESS_CONTROL);
        actions.put(ModeShapePermissions.REMOVE_CHILD_NODES, REMOVE_CHILD_NODES);
        actions.put(ModeShapePermissions.REMOVE, REMOVE_NODE);
    }
    
    /**
     * Supported privileges.
     * 
     * @return list of implemented privilege objects.
     */
    public Privilege[] listOfSupported() {
        return this.applicablePrivileges;
    }
    
    /**
     * Searches privilege object for the privilege with the given name.
     * 
     * @param name the name of privilege to find.
     * @return the privilege object or null if not found.
     */
    public PrivilegeImpl forName(String name) throws RepositoryException {
        if (name.contains("}")) {
            String localName = name.substring(name.indexOf('}') + 1);
            PrivilegeImpl p = privileges.get(localName);
            return privileges.get(localName);
        }
        
        if (name.contains(":")) {
            String localName = name.substring(name.indexOf(':') + 1);
            PrivilegeImpl p = privileges.get(localName);
            return p.getName().equals(name) ? p : null;
        }

        return null;
    }
    
    /**
     * Searches JCR defined privilege object which belongs to the given 
     * modeshape action.
     * 
     * @param action the name of the modeshape action
     * @return privilege object for the given action
     */
    public PrivilegeImpl forAction(String action) {
        return actions.get(action);
    }
    
}
