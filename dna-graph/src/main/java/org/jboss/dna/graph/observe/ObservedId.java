/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.observe;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.Immutable;

/**
 * A unique identifier for an event or observer that can be compared with IDs created before or after this ID.
 */
@Immutable
public final class ObservedId implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final AtomicLong idSequencer = new AtomicLong(0);
    
    private static long getNextId() {
        return idSequencer.getAndIncrement();
    }
    
    private final long id;
    
    /**
     * Constructs a unique ID.
     */
    public ObservedId() {
        this.id = getNextId();
    }
    
    /**
     * @param otherId the ID being compared to
     * @return <code>true</code> if this ID sequentially comes before the other ID
     */
    public boolean isBefore(ObservedId otherId) {
        return (this.id < otherId.id);
    }

}
