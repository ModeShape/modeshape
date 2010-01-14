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
package org.modeshape.graph.query.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents the abstract base class for all query commands. Subclasses include {@link Query} and {@link SetQuery}.
 */
public abstract class QueryCommand extends Command {
    private static final long serialVersionUID = 1L;

    private final List<Ordering> orderings;
    private final Limit limits;

    /**
     * Create a new query command.
     */
    protected QueryCommand() {
        this(null, null);
    }

    /**
     * Create a new query command that uses the supplied orderings and limits.
     * 
     * @param orderings the specifications of how the results are to be ordered, or null if the order is to be implementation
     *        determined result columns are to be implementation determiend
     * @param limit the limit for the results, or null if all of the results are to be included
     */
    protected QueryCommand( List<Ordering> orderings,
                            Limit limit ) {
        this.orderings = orderings != null ? orderings : Collections.<Ordering>emptyList();
        this.limits = limit != null ? limit : Limit.NONE;
    }

    /**
     * Return the orderings for this query.
     * 
     * @return the list of orderings; never null
     */
    public final List<Ordering> getOrderings() {
        return orderings;
    }

    /**
     * Get the limits associated with this query.
     * 
     * @return the limits; never null but possibly {@link Limit#isUnlimited() unlimited}
     */
    public final Limit getLimits() {
        return limits;
    }
}
