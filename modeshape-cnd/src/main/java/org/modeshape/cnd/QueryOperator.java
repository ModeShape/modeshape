/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.cnd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.modeshape.common.util.CheckArg;

/**
 * Enumeration of the query operators allowed in a CND file.
 */
public enum QueryOperator {

    EQUAL("="),
    NOT_EQUAL("<>"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    GREATER_THAN("<"),
    GREATER_THAN_OR_EQUALS("<="),
    LIKE("LIKE");

    private static final Map<String, QueryOperator> ALL_OPERATORS;
    static {
        Map<String, QueryOperator> operators = new HashMap<String, QueryOperator>();
        for (QueryOperator operator : QueryOperator.values()) {
            operators.put(operator.getText(), operator);
        }
        ALL_OPERATORS = Collections.unmodifiableMap(operators);
    }

    private final String text;

    private QueryOperator( String text ) {
        this.text = text;
    }

    /**
     * @return text
     */
    public String getText() {
        return text;
    }

    public static QueryOperator forText( String text ) {
        CheckArg.isNotNull(text, "text");
        return ALL_OPERATORS.get(text.trim().toUpperCase());
    }

    /**
     * Return an iterator over all the operator enumeration literals.
     * 
     * @return an immutable iterator
     */
    public static Iterator<QueryOperator> iterator() {
        return ALL_OPERATORS.values().iterator();
    }
}
