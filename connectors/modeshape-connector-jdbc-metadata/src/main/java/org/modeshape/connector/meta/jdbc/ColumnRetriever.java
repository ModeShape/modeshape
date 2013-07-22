package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * Class which converts column metadata to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ColumnRetriever extends AbstractMetadataRetriever {

    private static final Pattern COLUMN_PATH_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/tables/([^/]+)/([^/]+)");
    private static final String COLUMN_PREFIX = "col";
    private static final Pattern COLUMN_ID_PATTERN = Pattern.compile(
            "([^@]+)@([^@]+)@([^@]+)@([^@]+)@" + COLUMN_PREFIX + "@([^@]+)");

    protected ColumnRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        String columnId = columnIdFrom(id);
        String tableId = tableIdFrom(id);

        String catalogId = catalogIdFrom(id);
        String catalog = catalogId;
        if (catalog.equalsIgnoreCase(connector.getDefaultCatalogName())) {
            catalog = null;
        }

        String schemaId = schemaIdFrom(id);
        String schema = schemaId;
        if (schema.equalsIgnoreCase(connector.getDefaultSchemaName())) {
            schema = null;
        }

        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.COLUMN);
        writer.setParent(TableRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, false));

        List<ColumnMetadata> metadatas = connector.getMetadataCollector().getColumns(connection, catalog, schema, tableId,
                                                                                     columnId);
        if (!metadatas.isEmpty()) {
            ColumnMetadata columnMetadata = metadatas.get(0);
            writer.addProperty(JdbcMetadataLexicon.JDBC_DATA_TYPE, columnMetadata.getJdbcDataType());
            writer.addProperty(JdbcMetadataLexicon.TYPE_NAME, columnMetadata.getTypeName());
            writer.addProperty(JdbcMetadataLexicon.COLUMN_SIZE, columnMetadata.getColumnSize());
            writer.addProperty(JdbcMetadataLexicon.DECIMAL_DIGITS, columnMetadata.getDecimalDigits());
            writer.addProperty(JdbcMetadataLexicon.RADIX, columnMetadata.getRadix());
            writer.addProperty(JdbcMetadataLexicon.NULLABLE, columnMetadata.getNullable());
            writer.addProperty(JdbcMetadataLexicon.DESCRIPTION, columnMetadata.getDescription());
            writer.addProperty(JdbcMetadataLexicon.LENGTH, columnMetadata.getLength());
            writer.addProperty(JdbcMetadataLexicon.ORDINAL_POSITION, columnMetadata.getOrdinalPosition());
            writer.addProperty(JdbcMetadataLexicon.SCOPE_CATALOG_NAME, columnMetadata.getScopeCatalogName());
            writer.addProperty(JdbcMetadataLexicon.SCOPE_SCHEMA_NAME, columnMetadata.getScopeSchemaName());
            writer.addProperty(JdbcMetadataLexicon.SCOPE_TABLE_NAME, columnMetadata.getScopeTableName());
            writer.addProperty(JdbcMetadataLexicon.SCOPE_SCHEMA_NAME, columnMetadata.getScopeSchemaName());
            writer.addProperty(JdbcMetadataLexicon.SOURCE_JDBC_DATA_TYPE, columnMetadata.getSourceJdbcDataType());
            writer.addProperty(JdbcMetadataLexicon.DEFAULT_VALUE, columnMetadata.getDefaultValue());
        }

        return writer.document();
    }

    private String columnIdFrom( String id ) {
        Matcher matcher = COLUMN_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(5) : null;
    }

    private String tableIdFrom( String id ) {
        Matcher matcher = COLUMN_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(4) : null;
    }

    private String schemaIdFrom( String id ) {
        Matcher matcher = COLUMN_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(3) : null;
    }

    private String catalogIdFrom( String id ) {
        Matcher matcher = COLUMN_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(2) : null;
    }

    @Override
    protected String idFrom( String path ) {
        Matcher columnNameMatcher = COLUMN_PATH_PATTERN.matcher(path);
        if (columnNameMatcher.matches() && !TableRetriever.FKS_PATH_PATTERN.matcher(path).matches()) {
            return documentId(columnNameMatcher.group(1),
                              columnNameMatcher.group(2),
                              columnNameMatcher.group(3),
                              columnNameMatcher.group(4),
                              columnNameMatcher.group(5));
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return COLUMN_ID_PATTERN.matcher(id).matches();
    }

    static String documentId( String databaseId,
                              String catalogId,
                              String schemaId,
                              String tableId,
                              String columnId ) {
        return generateId(databaseId, catalogId, schemaId, tableId, COLUMN_PREFIX, columnId);
    }
}
