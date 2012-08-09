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

package org.modeshape.web.jcr.rest.handler;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.ModeShapeRestService;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestQueryResult;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Horia Chiorean
 */
public class RestQueryHandler extends QueryHandler {

    private static final String MODE_URI = "mode:uri";
    private static final String UNKNOWN_TYPE = "unknown-type";

    public RestQueryResult executeQuery( HttpServletRequest request,
                                         String rawRepositoryName,
                                         String rawWorkspaceName,
                                         String language,
                                         String statement,
                                         long offset,
                                         long limit,
                                         UriInfo uriInfo ) throws RepositoryException {
        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert language != null;
        assert statement != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Query query = createQuery(language, statement, session);
        bindExtraVariables(uriInfo, session.getValueFactory(), query);

        QueryResult result = query.execute();
        RestQueryResult restQueryResult = new RestQueryResult();

        String[] columnNames = result.getColumnNames();
        setColumns(result, restQueryResult, columnNames);

        String baseUrl = baseUrl(request);

        setRows(offset, limit, session, result, restQueryResult, columnNames, baseUrl);

        return restQueryResult;
    }

    private void setRows( long offset,
                          long limit,
                          Session session,
                          QueryResult result,
                          RestQueryResult restQueryResult,
                          String[] columnNames,
                          String baseUrl ) throws RepositoryException {
        RowIterator resultRows = result.getRows();
        if (offset > 0) {
            resultRows.skip(offset);
        }
        if (limit < 0) {
            limit = Long.MAX_VALUE;
        }

        while (resultRows.hasNext() && limit > 0) {
            limit--;
            Row resultRow = resultRows.nextRow();

            RestQueryResult.RestRow restRow = createRestRow(session, result, restQueryResult, columnNames, baseUrl, resultRow);
            createLinksFromNodePaths(result, baseUrl, resultRow, restRow);

            restQueryResult.addRow(restRow);
        }
    }

    private void createLinksFromNodePaths( QueryResult result,
                                           String baseUrl,
                                           Row resultRow,
                                           RestQueryResult.RestRow restRow ) throws RepositoryException {
        String defaultPath = resultRow.getPath();
        if (!StringUtil.isBlank(defaultPath)) {
            restRow.addValue(MODE_URI, RestHelper.urlFrom(baseUrl, ModeShapeRestService.ITEMS_METHOD_NAME,
                                                          defaultPath));
        }
        for (String selectorName : result.getSelectorNames()) {
            String selectorPath = resultRow.getPath(selectorName);
            if (!StringUtil.isBlank(defaultPath) && !selectorPath.equals(defaultPath)) {
                restRow.addValue(MODE_URI + "-" + selectorName, RestHelper.urlFrom(baseUrl,
                                                                                   ModeShapeRestService.ITEMS_METHOD_NAME,
                                                                                   selectorPath));
            }
        }
    }

    private RestQueryResult.RestRow createRestRow( Session session,
                                                   QueryResult result,
                                                   RestQueryResult restQueryResult,
                                                   String[] columnNames,
                                                   String baseUrl,
                                                   Row resultRow ) throws RepositoryException {
        RestQueryResult.RestRow restRow = restQueryResult.new RestRow();
        Map<Value, String> binaryPropertyPaths = null;

        for (String columnName : columnNames) {
            Value value = resultRow.getValue(columnName);
            if (value == null) {
                continue;
            }
            String propertyPath = null;
            //because we generate links for binary properties, we need the path of the property which has the value
            if (value.getType() == PropertyType.BINARY) {
                if (binaryPropertyPaths == null) {
                    binaryPropertyPaths = binaryPropertyPaths(resultRow, result.getSelectorNames());
                }
                propertyPath = binaryPropertyPaths.get(value);
            }

            String valueString = valueToString(propertyPath, value, baseUrl, session);
            restRow.addValue(columnName, valueString);
        }
        return restRow;
    }

    private Map<Value, String> binaryPropertyPaths( Row row,
                                                    String[] selectorNames ) throws RepositoryException {
        Map<Value, String> result = new HashMap<Value, String>();
        Node node = row.getNode();
        if (node != null) {
            result.putAll(binaryPropertyPaths(node));
        }

        for (String selectorName : selectorNames) {
            Node selectedNode = row.getNode(selectorName);
            if (selectedNode != null && selectedNode != node) {
                result.putAll(binaryPropertyPaths(selectedNode));
            }
        }
        return result;
    }

    private Map<Value, String> binaryPropertyPaths( Node node ) throws RepositoryException {
        Map<Value, String> result = new HashMap<Value, String>();
        for (PropertyIterator propertyIterator = node.getProperties(); propertyIterator.hasNext(); ) {
            Property property = propertyIterator.nextProperty();
            if (property.getType() == PropertyType.BINARY) {
                result.put(property.getValue(), property.getPath());
            }
        }
        return result;
    }

    private void setColumns( QueryResult result,
                             RestQueryResult restQueryResult,
                             String[] columnNames ) {
        if (result instanceof org.modeshape.jcr.api.query.QueryResult) {
            org.modeshape.jcr.api.query.QueryResult modeShapeQueryResult = (org.modeshape.jcr.api.query.QueryResult)result;
            String[] columnTypes = modeShapeQueryResult.getColumnTypes();
            for (int i = 0; i < columnNames.length; i++) {
                restQueryResult.addColumn(columnNames[i], columnTypes[i]);
            }
        } else {
            for (String columnName : columnNames) {
                restQueryResult.addColumn(columnName, UNKNOWN_TYPE);
            }
        }
    }
}
