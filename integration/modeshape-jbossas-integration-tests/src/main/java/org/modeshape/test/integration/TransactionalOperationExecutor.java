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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.modeshape.jcr.JcrRepository;

/**
 * SLSB which execute a {@link org.modeshape.test.integration.RepositoryOperation} in a separate transaction
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @param <V> the return type of the {@link org.modeshape.test.integration.RepositoryOperation}
 */
@Stateless
public class TransactionalOperationExecutor<V> {

    /**
     * Execute the given operation in a new transaction.
     *
     * @param repository a {@link org.modeshape.jcr.JcrRepository} instance; never {@code null}
     * @param operation a {@link org.modeshape.test.integration.RepositoryOperation} instance; never {@code null}
     * @return the result of the operation {@link org.modeshape.test.integration.RepositoryOperation#execute(org.modeshape.jcr.JcrRepository)}
     * @throws Exception if anything unexpected fails
     */
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    public V execute(final JcrRepository repository, final RepositoryOperation<V> operation ) throws Exception {
        return operation.execute(repository);
    }
}
