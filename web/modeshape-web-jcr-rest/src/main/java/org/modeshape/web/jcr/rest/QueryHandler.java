package org.modeshape.web.jcr.rest;

import java.util.LinkedList;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Resource handler that implements REST methods for items.
 */
@Immutable
public class QueryHandler extends AbstractHandler {

    public String postItem( HttpServletRequest request,
                            String rawRepositoryName,
                            String rawWorkspaceName,
                            String language,
                            String statement,
                            long offset,
                            long limit ) throws RepositoryException, JSONException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert language != null;
        assert statement != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        QueryManager queryManager = session.getWorkspace().getQueryManager();

        Query query = queryManager.createQuery(statement, language);
        QueryResult result = query.execute();

        String[] columnNames = result.getColumnNames();

        List<JSONObject> jsonRows = new LinkedList<JSONObject>();
        RowIterator resultRows = result.getRows();

        if (offset > 0) {
            resultRows.skip(offset);
        }

        if (limit < 0) limit = Long.MAX_VALUE;

        while (resultRows.hasNext() && limit > 0) {
            limit--;
            Row resultRow = resultRows.nextRow();
            JSONObject jsonRow = new JSONObject();

            for (String columnName : columnNames) {
                Value value = resultRow.getValue(columnName);

                if (value.getType() == PropertyType.BINARY) {
                    jsonRow.put(columnName + BASE64_ENCODING_SUFFIX, jsonEncodedStringFor(value));
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
        return results.toString();
    }

}
