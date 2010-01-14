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
package org.modeshape.jcr.xpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Order;

/**
 * Abstract syntax components of an XPath query. The supported grammar is defined by JCR 1.0, and is a subset of what is allowed
 * by the W3C XPath 2.0 specification.
 * 
 * @see XPathParser#parseXPath(String)
 */
public class XPath {

    public static enum NodeComparisonOperator {
        IS,
        PRECEDES,
        FOLLOWS;
    }

    public static abstract class Component {
        /**
         * Return the collapsable form
         * 
         * @return the collapsed form of th is component; never null and possibly the same as this
         */
        public Component collapse() {
            return this;
        }
    }

    public static abstract class UnaryComponent extends Component {
        protected final Component wrapped;

        public UnaryComponent( Component wrapped ) {
            this.wrapped = wrapped;
        }
    }

    public static class Negation extends UnaryComponent {
        public Negation( Component wrapped ) {
            super(wrapped);
        }

        public Component getNegated() {
            return wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "-" + wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Negation) {
                Negation that = (Negation)obj;
                return this.wrapped.equals(that.wrapped);
            }
            return false;
        }
    }

    public static abstract class BinaryComponent extends Component {
        private final Component left;
        private final Component right;

        public BinaryComponent( Component left,
                                Component right ) {
            this.left = left;
            this.right = right;
        }

        /**
         * @return left
         */
        public Component getLeft() {
            return left;
        }

        /**
         * @return right
         */
        public Component getRight() {
            return right;
        }
    }

    public static class Comparison extends BinaryComponent {
        private final Operator operator;

        public Comparison( Component left,
                           Operator operator,
                           Component right ) {
            super(left, right);
            this.operator = operator;
        }

        /**
         * @return operator
         */
        public Operator getOperator() {
            return operator;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new Comparison(getLeft().collapse(), operator, getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Comparison) {
                Comparison that = (Comparison)obj;
                if (this.operator != that.operator) return false;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class NodeComparison extends BinaryComponent {
        private final NodeComparisonOperator operator;

        public NodeComparison( Component left,
                               NodeComparisonOperator operator,
                               Component right ) {
            super(left, right);
            this.operator = operator;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new NodeComparison(getLeft().collapse(), operator, getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NodeComparison) {
                NodeComparison that = (NodeComparison)obj;
                if (this.operator != that.operator) return false;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Add extends BinaryComponent {
        public Add( Component left,
                    Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new Add(getLeft().collapse(), getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " + " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Add) {
                Add that = (Add)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Subtract extends BinaryComponent {
        public Subtract( Component left,
                         Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new Subtract(getLeft().collapse(), getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " - " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Subtract) {
                Subtract that = (Subtract)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class And extends BinaryComponent {
        public And( Component left,
                    Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new And(getLeft().collapse(), getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " and " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof And) {
                And that = (And)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Union extends BinaryComponent {
        public Union( Component left,
                      Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " union " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Union) {
                Union that = (Union)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Intersect extends BinaryComponent {
        public Intersect( Component left,
                          Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " intersect " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Intersect) {
                Intersect that = (Intersect)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Except extends BinaryComponent {
        public Except( Component left,
                       Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " except " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Except) {
                Except that = (Except)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Or extends BinaryComponent {
        public Or( Component left,
                   Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return new Or(getLeft().collapse(), getRight().collapse());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " or " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Or) {
                Or that = (Or)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class ContextItem extends Component {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof ContextItem;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return ".";
        }
    }

    public static class Literal extends Component {
        private final String value;

        public Literal( String value ) {
            this.value = value;
        }

        /**
         * @return value
         */
        public String getValue() {
            return value;
        }

        public boolean isInteger() {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public int getValueAsInteger() {
            return Integer.parseInt(value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Literal) {
                return this.value.equals(((Literal)obj).value);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    public static class FunctionCall extends Component {
        private final NameTest name;
        private final List<Component> arguments;

        public FunctionCall( NameTest name,
                             List<Component> arguments ) {
            assert name != null;
            assert arguments != null;
            this.name = name;
            this.arguments = arguments;
        }

        /**
         * @return name
         */
        public NameTest getName() {
            return name;
        }

        /**
         * @return arguments
         */
        public List<Component> getParameters() {
            return arguments;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            List<Component> args = new ArrayList<Component>(arguments.size());
            for (Component arg : arguments) {
                args.add(arg.collapse());
            }
            return new FunctionCall(name, args);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FunctionCall) {
                FunctionCall that = (FunctionCall)obj;
                return this.name.equals(that.name) && this.arguments.equals(that.arguments);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return name + "(" + asString(arguments, ",") + ")";
        }
    }

    protected static String asString( Iterable<?> components,
                                      String delimiter ) {
        StringBuilder sb = new StringBuilder();
        for (Object component : components) {
            if (sb.length() != 0) sb.append(delimiter);
            sb.append(component);
        }
        return sb.toString();
    }

    public static class PathExpression extends Component implements Iterable<StepExpression> {
        private final List<StepExpression> steps;
        private final boolean relative;
        private final OrderBy orderBy;

        public PathExpression( boolean relative,
                               List<StepExpression> steps,
                               OrderBy orderBy ) {
            this.steps = steps;
            this.relative = relative;
            this.orderBy = orderBy;
        }

        /**
         * @return relative
         */
        public boolean isRelative() {
            return relative;
        }

        /**
         * Get the order-by clause.
         * 
         * @return the order-by clause, or null if there is no such clause
         */
        public OrderBy getOrderBy() {
            return orderBy;
        }

        /**
         * @return steps
         */
        public List<StepExpression> getSteps() {
            return steps;
        }

        public StepExpression getLastStep() {
            return steps.isEmpty() ? null : steps.get(steps.size() - 1);
        }

        public PathExpression withoutLast() {
            assert !steps.isEmpty();
            return new PathExpression(relative, steps.subList(0, steps.size() - 1), orderBy);
        }

        public PathExpression withoutFirst() {
            assert !steps.isEmpty();
            return new PathExpression(relative, steps.subList(1, steps.size()), orderBy);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<StepExpression> iterator() {
            return steps.iterator();
        }

        @Override
        public Component collapse() {
            return steps.size() == 1 ? steps.get(0).collapse() : this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathExpression) {
                PathExpression that = (PathExpression)obj;
                if (this.relative != that.relative) return false;
                if (this.orderBy != null && !this.orderBy.equals(that.orderBy)) return false;
                if (this.orderBy == null && that.orderBy != null) return false;
                return this.steps.equals(that.steps);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return (relative ? "" : "/") + asString(steps, "/");
        }
    }

    public static abstract class StepExpression extends Component {
    }

    public static class FilterStep extends StepExpression {
        private final Component primaryExpression;
        private final List<Component> predicates;

        public FilterStep( Component primaryExpression,
                           List<Component> predicates ) {
            assert primaryExpression != null;
            assert predicates != null;
            this.primaryExpression = primaryExpression;
            this.predicates = predicates;
        }

        /**
         * @return nodeTest
         */
        public Component getPrimaryExpression() {
            return primaryExpression;
        }

        /**
         * @return predicates
         */
        public List<Component> getPredicates() {
            return predicates;
        }

        @Override
        public Component collapse() {
            return predicates.isEmpty() ? primaryExpression.collapse() : this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FilterStep) {
                FilterStep that = (FilterStep)obj;
                return this.primaryExpression.equals(that.primaryExpression) && this.predicates.equals(that.predicates);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return primaryExpression + (predicates.isEmpty() ? "" : predicates.toString());
        }
    }

    public static class DescendantOrSelf extends StepExpression {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof DescendantOrSelf;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "descendant-or-self::node()";
        }
    }

    public static class AxisStep extends StepExpression {
        private final NodeTest nodeTest;
        private final List<Component> predicates;

        public AxisStep( NodeTest nodeTest,
                         List<Component> predicates ) {
            assert nodeTest != null;
            assert predicates != null;
            this.nodeTest = nodeTest;
            this.predicates = predicates;
        }

        /**
         * @return nodeTest
         */
        public NodeTest getNodeTest() {
            return nodeTest;
        }

        /**
         * @return predicates
         */
        public List<Component> getPredicates() {
            return predicates;
        }

        @Override
        public Component collapse() {
            return predicates.isEmpty() ? nodeTest.collapse() : this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AxisStep) {
                AxisStep that = (AxisStep)obj;
                return this.nodeTest.equals(that.nodeTest) && this.predicates.equals(that.predicates);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return nodeTest + (predicates.isEmpty() ? "" : predicates.toString());
        }
    }

    public static class ParenthesizedExpression extends Component {
        private final Component wrapped;

        public ParenthesizedExpression() {
            this.wrapped = null;
        }

        public ParenthesizedExpression( Component wrapped ) {
            this.wrapped = wrapped; // may be null
        }

        /**
         * @return wrapped
         */
        public Component getWrapped() {
            return wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return wrapped instanceof BinaryComponent ? this : wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ParenthesizedExpression) {
                ParenthesizedExpression that = (ParenthesizedExpression)obj;
                return this.wrapped.equals(that.wrapped);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "(" + wrapped + ")";
        }
    }

    public static abstract class NodeTest extends Component {
    }

    public static abstract class KindTest extends NodeTest {
    }

    public static class NameTest extends NodeTest {
        private final String prefixTest;
        private final String localTest;

        public NameTest( String prefixTest,
                         String localTest ) {
            this.prefixTest = prefixTest;
            this.localTest = localTest;
        }

        /**
         * @return the prefix criteria, or null if the prefix criteria is a wildcard
         */
        public String getPrefixTest() {
            return prefixTest;
        }

        /**
         * @return the local name criteria, or null if the local name criteria is a wildcard
         */
        public String getLocalTest() {
            return localTest;
        }

        /**
         * Determine if this name test exactly matches the supplied prefix and local name values.
         * 
         * @param prefix the prefix; may be null
         * @param local the local name; may be null
         * @return true if this name matches the supplied values, or false otherwise.
         */
        public boolean matches( String prefix,
                                String local ) {
            if (this.prefixTest != null && !this.prefixTest.equals(prefix)) return false;
            if (this.localTest != null && !this.localTest.equals(local)) return false;
            return true;
        }

        /**
         * Return whether this represents a wildcard, meaning both {@link #getPrefixTest()} and {@link #getLocalTest()} are null.
         * 
         * @return true if this is a wildcard name test.
         */
        public boolean isWildcard() {
            return prefixTest == null && localTest == null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return HashCode.compute(this.prefixTest, this.localTest);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NameTest) {
                NameTest that = (NameTest)obj;
                return ObjectUtil.isEqualWithNulls(this.prefixTest, that.prefixTest)
                       && ObjectUtil.isEqualWithNulls(this.localTest, that.localTest);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            String local = localTest != null ? localTest : "*";
            return prefixTest == null ? local : (prefixTest + ":" + local);
        }
    }

    public static class AttributeNameTest extends NodeTest {
        private final NameTest nameTest;

        public AttributeNameTest( NameTest nameTest ) {
            this.nameTest = nameTest;
        }

        /**
         * @return nodeTest
         */
        public NameTest getNameTest() {
            return nameTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see XPath.NameTest#toString()
         */
        @Override
        public String toString() {
            return "@" + nameTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AttributeNameTest) {
                AttributeNameTest that = (AttributeNameTest)obj;
                return this.nameTest.equals(that.nameTest);
            }
            return false;
        }

    }

    public static class AnyKindTest extends KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof AnyKindTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "node()";
        }
    }

    public static class TextTest extends KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof TextTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "text()";
        }
    }

    public static class CommentTest extends KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof CommentTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "comment()";
        }
    }

    public static class ProcessingInstructionTest extends KindTest {
        private final String nameOrStringLiteral;

        public ProcessingInstructionTest( String nameOrStringLiteral ) {
            this.nameOrStringLiteral = nameOrStringLiteral;
        }

        /**
         * @return nameOrStringLiteral
         */
        public String getNameOrStringLiteral() {
            return nameOrStringLiteral;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ProcessingInstructionTest) {
                ProcessingInstructionTest that = (ProcessingInstructionTest)obj;
                return this.nameOrStringLiteral.equals(that.nameOrStringLiteral);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "processing-instruction(" + nameOrStringLiteral + ")";
        }
    }

    public static class DocumentTest extends KindTest {
        private KindTest elementOrSchemaElementTest;

        public DocumentTest( ElementTest elementTest ) {
            CheckArg.isNotNull(elementTest, "elementTest");
            this.elementOrSchemaElementTest = elementTest;
        }

        public DocumentTest( SchemaElementTest schemaElementTest ) {
            CheckArg.isNotNull(schemaElementTest, "schemaElementTest");
            this.elementOrSchemaElementTest = schemaElementTest;
        }

        /**
         * @return elementOrSchemaElementTest
         */
        public ElementTest getElementTest() {
            return elementOrSchemaElementTest instanceof ElementTest ? (ElementTest)elementOrSchemaElementTest : null;
        }

        /**
         * @return elementOrSchemaElementTest
         */
        public SchemaElementTest getSchemaElementTest() {
            return elementOrSchemaElementTest instanceof SchemaElementTest ? (SchemaElementTest)elementOrSchemaElementTest : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof DocumentTest) {
                DocumentTest that = (DocumentTest)obj;
                return this.elementOrSchemaElementTest.equals(that.elementOrSchemaElementTest);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "document-node(" + elementOrSchemaElementTest + ")";
        }
    }

    public static class AttributeTest extends KindTest {
        private final NameTest attributeNameOrWildcard;
        private final NameTest typeName;

        public AttributeTest( NameTest attributeNameOrWildcard,
                              NameTest typeName ) {
            this.attributeNameOrWildcard = attributeNameOrWildcard;
            this.typeName = typeName;
        }

        /**
         *@return the attribute name, which may be a wilcard
         */
        public NameTest getAttributeName() {
            return attributeNameOrWildcard;
        }

        /**
         * @return typeName
         */
        public NameTest getTypeName() {
            return typeName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AttributeTest) {
                AttributeTest that = (AttributeTest)obj;
                return ObjectUtil.isEqualWithNulls(this.typeName, that.typeName)
                       && ObjectUtil.isEqualWithNulls(this.attributeNameOrWildcard, that.attributeNameOrWildcard);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "attribute(" + attributeNameOrWildcard + (typeName != null ? "," + typeName : "") + ")";
        }
    }

    public static class ElementTest extends KindTest {
        private final NameTest elementNameOrWildcard;
        private final NameTest typeName;

        public ElementTest( NameTest elementNameOrWildcard,
                            NameTest typeName ) {
            this.elementNameOrWildcard = elementNameOrWildcard;
            this.typeName = typeName;
        }

        /**
         *@return the element name, which may be a wilcard
         */
        public NameTest getElementName() {
            return elementNameOrWildcard;
        }

        /**
         * @return typeName
         */
        public NameTest getTypeName() {
            return typeName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ElementTest) {
                ElementTest that = (ElementTest)obj;
                return ObjectUtil.isEqualWithNulls(this.typeName, that.typeName)
                       && ObjectUtil.isEqualWithNulls(this.elementNameOrWildcard, that.elementNameOrWildcard);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "element(" + elementNameOrWildcard + (typeName != null ? "," + typeName : "") + ")";
        }
    }

    public static class SchemaElementTest extends KindTest {
        private final NameTest elementDeclarationName;

        public SchemaElementTest( NameTest elementDeclarationName ) {
            this.elementDeclarationName = elementDeclarationName;
        }

        /**
         *@return the element declaration name, which will be a qualified name
         */
        public NameTest getElementDeclarationName() {
            return elementDeclarationName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaElementTest) {
                SchemaElementTest that = (SchemaElementTest)obj;
                return this.elementDeclarationName.equals(that.elementDeclarationName);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "schema-element(" + elementDeclarationName + ")";
        }
    }

    public static class SchemaAttributeTest extends KindTest {
        private final NameTest attributeDeclarationName;

        public SchemaAttributeTest( NameTest attributeDeclarationName ) {
            this.attributeDeclarationName = attributeDeclarationName;
        }

        /**
         *@return the attribute declaration name, which will be a qualified name
         */
        public NameTest getAttributeDeclarationName() {
            return attributeDeclarationName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaAttributeTest) {
                SchemaAttributeTest that = (SchemaAttributeTest)obj;
                return this.attributeDeclarationName.equals(that.attributeDeclarationName);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "schema-attribute(" + attributeDeclarationName + ")";
        }
    }

    public static class OrderBy extends Component implements Iterable<OrderBySpec> {
        private final List<OrderBySpec> orderBySpecifications;

        public OrderBy( List<OrderBySpec> orderBySpecifications ) {
            this.orderBySpecifications = orderBySpecifications;
        }

        /**
         *@return the list of order-by specifications; never null
         */
        public List<OrderBySpec> getOrderBySpecifications() {
            return orderBySpecifications;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.xpath.XPath.Component#collapse()
         */
        @Override
        public Component collapse() {
            return super.collapse();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<OrderBySpec> iterator() {
            return new ReadOnlyIterator<OrderBySpec>(orderBySpecifications.iterator());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof OrderBy) {
                OrderBy that = (OrderBy)obj;
                return this.orderBySpecifications.equals(that.orderBySpecifications);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("order-by(");
            boolean first = true;
            for (OrderBySpec spec : orderBySpecifications) {
                if (first) first = false;
                else sb.append(',');
                sb.append(spec);
            }
            sb.append(')');
            return sb.toString();
        }
    }

    public static class OrderBySpec {
        private final Order order;
        private final FunctionCall scoreFunction;
        private final NameTest attributeName;

        public OrderBySpec( Order order,
                            FunctionCall scoreFunction ) {
            assert order != null;
            assert scoreFunction != null;
            this.order = order;
            this.scoreFunction = scoreFunction;
            this.attributeName = null;
        }

        public OrderBySpec( Order order,
                            NameTest attributeName ) {
            assert order != null;
            assert attributeName != null;
            this.order = order;
            this.scoreFunction = null;
            this.attributeName = attributeName;
        }

        /**
         * Get the attribute name for this order specification.
         * 
         * @return the attribute name, or null if the order is defined by the {@link #getScoreFunction() score function}
         */
        public NameTest getAttributeName() {
            return attributeName;
        }

        /**
         * Get the score function for this order specification.
         * 
         * @return the score function with its parameters, or null if the order is defined by the {@link #getAttributeName()
         *         attribute name}
         */
        public FunctionCall getScoreFunction() {
            return scoreFunction;
        }

        /**
         * The order for this specification
         * 
         * @return the order; never null
         */
        public Order getOrder() {
            return order;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof OrderBySpec) {
                OrderBySpec that = (OrderBySpec)obj;
                if (this.order != that.order) return false;
                if (this.attributeName != null && !this.attributeName.equals(that.attributeName)) return false;
                if (this.scoreFunction != null && !this.scoreFunction.equals(that.scoreFunction)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (scoreFunction != null) {
                sb.append(scoreFunction.toString());
            } else {
                sb.append('@').append(attributeName.toString());
            }
            switch (order) {
                case ASCENDING:
                    sb.append(" ascending");
                    break;
                case DESCENDING:
                    sb.append(" descending");
                    break;
            }
            return sb.toString();
        }
    }
}
