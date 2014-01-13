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
import org.modeshape.web.jcr.rest.model.RestQueryPlanResult;
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
     * @return a response containing the string representation of the query plan
     * @throws RepositoryException if any operation fails at the JCR level
     */
    public RestQueryPlanResult planQuery( HttpServletRequest request,
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
        org.modeshape.jcr.api.query.Query query = createQuery(language, statement, session);
        bindExtraVariables(uriInfo, session.getValueFactory(), query);

        org.modeshape.jcr.api.query.QueryResult result = query.explain();
        String plan = result.getPlan();
        return new RestQueryPlanResult(plan, statement, language, query.getAbstractQueryModelRepresentation());
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
        String defaultPath = encodedPath(resultRow.getPath());
        if (!StringUtil.isBlank(defaultPath)) {
            restRow.addValue(MODE_URI, RestHelper.urlFrom(baseUrl, RestHelper.ITEMS_METHOD_NAME, defaultPath));
        }
        for (String selectorName : result.getSelectorNames()) {
            try {
                String selectorPath = encodedPath(resultRow.getPath(selectorName));
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
