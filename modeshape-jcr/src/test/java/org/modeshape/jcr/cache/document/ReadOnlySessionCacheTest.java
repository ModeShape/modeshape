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
package org.modeshape.jcr.cache.document;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.RepositoryEnvironment;
import org.modeshape.jcr.cache.SessionCache;

/**
 * Tests that operate against a {@link ReadOnlySessionCache}.
 */
public class ReadOnlySessionCacheTest extends AbstractSessionCacheTest {

    @Override
    protected SessionCache createSessionCache( ExecutionContext context,
                                               WorkspaceCache cache,
                                               RepositoryEnvironment repositoryEnvironment ) {
        return new ReadOnlySessionCache(context, workspaceCache, repositoryEnvironment);
    }
}
