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
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author kulikov
 */
public class JcrRepositoryDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<Param> info = new ArrayList<Param>();

    public JcrRepositoryDescriptor() {
    }

    public void add( String name,
                     String value ) {
        info.add(new Param(name, value));
    }

    public Collection<Param> info() {
        return info;
    }

}
