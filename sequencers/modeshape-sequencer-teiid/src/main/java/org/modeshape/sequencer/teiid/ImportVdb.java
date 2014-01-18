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

import org.modeshape.common.util.CheckArg;

/**
 * A simple POJO that represents a VDB that is imported by another VDB. It is read in from a VDB manifest ("vdb.xml").
 */
public class ImportVdb implements Comparable<ImportVdb> {

    private boolean importDataPolicies = true;
    private final String name;
    private final int version;

    /**
     * @param name the name of the import VDB (cannot be <code>null</code> or empty)
     * @param version the import VDB's version (must be greater than zero)
     */
    public ImportVdb( final String name,
                      final int version ) {
        CheckArg.isNotNull(name, "name");
        CheckArg.isGreaterThan(version, 0, "version");
        this.name = name;
        this.version = version;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final ImportVdb that ) {
        CheckArg.isNotNull(that, "that");

        if (this == that) {
            return 0;
        }

        // order by name first then by version if necessary
        final int result = this.name.compareTo(that.name);

        if (result == 0) {
            return (this.version - that.version);
        }

        return result;
    }

    /**
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return version the imported VDB's version (always greater than zero)
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * @return <code>true</code> if the data policies of the imported VDB should be used
     */
    public boolean isImportDataPolicies() {
        return this.importDataPolicies;
    }

    /**
     * @param newImportDataPolicies the new setting for if the data policies should be used
     */
    public void setImportDataPolicies( final boolean newImportDataPolicies ) {
        this.importDataPolicies = newImportDataPolicies;
    }

}
