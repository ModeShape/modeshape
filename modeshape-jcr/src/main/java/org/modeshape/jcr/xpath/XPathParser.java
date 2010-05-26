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

import static org.modeshape.common.text.TokenStream.ANY_VALUE;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.text.TokenStream.CharacterStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.text.TokenStream.Tokens;
import org.modeshape.common.xml.XmlCharacters;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.jcr.xpath.XPath.Add;
import org.modeshape.jcr.xpath.XPath.And;
import org.modeshape.jcr.xpath.XPath.AnyKindTest;
import org.modeshape.jcr.xpath.XPath.AttributeNameTest;
import org.modeshape.jcr.xpath.XPath.AttributeTest;
import org.modeshape.jcr.xpath.XPath.AxisStep;
import org.modeshape.jcr.xpath.XPath.CommentTest;
import org.modeshape.jcr.xpath.XPath.Comparison;
import org.modeshape.jcr.xpath.XPath.Component;
import org.modeshape.jcr.xpath.XPath.ContextItem;
import org.modeshape.jcr.xpath.XPath.DescendantOrSelf;
import org.modeshape.jcr.xpath.XPath.DocumentTest;
import org.modeshape.jcr.xpath.XPath.ElementTest;
import org.modeshape.jcr.xpath.XPath.Except;
import org.modeshape.jcr.xpath.XPath.FilterStep;
import org.modeshape.jcr.xpath.XPath.FunctionCall;
import org.modeshape.jcr.xpath.XPath.Intersect;
import org.modeshape.jcr.xpath.XPath.KindTest;
import org.modeshape.jcr.xpath.XPath.Literal;
import org.modeshape.jcr.xpath.XPath.NameTest;
import org.modeshape.jcr.xpath.XPath.Negation;
import org.modeshape.jcr.xpath.XPath.NodeComparison;
import org.modeshape.jcr.xpath.XPath.NodeComparisonOperator;
import org.modeshape.jcr.xpath.XPath.NodeTest;
import org.modeshape.jcr.xpath.XPath.Or;
import org.modeshape.jcr.xpath.XPath.OrderBy;
import org.modeshape.jcr.xpath.XPath.OrderBySpec;
import org.modeshape.jcr.xpath.XPath.ParenthesizedExpression;
import org.modeshape.jcr.xpath.XPath.PathExpression;
import org.modeshape.jcr.xpath.XPath.ProcessingInstructionTest;
import org.modeshape.jcr.xpath.XPath.SchemaAttributeTest;
import org.modeshape.jcr.xpath.XPath.SchemaElementTest;
import org.modeshape.jcr.xpath.XPath.StepExpression;
import org.modeshape.jcr.xpath.XPath.Subtract;
import org.modeshape.jcr.xpath.XPath.TextTest;
import org.modeshape.jcr.xpath.XPath.Union;

/**
 * A component that parses an XPath query string and creates an abstract syntax tree representation. The supported grammar is
 * defined by JCR 1.0, and is a subset of what is allowed by the W3C XPath 2.0 specification.
 */
public class XPathParser {
    private final TypeSystem typeSystem;
    private final XmlNameEncoder decoder = new XmlNameEncoder();

    public XPathParser( TypeSystem context ) {
        this.typeSystem = context;
    }

    public Component parseXPath( String xpath ) {
        Tokenizer tokenizer = new XPathTokenizer(false); // skip comments
        TokenStream tokens = new TokenStream(xpath, tokenizer, true).start(); // case sensitive!!
        return parseXPath(tokens);
    }

    protected Component parseXPath( TokenStream tokens ) {
        return parseExpr(tokens);
    }

    protected Component parseExpr( TokenStream tokens ) {
        Component result = parseExprSingle(tokens);
        if (tokens.matches(',')) {
            throw new ParsingException(tokens.nextPosition(), "Multiple XPath expressions are not supported");
        }
        return result;
    }

