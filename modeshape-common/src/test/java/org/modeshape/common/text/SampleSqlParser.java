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
package org.modeshape.common.text;

import java.util.LinkedList;
import java.util.List;
import org.modeshape.common.annotation.Immutable;

public class SampleSqlParser {

    public List<Statement> parse( String ddl ) {
        TokenStream tokens = new TokenStream(ddl, TokenStream.basicTokenizer(false), false);
        List<Statement> statements = new LinkedList<Statement>();
        tokens.start();

        while (tokens.hasNext()) {
            if (tokens.matches("SELECT")) {
                statements.add(parseSelect(tokens));
            } else {
                statements.add(parseDelete(tokens));
            }
        }
        return statements;
    }

    protected Select parseSelect( TokenStream tokens ) throws ParsingException {
        tokens.consume("SELECT");
        List<Column> columns = parseColumns(tokens);
        tokens.consume("FROM");
        String tableName = tokens.consume();
        return new Select(tableName, columns);
    }

    protected List<Column> parseColumns( TokenStream tokens ) throws ParsingException {
        List<Column> columns = new LinkedList<Column>();
        if (tokens.matches('*')) {
            tokens.consume(); // leave the columns empty to signal wildcard
        } else {
            // Read names until we see a ','
            do {
                String columnName = tokens.consume();
                if (tokens.canConsume("AS")) {
                    String columnAlias = tokens.consume();
                    columns.add(new Column(columnName, columnAlias));
                } else {
                    columns.add(new Column(columnName, null));
                }
            } while (tokens.canConsume(','));
        }
        return columns;
    }

    protected Delete parseDelete( TokenStream tokens ) throws ParsingException {
        tokens.consume("DELETE", "FROM");
        String tableName = tokens.consume();
        tokens.consume("WHERE");
        String lhs = tokens.consume();
        tokens.consume('=');
        String rhs = tokens.consume();
        return new Delete(tableName, new Criteria(lhs, rhs));
    }

    @Immutable
    public abstract static class Statement {
    }

    @Immutable
    public static class Select extends Statement {
        private final String from;
        private final List<Column> columns;

        public Select( String from,
                       List<Column> columns ) {
            this.from = from;
            this.columns = columns;
        }

        public String getFrom() {
            return from;
        }

        public List<Column> getColumns() {
            return columns;
        }
    }

    @Immutable
    public static class Delete extends Statement {
        private final String from;
        private final Criteria criteria;

        public Delete( String from,
                       Criteria criteria ) {
            this.from = from;
            this.criteria = criteria;
        }

        public String getFrom() {
            return from;
        }

        public Criteria getCriteria() {
            return criteria;
        }
    }

    @Immutable
    public static class Column {
        private final String name;
        private final String alias;

        public Column( String name,
                       String alias ) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }
    }

    @Immutable
    public static class Criteria {
        private final String lhs;
        private final String rhs;

        public Criteria( String lhs,
                         String rhs ) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public String getLhs() {
            return lhs;
        }

        public String getRhs() {
            return rhs;
        }
    }

    @Immutable
    public static class Query {
        private final String from;
        private final List<Column> columns;

        public Query( String from,
                      List<Column> columns ) {
            this.from = from;
            this.columns = columns;
        }

        public String getFrom() {
            return from;
        }

        public List<Column> getColumns() {
            return columns;
        }
    }

}
