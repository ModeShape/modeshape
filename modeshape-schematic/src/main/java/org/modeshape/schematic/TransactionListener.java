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
package org.modeshape.schematic;

/**
 * Notification interface which is used by ModeShape to tell a 3rd party (typically a {@link SchematicDb}) when a transaction has 
 * been started, committed or rolled back from ModeShape's perspective.
 * <p>
 * Since ModeShape supports both user-managed transactions and internal transactions, the notification methods from this interface
 * may be called in different contexts. For example, for a ModeShape-managed transaction the {@link #txStarted(String)} method
 * will be called immediately after ModeShape starts a new transaction and the {@link #txCommitted(String)} or 
 * {@link #txRolledback(String)} methods right after ModeShape has committed or rolled back the transaction.
 * </p>
 * <p>
 * On the other hand, for a non ModeShape-managed transaction the {@link #txStarted(String)} method will be called once ModeShape
 * detects an active external transaction (which may've been created a while back) while the {@link #txCommitted(String)} 
 * and {@link #txRolledback(String)} methods will be called once the external transaction notifies ModeShape via a transaction
 * synchronization.
 * </p> 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface TransactionListener {
    
    void txStarted(String id);
    void txCommitted(String id);
    void txRolledback(String id);
}