    protected Component parseExprSingle( TokenStream tokens ) {
        if (tokens.matches("for", "$", ANY_VALUE, "IN")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'for' expressions are not supported");
        }
        if (tokens.matches("some", "$", ANY_VALUE, "IN")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'some' expressions are not supported");
        }
        if (tokens.matches("every", "$", ANY_VALUE, "IN")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'every' expressions are not supported");
        }
        if (tokens.matches("if", "(", ANY_VALUE, "IN")) {
            throw new ParsingException(tokens.nextPosition(), "XPath if-then-else expressions are not supported");
        }
        return parseOrExpr(tokens);
    }

    protected Component parseOrExpr( TokenStream tokens ) {
        Component result = parseAndExpr(tokens);
        while (tokens.canConsume("or")) {
            result = new Or(result, parseInstanceofExpr(tokens));
        }
        return result;
    }

    protected Component parseAndExpr( TokenStream tokens ) {
        Component result = parseInstanceofExpr(tokens);
        while (tokens.canConsume("and")) {
            result = new And(result, parseInstanceofExpr(tokens));
        }
        return result;
    }

    protected Component parseInstanceofExpr( TokenStream tokens ) {
        Component result = parseTreatExpr(tokens);
        if (tokens.matches("instance", "of")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'instance of' expressions are not supported");
        }
        return result;
    }

    protected Component parseTreatExpr( TokenStream tokens ) {
        Component result = parseCastableExpr(tokens);
        if (tokens.matches("treat", "as")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'treat as' expressions are not supported");
        }
        return result;
    }

    protected Component parseCastableExpr( TokenStream tokens ) {
        Component result = parseCastExpr(tokens);
        if (tokens.matches("castable", "as")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'castable as' expressions are not supported");
        }
        return result;
    }

    protected Component parseCastExpr( TokenStream tokens ) {
        Component result = parseComparisonExpr(tokens);
        if (tokens.matches("cast", "as")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'cast as' expressions are not supported");
        }
        return result;
    }

    protected Component parseComparisonExpr( TokenStream tokens ) {
        Component result = parseRangeExpr(tokens);
        // General comparison is optional ...
        Operator operator = parseGeneralComp(tokens);
        if (operator == null) parseValueComp(tokens);
        if (operator != null) {
            return new Comparison(result, operator, parseRangeExpr(tokens));
        }
        NodeComparisonOperator nodeComp = parseNodeComp(tokens);
        if (nodeComp != null) {
            return new NodeComparison(result, nodeComp, parseRangeExpr(tokens));
        }
        return result;
    }

    protected Component parseValueComp( TokenStream tokens ) {
        if (tokens.matchesAnyOf("eq", "ne", "lt", "le", "gt")) {
            throw new ParsingException(tokens.nextPosition(),
                                       "XPath value comparisons using 'eq', 'ne', 'lt', 'le', or 'gt' are not supported");
        }
        return null;
    }

    protected NodeComparisonOperator parseNodeComp( TokenStream tokens ) {
        if (tokens.matches("is") || tokens.matches("<", "<") || tokens.matches(">", ">")) {
            throw new ParsingException(tokens.nextPosition(), "XPath 'is', '<<' and '>>' expressions are not supported");
        }
        return null;
    }

    protected Component parseRangeExpr( TokenStream tokens ) {
        Component result = parseAdditiveExpr(tokens);
        if (tokens.matches("to")) {
            throw new ParsingException(tokens.nextPosition(), "XPath range expressions with 'to' are not supported");
        }
        return result;
    }

    protected Component parseAdditiveExpr( TokenStream tokens ) {
        Component result = parseMultiplicativeExpr(tokens);
        while (true) {
            if (tokens.canConsume("+")) {
                result = new Add(result, parseMultiplicativeExpr(tokens));
            } else if (tokens.canConsume("-")) {
                result = new Subtract(result, parseMultiplicativeExpr(tokens));
            } else {
                break; // no more additions
            }
        }
        return result;
    }

