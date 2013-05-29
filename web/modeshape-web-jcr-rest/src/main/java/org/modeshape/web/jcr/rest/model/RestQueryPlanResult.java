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

/**
 * A REST representation of a query plan.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestQueryPlanResult implements JSONAble, Stringable {
    private final String plan;
    private final String language;
    private final String statement;
    private final String aqm;

    /**
     * Creates a result with the specified plan
     * 
     * @param plan the plan
     * @param statement the original query statement
     * @param language the query language
     * @param aqm the abstract query model for the query
     */
    public RestQueryPlanResult( String plan,
                                String statement,
                                String language,
                                String aqm ) {
        this.plan = plan;
        this.statement = statement;
        this.language = language;
        this.aqm = aqm;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("statement", statement);
        result.put("language", language);
        result.put("abstractQueryModel", aqm);
        String[] planParts = plan.split("\\n");
        JSONArray planArray = new JSONArray();
        for (String part : planParts) {
            planArray.put(part);
        }
        result.put("queryPlan", planArray);
        // result.put("queryPlan", plan);
        return result;
    }

    @Override
    public String asString() {
        return plan;
    }
}
