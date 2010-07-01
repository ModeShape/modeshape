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
package org.modeshape.jboss.managed.temp;

import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.jboss.managed.ManagedConnector;

/**
 * 
 */
public class TempManagedConnector extends ManagedConnector {

    private final RepositorySourceCapabilities capabilities;
    private final String name;
    private final boolean pingResult;
    private int retryLimit = 0;

    public TempManagedConnector( int id ) {
        this.name = "Connector-" + id;
        this.capabilities = new RepositorySourceCapabilities((id % 2 == 0), (id % 3 == 0), (id % 4 == 0), (id % 5 == 0),
                                                             (id % 2 == 0), (id % 3 == 0), (id % 4 == 0), (id % 5 == 0));
        this.pingResult = (id % 2 == 0);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getRetryLimit() {
        return this.retryLimit;
    }

    @Override
    public boolean isSupportingCreatingWorkspaces() {
        return this.capabilities.supportsCreatingWorkspaces();
    }

    @Override
    public boolean isSupportingEvents() {
        return this.capabilities.supportsEvents();
    }

    @Override
    public boolean isSupportingLocks() {
        return this.capabilities.supportsLocks();
    }

    @Override
    public boolean isSupportingQueries() {
        return this.capabilities.supportsQueries();
    }

    @Override
    public boolean isSupportingReferences() {
        return this.capabilities.supportsReferences();
    }

    @Override
    public boolean isSupportingSameNameSiblings() {
        return this.capabilities.supportsSameNameSiblings();
    }

    @Override
    public boolean isSupportingSearches() {
        return this.capabilities.supportsSearches();
    }

    @Override
    public boolean isSupportingUpdates() {
        return this.capabilities.supportsUpdates();
    }

    @Override
    public boolean ping() {
        return this.pingResult;
    }

    @Override
    public void setRetryLimit( int limit ) {
        this.retryLimit = limit;
    }

}
