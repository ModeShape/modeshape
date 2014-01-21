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
package org.modeshape.sequencer.teiid;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.util.CheckArg;

/**
 * A simple POJO that is used to represent the information for an overridden translator read in from a VDB manifest ("vdb.xml").
 */
public class VdbTranslator implements Comparable<VdbTranslator> {

    private String description;
    private final String name;
    private final String type;
    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * @param name the translator override name (cannot be <code>null</code> or empty)
     * @param type the translator type being overridden (cannot be <code>null</code> or empty)
     */
    public VdbTranslator( final String name,
                          final String type ) {
        CheckArg.isNotEmpty(name, "name");
        CheckArg.isNotEmpty(type, "type");

        this.name = name;
        this.type = type;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final VdbTranslator that ) {
        CheckArg.isNotNull(that, "that");

        if (this == that) {
            return 0;
        }

        // order by name
        return this.name.compareTo(that.name);
    }

    /**
     * @return the description (never <code>null</code> but can be empty)
     */
    public String getDescription() {
        return ((this.description == null) ? "" : this.description);
    }

    /**
     * @return the translator override name (never <code>null</code> or empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the overridden properties (never <code>null</code>)
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * @return the type of translator being overridden (never <code>null</code> or empty)
     */
    public String getType() {
        return this.type;
    }

    /**
     * @param newValue the new description value (can be <code>null</code> or empty)
     */
    public void setDescription( final String newValue ) {
        this.description = newValue;
    }
}
