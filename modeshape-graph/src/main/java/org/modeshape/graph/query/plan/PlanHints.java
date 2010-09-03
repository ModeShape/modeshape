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
package org.modeshape.graph.query.plan;

import java.io.Serializable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.query.QueryResults;

@NotThreadSafe
public final class PlanHints implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    /** This flag indicates that the plan has a criteria somewhere */
    public boolean hasCriteria = false;

    /** This flag indicates that the plan has a join somewhere */
    public boolean hasJoin = false;

    /** This flag indicates that the plan has a sort somewhere */
    public boolean hasSort = false;

    // List of groups to make dependent
    // public List makeDepGroups = null;

    /** flag indicates that the plan has a union somewhere */
    public boolean hasSetQuery = false;

    // flag indicating that the plan has a grouping node somewhere
    // public boolean hasAggregates = false;

    // List of groups that should not be dependent
    // public List makeNotDepGroups = null;

    public boolean hasLimit = false;

    public boolean hasOptionalJoin = false;

    public boolean hasFullTextSearch = false;

    public boolean hasSubqueries = false;

    /** Flag indicates that the plan has at least one view somewhere */
    public boolean hasView = false;

    /** Flag indicates whether the query plan should be included in the {@link QueryResults} */
    public boolean showPlan = false;

    /** Flag indicates whether to check during validation for the existance of columns used in column selectors and criteria. */
    public boolean validateColumnExistance = true;

    public PlanHints() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlanHints {");
        sb.append(" hasCriteria=").append(hasCriteria);
        sb.append(", hasView=").append(hasView);
        sb.append(", hasJoin=").append(hasJoin);
        sb.append(", hasSort=").append(hasSort);
        sb.append(", hasSetQuery=").append(hasSetQuery);
        sb.append(", hasLimit=").append(hasLimit);
        sb.append(", hasOptionalJoin=").append(hasOptionalJoin);
        sb.append(", hasFullTextSearch=").append(hasFullTextSearch);
        sb.append(", hasSubqueries=").append(hasSubqueries);
        sb.append(", showPlan=").append(showPlan);
        sb.append(", validateColumnExistance=").append(validateColumnExistance);
        sb.append('}');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public PlanHints clone() {
        PlanHints clone = new PlanHints();
        clone.hasCriteria = this.hasCriteria;
        clone.hasView = this.hasView;
        clone.hasJoin = this.hasJoin;
        clone.hasSort = this.hasSort;
        clone.hasSetQuery = this.hasSetQuery;
        clone.hasLimit = this.hasLimit;
        clone.hasOptionalJoin = this.hasOptionalJoin;
        clone.hasFullTextSearch = this.hasFullTextSearch;
        clone.showPlan = this.showPlan;
        clone.validateColumnExistance = this.validateColumnExistance;
        return clone;
    }
}
