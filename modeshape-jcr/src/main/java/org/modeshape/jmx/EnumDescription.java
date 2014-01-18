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

package org.modeshape.jmx;

/**
 * Value holder used by the JMX MBean to expose information about enums.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class EnumDescription {

    private final String name;
    private final String description;

    /**
     * @param name the name of the enum (as result of {@link Enum#name()}
     * @param description an additional description for the enum.
     */
    public EnumDescription( String name,
                            String description ) {
        this.name = name;
        this.description = description;
    }

    /**
     * @return the enum name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the enum description which better explains the enum
     */
    public String getDescription() {
        return description;
    }
}
