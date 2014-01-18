/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.security.acl;

import java.util.HashMap;
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
    private PrivilegeImpl addChildNodes;
    private PrivilegeImpl lifeCycleManagement;
    private PrivilegeImpl lockManagement;
    private PrivilegeImpl modifyAccessControl;
    private PrivilegeImpl modifyProperties;
    private PrivilegeImpl nodeTypeManagement;
    private PrivilegeImpl read;
    private PrivilegeImpl readAccessControl;
    private PrivilegeImpl removeChildNodes;
    private PrivilegeImpl removeNode;
    private PrivilegeImpl retentionManagement;
    private PrivilegeImpl versionManagement;
    private PrivilegeImpl write;
    private PrivilegeImpl all;

    /**
     * Map between privilege's JCR names and privilege objects.
     * 
     * Following to the specification, each Privilege is identified by JCR name.
     * This map allows to find privilege object using its name.
     */
    private final HashMap<String, PrivilegeImpl> privileges = new HashMap<String,PrivilegeImpl>();

    /**
     * List of applicable privileges.
     */
    private final PrivilegeImpl[] applicablePrivileges;

    /**
     * Map between actions names used by Modeshape internally and 
     * privilege object.     * 
     */
    private final HashMap<String, PrivilegeImpl> actions = new HashMap<String,PrivilegeImpl>();
    
    /**
     * Creates privileges related to the given session.
     * 
     * @param session the jcr session.
     */
    public Privileges(JcrSession session) {
        addChildNodes = new PrivilegeImpl(session, Privilege.JCR_ADD_CHILD_NODES, new Privilege[]{});
        lifeCycleManagement = new PrivilegeImpl(session, Privilege.JCR_LIFECYCLE_MANAGEMENT, new Privilege[]{});
        lockManagement = new PrivilegeImpl(session, Privilege.JCR_LOCK_MANAGEMENT, new Privilege[]{});
        modifyAccessControl = new PrivilegeImpl(session, Privilege.JCR_MODIFY_ACCESS_CONTROL, new Privilege[]{});
        modifyProperties = new PrivilegeImpl(session, Privilege.JCR_MODIFY_PROPERTIES, new Privilege[]{});
        nodeTypeManagement = new PrivilegeImpl(session, Privilege.JCR_NODE_TYPE_MANAGEMENT, new Privilege[]{});
        read = new PrivilegeImpl(session, Privilege.JCR_READ, new Privilege[]{});
        readAccessControl = new PrivilegeImpl(session, Privilege.JCR_READ_ACCESS_CONTROL, new Privilege[]{});
        removeChildNodes = new PrivilegeImpl(session, Privilege.JCR_REMOVE_CHILD_NODES, new Privilege[]{});
        removeNode = new PrivilegeImpl(session, Privilege.JCR_REMOVE_NODE, new Privilege[]{});
        retentionManagement = new PrivilegeImpl(session, Privilege.JCR_RETENTION_MANAGEMENT, new Privilege[]{});
        versionManagement = new PrivilegeImpl(session, Privilege.JCR_VERSION_MANAGEMENT, new Privilege[]{});
        write = new PrivilegeImpl(session, Privilege.JCR_WRITE,
                new Privilege[]{
            modifyProperties,
            addChildNodes,
            removeNode,
            removeChildNodes
        });
        all = new PrivilegeImpl(session, Privilege.JCR_ALL,
                new Privilege[]{
            read,
            write,
            readAccessControl,
            modifyAccessControl,
            lockManagement,
            lifeCycleManagement,
            versionManagement,
            nodeTypeManagement,
            retentionManagement
        });


        privileges.clear();
        
        if (session.isReadOnly()) {
            applicablePrivileges = new PrivilegeImpl[] {
                read, readAccessControl
            };
        } else {
            applicablePrivileges = new PrivilegeImpl[] {
                all, 
                addChildNodes, 
                lifeCycleManagement,
                lockManagement,
                modifyAccessControl,
                modifyProperties, 
                nodeTypeManagement, 
                removeChildNodes,
                removeNode, 
                retentionManagement, 
                versionManagement, 
                write,
                read, 
                readAccessControl
            };
        }
        
        privileges.put(addChildNodes.localName(), addChildNodes);
        privileges.put(all.localName(), all);
        privileges.put(lifeCycleManagement.localName(), lifeCycleManagement);
        privileges.put(lockManagement.localName(), lockManagement);
        privileges.put(modifyAccessControl.localName(), modifyAccessControl);
        privileges.put(modifyProperties.localName(), modifyProperties);
        privileges.put(nodeTypeManagement.localName(), nodeTypeManagement);
        privileges.put(removeChildNodes.localName(), removeChildNodes);
        privileges.put(removeNode.localName(), removeNode);
        privileges.put(retentionManagement.localName(), retentionManagement);
        privileges.put(versionManagement.localName(), versionManagement);
        privileges.put(write.localName(), write);
        privileges.put(read.localName(), read);
        privileges.put(readAccessControl.localName(), readAccessControl);
        
        actions.clear();
        actions.put(ModeShapePermissions.ADD_NODE, addChildNodes);
        actions.put(ModeShapePermissions.MODIFY_ACCESS_CONTROL, modifyAccessControl);
        actions.put(ModeShapePermissions.SET_PROPERTY, modifyProperties);
        actions.put(ModeShapePermissions.REGISTER_TYPE, nodeTypeManagement);
        actions.put(ModeShapePermissions.READ, read);
        actions.put(ModeShapePermissions.READ_ACCESS_CONTROL, readAccessControl);
        actions.put(ModeShapePermissions.REMOVE_CHILD_NODES, removeChildNodes);
        actions.put(ModeShapePermissions.REMOVE, removeNode);
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
    public PrivilegeImpl forName(String name) {
        if (name.contains("}")) {
            String localName = name.substring(name.indexOf('}') + 1);
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
