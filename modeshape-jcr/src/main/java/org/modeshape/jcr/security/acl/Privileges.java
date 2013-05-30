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
import javax.jcr.security.Privilege;
import org.modeshape.jcr.ModeShapePermissions;

/**
 * Collection of supported privileges.
 * 
 * This class groups privileges objects declared by JCR 2.0 spec and provides 
 * several utility methods.
 * 
 * @author kulikov
 */
public class Privileges {
    
    /**
     * Privilege objects defined by JCR spec.
     */ 
    public static final PrivilegeImpl ADD_CHILD_NODES = new PrivilegeImpl(Privilege.JCR_ADD_CHILD_NODES, new Privilege[]{});
    public static final PrivilegeImpl LIFECYCLE_MANAGEMENT = new PrivilegeImpl(Privilege.JCR_LIFECYCLE_MANAGEMENT, new Privilege[]{});
    public static final PrivilegeImpl LOCK_MANAGEMENT = new PrivilegeImpl(Privilege.JCR_LOCK_MANAGEMENT, new Privilege[]{});
    public static final PrivilegeImpl MODIFY_ACCESS_CONTROL = new PrivilegeImpl(Privilege.JCR_MODIFY_ACCESS_CONTROL, new Privilege[]{});
    public static final PrivilegeImpl MODIFY_PROPERTIES = new PrivilegeImpl(Privilege.JCR_MODIFY_PROPERTIES, new Privilege[]{});
    public static final PrivilegeImpl NODE_TYPE_MANAGEMENT = new PrivilegeImpl(Privilege.JCR_NODE_TYPE_MANAGEMENT, new Privilege[]{});
    public static final PrivilegeImpl READ = new PrivilegeImpl(Privilege.JCR_READ, new Privilege[]{});
    public static final PrivilegeImpl READ_ACCESS_CONTROL = new PrivilegeImpl(Privilege.JCR_READ_ACCESS_CONTROL, new Privilege[]{});
    public static final PrivilegeImpl REMOVE_CHILD_NODES = new PrivilegeImpl(Privilege.JCR_REMOVE_CHILD_NODES, new Privilege[]{});
    public static final PrivilegeImpl REMOVE_NODE = new PrivilegeImpl(Privilege.JCR_REMOVE_NODE, new Privilege[]{});
    public static final PrivilegeImpl RETENTION_MANAGEMENT = new PrivilegeImpl(Privilege.JCR_RETENTION_MANAGEMENT, new Privilege[]{});
    public static final PrivilegeImpl VERSION_MANAGEMENT = new PrivilegeImpl(Privilege.JCR_VERSION_MANAGEMENT, new Privilege[]{});
    public static final PrivilegeImpl WRITE = new PrivilegeImpl(Privilege.JCR_WRITE, 
            new Privilege[]{
                MODIFY_PROPERTIES, 
                ADD_CHILD_NODES, 
                REMOVE_NODE, 
                REMOVE_CHILD_NODES
            });
    public static final PrivilegeImpl ALL = new PrivilegeImpl(Privilege.JCR_ALL, 
            new Privilege[]{
                READ,
                WRITE, 
                READ_ACCESS_CONTROL,
                MODIFY_ACCESS_CONTROL,
                LOCK_MANAGEMENT,
                VERSION_MANAGEMENT,
                RETENTION_MANAGEMENT
            });
    
    /**
     * Map between privilege's JCR names and privilege objects.
     * 
     * Following to the specification, each Privilege is identified by JCR name.
     * This map allows to find privilege object using its name.
     */
    private static final HashMap<String, PrivilegeImpl> privileges = new HashMap();
    static {
        privileges.put(ADD_CHILD_NODES.getName(), ADD_CHILD_NODES);
        privileges.put(ALL.getName(), ALL);
        privileges.put(LIFECYCLE_MANAGEMENT.getName(), LIFECYCLE_MANAGEMENT);
        privileges.put(MODIFY_ACCESS_CONTROL.getName(), MODIFY_ACCESS_CONTROL);
        privileges.put(MODIFY_PROPERTIES.getName(), MODIFY_PROPERTIES);
        privileges.put(NODE_TYPE_MANAGEMENT.getName(), NODE_TYPE_MANAGEMENT);
        privileges.put(READ.getName(), READ);
        privileges.put(READ_ACCESS_CONTROL.getName(), READ_ACCESS_CONTROL);
        privileges.put(REMOVE_CHILD_NODES.getName(), REMOVE_CHILD_NODES);
        privileges.put(REMOVE_NODE.getName(), REMOVE_NODE);
        privileges.put(RETENTION_MANAGEMENT.getName(), RETENTION_MANAGEMENT);
        privileges.put(VERSION_MANAGEMENT.getName(), VERSION_MANAGEMENT);
        privileges.put(WRITE.getName(), WRITE);
    }
    
    /**
     * Map between actions names used by Modeshape internally and 
     * privilege object.     * 
     */
    private final static HashMap<String, PrivilegeImpl> actions = new HashMap();
    static {
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
    public static Privilege[] listOfSupported() {
        Privilege[] res = new Privilege[privileges.values().size()];
        privileges.values().toArray(res);
        return res;
    }
    
    /**
     * Searches privilege object for the privilege with the given name.
     * 
     * @param name the name of privilege to find.
     * @return the privilege object or null if not found.
     */
    public static PrivilegeImpl forName(String name) {
        return privileges.get(name);
    }
    
    /**
     * Searches JCR defined privilege object which belongs to the given 
     * modeshape action.
     * 
     * @param action the name of the modeshape action
     * @return privilege object for the given action
     */
    public static PrivilegeImpl forAction(String action) {
        return actions.get(action);
    }
}
