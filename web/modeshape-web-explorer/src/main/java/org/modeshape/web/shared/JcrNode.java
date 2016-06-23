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
 * Jcr node value object.
 * 
 * @author kulikov
 */
public class JcrNode implements Serializable, IsSerializable {
    private static final long serialVersionUID = 1L;
    
    private String repository;
    private String workspace;
    private String name;
    private String path;
    private String primaryType;
    private long childCount;
    
    private Acl acll;
    
    private String[] mixins;
    
    //children nodes
    private ArrayList<JcrNode> children = new ArrayList<JcrNode>();
    private Collection<JcrProperty> properties;
    private String[] propertyDefs;
    
    public JcrNode() {
    }
    
    public JcrNode(String repository, String workspace, String name, String path, String primaryType) {
        this.repository = repository;
        this.workspace = workspace;
        this.name = name;
        this.path = path;
        this.primaryType = primaryType;
    }
    
    public String getRepository() {
        return repository;
    }
    
    public String getWorkspace() {
        return workspace;
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
    
    public boolean hasBinaryContent() {
        for (JcrProperty p : properties) {
            if (p.isBinary()) {
                return true;
            }
        }
        return false;
    }
    
    public void setPropertyDefs(String[] propertyDefs) {
        this.propertyDefs = propertyDefs;
    }
    
    public String[] getPropertyDefs() {
        return propertyDefs;
    }
    
    public long getChildCount() {
        return childCount;
    }
    
    public void setChildCount(long childCount) {
        this.childCount = childCount;
    }
}
