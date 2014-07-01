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
 *
 * @author kulikov
 */
public class RepositoryName implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String descriptor;

    public RepositoryName() {
    }
    
    public RepositoryName(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
}
