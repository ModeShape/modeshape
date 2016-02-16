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
 * Class which converts foreign key metadata to connector documents
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ForeignKeyRetriever extends AbstractMetadataRetriever {
    private static final Pattern FK_PATH_PATTERN = Pattern.compile(
            "/([^/]+)/([^/]+)/([^/]+)/tables/([^/]+)/foreignKeys/([^/]+)");
    private static final String FK_PREFIX = "fk";
    private static final Pattern FK_ID_PATTERN = Pattern.compile("([^@]+)@([^@]+)@([^@]+)@([^@]+)@" + FK_PREFIX + "@([^@]+)");

    protected ForeignKeyRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        String fkId = fkIdFrom(id);
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
        writer.addMixinType(JdbcMetadataLexicon.FOREIGN_KEY);
        writer.setParent(TableRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, tableId, true));

        List<ForeignKeyMetadata> foreignKeyMetadatas = connector.getMetadataCollector().getForeignKeys(connection, catalog,
                                                                                                       schema, tableId, fkId);
        if (!foreignKeyMetadatas.isEmpty()) {
            ForeignKeyMetadata metadata = foreignKeyMetadatas.get(0);
            writer.addProperty(JdbcMetadataLexicon.PRIMARY_KEY_CATALOG_NAME, metadata.getPrimaryKeyCatalogName());
            writer.addProperty(JdbcMetadataLexicon.PRIMARY_KEY_SCHEMA_NAME, metadata.getPrimaryKeySchemaName());
            writer.addProperty(JdbcMetadataLexicon.PRIMARY_KEY_TABLE_NAME, metadata.getPrimaryKeyTableName());
            writer.addProperty(JdbcMetadataLexicon.PRIMARY_KEY_COLUMN_NAME, metadata.getPrimaryKeyColumnName());

            writer.addProperty(JdbcMetadataLexicon.FOREIGN_KEY_CATALOG_NAME, metadata.getForeignKeyCatalogName());
            writer.addProperty(JdbcMetadataLexicon.FOREIGN_KEY_SCHEMA_NAME, metadata.getForeignKeySchemaName());
            writer.addProperty(JdbcMetadataLexicon.FOREIGN_KEY_TABLE_NAME, metadata.getForeignKeyTableName());
            writer.addProperty(JdbcMetadataLexicon.FOREIGN_KEY_COLUMN_NAME, metadata.getForeignKeyColumnName());

            writer.addProperty(JdbcMetadataLexicon.SEQUENCE_NR, metadata.getSequenceNr());
            writer.addProperty(JdbcMetadataLexicon.UPDATE_RULE, metadata.getUpdateRule());
            writer.addProperty(JdbcMetadataLexicon.DELETE_RULE, metadata.getDeleteRule());
            writer.addProperty(JdbcMetadataLexicon.FOREIGN_KEY_NAME, metadata.getForeignKeyName());
            writer.addProperty(JdbcMetadataLexicon.PRIMARY_KEY_NAME, metadata.getPrimaryKeyName());
            writer.addProperty(JdbcMetadataLexicon.DEFERRABILITY, metadata.getDeferrability());
        }
        return writer.document();
    }

    private String fkIdFrom( String id ) {
        Matcher matcher = FK_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(5) : null;
    }

    private String tableIdFrom( String id ) {
        Matcher matcher = FK_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(4) : null;
    }

    private String schemaIdFrom( String id ) {
        Matcher matcher = FK_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(3) : null;
    }

    private String catalogIdFrom( String id ) {
        Matcher matcher = FK_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(2) : null;
    }

    @Override
    protected String idFrom( String path ) {
        Matcher fkMatcher = FK_PATH_PATTERN.matcher(path);
        if (fkMatcher.matches()) {
            return documentId(fkMatcher.group(1), fkMatcher.group(2), fkMatcher.group(3), fkMatcher.group(4), fkMatcher.group(
                    5));
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return FK_ID_PATTERN.matcher(id).matches();
    }

    static String documentId( String databaseId,
                              String catalogId,
                              String schemaId,
                              String tableId,
                              String fkId ) {
        return generateId(databaseId, catalogId, schemaId, tableId, FK_PREFIX, fkId);
    }
}
