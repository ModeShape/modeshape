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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Ordering;

/**
 * Represents the abstract base class for all query commands. Subclasses include {@link SetQuery} and {@link SelectQuery}.
 */
public interface QueryCommand {

    /**
     * Gets the orderings for this query.
     * 
     * @return an array of zero or more orderings; non-null
     */
    public Ordering[] getOrderings();

    /**
     * Gets the columns for this query.
     * 
     * @return an array of zero or more columns; non-null
     */
    public Column[] getColumns();

    /**
     * Get the limits associated with this query.
     * 
     * @return the limits; never null but possibly {@link Limit#isUnlimited() unlimited}
     */
    public Limit getLimits();
}
