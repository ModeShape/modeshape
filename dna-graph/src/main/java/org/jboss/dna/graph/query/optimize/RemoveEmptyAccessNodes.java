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
package org.jboss.dna.graph.query.optimize;

import java.util.LinkedList;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that removes any ACCESS nodes that are known to never return any tuples because of
 * conflicting constraints.
 */
@Immutable
public class RemoveEmptyAccessNodes implements OptimizerRule {

    public static final RemoveEmptyAccessNodes INSTANCE = new RemoveEmptyAccessNodes();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.OptimizerRule#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Find all access nodes ...
        for (PlanNode access : plan.findAllAtOrBelow(Type.ACCESS)) {
            if (access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class)) {
                // This node has conflicting constraints and will never return any results ...

                // TODO: implement this rule.

                // At least the QueryProcessor looks for this property and always creates a NoResultsComponent,
                // saving some work. But implementing this rule will make queries more efficient.
            }
        }

        return plan;
    }
}
