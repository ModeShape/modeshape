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
package org.modeshape.connector.store.jpa.model.basic;

import org.modeshape.connector.store.jpa.JpaConnectorNoCreateWorkspaceTest;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.connector.store.jpa.TestEnvironment;
import org.modeshape.graph.connector.RepositorySource;

public class BasicNoCreateWorkspaceTest extends JpaConnectorNoCreateWorkspaceTest {

    @Override
    protected RepositorySource setUpSource() {
        predefinedWorkspaces = new String[] {"workspace1", "workspace1a"};

        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);
        source.setModel(JpaSource.Models.BASIC.getName());

        // Override the inherited properties, since that's the focus of these tests ...
        source.setCreatingWorkspacesAllowed(true);
        source.setPredefinedWorkspaceNames(predefinedWorkspaces);

        return source;
    }
}
