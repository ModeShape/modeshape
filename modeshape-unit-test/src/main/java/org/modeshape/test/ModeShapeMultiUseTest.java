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

import org.modeshape.jcr.MultiUseAbstractTest;

/**
 * A base class for ModeShape integration tests that set up a single ModeShape engine once for all unit tests. Subclasses should
 * provide static startup and teardown methods such as:
 * 
 * <pre>
 * &#064;BeforeClass
 * public static void beforeAll() throws Exception {
 *     startRepository();
 * }
 * 
 * &#064;AfterClass
 * public static void afterAll() throws Exception {
 *     stopRepository();
 * }
 * </pre>
 */
public abstract class ModeShapeMultiUseTest extends MultiUseAbstractTest {
}
