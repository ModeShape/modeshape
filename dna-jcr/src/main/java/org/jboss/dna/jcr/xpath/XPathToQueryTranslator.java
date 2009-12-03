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
package org.jboss.dna.jcr.xpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.query.QueryBuilder;
import org.jboss.dna.graph.query.QueryBuilder.ConstraintBuilder;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.parse.InvalidQueryException;
import org.jboss.dna.jcr.xpath.XPath.And;
import org.jboss.dna.jcr.xpath.XPath.AttributeNameTest;
import org.jboss.dna.jcr.xpath.XPath.AxisStep;
import org.jboss.dna.jcr.xpath.XPath.BinaryComponent;
import org.jboss.dna.jcr.xpath.XPath.Comparison;
import org.jboss.dna.jcr.xpath.XPath.Component;
import org.jboss.dna.jcr.xpath.XPath.ContextItem;
import org.jboss.dna.jcr.xpath.XPath.DescendantOrSelf;
import org.jboss.dna.jcr.xpath.XPath.ElementTest;
import org.jboss.dna.jcr.xpath.XPath.Except;
import org.jboss.dna.jcr.xpath.XPath.FilterStep;
import org.jboss.dna.jcr.xpath.XPath.FunctionCall;
import org.jboss.dna.jcr.xpath.XPath.Intersect;
import org.jboss.dna.jcr.xpath.XPath.Literal;
import org.jboss.dna.jcr.xpath.XPath.NameTest;
import org.jboss.dna.jcr.xpath.XPath.NodeTest;
import org.jboss.dna.jcr.xpath.XPath.Or;
import org.jboss.dna.jcr.xpath.XPath.ParenthesizedExpression;
import org.jboss.dna.jcr.xpath.XPath.PathExpression;
import org.jboss.dna.jcr.xpath.XPath.StepExpression;
import org.jboss.dna.jcr.xpath.XPath.Union;

/**
 * A component that translates an {@link XPath} abstract syntax model representation into a {@link QueryCommand DNA abstract query
 * model}.
 */
public class XPathToQueryTranslator {

    protected static final Map<NameTest, String> CAST_FUNCTION_NAME_TO_TYPE;

    static {
        Map<NameTest, String> map = new HashMap<NameTest, String>();
        map.put(new NameTest("fn", "string"), PropertyType.STRING.getName().toUpperCase());
        map.put(new NameTest("xs", "string"), PropertyType.STRING.getName().toUpperCase());
        map.put(new NameTest("xs", "base64Binary"), PropertyType.BINARY.getName().toUpperCase());
        map.put(new NameTest("xs", "double"), PropertyType.DOUBLE.getName().toUpperCase());
        map.put(new NameTest("xs", "long"), PropertyType.LONG.getName().toUpperCase());
        map.put(new NameTest("xs", "boolean"), PropertyType.BOOLEAN.getName().toUpperCase());
        map.put(new NameTest("xs", "dateTime"), PropertyType.DATE.getName().toUpperCase());
        map.put(new NameTest("xs", "string"), PropertyType.PATH.getName().toUpperCase());
        map.put(new NameTest("xs", "string"), PropertyType.NAME.getName().toUpperCase());
        map.put(new NameTest("xs", "IDREF"), PropertyType.REFERENCE.getName().toUpperCase());
        CAST_FUNCTION_NAME_TO_TYPE = Collections.unmodifiableMap(map);
    }

    private final String query;
    private final TypeSystem typeSystem;
    private final QueryBuilder builder;
    private final Set<String> aliases = new HashSet<String>();

    public XPathToQueryTranslator( TypeSystem context,
                                   String query ) {
        this.query = query;
        this.typeSystem = context;
        this.builder = new QueryBuilder(this.typeSystem);
    }

