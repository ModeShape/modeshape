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
package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.spi.federation.DocumentWriter;

/**
 * Class which converts table metadata to connector documents.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class TableRetriever extends AbstractMetadataRetriever {
    private static final Pattern TABLE_PATH_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/tables/([^/]+)");
    private static final String TABLE_PREFIX = "table";
    private static final Pattern TABLE_ID_PATTERN = Pattern.compile("([^@]+)@([^@]+)@([^@]+)@" + TABLE_PREFIX + "@([^@]+)");

    static final Pattern FKS_PATH_PATTERN = Pattern.compile(TABLE_PATH_PATTERN.pattern() + "/foreignKeys");
    private static final String FKS_CONTAINER = "foreignKeys";
    private static final Pattern FKS_ID_PATTERN = Pattern.compile(TABLE_ID_PATTERN.pattern() + "@" + FKS_CONTAINER);

    protected TableRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        // the request is for a named table
        String tableId = tableIdFrom(id, TABLE_ID_PATTERN);
        if (tableId != null) {
            return createDocumentForTable(id, writer, connection, tableId);
        }

        // the request is for the foreign keys of a table
        tableId = tableIdFrom(id, FKS_ID_PATTERN);
        if (tableId != null) {
            return createDocumentForFks(id, writer, connection, tableId);
        }
        return null;
    }

    private Document createDocumentForFks( String id,
                                           DocumentWriter writer,
                                           Connection connection,
                                           String tableId ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.FOREIGN_KEYS);

        String catalogId = catalogIdFrom(id, FKS_ID_PATTERN);
        String catalog = catalogId;
        if (catalog.equalsIgnoreCase(connector.getDefaultCatalogName())) {
            catalog = null;
        }

        String schemaId = schemaIdFrom(id, FKS_ID_PATTERN);
        String schema = schemaId;
        if (schema.equalsIgnoreCase(connector.getDefaultSchemaName())) {
            schema = null;
        }
        writer.setParent(documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, false));

        List<ForeignKeyMetadata> fks = connector.getMetadataCollector().getForeignKeys(connection, catalog, schema, tableId, null);
        for (ForeignKeyMetadata fk : fks) {
            String foreignKeyColumnName = fk.getForeignKeyColumnName();
            String fkId = ForeignKeyRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, foreignKeyColumnName);
            writer.addChild(fkId, foreignKeyColumnName);
        }

        return writer.document();
    }

    private Document createDocumentForTable( String id,
                                             DocumentWriter writer,
                                             Connection connection,
                                             String tableId ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.TABLE);

        String catalogId = catalogIdFrom(id, TABLE_ID_PATTERN);
        String catalog = catalogId;
        if (catalog.equalsIgnoreCase(connector.getDefaultCatalogName())) {
            catalog = null;
        }

        String schemaId = schemaIdFrom(id, TABLE_ID_PATTERN);
        String schema = schemaId;
        if (schema.equalsIgnoreCase(connector.getDefaultSchemaName())) {
            schema = null;
        }
        writer.setParent(SchemaRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, true, false));

        List<TableMetadata> metadata = connector.getMetadataCollector().getTables(connection, catalog, schema, tableId);
        if (!metadata.isEmpty()) {
            TableMetadata tableMetadata = metadata.get(0);
            writer.addProperty(JdbcMetadataLexicon.TABLE_TYPE, tableMetadata.getType());
            writer.addProperty(JdbcMetadataLexicon.DESCRIPTION, tableMetadata.getDescription());
            writer.addProperty(JdbcMetadataLexicon.TYPE_CATALOG_NAME, tableMetadata.getTypeCatalogName());
            writer.addProperty(JdbcMetadataLexicon.TYPE_SCHEMA_NAME, tableMetadata.getTypeSchemaName());
            writer.addProperty(JdbcMetadataLexicon.TYPE_NAME, tableMetadata.getTypeName());
            writer.addProperty(JdbcMetadataLexicon.SELF_REFERENCING_COLUMN_NAME, tableMetadata.getSelfReferencingColumnName());
            writer.addProperty(JdbcMetadataLexicon.REFERENCE_GENERATION_STRATEGY_NAME,
                               tableMetadata.getReferenceGenerationStrategyName());
        }

        List<ColumnMetadata> columns = connector.getMetadataCollector().getColumns(connection, catalog, schema, tableId, null);
        for (ColumnMetadata column : columns) {
            String columnId = ColumnRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, column.getName());
            writer.addChild(columnId, column.getName());
        }

        String foreignKeysId = documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, true);
        writer.addChild(foreignKeysId, FKS_CONTAINER);

        return writer.document();
    }

    private String tableIdFrom( String id,
                                Pattern pattern ) {
        Matcher matcher = pattern.matcher(id);
        return matcher.matches() ? matcher.group(4) : null;
    }

    private String schemaIdFrom( String id,
                                 Pattern pattern ) {
        Matcher matcher = pattern.matcher(id);
        return matcher.matches() ? matcher.group(3) : null;
    }

    private String catalogIdFrom( String id,
                                  Pattern pattern ) {
        Matcher matcher = pattern.matcher(id);
        return matcher.matches() ? matcher.group(2) : null;
    }

    @Override
    protected String idFrom( String path ) {
        Matcher tableMatcher = TABLE_PATH_PATTERN.matcher(path);
        if (tableMatcher.matches() && !SchemaRetriever.PROCEDURES_PATH_PATTERN.matcher(path).matches()
            && !SchemaRetriever.TABLES_PATH_PATTERN.matcher(path).matches()) {
            String tableName = tableMatcher.group(4);
            return documentId(tableMatcher.group(1), tableMatcher.group(2), tableMatcher.group(3), tableName, false);
        }

        Matcher fksMatcher = FKS_PATH_PATTERN.matcher(path);
        if (fksMatcher.matches()) {
            return documentId(fksMatcher.group(1), fksMatcher.group(2), fksMatcher.group(3), fksMatcher.group(4), true);
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return TABLE_ID_PATTERN.matcher(id).matches() || FKS_ID_PATTERN.matcher(id).matches();
    }

    static String documentId( String databaseId,
                              String catalogId,
                              String schemaId,
                              String tableId,
                              boolean onlyFks ) {
        String baseId = generateId(databaseId, catalogId, schemaId, TABLE_PREFIX, tableId);
        if (onlyFks) {
            return generateId(baseId, FKS_CONTAINER);
        }
        return baseId;
    }
}
