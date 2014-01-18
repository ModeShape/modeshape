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

package org.modeshape.jcr;

import org.infinispan.configuration.cache.ConfigurationBuilder;

/**
 * Interface which should be implemented by unit tests that require a custom ISPN cache loader configuration and which
 * use the {@link TestingEnvironment} together with the repository configuration.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface CustomLoaderTest {

    public void applyLoaderConfiguration(ConfigurationBuilder configurationBuilder);
}