    public QueryCommand createQuery( Component xpath ) {
        if (xpath instanceof BinaryComponent) {
            BinaryComponent binary = (BinaryComponent)xpath;
            if (binary instanceof Union) {
                createQuery(binary.getLeft());
                builder.union();
                createQuery(binary.getRight());
                return builder.query();
            } else if (binary instanceof Intersect) {
                createQuery(binary.getLeft());
                builder.intersect();
                createQuery(binary.getRight());
                return builder.query();
            } else if (binary instanceof Except) {
                createQuery(binary.getLeft());
                builder.except();
                createQuery(binary.getRight());
                return builder.query();
            }
        } else if (xpath instanceof PathExpression) {
            translate((PathExpression)xpath);
            return builder.query();
        }
        // unexpected component ...
        throw new InvalidQueryException(query,
                                        "Acceptable XPath queries must lead with a path expression or must be a union, intersect or except");
    }

    protected void translate( PathExpression pathExpression ) {
        List<StepExpression> steps = pathExpression.getSteps();
        assert !steps.isEmpty();
        if (!pathExpression.isRelative()) {
            // Absolute path must start with "/jcr:root/" or "//" ...
            Component first = steps.get(0).collapse();
            // Result will be NameTest("jcr","root") or DescendantOrSelf ...
            if (first instanceof DescendantOrSelf) {
                // do nothing ...
            } else if (first instanceof NameTest && steps.size() == 1 && ((NameTest)first).matches("jcr", "root")) {
                // We can actually remove this first step, since relative paths are relative to the root ...
                steps = steps.subList(1, steps.size());
            } else if (first instanceof NameTest && steps.size() > 1 && ((NameTest)first).matches("jcr", "root")) {
                // We can actually remove this first step, since relative paths are relative to the root ...
                steps = steps.subList(1, steps.size());
            } else {
                throw new InvalidQueryException(query, "An absolute path expression must start with '//' or '/jcr:root/...'");
            }
        }

        // Walk the steps along the path expression ...
        ConstraintBuilder where = builder.where();
        List<StepExpression> path = new ArrayList<StepExpression>();
        String tableName = null;
        for (StepExpression step : steps) {
            if (step instanceof AxisStep) {
                AxisStep axis = (AxisStep)step;
                NodeTest nodeTest = axis.getNodeTest();
                if (nodeTest instanceof NameTest) {
                    if (appliesToPathConstraint(axis.getPredicates())) {
                        // Can go into the path constraint ...
                        path.add(step);
                    } else {
                        // The constraints are more complicated, so we need to define a new source/table ...
                        // path.add(step);
                        tableName = translateSource(tableName, path, where);
                        translatePredicates(axis.getPredicates(), tableName, where);
                        path.clear();
                    }
                } else if (nodeTest instanceof ElementTest) {
                    // We need to build a new source with the partial path we have so far ...
                    tableName = translateElementTest((ElementTest)nodeTest, path, where);
                    translatePredicates(axis.getPredicates(), tableName, where);
                    path.clear();
                } else if (nodeTest instanceof AttributeNameTest) {
                    AttributeNameTest attributeName = (AttributeNameTest)nodeTest;
                    builder.select(nameFrom(attributeName.getNameTest()));
                } else {
                    throw new InvalidQueryException(query, "The '" + step + "' step is not supported");
                }
            } else if (step instanceof FilterStep) {
                FilterStep filter = (FilterStep)step;
                Component primary = filter.getPrimaryExpression();
                List<Component> predicates = filter.getPredicates();
                if (primary instanceof ContextItem) {
                    if (appliesToPathConstraint(predicates)) {
                        // Can ignore the '.' ...
                    } else {
                        // The constraints are more complicated, so we need to define a new source/table ...
                        path.add(step);
                        tableName = translateSource(tableName, path, where);
                        translatePredicates(predicates, tableName, where);
                        path.clear();
                    }
                } else if (primary instanceof Literal) {
                    throw new InvalidQueryException(query,
                                                    "A literal is not supported in the primary path expression; therefore '"
                                                    + primary + "' is not valid");
                } else if (primary instanceof FunctionCall) {
                    throw new InvalidQueryException(query,
                                                    "A function call is not supported in the primary path expression; therefore '"
                                                    + primary + "' is not valid");
                } else if (primary instanceof ParenthesizedExpression) {
                    // This can be used to define an OR-ed set of expressions defining select columns ...
                    ParenthesizedExpression paren = (ParenthesizedExpression)primary;
                    Component wrapped = paren.getWrapped().collapse();
                    if (wrapped instanceof AttributeNameTest) {
                        AttributeNameTest attributeName = (AttributeNameTest)wrapped;
                        builder.select(nameFrom(attributeName.getNameTest()));
                    } else if (wrapped instanceof BinaryComponent) {
                        for (AttributeNameTest attributeName : extractAttributeNames((BinaryComponent)wrapped)) {
                            builder.select(nameFrom(attributeName.getNameTest()));
                        }
                        path.add(filter); // in case any element names are there
                    } else {
                        throw new InvalidQueryException(query,
                                                        "A parenthesized expression of this type is not supported in the primary path expression; therefore '"
                                                        + primary + "' is not valid");
                    }
                }

            } else {
                path.add(step);
            }
        }
        if (steps.isEmpty() || !path.isEmpty()) {
            translateSource(tableName, path, where);
        }
        where.end();
    }

