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
package org.modeshape.jdbc.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * POJO which can unmarshal the {@link org.codehaus.jettison.json.JSONObject} representation of a query response coming
 * from a ModeShape REST Service.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class QueryResult implements Iterable<QueryResult.Row>{

    private final Map<String, String> columns;
    private final List<Row> rows;

    /**
     * Creates a new query result which wraps the JSON response.
     *
     * @param object a {@link org.codehaus.jettison.json.JSONObject}, never {@code null}
     */
    @SuppressWarnings("unchecked")
    protected QueryResult(JSONObject object) {
        try {
            this.columns = new LinkedHashMap<>();
            if (object.has("columns")) {
                JSONObject columnsObject = object.getJSONObject("columns");
                Iterator<String> keysIterator = columnsObject.keys();
                while (keysIterator.hasNext()) {
                    String columnName = keysIterator.next();
                    String columnType = columnsObject.get(columnName).toString();
                    this.columns.put(columnName, columnType);
                }
            }

            this.rows = new ArrayList<>();
            if (object.has("rows")) {
                JSONArray rowsArray = object.getJSONArray("rows");
                for (int i = 0; i < rowsArray.length(); i++) {
                    this.rows.add(new Row(rowsArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<Row> iterator() {
        return rows.iterator();
    }

    /**
     * Returns the query result columns, in the [columnName, columnType] format.
     *
     * @return a {@link java.util.Map}, never {@code null}
     */
    public Map<String, String> getColumns() {
        return columns;
    }

    /**
     * Returns the result rows.
     *
     * @return a {@link java.util.List} of {@link QueryResult.Row}, never {@code null}
     */
    public List<Row> getRows() {
        return rows;
    }

    /**
     * Checks if this query result has any rows.
     *
     * @return {@code true} if there are any rows, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * A simple representation of a result row.
     */
    public final class Row {
        private final Map<String, String> values;

        @SuppressWarnings("unchecked")
        protected Row( JSONObject object ) {
            try {
                Iterator<String> keysIterator = object.keys();
                this.values = new LinkedHashMap<>();
                while (keysIterator.hasNext()) {
                    String key = keysIterator.next();
                    String value = object.get(key).toString();
                    this.values.put(key, value);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the value from the row for the given column
         *
         * @param columnName a {@link String} the name of a column; may not be {@code null}
         * @return a {@link String} representing the value for the column or {@code null} if there isn't a column with the given
         * name.
         */
        public String getValue(String columnName) {
            return values.get(columnName);
        }
    }
}
