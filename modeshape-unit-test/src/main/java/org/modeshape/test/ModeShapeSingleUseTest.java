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
package org.modeshape.test;

import org.junit.Before;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.SingleUseAbstractTest;

/**
 * A base class for ModeShape integration tests that set up a new ModeShape engine for each unit test.
 * <p>
 * There are two ways to run tests with this class:
 * <ol>
 * <li>All tests runs against a fresh repository created from the same configuration. In this case, the
 * {@link #startRepositoryAutomatically} variable should be set to true, and the
 * {@link #createRepositoryConfiguration(String)} should be overridden if a non-default configuration is to be used
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