    /**
     * Find any {@link AttributeNameTest attribute names} that have been unioned together (with '|'). Any other combination of
     * objects results in an error.
     * 
     * @param binary the binary component
     * @return the list of attribute names, if that's all that's in the supplied component; may be empty
     */
    protected List<AttributeNameTest> extractAttributeNames( BinaryComponent binary ) {
        List<AttributeNameTest> results = new ArrayList<AttributeNameTest>();
        boolean failed = false;
        if (binary instanceof Union) {
            for (int i = 0; i != 2; ++i) {
                Component comp = i == 0 ? binary.getLeft() : binary.getRight();
                comp = comp.collapse();
                if (comp instanceof Union) {
                    results.addAll(extractAttributeNames((BinaryComponent)comp));
                } else if (comp instanceof AttributeNameTest) {
                    results.add((AttributeNameTest)comp);
                } else if (comp instanceof NameTest) {
                    // Element names, which are fine but we'll ignore
                } else {
                    failed = true;
                    break;
                }
            }
        } else {
            failed = true;
        }
        if (failed) {
            throw new InvalidQueryException(query,
                                            "A parenthesized expression in a path step may only contain ORed and ANDed attribute names or element names; therefore '"
                                            + binary + "' is not valid");
        }
        return results;
    }

    /**
     * Find any {@link NameTest element names} that have been unioned together (with '|'). Any other combination of objects
     * results in an error.
     * 
     * @param binary the binary component
     * @return the list of attribute names, if that's all that's in the supplied component; may be empty
     */
    protected List<NameTest> extractElementNames( BinaryComponent binary ) {
        List<NameTest> results = new ArrayList<NameTest>();
        boolean failed = false;
        if (binary instanceof Union) {
            for (int i = 0; i != 2; ++i) {
                Component comp = i == 0 ? binary.getLeft() : binary.getRight();
                comp = comp.collapse();
                if (comp instanceof Union) {
                    results.addAll(extractElementNames((BinaryComponent)comp));
                } else if (comp instanceof AttributeNameTest) {
                    // ignore these ...
                } else if (comp instanceof NameTest) {
                    results.add((NameTest)comp);
                } else {
                    failed = true;
                    break;
                }
            }
        } else {
            failed = true;
        }
        if (failed) {
            throw new InvalidQueryException(query,
                                            "A parenthesized expression in a path step may only contain ORed element names; therefore '"
                                            + binary + "' is not valid");
        }
        return results;
    }

