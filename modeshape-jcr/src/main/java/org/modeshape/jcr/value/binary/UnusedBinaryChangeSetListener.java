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
 * 
 */
public class UnusedBinaryChangeSetListener implements ChangeSetListener {

    private final BinaryStore store;
    private final Logger logger;

    public UnusedBinaryChangeSetListener( BinaryStore store ) {
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
                if (unusedKeys == null) unusedKeys = new HashSet<BinaryKey>();
                unusedKeys.add(unused.getKey());
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
