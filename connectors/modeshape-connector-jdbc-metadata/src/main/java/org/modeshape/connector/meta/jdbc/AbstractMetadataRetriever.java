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
import java.util.Iterator;
import java.util.List;
import org.modeshape.schematic.document.Document;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.spi.federation.DocumentWriter;

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

    protected List<String> removeEmptyOrNullElements(List<String> list) {
        for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
            if (StringUtil.isBlank(it.next())) {
                it.remove();
            }
        }
        return list;
    }

    protected abstract Document getDocumentById( String id,
                                                 DocumentWriter writer,
                                                 Connection connection );

    protected abstract String idFrom( String path );

    protected abstract boolean canHandle( String id );
}
