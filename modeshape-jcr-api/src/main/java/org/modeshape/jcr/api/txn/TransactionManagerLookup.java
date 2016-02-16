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
package org.modeshape.jcr.api.txn;

import javax.transaction.TransactionManager;

/**
 * Factory interface which is used by ModeShape to obtain a reference to an existing transaction manager. Since ModeShape cannot
 * be used without transactions, the runtime environment must have a {@link javax.transaction.TransactionManager} instance which
 * is returned by an implementation of this class and which ModeShape can access. 
 * <p>
 * ModeShape provides an out-of-the-box implementation which integrates with most containers and JTA providers and which also
 * falls back to a default, in-memory implementation. It may happen that this is not enough though, in which case clients should
 * provide their own {@link TransactionManagerLookup} implementation and configure it in the repository:
 * <pre>
 *   {
        "storage" : {
            "transactionManagerLookup" : "org.modeshape.custom.CustomTransactionManagerLookup"
         }
 *   } 
 * </pre>
 * </p>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface TransactionManagerLookup {
  
    /**
     * Searches for a transaction manager instance. 
     * 
     * @return a {@link TransactionManager} instance; never {@code null}
     */
    TransactionManager getTransactionManager();
}
