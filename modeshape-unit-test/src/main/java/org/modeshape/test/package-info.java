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
 * This ModeShape module also brings in (as "compile" scope) all JARs that are required to run ModeShape
 * </p>
 */

package org.modeshape.test;