    protected String translateSource( String tableName,
                                      List<StepExpression> path,
                                      ConstraintBuilder where ) {
        String alias = newAlias();
        if (tableName != null) {
            // This is after some element(...) steps, so we need to join ...
            builder.joinAllNodesAs(alias);
        } else {
            // This is the only part of the query ...
            builder.fromAllNodesAs(alias);
        }
        tableName = alias;
        if (path.size() == 1 && path.get(0).collapse() instanceof NameTest) {
            // Node immediately below root ...
            NameTest nodeName = (NameTest)path.get(0).collapse();
            where.nodeName(alias).isEqualTo(nameFrom(nodeName)).and().depth(alias).isEqualTo(1);
        } else if (path.size() == 2 && path.get(0) instanceof DescendantOrSelf && path.get(1).collapse() instanceof NameTest) {
            // Node anywhere ...
            NameTest nodeName = (NameTest)path.get(1).collapse();
            if (!nodeName.isWildcard()) {
                where.nodeName(alias).isEqualTo(nameFrom(nodeName));
            }
        } else {
            // Must be just a bunch of descendant-or-self, axis and filter steps ...
            translatePathExpressionConstraint(new PathExpression(true, path), where, alias);
        }
        return tableName;
    }

    protected String translateElementTest( ElementTest elementTest,
                                           List<StepExpression> pathConstraint,
                                           ConstraintBuilder where ) {
        String tableName = null;
        NameTest typeName = elementTest.getTypeName();
        if (typeName.isWildcard()) {
            tableName = newAlias();
            builder.fromAllNodesAs(tableName);
        } else {
            if (typeName.getLocalTest() == null) {
                throw new InvalidQueryException(
                                                query,
                                                "The '"
                                                + elementTest
                                                + "' clause uses a partial wildcard in the type name, but only a wildcard on the whole name is supported");
            }
            tableName = nameFrom(typeName);
            builder.from(tableName);
        }
        if (elementTest.getElementName() != null) {
            NameTest nodeName = elementTest.getElementName();
            if (!nodeName.isWildcard()) {
                where.nodeName(tableName).isEqualTo(nameFrom(nodeName));
            }
        }
        if (pathConstraint.isEmpty()) {
            where.depth(tableName).isEqualTo(1);
        }
        return tableName;
    }

    protected void translatePredicates( List<Component> predicates,
                                        String tableName,
                                        ConstraintBuilder where ) {
        assert tableName != null;
        for (Component predicate : predicates) {
            translatePredicate(predicate, tableName, where);
        }
    }

