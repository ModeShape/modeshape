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
package org.modeshape.test.integration;

import org.modeshape.jcr.JcrRepository;

/**
 * Abstract operation executed server-side against a repository.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @param <V> the return type of the operation
 */
public interface RepositoryOperation<V> {

    /**
     * Execute the operation against the given repository.
     * @param repository a {@link org.modeshape.jcr.JcrRepository} instance, never {@code null}
     * @return a result
     * @throws Exception if anything unexpected fails
     */
    public V execute(JcrRepository repository) throws Exception;
}
