package org.modeshape.web.jcr.rest.handler;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.web.jcr.rest.RestHelper;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Resource handler that implements REST methods for items.
 *
 * @deprecated since 3.0, use {@link RestQueryHandler}
 */
@Immutable
public class QueryHandler extends AbstractHandler {

    protected static final List<String> SKIP_QUERY_PARAMETERS = Arrays.asList("offset", "limit");

    /**
     * @deprecated since 3.0
     */
    public String postItem( HttpServletRequest request,
                            String rawRepositoryName,
                            String rawWorkspaceName,
                            String language,
                            String statement,
                            long offset,
                            long limit,
                            UriInfo uriInfo ) throws RepositoryException, JSONException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert language != null;
        assert statement != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);

        Query query = createQuery(language, statement, session);
        bindExtraVariables(uriInfo, session.getValueFactory(), query);

        QueryResult result = query.execute();

        String[] columnNames = result.getColumnNames();

        List<JSONObject> jsonRows = new LinkedList<JSONObject>();
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
            JSONObject jsonRow = new JSONObject();

            for (String columnName : columnNames) {
                Value value = resultRow.getValue(columnName);

                if (value == null) {
                    // do nothing ...
                } else if (value.getType() == PropertyType.BINARY) {
                    jsonRow.put(columnName + BASE64_ENCODING_SUFFIX, RestHelper.jsonEncodedStringFor(value));
                } else {
                    jsonRow.put(columnName, value.getString());
                }
            }

            jsonRows.add(jsonRow);
        }

        JSONObject results = new JSONObject();

        if (result instanceof org.modeshape.jcr.api.query.QueryResult) {
            org.modeshape.jcr.api.query.QueryResult modeShapeResult = (org.modeshape.jcr.api.query.QueryResult)result;

            JSONObject columnTypeMap = new JSONObject();
            String[] columnTypes = modeShapeResult.getColumnTypes();

            assert columnTypes.length == columnNames.length;

            for (int i = 0; i < columnNames.length; i++) {
                columnTypeMap.put(columnNames[i], columnTypes[i]);
            }

            results.put("types", columnTypeMap);
        }

        results.put("rows", new JSONArray(jsonRows));
        return RestHelper.responseString(results, request);
    }

    protected Query createQuery( String language,
                                 String statement,
                                 Session session ) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        return queryManager.createQuery(statement, language);
    }

    protected void bindExtraVariables( UriInfo uriInfo,
                                       ValueFactory valueFactory,
                                       Query query ) throws RepositoryException {
        if (uriInfo == null) {
            return;
        }
        // Extract the query parameters and bind as variables ...
        for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            String variableName = entry.getKey();
            List<String> variableValues = entry.getValue();
            if (variableValues == null || variableValues.isEmpty() || SKIP_QUERY_PARAMETERS.contains(variableName)) {
                continue;
            }

            // Grab the first non-null value ...
            Iterator<String> valuesIterator = variableValues.iterator();
            String variableValue = null;
            while (valuesIterator.hasNext() && variableValue == null) {
                variableValue = valuesIterator.next();
            }
            if (variableValue == null) {
                continue;
            }
            // Bind the variable value to the variable name ...
            query.bindValue(variableName, valueFactory.createValue(variableValue));
        }
    }

}
