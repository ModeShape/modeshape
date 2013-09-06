/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author kulikov
 */
public class JcrNode implements Serializable, IsSerializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String path;
    private String primaryType;
    
    //children nodes
    private ArrayList<JcrNode> children = new ArrayList();
    private Collection<JcrProperty> properties;
    
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
}
