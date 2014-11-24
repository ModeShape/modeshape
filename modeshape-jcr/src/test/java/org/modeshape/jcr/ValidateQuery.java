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

package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.common.util.StringUtil;
import org.modeshape.common.util.StringUtil.Justify;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class ValidateQuery {

    public static ValidationBuilder validateQuery() {
        return new Builder();
    }

    public static interface ValidationBuilder {
        ValidationBuilder noWarnings();

        ValidationBuilder warnings( int expectedCount );

        ValidationBuilder onlyQueryPlan();

        ValidationBuilder rowCount( int expectedRowCount );

        ValidationBuilder rowCount( long expectedRowCount );

        ValidationBuilder useIndex( String indexName );

        ValidationBuilder useNoIndexes();

        ValidationBuilder considerIndex( String indexNames );

        ValidationBuilder considerIndexes( String... indexNames );

        ValidationBuilder hasColumns( String... columnNames );

        RowBuilder withRows();

        ValidationBuilder hasNodesAtPaths( String... paths );

        ValidationBuilder printDetail();

        ValidationBuilder printDetail( boolean print );

        ValidationBuilder onEachRow( Predicate predicate );

        void validate( Query query,
                       QueryResult result ) throws RepositoryException;
    }

    public static interface RowBuilder {
        RowBuilder withRow( String... paths );

        RowBuilder withRow( Node... nodes );

        ValidationBuilder endRows();
    }

    public static interface Predicate {
        void validate( int rowNumber,
                       Row row ) throws RepositoryException;
    }

    protected static class Builder implements ValidationBuilder {
        private int warningCount = -1;
        private boolean print = false;
        private boolean checkForQueryPlan = false;
        private String[] columnNames;
        private long numRows = -1L;
        private String nameOfIndexToUse;
        private String[] nameOfIndexesToConsider;
        private Validator validator;

        @Override
        public ValidationBuilder noWarnings() {
            warningCount = 0;
            return this;
        }

        @Override
        public ValidationBuilder warnings( int expectedCount ) {
            warningCount = expectedCount;
            return this;
        }

        @Override
        public ValidationBuilder onlyQueryPlan() {
            rowCount(0);
            checkForQueryPlan = true;
            return this;
        }

        @Override
        public ValidationBuilder hasColumns( String... columnNames ) {
            this.columnNames = columnNames;
            return this;
        }

        @Override
        public ValidationBuilder printDetail() {
            print = true;
            return this;
        }

        @Override
        public ValidationBuilder printDetail( boolean print ) {
            this.print = print;
            return this;
        }

        @Override
        public ValidationBuilder useIndex( String indexName ) {
            this.nameOfIndexToUse = indexName;
            checkForQueryPlan = true;
            return this;
        }

        @Override
        public ValidationBuilder considerIndexes( String... indexNames ) {
            this.nameOfIndexesToConsider = indexNames;
            checkForQueryPlan = true;
            return this;
        }

        @Override
        public ValidationBuilder considerIndex( String indexName ) {
            assert indexName != null;
            this.nameOfIndexesToConsider = new String[] {indexName};
            checkForQueryPlan = true;
            return this;
        }

        @Override
        public ValidationBuilder useNoIndexes() {
            this.nameOfIndexesToConsider = new String[0];
            this.nameOfIndexToUse = null;
            checkForQueryPlan = true;
            return this;
        }

        @Override
        public ValidationBuilder rowCount( int expectedRowCount ) {
            numRows = expectedRowCount;
            return this;
        }

        @Override
        public ValidationBuilder rowCount( long expectedRowCount ) {
            numRows = expectedRowCount;
            return this;
        }

        @Override
        public ValidationBuilder hasNodesAtPaths( String... paths ) {
            List<String> rowPaths = Arrays.asList(paths);
            numRows = rowPaths.size();
            return setValidator(new SingleSelectorRowValidator(rowPaths.iterator()));
        }

        protected ValidationBuilder setValidator( Validator validator ) {
            assert this.validator == null;
            this.validator = validator;
            return this;
        }

        @Override
        public ValidationBuilder onEachRow( final Predicate predicate ) {
            return setValidator(new Validator() {
                private int rowNumber = 0;

                @Override
                public void checkRow( Row row,
                                      String[] selectorNames ) throws RepositoryException {
                    predicate.validate(++rowNumber, row);
                }
            });
        }

        @Override
        public RowBuilder withRows() {
            final List<Object[]> rows = new ArrayList<Object[]>();
            return new RowBuilder() {
                @Override
                public RowBuilder withRow( String... paths ) {
                    rows.add(paths);
                    return this;
                }

                @Override
                public RowBuilder withRow( Node... nodes ) {
                    rows.add(nodes);
                    return this;
                }

                @Override
                public ValidationBuilder endRows() {
                    return setValidator(new MultiSelectorRowValidator(rows.iterator()));
                }
            };
        }

        @Override
        public void validate( Query query,
                              QueryResult result ) throws RepositoryException {
            assertThat(query, is(notNullValue()));
            assertThat(result, is(notNullValue()));
            if (print) print(query, result);
            try {
                validateWarnings(result);
                validateColumnNames(result);
                validateQueryPlan(result);
            } catch (AssertionError e) {
                if (!print) {
                    // print anyway since this is an error
                    print(query, result);
                }
                throw e;
            }
            NodeIterator nodes = null;
            if (result.getSelectorNames().length != 1) {
                // Make sure that we cannot get the multi-selector results from this single-selector results ...
                try {
                    nodes = result.getNodes();
                    if (!print) {
                        // print anyway since this is an error
                        print(query, result);
                    }
                    fail("should not be able to call this method when the query has multiple selectors");
                } catch (RepositoryException e) {
                    // expected; can't call this when the query uses multiple selectors ...
                }
            } else {
                nodes = result.getNodes();
            }

            // Check the row count ...
            RowIterator iter = result.getRows();
            if (!validateRowCount(iter.getSize()) && !print) {
                // we're not printing this, but print anyway since this is an error
                print(query, result);
                print = true;
            }

            // Now validate the query results ...
            Printer printer = new Printer(result.getColumnNames());
            if (print) printer.printHeader();
            while (iter.hasNext()) {
                Row row = iter.nextRow();
                assertThat(row, is(notNullValue()));
                if (print) printer.printRow(row);
                if (validator != null) validator.checkRow(row, result.getSelectorNames());
            }
            if (print) printer.printFooter();
            assertRowCount(iter.getSize());

            if (nodes != null) {
                // Check the single-selector results via node iterator ...
                assertTrue(result.getSelectorNames().length == 1);
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    assert node != null || node == null; // duh!
                    // if (print) printer.printNode(node);
                }
            } else {
                assertTrue(result.getSelectorNames().length != 1);
            }
        }

        protected boolean validateRowCount( long actual ) {
            if (actual < 0L || numRows < 0L) return true;
            return actual == numRows;
        }

        protected void assertRowCount( long actual ) {
            if (actual >= 0L && numRows >= 0L) assertThat(actual, is(numRows));
        }

        protected void validateWarnings( QueryResult result ) {
            if (warningCount >= 0) {
                Collection<String> warnings = ((org.modeshape.jcr.api.query.QueryResult)result).getWarnings();
                if (print) {
                    System.out.println("Warnings on query");
                    for (String warning : warnings) {
                        System.out.println("   " + warning);
                    }
                    System.out.println();
                }
                assertThat(warnings.size(), is(warningCount));
            }
        }

        protected void validateColumnNames( QueryResult result ) throws RepositoryException {
            if (columnNames != null) {
                List<String> expectedNames = new ArrayList<String>();
                for (String name : columnNames) {
                    expectedNames.add(name);
                }
                List<String> actualNames = new ArrayList<String>();
                for (String name : result.getColumnNames()) {
                    actualNames.add(name);
                }
                Collections.sort(expectedNames);
                Collections.sort(actualNames);
                assertThat(actualNames, is(expectedNames));
            }
        }

        protected void validateQueryPlan( QueryResult result ) {
            if (checkForQueryPlan) {
                String plan = ((org.modeshape.jcr.api.query.QueryResult)result).getPlan();
                assertNotNull(plan);
                assertTrue(plan.trim().length() > 0);

                // Figure out which indexes are expected ...
                Set<String> allIndexNames = new HashSet<>();
                if (nameOfIndexesToConsider != null) {
                    for (String indexName : nameOfIndexesToConsider) {
                        allIndexNames.add(indexName);
                    }
                }
                if (nameOfIndexToUse != null) {
                    allIndexNames.add(nameOfIndexToUse);
                }
                // Look for the indexes ...
                Set<String> allIndexNamesCopy = new HashSet<>(allIndexNames);
                boolean foundUsed = false;
                if (!allIndexNames.isEmpty()) {
                    for (String line : StringUtil.splitLines(plan)) {
                        Matcher matcher = INDEX_NAME_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String name = matcher.group(1);
                            if (allIndexNames.contains(name)) {
                                allIndexNamesCopy.remove(name);
                            } else {
                                fail("Index '" + name + "' was included in plan but not expected");
                            }
                            boolean isUsed = INDEX_USED_PATTERN.matcher(line).find();
                            if (isUsed && nameOfIndexToUse != null) {
                                assertEquals("Index '" + name + "' was used, but '" + nameOfIndexToUse
                                             + "' was expected to be used", nameOfIndexToUse, name);
                                foundUsed = true;
                            }
                        }
                    }
                }
                if (!foundUsed && nameOfIndexToUse != null) {
                    fail("Index '" + nameOfIndexToUse + "' was not used in query as expected");
                }
                if (!allIndexNamesCopy.isEmpty()) {
                    fail("Indexes " + allIndexNames + " were not found in query plan but expected");
                }
            }
        }

        protected void print( Query query,
                              QueryResult result ) {
            System.out.println();
            System.out.println(query);
            System.out.println(" plan -> " + ((org.modeshape.jcr.api.query.QueryResult)result).getPlan());
            System.out.println(result);
        }

    }

    protected static class Printer {
        protected static final int MAXIMUM_PATH_DISPLAY_LENGTH = 64;
        protected static final int MAXIMUM_NAME_DISPLAY_LENGTH = 32;
        protected static final int MAXIMUM_REFERENCE_DISPLAY_LENGTH = UUID.randomUUID().toString().length() + 2;
        protected static final int MAXIMUM_KNOWN_STRING_DISPLAY_LENGTH = 16;
        private int widthOfRowNumber = 4;
        private int rowNumber = 0;
        private final String[] columnNames;
        private final int[] columnWidths;

        protected Printer( String[] columnNames ) {
            this(columnNames, MAXIMUM_PATH_DISPLAY_LENGTH, MAXIMUM_NAME_DISPLAY_LENGTH, MAXIMUM_REFERENCE_DISPLAY_LENGTH,
                 MAXIMUM_KNOWN_STRING_DISPLAY_LENGTH);
        }

        protected Printer( String[] columnNames,
                           int maxPathLength,
                           int maxNameLength,
                           int maxRefLength,
                           int maxStringLength ) {
            this.columnNames = columnNames;
            columnWidths = new int[columnNames.length];
            for (int i = 0; i != columnNames.length; ++i) {
                String columnName = columnNames[i].toLowerCase();
                int columnWidth = columnName.length();
                if (columnName.endsWith("jcr:path")) {
                    columnWidths[i] = Math.max(columnWidth, maxPathLength);
                } else if (columnName.endsWith("jcr:name") || columnName.endsWith("mode:localName")) {
                    columnWidths[i] = Math.max(columnWidth, maxNameLength);
                } else if (columnName.endsWith("jcr:primaryType") || columnName.endsWith("jcr:mixinTypes")
                           || columnName.endsWith("jcr:createdBy")) {
                    columnWidths[i] = Math.max(columnWidth, maxStringLength);
                } else if (columnName.endsWith("reference") || columnName.endsWith("jcr:uuid") || columnName.endsWith("jcr_uuid")) {
                    columnWidths[i] = Math.max(columnWidth, maxRefLength);
                } else {
                    columnWidths[i] = Math.max(columnWidth, columnWidth);
                }
            }
        }

        protected void printHeader() {
            printDelimiter();
            System.out.print("| " + StringUtil.createString(' ', widthOfRowNumber));
            int columnIndex = 0;
            for (String columnName : columnNames) {
                System.out.print(" | ");
                System.out.print(formatForColumn(columnName, columnIndex++, Justify.CENTER));
            }
            System.out.println(" |");
            printDelimiter();
        }

        protected void printFooter() {
            printDelimiter();
        }

        private void printDelimiter() {
            System.out.print("+-" + StringUtil.createString('-', widthOfRowNumber));
            for (int i = 0; i != columnNames.length; ++i) {
                System.out.print("-+-");
                System.out.print(StringUtil.createString('-', columnWidths[i]));
            }
            System.out.println("-+");
        }

        private String formatForColumn( String value,
                                        int columnIndex,
                                        Justify justify ) {
            return StringUtil.justify(justify, value, columnWidths[columnIndex], ' ');
        }

        protected String rowNumberStr() {
            return StringUtil.justifyRight("" + (++rowNumber), widthOfRowNumber, ' ');
        }

        protected String valueAsString( Value value,
                                        int columnIndex ) throws RepositoryException {
            String str = value != null ? value.getString() : "";
            Justify justify = justify(value);
            return formatForColumn(str, columnIndex, justify);
        }

        protected Justify justify( Value value ) {
            return Justify.LEFT;
            // if (value == null) {
            // return Justify.RIGHT;
            // }
            // switch (value.getType()) {
            // case PropertyType.BOOLEAN:
            // case PropertyType.DECIMAL:
            // case PropertyType.DOUBLE:
            // case PropertyType.LONG:
            // return Justify.RIGHT;
            // default:
            // return Justify.LEFT;
            // }
        }

        protected void printRow( Row row ) throws RepositoryException {
            System.out.print("| ");
            System.out.print(rowNumberStr());
            int columnIndex = 0;
            for (String columnName : columnNames) {
                System.out.print(" | ");
                System.out.print(valueAsString(row.getValue(columnName), columnIndex++));
            }
            System.out.println(" |");
        }

        protected void printNode( Node node ) throws RepositoryException {
            System.out.print("| ");
            System.out.print(rowNumberStr());
            System.out.print(" | ");
            System.out.print(node.getPath());
            System.out.println(" |");
        }
    }

    protected static interface Validator {
        void checkRow( Row row,
                       String[] selectorNames ) throws RepositoryException;
    }

    protected static class SingleSelectorRowValidator implements Validator {
        private final Iterator<? extends Object> iterator;

        protected SingleSelectorRowValidator( Iterator<? extends Object> iter ) {
            this.iterator = iter;
        }

        @Override
        public void checkRow( Row row,
                              String[] selectorNames ) throws RepositoryException {
            Object expected = iterator.next();
            if (expected instanceof String) {
                assertThat(row.getPath(), is((String)expected));
            } else if (expected instanceof Node) {
                assertThat(row.getNode(), is(expected));
            }
        }
    }

    protected static class MultiSelectorRowValidator implements Validator {
        private final Iterator<Object[]> iterator;

        protected MultiSelectorRowValidator( Iterator<Object[]> iter ) {
            this.iterator = iter;
        }

        @Override
        public void checkRow( Row row,
                              String[] selectorNames ) throws RepositoryException {
            Object[] expected = iterator.next();
            int i = 0;
            if (expected[0] instanceof String) {
                for (String selector : selectorNames) {
                    assertThat(row.getPath(selector), is(expected[i++]));
                }
            } else if (expected[0] instanceof Node) {
                for (String selector : selectorNames) {
                    assertThat(row.getNode(selector), is(expected[i++]));
                }
            }
        }
    }

    protected static final Pattern INDEX_NAME_PATTERN = Pattern.compile("INDEX_SPECIFICATION=([^,]*)");
    protected static final Pattern INDEX_USED_PATTERN = Pattern.compile("INDEX_USED=true");

    private ValidateQuery() {
    }
}
