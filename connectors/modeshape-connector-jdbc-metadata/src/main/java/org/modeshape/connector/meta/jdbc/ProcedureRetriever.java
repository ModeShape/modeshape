package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * Class which converts procedure metadata to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ProcedureRetriever extends AbstractMetadataRetriever {
    private static final Pattern PROCEDURE_PATH_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/procedures/([^/]+)");
    private static final String PROCEDURE_PREFIX = "proc";
    private static final Pattern PROCEDURE_ID_PATTERN = Pattern.compile("([^@]+)@([^@]+)@([^@]+)@" + PROCEDURE_PREFIX + "@([^@]+)");

    protected ProcedureRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {

        String procedureId = procedureIdFrom(id);

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
        writer.addMixinType(JdbcMetadataLexicon.PROCEDURE);
        writer.setParent(SchemaRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaId, false, true));

        List<ProcedureMetadata> metadatas = connector.getMetadataCollector().getProcedures(connection, catalog, schema,
                                                                                           procedureId);
        if (!metadatas.isEmpty()) {
            ProcedureMetadata metadata = metadatas.get(0);
            writer.addProperty(JdbcMetadataLexicon.DESCRIPTION, metadata.getDescription());
            writer.addProperty(JdbcMetadataLexicon.PROCEDURE_RETURN_TYPE, metadata.getType());
        }
        return null;
    }

    private String procedureIdFrom( String id ) {
        Matcher matcher = PROCEDURE_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(4) : null;
    }

    private String schemaIdFrom( String id ) {
        Matcher matcher = PROCEDURE_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(3) : null;
    }

    private String catalogIdFrom( String id ) {
        Matcher matcher = PROCEDURE_ID_PATTERN.matcher(id);
        return matcher.matches() ? matcher.group(2) : null;
    }

    @Override
    protected String idFrom( String path ) {
        Matcher matcher = PROCEDURE_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            return documentId(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return PROCEDURE_ID_PATTERN.matcher(id).matches();
    }

    static String documentId( String databaseId,
                              String catalogId,
                              String schemaId,
                              String procedureId ) {
        return generateId(databaseId, catalogId, schemaId, PROCEDURE_PREFIX, procedureId);
    }
}