    protected Component parseMultiplicativeExpr( TokenStream tokens ) {
        Component result = parseUnaryExpr(tokens);
        if (tokens.matchesAnyOf("+", "div", "idiv", "mod")) {
            throw new ParsingException(tokens.nextPosition(),
                                       "XPath multiplicative expressions using '+', 'div', 'idiv', or 'mod' are not supported");
        }
        return result;
    }

    protected Component parseUnaryExpr( TokenStream tokens ) {
        boolean negative = false;
        // Technically more than one +/- are allowed by the spec
        while (tokens.matchesAnyOf("+", "-")) {
            if (tokens.canConsume("-")) negative = true;
            tokens.canConsume("+");
        }
        Component result = parseUnionExpr(tokens);
        return negative ? new Negation(result) : result;
    }

    protected Component parseUnionExpr( TokenStream tokens ) {
        Component result = parseIntersectExceptExpr(tokens);
        while (true) {
            if (tokens.canConsumeAnyOf("union", "|")) {
                result = new Union(result, parseIntersectExceptExpr(tokens));
            } else {
                break; // no more
            }
        }
        return result;
    }

    protected Component parseIntersectExceptExpr( TokenStream tokens ) {
        Component result = parseValueExpr(tokens);
        while (true) {
            if (tokens.canConsumeAnyOf("intersect")) {
                result = new Intersect(result, parseValueExpr(tokens));
            } else if (tokens.canConsumeAnyOf("except")) {
                result = new Except(result, parseValueExpr(tokens));
            } else {
                break; // no more
            }
        }
        return result;
    }

    protected Component parseValueExpr( TokenStream tokens ) {
        return parsePathExpr(tokens);
    }

    protected PathExpression parsePathExpr( TokenStream tokens ) {
        boolean relative = true;
        boolean prependDependentOrSelf = false;
        if (tokens.canConsume('/')) {
            if (tokens.canConsume('/')) {
                if (!tokens.hasNext()) {
                    // See http://www.w3.org/XML/2007/qt-errata/xpath20-errata.html#E3
                    throw new ParsingException(tokens.previousPosition(), "'//' is not a valid XPath expression");
                }
                prependDependentOrSelf = true;
            }
            relative = false;
        }
        PathExpression relativeExpr = parseRelativePathExpr(tokens);
        PathExpression result = new PathExpression(relative, relativeExpr.getSteps(), relativeExpr.getOrderBy());
        if (prependDependentOrSelf) {
            result.getSteps().add(0, new DescendantOrSelf());
        }
        return result;
    }

    protected PathExpression parseRelativePathExpr( TokenStream tokens ) {
        List<StepExpression> steps = new ArrayList<StepExpression>();
        steps.add(parseStepExpr(tokens));
        while (tokens.canConsume('/')) {
            if (tokens.canConsume('/')) {
                steps.add(new DescendantOrSelf());
            }
            if (tokens.hasNext()) {
                steps.add(parseStepExpr(tokens));
            }
        }
        OrderBy orderBy = parseOrderBy(tokens); // may be null
        return new PathExpression(true, steps, orderBy);
    }

    protected StepExpression parseStepExpr( TokenStream tokens ) {
        KindTest kindTest = parseKindTest(tokens);
        if (kindTest != null) {
            // Now parse the predicates ...
            List<Component> predicates = parsePredicates(tokens);
            return new AxisStep(kindTest, predicates);
        }
        if (tokens.matches('(') || tokens.matches('.') || tokens.matches(XPathTokenizer.QUOTED_STRING)
            || tokens.matches(ANY_VALUE, "(") || tokens.matches(ANY_VALUE, ":", ANY_VALUE, "(")) {
            // We know its a filter expression (though literals don't fit this pattern) ...
            return parseFilterExpr(tokens);
        }
        AxisStep result = parseAxisStep(tokens);
        if (result != null) return result;
        // It must be the remaining kind of filter expression ...
        return parseFilterExpr(tokens);
    }

