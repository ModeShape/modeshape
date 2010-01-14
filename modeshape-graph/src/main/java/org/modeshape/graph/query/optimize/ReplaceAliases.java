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
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanUtil;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.query.validate.Schemata.Table;

/**
 * An {@link OptimizerRule optimizer rule} that changes any nodes that make use of an alias for a SOURCE, including columns,
 * including criteria, project nodes, etc. This behavior is similar to what {@link ReplaceViews} does with views that are given
 * aliases.
 */
@Immutable
public class ReplaceAliases implements OptimizerRule {

    public static final ReplaceAliases INSTANCE = new ReplaceAliases();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the SOURCE nodes ...
        Schemata schemata = context.getSchemata();
        for (PlanNode sourceNode : plan.findAllAtOrBelow(Type.SOURCE)) {

            // Resolve the node to find the definition in the schemata ...
            SelectorName tableName = sourceNode.getProperty(Property.SOURCE_NAME, SelectorName.class);
            SelectorName tableAlias = sourceNode.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
            if (tableAlias != null) {
                Table table = schemata.getTable(tableName);
                // We also need to replace references to the alias for the view ...
                PlanUtil.ColumnMapping aliasMappings = PlanUtil.createMappingForAliased(tableAlias, table, sourceNode);
                // Adjust the plan nodes above the SOURCE node ...
                PlanUtil.replaceViewReferences(context, sourceNode.getParent(), aliasMappings);
                // And adjust the SOURCE node ...
                sourceNode.removeProperty(Property.SOURCE_ALIAS);
                sourceNode.getSelectors().remove(tableAlias);
                sourceNode.addSelector(tableName);
            }
        }
        return plan;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
