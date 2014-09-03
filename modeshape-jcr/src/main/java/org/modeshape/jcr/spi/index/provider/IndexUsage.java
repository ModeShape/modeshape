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

package org.modeshape.jcr.spi.index.provider;

import javax.jcr.query.qom.And;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.UpperCase;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.query.qom.ArithmeticOperand;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.ChildCount;
import org.modeshape.jcr.api.query.qom.NodeDepth;
import org.modeshape.jcr.api.query.qom.NodeId;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.query.qom.ReferenceValue;
import org.modeshape.jcr.api.query.qom.Relike;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.value.Name;

/**
 * A component that can determine if an index can be used to evaluate constraints. This is often used within an
 * {@link IndexProvider} implementation during index planning. Specifically, see
 * {@link IndexProvider#planUseOfIndex(QueryContext, IndexCostCalculator, String, ManagedIndex, IndexDefinition)}.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @see IndexProvider
 */
public class IndexUsage {
    private final IndexDefinition defn;
    private final QueryContext context;
    private final Name indexedNodeTypeName;

    public IndexUsage( QueryContext context,
                       IndexCostCalculator calculator,
                       IndexDefinition defn ) {
        this.context = context;
        this.defn = defn;
        this.indexedNodeTypeName = name(defn.getNodeTypeName());
    }