    protected AxisStep parseAxisStep( TokenStream tokens ) {
        NodeTest nodeTest = null;
        if (tokens.canConsume('@')) {
            // Abbreviated forward step with an attribute...
            nodeTest = new AttributeNameTest(parseNameTest(tokens));
        } else if (tokens.matches('*')) {
            // Abbreviated forward step with an wildcard element ...
            nodeTest = parseNodeTest(tokens);

        } else if (tokens.matches("child", ":", ":") || tokens.matches("attribute", ":", ":") || tokens.matches("self", ":", ":")
                   || tokens.matches("descendant", ":", ":") || tokens.matches("descendant-or-self", ":", ":")
                   || tokens.matches("following-sibling", ":", ":") || tokens.matches("following", ":", ":")
                   || tokens.matches("namespace", ":", ":")) {
            // No non-abbreviated forward steps allowed
            throw new ParsingException(
                                       tokens.nextPosition(),
                                       "XPath non-abbreviated forward steps (e.g., 'child::', 'attribute::', 'self::', 'descendant::', 'descendant-or-self::', 'following-sibling::', 'following::', or 'namespace::') are not supported");
        } else if (tokens.matches("..")) {
            // No abbreviated reverse steps allowed ...
            throw new ParsingException(tokens.nextPosition(), "XPath abbreviated reverse steps (e.g., '..') are not supported");
        } else if (tokens.matches("parent", ":", ":") || tokens.matches("ancestor-or-self", ":", ":")
                   || tokens.matches("preceding-sibling", ":", ":") || tokens.matches("preceding", ":", ":")
                   || tokens.matches("ancestor", ":", ":")) {
            // No non-abbreviated reverse steps allowed ...
            throw new ParsingException(
                                       tokens.nextPosition(),
                                       "XPath non-abbreviated reverse steps (e.g., 'parent::', 'ancestor::', 'ancestor-or-self::', 'preceding-or-sibling::', or 'preceding::') are not supported");
        } else if (tokens.matches(ANY_VALUE, ":", ANY_VALUE)
                   && tokens.matches(XPathTokenizer.NAME, XPathTokenizer.SYMBOL, XPathTokenizer.NAME)) {
            // This is probably a forward step with a (qualified) name test ...
            nodeTest = parseQName(tokens);
        } else if (tokens.matches(XPathTokenizer.NAME)) {
            // This is probably a forward step with an unqualified name test ...
            nodeTest = parseNodeTest(tokens);
        } else {
            return null;
        }

        // Parse the predicates
        List<Component> predicates = parsePredicates(tokens);
        return new AxisStep(nodeTest, predicates);
    }

    protected List<Component> parsePredicates( TokenStream tokens ) {
        List<Component> predicates = new ArrayList<Component>();
        while (tokens.canConsume('[')) {
            predicates.add(collapse(parseExpr(tokens)));
            tokens.consume(']');
        }
        return predicates;
    }

    protected FilterStep parseFilterExpr( TokenStream tokens ) {
        Component primaryExpr = parsePrimaryExpr(tokens);
        List<Component> predicates = parsePredicates(tokens);
        return new FilterStep(primaryExpr, predicates);
    }

    protected Component parsePrimaryExpr( TokenStream tokens ) {
        if (tokens.matches('(')) {
            return parseParenthesizedExpr(tokens);
        }
        if (tokens.matches('.')) {
            return parseContextItemExpr(tokens);
        }
        if (tokens.matches(XPathTokenizer.QUOTED_STRING)) {
            return parseStringLiteral(tokens);
        }
        if (tokens.matches(ANY_VALUE, "(") || tokens.matches(ANY_VALUE, ":", ANY_VALUE, "(")) {
            return parseFunctionCall(tokens);
        }
        return parseNumericLiteral(tokens);
    }

    protected ContextItem parseContextItemExpr( TokenStream tokens ) {
        tokens.consume('.');
        return new ContextItem();
    }

    protected ParenthesizedExpression parseParenthesizedExpr( TokenStream tokens ) {
        tokens.consume('(');
        if (tokens.canConsume(')')) {
            return new ParenthesizedExpression();
        }
        Component expr = collapse(parseExpr(tokens));
        tokens.consume(')');
        return new ParenthesizedExpression(expr);
    }