    protected String translatePredicate( Component predicate,
                                         String tableName,
                                         ConstraintBuilder where ) {
        predicate = predicate.collapse();
        assert tableName != null;
        if (predicate instanceof ParenthesizedExpression) {
            ParenthesizedExpression paren = (ParenthesizedExpression)predicate;
            where = where.openParen();
            translatePredicate(paren.getWrapped(), tableName, where);
            where.closeParen();
        } else if (predicate instanceof And) {
            And and = (And)predicate;
            where = where.openParen();
            translatePredicate(and.getLeft(), tableName, where);
            where.and();
            translatePredicate(and.getRight(), tableName, where);
            where.closeParen();
        } else if (predicate instanceof Or) {
            Or or = (Or)predicate;
            where = where.openParen();
            translatePredicate(or.getLeft(), tableName, where);
            where.or();
            translatePredicate(or.getRight(), tableName, where);
            where.closeParen();
        } else if (predicate instanceof Union) {
            Union union = (Union)predicate;
            where = where.openParen();
            translatePredicate(union.getLeft(), tableName, where);
            where.or();
            translatePredicate(union.getRight(), tableName, where);
            where.closeParen();
        } else if (predicate instanceof Literal) {
            Literal literal = (Literal)predicate;
            if (literal.isInteger()) return tableName; // do nothing, since this is a path constraint and is handled elsewhere
        } else if (predicate instanceof AttributeNameTest) {
            // This adds the criteria that the attribute exists, and adds it to the select ...
            AttributeNameTest attribute = (AttributeNameTest)predicate;
            String propertyName = nameFrom(attribute.getNameTest());
            where.hasProperty(tableName, propertyName);
        } else if (predicate instanceof NameTest) {
            // This adds the criteria that the child node exists ...
            NameTest childName = (NameTest)predicate;
            String alias = newAlias();
            builder.joinAllNodesAs(alias).onChildNode(tableName, alias);
            if (!childName.isWildcard()) where.nodeName(alias).isEqualTo(nameFrom(childName));
            tableName = alias;
        } else if (predicate instanceof Comparison) {
            Comparison comparison = (Comparison)predicate;
            Component left = comparison.getLeft();
            Component right = comparison.getRight();
            Operator operator = comparison.getOperator();
            if (left instanceof Literal) {
                Component temp = left;
                left = right;
                right = temp;
                operator = operator.getReverse();
            }
            if (left instanceof AttributeNameTest) {
                AttributeNameTest attribute = (AttributeNameTest)left;
                String propertyName = nameFrom(attribute.getNameTest());
                if (right instanceof Literal) {
                    String value = ((Literal)right).getValue();
                    where.propertyValue(tableName, propertyName).is(operator, value);
                } else if (right instanceof FunctionCall) {
                    FunctionCall call = (FunctionCall)right;
                    NameTest functionName = call.getName();
                    List<Component> parameters = call.getParameters();
                    // Is this a cast ...
                    String castType = CAST_FUNCTION_NAME_TO_TYPE.get(functionName);
                    if (castType != null) {
                        if (parameters.size() == 1 && parameters.get(0).collapse() instanceof Literal) {
                            // The first parameter can be the type name (or table name) ...
                            Literal value = (Literal)parameters.get(0).collapse();
                            where.propertyValue(tableName, propertyName).is(operator).cast(value.getValue()).as(castType);
                        } else {
                            throw new InvalidQueryException(query, "A cast function requires one literal parameter; therefore '"
                                                                   + comparison + "' is not valid");
                        }
                    } else {
                        throw new InvalidQueryException(query,
                                                        "Only the 'jcr:score' function is allowed in a comparison predicate; therefore '"
                                                        + comparison + "' is not valid");
                    }
                }
            } else if (left instanceof FunctionCall && right instanceof Literal) {
                FunctionCall call = (FunctionCall)left;
                NameTest functionName = call.getName();
                List<Component> parameters = call.getParameters();
                String value = ((Literal)right).getValue();
                if (functionName.matches("jcr", "score")) {
                    String scoreTableName = tableName;
                    if (parameters.isEmpty()) {
                        scoreTableName = tableName;
                    } else if (parameters.size() == 1 && parameters.get(0) instanceof NameTest) {
                        // The first parameter can be the type name (or table name) ...
                        NameTest name = (NameTest)parameters.get(0);
                        if (!name.isWildcard()) scoreTableName = nameFrom(name);
                    } else {
                        throw new InvalidQueryException(query,
                                                        "The 'jcr:score' function may have no parameters or the type name as the only parameter.");

                    }
                    where.fullTextSearchScore(scoreTableName).is(operator, value);
                } else {
                    throw new InvalidQueryException(query,
                                                    "Only the 'jcr:score' function is allowed in a comparison predicate; therefore '"
                                                    + comparison + "' is not valid");
                }
            }
        } else if (predicate instanceof FunctionCall) {
            FunctionCall call = (FunctionCall)predicate;
            NameTest functionName = call.getName();
            List<Component> parameters = call.getParameters();
            Component param1 = parameters.size() > 0 ? parameters.get(0) : null;
            Component param2 = parameters.size() > 1 ? parameters.get(1) : null;
            if (functionName.matches(null, "not")) {
                if (parameters.size() != 1) {
                    throw new InvalidQueryException(query, "The 'not' function requires one parameter; therefore '" + predicate
                                                           + "' is not valid");
                }
                where = where.not().openParen();
                translatePredicate(param1, tableName, where);
                where.closeParen();
            } else if (functionName.matches("jcr", "like")) {
                if (parameters.size() != 2) {
                    throw new InvalidQueryException(query, "The 'jcr:like' function requires two parameters; therefore '"
                                                           + predicate + "' is not valid");
                }
                if (!(param1 instanceof AttributeNameTest)) {
                    throw new InvalidQueryException(query,
                                                    "The first parameter of 'jcr:like' must be an property reference with the '@' symbol; therefore '"
                                                    + predicate + "' is not valid");
                }
                if (!(param2 instanceof Literal)) {
                    throw new InvalidQueryException(query, "The second parameter of 'jcr:like' must be a literal; therefore '"
                                                           + predicate + "' is not valid");
                }
                NameTest attributeName = ((AttributeNameTest)param1).getNameTest();
                String value = ((Literal)param2).getValue();
                where.propertyValue(tableName, nameFrom(attributeName)).isLike(value);
            } else if (functionName.matches("jcr", "contains")) {
                if (parameters.size() != 2) {
                    throw new InvalidQueryException(query, "The 'jcr:contains' function requires two parameters; therefore '"
                                                           + predicate + "' is not valid");
                }
                if (!(param2 instanceof Literal)) {
                    throw new InvalidQueryException(query,
                                                    "The second parameter of 'jcr:contains' must be a literal; therefore '"
                                                    + predicate + "' is not valid");
                }
                String value = ((Literal)param2).getValue();
                if (param1 instanceof ContextItem) {
                    // refers to the current node (or table) ...
                    where.search(tableName, value);
                } else if (param1 instanceof AttributeNameTest) {
                    // refers to an attribute on the current node (or table) ...
                    NameTest attributeName = ((AttributeNameTest)param1).getNameTest();
                    where.search(tableName, nameFrom(attributeName), value);
                } else if (param1 instanceof NameTest) {
                    // refers to child node, so we need to add a join ...
                    String alias = newAlias();
                    builder.joinAllNodesAs(alias).onChildNode(tableName, alias);
                    // Now add the criteria ...
                    where.search(alias, value);
                    tableName = alias;
                } else if (param1 instanceof PathExpression) {
                    // refers to a descendant node ...
                    PathExpression pathExpr = (PathExpression)param1;
                    if (pathExpr.getLastStep().collapse() instanceof AttributeNameTest) {
                        AttributeNameTest attributeName = (AttributeNameTest)pathExpr.getLastStep().collapse();
                        pathExpr = pathExpr.withoutLast();
                        String searchTable = translatePredicate(pathExpr, tableName, where);
                        if (attributeName.getNameTest().isWildcard()) {
                            where.search(searchTable, value);
                        } else {
                            where.search(searchTable, nameFrom(attributeName.getNameTest()), value);
                        }
                    } else {
                        String searchTable = translatePredicate(param1, tableName, where);
                        where.search(searchTable, value);
                    }
                } else {
                    throw new InvalidQueryException(query,
                                                    "The first parameter of 'jcr:contains' must be a relative path (e.g., '.', an attribute name, a child name, etc.); therefore '"
                                                    + predicate + "' is not valid");
                }
            } else if (functionName.matches("jcr", "deref")) {
                throw new InvalidQueryException(query,
                                                "The 'jcr:deref' function is not required by JCR and is not currently supported; therefore '"
                                                + predicate + "' is not valid");
            } else {
                throw new InvalidQueryException(query,
                                                "Only the 'jcr:like' and 'jcr:contains' functions are allowed in a predicate; therefore '"
                                                + predicate + "' is not valid");
            }
        } else if (predicate instanceof PathExpression) {
            // Requires that the descendant node with the relative path does exist ...
            PathExpression pathExpr = (PathExpression)predicate;
            List<StepExpression> steps = pathExpr.getSteps();
            assert steps.size() > 1; // 1 or 0 would have been collapsed ...
            Component firstStep = steps.get(0).collapse();
            if (firstStep instanceof ContextItem) {
                // Remove the context and retry ...
                return translatePredicate(new PathExpression(true, steps.subList(1, steps.size())), tableName, where);
            }
            if (firstStep instanceof NameTest) {
                // Special case where this is similar to '[a/@id]'
                NameTest childName = (NameTest)firstStep;
                String alias = newAlias();
                builder.joinAllNodesAs(alias).onChildNode(tableName, alias);
                if (!childName.isWildcard()) {
                    where.nodeName(alias).isEqualTo(nameFrom(childName));
                }
                return translatePredicate(new PathExpression(true, steps.subList(1, steps.size())), alias, where);
            }
            if (firstStep instanceof DescendantOrSelf) {
                // Special case where this is similar to '[a/@id]'
                String alias = newAlias();
                builder.joinAllNodesAs(alias).onDescendant(tableName, alias);
                return translatePredicate(new PathExpression(true, steps.subList(1, steps.size())), alias, where);
            }
            // Add the join ...
            String alias = newAlias();
            builder.joinAllNodesAs(alias).onDescendant(tableName, alias);
            // Now add the criteria ...
            translatePathExpressionConstraint(pathExpr, where, alias);
        } else {
            throw new InvalidQueryException(query, "Unsupported criteria '" + predicate + "'");
        }
        return tableName;
    }

