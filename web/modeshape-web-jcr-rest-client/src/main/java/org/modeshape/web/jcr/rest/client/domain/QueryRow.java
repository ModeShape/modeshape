package org.modeshape.web.jcr.rest.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import net.jcip.annotations.Immutable;

@Immutable
public class QueryRow {

    private Map<String, String> queryTypes;
    private Map<String, Object> values;

    public QueryRow( Map<String, String> queryTypes,
                     Map<String, Object> values ) {
        super();
        // queryTypes is expected to already be an unmodifiable map
        this.queryTypes = queryTypes;
        this.values = Collections.unmodifiableMap(values);
    }

    public Collection<String> getColumnNames() {
        return values.keySet();
    }

    public Object getValue( String columnName ) {
        return values.get(columnName);
    }

    public String getColumnType( String columnName ) {
        return queryTypes.get(columnName);
    }
}
