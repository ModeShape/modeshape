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
package org.modeshape.common.text;

import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.Immutable;

/**
 * 
 */
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
    public static abstract class Statement {
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
