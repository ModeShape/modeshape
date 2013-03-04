/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
