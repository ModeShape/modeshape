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
package org.modeshape.jcr.query.model;

import java.math.BigDecimal;
import java.net.URI;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A set of common visitors that can be reused or extended, and methods that provide easy construction and calling of visitors.
 */
public class Visitors {

    protected static final char OPEN_SQUARE = '[';
    protected static final char CLOSE_SQUARE = ']';
    protected static final char DOUBLE_QUOTE = '"';
    protected static final char SINGLE_QUOTE = '\'';

    private static final ExecutionContext DEFAULT_CONTEXT = new ExecutionContext();

    /**
     * Visit all objects in the supplied {@link Visitable object} using a {@link NavigationVisitor} (specifically a
     * {@link WalkAllVisitor}), and with each of these visited objects calling the appropriate {@code visit(...)} method on the
     * supplied {@link Visitor}.
     * 
     * @param <StrategyVisitor> the type of strategy visitor
     * @param visitable the top-level object to be visited
     * @param strategyVisitor the visitor that is to be called for each visited objects, but that does <i>not</i> call
     *        {@link Visitable#accept(Visitor)}
     * @return the strategy visitor, allowing the caller to easily invoke operations on the visitor after visitation has completed
     */
    public static <StrategyVisitor extends Visitor> StrategyVisitor visitAll( Visitable visitable,
                                                                              StrategyVisitor strategyVisitor ) {
        if (visitable != null) visitable.accept(new WalkAllVisitor(strategyVisitor));
        return strategyVisitor;
    }

    /**
     * Visit the supplied {@link Visitable object} using the supplied {@link Visitor}, which must be responsible for navigation as
     * well as any business logic.
     * 
     * @param <GeneralVisitor> the type of visitor
     * @param visitable the top-level object to be visited
     * @param visitor the visitor that is to be used
     * @return the visitor, allowing the caller to easily invoke operations on the visitor after visitation has completed
     */
    public static <GeneralVisitor extends Visitor> GeneralVisitor visit( Visitable visitable,
                                                                         GeneralVisitor visitor ) {
        if (visitable != null) visitable.accept(visitor);
        return visitor;
    }

    /**
     * Using a visitor, obtain the readable string representation of the supplied {@link Visitable object}
     * 
     * @param visitable the visitable
     * @return the string representation
     */
    public static String readable( Visitable visitable ) {
        // return visit(visitable, new ReadableVisitor()).getString();
        return visit(visitable, new JcrSql2Writer(DEFAULT_CONTEXT)).getString();
    }

    /**
     * Using a visitor, obtain the readable string representation of the supplied {@link Visitable object}
     * 
     * @param visitable the visitable
     * @param context the execution context in which the visitable should be converted to a string
     * @return the string representation
     */
    public static String readable( Visitable visitable,
                                   ExecutionContext context ) {
        // return visit(visitable, new ReadableVisitor()).getString();
        return visit(visitable, new JcrSql2Writer(context)).getString();
    }

    public static String readable( Object[] tuple ) {
        return readable(tuple, DEFAULT_CONTEXT);
    }

    public static String readable( Object[] tuple,
                                   ExecutionContext context ) {
        if (tuple.length == 0) {
            return "||";
        }
        ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        for (Object value : tuple) {
            if (value != null) {
                sb.append(' ');
                if (value instanceof Object[]) {
                    Object[] array = (Object[])value;
                    int len = array.length;
                    for (int i = 0; i != len; ++i) {
                        if (i != 0) sb.append(", ");
                        sb.append(stringFactory.create(array[i]));
                    }
                } else {
                    sb.append(stringFactory.create(value));
                }
            }
            sb.append(" |");
        }
        return sb.toString();
    }

    /**
     * Using a visitor, obtain the {@link Subquery} objects that are contained within the supplied {@link Visitable object}. This
     * method does find Subquery objets nested in other Subquery objects.
     * 
     * @param visitable the visitable
     * @param includeNestedSubqueries true if any Subquery objects within other Subquery objects should be included, or false if
     *        only the top-level Subquery objects should be included
     * @return the collection of subqueries; never null but possibly empty if no subqueries were found
     */
    public static Collection<Subquery> subqueries( Visitable visitable,
                                                   final boolean includeNestedSubqueries ) {
        final Collection<Subquery> subqueries = new LinkedList<Subquery>();
        Visitors.visitAll(visitable, new Visitors.AbstractVisitor() {
            @Override
            public void visit( Subquery subquery ) {
                subqueries.add(subquery);
                if (includeNestedSubqueries) {
                    // Now look for any subqueries in the subquery ...
                    subquery.getQuery().accept(this);
                }
            }
        });
        return subqueries;
    }

