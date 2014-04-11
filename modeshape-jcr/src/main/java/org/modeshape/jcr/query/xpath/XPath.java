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
package org.modeshape.jcr.query.xpath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.model.Order;

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

        @Override
        public String toString() {
            return "-" + wrapped;
        }

        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }

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

        public Component getLeft() {
            return left;
        }

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

        public Operator getOperator() {
            return operator;
        }

        @Override
        public Component collapse() {
            return new Comparison(getLeft().collapse(), operator, getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(operator, getLeft(), getRight());
        }

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

        @Override
        public Component collapse() {
            return new NodeComparison(getLeft().collapse(), operator, getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(operator, getLeft(), getRight());
        }

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

        @Override
        public Component collapse() {
            return new Add(getLeft().collapse(), getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " + " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public Component collapse() {
            return new Subtract(getLeft().collapse(), getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " - " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public Component collapse() {
            return new And(getLeft().collapse(), getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " and " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public String toString() {
            return getLeft() + " union " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public String toString() {
            return getLeft() + " intersect " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public String toString() {
            return getLeft() + " except " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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

        @Override
        public Component collapse() {
            return new Or(getLeft().collapse(), getRight().collapse());
        }

        @Override
        public String toString() {
            return getLeft() + " or " + getRight();
        }

        @Override
        public int hashCode() {
            return HashCode.compute(getLeft(), getRight());
        }

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
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof ContextItem;
        }

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

        @Override
        public int hashCode() {
            return HashCode.compute(value);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Literal) {
                return this.value.equals(((Literal)obj).value);
            }
            return false;
        }

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

        public NameTest getName() {
            return name;
        }

        public List<Component> getParameters() {
            return arguments;
        }

        @Override
        public Component collapse() {
            List<Component> args = new ArrayList<Component>(arguments.size());
            for (Component arg : arguments) {
                args.add(arg.collapse());
            }
            return new FunctionCall(name, args);
        }

        @Override
        public int hashCode() {
            return HashCode.compute(name, arguments);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FunctionCall) {
                FunctionCall that = (FunctionCall)obj;
                return this.name.equals(that.name) && this.arguments.equals(that.arguments);
            }
            return false;
        }

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

        @Override
        public Iterator<StepExpression> iterator() {
            return steps.iterator();
        }

        @Override
        public Component collapse() {
            return steps.size() == 1 ? steps.get(0).collapse() : this;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(relative, orderBy, steps);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathExpression) {
                PathExpression that = (PathExpression)obj;
                return this.relative == that.relative && ObjectUtil.isEqualWithNulls(this.orderBy, that.orderBy)
                       && this.steps.equals(that.steps);
            }
            return false;
        }

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

        public Component getPrimaryExpression() {
            return primaryExpression;
        }

        public List<Component> getPredicates() {
            return predicates;
        }

        @Override
        public Component collapse() {
            return predicates.isEmpty() ? primaryExpression.collapse() : this;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(primaryExpression, predicates);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FilterStep) {
                FilterStep that = (FilterStep)obj;
                return this.primaryExpression.equals(that.primaryExpression) && this.predicates.equals(that.predicates);
            }
            return false;
        }

        @Override
        public String toString() {
            return primaryExpression + (predicates.isEmpty() ? "" : predicates.toString());
        }
    }

    public static class DescendantOrSelf extends StepExpression {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof DescendantOrSelf;
        }

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

        public NodeTest getNodeTest() {
            return nodeTest;
        }

        public List<Component> getPredicates() {
            return predicates;
        }

        @Override
        public Component collapse() {
            return predicates.isEmpty() ? nodeTest.collapse() : this;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(nodeTest, predicates);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AxisStep) {
                AxisStep that = (AxisStep)obj;
                return this.nodeTest.equals(that.nodeTest) && this.predicates.equals(that.predicates);
            }
            return false;
        }

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

        public Component getWrapped() {
            return wrapped;
        }

        @Override
        public Component collapse() {
            return wrapped instanceof BinaryComponent ? this : wrapped;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(wrapped);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ParenthesizedExpression) {
                ParenthesizedExpression that = (ParenthesizedExpression)obj;
                return this.wrapped.equals(that.wrapped);
            }
            return false;
        }

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

        @Override
        public int hashCode() {
            return HashCode.compute(this.prefixTest, this.localTest);
        }

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

        public NameTest getNameTest() {
            return nameTest;
        }

        @Override
        public String toString() {
            return "@" + nameTest;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(nameTest);
        }

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
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof AnyKindTest;
        }

        @Override
        public String toString() {
            return "node()";
        }
    }

    public static class TextTest extends KindTest {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof TextTest;
        }

        @Override
        public String toString() {
            return "text()";
        }
    }

    public static class CommentTest extends KindTest {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof CommentTest;
        }

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

        public String getNameOrStringLiteral() {
            return nameOrStringLiteral;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(nameOrStringLiteral);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ProcessingInstructionTest) {
                ProcessingInstructionTest that = (ProcessingInstructionTest)obj;
                return this.nameOrStringLiteral.equals(that.nameOrStringLiteral);
            }
            return false;
        }

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

        public ElementTest getElementTest() {
            return elementOrSchemaElementTest instanceof ElementTest ? (ElementTest)elementOrSchemaElementTest : null;
        }

        public SchemaElementTest getSchemaElementTest() {
            return elementOrSchemaElementTest instanceof SchemaElementTest ? (SchemaElementTest)elementOrSchemaElementTest : null;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(elementOrSchemaElementTest);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof DocumentTest) {
                DocumentTest that = (DocumentTest)obj;
                return this.elementOrSchemaElementTest.equals(that.elementOrSchemaElementTest);
            }
            return false;
        }

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
         * @return the attribute name, which may be a wilcard
         */
        public NameTest getAttributeName() {
            return attributeNameOrWildcard;
        }

        public NameTest getTypeName() {
            return typeName;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(typeName, attributeNameOrWildcard);
        }

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
         * @return the element name, which may be a wilcard
         */
        public NameTest getElementName() {
            return elementNameOrWildcard;
        }

        public NameTest getTypeName() {
            return typeName;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(typeName, elementNameOrWildcard);
        }

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
         * @return the element declaration name, which will be a qualified name
         */
        public NameTest getElementDeclarationName() {
            return elementDeclarationName;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(elementDeclarationName);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaElementTest) {
                SchemaElementTest that = (SchemaElementTest)obj;
                return this.elementDeclarationName.equals(that.elementDeclarationName);
            }
            return false;
        }

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
         * @return the attribute declaration name, which will be a qualified name
         */
        public NameTest getAttributeDeclarationName() {
            return attributeDeclarationName;
        }

        @Override
        public int hashCode() {
            return HashCode.compute(attributeDeclarationName);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaAttributeTest) {
                SchemaAttributeTest that = (SchemaAttributeTest)obj;
                return this.attributeDeclarationName.equals(that.attributeDeclarationName);
            }
            return false;
        }

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
         * @return the list of order-by specifications; never null
         */
        public List<OrderBySpec> getOrderBySpecifications() {
            return orderBySpecifications;
        }

        @Override
        public Component collapse() {
            return super.collapse();
        }

        @Override
        public Iterator<OrderBySpec> iterator() {
            return ReadOnlyIterator.around(orderBySpecifications.iterator());
        }

        @Override
        public int hashCode() {
            return HashCode.compute(orderBySpecifications);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof OrderBy) {
                OrderBy that = (OrderBy)obj;
                return this.orderBySpecifications.equals(that.orderBySpecifications);
            }
            return false;
        }

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
        private final PathExpression path;

        public OrderBySpec( Order order,
                            FunctionCall scoreFunction ) {
            assert order != null;
            assert scoreFunction != null;
            this.order = order;
            this.scoreFunction = scoreFunction;
            this.attributeName = null;
            this.path = null;
        }

        public OrderBySpec( Order order,
                            NameTest attributeName ) {
            assert order != null;
            assert attributeName != null;
            this.order = order;
            this.scoreFunction = null;
            this.attributeName = attributeName;
            this.path = null;
        }

        public OrderBySpec( Order order,
                            PathExpression path ) {
            this.order = order;
            this.scoreFunction = null;
            this.attributeName = null;
            this.path = path;
        }

        /**
         * Gets child axis for this order specification.
         * 
         * @return child axis node or null if order is defined by an attribute or score function.
         */
        public PathExpression getPath() {
            return path;
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

        @Override
        public int hashCode() {
            return HashCode.compute(order);
        }

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
