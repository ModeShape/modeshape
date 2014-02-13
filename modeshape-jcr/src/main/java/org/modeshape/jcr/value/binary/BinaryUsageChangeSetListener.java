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
package org.modeshape.jcr.value.binary;

import java.util.HashSet;
import java.util.Set;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.change.BinaryValueUnused;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.BinaryKey;

/**
 * Listener for changes to binary values.
 */
public class BinaryUsageChangeSetListener implements ChangeSetListener {

    private final BinaryStore store;
    private final Logger logger;

    /**
     * Creates a new instance wrapping a binary store.
     *
     * @param store a {@link org.modeshape.jcr.value.binary.BinaryStore}
     */
    public BinaryUsageChangeSetListener( BinaryStore store ) {
        this.store = store;
        assert this.store != null;
        this.logger = Logger.getLogger(getClass());
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        Set<BinaryKey> unusedKeys = null;
        for (Change change : changeSet) {
            if (change instanceof BinaryValueUnused) {
                BinaryValueUnused unused = (BinaryValueUnused)change;
                BinaryKey key = unused.getKey();
                if (unusedKeys == null) unusedKeys = new HashSet<>();
                unusedKeys.add(key);
            }
        }
        if (unusedKeys != null && !unusedKeys.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Marking binary values as unused: ", unusedKeys);
            }
            try {
                store.markAsUnused(unusedKeys);
            } catch (BinaryStoreException e) {
                logger.error(JcrI18n.errorMarkingBinaryValuesUnused, e.getMessage());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Finished marking binary values as unused: ", unusedKeys);
            }
        }
    }
}
