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
package org.jboss.dna.graph.query.parse;

import org.jboss.dna.common.util.CheckArg;

/**
 * An exception signalling that an XPath query is invalid (but typically well-formed)
 */
public class InvalidQueryException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final String xpath;

    /**
     * Create an exception with the invalid query.
     * 
     * @param xpath the XPath query that is invalid
     */
    public InvalidQueryException( String xpath ) {
        super();
        this.xpath = xpath;
    }

    /**
     * Create an exception with the invalid query and a message.
     * 
     * @param xpath the XPath query that is invalid
     * @param message
     */
    public InvalidQueryException( String xpath,
                                  String message ) {
        super(message);
        CheckArg.isNotNull(xpath, "xpath");
        this.xpath = xpath;
    }

    /**
     * Get the XPath query that is invalid.
     * 
     * @return the query
     */
    public String getXPath() {
        return xpath;
    }

}