    protected Literal parseNumericLiteral( TokenStream tokens ) {
        Position pos = tokens.nextPosition();
        String sign = "";
        if (tokens.canConsume('-')) sign = "-";
        else if (tokens.canConsume('+')) sign = "";

        // Try to parse this value as a number ...
        String number = tokens.consume();
        if (number.indexOf(".") != -1) {
            String value = sign + number;
            if (value.endsWith("e") && (tokens.matches('+') || tokens.matches('-'))) {
                // There's more to the number ...
                value = value + tokens.consume() + tokens.consume(); // +/-EXP
            }
            try {
                // Convert to a double and then back to a string to get canonical form ...
                String canonical = typeSystem.getDoubleFactory().asString(value);
                return new Literal(canonical);
            } catch (ValueFormatException e) {
                String msg = GraphI18n.expectingLiteralAndUnableToParseAsDouble.text(value, pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
        }
        // try to parse an a long ...
        String value = sign + number;
        try {
            // Convert to a long and then back to a string to get canonical form ...
            String canonical = typeSystem.getLongFactory().asString(value);
            return new Literal(canonical);
        } catch (ValueFormatException e) {
            String msg = GraphI18n.expectingLiteralAndUnableToParseAsLong.text(value, pos.getLine(), pos.getColumn());
            throw new ParsingException(pos, msg);
        }
    }

    protected Literal parseStringLiteral( TokenStream tokens ) {
        boolean removeQuotes = tokens.matches(XPathTokenizer.QUOTED_STRING);
        String value = tokens.consume();
        if (removeQuotes) value = removeQuotes(value);
        return new Literal(value);
    }

    protected FunctionCall parseFunctionCall( TokenStream tokens ) {
        NameTest name = parseQName(tokens);
        tokens.consume("(");
        List<Component> args = new ArrayList<Component>();
        if (!tokens.matches(')')) {
            do {
                args.add(collapse(parseExprSingle(tokens)));
            } while (tokens.canConsume(","));
            tokens.consume(")");
        }
        return new FunctionCall(name, args);
    }

    protected Operator parseGeneralComp( TokenStream tokens ) {
        if (tokens.canConsume("!", "=")) return Operator.NOT_EQUAL_TO;
        if (tokens.canConsume("=")) return Operator.EQUAL_TO;
        if (tokens.canConsume("<", "=")) return Operator.LESS_THAN_OR_EQUAL_TO;
        if (tokens.canConsume(">", "=")) return Operator.GREATER_THAN_OR_EQUAL_TO;
        if (tokens.canConsume("<")) return Operator.LESS_THAN;
        if (tokens.canConsume(">")) return Operator.GREATER_THAN;
        return null;
    }

    protected NodeTest parseNodeTest( TokenStream tokens ) {
        KindTest kind = parseKindTest(tokens);
        if (kind != null) return kind;
        return parseNameTest(tokens);
    }

    protected NameTest parseNameTest( TokenStream tokens ) {
        NameTest wildcard = parseWildcard(tokens);
        if (wildcard != null) return wildcard;
        return parseQName(tokens);
    }

    protected NameTest parseQName( TokenStream tokens ) {
        String firstPart = parseNCName(tokens);
        if (tokens.canConsume(':')) {
            String secondPart = tokens.consume();
            return new NameTest(decode(firstPart), decode(secondPart));
        }
        return new NameTest(null, decode(firstPart));
    }

    protected String decode( String string ) {
        return decoder.decode(string);
    }

    protected String parseNCName( TokenStream tokens ) {
        String name = tokens.consume();
        if (!XmlCharacters.isValidNcName(name)) {
            throw new ParsingException(tokens.previousPosition(), "Expected valid NCName but found " + name);
        }
        return name;
    }

    protected NameTest parseWildcard( TokenStream tokens ) {
        if (tokens.canConsume('*')) {
            if (tokens.canConsume(':')) {
                if (tokens.canConsume('*')) {
                    return new NameTest(null, null);
                }
                String localName = tokens.consume();
                return new NameTest(null, decode(localName));
            }
            return new NameTest(null, null);
        }
        if (tokens.matches(XPathTokenizer.NAME, XPathTokenizer.SYMBOL, XPathTokenizer.SYMBOL)
            && tokens.matches(TokenStream.ANY_VALUE, ":", "*")) {
            String prefix = tokens.consume();
            tokens.consume(':');
            tokens.consume('*');
            return new NameTest(decode(prefix), null);
        }
        return null;
    }

    protected NameTest parseItemType( TokenStream tokens ) {
        return parseQName(tokens);
    }

    protected NameTest parseAtomicType( TokenStream tokens ) {
        return parseQName(tokens);
    }

    protected KindTest parseKindTest( TokenStream tokens ) {
        KindTest result = parseAnyKindTest(tokens);
        if (result == null) result = parseDocumentTest(tokens);
        if (result == null) result = parseElementTest(tokens);
        if (result == null) result = parseAttributeTest(tokens);
        if (result == null) result = parseSchemaElementTest(tokens);
        if (result == null) result = parseSchemaAttributeTest(tokens);
        if (result == null) result = parsePITest(tokens);
        if (result == null) result = parseCommentTest(tokens);
        if (result == null) result = parseTextTest(tokens);
        return result;
    }

    protected AnyKindTest parseAnyKindTest( TokenStream tokens ) {
        if (tokens.canConsume("node", "(", ")")) {
            return new AnyKindTest();
        }
        return null;
    }

    protected ProcessingInstructionTest parsePITest( TokenStream tokens ) {
        if (tokens.canConsume("processing-instruction", "(")) {
            if (tokens.canConsume(")")) return new ProcessingInstructionTest(null);
            String nameOrStringLiteral = tokens.consume();
            tokens.consume(")");
            return new ProcessingInstructionTest(nameOrStringLiteral);
        }
        return null;
    }

    protected CommentTest parseCommentTest( TokenStream tokens ) {
        if (tokens.canConsume("comment", "(", ")")) {
            return new CommentTest();
        }
        return null;
    }

    protected TextTest parseTextTest( TokenStream tokens ) {
        if (tokens.canConsume("text", "(", ")")) {
            return new TextTest();
        }
        return null;
    }

    protected DocumentTest parseDocumentTest( TokenStream tokens ) {
        if (tokens.canConsume("document-node", "(")) {
            // Document test ...
            ElementTest elementTest = parseElementTest(tokens);
            DocumentTest result = null;
            if (elementTest != null) {
                result = new DocumentTest(elementTest);
            } else {
                SchemaElementTest schemaTest = parseSchemaElementTest(tokens);
                result = schemaTest != null ? new DocumentTest(schemaTest) : null;
            }
            tokens.consume(")");
            return result;
        }
        return null;
    }

    protected ElementTest parseElementTest( TokenStream tokens ) {
        if (tokens.canConsume("element", "(")) {
            if (tokens.canConsume(")") || tokens.canConsume("*", ")")) {
                return new ElementTest(new NameTest(null, null), new NameTest(null, null));
            }
            ElementTest result = null;
            NameTest elementName = parseNameTest(tokens);
            if (tokens.canConsume(",")) {
                NameTest typeName = parseNameTest(tokens);
                result = new ElementTest(elementName, typeName);
                tokens.canConsume('?'); // just eat this
            } else {
                result = new ElementTest(elementName, new NameTest(null, null));
            }
            tokens.consume(")");
            return result;
        }
        return null;
    }

    protected SchemaElementTest parseSchemaElementTest( TokenStream tokens ) {
        if (tokens.canConsume("schema-element", "(")) {
            NameTest elementDeclarationName = parseNameTest(tokens);
            SchemaElementTest result = new SchemaElementTest(elementDeclarationName);
            tokens.consume(")");
            return result;
        }
        return null;
    }

    protected AttributeTest parseAttributeTest( TokenStream tokens ) {
        if (tokens.canConsume("attribute", "(")) {
            if (tokens.canConsume(")") || tokens.canConsume("*", ")")) {
                return new AttributeTest(new NameTest(null, null), new NameTest(null, null));
            }
            AttributeTest result = null;
            NameTest attributeName = parseNameTest(tokens);
            if (tokens.canConsume(",")) {
                NameTest typeName = parseNameTest(tokens);
                result = new AttributeTest(attributeName, typeName);
            } else {
                result = new AttributeTest(attributeName, new NameTest(null, null));
            }
            tokens.consume(")");
            return result;
        }
        return null;
    }

    protected SchemaAttributeTest parseSchemaAttributeTest( TokenStream tokens ) {
        if (tokens.canConsume("schema-attribute", "(")) {
            NameTest attributeDeclarationName = parseNameTest(tokens);
            SchemaAttributeTest result = new SchemaAttributeTest(attributeDeclarationName);
            tokens.consume(")");
            return result;
        }
        return null;
    }

    protected void parseSingleType( TokenStream tokens ) {
    }

    protected void parseSequenceType( TokenStream tokens ) {
    }

    protected OrderBy parseOrderBy( TokenStream tokens ) {
        if (tokens.canConsume("order", "by")) {
            List<OrderBySpec> specs = new ArrayList<OrderBySpec>();
            do {
                OrderBySpec spec = parseOrderBySpec(tokens);
                specs.add(spec);
            } while (tokens.canConsume(','));
            if (!specs.isEmpty()) return new OrderBy(specs);
        }
        return null;
    }

    protected OrderBySpec parseOrderBySpec( TokenStream tokens ) {
        if (tokens.canConsume('@')) {
            NameTest attributeName = parseQName(tokens);
            Order order = Order.ASCENDING;
            if (tokens.canConsume("ascending")) order = Order.ASCENDING;
            else if (tokens.canConsume("descending")) order = Order.DESCENDING;
            return new OrderBySpec(order, attributeName);
        }
        if (tokens.matches("jcr", ":", "score", "(")) {
            FunctionCall scoreFunction = parseFunctionCall(tokens);
            Order order = Order.ASCENDING;
            if (tokens.canConsume("ascending")) order = Order.ASCENDING;
            else if (tokens.canConsume("descending")) order = Order.DESCENDING;
            return new OrderBySpec(order, scoreFunction);
        }
        throw new ParsingException(tokens.nextPosition(),
                                   "Expected either 'jcr:score(tableName)' or '@<propertyName>' but found " + tokens.consume());
    }

    /**
     * Remove any leading and trailing single-quotes or double-quotes from the supplied text.
     * 
     * @param text the input text; may not be null
     * @return the text without leading and trailing quotes, or <code>text</code> if there were no square brackets or quotes
     */
    protected String removeQuotes( String text ) {
        assert text != null;
        if (text.length() > 2) {
            char first = text.charAt(0);
            // Need to remove these only if they are paired ...
            if (first == '"' || first == '\'') {
                int indexOfLast = text.length() - 1;
                char last = text.charAt(indexOfLast);
                if (last == first) {
                    text = text.substring(1, indexOfLast);
                }
            }
        }
        return text;
    }

    protected Component collapse( Component component ) {
        return component.collapse();
    }

    /**
     * A {@link TokenStream.Tokenizer} implementation that parses single- and double-quoted strings, symbols, words consisting of
     * {@link TokenStream.CharacterStream#isNextValidXmlNcNameCharacter() NCName}s (as defined by the <a
     * href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">Namespaces in XML 1.0</a> specification), XPath comments,and
     * {@link #OTHER other} single-character tokens. Quoted phrases are delimited by single-quote and double-quote characters
     * (which may be escaped within the quote). XPath comments begin with a "(:" and end with a ":)".
     */
    public static class XPathTokenizer implements TokenStream.Tokenizer {
        /**
         * The token type for tokens that represent an unquoted string containing a character sequence made up of non-whitespace
         * and non-symbol characters.
         */
        public static final int NAME = 2 << 0;
        /**
         * The token type for tokens that consist of an individual "symbol" character. The set of characters includes:
         * <code>(){}*.,;+%?$!<>|=:-[]^/\#@</code>
         */
        public static final int SYMBOL = 2 << 1;
        /**
         * The token type for tokens that consist of all the characters within single-quotes, double-quotes, or square brackets.
         */
        public static final int QUOTED_STRING = 2 << 2;
        /**
         * The token type for tokens that consist of all the characters between "(:" and ":)".
         */
        public static final int COMMENT = 2 << 3;
        /**
         * The token type for tokens that consist of single characters that are not a {@link #SYMBOL}, valid {@link #NAME}, or
         * {@link #QUOTED_STRING}.
         */
        public static final int OTHER = 2 << 4;

        private final boolean useComments;

        public XPathTokenizer( boolean useComments ) {
            this.useComments = useComments;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.text.TokenStream.Tokenizer#tokenize(CharacterStream, Tokens)
         */
        public void tokenize( CharacterStream input,
                              Tokens tokens ) throws ParsingException {
            while (input.hasNext()) {
                char c = input.next();
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // Just skip these whitespace characters ...
                        break;
                    case ')':
                    case '{':
                    case '}':
                    case '*':
                    case '.':
                    case ',':
                    case ';':
                    case '+':
                    case '%':
                    case '?':
                    case '$':
                    case '!':
                    case '<':
                    case '>':
                    case '|':
                    case '=':
                    case ':':
                    case '-':
                    case '[':
                    case ']':
                    case '^':
                    case '/':
                    case '\\':
                    case '#':
                    case '@':
                        tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                        break;
                    case '\'':
                    case '\"':
                        int startIndex = input.index();
                        char closingChar = c;
                        Position pos = input.position(startIndex);
                        boolean foundClosingQuote = false;
                        while (input.hasNext()) {
                            c = input.next();
                            if (c == closingChar && input.isNext(closingChar)) {
                                c = input.next(); // consume the next closeChar since it is escaped
                            } else if (c == closingChar) {
                                foundClosingQuote = true;
                                break;
                            }
                        }
                        if (!foundClosingQuote) {
                            String msg = CommonI18n.noMatchingDoubleQuoteFound.text(pos.getLine(), pos.getColumn());
                            if (closingChar == '\'') {
                                msg = CommonI18n.noMatchingSingleQuoteFound.text(pos.getLine(), pos.getColumn());
                            }
                            throw new ParsingException(pos, msg);
                        }
                        int endIndex = input.index() + 1; // beyond last character read
                        tokens.addToken(pos, startIndex, endIndex, QUOTED_STRING);
                        break;
                    case '(':
                        startIndex = input.index();
                        if (input.isNext(':')) {
                            // This is a comment ...
                            pos = input.position(startIndex);
                            while (input.hasNext() && !input.isNext(':', ')')) {
                                c = input.next();
                            }
                            if (input.hasNext()) input.next(); // consume the ':'
                            if (input.hasNext()) input.next(); // consume the ')'
                            if (useComments) {
                                endIndex = input.index() + 1; // the token will include the closing ':' and ')' characters
                                tokens.addToken(pos, startIndex, endIndex, COMMENT);
                            }
                        } else {
                            tokens.addToken(input.position(startIndex), input.index(), input.index() + 1, SYMBOL);
                            break;
                        }
                        break;
                    default:
                        startIndex = input.index();
                        pos = input.position(startIndex);
                        // Read as long as there is a valid XML character ...
                        int tokenType = (XmlCharacters.isValidNcNameStart(c)) ? NAME : OTHER;
                        while (input.isNextValidXmlNcNameCharacter()) {
                            c = input.next();
                        }
                        endIndex = input.index() + 1; // beyond last character that was included
                        tokens.addToken(pos, startIndex, endIndex, tokenType);
                }
            }
        }
    }
}
