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
/**
 * This package provides a set of abstract test classes that can be used as base classes for your own JUnit tests that use
 * a repository.
 * <p>
 * Extend the {@link org.modeshape.test.ModeShapeMultiUseTest} if a single repository instance be created and initialized,
 * and all of the test methods are to run against that same repository instance. This works well when you need a lot of tests
 * that operate against a repository and do not change the content in that repository, since the overhead of starting a repository
 * is incurred only once (even if there are hundreds of tests). Note that this can be used in cases where the tests modify
 * the content, but be sure that each test cleans up the content it created at the end of the test (or each test works on 
 * a specific and independent area of the repository).
 * </p>
 * <p>
 * Alternatively, extend the {@link org.modeshape.test.ModeShapeSingleUseTest} if each test method should have a fresh, newly
 * created repository instance. This works well when your tests are frequently modifying content, since each test doesn't
 * have to worry about cleaning up the content. A ModeShape repository generally starts very quickly, but this may add up
 * for many dozens or hundreds of tests.
 * </p>
 * <p>
 * This ModeShape module also brings in (as "compile" scope) all JARs that are required to run ModeShape and Infinispan,
 * including the more popular Infinispan Cache Loader libraries and the H2 (in-memory or file-based) database.
 * </p>
 */

package org.modeshape.test;

