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

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;

/**
 * A simple POJO that is used to represent the information for a data role read in from a VDB manifest ("vdb.xml").
 */
public class VdbDataRole implements Comparable<VdbDataRole> {

    private final String name;
    private boolean anyAuthenticated;
    private boolean allowCreateTempTables;
    private String description;
    private final List<Permission> permissions = new ArrayList<VdbDataRole.Permission>();
    private final List<String> roleNames = new ArrayList<String>();

    /**
     * @param name the data role name (cannot be <code>null</code> or empty)
     */
    public VdbDataRole( final String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final VdbDataRole that ) {
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
        return this.description;
    }

    /**
     * @return the mapped data role names
     */
    public List<String> getMappedRoleNames() {
        return this.roleNames;
    }

    /**
     * @return the data role name (never <code>null</code> or empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the data role permissions (never <code>null</code> but can be empty)
     */
    public List<Permission> getPermissions() {
        return this.permissions;
    }

    /**
     * @return <code>true</code> if data role can create temp tables
     */
    public boolean isAllowCreateTempTables() {
        return this.allowCreateTempTables;
    }

    /**
     * @return <code>true</code> if data role has any authenticated
     */
    public boolean isAnyAuthenticated() {
        return this.anyAuthenticated;
    }

    /**
     * @param newValue the new value for allowCreateTempTables
     */
    public void setAllowCreateTempTables( final boolean newValue ) {
        this.allowCreateTempTables = newValue;
    }

    /**
     * @param newValue the new value for anyAuthenticated
     */
    public void setAnyAuthenticated( final boolean newValue ) {
        this.anyAuthenticated = newValue;
    }

    /**
     * @param newValue the new description value (can be <code>null</code> or empty)
     */
    public void setDescription( final String newValue ) {
        this.description = StringUtil.isBlank(newValue) ? "" : newValue;
    }

    /**
     * A simple POJO that is used to represent one data role permission found in the VDB manifest ("vdb.xml").
     */
    public class Permission {

        private boolean alter;
        private boolean create;
        private boolean delete;
        private boolean execute;
        private boolean read;
        private final String resourceName;
        private boolean update;

        /**
         * @param resourceName the resource name associated with the permission (cannot be <code>null</code> or empty)
         */
        public Permission( final String resourceName ) {
            CheckArg.isNotEmpty(resourceName, "resourceName");
            this.resourceName = resourceName;
        }

        /**
         * @param newValue the new allow-alter value
         */
        public void allowAlter( final boolean newValue ) {
            this.alter = newValue;
        }

        /**
         * @param newValue the new allow-create value
         */
        public void allowCreate( final boolean newValue ) {
            this.create = newValue;
        }

        /**
         * @param newValue the new allow-delete value
         */
        public void allowDelete( final boolean newValue ) {
            this.delete = newValue;
        }

        /**
         * @param newValue the new allow-execute value
         */
        public void allowExecute( final boolean newValue ) {
            this.execute = newValue;
        }

        /**
         * @param newValue the new allow-read value
         */
        public void allowRead( final boolean newValue ) {
            this.read = newValue;
        }

        /**
         * @param newValue the new allow-update value
         */
        public void allowUpdate( final boolean newValue ) {
            this.update = newValue;
        }

        /**
         * @return <code>true</code> if the permission can alter
         */
        public boolean canAlter() {
            return this.alter;
        }

        /**
         * @return <code>true</code> if the permission can create
         */
        public boolean canCreate() {
            return this.create;
        }

        /**
         * @return <code>true</code> if the permission can delete
         */
        public boolean canDelete() {
            return this.delete;
        }

        /**
         * @return <code>true</code> if the permission can execute
         */
        public boolean canExecute() {
            return this.execute;
        }

        /**
         * @return <code>true</code> if the permission can read
         */
        public boolean canRead() {
            return this.read;
        }

        /**
         * @return <code>true</code> if the permission can update
         */
        public boolean canUpdate() {
            return this.update;
        }

        /**
         * @return the resource name associated with the permission (never <code>null</code> or empty)
         */
        public String getResourceName() {
            return this.resourceName;
        }
    }
}
