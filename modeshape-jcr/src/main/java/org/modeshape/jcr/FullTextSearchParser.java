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
package org.modeshape.jcr;

import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.InvalidQueryException;
import org.modeshape.graph.query.parse.QueryParser;

/**
 * A {@link QueryParser} implementation that is stored in the {@link JcrRepository}'s list of {@link JcrRepository#queryParsers()
 * query parsers} so that the name is there, but it should never be used.
 * 
 * @see JcrRepository#queryParsers()
 * @see JcrQueryManager#createQuery(String, String)
 */
class FullTextSearchParser implements QueryParser {

    public static final String LANGUAGE = "Search";

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
     * @see org.modeshape.graph.query.parse.QueryParser#parseQuery(java.lang.String, org.modeshape.graph.query.model.TypeSystem)
     */
    public QueryCommand parseQuery( String query,
                                    TypeSystem typeSystem ) throws InvalidQueryException {
        assert false; // This method should never be called;
        throw new UnsupportedOperationException();
    }
}
