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

import org.modeshape.common.i18n.I18n;
import org.modeshape.jcr.spi.federation.ConnectorException;

/**
 * Thrown to indicate that there was a failure while attempting to retrieve metadata
 */
public class JdbcMetadataException extends ConnectorException {

    private static final long serialVersionUID = 1L;

    public JdbcMetadataException( I18n i18nText,
                                  Object... arguments ) {
        super(i18nText, arguments);
    }

    public JdbcMetadataException( I18n i18nText,
                                  Throwable cause,
                                  Object... arguments ) {
        super(i18nText, arguments);
    }

    public JdbcMetadataException( Throwable cause ) {
        super(cause);
    }

    public JdbcMetadataException( String message,
                                  Throwable cause ) {
        super(message, cause);
    }
}
