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
package org.modeshape.connector.store.jpa.model.simple;

import javax.persistence.EntityTransaction;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.connector.map.MapRepositoryTransaction;

/**
 * A {@link MapRepositoryTransaction} that wraps an {@link EntityTransaction}.
 */
@Immutable
public class SimpleJpaTransaction implements MapRepositoryTransaction {
    private final EntityTransaction txn;

    /**
     * Create a transaction to wrap an {@link EntityTransaction}.
     * 
     * @param txn the entity transaction, which may be null if there is none
     */
    SimpleJpaTransaction( EntityTransaction txn ) {
        this.txn = txn;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.MapRepositoryTransaction#commit()
     */
    public void commit() {
        if (txn != null) txn.commit();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.MapRepositoryTransaction#rollback()
     */
    public void rollback() {
        if (txn != null) this.txn.rollback();
    }

}
