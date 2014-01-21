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

/**
 * The internationalized string constants for the <code>org.modeshape.connector.meta.jdbc.*</code> packages.
 */
public final class JdbcMetadataI18n {

    public static I18n errorClosingConnection;
    public static I18n errorObtainingConnection;

    public static I18n errorFindingDataSourceInJndi;
    public static I18n driverClassNameAndUrlAreRequired;

    private JdbcMetadataI18n() {
    }

    static {
        try {
            I18n.initialize(JdbcMetadataI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
