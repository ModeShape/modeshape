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

import org.junit.Test;

/**
 * We don't test a lot of functionality here. It's the same provider as tested in the {@link LocalIndexProviderTest}, except that
 * in these tests we want to simply verify that the asynchronous indexing is working. (These asynchronous tests take a lot longer
 * since we have to wait an unknown amount of time after saving changes before we can issue the query.)
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 * 
 * @see LocalIndexProviderTest
 */
public class LocalIndexProviderAsynchronousTest extends AbstractIndexProviderTest {

    @Override
    protected boolean useSynchronousIndexes() {
        return false;
    }
    
    @Override
    protected String providerName() {
        return "local";
    }

    // ---------------------------------------------------------------
    // Override these so that we can easily run them via JUnit runner.
    // ---------------------------------------------------------------

    @Override
    @Test
    public void shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn() throws Exception {
        super.shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn();
    }

    @Override
    @Test
    public void shouldUseSingleColumnStringIndexInQueryAgainstSameNodeType() throws Exception {
        super.shouldUseSingleColumnStringIndexInQueryAgainstSameNodeType();
    }

    @Override
    public void shouldSkipEntireBatches() throws Exception {
        super.shouldSkipEntireBatches();
    }
}