    /**
     * Determine if the predicates contain any expressions that cannot be put into a LIKE constraint on the path.
     * 
     * @param predicates the predicates
     * @return true if the supplied predicates can be handled entirely in the LIKE constraint on the path, or false if they have
     *         to be handled as other criteria
     */
    protected boolean appliesToPathConstraint( List<Component> predicates ) {
        if (predicates.isEmpty()) return true;
        if (predicates.size() > 1) return false;
        assert predicates.size() == 1;
        Component predicate = predicates.get(0);
        if (predicate instanceof Literal && ((Literal)predicate).isInteger()) return true;
        if (predicate instanceof NameTest && ((NameTest)predicate).isWildcard()) return true;
        return false;
    }

    protected boolean translatePathExpressionConstraint( PathExpression pathExrp,
                                                         ConstraintBuilder where,
                                                         String tableName ) {
        String[] paths = relativePathLikeExpressions(pathExrp);
        if (paths == null || paths.length == 0) return false;
        where = where.openParen();
        boolean first = true;
        int number = 0;
        for (String path : paths) {
            if (path == null || path.length() == 0 || path.equals("%/")) continue;
            if (first) first = false;
            else where.or();
            if (path.indexOf('%') != -1) {
                where.path(tableName).isLike(path);
            } else {
                where.path(tableName).isEqualTo(path);
            }
            ++number;
        }
        if (number > 0) where.closeParen();
        return true;
    }

