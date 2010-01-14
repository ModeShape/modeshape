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

import javax.jcr.version.OnParentVersionAction;

/**
 * Enumeration of possible behaviors for on-parent-version setting of properties and child nodes in JCR specification.
 */
enum OnParentVersionBehavior {
    /** @see OnParentVersionAction#ABORT */
    ABORT(OnParentVersionAction.ABORT, OnParentVersionAction.ACTIONNAME_ABORT),
    /** @see OnParentVersionAction#COMPUTE */
    COMPUTE(OnParentVersionAction.COMPUTE, OnParentVersionAction.ACTIONNAME_COMPUTE),
    /** @see OnParentVersionAction#COPY */
    COPY(OnParentVersionAction.COPY, OnParentVersionAction.ACTIONNAME_COPY),
    /** @see OnParentVersionAction#IGNORE */
    IGNORE(OnParentVersionAction.IGNORE, OnParentVersionAction.ACTIONNAME_IGNORE),
    /** @see OnParentVersionAction#INITIALIZE */
    INITIALIZE(OnParentVersionAction.INITIALIZE, OnParentVersionAction.ACTIONNAME_INITIALIZE),
    /** @see OnParentVersionAction#VERSION */
    VERSION(OnParentVersionAction.VERSION, OnParentVersionAction.ACTIONNAME_VERSION);

    private final int jcrValue;
    private final String name;

    OnParentVersionBehavior( int jcrValue,
                             String name ) {
        this.jcrValue = jcrValue;
        this.name = name;
    }

    public int getJcrValue() {
        return jcrValue;
    }

    public String getName() {
        return name;
    }

    public static OnParentVersionBehavior fromValue( int onParentVersionAction ) {
        for (OnParentVersionBehavior opvb : OnParentVersionBehavior.values()) {
            if (opvb.jcrValue == onParentVersionAction) {
                return opvb;
            }
        }

        throw new IllegalStateException("No matching version for value: " + onParentVersionAction);
    }
}
