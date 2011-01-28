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
package org.modeshape.test;

import org.junit.After;
import org.junit.Before;

/**
 * A base class for ModeShape integration tests that set up a new ModeShape engine for each unit test. There are three ways to use
 * this base class.
 * <ol>
 * <li>All unit test methods use the same ModeShape configuration. In this case, override the
 * {@link #getPathToDefaultConfiguration()} method to return the path to the ModeShape configuration file, and this configuration
 * will be loaded and the engine automatically started {@link #beforeEach() before each} test is run.</li>
 * <li>Each unit test uses a different ModeShape configuration. In this case, each test case should
 * {@link #startEngineUsing(String) manually start the engine} by supplying the path to the appropriate configuration file.</li>
 * <li>Some tests using a default configuration, and other tests using different configurations. In this case, override the
 * {@link #getPathToDefaultConfiguration()} method to return the path to the ModeShape configuration file. In those test methods
 * that should use the default configuration, just {@link #sessionTo(String, String, javax.jcr.Credentials) create sessions} or
 * {@link #engine() get the engine} as needed, and the default configuration will be used. In those tests wanting to use a
 * different configuration, first {@link #startEngineUsing(String) manually start the engine} by supplying the path to the
 * appropriate configuration file.</li>
 * </ol>
 * <p>
 * Classes can define a custom unit test startup method, but it should always first call the {@link #beforeEach()
 * super.beforeEach()} method. Similarly, a custom unit test teardown method can be defined, but should always first call the
 * {@link #afterEach() super.afterEach()} method.
 * </p>
 */
public abstract class ModeShapeSingleUseTest extends ModeShapeUnitTest {

    @Override
    @Before
    public void beforeEach() throws Exception {
        configuration = null;
        stopEngine();
        super.beforeEach();
    }

    @Override
    @After
    public void afterEach() throws Exception {
        try {
            super.afterEach();
            stopEngine();
        } finally {
            configuration = null;
        }
    }
}