    protected String[] relativePathLikeExpressions( PathExpression pathExpression ) {
        List<StepExpression> steps = pathExpression.getSteps();
        if (steps.isEmpty()) return new String[] {};
        if (steps.size() == 1 && steps.get(0) instanceof DescendantOrSelf) return new String[] {};
        PathLikeBuilder builder = new SinglePathLikeBuilder();
        for (StepExpression step : steps) {
            if (step instanceof DescendantOrSelf) {
                if (builder.isEmpty()) {
                    builder.append("%/");
                } else {
                    builder = new DualPathLikeBuilder(builder.clone(), builder.append("/%"));
                }
            } else if (step instanceof AxisStep) {
                AxisStep axis = (AxisStep)step;
                NodeTest nodeTest = axis.getNodeTest();
                assert !(nodeTest instanceof ElementTest);
                if (nodeTest instanceof NameTest) {
                    NameTest nameTest = (NameTest)nodeTest;
                    builder.append('/');
                    if (nameTest.getPrefixTest() != null) {
                        builder.append(nameTest.getPrefixTest()).append(':');
                    }
                    if (nameTest.getLocalTest() != null) {
                        builder.append(nameTest.getLocalTest());
                    } else {
                        builder.append('%');
                    }
                    List<Component> predicates = axis.getPredicates();
                    if (!predicates.isEmpty()) {
                        assert predicates.size() == 1;
                        Component predicate = predicates.get(0);
                        if (predicate instanceof Literal && ((Literal)predicate).isInteger()) {
                            builder.append('[').append(((Literal)predicate).getValue()).append(']');
                        }
                    }
                }
            } else if (step instanceof FilterStep) {
                FilterStep filter = (FilterStep)step;
                Component primary = filter.getPrimaryExpression();
                if (primary instanceof ContextItem) {
                    continue; // ignore this '.'
                } else if (primary instanceof ParenthesizedExpression) {
                    ParenthesizedExpression paren = (ParenthesizedExpression)primary;
                    Component wrapped = paren.getWrapped().collapse();
                    if (wrapped instanceof AttributeNameTest) {
                        // ignore this; handled earlier ...
                    } else if (wrapped instanceof BinaryComponent) {
                        List<NameTest> names = extractElementNames((BinaryComponent)wrapped);
                        if (names.size() >= 1) {
                            PathLikeBuilder orig = builder.clone();
                            builder.append('/').append(nameFrom(names.get(0)));
                            if (names.size() > 1) {
                                for (NameTest name : names.subList(1, names.size())) {
                                    builder = new DualPathLikeBuilder(orig.clone().append('/').append(nameFrom(name)), builder);
                                }
                            }
                        }
                    } else {
                        throw new InvalidQueryException(query,
                                                        "A parenthesized expression of this type is not supported in the primary path expression; therefore '"
                                                        + primary + "' is not valid");
                    }
                }
            }
        }
        return builder.getPaths();
    }

