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
package org.modeshape.jcr.query.parse;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.TypeSystem;

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
