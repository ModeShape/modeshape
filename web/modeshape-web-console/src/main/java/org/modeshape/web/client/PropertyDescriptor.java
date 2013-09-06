/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import java.io.Serializable;

/**
 *
 * @author kulikov
 */
public class PropertyDescriptor implements Serializable {
    private String name;
    private String type;
    private String value;
    
    public PropertyDescriptor() {
    }

    public PropertyDescriptor(String name, String type, String value) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    
}