    protected static interface PathLikeBuilder {
        PathLikeBuilder append( String string );

        PathLikeBuilder append( char c );

        boolean isEmpty();

        PathLikeBuilder clone();

        String[] getPaths();
    }

    protected static class SinglePathLikeBuilder implements PathLikeBuilder {
        private final StringBuilder builder = new StringBuilder();

        public SinglePathLikeBuilder append( String string ) {
            builder.append(string);
            return this;
        }

        public SinglePathLikeBuilder append( char c ) {
            builder.append(c);
            return this;
        }

        public boolean isEmpty() {
            return builder.length() == 0;
        }

        @Override
        public SinglePathLikeBuilder clone() {
            return new SinglePathLikeBuilder().append(builder.toString());
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        public String[] getPaths() {
            return isEmpty() ? new String[] {} : new String[] {builder.toString()};
        }
    }

    protected static class DualPathLikeBuilder implements PathLikeBuilder {
        private final PathLikeBuilder builder1;
        private final PathLikeBuilder builder2;

        protected DualPathLikeBuilder( PathLikeBuilder builder1,
                                       PathLikeBuilder builder2 ) {
            this.builder1 = builder1;
            this.builder2 = builder2;
        }

        public DualPathLikeBuilder append( String string ) {
            builder1.append(string);
            builder2.append(string);
            return this;
        }

        public DualPathLikeBuilder append( char c ) {
            builder1.append(c);
            builder2.append(c);
            return this;
        }

        public boolean isEmpty() {
            return false;
        }

        @Override
        public DualPathLikeBuilder clone() {
            return new DualPathLikeBuilder(builder1.clone(), builder2.clone());
        }

        public String[] getPaths() {
            String[] paths1 = builder1.getPaths();
            String[] paths2 = builder2.getPaths();
            String[] result = new String[paths1.length + paths2.length];
            System.arraycopy(paths1, 0, result, 0, paths1.length);
            System.arraycopy(paths2, 0, result, paths1.length, paths2.length);
            return result;
        }
    }

    protected String nameFrom( NameTest name ) {
        String prefix = name.getPrefixTest();
        String local = name.getLocalTest();
        assert local != null;
        return (prefix != null ? prefix + ":" : "") + local;
    }

    protected String newAlias() {
        String root = "nodeSet";
        int num = 1;
        String alias = root + num;
        while (aliases.contains(alias)) {
            num += 1;
            alias = root + num;
        }
        aliases.add(alias);
        return alias;
    }
}
