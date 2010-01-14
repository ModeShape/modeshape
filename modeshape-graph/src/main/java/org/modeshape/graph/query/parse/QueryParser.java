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
package org.modeshape.graph.query.parse;

import net.jcip.annotations.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;

/**
 * The basic interface defining a component that is able to parse a string query into a {@link QueryCommand}.
 */
@Immutable
public interface QueryParser {

    /**
     * Get the name of the language that this parser is able to understand.
     * 
     * @return the language name; never null
     */
    String getLanguage();

    /**
     * Parse the supplied query from a string representation into a {@link QueryCommand}.
     * 
     * @param query the query in string form; may not be null
     * @param typeSystem the type system used by the query; may not be null
     * @return the query command
     * @throws ParsingException if there is an error parsing the supplied query
     * @throws InvalidQueryException if the supplied query can be parsed but is invalid
     */
    QueryCommand parseQuery( String query,
                             TypeSystem typeSystem ) throws InvalidQueryException;

}
