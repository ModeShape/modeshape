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

import java.io.Serializable;

/**
 * Node property object descriptor.
 * 
 * @author kulikov
 */
public class JcrProperty implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private String value;
    private boolean isProtected;
    private boolean isMultiValue;

    public JcrProperty() {
    }

    /**
     * Creates new property object.
     * 
     * @param name the name of the property.
     * @param type text description for the type
     * @param value text view of the value.
     */
    public JcrProperty( String name,
                        String type,
                        String value ) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /**
     * Gets name of the property.
     * 
     * @return name of the property.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets property type.
     * 
     * @return type of the property.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets value of the property.
     * 
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Modifies name of the property.
     * 
     * @param name the new name for the property.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Modifies type of the property.
     * 
     * @param type the new type of the property.
     */
    public void setType( String type ) {
        this.type = type;
    }

    /**
     * Modifies value of the property.
     * 
     * @param value
     */
    public void setValue( String value ) {
        this.value = value;
    }

    public boolean isMultiValue() {
        return this.isMultiValue;
    }

    /**
     * Marks property as multiple or single value.
     * 
     * @param isMultiValue true if property has multiple value
     */
    public void setMultiValue( boolean isMultiValue ) {
        this.isMultiValue = isMultiValue;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    /**
     * Marks property either protected or not.
     * 
     * @param isProtected true if property is protected false otherwise.
     */
    public void setProtected( boolean isProtected ) {
        this.isProtected = isProtected;
    }
}
