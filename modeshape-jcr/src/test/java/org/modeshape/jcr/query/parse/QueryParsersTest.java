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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.TypeSystem;

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

        @Override
        public String getLanguage() {
            return language;
        }

        @Override
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
