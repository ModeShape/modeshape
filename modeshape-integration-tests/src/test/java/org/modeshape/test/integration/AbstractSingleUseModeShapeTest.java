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

import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.JcrTools;

/**
 * A base class for ModeShape integration tests that set up a new ModeShape engine for each unit test.
 */
public abstract class AbstractSingleUseModeShapeTest extends AbstractModeShapeTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        print = false;
        startEngine(getClass(), getResourcePathToConfigurationFile(), getRepositoryName());
        session = repository.login();
        tools = new JcrTools();
    }

    @Override
    @After
    public void afterEach() throws Exception {

        try {
            if (session != null) {
                session.logout();
            }
        } finally {
            session = null;
            stopEngine();
        }
    }

    /**
     * Return the path to the configuration file that is available on the classpath of this class. Because this class' classloader
     * is used, the resulting path should not include a leading '/'.
     * 
     * @return the configuration file path; may not be null
     */
    protected abstract String getResourcePathToConfigurationFile();

    /**
     * Return the name of the repository defined in the {@link #getResourcePathToConfigurationFile() configuration file}.
     * 
     * @return the repository name; may not be null
     */
    protected abstract String getRepositoryName();
}
