package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.DocumentWriter;

/**
 * Base class for converting DB Metadata to connector documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractMetadataRetriever {
    protected JdbcMetadataConnector connector;

    protected AbstractMetadataRetriever( JdbcMetadataConnector connector ) {
        this.connector = connector;
    }

    protected static String generateId( String prefix,
                                        String... segments ) {
        StringBuilder builder = new StringBuilder(prefix);
        for (String segment : segments) {
            builder.append("@").append(segment);
        }
        return builder.toString();
    }

    protected abstract Document getDocumentById( String id,
                                                 DocumentWriter writer,
                                                 Connection connection );

    protected abstract String idFrom( String path );

    protected abstract boolean canHandle( String id );
}
