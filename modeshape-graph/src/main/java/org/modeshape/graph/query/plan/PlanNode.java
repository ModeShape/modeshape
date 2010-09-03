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
package org.modeshape.graph.query.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.Readable;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Visitable;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.model.SetQuery.Operation;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A representation of a single node within a plan tree.
 */
@NotThreadSafe
public final class PlanNode implements Iterable<PlanNode>, Readable, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * An enumeration dictating the type of plan tree nodes.
     */
    public enum Type {

        /** A node that represents an access of the underlying storage. */
        ACCESS("Access"),
        /** A node that defines the removal of duplicate tuples. */
        DUP_REMOVE("DupRemoval"),
        /** A node that defines the join type, join criteria, and join strategy */
        JOIN("Join"),
        /** A node that defines the columns returned from the node. */
        PROJECT("Project"),
        /** A node that selects a filters the tuples by applying a criteria evaluation filter node (WHERE / HAVING) */
        SELECT("Select"),
        /** A node that defines the columns to sort on, the sort direction for each column, and whether to remove duplicates. */
        SORT("Sort"),
        /** A node that defines the 'table' from which the tuples are being obtained */
        SOURCE("Source"),
        /** A node that groups sets of rows into groups (and where aggregation would be performed) */
        GROUP("Group"),
        /** A node that produces no results */
        NULL("Null"),
        /** A node that limits the number of tuples returned */
        LIMIT("Limit"),
        /** A node the performs set operations on two sets of tuples, including UNION */
        SET_OPERATION("SetOperation"),
        /** A node that contains two nodes, where the left side must be done before the right */
        DEPENDENT_QUERY("DependentQuery");

        private static final Map<String, Type> TYPE_BY_SYMBOL;
        static {
            Map<String, Type> typesBySymbol = new HashMap<String, Type>();
            for (Type type : Type.values()) {
                typesBySymbol.put(type.getSymbol().toUpperCase(), type);
            }
            TYPE_BY_SYMBOL = Collections.unmodifiableMap(typesBySymbol);
        }

        private final String symbol;

        private Type( String symbol ) {
            this.symbol = symbol;
        }

        /**
         * Get the symbol representation of this node type.
         * 
         * @return the symbol; never null and never empty
         */
        public String getSymbol() {
            return symbol;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return symbol;
        }

        /**
         * Attempt to find the Type given a symbol. The matching is done independent of case.
         * 
         * @param symbol the symbol
         * @return the Type having the supplied symbol, or null if there is no Type with the supplied symbol
         * @throws IllegalArgumentException if the symbol is null
         */
        public static Type forSymbol( String symbol ) {
            CheckArg.isNotNull(symbol, "symbol");
            return TYPE_BY_SYMBOL.get(symbol.toUpperCase().trim());
        }
    }

    /**
     * An enumeration dictating the type of plan tree nodes.
     */
    public enum Property {
        /** For SELECT and JOIN nodes, a flag specifying whether the criteria is dependent. Value is a {@link Boolean} object. */
        IS_DEPENDENT,

        /** For SELECT nodes, the criteria object that is to be applied. Value is a {@link Constraint} object. */
        SELECT_CRITERIA,

        /** For SET_OPERATION nodes, the type of set operation to be performed. Value is a {@link Operation} object. */
        SET_OPERATION,
        /** For SET_OPERATION nodes, whether the 'all' clause is used. Value is a {@link Boolean} object. */
        SET_USE_ALL,

        /** For JOIN nodes, the type of join operation. Value is a {@link JoinType} object. */
        JOIN_TYPE,
        /** For JOIN nodes, the type of join algorithm. Value is a {@link JoinAlgorithm} object. */
        JOIN_ALGORITHM,
        /** For JOIN nodes, the join criteria (or join condition). Value is a {@link JoinCondition} object. */
        JOIN_CONDITION,
        /**
         * For JOIN nodes, additional criteria that have been pushed down to the join. Value is a List of {@link Constraint}
         * object.
         */
        JOIN_CONSTRAINTS,

        /** For SOURCE nodes, the literal name of the selector. Value is a {@link SelectorName} object. */
        SOURCE_NAME,
        /** For SOURCE nodes, the alias name of the selector. Value is a {@link SelectorName} object. */
        SOURCE_ALIAS,
        /**
         * For SOURCE nodes, the collection of columns that are available. Value is a Collection of {@link Schemata.Column}
         * objects.
         */
        SOURCE_COLUMNS,

        /** For PROJECT nodes, the ordered collection of columns being projected. Value is a Collection of {@link Column} objects. */
        PROJECT_COLUMNS,
        /**
         * For PROJECT nodes, the ordered collection of the type names for the columns being projected. Value is a Collection of
         * {@link String} objects.
         */
        PROJECT_COLUMN_TYPES,

        /**
         * For GROUP nodes, the ordered collection of columns used to group the result tuples. Value is a Collection of
         * {@link Column} objects.
         */
        GROUP_COLUMNS,

        /**
         * For SET_OPERATION nodes, the list of orderings for the results. Value is either a Collection of {@link Ordering}
         * objects or a collection of {@link SelectorName} objects (if the sorting is being done as an input to a merge-join).
         */
        SORT_ORDER_BY,

        /** For LIMIT nodes, the maximum number of rows to return. Value is an {@link Integer} object. */
        LIMIT_COUNT,
        /** For LIMIT nodes, the offset value. Value is an {@link Integer} object. */
        LIMIT_OFFSET,

        /**
         * For ACESS nodes, this signifies that the node will never return results. Value is a {@link Boolean} object, though the
         * mere presence of this property signifies that it is no longer needed.
         */
        ACCESS_NO_RESULTS,

        /** For dependenty queries, defines the variable where the results will be placed. */
        VARIABLE_NAME
    }

    private Type type;
    private PlanNode parent;
    private LinkedList<PlanNode> children = new LinkedList<PlanNode>();
    private List<PlanNode> childrenView = Collections.unmodifiableList(children);
    private Map<Property, Object> nodeProperties;

    /** The set of named selectors (e.g., tables) that this node deals with. */
    private Set<SelectorName> selectors = new HashSet<SelectorName>();

    /**
     * Create a new plan node with the supplied initial type.
     * 
     * @param type the type of the node; may not be null
     */
    public PlanNode( Type type ) {
        assert type != null;
        this.type = type;
    }

    /**
     * Create a new plan node with the supplied initial type ad that is a child of the supplied parent.
     * 
     * @param type the type of the node; may not be null
     * @param parent the parent node, or null if there is no parent
     */
    public PlanNode( Type type,
                     PlanNode parent ) {
        assert type != null;
        this.type = type;
        if (parent != null) {
            this.parent = parent;
            this.parent.children.add(this);
        }
    }

    /**
     * Create a new plan node with the supplied initial type ad that is a child of the supplied parent.
     * 
     * @param type the type of the node; may not be null
     * @param selectors the selectors that should be assigned to this node
     */
    public PlanNode( Type type,
                     SelectorName... selectors ) {
        this(type);
        for (SelectorName selector : selectors) {
            addSelector(selector);
        }
    }

    /**
     * Create a new plan node with the supplied initial type ad that is a child of the supplied parent.
     * 
     * @param type the type of the node; may not be null
     * @param selectors the selectors that should be assigned to this node
     */
    public PlanNode( Type type,
                     Iterable<SelectorName> selectors ) {
        this(type);
        addSelectors(selectors);
    }

    /**
     * Create a new plan node with the supplied initial type ad that is a child of the supplied parent.
     * 
     * @param type the type of the node; may not be null
     * @param parent the parent node, or null if there is no parent
     * @param selectors the selectors that should be assigned to this node
     */
    public PlanNode( Type type,
                     PlanNode parent,
                     SelectorName... selectors ) {
        this(type, parent);
        for (SelectorName selector : selectors) {
            addSelector(selector);
        }
    }

    /**
     * Create a new plan node with the supplied initial type ad that is a child of the supplied parent.
     * 
     * @param type the type of the node; may not be null
     * @param parent the parent node, or null if there is no parent
     * @param selectors the selectors that should be assigned to this node
     */
    public PlanNode( Type type,
                     PlanNode parent,
                     Iterable<SelectorName> selectors ) {
        this(type, parent);
        addSelectors(selectors);
    }

    /**
     * Get the type for this node.
     * 
     * @return the node type, or null if there is no node type
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the type for this node.
     * 
     * @param type Sets type to the specified value; may not be null
     */
    public void setType( Type type ) {
        assert type != null;
        this.type = type;
    }

    /**
     * Return true if this node's type does not match the supplied type
     * 
     * @param type the type to compare
     * @return true if this node's type is different than that supplied, or false if they are the same
     */
    public boolean isNot( Type type ) {
        return this.type != type;
    }

    /**
     * Return true if this node's type does not match any of the supplied types
     * 
     * @param first the type to compare
     * @param rest the additional types to compare
     * @return true if this node's type is different than all of those supplied, or false if matches one of the supplied types
     */
    public boolean isNotOneOf( Type first,
                               Type... rest ) {
        return isNotOneOf(EnumSet.of(first, rest));
    }

    /**
     * Return true if this node's type does not match any of the supplied types
     * 
     * @param types the types to compare
     * @return true if this node's type is different than all of those supplied, or false if matches one of the supplied types
     */
    public boolean isNotOneOf( Set<Type> types ) {
        return !types.contains(type);
    }

    /**
     * Return true if this node's type does match the supplied type
     * 
     * @param type the type to compare
     * @return true if this node's type is the same as that supplied, or false if the types are different
     */
    public boolean is( Type type ) {
        return this.type == type;
    }

    /**
     * Return true if this node's type matches one of the supplied types
     * 
     * @param first the type to compare
     * @param rest the additional types to compare
     * @return true if this node's type is one of those supplied, or false otherwise
     */
    public boolean isOneOf( Type first,
                            Type... rest ) {
        return isOneOf(EnumSet.of(first, rest));
    }

    /**
     * Return true if this node's type matches one of the supplied types
     * 
     * @param types the types to compare
     * @return true if this node's type is one of those supplied, or false otherwise
     */
    public boolean isOneOf( Set<Type> types ) {
        return types.contains(type);
    }

    /**
     * Determine if the supplied node is an ancestor of this node.
     * 
     * @param possibleAncestor the node that is to be determined if it is an ancestor
     * @return true if the supplied node is indeed an ancestor, or false if it is not an ancestor
     */
    public boolean isBelow( PlanNode possibleAncestor ) {
        PlanNode node = this;
        while (node != null) {
            if (node == possibleAncestor) return true;
            node = node.getParent();
        }
        return false;
    }

    /**
     * Determine if the supplied node is a descendant of this node.
     * 
     * @param possibleDescendant the node that is to be determined if it is a descendant
     * @return true if the supplied node is indeed an ancestor, or false if it is not an ancestor
     */
    public boolean isAbove( PlanNode possibleDescendant ) {
        return possibleDescendant != null && possibleDescendant.isBelow(this);
    }

    /**
     * Get the parent of this node.
     * 
     * @return the parent node, or null if this node has no parent
     */
    public PlanNode getParent() {
        return parent;
    }

    /**
     * Set the parent for this node. If this node already has a parent, this method will remove this node from the current parent.
     * If the supplied parent is not null, then this node will be added to the supplied parent's children.
     * 
     * @param parent the new parent, or null if this node is to have no parent
     */
    public void setParent( PlanNode parent ) {
        removeFromParent();
        if (parent != null) {
            this.parent = parent;
            this.parent.children.add(this);
        }
    }

    /**
     * Insert the supplied node into the plan node tree immediately above this node. If this node has a parent when this method is
     * called, the new parent essentially takes the place of this node within the list of children of the old parent. This method
     * does nothing if the supplied new parent is null.
     * <p>
     * For example, consider a plan node tree before this method is called:
     * 
     * <pre>
     *        A
     *      / | \
     *     /  |  \
     *    B   C   D
     * </pre>
     * 
     * Then after this method is called with <code>c.insertAsParent(e)</code>, the resulting plan node tree will be:
     * 
     * <pre>
     *        A
     *      / | \
     *     /  |  \
     *    B   E   D
     *        |
     *        |
     *        C
     * </pre>
     * 
     * </p>
     * <p>
     * Also note that the node on which this method is called ('C' in the example above) will always be added as the
     * {@link #addLastChild(PlanNode) last child} to the new parent. This allows the new parent to already have children before
     * this method is called.
     * </p>
     * 
     * @param newParent the new parent; method does nothing if this is null
     */
    public void insertAsParent( PlanNode newParent ) {
        if (newParent == null) return;
        newParent.removeFromParent();
        if (this.parent != null) {
            this.parent.replaceChild(this, newParent);
        }
        newParent.addLastChild(this);
    }

    /**
     * Remove this node from its parent, and return the node that used to be the parent of this node. Note that this method
     * removes the entire subgraph under this node.
     * 
     * @return the node that was the parent of this node, or null if this node had no parent
     * @see #extractChild(PlanNode)
     * @see #extractFromParent()
     */
    public PlanNode removeFromParent() {
        PlanNode result = this.parent;
        if (this.parent != null) {
            // Remove this node from its current parent ...
            this.parent.children.remove(this);
            this.parent = null;
        }
        return result;
    }

    /**
     * Get the unmodifiable list of child nodes. This list will immediately reflect any changes made to the children (via other
     * methods), but this list cannot be used to add or remove children.
     * 
     * @return the list of children, which immediately reflects changes but which cannot be modified directly; never null
     */
    public List<PlanNode> getChildren() {
        return childrenView;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This iterator is immutable.
     * </p>
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<PlanNode> iterator() {
        return childrenView.iterator();
    }

    /**
     * Remove all children from this node. All nodes immediately become orphaned. The resulting list will be mutable.
     * 
     * @return a copy of all the of the children that were removed (and which have no parent); never null
     */
    public List<PlanNode> removeAllChildren() {
        if (this.children.isEmpty()) {
            return new ArrayList<PlanNode>(0);
        }
        List<PlanNode> copyOfChildren = new ArrayList<PlanNode>(this.children);
        for (Iterator<PlanNode> childIter = this.children.iterator(); childIter.hasNext();) {
            PlanNode child = childIter.next();
            childIter.remove();
            child.parent = null;
        }
        return copyOfChildren;
    }

    /**
     * Replace the supplied child with another node. If the replacement is already a child of this node, this method effectively
     * swaps the position of the child and replacement nodes.
     * 
     * @param child the node that is already a child and that is to be replaced; may not be null and must be a child
     * @param replacement the node that is to replace the 'child' node; may not be null
     * @return true if the child was successfully replaced
     */
    public boolean replaceChild( PlanNode child,
                                 PlanNode replacement ) {
        assert child != null;
        assert replacement != null;
        if (child.parent == this) {
            int i = this.children.indexOf(child);
            if (replacement.parent == this) {
                // Swapping the positions ...
                int j = this.children.indexOf(replacement);
                this.children.set(i, replacement);
                this.children.set(j, child);
                return true;
            }
            // The replacement is not yet a child ...
            this.children.set(i, replacement);
            replacement.removeFromParent();
            replacement.parent = this;
            child.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Get the number of child nodes.
     * 
     * @return the number of children; never negative
     */
    public int getChildCount() {
        return this.children.size();
    }

    /**
     * Get the first child.
     * 
     * @return the first child, or null if there are no children
     */
    public PlanNode getFirstChild() {
        return this.children.isEmpty() ? null : this.children.getFirst();
    }

    /**
     * Get the last child.
     * 
     * @return the last child, or null if there are no children
     */
    public PlanNode getLastChild() {
        return this.children.isEmpty() ? null : this.children.getLast();
    }

    /**
     * Get the child at the supplied index.
     * 
     * @param index the index
     * @return the child, or null if there are no children
     * @throws IndexOutOfBoundsException if the index is not valid given the number of children
     */
    public PlanNode getChild( int index ) {
        return this.children.isEmpty() ? null : this.children.get(index);
    }

    /**
     * Add the supplied node to the front of the list of children.
     * 
     * @param child the node that should be added as the first child; may not be null
     */
    public void addFirstChild( PlanNode child ) {
        assert child != null;
        this.children.addFirst(child);
        child.removeFromParent();
        child.parent = this;
    }

    /**
     * Add the supplied node to the end of the list of children.
     * 
     * @param child the node that should be added as the last child; may not be null
     */
    public void addLastChild( PlanNode child ) {
        assert child != null;
        this.children.addLast(child);
        child.removeFromParent();
        child.parent = this;
    }

    /**
     * Add the supplied nodes at the end of the list of children.
     * 
     * @param otherChildren the children to add; may not be null
     */
    public void addChildren( Iterable<PlanNode> otherChildren ) {
        assert otherChildren != null;
        for (PlanNode planNode : otherChildren) {
            this.addLastChild(planNode);
        }
    }

    /**
     * Add the supplied nodes at the end of the list of children.
     * 
     * @param first the first child to add
     * @param second the second child to add
     */
    public void addChildren( PlanNode first,
                             PlanNode second ) {
        if (first != null) this.addLastChild(first);
        if (second != null) this.addLastChild(second);
    }

    /**
     * Add the supplied nodes at the end of the list of children.
     * 
     * @param first the first child to add
     * @param second the second child to add
     * @param third the third child to add
     */
    public void addChildren( PlanNode first,
                             PlanNode second,
                             PlanNode third ) {
        if (first != null) this.addLastChild(first);
        if (second != null) this.addLastChild(second);
        if (third != null) this.addLastChild(third);
    }

    /**
     * Remove the node from this node.
     * 
     * @param child the child node; may not be null
     * @return true if the child was removed from this node, or false if the supplied node was not a child of this node
     */
    public boolean removeChild( PlanNode child ) {
        boolean result = this.children.remove(child);
        if (result) {
            child.parent = null;
        }
        return result;
    }

    /**
     * Remove the child node from this node, and replace that child with its first child (if there is one).
     * 
     * @param child the child to be extracted; may not be null and must have at most 1 child
     * @see #extractFromParent()
     */
    public void extractChild( PlanNode child ) {
        if (child.getChildCount() == 0) {
            removeChild(child);
        } else {
            PlanNode grandChild = child.getFirstChild();
            replaceChild(child, grandChild);
        }
    }

    /**
     * Extract this node from its parent, but replace this node with its child (if there is one).
     * 
     * @see #extractChild(PlanNode)
     */
    public void extractFromParent() {
        this.parent.extractChild(this);
    }

    /**
     * Get the keys for the property values that are set on this node.
     * 
     * @return the property keys; never null but possibly empty
     */
    public Set<Property> getPropertyKeys() {
        return nodeProperties != null ? nodeProperties.keySet() : Collections.<Property>emptySet();
    }

    /**
     * Get the node's value for this supplied property.
     * 
     * @param propertyId the property identifier
     * @return the value, or null if there is no property on this node
     */
    public Object getProperty( Property propertyId ) {
        return nodeProperties != null ? nodeProperties.get(propertyId) : null;
    }

    /**
     * Get the node's value for this supplied property, casting the result to the supplied type.
     * 
     * @param <ValueType> the type of the value expected
     * @param propertyId the property identifier
     * @param type the class denoting the type of value expected; may not be null
     * @return the value, or null if there is no property on this node
     */
    public <ValueType> ValueType getProperty( Property propertyId,
                                              Class<ValueType> type ) {
        return nodeProperties != null ? type.cast(nodeProperties.get(propertyId)) : null;
    }

    /**
     * Get the node's value for this supplied property, casting the result to a {@link Collection} of the supplied type.
     * 
     * @param <ValueType> the type of the value expected
     * @param propertyId the property identifier
     * @param type the class denoting the type of value expected; may not be null
     * @return the value, or null if there is no property on this node
     */
    @SuppressWarnings( "unchecked" )
    public <ValueType> Collection<ValueType> getPropertyAsCollection( Property propertyId,
                                                                      Class<ValueType> type ) {
        if (nodeProperties == null) return null;
        return (Collection)nodeProperties.get(propertyId);
    }

    /**
     * Get the node's value for this supplied property, casting the result to a {@link List} of the supplied type.
     * 
     * @param <ValueType> the type of the value expected
     * @param propertyId the property identifier
     * @param type the class denoting the type of value expected; may not be null
     * @return the value, or null if there is no property on this node
     */
    @SuppressWarnings( "unchecked" )
    public <ValueType> List<ValueType> getPropertyAsList( Property propertyId,
                                                          Class<ValueType> type ) {
        if (nodeProperties == null) return null;
        return (List)nodeProperties.get(propertyId);
    }

    /**
     * Set the node's value for the supplied property.
     * 
     * @param propertyId the property identifier
     * @param value the value, or null if the property is to be removed
     * @return the previous value that was overwritten by this call, or null if there was prior value
     */
    public Object setProperty( Property propertyId,
                               Object value ) {
        if (value == null) {
            // Removing this property ...
            return nodeProperties != null ? nodeProperties.remove(propertyId) : null;
        }
        // Otherwise, we're adding the property
        if (nodeProperties == null) nodeProperties = new HashMap<Property, Object>();
        return nodeProperties.put(propertyId, value);
    }

    /**
     * Remove the node's value for this supplied property.
     * 
     * @param propertyId the property identifier
     * @return the value that was removed, or null if there was no property on this node
     */
    public Object removeProperty( Object propertyId ) {
        return nodeProperties != null ? nodeProperties.remove(propertyId) : null;
    }

    /**
     * Indicates if there is a non-null value for the property.
     * 
     * @param propertyId the property identifier
     * @return true if this node has a non-null value for that property, or false if the value is null or there is no property
     */
    public boolean hasProperty( Property propertyId ) {
        return nodeProperties != null && nodeProperties.containsKey(propertyId);
    }

    /**
     * Indicates if there is a non-null and non-empty Collection value for the property.
     * 
     * @param propertyId the property identifier
     * @return true if this node has value for the supplied property and that value is a non-empty Collection
     */
    public boolean hasCollectionProperty( Property propertyId ) {
        Object value = getProperty(propertyId);
        return (value instanceof Collection<?> && !((Collection<?>)value).isEmpty());
    }

    /**
     * Indicates if there is a non-null property value that equates to a <code>true</code> boolean value.
     * 
     * @param propertyId the property identifier
     * @return true if this node has value for the supplied property and that value is a boolean value of <code>true</code>
     */
    public boolean hasBooleanProperty( Property propertyId ) {
        Object value = getProperty(propertyId);
        return (value instanceof Boolean && ((Boolean)value).booleanValue());
    }

    /**
     * Add a selector to this plan node. This method does nothing if the supplied selector is null.
     * 
     * @param symbol the symbol of the selector
     */
    public void addSelector( SelectorName symbol ) {
        if (symbol != null) selectors.add(symbol);
    }

    /**
     * Add the selectors to this plan node. This method does nothing for any supplied selector that is null.
     * 
     * @param first the first symbol to be added
     * @param second the second symbol to be added
     */
    public void addSelector( SelectorName first,
                             SelectorName second ) {
        if (first != null) selectors.add(first);
        if (second != null) selectors.add(second);
    }

    /**
     * Add the selectors to this plan node. This method does nothing for any supplied selector that is null.
     * 
     * @param names the symbols to be added
     */
    public void addSelectors( Iterable<SelectorName> names ) {
        for (SelectorName name : names) {
            if (name != null) selectors.add(name);
        }
    }

    /**
     * Get the selectors that are referenced by this plan node.
     * 
     * @return the names of the selectors; never null but possibly empty
     */
    public Set<SelectorName> getSelectors() {
        return selectors;
    }

    /**
     * Get the path from this node (inclusive) to the supplied descendant node (inclusive)
     * 
     * @param descendant the descendant; may not be null, and must be a descendant of this node
     * @return the path from this node to the supplied descendant node; never null
     */
    public LinkedList<PlanNode> getPathTo( PlanNode descendant ) {
        assert descendant != null;
        LinkedList<PlanNode> stack = new LinkedList<PlanNode>();
        PlanNode node = descendant;
        while (node != this) {
            stack.addFirst(node);
            node = node.getParent();
            assert node != null : "The supplied node is not a descendant of this node";
        }
        stack.addFirst(this);
        return stack;
    }

    /**
     * Determine whether this node has an ancestor with the supplied type.
     * 
     * @param type the type; may not be null
     * @return true if there is at least one ancestor of the supplied type, or false otherwise
     */
    public boolean hasAncestorOfType( Type type ) {
        return hasAncestorOfType(EnumSet.of(type));
    }

    /**
     * Determine whether this node has an ancestor with any of the supplied types.
     * 
     * @param firstType the first type; may not be null
     * @param additionalTypes the additional types; may not be null
     * @return true if there is at least one ancestor that has any of the supplied types, or false otherwise
     */
    public boolean hasAncestorOfType( Type firstType,
                                      Type... additionalTypes ) {
        return hasAncestorOfType(EnumSet.of(firstType, additionalTypes));
    }

    /**
     * Determine whether this node has an ancestor with any of the supplied types.
     * 
     * @param types the types; may not be null
     * @return true if there is at least one ancestor that has any of the supplied types, or false otherwise
     */
    public boolean hasAncestorOfType( Set<Type> types ) {
        PlanNode node = this.parent;
        while (node != null) {
            if (types.contains(node.getType())) return true;
            node = node.getParent();
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
        return getString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals( Object obj ) {
        // Quite a few methods rely upon instance/reference equality ...
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This class returns a new clone of the plan tree rooted at this node. However, the top node of the resulting plan tree (that
     * is, the node returned from this method) has no parent.
     * </p>
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public PlanNode clone() {
        return cloneWithoutNewParent();
    }

    protected PlanNode cloneWithoutNewParent() {
        PlanNode result = new PlanNode(this.type, null, this.selectors);
        if (this.nodeProperties != null && !this.nodeProperties.isEmpty()) {
            result.nodeProperties = new HashMap<Property, Object>(this.nodeProperties);
        }
        // Clone the children ...
        for (PlanNode child : children) {
            PlanNode childClone = child.cloneWithoutNewParent();
            // The child has no parent, so add the child to the new result ...
            result.addLastChild(childClone);
        }
        return result;
    }

    /**
     * Determine whether the supplied plan is equivalent to this plan.
     * 
     * @param other the other plan to compare with this instance
     * @return true if the two plans are equivalent, or false otherwise
     */
    public boolean isSameAs( PlanNode other ) {
        if (other == null) return false;
        if (this.getType() != other.getType()) return false;
        if (!ObjectUtil.isEqualWithNulls(this.nodeProperties, other.nodeProperties)) return false;
        if (!this.getSelectors().equals(other.getSelectors())) return false;
        if (this.getChildCount() != other.getChildCount()) return false;
        Iterator<PlanNode> thisChildren = this.getChildren().iterator();
        Iterator<PlanNode> thatChildren = other.getChildren().iterator();
        while (thisChildren.hasNext() && thatChildren.hasNext()) {
            if (!thisChildren.next().isSameAs(thatChildren.next())) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Readable#getString()
     */
    public String getString() {
        StringBuilder sb = new StringBuilder();
        getRecursiveString(sb, 0);
        return sb.toString();
    }

    private void getRecursiveString( StringBuilder str,
                                     int indentLevel ) {
        for (int i = 0; i < indentLevel; ++i) {
            str.append("  ");
        }
        getNodeString(str).append('\n');

        // Recursively add children at one greater tab level
        for (PlanNode child : this) {
            child.getRecursiveString(str, indentLevel + 1);
        }
    }

    private StringBuilder getNodeString( StringBuilder str ) {
        str.append(this.type.getSymbol());
        if (!selectors.isEmpty()) {
            str.append(" [");
            boolean first = true;
            for (SelectorName symbol : selectors) {
                if (first) first = false;
                else str.append(',');
                str.append(symbol.name());
            }
            str.append(']');
        }
        if (nodeProperties != null && !nodeProperties.isEmpty()) {
            str.append(" <");
            boolean first = true;
            for (Map.Entry<Property, Object> entry : nodeProperties.entrySet()) {
                if (first) first = false;
                else str.append(", ");
                str.append(entry.getKey()).append('=');
                Object value = entry.getValue();
                if (value instanceof Visitable) {
                    str.append(Visitors.readable((Visitable)value));
                } else if (value instanceof Collection<?>) {
                    boolean firstItem = true;
                    str.append('[');
                    for (Object item : (Collection<?>)value) {
                        if (firstItem) firstItem = false;
                        else str.append(", ");
                        if (item instanceof Visitable) {
                            str.append(Visitors.readable((Visitable)item));
                        } else {
                            str.append(item);
                        }
                    }
                    str.append(']');
                } else {
                    str.append(value);
                }
            }
            str.append('>');
        }
        return str;
    }

    /**
     * Starting at the parent of this node, find the lowest (also closest) ancestor that has the specified type.
     * 
     * @param typeToFind the type of the node to find; may not be null
     * @return the node with the specified type, or null if there is no such ancestor
     */
    public PlanNode findAncestor( Type typeToFind ) {
        return findAncestor(EnumSet.of(typeToFind));
    }

    /**
     * Starting at the parent of this node, find the lowest (also closest) ancestor that has one of the specified types.
     * 
     * @param firstTypeToFind the first type to find; may not be null
     * @param additionalTypesToFind additional types to find; may not be null
     * @return the node with the specified type, or null if there is no such ancestor
     */
    public PlanNode findAncestor( Type firstTypeToFind,
                                  Type... additionalTypesToFind ) {
        return findAncestor(EnumSet.of(firstTypeToFind, additionalTypesToFind));
    }

    /**
     * Starting at the parent of this node, find the lowest (also closest) ancestor that has one of the specified types.
     * 
     * @param typesToFind the set of types to find; may not be null
     * @return the node with one of the specified types, or null if there is no such ancestor
     */
    public PlanNode findAncestor( Set<Type> typesToFind ) {
        PlanNode node = this;
        PlanNode parent = null;
        while ((parent = node.getParent()) != null) {
            if (typesToFind.contains(parent.getType())) return parent;
            node = parent;
        }
        return null;
    }

    /**
     * Look at nodes below this node, searching for nodes that have the supplied type. As soon as a node with a matching type is
     * found, then no other nodes below it are searched.
     * 
     * @param typeToFind the type of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have the supplied type; never null but possibly
     *         empty
     */
    public List<PlanNode> findAllFirstNodesAtOrBelow( Type typeToFind ) {
        List<PlanNode> results = new LinkedList<PlanNode>();
        LinkedList<PlanNode> queue = new LinkedList<PlanNode>();
        queue.add(this);
        while (!queue.isEmpty()) {
            PlanNode aNode = queue.poll();
            if (aNode.getType() == Type.PROJECT) {
                results.add(aNode);
            } else {
                queue.addAll(0, aNode.getChildren());
            }
        }
        return results;
    }

    public static enum Traversal {
        LEVEL_ORDER,
        PRE_ORDER;
    }

    /**
     * Find all of the nodes that are at or below this node.
     * 
     * @return the collection of nodes that are at or below this node; never null and never empty
     */
    public List<PlanNode> findAllAtOrBelow() {
        return findAllAtOrBelow(Traversal.PRE_ORDER);
    }

    /**
     * Find all of the nodes that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @return the collection of nodes that are at or below this node; never null and never empty
     */
    public List<PlanNode> findAllAtOrBelow( Traversal order ) {
        assert order != null;
        List<PlanNode> results = new LinkedList<PlanNode>();
        LinkedList<PlanNode> queue = new LinkedList<PlanNode>();
        queue.add(this);
        while (!queue.isEmpty()) {
            PlanNode aNode = queue.poll();
            switch (order) {
                case LEVEL_ORDER:
                    queue.addAll(aNode.getChildren());
                    break;
                case PRE_ORDER:
                    queue.addAll(0, aNode.getChildren());
                    break;
            }
        }
        return results;
    }

    /**
     * Find all of the nodes of the specified type that are at or below this node, using pre-order traversal.
     * 
     * @param typeToFind the type of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have the supplied type; never null but possibly
     *         empty
     */
    public List<PlanNode> findAllAtOrBelow( Type typeToFind ) {
        return findAllAtOrBelow(EnumSet.of(typeToFind));
    }

    /**
     * Find all of the nodes with one of the specified types that are at or below this node.
     * 
     * @param firstTypeToFind the first type of node to find; may not be null
     * @param additionalTypesToFind the additional types of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have one of the supplied types; never null but
     *         possibly empty
     */
    public List<PlanNode> findAllAtOrBelow( Type firstTypeToFind,
                                            Type... additionalTypesToFind ) {
        return findAllAtOrBelow(EnumSet.of(firstTypeToFind, additionalTypesToFind));
    }

    /**
     * Find all of the nodes with one of the specified types that are at or below this node.
     * 
     * @param typesToFind the types of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have one of the supplied types; never null but
     *         possibly empty
     */
    public List<PlanNode> findAllAtOrBelow( Set<Type> typesToFind ) {
        return findAllAtOrBelow(Traversal.PRE_ORDER, typesToFind);
    }

    /**
     * Find all of the nodes of the specified type that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param typeToFind the type of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have the supplied type; never null but possibly
     *         empty
     */
    public List<PlanNode> findAllAtOrBelow( Traversal order,
                                            Type typeToFind ) {
        return findAllAtOrBelow(order, EnumSet.of(typeToFind));
    }

    /**
     * Find all of the nodes with one of the specified types that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param firstTypeToFind the first type of node to find; may not be null
     * @param additionalTypesToFind the additional types of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have one of the supplied types; never null but
     *         possibly empty
     */
    public List<PlanNode> findAllAtOrBelow( Traversal order,
                                            Type firstTypeToFind,
                                            Type... additionalTypesToFind ) {
        return findAllAtOrBelow(order, EnumSet.of(firstTypeToFind, additionalTypesToFind));
    }

    /**
     * Find all of the nodes with one of the specified types that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param typesToFind the types of node to find; may not be null
     * @return the collection of nodes that are at or below this node that all have one of the supplied types; never null but
     *         possibly empty
     */
    public List<PlanNode> findAllAtOrBelow( Traversal order,
                                            Set<Type> typesToFind ) {
        assert order != null;
        List<PlanNode> results = new LinkedList<PlanNode>();
        LinkedList<PlanNode> queue = new LinkedList<PlanNode>();
        queue.add(this);
        while (!queue.isEmpty()) {
            PlanNode aNode = queue.poll();
            if (typesToFind.contains(aNode.getType())) {
                results.add(aNode);
            }
            switch (order) {
                case LEVEL_ORDER:
                    queue.addAll(aNode.getChildren());
                    break;
                case PRE_ORDER:
                    queue.addAll(0, aNode.getChildren());
                    break;
            }
        }
        return results;
    }

    /**
     * Find the first node with the specified type that are at or below this node.
     * 
     * @param typeToFind the type of node to find; may not be null
     * @return the first node that is at or below this node that has the supplied type; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Type typeToFind ) {
        return findAtOrBelow(EnumSet.of(typeToFind));
    }

    /**
     * Find the first node with one of the specified types that are at or below this node.
     * 
     * @param firstTypeToFind the first type of node to find; may not be null
     * @param additionalTypesToFind the additional types of node to find; may not be null
     * @return the first node that is at or below this node that has one of the supplied types; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Type firstTypeToFind,
                                   Type... additionalTypesToFind ) {
        return findAtOrBelow(EnumSet.of(firstTypeToFind, additionalTypesToFind));
    }

    /**
     * Find the first node with one of the specified types that are at or below this node.
     * 
     * @param typesToFind the types of node to find; may not be null
     * @return the first node that is at or below this node that has one of the supplied types; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Set<Type> typesToFind ) {
        return findAtOrBelow(Traversal.PRE_ORDER, typesToFind);
    }

    /**
     * Find the first node with the specified type that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param typeToFind the type of node to find; may not be null
     * @return the first node that is at or below this node that has the supplied type; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Traversal order,
                                   Type typeToFind ) {
        return findAtOrBelow(order, EnumSet.of(typeToFind));
    }

    /**
     * Find the first node with one of the specified types that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param firstTypeToFind the first type of node to find; may not be null
     * @param additionalTypesToFind the additional types of node to find; may not be null
     * @return the first node that is at or below this node that has one of the supplied types; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Traversal order,
                                   Type firstTypeToFind,
                                   Type... additionalTypesToFind ) {
        return findAtOrBelow(order, EnumSet.of(firstTypeToFind, additionalTypesToFind));
    }

    /**
     * Find the first node with one of the specified types that are at or below this node.
     * 
     * @param order the order to traverse; may not be null
     * @param typesToFind the types of node to find; may not be null
     * @return the first node that is at or below this node that has one of the supplied types; or null if there is no such node
     */
    public PlanNode findAtOrBelow( Traversal order,
                                   Set<Type> typesToFind ) {
        LinkedList<PlanNode> queue = new LinkedList<PlanNode>();
        queue.add(this);
        while (!queue.isEmpty()) {
            PlanNode aNode = queue.poll();
            if (typesToFind.contains(aNode.getType())) {
                return aNode;
            }
            switch (order) {
                case LEVEL_ORDER:
                    queue.addAll(aNode.getChildren());
                    break;
                case PRE_ORDER:
                    queue.addAll(0, aNode.getChildren());
                    break;
            }
        }
        return null;
    }

}
