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

package org.modeshape.test.integration;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.modeshape.jdbc.JcrHttpDriverIntegrationTest;

/**
 * Extension of the {@link JcrHttpDriverIntegrationTest} which runs the same tests, via Arquillian, in an AS7 container with the
 * ModeShape distribution kit deployed.
 *
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class )
public class JdbcDriverIntegrationTest extends JcrHttpDriverIntegrationTest {

    @Override
    protected String getWorkspaceName() {
        //must match the default config from standalone.xml
        return "default";
    }

    @Override
    protected String getUserName() {
        //must be a valid user name from the ModeShape AS security domain
        return "admin";
    }

    @Override
    protected String getRepositoryName() {
        //must match the default config from standalone.xml
        return "sample";
    }

    @Override
    protected String getPassword() {
        //must be a password for the above user, from the ModeShape AS security domain
        return "admin";
    }

    @Override
    protected String getContextPathUrl() {
        //this should be the context of the web application deployed inside AS7
        return"localhost:8080/modeshape-rest";
    }
}
