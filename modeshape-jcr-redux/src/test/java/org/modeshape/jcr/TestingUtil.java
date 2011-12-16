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
package org.modeshape.jcr;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrEngine.State;
import org.modeshape.jcr.value.binary.TransientBinaryStore;

/**
 * 
 */
public class TestingUtil {

    private static final Logger log = Logger.getLogger(TestingUtil.class);

    public static void killTransientBinaryStore() {
        File directory = TransientBinaryStore.get().getDirectory();
        FileUtil.delete(directory);
    }

    public static void killRepositories( Repository... repositories ) {
        killRepositories(Arrays.asList(repositories));
    }

    public static void killRepositories( Iterable<Repository> repositories ) {
        for (Repository repository : repositories) {
            if (repository instanceof JcrRepository) {
                killRepository((JcrRepository)repository);
            }
        }
    }

    public static void killRepository( JcrRepository repository ) {
        if (repository == null) return;
        try {
            if (repository.getState() != State.RUNNING) return;
            // Rollback any open transactions ...
            TransactionManager txnMgr = repository.runningState().txnManager();
            if (txnMgr != null) {
                try {
                    txnMgr.rollback();
                } catch (Throwable t) {
                    // don't care
                }
            }

            // Then get the caches (which we'll kill after we shutdown the repository) ...
            Collection<Cache<?, ?>> caches = repository.caches();

            // First shut down the repository ...
            repository.shutdown().get(20, TimeUnit.SECONDS);

            // Get the caches and kill them ...
            for (Cache<?, ?> cache : caches) {
                if (cache != null) org.infinispan.test.TestingUtil.killCaches(cache);
            }
        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingRepository, repository.getName(), t.getMessage());
        }
    }

    public static void killEngine( JcrEngine engine ) {
        if (engine == null) return;
        try {
            if (engine.getState() != State.RUNNING) return;

            // First shutdown and destroy the repositories ...
            for (String key : engine.getRepositoryKeys()) {
                JcrRepository repository = engine.getRepository(key);
                killRepository(repository);
            }
            // Then shutdown the engine ...
            engine.shutdown().get(20, TimeUnit.SECONDS);

        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingEngine, t.getMessage());
        }
    }
}
