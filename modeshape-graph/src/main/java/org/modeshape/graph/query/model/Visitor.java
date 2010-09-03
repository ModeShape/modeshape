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

/**
 * The basic interface for all query visitor implementations.
 */
public interface Visitor {

    void visit( AllNodes obj );

    void visit( And obj );

    void visit( Between obj );

    void visit( BindVariableName obj );

    void visit( ChildNode obj );

    void visit( ChildNodeJoinCondition obj );

    void visit( Column obj );

    void visit( Comparison obj );

    void visit( DescendantNode obj );

    void visit( DescendantNodeJoinCondition obj );

    void visit( EquiJoinCondition obj );

    void visit( FullTextSearch obj );

    void visit( FullTextSearchScore obj );

    void visit( Join obj );

    void visit( Length obj );

    void visit( Limit limit );

    void visit( Literal obj );

    void visit( LowerCase obj );

    void visit( NodeLocalName obj );

    void visit( NodeName obj );

    void visit( NodePath obj );

    void visit( NodeDepth obj );

    void visit( NamedSelector obj );

    void visit( Not obj );

    void visit( Or obj );

    void visit( Ordering obj );

    void visit( PropertyExistence obj );

    void visit( PropertyValue obj );

    void visit( Query obj );

    void visit( Subquery obj );

    void visit( ReferenceValue obj );

    void visit( SameNode obj );

    void visit( SameNodeJoinCondition obj );

    void visit( SetCriteria obj );

    void visit( SetQuery obj );

    void visit( ArithmeticOperand obj );

    void visit( UpperCase obj );

}
