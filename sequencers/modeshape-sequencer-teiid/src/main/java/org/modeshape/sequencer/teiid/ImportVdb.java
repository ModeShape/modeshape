/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
