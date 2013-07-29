package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * Class which converts database metadata, to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class DatabaseRetriever extends AbstractMetadataRetriever {

    protected static final String ID = "databaseRoot";
    private static final Pattern ROOT_DB_PATTERN = Pattern.compile("/[^/]*");

    protected DatabaseRetriever( JdbcMetadataConnector connector ) {
        super(connector);
    }

    @Override
    protected Document getDocumentById( String id,
                                        DocumentWriter writer,
                                        Connection connection ) {
        MetadataCollector metadataCollector = connector.getMetadataCollector();

        DBMetadata dbMetadata = metadataCollector.getDatabaseMetadata(connection);
        writer.setPrimaryType(JcrNtLexicon.UNSTRUCTURED);
        writer.addMixinType(JdbcMetadataLexicon.DATABASE_ROOT);
        writer.addProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_NAME, dbMetadata.getDatabaseProductName());
        writer.addProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION, dbMetadata.getDatabaseProductVersion());
        writer.addProperty(JdbcMetadataLexicon.DATABASE_MAJOR_VERSION, dbMetadata.getDatabaseMajorVersion());
        writer.addProperty(JdbcMetadataLexicon.DATABASE_MINOR_VERSION, dbMetadata.getDatabaseMinorVersion());

        List<String> catalogs = removeEmptyOrNullElements(metadataCollector.getCatalogNames(connection));
        if (catalogs.isEmpty()) {
            catalogs.add(connector.getDefaultCatalogName());
        }
        for (String catalogName : catalogs) {
            writer.addChild(CatalogRetriever.documentId(id, catalogName), catalogName);
        }

        return writer.document();
    }

    @Override
    protected String idFrom( String path ) {
        if (ROOT_DB_PATTERN.matcher(path).matches()) {
            return ID;
        }
        return null;
    }

    @Override
    protected boolean canHandle( String id ) {
        return id.toLowerCase().equalsIgnoreCase(ID);
    }
}