    /**
     * Get a map of the selector names keyed by their aliases.
     * 
     * @param visitable the object to be visited
     * @return the map from the aliases to the aliased selector name; never null but possibly empty
     */
    public static Map<SelectorName, SelectorName> getSelectorNamesByAlias( Visitable visitable ) {
        // Find all of the selectors that have aliases ...
        final Map<SelectorName, SelectorName> result = new HashMap<SelectorName, SelectorName>();
        Visitors.visitAll(visitable, new Visitors.AbstractVisitor() {
            @Override
            public void visit( AllNodes allNodes ) {
                if (allNodes.hasAlias()) {
                    result.put(allNodes.alias(), allNodes.name());
                }
            }

            @Override
            public void visit( NamedSelector selector ) {
                if (selector.hasAlias()) {
                    result.put(selector.alias(), selector.name());
                }
            }
        });
        return result;
    }

    /**
     * Get a map of the selector aliases keyed by their names.
     * 
     * @param visitable the object to be visited
     * @return the map from the selector names to their alias (or name if there is no alias); never null but possibly empty
     */
    public static Map<SelectorName, SelectorName> getSelectorAliasesByName( Visitable visitable ) {
        // Find all of the selectors that have aliases ...
        final Map<SelectorName, SelectorName> result = new HashMap<SelectorName, SelectorName>();
        Visitors.visitAll(visitable, new Visitors.AbstractVisitor() {
            @Override
            public void visit( AllNodes allNodes ) {
                if (allNodes.hasAlias()) {
                    result.put(allNodes.name(), allNodes.aliasOrName());
                }
            }

            @Override
            public void visit( NamedSelector selector ) {
                if (selector.hasAlias()) {
                    result.put(selector.name(), selector.aliasOrName());
                }
            }
        });
        return result;
    }

