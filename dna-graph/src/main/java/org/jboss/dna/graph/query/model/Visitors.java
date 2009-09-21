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
package org.jboss.dna.graph.query.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;

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
        visitable.accept(new WalkAllVisitor(strategyVisitor));
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
        visitable.accept(visitor);
        return visitor;
    }

    /**
     * Using a visitor, obtain the readable string representation of the supplied {@link Visitable object}
     * 
     * @param visitable the visitable
     * @param context the execution context in which the representation should be produced, or null if there is none
     * @return the string representation
     */
    public static String readable( Visitable visitable,
                                   ExecutionContext context ) {
        return visit(visitable, new ReadableVisitor(context)).getString();
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
                    symbols.add(allNodes.getAlias());
                } else {
                    symbols.add(allNodes.getName());
                }
            }

            @Override
            public void visit( ChildNode childNode ) {
                symbols.add(childNode.getSelectorName());
            }

            @Override
            public void visit( ChildNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.getChildSelectorName());
                symbols.add(joinCondition.getParentSelectorName());
            }

            @Override
            public void visit( Column column ) {
                symbols.add(column.getSelectorName());
            }

            @Override
            public void visit( DescendantNode descendant ) {
                symbols.add(descendant.getSelectorName());
            }

            @Override
            public void visit( DescendantNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.getAncestorSelectorName());
                symbols.add(joinCondition.getDescendantSelectorName());
            }

            @Override
            public void visit( EquiJoinCondition joinCondition ) {
                symbols.add(joinCondition.getSelector1Name());
                symbols.add(joinCondition.getSelector2Name());
            }

            @Override
            public void visit( FullTextSearch fullTextSearch ) {
                symbols.add(fullTextSearch.getSelectorName());
            }

            @Override
            public void visit( Length length ) {
                symbols.add(length.getSelectorName());
            }

            @Override
            public void visit( NodeLocalName node ) {
                symbols.add(node.getSelectorName());
            }

            @Override
            public void visit( NodeName node ) {
                symbols.add(node.getSelectorName());
            }

            @Override
            public void visit( NamedSelector node ) {
                if (node.hasAlias()) {
                    symbols.add(node.getAlias());
                } else {
                    symbols.add(node.getName());
                }
            }

            @Override
            public void visit( PropertyExistence prop ) {
                symbols.add(prop.getSelectorName());
            }

            @Override
            public void visit( PropertyValue prop ) {
                symbols.add(prop.getSelectorName());
            }

            @Override
            public void visit( SameNode node ) {
                symbols.add(node.getSelectorName());
            }

            @Override
            public void visit( SameNodeJoinCondition joinCondition ) {
                symbols.add(joinCondition.getSelector1Name());
                symbols.add(joinCondition.getSelector2Name());
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
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.AllNodes)
         */
        public void visit( AllNodes obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.And)
         */
        public void visit( And obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNode)
         */
        public void visit( ChildNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Column)
         */
        public void visit( Column obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Comparison)
         */
        public void visit( Comparison obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Join)
         */
        public void visit( Join obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Length)
         */
        public void visit( Length obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Literal)
         */
        public void visit( Literal obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.LowerCase)
         */
        public void visit( LowerCase obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeName)
         */
        public void visit( NodeName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Not)
         */
        public void visit( Not obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Or)
         */
        public void visit( Or obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Ordering)
         */
        public void visit( Ordering obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Query)
         */
        public void visit( Query obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNode)
         */
        public void visit( SameNode obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SetQuery)
         */
        public void visit( SetQuery obj ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.UpperCase)
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

        protected final void enqueue( Visitable objectToBeVisited ) {
            itemQueue.add(objectToBeVisited);
        }

        protected final void enqueue( Iterable<? extends Visitable> objectsToBeVisited ) {
            for (Visitable objectToBeVisited : objectsToBeVisited) {
                itemQueue.add(objectToBeVisited);
            }
        }

        protected final void visitNext() {
            if (!itemQueue.isEmpty()) {
                Visitable first = (Visitable)itemQueue.removeFirst();
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
        protected WalkAllVisitor( Visitor strategy ) {
            super(strategy);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.AllNodes)
         */
        public void visit( AllNodes allNodes ) {
            strategy.visit(allNodes);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.And)
         */
        public void visit( And and ) {
            strategy.visit(and);
            enqueue(and.getLeft());
            enqueue(and.getRight());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName variableName ) {
            strategy.visit(variableName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNode)
         */
        public void visit( ChildNode child ) {
            strategy.visit(child);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition joinCondition ) {
            strategy.visit(joinCondition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Column)
         */
        public void visit( Column column ) {
            strategy.visit(column);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Comparison)
         */
        public void visit( Comparison comparison ) {
            strategy.visit(comparison);
            enqueue(comparison.getOperand1());
            enqueue(comparison.getOperand2());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode descendant ) {
            strategy.visit(descendant);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch fullTextSearch ) {
            strategy.visit(fullTextSearch);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore score ) {
            strategy.visit(score);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Join)
         */
        public void visit( Join join ) {
            strategy.visit(join);
            enqueue(join.getLeft());
            enqueue(join.getJoinCondition());
            enqueue(join.getRight());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Length)
         */
        public void visit( Length length ) {
            strategy.visit(length);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
            strategy.visit(limit);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Literal)
         */
        public void visit( Literal literal ) {
            strategy.visit(literal);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.LowerCase)
         */
        public void visit( LowerCase lowerCase ) {
            strategy.visit(lowerCase);
            enqueue(lowerCase.getOperand());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeName)
         */
        public void visit( NodeName nodeName ) {
            strategy.visit(nodeName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName nodeLocalName ) {
            strategy.visit(nodeLocalName);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector selector ) {
            strategy.visit(selector);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Not)
         */
        public void visit( Not not ) {
            strategy.visit(not);
            enqueue(not.getConstraint());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Or)
         */
        public void visit( Or or ) {
            strategy.visit(or);
            enqueue(or.getLeft());
            enqueue(or.getRight());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Ordering)
         */
        public void visit( Ordering ordering ) {
            strategy.visit(ordering);
            enqueue(ordering.getOperand());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence existence ) {
            strategy.visit(existence);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue propertyValue ) {
            strategy.visit(propertyValue);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Query)
         */
        public void visit( Query query ) {
            strategy.visit(query);
            enqueue(query.getSource());
            enqueue(query.getColumns());
            enqueue(query.getConstraint());
            enqueue(query.getOrderings());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNode)
         */
        public void visit( SameNode sameNode ) {
            strategy.visit(sameNode);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition condition ) {
            strategy.visit(condition);
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SetQuery)
         */
        public void visit( SetQuery setQuery ) {
            strategy.visit(setQuery);
            enqueue(setQuery.getLeft());
            enqueue(setQuery.getRight());
            visitNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.UpperCase)
         */
        public void visit( UpperCase upperCase ) {
            strategy.visit(upperCase);
            enqueue(upperCase.getOperand());
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
            sb.append(name.getString(context));
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
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.AllNodes)
         */
        public void visit( AllNodes allNodes ) {
            append(allNodes.getName());
            if (allNodes.hasAlias()) {
                append(" AS ").append(allNodes.getAlias());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.And)
         */
        public void visit( And and ) {
            append('(');
            and.getLeft().accept(this);
            append(" AND ");
            and.getRight().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.BindVariableName)
         */
        public void visit( BindVariableName variable ) {
            append('$').append(variable.getVariableName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNode)
         */
        public void visit( ChildNode child ) {
            append("ISCHILDNODE(");
            append(child.getSelectorName());
            append(',');
            append(child.getParentPath());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.ChildNodeJoinCondition)
         */
        public void visit( ChildNodeJoinCondition condition ) {
            append("ISCHILDNODE(");
            append(condition.getChildSelectorName());
            append(',');
            append(condition.getParentSelectorName());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Column)
         */
        public void visit( Column column ) {
            append(column.getSelectorName());
            if (column.getPropertyName() == null) {
                append(".*");
            } else {
                Name propertyName = column.getPropertyName();
                String propName = propertyName.getString(registry, null, null);
                append('.').append(propName);
                if (!propName.equals(column.getColumnName()) && !propertyName.getLocalName().equals(column.getColumnName())) {
                    append(" AS ").append(column.getColumnName());
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Comparison)
         */
        public void visit( Comparison comparison ) {
            comparison.getOperand1().accept(this);
            append(' ').append(comparison.getOperator().getSymbol()).append(' ');
            comparison.getOperand2().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNode)
         */
        public void visit( DescendantNode descendant ) {
            append("ISDESCENDANTNODE(");
            append(descendant.getSelectorName());
            append(',');
            append(descendant.getAncestorPath());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.DescendantNodeJoinCondition)
         */
        public void visit( DescendantNodeJoinCondition condition ) {
            append("ISDESCENDANTNODE(");
            append(condition.getDescendantSelectorName());
            append(',');
            append(condition.getAncestorSelectorName());
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.EquiJoinCondition)
         */
        public void visit( EquiJoinCondition condition ) {
            append(condition.getSelector1Name()).append('.').append(condition.getProperty1Name());
            append(" = ");
            append(condition.getSelector2Name()).append('.').append(condition.getProperty2Name());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearch)
         */
        public void visit( FullTextSearch fullText ) {
            append("CONTAINS(").append(fullText.getSelectorName());
            if (fullText.getPropertyName() != null) {
                append('.').append(fullText.getPropertyName());
            }
            sb.append(",'").append(fullText.getFullTextSearchExpression()).append("')");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.FullTextSearchScore)
         */
        public void visit( FullTextSearchScore score ) {
            append("SCORE(").append(score.getSelectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Join)
         */
        public void visit( Join join ) {
            join.getLeft().accept(this);
            // if (join.getType() != JoinType.INNER) {
            sb.append(' ').append(join.getType().getSymbol());
            // } else {
            // sb.append(',');
            // }
            append(' ');
            join.getRight().accept(this);
            append(" ON ");
            join.getJoinCondition().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Length)
         */
        public void visit( Length length ) {
            append("LENGTH(");
            length.getPropertyValue().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Limit)
         */
        public void visit( Limit limit ) {
            append("LIMIT ").append(limit.getRowLimit());
            if (limit.getOffset() != 0) {
                append(" OFFSET ").append(limit.getOffset());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Literal)
         */
        public void visit( Literal literal ) {
            if (context == null) append(literal.getValue().toString());
            else append(context.getValueFactories().getStringFactory().create(literal.getValue()));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.LowerCase)
         */
        public void visit( LowerCase lowerCase ) {
            append("LOWER(");
            lowerCase.getOperand().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeLocalName)
         */
        public void visit( NodeLocalName name ) {
            append("LOCALNAME(").append(name.getSelectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NodeName)
         */
        public void visit( NodeName name ) {
            append("NAME(").append(name.getSelectorName()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.NamedSelector)
         */
        public void visit( NamedSelector selector ) {
            append(selector.getName());
            if (selector.hasAlias()) {
                append(" AS ").append(selector.getAlias());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Not)
         */
        public void visit( Not not ) {
            append('(');
            append("NOT ");
            not.getConstraint().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Or)
         */
        public void visit( Or or ) {
            append('(');
            or.getLeft().accept(this);
            append(" OR ");
            or.getRight().accept(this);
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Ordering)
         */
        public void visit( Ordering ordering ) {
            ordering.getOperand().accept(this);
            append(' ').append(ordering.getOrder().getSymbol());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyExistence)
         */
        public void visit( PropertyExistence existence ) {
            append(existence.getSelectorName()).append('.').append(existence.getPropertyName()).append(" IS NOT NULL");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.PropertyValue)
         */
        public void visit( PropertyValue value ) {
            append(value.getSelectorName()).append('.').append(value.getPropertyName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.Query)
         */
        public void visit( Query query ) {
            append("SELECT ");
            if (query.isDistinct()) append("DISTINCT ");
            if (query.getColumns().isEmpty()) {
                append('*');
            } else {
                boolean isFirst = true;
                for (Column column : query.getColumns()) {
                    if (isFirst) isFirst = false;
                    else append(',');
                    column.accept(this);
                }
            }
            append(" FROM ");
            query.getSource().accept(this);
            if (query.getConstraint() != null) {
                append(" WHERE ");
                query.getConstraint().accept(this);
            }
            if (!query.getOrderings().isEmpty()) {
                append(" ORDER BY ");
                boolean isFirst = true;
                for (Ordering ordering : query.getOrderings()) {
                    if (isFirst) isFirst = false;
                    else append(',');
                    ordering.accept(this);
                }
            }
            if (!query.getLimits().isUnlimited()) {
                append(' ');
                query.getLimits().accept(this);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNode)
         */
        public void visit( SameNode sameNode ) {
            append("ISSAMENODE(").append(sameNode.getSelectorName()).append(',').append(sameNode.getPath()).append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SameNodeJoinCondition)
         */
        public void visit( SameNodeJoinCondition condition ) {
            append("ISSAMENODE(").append(condition.getSelector1Name()).append(',').append(condition.getSelector2Name());
            if (condition.getSelector2Path() != null) {
                append(',').append(condition.getSelector2Path());
            }
            append(')');
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.SetQuery)
         */
        public void visit( SetQuery query ) {
            query.getLeft().accept(this);
            append(' ').append(query.getOperation().getSymbol()).append(' ');
            if (query.isAll()) append("ALL ");
            query.getRight().accept(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Visitor#visit(org.jboss.dna.graph.query.model.UpperCase)
         */
        public void visit( UpperCase upperCase ) {
            append("UPPER(");
            upperCase.getOperand().accept(this);
            append(')');
        }

    }
}
