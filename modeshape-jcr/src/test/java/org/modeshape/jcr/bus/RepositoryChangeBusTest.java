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

package org.modeshape.jcr.bus;

import java.util.concurrent.Executors;
import org.junit.Ignore;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Unit test for {@link org.modeshape.jcr.bus.RepositoryChangeBus}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Ignore("Uses the deprecated bus")
public class RepositoryChangeBusTest extends AbstractChangeBusTest {

    @Override
    protected ChangeBus createRepositoryChangeBus() throws Exception {
        return new RepositoryChangeBus(Executors.newCachedThreadPool(), RepositoryConfiguration.SYSTEM_WORKSPACE_NAME);
    }
}
