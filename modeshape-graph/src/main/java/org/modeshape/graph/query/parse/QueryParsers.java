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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;

/**
 * A thread-safe collection of {@link QueryParser} implementations that can be used to parse queries by language.
 */
@ThreadSafe
public class QueryParsers {

    private final ConcurrentMap<String, QueryParser> parsers = new ConcurrentHashMap<String, QueryParser>();

    /**
     * Create a new collection of the supplied {@link QueryParser} objects.
     * 
     * @param parsers the parsers
     */
    public QueryParsers( QueryParser... parsers ) {
        if (parsers != null) {
            for (QueryParser parser : parsers) {
                if (parser != null) addLanguage(parser);
            }
        }
    }

    /**
     * Create a new collection of the supplied {@link QueryParser} objects.
     * 
     * @param parsers the parsers
     */
    public QueryParsers( Iterable<QueryParser> parsers ) {
        if (parsers != null) addLanguages(parsers);
    }

    /**
     * Add a language to this engine by supplying its parser.
     * 
     * @param languageParser the query parser for the language
     * @throws IllegalArgumentException if the language parser is null
     */
    public void addLanguage( QueryParser languageParser ) {
        CheckArg.isNotNull(languageParser, "languageParser");
        this.parsers.put(languageParser.getLanguage().trim().toLowerCase(), languageParser);
    }

    /**
     * Add one or more languages to this engine by supplying the corresponding parsers.
     * 
     * @param firstLanguage the query parser for the first language
     * @param additionalLanguages the query parsers for the additional languages
     * @throws IllegalArgumentException if the language parser is null
     */
    public void addLanguages( QueryParser firstLanguage,
                              QueryParser... additionalLanguages ) {
        addLanguage(firstLanguage);
        for (QueryParser language : additionalLanguages) {
            addLanguage(language);
        }
    }

    /**
     * Add one or more languages to this engine by supplying the corresponding parsers.
     * 
     * @param languages the query parsers for the languages that are to be added
     * @throws IllegalArgumentException if the iterable reference is null
     */
    public void addLanguages( Iterable<QueryParser> languages ) {
        CheckArg.isNotNull(languages, "languages");
        for (QueryParser language : languages) {
            addLanguage(language);
        }
    }

    /**
     * Remove from this engine the language with the given name.
     * 
     * @param language the name of the language, which is to match the {@link QueryParser#getLanguage() language} of the parser
     * @return the parser for the language, or null if the engine had no support for the named language
     * @throws IllegalArgumentException if the language is null
     */
    public QueryParser removeLanguage( String language ) {
        CheckArg.isNotNull(language, "language");
        return this.parsers.remove(language.toLowerCase());
    }

    /**
     * Remove from this engine the language with the given name.
     * 
     * @param firstLanguage the name of the first language to remove, which must match the {@link QueryParser#getLanguage()
     *        language} of the parser
     * @param additionalLanguages the names of the additional languages to remove, which must match the
     *        {@link QueryParser#getLanguage() language} of the parser
     * @return the parser for the language, or null if the engine had no support for the named language
     * @throws IllegalArgumentException if the language is null
     */
    public Collection<QueryParser> removeLanguages( String firstLanguage,
                                                    String... additionalLanguages ) {
        Collection<QueryParser> removed = new HashSet<QueryParser>();
        QueryParser parser = removeLanguage(firstLanguage);
        if (parser != null) removed.add(parser);
        for (String language : additionalLanguages) {
            parser = removeLanguage(language);
            if (parser != null) removed.add(parser);
        }
        return removed;
    }

    /**
     * Get the set of languages that this engine is capable of parsing.
     * 
     * @return the unmodifiable copy of the set of languages; never null but possibly empty
     */
    public Set<String> getLanguages() {
        Set<String> result = new HashSet<String>();
        for (QueryParser parser : parsers.values()) {
            result.add(parser.getLanguage());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Get the parser for the supplied language.
     * 
     * @param language the language in which the query is expressed; must case-insensitively match one of the supported
     *        {@link #getLanguages() languages}
     * @return the query parser, or null if the supplied language is not supported
     * @throws IllegalArgumentException if the language is null
     */
    public QueryParser getParserFor( String language ) {
        CheckArg.isNotNull(language, "language");
        return parsers.get(language.trim().toLowerCase());
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param typeSystem the type system that should be used
     * @param language the language in which the query is expressed; must case-insensitively match one of the supported
     *        {@link #getLanguages() languages}
     * @param query the query that is to be executed
     * @return the parsed query command; never null
     * @throws IllegalArgumentException if the language, context or query references are null, or if the language is not known
     * @throws ParsingException if there is an error parsing the supplied query
     * @throws InvalidQueryException if the supplied query can be parsed but is invalid
     */
    public QueryCommand parse( TypeSystem typeSystem,
                               String language,
                               String query ) {
        CheckArg.isNotNull(language, "language");
        CheckArg.isNotNull(typeSystem, "typeSystem");
        CheckArg.isNotNull(query, "query");
        QueryParser parser = parsers.get(language.trim().toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException(GraphI18n.unknownQueryLanguage.text(language));
        }
        return parser.parseQuery(query, typeSystem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // There won't be too many instances of QueryParsers, so have them all return the same hash code
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryParsers) {
            QueryParsers that = (QueryParsers)obj;
            return this.getLanguages().equals(that.getLanguages());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Query parsers: " + parsers.keySet();
    }
}
