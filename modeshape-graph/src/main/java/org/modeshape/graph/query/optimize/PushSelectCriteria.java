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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that attempts to push the criteria nodes in a canonical plan down as far as possible.
 * <p>
 * For example, here is a single-access plan before:
 * 
 * <pre>
 *          ...
 *           |
 *        PROJECT      with the list of columns being SELECTed
 *           |
 *        SELECT1
 *           |         One or more SELECT plan nodes that each have
 *        SELECT2      a single non-join constraint that are then all AND-ed
 *           |         together
 *        SELECTn
 *           |
 *        ACCESS
 *           |
 *        SOURCE
 * </pre>
 * 
 * And after:
 * 
 * <pre>
 *          ...
 *           |
 *        ACCESS
 *           |
 *        PROJECT      with the list of columns being SELECTed
 *           |
 *        SELECT1
 *           |         One or more SELECT plan nodes that each have
 *        SELECT2      a single non-join constraint that are then all AND-ed
 *           |         together
 *        SELECTn
 *           |
 *        SOURCE
 * </pre>
 * 
 * Here is another case, where multiple SELECT nodes above a simple JOIN and where each SELECT node applies to one or more of the
 * SOURCE nodes (via the named selectors). Each SELECT node that applies to a single selector will get pushed toward that source,
 * but will have the same order relative to other SELECT nodes also pushed toward that SOURCE. However, this rules does not push
 * SELECT nodes that apply to multiple selectors.
 * </p>
 * <p>
 * Before:
 * 
 * <pre>
 *          ...
 *           |
 *        PROJECT ('s1','s2')      with the list of columns being SELECTed (from 's1' and 's2' selectors)
 *           |
 *        SELECT1 ('s1')
 *           |                     One or more SELECT plan nodes that each have
 *        SELECT2 ('s2')           a single non-join constraint that are then all AND-ed
 *           |                     together, and that each have the selector(s) they apply to
 *        SELECT3 ('s1','s2')
 *           |
 *        SELECT4 ('s1')
 *           |
 *         JOIN ('s1','s2')
 *        /     \
 *       /       \
 *   ACCESS     ACCESS
 *    ('s1')    ('s2')
 *     |           |
 *   PROJECT    PROJECT
 *    ('s1')    ('s2')
 *     |           |
 *   SOURCE     SOURCE
 *    ('s1')    ('s2')
 * </pre>
 * 
 * And after:
 * 
 * <pre>
 *          ...
 *           |
 *        PROJECT ('s1','s2')      with the list of columns being SELECTed (from 's1' and 's2' selectors)
 *           |
 *        SELECT3 ('s1','s2')      Any SELECT plan nodes that apply to multiple selectors are left above
 *           |                     the ACCESS nodes.
 *         JOIN ('s1','s2')
 *        /     \
 *       /       \
 *   ACCESS     ACCESS
 *   ('s1')     ('s2')
 *     |           |
 *  PROJECT     PROJECT
 *   ('s1')     ('s2')
 *     |           |
 *  SELECT1     SELECT2
 *   ('s1')     ('s2')
 *     |           |
 *  SELECT4     SOURCE
 *   ('s1')     ('s2')
 *     |
 *   SOURCE
 *   ('s1')
 * </pre>
 * 
 * </p>
 * <p>
 * Also, any SELECT that applies to one side of an equi-join will be applied to <i>both</i> sides of the JOIN.
 * </p>
 */
@Immutable
public class PushSelectCriteria implements OptimizerRule {

    public static final PushSelectCriteria INSTANCE = new PushSelectCriteria();
    private static final Set<Type> ORIGINATING_TYPES = Collections.unmodifiableSet(EnumSet.of(Type.NULL,
                                                                                              Type.SOURCE,
                                                                                              Type.JOIN,
                                                                                              Type.SET_OPERATION));

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Create set of nodes that no longer need to be considered
        Set<PlanNode> deadNodes = new HashSet<PlanNode>();

