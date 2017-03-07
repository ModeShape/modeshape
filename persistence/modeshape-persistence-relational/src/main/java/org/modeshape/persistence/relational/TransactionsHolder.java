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
package org.modeshape.persistence.relational;

import java.util.Optional;

/**
 * Class that manages the information about the current thread-bound transaction.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class TransactionsHolder {
    /**
     * Thread local holder for the current active transaction id.
     */
    private static final ThreadLocal<String> ACTIVE_TX_ID = new ThreadLocal<>();

    private TransactionsHolder() {
    }
    
    protected static boolean hasActiveTransaction() {
        return activeTransaction() != null;
    }
    
    /**
     * Requires that an active transaction exists for the current calling thread.
     *
     * @return the ID of the active transaction, never {@code null}
     * @throws RelationalProviderException if the current thread is not associated with a transaction.
     */
    protected static String requireActiveTransaction() {
        return Optional.ofNullable(ACTIVE_TX_ID.get()).orElseThrow(() -> new RelationalProviderException(
                RelationalProviderI18n.threadNotAssociatedWithTransaction,
                Thread.currentThread().getName()));
    }
   
    protected static void setActiveTxId(String txId) {
        ACTIVE_TX_ID.set(txId);
    }
    
    protected static String activeTransaction() {
        return ACTIVE_TX_ID.get();
    }
    
    protected static void clearActiveTransaction() {
        ACTIVE_TX_ID.remove();        
    }
}
