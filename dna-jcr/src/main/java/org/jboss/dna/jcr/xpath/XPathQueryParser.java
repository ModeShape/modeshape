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
package org.jboss.dna.jcr.xpath;

import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.parse.InvalidQueryException;
import org.jboss.dna.graph.query.parse.QueryParser;
import org.jboss.dna.jcr.xpath.XPath.Component;

/**
 * A {@link QueryParser} implementation that accepts XPath expressions and converts them to a {@link QueryCommand DNA Abstract
 * Query Model} representation.
 */
public class XPathQueryParser implements QueryParser {

    static final boolean COLLAPSE_INNER_COMPONENTS = true;
    private static final String LANGUAGE = "XPath";

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.parse.QueryParser#getLanguage()
     */
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.parse.QueryParser#parseQuery(java.lang.String, org.jboss.dna.graph.ExecutionContext)
     */
    public QueryCommand parseQuery( String query,
                                    ExecutionContext context ) throws InvalidQueryException, ParsingException {
        Component xpath = new XPathParser(context).parseXPath(query);
        System.out.println(query);
        System.out.println(" --> " + xpath);
        // Convert the result into a QueryCommand ...
        QueryCommand command = new XPathToQueryTranslator(context, query).createQuery(xpath);
        return command;
    }
}