    /**
     * Get the names of the selectors referenced by the visitable object.
     * 
     * @param visitable the object to be visited
     * @return the set of selector names referenced in some way by the visitable; never null
     */
    public static Set<SelectorName> getSelectorsReferencedBy( Visitable visitable ) {
        final Set<SelectorName> symbols = new HashSet<SelectorName>();
        // Walk the entire structure, so only supply a StrategyVisitor (that does no navigation) ...
        visitAll(visitable, new AbstractVisitor() {
            @Override
            public void visit( AllNodes allNodes ) {
                if (allNodes.hasAlias()) {
                    symbols.add(allNodes.alias());
                } else {
                    symbols.add(allNodes.name());
                }
            }

            @Override
            public void visit( ChildNode childNode ) {
                symbols.add(childNode.selectorName());
            }

            @Override
            public void visit( ChildNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.childSelectorName());
                symbols.add(joinCondition.parentSelectorName());
            }

            @Override
            public void visit( Column column ) {
                symbols.add(column.selectorName());
            }

            @Override
            public void visit( DescendantNode descendant ) {
                symbols.add(descendant.selectorName());
            }

            @Override
            public void visit( DescendantNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.ancestorSelectorName());
                symbols.add(joinCondition.descendantSelectorName());
            }

            @Override
            public void visit( EquiJoinCondition joinCondition ) {
                symbols.add(joinCondition.selector1Name());
                symbols.add(joinCondition.selector2Name());
            }

            @Override
            public void visit( FullTextSearch fullTextSearch ) {
                symbols.add(fullTextSearch.selectorName());
            }

            @Override
            public void visit( FullTextSearchScore fullTextSearchScore ) {
                symbols.add(fullTextSearchScore.selectorName());
            }

            @Override
            public void visit( Length length ) {
                symbols.add(length.selectorName());
            }

            @Override
            public void visit( NodeDepth depth ) {
                symbols.add(depth.selectorName());
            }

            @Override
            public void visit( NodePath path ) {
                symbols.add(path.selectorName());
            }

            @Override
            public void visit( NodeLocalName node ) {
                symbols.add(node.selectorName());
            }

            @Override
            public void visit( NodeName node ) {
                symbols.add(node.selectorName());
            }

            @Override
            public void visit( NamedSelector node ) {
                if (node.hasAlias()) {
                    symbols.add(node.alias());
                } else {
                    symbols.add(node.name());
                }
            }

            @Override
            public void visit( PropertyExistence prop ) {
                symbols.add(prop.selectorName());
            }

            @Override
            public void visit( PropertyValue prop ) {
                symbols.add(prop.selectorName());
            }

            @Override
            public void visit( Subquery obj ) {
                // do nothing ...
            }

            @Override
            public void visit( ReferenceValue ref ) {
                symbols.add(ref.selectorName());
            }

            @Override
            public void visit( SameNode node ) {
                symbols.add(node.selectorName());
            }

            @Override
            public void visit( SameNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.selector1Name());
                symbols.add(joinCondition.selector2Name());
            }
        });
        return symbols;
    }

    /**
     * A common base class for all visitors, which provides no-op implementations for all {@code visit(...)} methods. Visitor
     * implementations can subclass and implement only those methods that they need to implement.
     * <p>
     * This is often an excellent base class for <i>strategy visitors</i>, which simply are {@link Visitor} implementations that
     * are responsible only for visiting the supplied object but that never call {@link Visitable#accept(Visitor)}. Such strategy
     * visitors can be used in conjunction with separate <i>{@link NavigationVisitor navigation visitors}</i> that do the job of
     * navigating the Visitable objects and, for each, delegating to the strategy visitor. See
     * {@link Visitors#visitAll(Visitable, Visitor)} for an example.
     * </p>
     */
    public static class AbstractVisitor implements Visitor {

        @Override
        public void visit( AllNodes obj ) {
        }

        @Override
        public void visit( And obj ) {
        }

        @Override
        public void visit( ArithmeticOperand obj ) {
        }

        @Override
        public void visit( Between obj ) {
        }

        @Override
        public void visit( BindVariableName obj ) {
        }

        @Override
        public void visit( ChildNode obj ) {
        }

        @Override
        public void visit( ChildNodeJoinCondition obj ) {
        }

        @Override
        public void visit( Column obj ) {
        }

        @Override
        public void visit( Comparison obj ) {
        }

        @Override
        public void visit( DescendantNode obj ) {
        }

        @Override
        public void visit( DescendantNodeJoinCondition obj ) {
        }

        @Override
        public void visit( EquiJoinCondition obj ) {
        }

        @Override
        public void visit( FullTextSearch obj ) {
        }

        @Override
        public void visit( FullTextSearchScore obj ) {
        }

        @Override
        public void visit( Join obj ) {
        }

        @Override
        public void visit( Length obj ) {
        }

        @Override
        public void visit( Limit limit ) {
        }

        @Override
        public void visit( Literal obj ) {
        }

        @Override
        public void visit( LowerCase obj ) {
        }

        @Override
        public void visit( NodeDepth obj ) {
        }

        @Override
        public void visit( NodePath obj ) {
        }

        @Override
        public void visit( NodeName obj ) {
        }

        @Override
        public void visit( NodeLocalName obj ) {
        }

        @Override
        public void visit( NamedSelector obj ) {
        }

        @Override
        public void visit( Not obj ) {
        }

        @Override
        public void visit( Or obj ) {
        }

        @Override
        public void visit( Ordering obj ) {
        }

        @Override
        public void visit( PropertyExistence obj ) {
        }

        @Override
        public void visit( PropertyValue obj ) {
        }

        @Override
        public void visit( Query obj ) {
        }

        @Override
        public void visit( Subquery obj ) {
        }

        @Override
        public void visit( ReferenceValue obj ) {
        }

        @Override
        public void visit( SameNode obj ) {
        }

        @Override
        public void visit( SameNodeJoinCondition obj ) {
        }

        @Override
        public void visit( SetCriteria obj ) {
        }

        @Override
        public void visit( SetQuery obj ) {
        }

        @Override
        public void visit( UpperCase obj ) {
        }
    }

    /**
     * An abstract visitor implementation that performs navigation of the query object.
     * <p>
     * Subclasses should always implement the {@code visit(T object)} methods by performing the following actions:
     * <ol>
     * <li>Call <code>strategy.visit(object);</code></li>
     * <li>Add any children of {@code object} that are to be visited using {@link #enqueue(Visitable)}</li>
     * <li>Call {@link #visitNext()}</code></li>
     * </ol>
     * </p>
     */
    public static abstract class NavigationVisitor implements Visitor {
        protected final Visitor strategy;
        private final LinkedList<? super Visitable> itemQueue = new LinkedList<Visitable>();

        /**
         * Create a visitor that walks all query objects.
         * 
         * @param strategy the visitor that should be called at every node.
         */
        protected NavigationVisitor( Visitor strategy ) {
            assert strategy != null;
            this.strategy = strategy;
        }

        protected void enqueue( Visitable objectToBeVisited ) {
            if (objectToBeVisited != null) {
                itemQueue.add(objectToBeVisited);
            }
        }

        protected void enqueue( Iterable<? extends Visitable> objectsToBeVisited ) {
            for (Visitable objectToBeVisited : objectsToBeVisited) {
                enqueue(objectToBeVisited);
            }
        }

        protected final void visitNext() {
            if (!itemQueue.isEmpty()) {
                Visitable first = (Visitable)itemQueue.removeFirst();
                assert first != null;
                first.accept(this);
            }
        }
    }

    /**
     * A visitor implementation that walks the entire query object tree and delegates to another supplied visitor to do the actual
     * work.
     */
    public static class WalkAllVisitor extends NavigationVisitor {

        /**
         * Create a visitor that walks all query objects.
         * 
         * @param strategy the visitor that should be called at every node.
         */
        public WalkAllVisitor( Visitor strategy ) {
            super(strategy);
        }

        @Override
        public void visit( AllNodes allNodes ) {
            strategy.visit(allNodes);
            visitNext();
        }

        @Override
        public void visit( And and ) {
            strategy.visit(and);
            enqueue(and.left());
            enqueue(and.right());
            visitNext();
        }

        @Override
        public void visit( ArithmeticOperand arithmeticOperation ) {
            strategy.visit(arithmeticOperation);
            enqueue(arithmeticOperation.getLeft());
            enqueue(arithmeticOperation.getRight());
            visitNext();
        }

        @Override
        public void visit( Between between ) {
            strategy.visit(between);
            enqueue(between.getOperand());
            enqueue(between.getLowerBound());
            enqueue(between.getUpperBound());
            visitNext();
        }

        @Override
        public void visit( BindVariableName variableName ) {
            strategy.visit(variableName);
            visitNext();
        }

        @Override
        public void visit( ChildNode child ) {
            strategy.visit(child);
            visitNext();
        }

        @Override
        public void visit( ChildNodeJoinCondition joinCondition ) {
            strategy.visit(joinCondition);
            visitNext();
        }

        @Override
        public void visit( Column column ) {
            strategy.visit(column);
            visitNext();
        }

        @Override
        public void visit( Comparison comparison ) {
            strategy.visit(comparison);
            enqueue(comparison.getOperand1());
            enqueue(comparison.getOperand2());
            visitNext();
        }

        @Override
        public void visit( DescendantNode descendant ) {
            strategy.visit(descendant);
            visitNext();
        }

        @Override
        public void visit( DescendantNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        @Override
        public void visit( EquiJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        @Override
        public void visit( FullTextSearch fullTextSearch ) {
            strategy.visit(fullTextSearch);
            visitNext();
        }

        @Override
        public void visit( FullTextSearchScore score ) {
            strategy.visit(score);
            visitNext();
        }

        @Override
        public void visit( Join join ) {
            strategy.visit(join);
            enqueue(join.getLeft());
            enqueue(join.getJoinCondition());
            enqueue(join.getRight());
            visitNext();
        }

        @Override
        public void visit( Length length ) {
            strategy.visit(length);
            visitNext();
        }

        @Override
        public void visit( Limit limit ) {
            strategy.visit(limit);
            visitNext();
        }

        @Override
        public void visit( Literal literal ) {
            strategy.visit(literal);
            visitNext();
        }

        @Override
        public void visit( LowerCase lowerCase ) {
            strategy.visit(lowerCase);
            enqueue(lowerCase.getOperand());
            visitNext();
        }

        @Override
        public void visit( NodeDepth depth ) {
            strategy.visit(depth);
            visitNext();
        }

        @Override
        public void visit( NodePath path ) {
            strategy.visit(path);
            visitNext();
        }

        @Override
        public void visit( NodeName nodeName ) {
            strategy.visit(nodeName);
            visitNext();
        }

        @Override
        public void visit( NodeLocalName nodeLocalName ) {
            strategy.visit(nodeLocalName);
            visitNext();
        }

        @Override
        public void visit( NamedSelector selector ) {
            strategy.visit(selector);
            visitNext();
        }

        @Override
        public void visit( Not not ) {
            strategy.visit(not);
            enqueue(not.getConstraint());
            visitNext();
        }

        @Override
        public void visit( Or or ) {
            strategy.visit(or);
            enqueue(or.left());
            enqueue(or.right());
            visitNext();
        }

        @Override
        public void visit( Ordering ordering ) {
            strategy.visit(ordering);
            enqueue(ordering.getOperand());
            visitNext();
        }

        @Override
        public void visit( PropertyExistence existence ) {
            strategy.visit(existence);
            visitNext();
        }

        @Override
        public void visit( PropertyValue propertyValue ) {
            strategy.visit(propertyValue);
            visitNext();
        }

        @Override
        public void visit( Query query ) {
            strategy.visit(query);
            enqueue(query.source());
            enqueue(query.columns());
            enqueue(query.constraint());
            enqueue(query.orderings());
            visitNext();
        }

        @Override
        public void visit( Subquery subquery ) {
            strategy.visit(subquery);
            enqueue(subquery.getQuery());
            visitNext();
        }

        @Override
        public void visit( ReferenceValue referenceValue ) {
            strategy.visit(referenceValue);
            visitNext();
        }

        @Override
        public void visit( SameNode sameNode ) {
            strategy.visit(sameNode);
            visitNext();
        }

        @Override
        public void visit( SameNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        @Override
        public void visit( SetCriteria setCriteria ) {
            strategy.visit(setCriteria);
            enqueue(setCriteria.leftOperand());
            for (StaticOperand right : setCriteria.rightOperands()) {
                enqueue(right);
            }
            visitNext();
        }

        @Override
        public void visit( SetQuery setQuery ) {
            strategy.visit(setQuery);
            enqueue(setQuery.getLeft());
            enqueue(setQuery.getRight());
            visitNext();
        }

        @Override
        public void visit( UpperCase upperCase ) {
            strategy.visit(upperCase);
            enqueue(upperCase.getOperand());
            visitNext();
        }
    }

    public static class ReadableVisitor implements Visitor {
        protected final StringBuilder sb = new StringBuilder();
        protected final ExecutionContext context;
        protected final NamespaceRegistry registry;

        public ReadableVisitor( ExecutionContext context ) {
            CheckArg.isNotNull(context, "context");
            this.context = context;
            this.registry = context == null ? null : context.getNamespaceRegistry();
        }

        protected ReadableVisitor appendAlias( String columnName ) {
            append(columnName);
            return this;
        }

        protected ReadableVisitor appendColumnName( String columnName ) {
            append(columnName);
            return this;
        }

        protected ReadableVisitor appendPropertyName( String columnName ) {
            append(columnName);
            return this;
        }

        protected ReadableVisitor append( String string ) {
            sb.append(string);
            return this;
        }

        protected ReadableVisitor append( char character ) {
            sb.append(character);
            return this;
        }

        protected ReadableVisitor append( int value ) {
            sb.append(value);
            return this;
        }

        protected ReadableVisitor append( SelectorName name ) {
            sb.append(name.getString());
            return this;
        }

        protected ReadableVisitor append( Name name ) {
            append(SINGLE_QUOTE);
            append(name.getString(registry, null, null));
            append(SINGLE_QUOTE);
            return this;
        }

        protected ReadableVisitor append( Path path ) {
            sb.append(SINGLE_QUOTE);
            sb.append(path.getString(registry));
            sb.append(SINGLE_QUOTE);
            return this;
        }

        /**
         * @return context
         */
        public final ExecutionContext getContext() {
            return context;
        }

        /**
         * Get the string representation of the visited objects.
         * 
         * @return the string representation
         */
        public final String getString() {
            return sb.toString();
        }

        @Override
        public String toString() {
            return sb.toString();
        }

        @Override
        public void visit( AllNodes allNodes ) {
            append(allNodes.name());
            if (allNodes.hasAlias()) {
                append(" AS ").append(allNodes.alias());
            }
        }

        @Override
        public void visit( And and ) {
            append('(');
            and.left().accept(this);
            append(" AND ");
            and.right().accept(this);
            append(')');
        }

        @Override
        public void visit( ArithmeticOperand arithmeticOperand ) {
            append('(');
            arithmeticOperand.getLeft().accept(this);
            append(' ');
            append(arithmeticOperand.operator().symbol());
            append(' ');
            arithmeticOperand.getRight().accept(this);
            append(')');
        }

        @Override
        public void visit( Between between ) {
            between.getOperand().accept(this);
            append(" BETWEEN ");
            between.getLowerBound().accept(this);
            if (!between.isLowerBoundIncluded()) append(" EXCLUSIVE");
            append(" AND ");
            between.getUpperBound().accept(this);
            if (!between.isUpperBoundIncluded()) append(" EXCLUSIVE");
        }

        @Override
        public void visit( BindVariableName variable ) {
            append('$').append(variable.getBindVariableName());
        }

        @Override
        public void visit( ChildNode child ) {
            append("ISCHILDNODE(");
            append(child.selectorName());
            append(',');
            append(SINGLE_QUOTE);
            append(child.getParentPath());
            append(SINGLE_QUOTE);
            append(')');
        }

        @Override
        public void visit( ChildNodeJoinCondition condition ) {
            append("ISCHILDNODE(");
            append(condition.childSelectorName());
            append(',');
            append(condition.parentSelectorName());
            append(')');
        }

        @Override
        public void visit( Column column ) {
            append(column.selectorName());
            if (column.getPropertyName() == null) {
                append(".*");
            } else {
                String propertyName = column.getPropertyName();
                append('.').appendPropertyName(propertyName);
                if (!propertyName.equals(column.getColumnName()) && !propertyName.equals(column.getColumnName())) {
                    append(" AS ").appendAlias(column.getColumnName());
                }
            }
        }

        @Override
        public void visit( Comparison comparison ) {
            comparison.getOperand1().accept(this);
            append(' ').append(comparison.operator().symbol()).append(' ');
            comparison.getOperand2().accept(this);
        }

        @Override
        public void visit( DescendantNode descendant ) {
            append("ISDESCENDANTNODE(");
            append(descendant.selectorName());
            append(',');
            append(SINGLE_QUOTE);
            append(descendant.getAncestorPath());
            append(SINGLE_QUOTE);
            append(')');
        }

        @Override
        public void visit( DescendantNodeJoinCondition condition ) {
            append("ISDESCENDANTNODE(");
            append(condition.descendantSelectorName());
            append(',');
            append(condition.ancestorSelectorName());
            append(')');
        }

        @Override
        public void visit( EquiJoinCondition condition ) {
            append(condition.selector1Name()).append('.').appendPropertyName(condition.getProperty1Name());
            append(" = ");
            append(condition.selector2Name()).append('.').appendPropertyName(condition.getProperty2Name());
        }

        @Override
        public void visit( FullTextSearch fullText ) {
            append("CONTAINS(").append(fullText.selectorName());
            if (fullText.getPropertyName() != null) {
                append('.').appendPropertyName(fullText.getPropertyName());
            }
            sb.append(",'").append(fullText.fullTextSearchExpression()).append("')");
        }

        @Override
        public void visit( FullTextSearchScore score ) {
            append("SCORE(").append(score.selectorName()).append(')');
        }

        @Override
        public void visit( Join join ) {
            join.getLeft().accept(this);
            // if (join.getType() != JoinType.INNER) {
            sb.append(' ').append(join.type().symbol());
            // } else {
            // sb.append(',');
            // }
            append(' ');
            join.getRight().accept(this);
            append(" ON ");
            join.getJoinCondition().accept(this);
        }

        @Override
        public void visit( Length length ) {
            append("LENGTH(");
            length.getPropertyValue().accept(this);
            append(')');
        }

        @Override
        public void visit( Limit limit ) {
            append("LIMIT ").append(limit.getRowLimit());
            if (limit.getOffset() != 0) {
                append(" OFFSET ").append(limit.getOffset());
            }
        }

        @Override
        public void visit( Literal literal ) {
            if (literal instanceof LiteralValue) {
                LiteralValue literalValue = (LiteralValue)literal;
                Value value = literalValue.getLiteralValue();
                String typeName = null;
                ValueFactories factories = context.getValueFactories();
                switch (value.getType()) {
                    case PropertyType.UNDEFINED:
                    case PropertyType.STRING:
                        append(SINGLE_QUOTE);
                        String str = factories.getStringFactory().create(literalValue.value());
                        append(str);
                        append(SINGLE_QUOTE);
                        return;
                    case PropertyType.PATH:
                        append("CAST(");
                        append(factories.getPathFactory().create(literalValue.value()));
                        append(" AS ").append(PropertyType.TYPENAME_PATH.toUpperCase()).append(')');
                        return;
                    case PropertyType.NAME:
                        append("CAST(");
                        append(factories.getNameFactory().create(literalValue.value()));
                        append(" AS ").append(PropertyType.TYPENAME_NAME.toUpperCase()).append(')');
                        return;
                    case PropertyType.REFERENCE:
                        typeName = PropertyType.TYPENAME_REFERENCE;
                        break;
                    case PropertyType.WEAKREFERENCE:
                        typeName = PropertyType.TYPENAME_WEAKREFERENCE;
                        break;
                    case PropertyType.BINARY:
                        typeName = PropertyType.TYPENAME_BINARY;
                        break;
                    case PropertyType.BOOLEAN:
                        typeName = PropertyType.TYPENAME_BOOLEAN;
                        break;
                    case PropertyType.DATE:
                        typeName = PropertyType.TYPENAME_DATE;
                        break;
                    case PropertyType.DECIMAL:
                        typeName = PropertyType.TYPENAME_DECIMAL;
                        break;
                    case PropertyType.DOUBLE:
                        typeName = PropertyType.TYPENAME_DOUBLE;
                        break;
                    case PropertyType.LONG:
                        typeName = PropertyType.TYPENAME_LONG;
                        break;
                    case PropertyType.URI:
                        typeName = PropertyType.TYPENAME_URI;
                        break;
                }
                assert typeName != null;
                String str = factories.getStringFactory().create(literalValue.value());
                append("CAST('").append(str).append("' AS ").append(typeName.toUpperCase()).append(')');
            } else {
                Object value = literal.value();
                String typeName = null;
                ValueFactories factories = context.getValueFactories();
                if (value instanceof String || value instanceof Character) {
                    append(SINGLE_QUOTE);
                    String str = factories.getStringFactory().create(value);
                    append(str);
                    append(SINGLE_QUOTE);
                    return;
                }
                if (value instanceof Path) {
                    append("CAST(");
                    append(factories.getPathFactory().create(value));
                    append(" AS ").append(PropertyType.TYPENAME_PATH.toUpperCase()).append(')');
                    return;
                }
                if (value instanceof Name) {
                    append("CAST(");
                    append(factories.getNameFactory().create(value));
                    append(" AS ").append(PropertyType.TYPENAME_NAME.toUpperCase()).append(')');
                    return;
                }
                if (value instanceof Reference) {
                    typeName = ((Reference)value).isWeak() ? PropertyType.TYPENAME_WEAKREFERENCE.toUpperCase() : PropertyType.TYPENAME_REFERENCE.toUpperCase();
                } else if (value instanceof Binary) {
                    typeName = PropertyType.TYPENAME_BINARY.toUpperCase();
                } else if (value instanceof Boolean) {
                    typeName = PropertyType.TYPENAME_BOOLEAN.toUpperCase();
                } else if (value instanceof DateTime) {
                    typeName = PropertyType.TYPENAME_DATE.toUpperCase();
                } else if (value instanceof BigDecimal) {
                    typeName = PropertyType.TYPENAME_DECIMAL.toUpperCase();
                } else if (value instanceof Double || value instanceof Float) {
                    typeName = PropertyType.TYPENAME_DOUBLE.toUpperCase();
                } else if (value instanceof Long || value instanceof Integer || value instanceof Short) {
                    typeName = PropertyType.TYPENAME_LONG.toUpperCase();
                } else if (value instanceof URI) {
                    typeName = PropertyType.TYPENAME_URI.toUpperCase();
                }
                assert typeName != null;
                String str = factories.getStringFactory().create(value);
                append("CAST('").append(str).append("' AS ").append(typeName.toUpperCase()).append(')');
            }
        }

        @Override
        public void visit( LowerCase lowerCase ) {
            append("LOWER(");
            lowerCase.getOperand().accept(this);
            append(')');
        }

        @Override
        public void visit( NodeDepth depth ) {
            append("DEPTH(").append(depth.selectorName()).append(')');
        }

        @Override
        public void visit( NodePath path ) {
            append("PATH(").append(path.selectorName()).append(')');
        }

        @Override
        public void visit( NodeLocalName name ) {
            append("LOCALNAME(").append(name.selectorName()).append(')');
        }

        @Override
        public void visit( NodeName name ) {
            append("NAME(").append(name.selectorName()).append(')');
        }

        @Override
        public void visit( NamedSelector selector ) {
            append(selector.name());
            if (selector.hasAlias()) {
                append(" AS ").append(selector.alias());
            }
        }

        @Override
        public void visit( Not not ) {
            append("NOT ");
            append('(');
            not.getConstraint().accept(this);
            append(')');
        }

        @Override
        public void visit( Or or ) {
            append('(');
            or.left().accept(this);
            append(" OR ");
            or.right().accept(this);
            append(')');
        }

        @Override
        public void visit( Ordering ordering ) {
            ordering.getOperand().accept(this);
            append(' ').append(ordering.order().symbol());
        }

        @Override
        public void visit( PropertyExistence existence ) {
            append(existence.selectorName()).append('.').appendPropertyName(existence.getPropertyName()).append(" IS NOT NULL");
        }

        @Override
        public void visit( PropertyValue value ) {
            append(value.selectorName()).append('.').appendPropertyName(value.getPropertyName());
        }

        @Override
        public void visit( ReferenceValue value ) {
            append(value.selectorName());
            if (value.getPropertyName() != null) {
                append('.').appendPropertyName(value.getPropertyName());
            }
        }

        @Override
        public void visit( Query query ) {
            append("SELECT ");
            if (query.isDistinct()) append("DISTINCT ");
            if (query.columns().isEmpty()) {
                append('*');
            } else {
                boolean isFirst = true;
                for (Column column : query.columns()) {
                    if (isFirst) isFirst = false;
                    else append(", ");
                    column.accept(this);
                }
            }
            append(" FROM ");
            query.source().accept(this);
            if (query.constraint() != null) {
                append(" WHERE ");
                query.constraint().accept(this);
            }
            if (!query.orderings().isEmpty()) {
                append(" ORDER BY ");
                boolean isFirst = true;
                for (Ordering ordering : query.orderings()) {
                    if (isFirst) isFirst = false;
                    else append(", ");
                    ordering.accept(this);
                }
            }
            if (!query.getLimits().isUnlimited()) {
                append(' ');
                query.getLimits().accept(this);
            }
        }

        @Override
        public void visit( Subquery subquery ) {
            append('(');
            subquery.getQuery().accept(this);
            append(')');
        }

        @Override
        public void visit( SameNode sameNode ) {
            append("ISSAMENODE(").append(sameNode.selectorName()).append(",'").append(sameNode.getPath()).append("')");
        }

        @Override
        public void visit( SameNodeJoinCondition condition ) {
            append("ISSAMENODE(").append(condition.selector1Name()).append(',').append(condition.selector2Name());
            if (condition.getSelector2Path() != null) {
                append(",'").append(condition.getSelector2Path()).append('\'');
            }
            append(')');
        }

        @Override
        public void visit( SetCriteria criteria ) {
            criteria.leftOperand().accept(this);
            append(" IN (");
            Iterator<? extends StaticOperand> iter = criteria.rightOperands().iterator();
            if (iter.hasNext()) {
                iter.next().accept(this);
                while (iter.hasNext()) {
                    append(',');
                    iter.next().accept(this);
                }
            }
            append(')');
        }

        @Override
        public void visit( SetQuery query ) {
            query.getLeft().accept(this);
            append(' ').append(query.operation().getSymbol()).append(' ');
            if (query.isAll()) append("ALL ");
            query.getRight().accept(this);
        }

        @Override
        public void visit( UpperCase upperCase ) {
            append("UPPER(");
            upperCase.getOperand().accept(this);
            append(')');
        }

    }

    public static class JcrSql2Writer extends ReadableVisitor {

        public JcrSql2Writer( ExecutionContext context ) {
            super(context);
        }

        protected final boolean needsQuotes( String str ) {
            CharacterIterator iter = new StringCharacterIterator(str);
            for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
                if (!Character.isLetterOrDigit(c)) return true;
            }
            return false;
        }

        protected void appendQuoted( char openQuote,
                                     String name,
                                     char closeQuote ) {
            // If the name contains any non-alphanumeric characters, then we'll quote.
            // It's okay (and safer) to quote more often than necessary.
            if (needsQuotes(name)) {
                append(OPEN_SQUARE);
                append(name);
                append(CLOSE_SQUARE);
            } else {
                append(name);
            }
        }

        @Override
        protected ReadableVisitor append( String string ) {
            return super.append(string);
        }

        @Override
        protected ReadableVisitor append( char character ) {
            return super.append(character);
        }

        @Override
        protected ReadableVisitor append( int value ) {
            return super.append(value);
        }

        @Override
        protected ReadableVisitor appendColumnName( String columnName ) {
            appendQuoted(OPEN_SQUARE, columnName, CLOSE_SQUARE);
            return this;
        }

        @Override
        protected ReadableVisitor appendPropertyName( String propertyName ) {
            appendQuoted(OPEN_SQUARE, propertyName, CLOSE_SQUARE);
            return this;
        }

        @Override
        protected ReadableVisitor appendAlias( String alias ) {
            appendQuoted(OPEN_SQUARE, alias, CLOSE_SQUARE);
            return this;
        }

        @Override
        protected ReadableVisitor append( SelectorName name ) {
            appendQuoted(OPEN_SQUARE, name.name(), CLOSE_SQUARE);
            return this;
        }
    }
}
