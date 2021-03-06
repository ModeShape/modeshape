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
package org.modeshape.persistence.relational;

import org.modeshape.common.i18n.I18n;

/**
 * I18n message holder for the {@link RelationalProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since  5.0
 */
public final class RelationalProviderI18n {
    
    public static I18n jndiError;
    public static I18n unsupportedDBError;
    public static I18n warnCannotCloseConnection;
    public static I18n threadNotAssociatedWithTransaction;
    public static I18n threadAssociatedWithAnotherTransaction;
    public static I18n warnConnectionsNeedCleanup;

    private RelationalProviderI18n() {
    }

    static {
        try {
            I18n.initialize(RelationalProviderI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
