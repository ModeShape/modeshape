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
package org.modeshape.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Jcr Node value object.
 * 
 * @author kulikov
 */
public class JcrNode implements Serializable, IsSerializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String path;
    private String primaryType;
    
    private JcrAccessControlList acl;
    private Acl acll;
    
    private String[] mixins;
    
    //children nodes
    private ArrayList<JcrNode> children = new ArrayList<JcrNode>();
    private Collection<JcrProperty> properties;
    private String[] propertyDefs;
    
    public JcrNode() {
    }
    
    public JcrNode(String name, String path, String primaryType) {
        this.name = name;
        this.path = path;
        this.primaryType = primaryType;
    }
    
    public void setName(String name) {
        this.name = name;
    }
       
    public String getName() {
        return name;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getPrimaryType() {
        return primaryType;
    }
    
    public Collection<JcrNode> children() {
        return children;
    }
    
    public void addChild(JcrNode child) {
        children.add(child);
    }
    
    public Collection<JcrProperty> getProperties() {
        return properties;
    }
    
    public void setProperties(Collection<JcrProperty> properties) {
        this.properties = properties;
    }
    
    public void addProperty(String name, String type, String value) {
        properties.add(new JcrProperty(name, type, value));
    }
    
    public JcrAccessControlList getAccessList() {
        return acl;
    }
    
    public void setAcessControlList(JcrAccessControlList acl) {
        this.acl = acl;
    }
    
    public Acl getAcl() {
        return acll;
    }
    
    public void setAcl(Acl acll) {
        this.acll = acll;
    }
    
    public void setMixins(String[] mixins) {
        this.mixins = mixins;
    }
    
    public String[] getMixins() {
        return mixins;
    }
    
    public void setPropertyDefs(String[] propertyDefs) {
        this.propertyDefs = propertyDefs;
    }
    
    public String[] getPropertyDefs() {
        return propertyDefs;
    }
}
