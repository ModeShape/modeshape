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
package org.modeshape.graph;

import net.jcip.annotations.NotThreadSafe;

/**
 * A class used by this package to manage a single {@link Location} or multiple {@link Location} objects, without having the
 * overhead of a collection (when only one is needed) and which can grow efficiently as new locations are added. This is achieved
 * through an effective linked list.
 */
@NotThreadSafe
class Locations {
    private final Location location;
    private Locations next;

    /*package*/Locations( Location location ) {
        this.location = location;
    }

    /*package*/void add( Location location ) {
        if (this.next == null) {
            this.next = new Locations(location);
        } else {
            Locations theNextOne = this.next;
            while (theNextOne != null) {
                if (theNextOne.next == null) {
                    theNextOne.next = new Locations(location);
                    break;
                }
                theNextOne = theNextOne.next;
            }
        }
    }

    /*package*/boolean hasNext() {
        return this.next != null;
    }

    /*package*/Locations next() {
        return this.next;
    }

    /*package*/Location getLocation() {
        return this.location;
    }
}
