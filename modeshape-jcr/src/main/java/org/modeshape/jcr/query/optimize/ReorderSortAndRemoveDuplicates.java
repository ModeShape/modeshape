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
package org.modeshape.jcr.query.optimize;

import java.util.LinkedList;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Traversal;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that looks for a {@link Type#SORT} plan node below a {@link Type#DUP_REMOVE}, and swaps
 * them. This ensures that the duplicate removal is always lower in the tree. This is advantageous because many sort algorithms
 * can also efficiently remove duplicates, so it is often better for the {@link Type#SORT} plan node to appear higher in the
 * optimized plan.
 */
@Immutable
public class ReorderSortAndRemoveDuplicates implements OptimizerRule {

    public static final ReorderSortAndRemoveDuplicates INSTANCE = new ReorderSortAndRemoveDuplicates();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        for (PlanNode distinct : plan.findAllAtOrBelow(Traversal.PRE_ORDER, Type.DUP_REMOVE)) {
            // If there is a SORT below the DUP_REMOVE, then swap them...
            if (distinct.getFirstChild().getType() == Type.SORT) {
                // Swap them so that duplicate removal happens first (it's lower in the plan) ...
                PlanNode parent = distinct.getParent();
                PlanNode sort = distinct.getFirstChild();
                assert sort.getParent() == distinct;
                // First, remove SORT from DUP_REMOVE (which will be empty) ...
                sort.removeFromParent();
                assert sort.getParent() == null;
                // Move all children of SORT into the currently-empty DUP_REMOVE ...
                distinct.addChildren(sort.getChildren());
                assert sort.getChildCount() == 0;
                assert sort.getParent() == null;
                // Swap DUP_REMOVE in parent with SORT
                parent.replaceChild(distinct, sort);
                assert sort.getParent() == parent;
                assert sort.getChildCount() == 0;
                assert distinct.getParent() == null;
                // Now move DUP_REMOVE under SORT ...
                distinct.setParent(sort);
                assert distinct.getParent() == sort;
                assert sort.getParent() == parent;
            }
        }
        return plan;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
