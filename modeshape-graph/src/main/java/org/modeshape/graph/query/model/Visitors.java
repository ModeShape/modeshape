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
package org.modeshape.graph.query.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;

/**
 * A set of common visitors that can be reused or extended, and methods that provide easy construction and calling of visitors.
 */
public class Visitors {

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
        return visit(visitable, new ReadableVisitor()).getString();
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
                    subquery.query().accept(this);
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

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.AllNodes)
         */
        public void visit( AllNodes obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.And)
         */
        public void visit( And obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ArithmeticOperand)
         */
        public void visit( ArithmeticOperand obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Between)
         */
        public void visit( Between obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNode)
         */
        public void visit( ChildNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Column)
         */
        public void visit( Column obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Comparison)
         */
        public void visit( Comparison obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Join)
         */
        public void visit( Join obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Length)
         */
        public void visit( Length obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Literal)
         */
        public void visit( Literal obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.LowerCase)
         */
        public void visit( LowerCase obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeDepth)
         */
        public void visit( NodeDepth obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodePath)
         */
        public void visit( NodePath obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeName)
         */
        public void visit( NodeName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Not)
         */
        public void visit( Not obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Or)
         */
        public void visit( Or obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Ordering)
         */
        public void visit( Ordering obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Query)
         */
        public void visit( Query obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Subquery)
         */
        public void visit( Subquery obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ReferenceValue)
         */
        public void visit( ReferenceValue obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNode)
         */
        public void visit( SameNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetCriteria)
         */
        public void visit( SetCriteria obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetQuery)
         */
        public void visit( SetQuery obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.UpperCase)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.AllNodes)
         */
        public void visit( AllNodes allNodes ) {
            strategy.visit(allNodes);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.And)
         */
        public void visit( And and ) {
            strategy.visit(and);
            enqueue(and.left());
            enqueue(and.right());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ArithmeticOperand)
         */
        public void visit( ArithmeticOperand arithmeticOperation ) {
            strategy.visit(arithmeticOperation);
            enqueue(arithmeticOperation.left());
            enqueue(arithmeticOperation.right());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Between)
         */
        public void visit( Between between ) {
            strategy.visit(between);
            enqueue(between.operand());
            enqueue(between.lowerBound());
            enqueue(between.upperBound());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName variableName ) {
            strategy.visit(variableName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNode)
         */
        public void visit( ChildNode child ) {
            strategy.visit(child);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition joinCondition ) {
            strategy.visit(joinCondition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Column)
         */
        public void visit( Column column ) {
            strategy.visit(column);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Comparison)
         */
        public void visit( Comparison comparison ) {
            strategy.visit(comparison);
            enqueue(comparison.operand1());
            enqueue(comparison.operand2());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode descendant ) {
            strategy.visit(descendant);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch fullTextSearch ) {
            strategy.visit(fullTextSearch);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore score ) {
            strategy.visit(score);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Join)
         */
        public void visit( Join join ) {
            strategy.visit(join);
            enqueue(join.left());
            enqueue(join.joinCondition());
            enqueue(join.right());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Length)
         */
        public void visit( Length length ) {
            strategy.visit(length);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
            strategy.visit(limit);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Literal)
         */
        public void visit( Literal literal ) {
            strategy.visit(literal);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.LowerCase)
         */
        public void visit( LowerCase lowerCase ) {
            strategy.visit(lowerCase);
            enqueue(lowerCase.operand());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeDepth)
         */
        public void visit( NodeDepth depth ) {
            strategy.visit(depth);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodePath)
         */
        public void visit( NodePath path ) {
            strategy.visit(path);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeName)
         */
        public void visit( NodeName nodeName ) {
            strategy.visit(nodeName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName nodeLocalName ) {
            strategy.visit(nodeLocalName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector selector ) {
            strategy.visit(selector);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Not)
         */
        public void visit( Not not ) {
            strategy.visit(not);
            enqueue(not.constraint());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Or)
         */
        public void visit( Or or ) {
            strategy.visit(or);
            enqueue(or.left());
            enqueue(or.right());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Ordering)
         */
        public void visit( Ordering ordering ) {
            strategy.visit(ordering);
            enqueue(ordering.operand());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence existence ) {
            strategy.visit(existence);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue propertyValue ) {
            strategy.visit(propertyValue);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Query)
         */
        public void visit( Query query ) {
            strategy.visit(query);
            enqueue(query.source());
            enqueue(query.columns());
            enqueue(query.constraint());
            enqueue(query.orderings());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Subquery)
         */
        public void visit( Subquery subquery ) {
            strategy.visit(subquery);
            enqueue(subquery.query());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ReferenceValue)
         */
        public void visit( ReferenceValue referenceValue ) {
            strategy.visit(referenceValue);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNode)
         */
        public void visit( SameNode sameNode ) {
            strategy.visit(sameNode);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetCriteria)
         */
        public void visit( SetCriteria setCriteria ) {
            strategy.visit(setCriteria);
            enqueue(setCriteria.leftOperand());
            for (StaticOperand right : setCriteria.rightOperands()) {
                enqueue(right);
            }
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetQuery)
         */
        public void visit( SetQuery setQuery ) {
            strategy.visit(setQuery);
            enqueue(setQuery.left());
            enqueue(setQuery.right());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.UpperCase)
         */
        public void visit( UpperCase upperCase ) {
            strategy.visit(upperCase);
            enqueue(upperCase.operand());
            visitNext();
        }
    }

    public static class ReadableVisitor implements Visitor {
        private final StringBuilder sb = new StringBuilder();
        private final ExecutionContext context;
        private final NamespaceRegistry registry;

        public ReadableVisitor( ExecutionContext context ) {
            CheckArg.isNotNull(context, "context");
            this.context = context;
            this.registry = context == null ? null : context.getNamespaceRegistry();
        }

        public ReadableVisitor() {
            this.context = null;
            this.registry = null;
        }

        protected final ReadableVisitor append( String string ) {
            sb.append(string);
            return this;
        }

        protected final ReadableVisitor append( char character ) {
            sb.append(character);
            return this;
        }

        protected final ReadableVisitor append( int value ) {
            sb.append(value);
            return this;
        }

        protected final ReadableVisitor append( SelectorName name ) {
            sb.append(name.getString());
            return this;
        }

        protected final ReadableVisitor append( Name name ) {
            sb.append(name.getString(registry, null, null));
            return this;
        }

        protected final ReadableVisitor append( Path path ) {
            sb.append('\'');
            sb.append(path.getString(registry));
            sb.append('\'');
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

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.AllNodes)
         */
        public void visit( AllNodes allNodes ) {
            append(allNodes.name());
            if (allNodes.hasAlias()) {
                append(" AS ").append(allNodes.alias());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.And)
         */
        public void visit( And and ) {
            append('(');
            and.left().accept(this);
            append(" AND ");
            and.right().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ArithmeticOperand)
         */
        public void visit( ArithmeticOperand arithmeticOperand ) {
            append('(');
            arithmeticOperand.left().accept(this);
            append(' ');
            append(arithmeticOperand.operator().symbol());
            append(' ');
            arithmeticOperand.right().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Between)
         */
        public void visit( Between between ) {
            between.operand().accept(this);
            append(" BETWEEN ");
            between.lowerBound().accept(this);
            if (!between.isLowerBoundIncluded()) append(" EXCLUSIVE");
            append(" AND ");
            between.upperBound().accept(this);
            if (!between.isUpperBoundIncluded()) append(" EXCLUSIVE");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName variable ) {
            append('$').append(variable.variableName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNode)
         */
        public void visit( ChildNode child ) {
            append("ISCHILDNODE(");
            append(child.selectorName());
            append(',');
            append(child.parentPath());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition condition ) {
            append("ISCHILDNODE(");
            append(condition.childSelectorName());
            append(',');
            append(condition.parentSelectorName());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Column)
         */
        public void visit( Column column ) {
            append(column.selectorName());
            if (column.propertyName() == null) {
                append(".*");
            } else {
                String propertyName = column.propertyName();
                append('.').append(propertyName);
                if (!propertyName.equals(column.columnName()) && !propertyName.equals(column.columnName())) {
                    append(" AS ").append(column.columnName());
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Comparison)
         */
        public void visit( Comparison comparison ) {
            comparison.operand1().accept(this);
            append(' ').append(comparison.operator().symbol()).append(' ');
            comparison.operand2().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode descendant ) {
            append("ISDESCENDANTNODE(");
            append(descendant.selectorName());
            append(',');
            append(descendant.ancestorPath());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition condition ) {
            append("ISDESCENDANTNODE(");
            append(condition.descendantSelectorName());
            append(',');
            append(condition.ancestorSelectorName());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition condition ) {
            append(condition.selector1Name()).append('.').append(condition.property1Name());
            append(" = ");
            append(condition.selector2Name()).append('.').append(condition.property2Name());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch fullText ) {
            append("CONTAINS(").append(fullText.selectorName());
            if (fullText.propertyName() != null) {
                append('.').append(fullText.propertyName());
            }
            sb.append(",'").append(fullText.fullTextSearchExpression()).append("')");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore score ) {
            append("SCORE(").append(score.selectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Join)
         */
        public void visit( Join join ) {
            join.left().accept(this);
            // if (join.getType() != JoinType.INNER) {
            sb.append(' ').append(join.type().symbol());
            // } else {
            // sb.append(',');
            // }
            append(' ');
            join.right().accept(this);
            append(" ON ");
            join.joinCondition().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Length)
         */
        public void visit( Length length ) {
            append("LENGTH(");
            length.propertyValue().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
            append("LIMIT ").append(limit.rowLimit());
            if (limit.offset() != 0) {
                append(" OFFSET ").append(limit.offset());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Literal)
         */
        public void visit( Literal literal ) {
            Object value = literal.value();
            boolean quote = value instanceof String || value instanceof Path || value instanceof Name;
            if (quote) append('\'');
            if (context == null) {
                append(literal.value().toString());
            } else {
                append(context.getValueFactories().getStringFactory().create(literal.value()));
            }
            if (quote) append('\'');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.LowerCase)
         */
        public void visit( LowerCase lowerCase ) {
            append("LOWER(");
            lowerCase.operand().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeDepth)
         */
        public void visit( NodeDepth depth ) {
            append("DEPTH(").append(depth.selectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodePath)
         */
        public void visit( NodePath path ) {
            append("PATH(").append(path.selectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName name ) {
            append("LOCALNAME(").append(name.selectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NodeName)
         */
        public void visit( NodeName name ) {
            append("NAME(").append(name.selectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector selector ) {
            append(selector.name());
            if (selector.hasAlias()) {
                append(" AS ").append(selector.alias());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Not)
         */
        public void visit( Not not ) {
            append('(');
            append("NOT ");
            not.constraint().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Or)
         */
        public void visit( Or or ) {
            append('(');
            or.left().accept(this);
            append(" OR ");
            or.right().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Ordering)
         */
        public void visit( Ordering ordering ) {
            ordering.operand().accept(this);
            append(' ').append(ordering.order().symbol());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence existence ) {
            append(existence.selectorName()).append('.').append(existence.propertyName()).append(" IS NOT NULL");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue value ) {
            append(value.selectorName()).append('.').append(value.propertyName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.ReferenceValue)
         */
        public void visit( ReferenceValue value ) {
            append(value.selectorName());
            if (value.propertyName() != null) {
                append('.').append(value.propertyName());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Query)
         */
        public void visit( Query query ) {
            append("SELECT ");
            if (query.isDistinct()) append("DISTINCT ");
            if (query.columns().isEmpty()) {
                append('*');
            } else {
                boolean isFirst = true;
                for (Column column : query.columns()) {
                    if (isFirst) isFirst = false;
                    else append(',');
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
                    else append(',');
                    ordering.accept(this);
                }
            }
            if (!query.limits().isUnlimited()) {
                append(' ');
                query.limits().accept(this);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.Subquery)
         */
        public void visit( Subquery subquery ) {
            append('(');
            subquery.query().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNode)
         */
        public void visit( SameNode sameNode ) {
            append("ISSAMENODE(").append(sameNode.selectorName()).append(',').append(sameNode.path()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition condition ) {
            append("ISSAMENODE(").append(condition.selector1Name()).append(',').append(condition.selector2Name());
            if (condition.selector2Path() != null) {
                append(',').append(condition.selector2Path());
            }
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetCriteria)
         */
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

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.SetQuery)
         */
        public void visit( SetQuery query ) {
            query.left().accept(this);
            append(' ').append(query.operation().getSymbol()).append(' ');
            if (query.isAll()) append("ALL ");
            query.right().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitor#visit(org.modeshape.graph.query.model.UpperCase)
         */
        public void visit( UpperCase upperCase ) {
            append("UPPER(");
            upperCase.operand().accept(this);
            append(')');
        }

    }
}
