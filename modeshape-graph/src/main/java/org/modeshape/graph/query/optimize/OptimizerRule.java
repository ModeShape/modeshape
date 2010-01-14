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
package org.modeshape.graph.query.optimize;

import java.util.LinkedList;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.plan.PlanNode;

/**
 * Interface that defines an {@link Optimizer} rule.
 */
@Immutable
public interface OptimizerRule {

    /**
     * Optimize the supplied plan using the supplied context, hints, and yet-to-be-run rules.
     * 
     * @param context the context in which the query is being optimized; never null
     * @param plan the plan to be optimized; never null
     * @param ruleStack the stack of rules that will be run after this rule; never null
     * @return the optimized plan; never null
     */
    PlanNode execute( QueryContext context,
                      PlanNode plan,
                      LinkedList<OptimizerRule> ruleStack );

}
