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

import java.util.HashMap;
import java.util.Map;
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
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestQueryResult;

/**
 * An extension of the {@link QueryHandler} which allows executing queries against a repository and returns rest representations
 * of the query results.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "deprecation" )
public final class RestQueryHandler extends QueryHandler {

    private static final String MODE_URI = "mode:uri";
    private static final String UNKNOWN_TYPE = "unknown-type";

    /**
     * Executes a the given query string (based on the language information) against a JCR repository, returning a rest model
     * based result.
     * 
     * @param request a non-null {@link HttpServletRequest}
     * @param repositoryName a non-null, URL encoded {@link String} representing the name of a repository
     * @param workspaceName a non-null, URL encoded {@link String} representing the name of a workspace
     * @param language a non-null String which should be a valid query language, as recognized by the
     *        {@link javax.jcr.query.QueryManager}
     * @param statement a non-null String which should be a valid query string in the above language.
     * @param offset a numeric value which indicates the index in the result set from where results should be returned.
     * @param limit a numeric value indicating the maximum number of rows to return.
     * @param uriInfo a non-null {@link UriInfo} object which is provided by RestEASY, allowing extra request parameters to be
     *        retrieved.
     * @return a {@link RestQueryHandler} instance
     * @throws RepositoryException if any operation fails at the JCR level
     */
    public RestQueryResult executeQuery( HttpServletRequest request,
                                         String repositoryName,
                                         String workspaceName,
                                         String language,
                                         String statement,
                                         long offset,
                                         long limit,
                                         UriInfo uriInfo ) throws RepositoryException {
        assert repositoryName != null;
        assert workspaceName != null;
        assert language != null;
        assert statement != null;

        Session session = getSession(request, repositoryName, workspaceName);
        Query query = createQuery(language, statement, session);
        bindExtraVariables(uriInfo, session.getValueFactory(), query);

        QueryResult result = query.execute();
        RestQueryResult restQueryResult = new RestQueryResult();

        String[] columnNames = result.getColumnNames();
        setColumns(result, restQueryResult, columnNames);

        String baseUrl = RestHelper.repositoryUrl(request);

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
            restRow.addValue(MODE_URI, RestHelper.urlFrom(baseUrl, RestHelper.ITEMS_METHOD_NAME, defaultPath));
        }
        for (String selectorName : result.getSelectorNames()) {
            try {
                String selectorPath = resultRow.getPath(selectorName);
                if (!StringUtil.isBlank(defaultPath) && !selectorPath.equals(defaultPath)) {
                    restRow.addValue(MODE_URI + "-" + selectorName,
                                     RestHelper.urlFrom(baseUrl, RestHelper.ITEMS_METHOD_NAME, selectorPath));
                }
            } catch (RepositoryException e) {
                logger.debug(e, e.getMessage());
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
            // because we generate links for binary properties, we need the path of the property which has the value
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
        for (PropertyIterator propertyIterator = node.getProperties(); propertyIterator.hasNext();) {
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
