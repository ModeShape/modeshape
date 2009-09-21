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
package org.jboss.dna.graph.query.plan;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public final class PlanHints {

    // This flag indicates that the plan has a criteria somewhere
    public boolean hasCriteria = false;

    // This flag indicates that the plan has a join somewhere
    public boolean hasJoin = false;

    // This flag indicates that the plan has a sort somewhere
    public boolean hasSort = false;

    // List of groups to make dependent
    // public List makeDepGroups = null;

    // flag indicates that the plan has a union somewhere
    public boolean hasSetQuery = false;

    // flag indicating that the plan has a grouping node somewhere
    // public boolean hasAggregates = false;

    // List of groups that should not be dependent
    // public List makeNotDepGroups = null;

    public boolean hasLimit = false;

    public boolean hasOptionalJoin = false;

    public boolean hasFullTextSearch = false;

    public PlanHints() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlanHints {");
        sb.append(" hasCriteria=").append(hasCriteria);
        sb.append(", hasJoin=").append(hasJoin);
        sb.append(", hasSort=").append(hasSort);
        sb.append(", hasSetQuery=").append(hasSetQuery);
        sb.append(", hasLimit=").append(hasLimit);
        sb.append(", hasOptionalJoin=").append(hasOptionalJoin);
        sb.append(", hasFullTextSearch=").append(hasFullTextSearch);
        sb.append('}');
        return sb.toString();
    }
}
