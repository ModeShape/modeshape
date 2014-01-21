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
package org.modeshape.jcr.query.plan;

import java.io.Serializable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.query.QueryResults;

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

    /**
     * Set when the query results are not ordered or offset but are limited to a single row, implying that the query is just
     * trying to see if a row exists.
     */
    public boolean isExistsQuery = false;

    public boolean hasOptionalJoin = false;

    public boolean hasFullTextSearch = false;

    public boolean hasSubqueries = false;

    /** Flag indicates that the plan has at least one view somewhere */
    public boolean hasView = false;

    /** Flag indicates whether the query plan should be included in the {@link QueryResults} */
    public boolean showPlan = false;

    /** Flag indicates whether the query execution can be stopped immediately after the plan is developed. */
    public boolean planOnly = false;

    /** Flag indicates whether to check during validation for the existance of columns used in column selectors and criteria. */
    public boolean validateColumnExistance = true;

    /** Flag indicates whether the content under "/jcr:system" should be included in the results. */
    public boolean includeSystemContent = true;

    /**
     * Flag indicates whether the Session's transient (unsaved) content should be used for the results. If not, then only the
     * persisted content will be used.
     */
    public boolean useSessionContent = true;

    /**
     * Flag indicates whether to fully-qualify (with the selector name) the names of columns that are expanded from wildcard
     * projections.
     */
    public boolean qualifyExpandedColumnNames = false;

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
        sb.append(", isExistsQuery=").append(isExistsQuery);
        sb.append(", showPlan=").append(showPlan);
        sb.append(", planOnly=").append(planOnly);
        sb.append(", validateColumnExistance=").append(validateColumnExistance);
        sb.append(", includeSystemContent=").append(includeSystemContent);
        sb.append(", useSessionContent=").append(useSessionContent);
        sb.append(", qualifyExpandedColumnNames=").append(qualifyExpandedColumnNames);
        sb.append('}');
        return sb.toString();
    }

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
        clone.hasSubqueries = this.hasSubqueries;
        clone.isExistsQuery = this.isExistsQuery;
        clone.showPlan = this.showPlan;
        clone.planOnly = this.planOnly;
        clone.validateColumnExistance = this.validateColumnExistance;
        clone.includeSystemContent = this.includeSystemContent;
        clone.useSessionContent = this.useSessionContent;
        clone.qualifyExpandedColumnNames = this.qualifyExpandedColumnNames;
        return clone;
    }
}