        // Loop while criteria nodes are still being moved ...
        boolean movedSomeNode = true;
        while (movedSomeNode) {

            // Reset flag to false for this iteration
            movedSomeNode = false;

            // Find all of the criteria (SELECT) nodes that can be pushed ...
            List<PlanNode> criteriaNodes = plan.findAllAtOrBelow(Type.SELECT);

            // Find all of the NULL, SOURCE, SET_OPERATION or JOIN nodes, ordered correctly; we'll use this
            // to look for the node on which each criteria can apply ...
            List<PlanNode> originatingNodes = plan.findAllAtOrBelow(ORIGINATING_TYPES);

            // Starting with the lowest one first ...
            Collections.reverse(criteriaNodes);
            for (PlanNode criteriaNode : criteriaNodes) {
                // Skip any node we've already tried and failed to move ...
                if (deadNodes.contains(criteriaNode)) continue;

                // Find the first originating node that has all of the required selectors for this criteria ...
                PlanNode originatingNode = findOriginatingNode(criteriaNode, originatingNodes);
                if (originatingNode == null || originatingNode == criteriaNode) {
                    deadNodes.add(criteriaNode);
                    continue;
                }

                // Try to push the criteria node down ...
                if (!pushTowardsOriginatingNode(criteriaNode, originatingNode)) {
                    // criteria node could not be moved ...
                    deadNodes.add(criteriaNode);
                    continue;
                }

                // The criteria node was indeed moved, but it may need to be adjusted ...
                boolean adjusted = false;
                switch (originatingNode.getType()) {
                    case SOURCE:

                        break;
                    case JOIN:
                        if (!criteriaNode.hasAncestorOfType(Type.ACCESS)) {
                            // Try to push down the join criteria (only when above ACCESS nodes) ...
                            adjusted = pushDownJoinCriteria(criteriaNode, originatingNode);
                        }
                        break;
                    default:
                        // Nothing to change ...
                }
                if (adjusted) {
                    // We changed something, so make sure we go through the loop again ...
                    movedSomeNode = true;
                } else {
                    // Nothing was changed from the push-down, so consider this criteria node as processed ...
                    deadNodes.add(criteriaNode);
                }
            }
        }
        return plan;
    }

    /**
     * Attempt to push down criteria that applies to the JOIN as additional constraints on the JOIN itself.
     * 
     * @param criteriaNode the SELECT node; may not be null
     * @param joinNode the JOIN node; may not be null
     * @return true if the criteria was pushed down, or false otherwise
     */
    protected boolean pushDownJoinCriteria( PlanNode criteriaNode,
                                            PlanNode joinNode ) {
        JoinType joinType = (JoinType)joinNode.getProperty(Property.JOIN_TYPE);

        switch (joinType) {
            case CROSS:
                joinNode.setProperty(Property.JOIN_TYPE, JoinType.INNER);
                moveCriteriaIntoOnClause(criteriaNode, joinNode);
                break;
            case INNER:
                moveCriteriaIntoOnClause(criteriaNode, joinNode);
                break;
            default:
                // This is where we could attempt to optimize the join type ...
                // if (optimizeJoinType(criteriaNode, joinNode) == JoinType.INNER) {
                // // The join type has changed ...
                // moveCriteriaIntoOnClause(criteriaNode, joinNode);
                // return true; // since the join type has changed ...
                // }
        }
        return false;
    }

    /**
     * Move the criteria that applies to the join to be included in the actual join criteria.
     * 
     * @param criteriaNode the SELECT node; may not be null
     * @param joinNode the JOIN node; may not be null
     */
    private void moveCriteriaIntoOnClause( PlanNode criteriaNode,
                                           PlanNode joinNode ) {
        List<Constraint> constraints = joinNode.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
        Constraint criteria = criteriaNode.getProperty(Property.SELECT_CRITERIA, Constraint.class);

        // since the parser uses EMPTY_LIST, check for size 0 also
        if (constraints == null || constraints.isEmpty()) {
            constraints = new LinkedList<Constraint>();
            joinNode.setProperty(Property.JOIN_CONSTRAINTS, constraints);
        }

        if (!constraints.contains(criteria)) {
            constraints.add(criteria);
            if (criteriaNode.hasBooleanProperty(Property.IS_DEPENDENT)) {
                joinNode.setProperty(Property.IS_DEPENDENT, Boolean.TRUE);
            }
        }
        criteriaNode.extractFromParent();
    }

    /**
     * Find the first node that has all of the same {@link PlanNode#getSelectors() selectors} as the supplied criteria.
     * 
     * @param criteriaNode the criteria
     * @param originatingNodes the list of nodes to search
     * @return the first (highest) node that is uses all of the same selectors as the criteria, or null if no such node could be
     *         found
     */
    protected PlanNode findOriginatingNode( PlanNode criteriaNode,
                                            List<PlanNode> originatingNodes ) {
        Set<SelectorName> requiredSelectors = criteriaNode.getSelectors();
        if (requiredSelectors.isEmpty()) return criteriaNode;

        // first look for originating nodes that exactly match the required selectors ...
        for (PlanNode originatingNode : originatingNodes) {
            if (!criteriaNode.isAbove(originatingNode)) continue;
            if (originatingNode.getSelectors().equals(requiredSelectors)) return originatingNode;
        }

        // Nothing matched exactly, so can we push down to a node that contain all of the required selectors ...
        for (PlanNode originatingNode : originatingNodes) {
            if (originatingNode.getSelectors().containsAll(requiredSelectors)) return originatingNode;
        }
        return null;
    }

    /**
     * Push the criteria node as close to the originating node as possible. In general, the criteria node usually ends up being
     * moved to be a parent of the supplied originating node, except in a couple of cases:
     * <ul>
     * <li>There are already criteria nodes immediately above the originating node; in this case, the supplied criteria node is
     * placed above all these existing criteria nodes.</li>
     * <li>The originating node is below a JOIN node that is itself below an ACCESS node; in this case, the criteria node is
     * placed immediately above the JOIN node.</li>
     * <li>The originating node is below a LIMIT with a single SORT child node; in this case, the criteria node is placed
     * immediately above the LIMIT node.</li>
     * </ul>
     * 
     * @param criteriaNode the criteria node that is to be pushed down; may not be null
     * @param originatingNode the target node that represents the node above which the criteria node should be inserted; may not
     *        be null
     * @return true if the criteria node was pushed down, or false if the criteria node could not be pushed down
     */
    protected boolean pushTowardsOriginatingNode( PlanNode criteriaNode,
                                                  PlanNode originatingNode ) {
        // To keep things stable, 'originatingNode' should be the top-most SELECT (criteria) node above a SOURCE ...
        while (originatingNode.getParent().getType() == Type.SELECT) {
            originatingNode = originatingNode.getParent();
            if (originatingNode == criteriaNode) return false;
        }

        // Find out the best node above which the criteria node should be placed ...
        PlanNode bestChild = findBestChildForCriteria(criteriaNode, originatingNode);
        if (bestChild == criteriaNode) return false;
        criteriaNode.extractFromParent();
        bestChild.insertAsParent(criteriaNode);
        assert atBoundary(criteriaNode, originatingNode);
        return true;
    }

    protected PlanNode findBestChildForCriteria( PlanNode criteriaNode,
                                                 PlanNode originatingNode ) {
        // Walk the nodes, from the criteria node down to the originating node ...
        for (PlanNode node : criteriaNode.getPathTo(originatingNode)) {
            // Check the node to see if there is any reason why the node cannot be pushed
            if (node.getType() == Type.JOIN) {
                // Pushing below a JOIN is not necessary under an ACCESS node
                if (node.hasAncestorOfType(Type.ACCESS)) return node;
            } else if (node.getType() == Type.LIMIT) {
                // Don't push below a LIMIT above a SORT ...
                if (node.getChildCount() == 1 && node.getFirstChild().getType() == Type.SORT) {
                    return node;
                }
            }
        }
        return originatingNode;
    }

    /**
     * Determine whether all of the nodes between the criteria node and its originating node are criteria (SELECT) nodes.
     * 
     * @param criteriaNode the criteria node; may not be null
     * @param originatingNode the originating node
     * @return true if all nodes between the criteria and originating nodes are SELECT nodes
     */
    protected boolean atBoundary( PlanNode criteriaNode,
                                  PlanNode originatingNode ) {
        // Walk from source node to critNode to check each intervening node
        PlanNode currentNode = originatingNode.getParent();
        while (currentNode != criteriaNode) {
            if (currentNode.getType() != Type.SELECT) return false;
            currentNode = currentNode.getParent();
        }
        return true;
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
