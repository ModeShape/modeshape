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
 * A simple POJO that is used to represent the information for a VDB entry read in from a VDB manifest ("vdb.xml").
 */
public class VdbEntry implements Comparable<VdbEntry> {

    private String description;
    private final String path;
    private final Map<String, String> properties = new HashMap<String, String>();

    /**
     * @param path the path associated with the entry (cannot be <code>null</code> or empty)
     */
    public VdbEntry( final String path ) {
        CheckArg.isNotNull(path, "path");
        this.path = path;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final VdbEntry that ) {
        CheckArg.isNotNull(that, "that");

        if (this == that) {
            return 0;
        }

        // order by path
        return this.path.compareTo(that.path);
    }

    /**
     * @return the description (never <code>null</code> but can be empty)
     */
    public String getDescription() {
        return ((this.description == null) ? "" : this.description);
    }

    /**
     * @return the entry path (never <code>null</code> or empty)
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return the overridden properties (never <code>null</code>)
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * @param newValue the new description value (can be <code>null</code> or empty)
     */
    public void setDescription( final String newValue ) {
        this.description = newValue;
    }
}
