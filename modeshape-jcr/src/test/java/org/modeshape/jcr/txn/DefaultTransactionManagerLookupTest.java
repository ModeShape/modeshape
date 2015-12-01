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
package org.modeshape.jcr.txn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;

/**
 * Unit test for {@link DefaultTransactionManagerLookup}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DefaultTransactionManagerLookupTest {
    
    private TransactionManagerLookup txLookup;

    @Before
    public void setup() {
        txLookup = new DefaultTransactionManagerLookup();    
    }
    
    @Test
    public void shouldDetectJBossJTAStandaloneJTAManager() throws Exception {
        //JBoss JTA (Narayana) is used by default with the 'test' scope, so just test that the lookup picks this up by default
        TransactionManager transactionManager = txLookup.getTransactionManager();
        assertNotNull(transactionManager);
        assertTrue(transactionManager.getClass().getName().contains("arjuna"));
        transactionManager.begin();
        Transaction tx = transactionManager.getTransaction();
        assertNotNull(tx);
        tx.commit();
    }
}
