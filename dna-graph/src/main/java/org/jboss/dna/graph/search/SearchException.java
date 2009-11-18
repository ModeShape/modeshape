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
package org.jboss.dna.graph.search;

/**
 * An exception signalling an error during a search.
 */
public class SearchException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final String expression;

    /**
     * Create an exception with the search expression.
     * 
     * @param expression the search expression
     */
    public SearchException( String expression ) {
        super();
        this.expression = expression;
    }

    /**
     * Create an exception with the search expression and a message.
     * 
     * @param expression the search expression
     * @param message the exception message
     */
    public SearchException( String expression,
                            String message ) {
        super(message);
        assert expression != null;
        this.expression = expression;
    }

    /**
     * Construct a system failure exception with another exception that is the cause of the failure.
     * 
     * @param expression the search expression
     * @param cause the original cause of the failure
     */
    public SearchException( String expression,
                            Throwable cause ) {
        super(cause);
        assert expression != null;
        this.expression = expression;
    }

    /**
     * Construct a system failure exception with a single message and another exception that is the cause of the failure.
     * 
     * @param expression the search expression
     * @param message the message describing the failure
     * @param cause the original cause of the failure
     */
    public SearchException( String expression,
                            String message,
                            Throwable cause ) {
        super(message, cause);
        assert expression != null;
        this.expression = expression;
    }

    /**
     * Get the search expression.
     * 
     * @return the search expression; never null
     */
    public String getSearchExpression() {
        return expression;
    }

}