    /**
     * Determine if this index can be used to evaluate the given constraint.
     *
     * @param constraint the constraint; may not be null
     * @return true if it can be used to evaluate the constraint, or false otherwise
     */
    public boolean indexAppliesTo( Constraint constraint ) {
        if (constraint instanceof Comparison) {
            return indexAppliesTo((Comparison)constraint);
        }
        if (constraint instanceof And) {
            return indexAppliesTo((And)constraint);
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            return indexAppliesTo(or.getConstraint1()) || indexAppliesTo(or.getConstraint2());
        }
        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            return indexAppliesTo(not.getConstraint());
        }
        if (constraint instanceof Between) {
            return indexAppliesTo((Between)constraint);
        }
        if (constraint instanceof SetCriteria) {
            return indexAppliesTo((SetCriteria)constraint);
        }
        if (constraint instanceof Relike) {
            return indexAppliesTo((Relike)constraint);
        }
        if (constraint instanceof PropertyExistence) {
            return indexAppliesTo((PropertyExistence)constraint);
        }
        return false;
    }

    protected boolean indexAppliesTo( Comparison constraint ) {
        Operator operator = Operator.forSymbol(constraint.getOperator());
        return applies(operator) && applies(constraint.getOperand1());
    }

    protected boolean indexAppliesTo( And and ) {
        return indexAppliesTo(and.getConstraint1()) || indexAppliesTo(and.getConstraint2());
    }

    protected boolean indexAppliesTo( Or or ) {
        return indexAppliesTo(or.getConstraint1()) || indexAppliesTo(or.getConstraint2());
    }

    protected boolean indexAppliesTo( Not not ) {
        return indexAppliesTo(not.getConstraint());
    }

    protected boolean indexAppliesTo( Between constraint ) {
        return applies(constraint.getOperand());
    }

    protected boolean indexAppliesTo( SetCriteria constraint ) {
        return applies(constraint.getOperand());
    }

    protected boolean indexAppliesTo( Relike constraint ) {
        return applies(constraint.getOperand2());
    }

    protected boolean indexAppliesTo( PropertyExistence constraint ) {
        // The selected node type must match or be a subtype of the indexed node type, and
        // one of the indexed columns must match the property ...
        return matchesSelectorName(constraint.getSelectorName()) && defn.appliesToProperty(constraint.getPropertyName());
    }

    protected boolean applies( Operator operator ) {
        return true;
    }

    protected boolean applies( DynamicOperand operand ) {
        if (operand instanceof PropertyValue) {
            return applies((PropertyValue)operand);
        }
        if (operand instanceof UpperCase) {
            return applies((UpperCase)operand);
        }
        if (operand instanceof LowerCase) {
            return applies((LowerCase)operand);
        }
        if (operand instanceof Length) {
            return applies((Length)operand);
        }
        if (operand instanceof ArithmeticOperand) {
            return applies((ArithmeticOperand)operand);
        }
        if (operand instanceof NodeName) {
            return applies((NodeName)operand);
        }
        if (operand instanceof NodeLocalName) {
            return applies((NodeLocalName)operand);
        }
        if (operand instanceof NodePath) {
            return applies((NodePath)operand);
        }
        if (operand instanceof NodeId) {
            return applies((NodeId)operand);
        }
        if (operand instanceof NodeDepth) {
            return applies((NodeDepth)operand);
        }
        if (operand instanceof ChildCount) {
            return applies((ChildCount)operand);
        }
        if (operand instanceof ReferenceValue) {
            return applies((ReferenceValue)operand);
        }
        if (operand instanceof FullTextSearch) {
            return applies((FullTextSearch)operand);
        }
        return false;
    }

    protected boolean applies( PropertyValue operand ) {
        // The selected node type must match or be a subtype of the indexed node type, and
        // one of the indexed columns must match the property ...
        return matchesSelectorName(operand.getSelectorName()) && defn.appliesToProperty(operand.getPropertyName());
    }

    protected boolean applies( UpperCase operand ) {
        return applies(operand.getOperand());
    }

    protected boolean applies( LowerCase operand ) {
        return applies(operand.getOperand());
    }

    protected boolean applies( Length operand ) {
        return applies(operand.getPropertyValue());
    }

    protected boolean applies( ArithmeticOperand operand ) {
        return applies(operand.getLeft()) && applies(operand.getRight());
    }

    protected boolean applies( NodeName operand ) {
        // This should apply to the 'jcr:name' pseudo-column on the index ...
        return defn.appliesToProperty("jcr:name");
    }

    protected boolean applies( NodeLocalName operand ) {
        // This should apply to the 'mode:localName' pseudo-column on the index ...
        return defn.appliesToProperty("mode:localName");
    }

    protected boolean applies( NodePath operand ) {
        // This should apply to the 'jcr:name' pseudo-column on the index ...
        return defn.appliesToProperty("jcr:path");
    }

    protected boolean applies( NodeId operand ) {
        // This should apply to the 'jcr:id' pseudo-column on the index ...
        return defn.appliesToProperty("jcr:id");
    }

    protected boolean applies( NodeDepth operand ) {
        // This should apply to the 'mode:depth' pseudo-column on the index ...
        return defn.appliesToProperty("mode:depth");
    }

    protected boolean applies( ChildCount operand ) {
        // This should apply to the 'mode:childCount' pseudo-column on the index ...
        return defn.appliesToProperty("mode:childCount");
    }

    protected boolean applies( ReferenceValue operand ) {
        if (matchesSelectorName(operand.getSelectorName())) {
            // The selected node type matches or is a subtype of the indexed node type ...
            String refPropName = operand.getPropertyName();
            if (refPropName != null) {
                // The constraint applies to a specific reference property ...
                return isReferenceIndex(refPropName);
            }
            // Otherwise, the constraint applies to any reference property ...
            return isReferenceIndex();
        }
        return false;
    }

    protected boolean applies( FullTextSearch operand ) {
        return true;
    }

    protected final boolean matchesSelectorName( String selectorName ) {
        Name selectedNodeTypeName = name(selectorName);
        return nodeTypes().isTypeOrSubtype(selectedNodeTypeName, indexedNodeTypeName);
    }

    protected final NodeTypes nodeTypes() {
        return context.getNodeTypes();
    }

    protected final Name name( String name ) {
        return context.getExecutionContext().getValueFactories().getNameFactory().create(name);
    }

    protected final boolean isReferenceIndex( String refPropName ) {
        assert refPropName != null;
        if (defn.appliesToProperty(refPropName)) {
            // The definition does apply to this property ...
            Name columnName = name(refPropName);
            return nodeTypes().isReferenceProperty(indexedNodeTypeName, columnName);
        }
        return false;
    }

    protected final boolean isReferenceIndex() {
        for (IndexColumnDefinition columnDefn : defn) {
            Name columnName = name(columnDefn.getPropertyName());
            if (nodeTypes().isReferenceProperty(indexedNodeTypeName, columnName)) return true;
        }
        return false;
    }
}
