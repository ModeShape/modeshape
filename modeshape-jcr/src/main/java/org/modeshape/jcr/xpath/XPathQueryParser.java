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
package org.modeshape.jcr.xpath;

import javax.jcr.query.Query;
import org.modeshape.common.text.ParsingException;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.InvalidQueryException;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.jcr.xpath.XPath.Component;

/**
 * A {@link QueryParser} implementation that accepts XPath expressions and converts them to a {@link QueryCommand ModeShape
 * Abstract Query Model} representation.
 */
public class XPathQueryParser implements QueryParser {

    static final boolean COLLAPSE_INNER_COMPONENTS = true;
    @SuppressWarnings( "deprecation" )
    private static final String LANGUAGE = Query.XPATH;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#getLanguage()
     */
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryParser) {
            QueryParser that = (QueryParser)obj;
            return this.getLanguage().equals(that.getLanguage());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#parseQuery(java.lang.String, org.modeshape.graph.query.model.TypeSystem)
     */
    public QueryCommand parseQuery( String query,
                                    TypeSystem typeSystem ) throws InvalidQueryException, ParsingException {
        Component xpath = new XPathParser(typeSystem).parseXPath(query);
        // Convert the result into a QueryCommand ...
        QueryCommand command = new XPathToQueryTranslator(typeSystem, query).createQuery(xpath);
        return command;
    }
}
