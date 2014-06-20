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
 * Value object representing types;
 * 
 * @author kulikov
 */
public class JcrNodeType implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private boolean isPrimary;
    private boolean isMixin;
    private boolean isAbstract;
    
    public JcrNodeType() {
    }

    public String getName() {
        return name;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean isMixin() {
        return isMixin;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public void setMixin(boolean isMixin) {
        this.isMixin = isMixin;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }
    
    
}
