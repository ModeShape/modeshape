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
package org.modeshape.transaction.lookup;

import java.util.Properties;
import java.util.UUID;
import javax.transaction.TransactionManager;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * Used only for testing ModeShape/Infinispan with Atomikos.
 */
public class AtomikosStandaloneJTAManagerLookup implements TransactionManagerLookup {

    private static final TransactionManager INSTANCE;
    private static final UserTransactionServiceImp SERVICE;

    static {
        Properties props = new Properties();
        props.setProperty("com.atomikos.icatch.log_base_dir", "target/atomikos/log");
        props.setProperty("com.atomikos.icatch.output_dir", "target/atomikos/out");
        //we need to set the next property, or Atomikos will not run in using IPv6
        props.setProperty("com.atomikos.icatch.tm_unique_name", UUID.randomUUID().toString());
        SERVICE = new UserTransactionServiceImp(props);
        SERVICE.init();

        INSTANCE = new UserTransactionManager();
    }

    @Override
    public TransactionManager getTransactionManager() throws Exception {
        return INSTANCE;
    }
}
