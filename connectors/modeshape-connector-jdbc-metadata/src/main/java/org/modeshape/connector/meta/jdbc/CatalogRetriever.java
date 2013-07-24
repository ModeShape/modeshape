package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * Class which converts catalog metadata to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class CatalogRetriever extends AbstractMetadataRetriever {
    private static final Pattern CATALOG_PATH_PATTERN = Pattern.compile("/([^/]+)/([^/]+)");
    private static final Pattern CATALOG_ID_PATTERN = Pattern.compile("([^@]+)@([^@]+)");

    protected CatalogRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.CATALOG);
        writer.setParent(DatabaseRetriever.ID);

        String catalogId = catalogIdFrom(id);
        MetadataCollector metadataCollector = connector.getMetadataCollector();
        List<String> schemaNames = catalogId.equalsIgnoreCase(connector.getDefaultCatalogName()) ?
                                   metadataCollector.getSchemaNames(connection, catalogId) :
                                   metadataCollector.getSchemaNames(connection, null);
        schemaNames = removeEmptyOrNullElements(schemaNames);
        if (schemaNames.isEmpty()) {
            schemaNames.add(connector.getDefaultSchemaName());
        }

        for (String schemaName : schemaNames) {
            String schemaDocumentId = SchemaRetriever.documentId(DatabaseRetriever.ID, catalogId, schemaName, false, false);
            writer.addChild(schemaDocumentId, schemaName);
        }
        return writer.document();
    }

    @Override
    protected String idFrom( String path ) {
        Matcher matcher = CATALOG_PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        return documentId(matcher.group(1), matcher.group(2));
    }

    @Override
    protected boolean canHandle( String id ) {
        return CATALOG_ID_PATTERN.matcher(id).matches();
    }

    private String catalogIdFrom( String id ) {
        Matcher matcher = CATALOG_ID_PATTERN.matcher(id);
        matcher.matches();
        return matcher.group(2);
    }

    static String documentId( String databaseId,
                              String catalogId ) {
        return generateId(databaseId, catalogId);
    }
}
