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
    private String[] mixins;
    
    //children nodes
    private ArrayList<JcrNode> children = new ArrayList();
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
    
    public Collection getProperties() {
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
