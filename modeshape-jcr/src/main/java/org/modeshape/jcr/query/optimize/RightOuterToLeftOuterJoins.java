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
package org.modeshape.jcr.query.optimize;

import java.util.LinkedList;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that converts {@link JoinType#RIGHT_OUTER right outer joins} into
 * {@link JoinType#LEFT_OUTER left outer joins}.
 */
@Immutable
public class RightOuterToLeftOuterJoins implements OptimizerRule {

    public static final RightOuterToLeftOuterJoins INSTANCE = new RightOuterToLeftOuterJoins();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the JOIN nodes ...
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            if (JoinType.RIGHT_OUTER == joinNode.getProperty(Property.JOIN_TYPE, JoinType.class)) {
                // Swap the information ...
                PlanNode left = joinNode.getFirstChild();
                left.removeFromParent(); // right is now the first child ...
                left.setParent(joinNode);
                joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
                // None of the Constraints or JoinCondition need to be changed (they refer to named selectors) ...
            }
        }
        return plan;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
