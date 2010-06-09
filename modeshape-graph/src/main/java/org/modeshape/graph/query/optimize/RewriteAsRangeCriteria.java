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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.ValueComparators;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.Visitor;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * An {@link OptimizerRule optimizer rule} that rewrites two {@link And AND-ed} {@link Constraint}s that constraint a dynamic
 * operand to a range of values as a single {@link Between} constraint. This rule also collapses and removes any constraints that
 * are unnecessary because other constraints are more restrictive or because they cancel out other constraints.
 */
@Immutable
public class RewriteAsRangeCriteria implements OptimizerRule {

    protected static final Constraint CONFLICTING_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 1L;

        public void accept( Visitor visitor ) {
            throw new UnsupportedOperationException();
        }
    };

    public static final RewriteAsRangeCriteria INSTANCE = new RewriteAsRangeCriteria();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Find all the access nodes ...
        boolean rewritten = false;
        boolean foundNoResults = false;
        for (PlanNode access : plan.findAllAtOrBelow(Type.ACCESS)) {
            // Look for select nodes below an ACCESS node that have a single Comparison constraint,
            // and accumulate them keyed by the dynamic operand ...
            Multimap<DynamicOperand, PlanNode> selectNodeByOperand = ArrayListMultimap.create();
            for (PlanNode select : access.findAllAtOrBelow(Type.SELECT)) {
                Constraint constraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                // Look for Comparison constraints that use a range operator
                if (constraint instanceof Comparison) {
                    Comparison comparison = (Comparison)constraint;
                    if (comparison.operator().isRangeOperator()) {
                        selectNodeByOperand.put(comparison.operand1(), select);
                    }
                }
            }

            if (!selectNodeByOperand.isEmpty()) {

                // Go through the constraints we've found ...
                for (DynamicOperand operand : selectNodeByOperand.keySet()) {
                    Collection<PlanNode> nodes = selectNodeByOperand.get(operand);
                    if (nodes.size() <= 1) continue;

                    // Extract the constraints from the nodes ...
                    List<Comparison> rangeConstraints = new ArrayList<Comparison>(nodes.size());
                    List<PlanNode> selectNodes = new ArrayList<PlanNode>(nodes.size());
                    Set<SelectorName> selectors = null;
                    for (PlanNode select : nodes) {
                        selectNodes.add(select);
                        Comparison constraint = select.getProperty(Property.SELECT_CRITERIA, Comparison.class);
                        rangeConstraints.add(constraint);
                        // Record the selector names (should all be the same) ...
                        if (selectors == null) selectors = select.getSelectors();
                        else assert selectors.equals(select.getSelectors());
                    }

                    // Attempt to merge the constraints ...
                    Constraint merged = rewrite(context, rangeConstraints);
                    if (merged == CONFLICTING_CONSTRAINT) {
                        // The ANDed constraints cancel each other out, so this whole access node will return no results ...
                        access.setProperty(Property.ACCESS_NO_RESULTS, Boolean.TRUE);
                        foundNoResults = true;
                        break; // don't do anything else under this access node
                    }
                    if (merged != null) {
                        // Add a SELECT node for the new merged constraint ...
                        PlanNode newSelect = new PlanNode(Type.SELECT);
                        newSelect.getSelectors().addAll(selectors);
                        newSelect.setProperty(Property.SELECT_CRITERIA, merged);

                        // And insert the SELECT node into the tree (just below the ACCESS, we'll rerun pushdown selects) ...
                        assert access.getChildCount() == 1;
                        access.getFirstChild().insertAsParent(newSelect);
                        rewritten = true;
                    }

                    // Remove any of the SELECT nodes that were not needed (this can happen if the constraints are not needed) ...
                    Iterator<PlanNode> nodeIter = selectNodes.iterator();
                    Iterator<Comparison> constraintIter = rangeConstraints.iterator();
                    while (nodeIter.hasNext()) {
                        assert constraintIter.hasNext();
                        PlanNode node = nodeIter.next();
                        Comparison comparison = constraintIter.next();
                        if (comparison == null) {
                            // This comparison was rewritten, so remove the PlanNode ...
                            node.extractFromParent();
                            nodeIter.remove();
                        }
                    }
                    assert !constraintIter.hasNext();
                }
            }
        }

        if (rewritten) {
            // We mucked with the SELECT nodes, adding SELECT node for each rewritten constraint.
            // Rerun the rule that pushes SELECT nodes ...
            ruleStack.addFirst(PushSelectCriteria.INSTANCE);
        }
        if (foundNoResults) {
            ruleStack.addFirst(RemoveEmptyAccessNodes.INSTANCE);
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

    /**
     * Rewrite the supplied comparisons, returning the new constraint and nulling in the supplied list those comparisons that were
     * rewritten (and leaving those that were not rewritten)
     * 
     * @param context the query context
     * @param comparisons the list of comparisons that sould be rewritten if possible; never null
     * @return the rewritten constraint, or null if no comparisons were rewritten
     */
    @SuppressWarnings( "fallthrough" )
    protected Constraint rewrite( QueryContext context,
                                  List<Comparison> comparisons ) {
        // Look for the lower bound (greater-than) and upper bound (less-than) ...
        Comparison lessThan = null;
        Comparison greaterThan = null;
        List<Comparison> notNeeded = new LinkedList<Comparison>();
        boolean inclusive = false;
        for (Comparison comparison : comparisons) {
            switch (comparison.operator()) {
                case GREATER_THAN_OR_EQUAL_TO:
                    inclusive = true;
                case GREATER_THAN:
                    if (greaterThan != null) {
                        // Find the smallest value ...
                        Comparison newGreaterThan = getComparison(context, greaterThan, comparison, true);
                        notNeeded.add(newGreaterThan == greaterThan ? comparison : greaterThan);
                        greaterThan = newGreaterThan;
                    } else {
                        greaterThan = comparison;
                    }
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    inclusive = true;
                case LESS_THAN:
                    if (lessThan != null) {
                        // Find the largest value ...
                        Comparison newLessThan = getComparison(context, lessThan, comparison, false);
                        notNeeded.add(newLessThan == lessThan ? comparison : lessThan);
                        greaterThan = newLessThan;
                    } else {
                        lessThan = comparison;
                    }
                    break;
                default:
                    assert false;
                    return null;
            }
        }
        if (lessThan == null || greaterThan == null) return null;

        // Create the new Comparison ...
        Constraint result = null;

        // Compute the difference between the lessThan value and greaterThan value ...
        int diff = compareStaticOperands(context, greaterThan, lessThan);
        if (diff == 0) {
            // The static operands are equivalent ...
            if (inclusive) {
                // At least one of the sides was inclusive, meaning the constraints were something
                // like 'x >= 2 AND x < 2', so we can replace these with an equality constraint ...
                result = new Comparison(lessThan.operand1(), Operator.EQUAL_TO, lessThan.operand2());
                notNeeded.add(lessThan);
                notNeeded.add(greaterThan);
            } else {
                // Neither is inclusive, so really the constraints are not needed anymore.
                // And, because the constraints conflict, the whole access will return no nodes.
                // So return the placeholder ...
                return CONFLICTING_CONSTRAINT;
            }
        } else if (diff < 0) {
            // The range is valid as is ...
            boolean lowerInclusive = greaterThan.operator() == Operator.GREATER_THAN_OR_EQUAL_TO;
            boolean upperInclusive = lessThan.operator() == Operator.LESS_THAN_OR_EQUAL_TO;
            result = new Between(lessThan.operand1(), greaterThan.operand2(), lessThan.operand2(), lowerInclusive, upperInclusive);
            notNeeded.add(lessThan);
            notNeeded.add(greaterThan);
        } else {
            // The range is actually something like 'x < 2 AND x > 4', which can never happen ...
            return CONFLICTING_CONSTRAINT;
        }

        // Now null out those comparison objects that are not needed ...
        nullReference(comparisons, notNeeded);
        return result;
    }

    /**
     * Find all occurrences of the comparison object in the supplied list and null the list's reference to it.
     * 
     * @param comparisons the collection in which null references are to be placed
     * @param comparisonToNull the comparison that is to be found and nulled in the collection
     */
    protected void nullReference( List<Comparison> comparisons,
                                  Comparison comparisonToNull ) {
        if (comparisonToNull != null) {
            for (int i = 0; i != comparisons.size(); ++i) {
                if (comparisons.get(i) == comparisonToNull) comparisons.set(i, null);
            }
        }
    }

    /**
     * Find all references in the supplied list that match those supplied and set them to null.
     * 
     * @param comparisons the collection in which null references are to be placed
     * @param comparisonsToNull the comparisons that are to be found and nulled in the collection
     */
    protected void nullReference( List<Comparison> comparisons,
                                  Iterable<Comparison> comparisonsToNull ) {
        for (Comparison comparisonToNull : comparisonsToNull) {
            nullReference(comparisons, comparisonToNull);
        }
    }

    /**
     * Compare the values used in the two comparisons
     * 
     * @param context the query context; may not be null
     * @param comparison1 the first comparison object; may not be null
     * @param comparison2 the second comparison object; may not be null
     * @return 0 if the values are the same, less than 0 if the first comparison's value is less than the second's, or greater
     *         than 0 if the first comparison's value is greater than the second's
     */
    protected int compareStaticOperands( QueryContext context,
                                         Comparison comparison1,
                                         Comparison comparison2 ) {
        Object value1 = getValue(context, comparison1.operand2());
        Object value2 = getValue(context, comparison2.operand2());
        return ValueComparators.OBJECT_COMPARATOR.compare(value1, value2);
    }

    /**
     * Get the comparison with the smallest (or largest) value.
     * 
     * @param context the query context; may not be null
     * @param comparison1 the first comparison object; may not be null
     * @param comparison2 the second comparison object; may not be null
     * @param smallest true if the comparison with the smallest value should be returned, or false otherwise
     * @return the comparison with the smallest (or largest) value
     */
    protected Comparison getComparison( QueryContext context,
                                        Comparison comparison1,
                                        Comparison comparison2,
                                        boolean smallest ) {
        int diff = compareStaticOperands(context, comparison1, comparison2);
        if (diff == 0) {
            // They are the same ...
            return comparison1;
        }
        if (!smallest) diff = -1 * diff;
        return diff < 1 ? comparison1 : comparison2;
    }

    /**
     * Get the value associated with the static operand of the comparison. If the operand is a {@link BindVariableName variable
     * name}, the variable value is returned.
     * 
     * @param context the query context; may not be null
     * @param operand the static operand; may not be null
     * @return the value of the static operand
     */
    protected Object getValue( QueryContext context,
                               StaticOperand operand ) {
        if (operand instanceof Literal) {
            Literal literal = (Literal)operand;
            return literal.value();
        }
        BindVariableName variable = (BindVariableName)operand;
        return context.getVariables().get(variable.variableName());
    }
}
