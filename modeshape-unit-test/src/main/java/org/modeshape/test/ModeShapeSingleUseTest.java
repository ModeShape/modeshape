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

import org.junit.Before;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;

/**
 * A base class for ModeShape integration tests that set up a new ModeShape engine for each unit test.
 * <p>
 * There are two ways to run tests with this class:
 * <ol>
 * <li>All tests runs against a fresh repository created from the same configuration. In this case, the
 * {@link #startRepositoryAutomatically} variable should be set to true, and the
 * {@link #createRepositoryConfiguration(String, Environment)} should be overridden if a non-default configuration is to be used
 * for all the tests.</li>
 * <li>Each test requires a fresh repository with a different configuration. In this case, the
 * {@link #startRepositoryAutomatically} variable should be set to <code>false</code>, and each test should then call one of the
 * {@link #startRepositoryWithConfiguration(RepositoryConfiguration)} methods before using the repository.</li>
 * </ol>
 * </p>
 * <p>
 * Classes can define a custom unit test {@link Before startup method}, but should always first call the {@link #beforeEach()
 * super.beforeEach()} method. Similarly, a custom unit test teardown method can be defined, but should always first call the
 * {@link #afterEach() super.afterEach()} method.
 * </p>
 */
public abstract class ModeShapeSingleUseTest extends SingleUseAbstractTest {

}
