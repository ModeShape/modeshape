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
package org.modeshape.jcr.query.model;

/**
 * The basic interface for all query visitor implementations.
 */
public interface Visitor {

    void visit( Relike obj );

    void visit( AllNodes obj );

    void visit( And obj );

    void visit( Between obj );

    void visit( BindVariableName obj );

    void visit( ChildCount obj );

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

    void visit( NodeId obj );

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

    void visit( Cast cast);
}
