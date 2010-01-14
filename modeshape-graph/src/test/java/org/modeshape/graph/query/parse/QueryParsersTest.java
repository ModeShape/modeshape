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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class QueryParsersTest {

    private QueryParsers parsers;
    private QueryParser parser1;
    private QueryParser parser2;
    private QueryParser parser3;

    @Before
    public void beforeEach() {
        parsers = new QueryParsers();
        parser1 = new MockParser("language 1");
        parser2 = new MockParser("language 2");
        parser3 = new MockParser("language 3");
    }

    @Test
    public void shouldAllowNullArrayInConstructor() {
        parsers = new QueryParsers((QueryParser[])null);
        assertThat(parsers.getLanguages().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowNullIterableInConstructor() {
        parsers = new QueryParsers((Iterable<QueryParser>)null);
        assertThat(parsers.getLanguages().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowOneQueryParserReferenceInConstructor() {
        parsers = new QueryParsers(parser1);
        assertThat(parsers.getLanguages().size(), is(1));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
    }

    @Test
    public void shouldAllowTwoQueryParserReferencesInConstructor() {
        parsers = new QueryParsers(parser1, parser2);
        assertThat(parsers.getLanguages().size(), is(2));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
    }

    @Test
    public void shouldAllowThreeQueryParserReferencesInConstructor() {
        parsers = new QueryParsers(parser1, parser2, parser3);
        assertThat(parsers.getLanguages().size(), is(3));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser3.getLanguage()), is(true));
    }

    @Test
    public void shouldOverwriteQueryParsersWithSameLanguageInConstructor() {
        parser2 = new MockParser(parser1.getLanguage());
        parsers = new QueryParsers(parser1, parser2, parser3);
        assertThat(parsers.getLanguages().size(), is(2));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser3.getLanguage()), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullQueryParserReferenceInAddLanguagesMethod() {
        parsers.addLanguages((QueryParser)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldAllowNullIterableInAddLanguagesMethod() {
        parsers.addLanguages((Iterable<QueryParser>)null);
    }

    @Test
    public void shouldAddOneQueryParserReference() {
        parsers.addLanguage(parser1);
        assertThat(parsers.getLanguages().size(), is(1));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
    }

    @Test
    public void shouldAddTwoQueryParserReferences() {
        parsers.addLanguages(parser1, parser2);
        assertThat(parsers.getLanguages().size(), is(2));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
    }

    @Test
    public void shouldAddThreeQueryParserReferences() {
        parsers.addLanguages(parser1, parser2, parser3);
        assertThat(parsers.getLanguages().size(), is(3));
        assertThat(parsers.getLanguages().contains(parser1.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser3.getLanguage()), is(true));
    }

    @Test
    public void shouldOverwriteQueryParsersWithSameLanguageWhenAddingQueryParser() {
        parser2 = new MockParser(parser1.getLanguage());
        parsers.addLanguages(parser1, parser2, parser3);
        assertThat(parsers.getLanguages().size(), is(2));
        assertThat(parsers.getLanguages().contains(parser2.getLanguage()), is(true));
        assertThat(parsers.getLanguages().contains(parser3.getLanguage()), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToParseUnknownLanguage() {
        TypeSystem typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        parsers.parse(typeSystem, "unknown language", "This is a bogus query");
    }

    protected static class MockParser implements QueryParser {
        private final String language;

        protected MockParser( String language ) {
            this.language = language;
            assert this.language != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.parse.QueryParser#getLanguage()
         */
        public String getLanguage() {
            return language;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.parse.QueryParser#parseQuery(java.lang.String,
         *      org.modeshape.graph.query.model.TypeSystem)
         */
        public QueryCommand parseQuery( String query,
                                        TypeSystem typeSystem ) throws InvalidQueryException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Mock parser for " + language;
        }
    }
}
