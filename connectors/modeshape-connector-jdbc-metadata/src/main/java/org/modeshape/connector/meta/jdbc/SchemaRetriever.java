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
 * Class which converts schema metadata to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SchemaRetriever extends AbstractMetadataRetriever {

    private static final Pattern SCHEMA_PATH_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)");
    private static final Pattern SCHEMA_ID_PATTERN = Pattern.compile("([^@]+)@([^@]+)@([^@]+)");

    static final Pattern TABLES_PATH_PATTERN = Pattern.compile(SCHEMA_PATH_PATTERN.pattern() + "/tables");
    static final String TABLES_CONTAINER = "tables";
    private static final Pattern TABLES_ID_PATTERN = Pattern.compile(SCHEMA_ID_PATTERN.pattern() + "@" + TABLES_CONTAINER);

    static final Pattern PROCEDURES_PATH_PATTERN = Pattern.compile(SCHEMA_PATH_PATTERN.pattern() + "/procedures");
    static final String PROCEDURES_CONTAINER = "procedures";
    private static final Pattern PROCEDURES_ID_PATTERN = Pattern.compile(SCHEMA_ID_PATTERN + "@" + PROCEDURES_CONTAINER);

    protected SchemaRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        //the request is for a named/default schema
        String schemaId = schemaIdFrom(id, SCHEMA_ID_PATTERN);
        if (schemaId != null) {
            String catalogId = catalogIdFrom(id, SCHEMA_ID_PATTERN);
            return createDocumentForSchema(writer, catalogId, schemaId);
        }

        //the request is for all the tables of a schema
        String schemaIdFromTable = schemaIdFrom(id, TABLES_ID_PATTERN);
        if (schemaIdFromTable != null) {
            String catalogId = catalogIdFrom(id, TABLES_ID_PATTERN);
            return createDocumentForAllTables(writer, connection, catalogId, schemaIdFromTable);
        }

        //the request is for all the procedures of a schema
        String schemaIdFromProcedures = schemaIdFrom(id, PROCEDURES_ID_PATTERN);
        if (schemaIdFromProcedures != null) {
            String catalogId = catalogIdFrom(id, PROCEDURES_ID_PATTERN);
            return createDocumentForAllProcedures(writer, connection, catalogId, schemaIdFromProcedures);
        }

        return null;
    }

    private Document createDocumentForAllProcedures( DocumentWriter writer,
                                                     Connection connection,
                                                     String catalogId,
                                                     String schemaId ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.PROCEDURES);
        writer.setParent(SchemaRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, false, false));
        String catalog = catalogId;
        if (catalog.equalsIgnoreCase(connector.getDefaultCatalogName())) {
            catalog = null;
        }
        String schema = schemaId;
        if (schema.equalsIgnoreCase(connector.getDefaultSchemaName())) {
            schema = null;
        }

        List<ProcedureMetadata> procedures = connector.getMetadataCollector().getProcedures(connection, catalog, schema, null);
        if (!procedures.isEmpty()) {
            for (ProcedureMetadata procedureMetadata : procedures) {
                String procedureId = ProcedureRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId,
                                                                   procedureMetadata.getName());
                writer.addChild(procedureId, procedureMetadata.getName());
            }
        }
        return writer.document();
    }

    private Document createDocumentForAllTables( DocumentWriter writer,
                                                 Connection connection,
                                                 String catalogId,
                                                 String schemaId ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.TABLES);
        writer.setParent(SchemaRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, false, false));
        String catalog = catalogId;
        if (catalog.equalsIgnoreCase(connector.getDefaultCatalogName())) {
            catalog = null;
        }
        String schema = schemaId;
        if (schema.equalsIgnoreCase(connector.getDefaultSchemaName())) {
            schema = null;
        }

        List<TableMetadata> tables = connector.getMetadataCollector().getTables(connection, catalog, schema, null);
        if (!tables.isEmpty()) {
            for (TableMetadata tableMetadata : tables) {
                String tableId = TableRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId,
                                                           tableMetadata.getName(),
                                                           false);
                writer.addChild(tableId, tableMetadata.getName());
            }
        }
        return writer.document();
    }

    private Document createDocumentForSchema( DocumentWriter writer,
                                              String catalogId,
                                              String schemaId ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.SCHEMA);
        writer.setParent(CatalogRetriever.documentId(DatabaseRetriever.ID, catalogId));
        writer.addChild(documentId(DatabaseRetriever.ID, catalogId, schemaId, true, false), TABLES_CONTAINER);
        writer.addChild(documentId(DatabaseRetriever.ID, catalogId, schemaId, false, true), PROCEDURES_CONTAINER);
        return writer.document();
    }

    static String catalogIdFrom( String id,
                                 Pattern pattern ) {
        Matcher matcher = pattern.matcher(id);
        return matcher.matches() ? matcher.group(2) : null;
    }

    static String schemaIdFrom( String id,
                                Pattern pattern ) {
        Matcher matcher = pattern.matcher(id);
        return matcher.matches() ? matcher.group(3) : null;
    }

    @Override
    protected String idFrom( String path ) {
        Matcher namedSchemaMatcher = SCHEMA_PATH_PATTERN.matcher(path);
        if (namedSchemaMatcher.matches()) {
            return documentId(namedSchemaMatcher.group(1), namedSchemaMatcher.group(2), namedSchemaMatcher.group(3), false,
                              false);
        }
        Matcher tablesMatcher = TABLES_PATH_PATTERN.matcher(path);
        if (tablesMatcher.matches()) {
            return documentId(tablesMatcher.group(1), tablesMatcher.group(2), tablesMatcher.group(3), true, false);
        }
        Matcher proceduresMatcher = PROCEDURES_PATH_PATTERN.matcher(path);
        if (proceduresMatcher.matches()) {
            return documentId(proceduresMatcher.group(1), proceduresMatcher.group(2), proceduresMatcher.group(3), false, true);
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return SCHEMA_ID_PATTERN.matcher(id).matches() ||
                TABLES_ID_PATTERN.matcher(id).matches() ||
                PROCEDURES_ID_PATTERN.matcher(id).matches();
    }

    static String documentId( String databaseId,
                              String catalogId,
                              String schemaId,
                              boolean onlyTables,
                              boolean onlyProcedures ) {
        String baseId = generateId(databaseId, catalogId, schemaId);
        if (onlyTables) {
            return generateId(baseId, "tables");
        } else if (onlyProcedures) {
            return generateId(baseId, "procedures");
        } else {
            return baseId;
        }
    }
}
