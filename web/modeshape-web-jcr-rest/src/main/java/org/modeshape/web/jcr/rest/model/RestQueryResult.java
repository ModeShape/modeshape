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

package org.modeshape.web.jcr.rest.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.StringUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A REST representation of a {@link javax.jcr.query.QueryResult}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestQueryResult implements JSONAble {
    private final Map<String, String> columns;
    private final List<RestRow> rows;

    /**
     * Creates an empty instance
     */
    public RestQueryResult() {
        columns = new LinkedHashMap<String, String>();
        rows = new ArrayList<RestRow>();
    }

    /**
     * Adds a new column to this result.
     *
     * @param name a {@code non-null} string, the name of the column
     * @param type a {@code non-null} string, the type of the column
     * @return this instance
     */
    public RestQueryResult addColumn( String name,
                                      String type ) {
        if (!StringUtil.isBlank(name)) {
            columns.put(name, type);
        }
        return this;
    }

    /**
     * Adds a new row to this result
     *
     * @param row a {@code non-null} {@link RestRow}
     * @return this instance
     */
    public RestQueryResult addRow( RestRow row ) {
        rows.add(row);
        return this;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        if (!columns.isEmpty()) {
            result.put("columns", columns);
        }
        if (!rows.isEmpty()) {
            JSONArray rows = new JSONArray();
            for (RestRow row : this.rows) {
                rows.put(row.toJSON());
            }
            result.put("rows", rows);
        }
        return result;
    }

    public class RestRow implements JSONAble {
        private final Map<String, String> values;

        public RestRow() {
            this.values = new LinkedHashMap<String, String>();
        }

        public void addValue( String name,
                              String value ) {
            if (!StringUtil.isBlank(name) && !StringUtil.isBlank(value)) {
                values.put(name, value);
            }
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            return new JSONObject(values);
        }
    }
}
