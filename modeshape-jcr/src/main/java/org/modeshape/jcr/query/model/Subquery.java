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
 * A representation of a non-correlated subquery. This component uses composition to hold the various types of QueryCommand
 * objects, rather than inheriting from StaticOperand and QueryCommand.
 */
public class Subquery implements StaticOperand, org.modeshape.jcr.api.query.qom.Subquery {

    private static final long serialVersionUID = 1L;

    public static final String VARIABLE_PREFIX = "__mode:subquery";

    public static boolean isSubqueryVariableName( String variableName ) {
        return variableName.startsWith(VARIABLE_PREFIX);
    }

    private final QueryCommand query;

    /**
     * Create a new subquery component that uses the supplied query as the subquery expression.
     * 
     * @param query the Command representing the subquery.
     */
    public Subquery( QueryCommand query ) {
        this.query = query;
    }

    @Override
    public QueryCommand getQuery() {
        return query;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return query.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Subquery) {
            Subquery that = (Subquery)obj;
            return this.query.equals(that.query) || this.query.toString().equals(that.query.toString());
        }
        return false;
    }
}
